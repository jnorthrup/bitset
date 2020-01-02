package com.shouldis.bitset;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Represents a fixed number of bits stored within an internal long array,
 * mapping positive integer indices to individual bits and facilitating the
 * manipulation of those individual bits or ranges of bits, as well as
 * retrieving their boolean values. Using this memory structure, this class
 * allows grouping of multiple bitwise operations into fewer operations on the
 * underlying long words.
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
 * If {@link #size} isn't a multiple of 64, there will be "hanging" bits that
 * exist on the end of the last long within {@link #words}, which are not
 * accounted for by {@link #size}. No exception will be thrown when these bit
 * indices are manipulated or read, and in the aggregating functions
 * {@link #population()}, {@link #hashCode()}, etc., any hanging bits will have
 * their effect on those functions made consistent by {@link #cleanLastWord()}.
 * <p>
 * Otherwise, accessing a negative index, or any index greater than or equal to
 * {@link #size} will cause an {@link IndexOutOfBoundsException} to be thrown.
 * 
 * @author Aaron Shouldis
 * @see ConcurrentBitSet
 */
public class BitSet {

	/**
	 * Mask used to compute potentially faster modulo operations. {@code n % m} is
	 * equivalent to {@code n & (m -1)} if n is positive, and m = 2<sup>k</sup>.
	 */
	protected static final int MOD_SIZE_MASK = Long.SIZE - 1;

	/**
	 * log<sub>2</sub>64. Used to relate bit indices to word indices through
	 * bit-shifting as an alternative to division or multiplication by 64.
	 */
	protected static final int LOG_2_SIZE = 6;

	/**
	 * Long mask with all bits set. Used to isolate portions of words.
	 * (0xFFFFFFFFFFFFFFFF)
	 */
	protected static final long MASK = -1L;

	/**
	 * The number of indices accessible by this {@link BitSet}. Indices <b>0</b>
	 * through <b>size -1</b> are accessible.
	 */
	public final int size;

	/**
	 * Array holding the long words whose bits are manipulated. Has length
	 * ceiling({@link #size} / 64).
	 */
	protected final long[] words;

	/**
	 * Creates a {@link BitSet} with the specified number of bits indices. Indices 0
	 * through <b>size</b> -1 will be accessible. All bits are initially in the
	 * <i>dead</i> state.
	 * 
	 * @param size the number of bit indices that this {@link BitSet} will hold.
	 * @throws IllegalArgumentException if <b>size</b> is less than 0.
	 */
	public BitSet(final int size) {
		int wordCount = divideSize(this.size = size);
		if (wordCount >= 0 && modSize(size) > 0) {
			wordCount++;
		}
		if (wordCount < 0) {
			throw new IllegalArgumentException(Integer.toString(size));
		}
		words = new long[wordCount];
	}

	/**
	 * Creates a {@link BitSet} which is a clone of the specified {@link BitSet}
	 * <b>set</b>. The copy will have an identical {@link #size}, and will copy the
	 * contents of <b>set</b>'s {@link #words} through {@link #copy(BitSet)}.
	 * 
	 * @param set the {@link BitSet} to copy.
	 * @throws NullPointerException if <b>set</b> is null.
	 */
	public BitSet(final BitSet set) {
		this(set.size);
		copy(set);
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
	public boolean add(final int index) {
		final int wordIndex = divideSize(index);
		final long mask = bitMask(index);
		if ((words[wordIndex] & mask) != 0L) {
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
	public boolean remove(final int index) {
		final int wordIndex = divideSize(index);
		final long mask = bitMask(index);
		if ((words[wordIndex] & mask) == 0L) {
			return false;
		}
		words[wordIndex] &= ~mask;
		return true;
	}

	/**
	 * Checks the current state of the bit at the specified <b>index</b>. Returns
	 * {@code true} if the bit is in the <i>live</i> state, and {@code false} if it
	 * is not.
	 * 
	 * @param index the index of the bit to examine.
	 * @return whether the bit at the specified <b>index</b> is in the <i>live</i>
	 *         state.
	 * @throws ArrayIndexOutOfBoundsException if <b>index</b> is negative or greater
	 *                                        than or equal to {@link #size}.
	 */
	public final boolean get(final int index) {
		return (words[divideSize(index)] & bitMask(index)) != 0L;
	}

	/**
	 * Calculates the number of <i>live</i> bits in the specified range.
	 * 
	 * @param from (inclusive) the index of the first bit to be checked.
	 * @param to   (exclusive) the end of the range of bits to be checked.
	 * @return the number of <i>live</i> bits inside the specified range.
	 * @throws ArrayIndexOutOfBoundsException if <b>from</b> or <b>to</b> are
	 *                                        outside of the range 0 to
	 *                                        {@link #size},
	 */
	public final int get(final int from, final int to) {
		if (from >= to) {
			return 0;
		}
		final int start = divideSize(from);
		final int end = divideSize(to - 1);
		final long startMask = MASK << from;
		final long endMask = MASK >>> -to;
		int sum = 0;
		if (start == end) {
			sum += Long.bitCount(words[start] & startMask & endMask);
		} else {
			sum += Long.bitCount(words[start] & startMask);
			for (int i = start + 1; i < end; i++) {
				sum += Long.bitCount(words[i]);
			}
			sum += Long.bitCount(words[end] & endMask);
		}
		return sum;
	}

	/**
	 * Changes the state of a bit at the specified <b>index</b> to the <i>live</i>
	 * state.
	 * 
	 * @param index the index of the bit to change to the <i>live</i> state.
	 * @throws ArrayIndexOutOfBoundsException if <b>index</b> is negative, or
	 *                                        greater than or equal to
	 *                                        {@link #size}.
	 */
	public void set(final int index) {
		words[divideSize(index)] |= bitMask(index);
	}

	/**
	 * Changes the state of all bits in the specified range to the <i>live</i>
	 * state.
	 * 
	 * @param from (inclusive) the index of the first bit to be changed to the
	 *             <i>live</i> state.
	 * @param to   (exclusive) the end of the range of bits to be changed to the
	 *             <i>live</i> state.
	 * @throws ArrayIndexOutOfBoundsException if <b>from</b> or <b>to</b> are
	 *                                        outside of the range 0 to
	 *                                        {@link #size}.
	 */
	public void set(final int from, final int to) {
		if (from >= to) {
			return;
		}
		final int start = divideSize(from);
		final int end = divideSize(to - 1);
		final long startMask = MASK << from;
		final long endMask = MASK >>> -to;
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
	public void clear(final int index) {
		words[divideSize(index)] &= ~bitMask(index);
	}

	/**
	 * Changes the state of all bits in the specified range to the <i>dead</i>
	 * state.
	 * 
	 * @param from (inclusive) the index of the first bit to be cleared.
	 * @param to   (exclusive) the end of the range of bits to be cleared.
	 * @throws ArrayIndexOutOfBoundsException if <b>from</b> or <b>to</b> are
	 *                                        outside of the range 0 to
	 *                                        {@link #size}.
	 */
	public void clear(final int from, final int to) {
		if (from >= to) {
			return;
		}
		final int start = divideSize(from);
		final int end = divideSize(to - 1);
		final long startMask = MASK << from;
		final long endMask = MASK >>> -to;
		if (start == end) {
			words[start] &= ~(startMask & endMask);
		} else {
			words[start] &= ~startMask;
			for (int i = start + 1; i < end; i++) {
				words[i] = 0L;
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
	public void toggle(final int index) {
		words[divideSize(index)] ^= bitMask(index);
	}

	/**
	 * Changes the state of all bits in the specified range to their respective
	 * opposites through an {@code XOR} operation.
	 * 
	 * @param from (inclusive) the index of the first bit to be toggled.
	 * @param to   (exclusive) the end of the range of bits to be toggled.
	 * @throws ArrayIndexOutOfBoundsException if <b>from</b> or <b>to</b> are
	 *                                        outside of the range 0 to
	 *                                        {@link #size}.
	 */
	public void toggle(final int from, final int to) {
		if (from >= to) {
			return;
		}
		final int start = divideSize(from);
		final int end = divideSize(to - 1);
		final long startMask = MASK << from;
		final long endMask = MASK >>> -to;
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
	 * Returns the raw long word at the specified <b>wordIndex</b> within
	 * {@link #words}.
	 * 
	 * @param wordIndex the index within {@link #words} to read.
	 * @return the raw contents of {@link #words} at the specified <b>wordIndex</b>.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range 0 to ceiling({@link #size} /
	 *                                        64).
	 */
	public final long getWord(final int wordIndex) {
		return words[wordIndex];
	}

	/**
	 * Modifies the raw long word at the specified <b>wordIndex</b> within
	 * {@link #words}, setting it to <b>word</b>.
	 * 
	 * @param wordIndex the index within {@link #words} to set.
	 * @param word      the raw long value to be set to {@link #words} at
	 *                  <b>wordIndex</b>.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range 0 to ceiling({@link #size} /
	 *                                        64).
	 */
	public void setWord(final int wordIndex, final long word) {
		words[wordIndex] = word;
	}

	/**
	 * Changes the state of all bits in the specified range randomly according to
	 * the density of the {@link XOrShift} <b>random</b>. In contrast to
	 * {@link #xOrRandomize(XOrShift)} and
	 * {@link #xOrRandomize(XOrShift, int, int)}, this method preserves that
	 * density. When using a uniformly distributed {@link XOrShift} (as opposed to
	 * {@link DensityXOrShift}), this is unnecessary.
	 * 
	 * @param random the instance of {@link XOrShift} used to randomize.
	 * @param from   (inclusive) the index of the first bit to be randomized.
	 * @param to     (exclusive) the end of the range of bits to be randomized.
	 * @throws ArrayIndexOutOfBoundsException if <b>from</b> or <b>to</b> are
	 *                                        outside of the range 0 to
	 *                                        {@link #size}.
	 * @throws NullPointerException           if <b>random</b> is null.
	 * @see DensityXOrShift
	 */
	public void randomize(final XOrShift random, final int from, final int to) {
		if (from >= to) {
			return;
		}
		final int start = divideSize(from);
		final int end = divideSize(to - 1);
		final long startMask = MASK << from;
		final long endMask = MASK >>> -to;
		if (start == end) {
			long combinedMask = startMask & endMask;
			words[start] = (random.nextLong() & combinedMask) | (words[start] & ~combinedMask);
		} else {
			words[start] = (random.nextLong() & startMask) | (words[start] & ~startMask);
			for (int i = start + 1; i < end; i++) {
				words[i] = random.nextLong();
			}
			words[end] = (random.nextLong() & endMask) | (words[end] & ~endMask);
		}
	}

	/**
	 * Changes the state of all bits randomly according to the density of the
	 * {@link XOrShift} <b>random</b>. In contrast to
	 * {@link #xOrRandomize(XOrShift)} and
	 * {@link #xOrRandomize(XOrShift, int, int)}, this method preserves that
	 * density, at a small cost to performance. When using a uniformly distributed
	 * {@link XOrShift} (as opposed to {@link DensityXOrShift}), this is
	 * unnecessary.
	 * 
	 * @param random the instance of {@link XOrShift} used to randomize.
	 * @throws NullPointerException if <b>random</b> is null.
	 * @see DensityXOrShift
	 */
	public void randomize(final XOrShift random) {
		for (int i = 0; i < words.length; i++) {
			words[i] = random.nextLong();
		}
	}

	/**
	 * Performs an {@code XOR} operation on all bits in the specified range against
	 * random bits generated by the specified {@link XOrShift} <b>random</b>. When
	 * using a non-uniformly distributed {@link XOrShift}, namely
	 * {@link DensityXOrShift}, the resulting density of this {@link BitSet} can be
	 * estimated with the function: <br>
	 * {@code density(a) + density(b) - (2 * density(a) * density(b))}
	 * 
	 * @param random the instance of {@link XOrShift} used to randomize.
	 * @param from   (inclusive) the index of the first bit to be randomized.
	 * @param to     (exclusive) the end of the range of bits to be randomized.
	 * @throws ArrayIndexOutOfBoundsException if <b>from</b> or <b>to</b> are
	 *                                        outside of the range 0 to
	 *                                        {@link #size}.
	 * @throws NullPointerException           if <b>random</b> is null.
	 * @see DensityXOrShift
	 */
	public void xOrRandomize(final XOrShift random, final int from, final int to) {
		if (from >= to) {
			return;
		}
		final int start = divideSize(from);
		final int end = divideSize(to - 1);
		final long startMask = MASK << from;
		final long endMask = MASK >>> -to;
		if (start == end) {
			words[start] ^= random.nextLong() & startMask & endMask;
		} else {
			words[start] ^= random.nextLong() & startMask;
			for (int i = start + 1; i < end; i++) {
				words[i] ^= random.nextLong();
			}
			words[end] ^= random.nextLong() & endMask;
		}
	}

	/**
	 * Performs an {@code XOR} operation on all bits against random bits generated
	 * by the specified {@link XOrShift} <b>random</b>. When using a non-uniformly
	 * distributed {@link XOrShift}, namely {@link DensityXOrShift}, the resulting
	 * density of this {@link BitSet} can be estimated with the function: <br>
	 * {@code density(a) + density(b) - (2 * density(a) * density(b))}
	 * 
	 * @param random the instance of {@link XOrShift} used to randomize.
	 * @throws NullPointerException if <b>random</b> is null.
	 * @see DensityXOrShift
	 */
	public void xOrRandomize(final XOrShift random) {
		for (int i = 0; i < words.length; i++) {
			words[i] ^= random.nextLong();
		}
	}

	/**
	 * Calculates the index of the next <i>live</i> bit after the specified
	 * <b>index</b>, including that <b>index</b>. All bits from that specified
	 * <b>index</b>, until {@link #size} will be checked. If no <i>live</i> bits are
	 * found, -1 is returned.
	 * 
	 * @param index (inclusive) the first index to check.
	 * @return the index of the next <i>live</i> bit, or -1 if none were found.
	 */
	public final int nextLive(final int index) {
		int wordIndex = divideSize(index);
		if (wordIndex >= words.length || wordIndex < 0) {
			return -1;
		}
		long word = words[wordIndex] & (MASK << index);
		if (word != 0L) {
			return nextLiveBit(word, wordIndex);
		}
		while (++wordIndex < words.length) {
			word = words[wordIndex];
			if (word != 0L) {
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
		int wordIndex = divideSize(index);
		if (wordIndex >= words.length || wordIndex < 0) {
			return -1;
		}
		long word = ~words[wordIndex] & (MASK << index);
		if (word != 0L) {
			return nextLiveBit(word, wordIndex);
		}
		while (++wordIndex < words.length) {
			word = ~words[wordIndex];
			if (word != 0L) {
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
	public final int lastLive(final int index) {
		int wordIndex = divideSize(index);
		if (wordIndex >= words.length || wordIndex < 0) {
			return -1;
		}
		long word = words[wordIndex] & (MASK >>> -(index + 1));
		if (word != 0L) {
			return lastLiveBit(word, wordIndex);
		}
		while (wordIndex-- > 0) {
			word = words[wordIndex];
			if (word != 0L) {
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
	public final int lastDead(final int index) {
		int wordIndex = divideSize(index);
		if (wordIndex >= words.length || wordIndex < 0) {
			return -1;
		}
		long word = ~words[wordIndex] & (MASK >>> -(index + 1));
		if (word != 0L) {
			return lastLiveBit(word, wordIndex);
		}
		while (wordIndex-- > 0) {
			word = ~words[wordIndex];
			if (word != 0L) {
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
	public void or(final BitSet set) {
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
	public void xor(final BitSet set) {
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
	public void and(final BitSet set) {
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
	public void not(final BitSet set) {
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
	public final void copy(final BitSet set) {
		compareSize(set);
		System.arraycopy(set.words, 0, words, 0, words.length);
	}

	/**
	 * Transforms each bit in this {@link BitSet} to the <i>live</i> state.
	 */
	public final void fill() {
		for (int i = 0; i < words.length; i++) {
			setWord(i, MASK);
		}
	}

	/**
	 * Transforms each bit in this {@link BitSet} to the <i>dead</i> state.
	 */
	public final void empty() {
		for (int i = 0; i < words.length; i++) {
			setWord(i, 0L);
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
			population += Long.bitCount(words[i]);
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
	public final double density(final int from, final int to) {
		if (from >= to) {
			return 0.0;
		}
		return get(from, to) / (double) (to - from);
	}

	/**
	 * Calculates what percentage of bits in this {@link BitSet} are in the
	 * <i>live</i> state.
	 *
	 * @return the percentage of <i>live</i> bits.
	 */
	public final double density() {
		return population() / (double) size;
	}

	/**
	 * Calculates a {@code long} identifier number generated from
	 * {@link #hashCode()} and {@link #population()}.
	 * 
	 * @return the unique identifying code.
	 */
	public final long identifier() {
		return ((long) hashCode() << Long.SIZE) + population();
	}

	/**
	 * Changes the state of any "hanging bits" to the <i>dead</i> state in order to
	 * maintain their effect on aggregating functions ({@link #population()}, etc).
	 */
	protected void cleanLastWord() {
		final int hangingBits = modSize(-size);
		if (hangingBits > 0) {
			words[words.length - 1] &= (MASK >>> hangingBits);
		}
	}

	/**
	 * Calculates a mask to represent the bit at which a specific index will be
	 * stored within a long word.
	 * 
	 * @param index the index to represent as a bit.
	 * @return the bit that represents the position of an index within a word.
	 */
	protected static final long bitMask(final int index) {
		return 1L << index;
	}

	/**
	 * Equivalent to {@code index % 64} for positive numbers, and modulo of positive
	 * numbers faster.
	 * 
	 * @param index the index to perform the modulo operation upon.
	 * @return the result of the modulo operation.
	 * @see #MOD_SIZE_MASK
	 */
	protected static final int modSize(final int index) {
		return index & MOD_SIZE_MASK;
	}

	/**
	 * Calculates <b>index</b> divided by 64. Equivalent to the index of the word
	 * corresponding to the specified <b>index</b>.
	 * 
	 * @param index the index to divide by 64.
	 * @return <b>wordIndex</b> / 64.
	 */
	protected static final int divideSize(final int index) {
		return index >> LOG_2_SIZE;
	}

	/**
	 * Calculates <b>wordIndex</b> multiplied by 64. Equivalent to the first index
	 * of the word at the specified <b>wordIndex</b>.
	 * 
	 * @param wordIndex the index to multiply by 64.
	 * @return <b>wordIndex</b> * 64.
	 */
	protected static final int multiplySize(final int wordIndex) {
		return wordIndex << LOG_2_SIZE;
	}

	/**
	 * Calculates the index of the next <i>live</i> bit within a specified
	 * <b>word</b> that is at the specified <b>wordIndex</b> within {@link #words}.
	 * This index will represent its bit index in the underlying long array as well
	 * as the offset within the long <b>word</b>.
	 * 
	 * @param word      the long word to be checked for a <i>live</i> bit.
	 * @param wordIndex the index of the word within {@link #words}.
	 * @return the index of the next <i>live</i> bit within the specified word, or
	 *         -1 if none is found.
	 */
	private int nextLiveBit(final long word, final int wordIndex) {
		final int index = multiplySize(wordIndex) + Long.numberOfTrailingZeros(word);
		return index < size ? index : -1;
	}

	/**
	 * Calculates the index of the recent-most <i>live</i> bit within a specified
	 * <b>word</b> that is at the specified <b>wordIndex</b> within {@link #words}.
	 * This index will represent its bit index in the underlying long array as well
	 * as the offset within the long <b>word</b>.
	 * 
	 * @param word      the long word to be checked for a <i>live</i> bit.
	 * @param wordIndex the index of the word within {@link #words}.
	 * @return the index of the recent-most <i>live</i> bit within the specified
	 *         word.
	 */
	private int lastLiveBit(final long word, final int wordIndex) {
		final int index = ((wordIndex + 1) << LOG_2_SIZE) - Long.numberOfLeadingZeros(word) - 1;
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
	protected final void compareSize(final BitSet set) {
		if (set.size != size) {
			throw new ArrayIndexOutOfBoundsException(Math.min(set.words.length, words.length) - 1);
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Size: [").append(size).append("] Population: [").append(population()).append(']');
		return builder.toString();
	}

	@Override
	public int hashCode() {
		cleanLastWord();
		return size ^ Arrays.hashCode(words);
	}

	@Override
	public boolean equals(final Object obj) {
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