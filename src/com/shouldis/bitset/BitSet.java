package com.shouldis.bitset;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * This class represents a fixed number of bits stored within an internal
 * integer array, mapping positive integer indices to individual bits and
 * facilitating the manipulation of those individual bits or ranges of bits, as
 * well as retrieving their boolean values. Using this memory structure, this
 * class allows grouping of multiple bitwise operations into fewer operations on
 * the underlying integer words.
 * <p>
 * The bits represented by this {@link BitSet} will either be in the <i>live</i>
 * state ({@code 1, true}), or the <i>dead</i> state ({@code 0, false}).
 * <p>
 * {@link BitSet} on its own is thread-safe only for read operations, although a
 * {@link BitSpliterator} may be used to stream indices in an order and grouping
 * appropriate for parallel manipulation of bits. Alternatively, a
 * {@link ConcurrentBitSet} may be used to make all operations thread-safe,
 * requiring no external synchronization at the cost of performance.
 * <p>
 * If {@link #size} isn't a multiple of {@link BitSet#WORD_SIZE}, there will be
 * "hanging" bits that exist on the end of the last integer word, but are not
 * accounted for by {@link #size}. No exception will be thrown when these bit
 * indices are manipulated or read, and in aggregating functions
 * ({@link #population()}, {@link #hashCode()}, etc.), any hanging bits will
 * have their effect on those functions made consistent by
 * {@link #cleanLastWord()}.
 * <p>
 * Otherwise, accessing a negative index, or any index greater than or equal to
 * {@link #size} will cause an {@link IndexOutOfBoundsException}.
 * 
 * @author Aaron Shouldis
 * @see BitSpliterator
 * @see ConcurrentBitSet
 */
public class BitSet {

	/**
	 * Number of bits in each integer word (32).
	 */
	public static final int WORD_SIZE = Integer.SIZE;

	/**
	 * Mask used to compute potentially faster modulo operations. {@code n % m} is
	 * equivalent to {@code n & (m -1)} if n is positive, and m = 2<sup>k</sup>.
	 */
	protected static final int MOD_SIZE_MASK = WORD_SIZE - 1;

	/**
	 * log<sub>2</sub>({@link BitSet#WORD_SIZE}). Used to relate bit indices to word
	 * indices through bit-shifting as an alternative to division or multiplication
	 * by {@link #WORD_SIZE}.
	 */
	protected static final int LOG_2_SIZE = 5;

	/**
	 * Integer mask with all bits set. Used to isolate portions of words.
	 * (0xFFFFFFFF)
	 */
	protected static final int MASK = -1;

	/**
	 * The number of indices accessible by this {@link BitSet}. Indices <b>0</b>
	 * through <b>size -1</b> are accessible.
	 */
	public final int size;

	/**
	 * Array holding the integer words whose bits are manipulated. Has length
	 * ceiling({@link #size} / {@link BitSet#WORD_SIZE}).
	 */
	protected final int[] words;

	/**
	 * Creates a {@link BitSet} with the specified number of bits indices. Indices 0
	 * through <b>size</b> -1 will be accessible. All bits are initially in the
	 * <i>dead</i> state.
	 * 
	 * @param size the number of bit indices that this {@link BitSet} will hold.
	 * @throws IllegalArgumentException if <b>size</b> is less than 1.
	 */
	public BitSet(int size) {
		this.size = size;
		int wordCount = wordIndex(size);
		if (wordCount >= 0 && modSize(size) > 0) {
			wordCount++;
		}
		if (wordCount < 0) {
			throw new IllegalArgumentException(Integer.toString(size));
		}
		words = new int[wordCount];
	}

	/**
	 * Creates a {@link BitSet} which is a clone of the specified {@link BitSet}
	 * <b>set</b>. The copy will have an identical {@link #size}, and will copy the
	 * contents of <b>set</b>'s {@link #words}.
	 * 
	 * @param set the {@link BitSet} to copy.
	 * @throws NullPointerException if <b>set</b> is null.
	 */
	public BitSet(BitSet set) {
		this(set.size);
		copy(set);
	}

	/**
	 * Creates a {@link BitSet} with the specified <b>size</b> using the specified
	 * array of <b>bytes</b> created by serializing a {@link BitSet} with
	 * {@link #bytes()}.
	 * 
	 * @param bytes the byte array representation of a {@link BitSet} to be copied
	 *              into the contents {@link #words}.
	 * @param size  the number of indices the created {@link BitSet} will hold.
	 * @return a {@link BitSet} of the specified <b>size</b> made from the specified
	 *         <b>bytes</b>.
	 * @throws NullPointerException       if <b>bytes</b> is null.
	 * @throws NegativeArraySizeException if <b>size</b> is negative.
	 * @throws IllegalArgumentException   if (<b>bytes</b>.length * 8) is less than
	 *                                    <b>size</b>.
	 * @see BitSet#BitSet(int)
	 */
	public static BitSet read(byte[] bytes, int size) {
		if (bytes.length < size >> 3) {
			StringBuilder builder = new StringBuilder();
			builder.append(bytes.length << 3).append(" < ").append(size);
			throw new IllegalArgumentException(builder.toString());
		}
		BitSet set = new BitSet(size);
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < set.words.length; i++) {
			set.words[i] = buffer.getInt();
		}
		return set;
	}

	/**
	 * Ensures that the bit at the specified <b>index</b> is in the <i>live</i>
	 * state. If it is not, it will be changed.
	 * 
	 * @param index the index of the bit to change to the <i>live</i> state.
	 * @return whether or not this {@link BitSet} was changed as a result.
	 * @throws ArrayIndexOutOfBoundsException if <b>index</b> is negative, or
	 *                                        greater than or equal to
	 *                                        {@link #size}.
	 */
	public boolean add(int index) {
		int wordIndex = wordIndex(index);
		int mask = bitMask(index);
		if ((words[wordIndex] & mask) != 0) {
			return false;
		}
		words[wordIndex] |= mask;
		return true;
	}

	/**
	 * Ensures that the bit at the specified <b>index</b> is the <i>dead</i> state.
	 * If it is not, it will be changed.
	 * 
	 * @param index the index of the bit to change to the <i>dead</i> state.
	 * @return whether or not this {@link BitSet} was changed as a result.
	 * @throws ArrayIndexOutOfBoundsException if <b>index</b> is negative, or
	 *                                        greater than or equal to
	 *                                        {@link #size}.
	 */
	public boolean remove(int index) {
		int wordIndex = wordIndex(index);
		int mask = bitMask(index);
		if ((words[wordIndex] & mask) == 0) {
			return false;
		}
		words[wordIndex] &= ~mask;
		return true;
	}

	/**
	 * Checks the current state of a bit at the specified <b>index</b>. Returns
	 * {@code true} if the bit is in the <i>live</i> state, and {@code false} if it
	 * is not.
	 * 
	 * @param index the bit to examine.
	 * @return the state of the specified bit.
	 * @throws ArrayIndexOutOfBoundsException if <b>index</b> is negative or greater
	 *                                        than or equal to {@link #size}.
	 */
	public final boolean get(int index) {
		return (words[wordIndex(index)] & bitMask(index)) != 0;
	}

	/**
	 * Calculates the number of <i>live</i> bits in the specified range.
	 * 
	 * @param from (inclusive) the index of the first bit to be checked.
	 * @param to   (exclusive) the end of the range of bits to be checked.
	 * @return the number of <i>live</i> bits inside the specified range.
	 * @throws {@link ArrayIndexOutOfBoundsException} if <b>from</b> or <b>to</b>
	 *                are outside of the range 0 to {@link #size},
	 */
	public final int get(int from, int to) {
		if (from >= to) {
			return 0;
		}
		int start = wordIndex(from);
		int end = wordIndex(to - 1);
		int startMask = MASK << from;
		int endMask = MASK >>> -to;
		int sum = 0;
		if (start == end) {
			sum += Integer.bitCount(words[start] & startMask & endMask);
		} else {
			sum += Integer.bitCount(words[start] & startMask);
			for (int i = start + 1; i < end; i++) {
				sum += Integer.bitCount(words[i]);
			}
			sum += Integer.bitCount(words[end] & endMask);
		}
		return sum;
	}

	/**
	 * Changes the state of a bit at the specified <b>index</b> to the <i>live</i>
	 * state.
	 * 
	 * @param index the index of the bit to change to the <i>live</i> state.
	 * @throws ArrayIndexOutOfBoundsException if <b>index</b> is negative or greater
	 *                                        than or equal to {@link #size}.
	 */
	public void set(int index) {
		words[wordIndex(index)] |= bitMask(index);
	}

	/**
	 * Changes the state of all bits in the specified range to the <i>live</i>
	 * state.
	 * 
	 * @param from (inclusive) the index of the first bit to be changed to the
	 *             <i>live</i> state.
	 * @param to   (exclusive) the end of the range of bits to be changed to the
	 *             <i>live</i> state.
	 * @throw {@link ArrayIndexOutOfBoundsException} if <b>from</b> or <b>to</b> are
	 *        outside of the range 0 to {@link #size},
	 */
	public void set(int from, int to) {
		if (from >= to) {
			return;
		}
		int start = wordIndex(from);
		int end = wordIndex(to - 1);
		int startMask = MASK << from;
		int endMask = MASK >>> -to;
		if (start == end) {
			words[start] |= startMask & endMask;
		} else {
			words[start] |= startMask;
			for (int i = start + 1; i < end; i++) {
				words[i] = MASK;
			}
			words[end] |= endMask;
		}
	}

	/**
	 * Changes the state of a bit at the specified <b>index</b> to the <i>dead</i>
	 * state.
	 * 
	 * @param index the index of the bit to clear.
	 * @throws ArrayIndexOutOfBoundsException if <b>index</b> is negative or greater
	 *                                        than or equal to {@link #size}.
	 */
	public void clear(int index) {
		words[wordIndex(index)] &= ~bitMask(index);
	}

	/**
	 * Changes the state of all bits in the specified range to the <i>dead</i>
	 * state.
	 * 
	 * @param from (inclusive) the index of the first bit to be cleared.
	 * @param to   (exclusive) the end of the range of bits to be cleared.
	 * @throw {@link ArrayIndexOutOfBoundsException} if <b>from</b> or <b>to</b> are
	 *        outside of the range 0 to {@link #size},
	 */
	public void clear(int from, int to) {
		if (from >= to) {
			return;
		}
		int start = wordIndex(from);
		int end = wordIndex(to - 1);
		int startMask = MASK << from;
		int endMask = MASK >>> -to;
		if (start == end) {
			words[start] &= ~(startMask & endMask);
		} else {
			words[start] &= ~startMask;
			for (int i = start + 1; i < end; i++) {
				words[i] = 0;
			}
			words[end] &= ~endMask;
		}
	}

	/**
	 * Changes the state of the bit at the specified <b>index</b> to its opposite
	 * through an {@code XOR} operation.
	 * 
	 * @param index the index of the bit to toggle.
	 * @throws ArrayIndexOutOfBoundsException if <b>index</b> is negative or greater
	 *                                        than or equal to {@link #size}.
	 */
	public void toggle(int index) {
		words[wordIndex(index)] ^= bitMask(index);
	}

	/**
	 * Changes the state of all bits in the specified range to their respective
	 * opposites through an {@code XOR} operation.
	 * 
	 * @param from (inclusive) the index of the first bit to be toggled.
	 * @param to   (exclusive) the end of the range of bits to be toggled.
	 * @throw {@link ArrayIndexOutOfBoundsException} if <b>from</b> or <b>to</b> are
	 *        outside of the range 0 to {@link #size},
	 */
	public void toggle(int from, int to) {
		if (from >= to) {
			return;
		}
		int start = wordIndex(from);
		int end = wordIndex(to - 1);
		int startMask = MASK << from;
		int endMask = MASK >>> -to;
		if (start == end) {
			words[start] ^= startMask & endMask;
		} else {
			words[start] ^= startMask;
			for (int i = start + 1; i < end; i++) {
				words[i] = ~words[i];
			}
			words[end] ^= endMask;
		}
	}

	/**
	 * Changes the state of all bits in the specified range randomly.
	 * 
	 * @param from (inclusive) the index of the first bit to be randomized.
	 * @param to   (exclusive) the end of the range of bits to be randomized.
	 * @throw {@link ArrayIndexOutOfBoundsException} if <b>from</b> or <b>to</b> are
	 *        outside of the range 0 to {@link #size},
	 */
	public void randomize(int from, int to) {
		if (from >= to) {
			return;
		}
		ThreadLocalRandom random = ThreadLocalRandom.current();
		int start = wordIndex(from);
		int end = wordIndex(to - 1);
		int startMask = MASK << from;
		int endMask = MASK >>> -to;
		if (start == end) {
			words[start] ^= startMask & endMask & random.nextInt();
		} else {
			words[start] ^= startMask & random.nextInt();
			for (int i = start + 1; i < end; i++) {
				words[i] = random.nextInt();
			}
			words[end] ^= endMask & random.nextInt();
		}
	}

	/**
	 * Changes the state of all bits randomly.
	 */
	public void randomize() {
		randomize(0, size);
	}

	/**
	 * Calculates the index of the next <i>live</i> bit after the specified
	 * <b>index</b>, including that <b>index</b>. All bits from that specified
	 * <B>index</b>, until {@link #size} will be checked. If no <i>live</i> bits are
	 * found, -1 is returned.
	 * 
	 * @param index (inclusive) the first index to check.
	 * @return the index of the next <i>live</i> bit, or -1 if none were found.
	 */
	public final int nextLive(int index) {
		int wordIndex = wordIndex(index);
		if (wordIndex >= words.length || wordIndex < 0) {
			return -1;
		}
		int word = words[wordIndex] & (MASK << index);
		if (word != 0) {
			return nextLiveBit(word, wordIndex);
		}
		while (++wordIndex < words.length) {
			word = words[wordIndex];
			if (word != 0) {
				return nextLiveBit(word, wordIndex);
			}
		}
		return -1;
	}

	/**
	 * Calculates the index of the next <i>dead</i> bit after the specified
	 * <b>index</b>, including that <b>index</b>. All bits from that specified
	 * <b>index</b>, until {@link #size} will be checked. If none are found, -1 is
	 * returned.
	 * 
	 * @param index (inclusive) the first index to check.
	 * @return the index of the next <i>dead</i> bit, or -1 if none were found.
	 */
	public final int nextDead(int index) {
		int wordIndex = wordIndex(index);
		if (wordIndex >= words.length || wordIndex < 0) {
			return -1;
		}
		int word = ~words[wordIndex] & (MASK << index);
		if (word != 0) {
			return nextLiveBit(word, wordIndex);
		}
		while (++wordIndex < words.length) {
			word = ~words[wordIndex];
			if (word != 0) {
				return nextLiveBit(word, wordIndex);
			}
		}
		return -1;
	}

	/**
	 * Calculates the index of the most recent <i>live</i> bit before the specified
	 * <b>index</b>, including that <b>index</b>. If none are found, -1 is returned.
	 * 
	 * @param index (inclusive) the first index to check.
	 * @return the index of the next <i>live</i> bit, or -1 if none were found.
	 */
	public final int lastLive(int index) {
		int wordIndex = wordIndex(index);
		if (wordIndex >= words.length || wordIndex < 0) {
			return -1;
		}
		int word = words[wordIndex] & (MASK >>> -(index + 1));
		if (word != 0) {
			return lastLiveBit(word, wordIndex);
		}
		while (wordIndex-- > 0) {
			word = words[wordIndex];
			if (word != 0) {
				return lastLiveBit(word, wordIndex);
			}
		}
		return -1;
	}

	/**
	 * Calculates the index of the most recent <i>dead</i> bit before the specified
	 * <b>index</b>, including that <b>index</b>. If none are found, -1 is returned.
	 * 
	 * @param index (inclusive) the first index to check.
	 * @return the index of the next <i>dead</i> bit, or -1 if none were found.
	 */
	public final int lastDead(int index) {
		int wordIndex = wordIndex(index);
		if (wordIndex >= words.length || wordIndex < 0) {
			return -1;
		}
		int word = ~words[wordIndex] & (MASK >>> -(index + 1));
		if (word != 0) {
			return lastLiveBit(word, wordIndex);
		}
		while (wordIndex-- > 0) {
			word = ~words[wordIndex];
			if (word != 0) {
				return lastLiveBit(word, wordIndex);
			}
		}
		return -1;
	}

	/**
	 * Creates a parallel-safe {@link IntStream} consisting of the indices of all
	 * <i>live</i> bits within this {@link BitSet} using
	 * {@link BitSpliterator.Live}.
	 * 
	 * @return a parallel-safe {@link IntStream} representation of <i>live</i>
	 *         indices.
	 */
	public final IntStream live() {
		return new BitSpliterator.Live(this).stream();
	}

	/**
	 * Creates a parallel-safe {@link IntStream} consisting of the indices of all
	 * <i>dead</i> bits within this {@link BitSet} using
	 * {@link BitSpliterator.Dead}.
	 * 
	 * @return a parallel-safe {@link IntStream} representation of <i>dead</i>
	 *         indices.
	 */
	public final IntStream dead() {
		return new BitSpliterator.Dead(this).stream();
	}

	/**
	 * Performs a global {@code OR} operation on all bits in this {@link BitSet}
	 * with those in the specified {@link BitSet} <b>set</b>.
	 * 
	 * @param set the other {@link BitSet} from which to perform the {@code OR}
	 *            operation.
	 * @throws IllegalArgumentException if the {@link #size}s of both
	 *                                  {@link BitSet}s are not equal.
	 * @throws NullPointerException     if <b>set</b> is null.
	 */
	public void or(BitSet set) {
		compareSize(set);
		for (int i = 0; i < words.length; i++) {
			words[i] |= set.words[i];
		}
	}

	/**
	 * Performs a global {@code XOR} operation on all bits in this {@link BitSet}
	 * with those in the specified {@link BitSet} <b>set</b>.
	 * 
	 * @param set the other {@link BitSet} from which to perform the {@code XOR}
	 *            operation.
	 * @throws IllegalArgumentException if the {@link #size}s of both
	 *                                  {@link BitSet}s are not equal.
	 * @throws NullPointerException     if <b>set</b> is null.
	 */
	public void xor(BitSet set) {
		compareSize(set);
		for (int i = 0; i < words.length; i++) {
			words[i] ^= set.words[i];
		}
	}

	/**
	 * Performs a global {@code AND} operation on all bits in this {@link BitSet}
	 * with those in the specified {@link BitSet} <b>set</b>.
	 * 
	 * @param set the other {@link BitSet} from which to perform the {@code AND}
	 *            operation.
	 * @throws IllegalArgumentException if the {@link #size}s of both
	 *                                  {@link BitSet}s are not equal.
	 * @throws NullPointerException     if <b>set</b> is null.
	 */
	public void and(BitSet set) {
		compareSize(set);
		for (int i = 0; i < words.length; i++) {
			words[i] &= set.words[i];
		}
	}

	/**
	 * Transforms this {@link BitSet} into the complement of the specified
	 * <b>set</b>.
	 * 
	 * @param set the other {@link BitSet} from which to perform the {@code NOT}
	 *            operation.
	 * @throws IllegalArgumentException if the {@link #size}s of both
	 *                                  {@link BitSet}s are not equal.
	 * @throws NullPointerException     if <b>set</b> is null.
	 */
	public void not(BitSet set) {
		compareSize(set);
		for (int i = 0; i < words.length; i++) {
			words[i] = ~set.words[i];
		}
	}

	/**
	 * Transforms this {@link BitSet} so that each bit matches the state of that in
	 * the give <b>set</b>.
	 * 
	 * @param set the other {@link BitSet} from which to copy.
	 * @throws IllegalArgumentException if the {@link #size}s of both
	 *                                  {@link BitSet}s are not equal.
	 * @throws NullPointerException     if <b>set</b> is null.
	 */
	public final void copy(BitSet set) {
		compareSize(set);
		System.arraycopy(set.words, 0, words, 0, words.length);
	}

	/**
	 * Transforms each bit in this {@link BitSet} to the <i>live</i> state.
	 */
	public final void fill() {
		for (int i = 0; i < words.length; i++) {
			words[i] = MASK;
		}
	}

	/**
	 * Transforms each bit in this {@link BitSet} to the <i>dead</i> state.
	 */
	public final void empty() {
		for (int i = 0; i < words.length; i++) {
			words[i] = 0;
		}
	}

	/**
	 * Transforms each bit in this {@link BitSet} into the complement of its current
	 * state.
	 */
	public void not() {
		for (int i = 0; i < words.length; i++) {
			words[i] = ~words[i];
		}
	}

	/**
	 * Calculates the number of <i>live</i> bits within this {@link BitSet}.
	 * 
	 * @return the number of <i>live</i> bits.
	 */
	public final int population() {
		int population = 0;
		cleanLastWord();
		for (int i = 0; i < words.length; i++) {
			population += Integer.bitCount(words[i]);
		}
		return population;
	}

	/**
	 * Calculates what percentage of bits in this {@link BitSet} are in the
	 * <i>live</i> state in the specified range.
	 * 
	 * @param from (inclusive) the index of the first bit to be checked.
	 * @param to   (exclusive) the end of the range of bits to be checked.
	 * @return the percentage of <i>live</i> bits.
	 */
	public final float density(int from, int to) {
		if (from >= to) {
			return 0.0f;
		}
		return get(from, to) / (float) (to - from);
	}

	/**
	 * Calculates what percentage of bits in this {@link BitSet} are in the
	 * <i>live</i> state.
	 *
	 * @return the percentage of <i>live</i> bits.
	 */
	public final float density() {
		return population() / (float) size;
	}

	/**
	 * Calculates a {@code long} identifier number generated from
	 * {@link #hashCode()} and {@link #population()}.
	 * 
	 * @return the unique identifying code.
	 */
	public final long identifier() {
		return ((long) hashCode() << WORD_SIZE) + population();
	}

	/**
	 * Changes the state of any "hanging bits" to the <i>dead</i> state in order to
	 * maintain their effect on aggregating functions ({@link #population()}, etc).
	 */
	protected void cleanLastWord() {
		int hangingBits = modSize(-size);
		if (hangingBits > 0 && words.length > 0) {
			words[words.length - 1] &= (MASK >>> hangingBits);
		}
	}

	/**
	 * Converts the contents of this {@link BitSet} into an array of bytes.
	 * 
	 * @return the byte array representation.
	 * @see {@link BitSet#read(byte[], int)}
	 */
	public final byte[] bytes() {
		cleanLastWord();
		byte[] bytes = new byte[words.length << 2];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < words.length; i++) {
			buffer.putInt(words[i]);
		}
		return bytes;
	}

	/**
	 * Calculates a mask to represent the bit at which a specific index will be
	 * stored within an integer word.
	 * 
	 * @param index the index to represent as a bit.
	 * @return the bit that represents the position of an index within a word.
	 */
	protected static final int bitMask(int index) {
		return 1 << index;
	}

	/**
	 * Equivalent to {@code index % SIZE} for positive numbers, and
	 * {@code SIZE - (index % SIZE)} for negative numbers. Used calculate the modulo
	 * of positive numbers faster.
	 * 
	 * @param index the index to perform the modulo operation upon.
	 * @return the result of the modulo operation.
	 * @see BitSet#MOD_SIZE_MASK
	 */
	protected static final int modSize(int index) {
		return index & MOD_SIZE_MASK;
	}

	/**
	 * Calculates the index of the word corresponding to the specified <b>index</b>.
	 * 
	 * @param index the position of the word containing the bit at the specified
	 *              index.
	 * @return the index of the word within {@link #words}.
	 */
	protected static final int wordIndex(int index) {
		return index >> LOG_2_SIZE;
	}

	/**
	 * Calculates the first <i>live</i> bit index within a word at the specified
	 * <b>wordIndex</b>.
	 * 
	 * @param wordIndex the index of a word within {@link #words}.
	 * @return the index of the first bit contained by that word.
	 */
	protected static final int wordStart(int wordIndex) {
		return wordIndex << LOG_2_SIZE;
	}

	/**
	 * Calculates the index of the next <i>live</i> bit within a specified
	 * <b>word</b> that is at the specified <b>wordIndex</b> within {@link #words}.
	 * This index will represent its bit index in the underlying integer array as
	 * well as the offset within the integer word.
	 * 
	 * @param word      the integer word to be checked for a <i>live</i> bit.
	 * @param wordIndex the index of the word within {@link #words}.
	 * @return the index of the next <i>live</i> bit within the specified word, or
	 *         -1 if none is found.
	 */
	private int nextLiveBit(int word, int wordIndex) {
		int index = wordStart(wordIndex) + Integer.numberOfTrailingZeros(word);
		return index < size ? index : -1;
	}

	/**
	 * Calculates the index of the recent-most <i>live</i> bit within a specified
	 * <b>word</b> that is at the specified <b>wordIndex</b> within {@link #words}.
	 * This index will represent its bit index in the underlying integer array as
	 * well as the offset within the integer word.
	 * 
	 * @param word      the integer word to be checked for a <i>live</i> bit.
	 * @param wordIndex the index of the word within {@link #words}.
	 * @return the index of the recent-most <i>live</i> bit within the specified
	 *         word.
	 */
	private int lastLiveBit(int word, int wordIndex) {
		int index = ((wordIndex + 1) << LOG_2_SIZE) - Integer.numberOfLeadingZeros(word) - 1;
		return index < size ? index : -1;
	}

	/**
	 * Compares the {@link #size} of this {@link BitSet} with that of the other
	 * specified {@link BitSet} <b>set</b>. Enforces that they are equal, throwing a
	 * {@link IllegalArgumentException} otherwise.
	 * 
	 * @param set the {@link BitSet} to compare {@link #size}s with.
	 * @throws NullPointerException           if <b>set</b> is null.
	 * @throws ArrayIndexOutOfBoundsException if the {@link #size}s are different.
	 */
	protected final void compareSize(BitSet set) {
		if (set.size != size) {
			throw new ArrayIndexOutOfBoundsException(Math.min(set.words.length, words.length) - 1);
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Size: [").append(size).append("] Population: [").append(population()).append(']');
		return builder.toString();
	}

	@Override
	public int hashCode() {
		cleanLastWord();
		return size ^ Arrays.hashCode(words);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		boolean equal = obj instanceof BitSet;
		if (equal) {
			BitSet other = (BitSet) obj;
			cleanLastWord();
			other.cleanLastWord();
			equal = Arrays.equals(words, other.words);
		}
		return equal;
	}

}