package com.shouldis.bitset;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.IntStream;

import com.shouldis.bitset.parallel.DeadBiterator;
import com.shouldis.bitset.parallel.LiveBiterator;

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
 * {@link com.shouldis.bitset.parallel.Biterator} may be used to stream indices
 * in an order and grouping appropriate for parallel manipulation of bits.
 * Alternatively, a {@link ConcurrentBitSet} may be used to make all operations
 * thread-safe, requiring no external synchronization at the cost of
 * performance.
 * <p>
 * If {@link #size} isn't a multiple of 64, there will be hanging bits that
 * exist on the end of the last long within {@link #words}, which are not
 * accounted for by {@link #size}. No exception will be thrown when these bit
 * indices are manipulated or read, and in the aggregating functions
 * {@link #population()}, {@link #hashCode()}, etc., any hanging bits will have
 * their effect on those functions made consistent by {@link #cleanLastWord()}.
 * Otherwise, accessing a negative index, or any index greater than or equal to
 * {@link #size} will cause an {@link IndexOutOfBoundsException} to be thrown.
 * 
 * @author Aaron Shouldis
 * @see ConcurrentBitSet
 */
public class BitSet implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Long mask with all bits in the <i>live</i> state. (0xFFFFFFFFFFFFFFFF)
	 */
	public static final long MASK = -1L;

	/**
	 * Mask used to compute potentially faster modulo operations. {@code n % m} is
	 * equivalent to {@code n & (m -1)} if n is positive, and m = 2<sup>k</sup>.
	 */
	private static final int MOD_SIZE_MASK = Long.SIZE - 1;

	/**
	 * log<sub>2</sub>64. Used to relate bit indices to word indices through
	 * bit-shifting as an alternative to division or multiplication by 64.
	 */
	private static final int LOG_2_SIZE = 6;

	/**
	 * The number of indices accessible by this {@link BitSet}. Indices <b>0</b>
	 * through <b>size -1</b> are accessible.
	 */
	public final int size;

	/**
	 * The number of long words contained by this {@link BitSet}. Equal to
	 * ceiling({@link #size} / 64) and {@link #words}.length.
	 */
	public final int wordCount;

	/**
	 * Array holding the long words whose bits are manipulated. Has length
	 * ceiling({@link #size} / 64). Though this has protected visibility, using
	 * methods such as {@link #getWord(int)}, {@link #setWord(int, long)},
	 * {@link #andWord(int, long)} is preferred over direct access.
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
		this.size = size;
		int wordCount = divideSize(size);
		if (wordCount >= 0 && modSize(size) > 0) {
			wordCount++;
		}
		if (wordCount < 0) {
			throw new IllegalArgumentException(Integer.toString(size));
		}
		this.wordCount = wordCount;
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
		if ((getWord(wordIndex) & mask) != 0L) {
			return false;
		}
		orWord(wordIndex, mask);
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
		if ((getWord(wordIndex) & mask) == 0L) {
			return false;
		}
		andWord(wordIndex, ~mask);
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
		return (getWord(divideSize(index)) & bitMask(index)) != 0L;
	}

	/**
	 * Calculates the number of <i>live</i> bits in the specified range.
	 * 
	 * @param from (inclusive) the index of the first bit to be checked.
	 * @param to   (exclusive) the end of the range of bits to be checked.
	 * @return the number of <i>live</i> bits inside the specified range, or 0 if
	 *         <b>from</b> is greater than or equal to <b>to</b>.
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
			sum += Long.bitCount(getWord(start) & startMask & endMask);
		} else {
			sum += Long.bitCount(getWord(start) & startMask);
			for (int i = start + 1; i < end; i++) {
				sum += Long.bitCount(getWord(i));
			}
			sum += Long.bitCount(getWord(end) & endMask);
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
	public final void set(final int index) {
		orWord(divideSize(index), bitMask(index));
	}

	/**
	 * Changes the state of all bits in the specified range to the <i>live</i>
	 * state. No action is taken if <b>from</b> is greater than or equal to
	 * <b>to</b>.
	 * 
	 * @param from (inclusive) the index of the first bit to be changed to the
	 *             <i>live</i> state.
	 * @param to   (exclusive) the end of the range of bits to be changed to the
	 *             <i>live</i> state.
	 * @throws ArrayIndexOutOfBoundsException if <b>from</b> or <b>to</b> are
	 *                                        outside of the range 0 to
	 *                                        {@link #size}.
	 */
	public final void set(final int from, final int to) {
		if (from >= to) {
			return;
		}
		final int start = divideSize(from);
		final int end = divideSize(to - 1);
		final long startMask = MASK << from;
		final long endMask = MASK >>> -to;
		if (start == end) {
			orWord(start, startMask & endMask);
		} else {
			orWord(start, startMask);
			for (int i = start + 1; i < end; i++) {
				fillWord(i);
			}
			orWord(end, endMask);
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
	public final void clear(final int index) {
		andWord(divideSize(index), ~bitMask(index));
	}

	/**
	 * Changes the state of all bits in the specified range to the <i>dead</i>
	 * state. No action is taken if <b>from</b> is greater than or equal to
	 * <b>to</b>.
	 * 
	 * @param from (inclusive) the index of the first bit to be cleared.
	 * @param to   (exclusive) the end of the range of bits to be cleared.
	 * @throws ArrayIndexOutOfBoundsException if <b>from</b> or <b>to</b> are
	 *                                        outside of the range 0 to
	 *                                        {@link #size}.
	 */
	public final void clear(final int from, final int to) {
		if (from >= to) {
			return;
		}
		final int start = divideSize(from);
		final int end = divideSize(to - 1);
		final long startMask = MASK << from;
		final long endMask = MASK >>> -to;
		if (start == end) {
			andWord(start, ~(startMask & endMask));
		} else {
			andWord(start, ~startMask);
			for (int i = start + 1; i < end; i++) {
				emptyWord(i);
			}
			andWord(end, ~endMask);
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
	public final void toggle(final int index) {
		xOrWord(divideSize(index), bitMask(index));
	}

	/**
	 * Changes the state of all bits in the specified range to their respective
	 * opposites through an {@code XOR} operation. No action is taken if <b>from</b>
	 * is greater than or equal to <b>to</b>.
	 * 
	 * @param from (inclusive) the index of the first bit to be toggled.
	 * @param to   (exclusive) the end of the range of bits to be toggled.
	 * @throws ArrayIndexOutOfBoundsException if <b>from</b> or <b>to</b> are
	 *                                        outside of the range 0 to
	 *                                        {@link #size}.
	 */
	public final void toggle(final int from, final int to) {
		if (from >= to) {
			return;
		}
		final int start = divideSize(from);
		final int end = divideSize(to - 1);
		final long startMask = MASK << from;
		final long endMask = MASK >>> -to;
		if (start == end) {
			xOrWord(start, startMask & endMask);
		} else {
			xOrWord(start, startMask);
			for (int i = start + 1; i < end; i++) {
				toggleWord(i);
			}
			xOrWord(end, endMask);
		}
	}

	/**
	 * Returns the long word at the specified <b>wordIndex</b> within
	 * {@link #words}.
	 * 
	 * @param wordIndex the index within {@link #words} to read.
	 * @return the raw contents of {@link #words} at the specified <b>wordIndex</b>.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range 0 to ceiling({@link #size} /
	 *                                        64).
	 */
	public long getWord(final int wordIndex) {
		return words[wordIndex];
	}

	/**
	 * Changes the long word at the specified <b>wordIndex</b> within
	 * {@link #words}, setting it to <b>word</b>.
	 * 
	 * @param wordIndex the index within {@link #words} to set.
	 * @param word      the long value to be set to {@link #words} at
	 *                  <b>wordIndex</b>.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range 0 to ceiling({@link #size} /
	 *                                        64).
	 */
	public void setWord(final int wordIndex, final long word) {
		words[wordIndex] = word;
	}

	/**
	 * Changes the long word at <b>wordIndex</b> within {@link #words} to the result
	 * of an {@code AND} operation between the current value at the specified
	 * <b>wordIndex</b> within {@link #words} and the specified <b>mask</b>. <br>
	 * {@code words[wordIndex] &= mask;}
	 * 
	 * @param wordIndex the index within {@link #words} to perform the {@code AND}
	 *                  operation upon.
	 * @param mask      the mask to use in the {@code AND} operation on the current
	 *                  value at the specified <b>wordIndex</b>.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range 0 to ceiling({@link #size} /
	 *                                        64).
	 */
	public void andWord(final int wordIndex, final long mask) {
		words[wordIndex] &= mask;
	}

	/**
	 * Changes the long word at <b>wordIndex</b> within {@link #words} to the result
	 * of an {@code OR} operation between the current value at the specified
	 * <b>wordIndex</b> within {@link #words} and the specified <b>mask</b>. <br>
	 * {@code words[wordIndex] |= mask;}
	 * 
	 * @param wordIndex the index within {@link #words} to perform the {@code OR}
	 *                  operation upon.
	 * @param mask      the mask to use in the {@code OR} operation on the current
	 *                  value at the specified <b>wordIndex</b>.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range 0 to ceiling({@link #size} /
	 *                                        64).
	 */
	public void orWord(final int wordIndex, final long mask) {
		words[wordIndex] |= mask;
	}

	/**
	 * Changes the long word at <b>wordIndex</b> within {@link #words} to the result
	 * of an {@code XOR} operation between the current value at the specified
	 * <b>wordIndex</b> within {@link #words} and the specified <b>mask</b>. <br>
	 * {@code words[wordIndex] ^= mask;}
	 * 
	 * @param wordIndex the index within {@link #words} to perform the {@code XOR}
	 *                  operation upon.
	 * @param mask      the mask to use in the {@code XOR} operation on the current
	 *                  value at the specified <b>wordIndex</b>.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range 0 to ceiling({@link #size} /
	 *                                        64).
	 */
	public void xOrWord(final int wordIndex, final long mask) {
		words[wordIndex] ^= mask;
	}

	/**
	 * Changes the long word at <b>wordIndex</b> within {@link #words} to the result
	 * of a {@code NOT AND} operation between the current value at the specified
	 * <b>wordIndex</b> within {@link #words} and the specified <b>mask</b>.
	 * 
	 * @param wordIndex the index within {@link #words} to perform the
	 *                  {@code NOT AND} operation upon.
	 * @param mask      the mask to use in the {@code NOT AND} operation on the
	 *                  current value at the specified <b>wordIndex</b>.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range 0 to ceiling({@link #size} /
	 *                                        64).
	 */
	public void notAndWord(final int wordIndex, final long mask) {
		words[wordIndex] = ~(words[wordIndex] & mask);
	}

	/**
	 * Changes the long word at <b>wordIndex</b> within {@link #words} to the result
	 * of a {@code NOT OR} operation between the current value at the specified
	 * <b>wordIndex</b> within {@link #words} and the specified <b>mask</b>.
	 * 
	 * @param wordIndex the index within {@link #words} to perform the
	 *                  {@code NOT OR} operation upon.
	 * @param mask      the mask to use in the {@code NOT OR} operation on the
	 *                  current value at the specified <b>wordIndex</b>.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range 0 to ceiling({@link #size} /
	 *                                        64).
	 */
	public void notOrWord(final int wordIndex, final long mask) {
		words[wordIndex] = ~(words[wordIndex] | mask);
	}

	/**
	 * Changes the long word at <b>wordIndex</b> within {@link #words} to the result
	 * of a {@code NOT XOR} operation between the current value at the specified
	 * <b>wordIndex</b> within {@link #words} and the specified <b>mask</b>.
	 * 
	 * @param wordIndex the index within {@link #words} to perform the
	 *                  {@code NOT XOR} operation upon.
	 * @param mask      the mask to use in the {@code NOT XOR} operation on the
	 *                  current value at the specified <b>wordIndex</b>.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range 0 to ceiling({@link #size} /
	 *                                        64).
	 */
	public void notXOrWord(final int wordIndex, final long mask) {
		words[wordIndex] = ~(words[wordIndex] ^ mask);
	}

	/**
	 * Changes the long word at the specified <b>wordIndex</b> within {@link #words}
	 * such that it retains the previous state of bits where the bits of <b>mask</b>
	 * are in the <i>dead</i> state, and takes on the value of bits in <b>word</b>
	 * where <b>mask</b> has bits in the <i>live</i> state.
	 * 
	 * @param wordIndex the index within {@link #words} to change.
	 * @param word      the new long value to be set to {@link #words}.
	 * @param mask      the mask used to determine which bits from <b>word</b> will
	 *                  be applied.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range 0 to ceiling({@link #size} /
	 *                                        64).
	 */
	public void setWordSegment(final int wordIndex, final long word, final long mask) {
		setWord(wordIndex, (mask & word) | (~mask & getWord(wordIndex)));
	}

	/**
	 * Changes the long word at <b>wordIndex</b> within {@link #words} to the
	 * complement of its current state.
	 * 
	 * @param wordIndex the index within {@link #words} to perform the toggle
	 *                  operation upon.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range 0 to ceiling({@link #size} /
	 *                                        64).
	 */
	public final void toggleWord(final int wordIndex) {
		xOrWord(wordIndex, MASK);
	}

	/**
	 * Changes the long word at <b>wordIndex</b> within {@link #words} to
	 * {@link #MASK}, setting all bits to the <i>live</i> state.
	 * 
	 * @param wordIndex the index within {@link #words} to perform the fill
	 *                  operation upon.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range 0 to ceiling({@link #size} /
	 *                                        64).
	 */
	public final void fillWord(final int wordIndex) {
		setWord(wordIndex, MASK);
	}

	/**
	 * Changes the long word at <b>wordIndex</b> within {@link #words} to 0,
	 * removing all bits in the <i>live</i> state.
	 * 
	 * @param wordIndex the index within {@link #words} to perform the empty
	 *                  operation upon.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range 0 to ceiling({@link #size} /
	 *                                        64).
	 */
	public final void emptyWord(final int wordIndex) {
		setWord(wordIndex, 0L);
	}

	/**
	 * Applies the specified {@link WordFunction} <b>function</b> to the word at the
	 * specified <b>wordIndex</b>.
	 * 
	 * @param wordIndex the index within {@link #words} to apply the function to.
	 * @param function  the {@link WordFunction} to apply at the specified
	 *                  <b>wordIndex</b>.
	 */
	public void applyFunction(final int wordIndex, final WordFunction function) {
		setWord(wordIndex, function.apply(getWord(wordIndex)));
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
		if (wordIndex >= wordCount || wordIndex < 0) {
			return -1;
		}
		long word = getWord(wordIndex) & (MASK << index);
		if (word != 0L) {
			return nextLiveBit(word, wordIndex);
		}
		while (++wordIndex < wordCount) {
			word = getWord(wordIndex);
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
	public final int nextDead(final int index) {
		int wordIndex = divideSize(index);
		if (wordIndex >= wordCount || wordIndex < 0) {
			return -1;
		}
		long word = ~getWord(wordIndex) & (MASK << index);
		if (word != 0L) {
			return nextLiveBit(word, wordIndex);
		}
		while (++wordIndex < wordCount) {
			word = ~getWord(wordIndex);
			if (word != 0L) {
				return nextLiveBit(word, wordIndex);
			}
		}
		return -1;
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
	private final int nextLiveBit(final long word, final int wordIndex) {
		final int index = multiplySize(wordIndex) + Long.numberOfTrailingZeros(word);
		return index < size ? index : -1;
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
		if (wordIndex >= wordCount || wordIndex < 0) {
			return -1;
		}
		long word = getWord(wordIndex) & (MASK >>> -(index + 1));
		if (word != 0L) {
			return lastLiveBit(word, wordIndex);
		}
		while (wordIndex-- > 0) {
			word = getWord(wordIndex);
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
		if (wordIndex >= wordCount || wordIndex < 0) {
			return -1;
		}
		long word = ~getWord(wordIndex) & (MASK >>> -(index + 1));
		if (word != 0L) {
			return lastLiveBit(word, wordIndex);
		}
		while (wordIndex-- > 0) {
			word = ~getWord(wordIndex);
			if (word != 0L) {
				return lastLiveBit(word, wordIndex);
			}
		}
		return -1;
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
	private final int lastLiveBit(final long word, final int wordIndex) {
		final int index = multiplySize(wordIndex + 1) - Long.numberOfLeadingZeros(word) - 1;
		return index < size ? index : -1;
	}

	/**
	 * Creates a parallel-safe {@link IntStream} consisting of the indices of all
	 * <i>live</i> bits within this {@link BitSet} using {@link LiveBiterator}.
	 * 
	 * @return a parallel-safe {@link IntStream} representation of the bit indices
	 *         in the <i>live</i> state.
	 */
	public final IntStream live() {
		return new LiveBiterator(this).stream();
	}

	/**
	 * Creates a parallel-safe {@link IntStream} consisting of the indices of all
	 * <i>dead</i> bits within this {@link BitSet} using {@link DeadBiterator}.
	 * 
	 * @return a parallel-safe {@link IntStream} representation of the bit indices
	 *         in the <i>dead</i> state.
	 */
	public final IntStream dead() {
		return new DeadBiterator(this).stream();
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
	public final void and(final BitSet set) {
		compareSize(set);
		for (int i = 0; i < wordCount; i++) {
			andWord(i, set.getWord(i));
		}
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
	public final void or(final BitSet set) {
		compareSize(set);
		for (int i = 0; i < wordCount; i++) {
			orWord(i, set.getWord(i));
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
	public final void xOr(final BitSet set) {
		compareSize(set);
		for (int i = 0; i < wordCount; i++) {
			xOrWord(i, set.getWord(i));
		}
	}

	/**
	 * Performs a global {@code NOT AND} operation on all bits in this
	 * {@link BitSet} with those in the specified {@link BitSet} <b>set</b>.
	 * 
	 * @param set the other {@link BitSet} from which to perform the {@code NOT AND}
	 *            operation.
	 * @throws IllegalArgumentException if the {@link #size}s of both
	 *                                  {@link BitSet}s are not equal.
	 * @throws NullPointerException     if <b>set</b> is null.
	 */
	public final void notAnd(final BitSet set) {
		compareSize(set);
		for (int i = 0; i < wordCount; i++) {
			notAndWord(i, set.getWord(i));
		}
	}

	/**
	 * Performs a global {@code NOT OR} operation on all bits in this {@link BitSet}
	 * with those in the specified {@link BitSet} <b>set</b>.
	 * 
	 * @param set the other {@link BitSet} from which to perform the {@code NOT OR}
	 *            operation.
	 * @throws IllegalArgumentException if the {@link #size}s of both
	 *                                  {@link BitSet}s are not equal.
	 * @throws NullPointerException     if <b>set</b> is null.
	 */
	public final void notOr(final BitSet set) {
		compareSize(set);
		for (int i = 0; i < wordCount; i++) {
			notOrWord(i, set.getWord(i));
		}
	}

	/**
	 * Performs a global {@code NOT XOR} operation on all bits in this
	 * {@link BitSet} with those in the specified {@link BitSet} <b>set</b>.
	 * 
	 * @param set the other {@link BitSet} from which to perform the {@code NOT XOR}
	 *            operation.
	 * @throws IllegalArgumentException if the {@link #size}s of both
	 *                                  {@link BitSet}s are not equal.
	 * @throws NullPointerException     if <b>set</b> is null.
	 */
	public final void notXOr(final BitSet set) {
		compareSize(set);
		for (int i = 0; i < wordCount; i++) {
			notXOrWord(i, set.getWord(i));
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
	public final void not(final BitSet set) {
		compareSize(set);
		for (int i = 0; i < wordCount; i++) {
			setWord(i, ~set.getWord(i));
		}
	}

	/**
	 * Transforms each bit in this {@link BitSet} into the complement of its current
	 * state.
	 */
	public final void not() {
		for (int i = 0; i < wordCount; i++) {
			toggleWord(i);
		}
	}

	/**
	 * Transforms each bit in this {@link BitSet} to the <i>live</i> state.
	 */
	public final void fill() {
		for (int i = 0; i < wordCount; i++) {
			fillWord(i);
		}
	}

	/**
	 * Transforms each bit in this {@link BitSet} to the <i>dead</i> state.
	 */
	public final void empty() {
		for (int i = 0; i < wordCount; i++) {
			emptyWord(i);
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
		System.arraycopy(set.words, 0, words, 0, wordCount);
	}

	/**
	 * Changes the state of any hanging bits to the <i>dead</i> state in order to
	 * maintain their effect on aggregating functions ({@link #population()}, etc).
	 */
	protected final void cleanLastWord() {
		final int hanging = BitSet.modSize(-size);
		if (hanging > 0) {
			andWord(wordCount - 1, MASK >>> hanging);
		}
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
	public final void compareSize(final BitSet set) {
		if (set.size != size) {
			throw new ArrayIndexOutOfBoundsException(Math.min(set.wordCount, wordCount) - 1);
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
		for (int i = 0; i < wordCount; i++) {
			population += Long.bitCount(getWord(i));
		}
		return population;
	}

	/**
	 * Calculates what percentage of bits in this {@link BitSet} are in the
	 * <i>live</i> state in the specified range. No action is taken if <b>from</b>
	 * is greater than or equal to <b>to</b>.
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
	 * Calculates a {@code long} identifier number which acts as a larger hash code
	 * with fewer collisions, drawing from the hashCode and population.
	 * 
	 * @return the unique identifying code.
	 */
	public final long identifier() {
		long word, hash = size;
		int population = 0;
		cleanLastWord();
		for (int i = 0; i < wordCount; i++) {
			word = getWord(i);
			hash *= 31;
			hash += word;
			population += Long.bitCount(word);
		}
		return hash ^ population;
	}

	/**
	 * Calculates <b>index</b> divided by 64. Equivalent to the index of the word
	 * corresponding to the specified <b>index</b>.
	 * 
	 * @param index the index to divide by 64.
	 * @return <b>wordIndex</b> / 64.
	 */
	public static final int divideSize(final int index) {
		return index >> LOG_2_SIZE;
	}

	/**
	 * Calculates <b>wordIndex</b> multiplied by 64. Equivalent to the first index
	 * of the word at the specified <b>wordIndex</b>.
	 * 
	 * @param wordIndex the index to multiply by 64.
	 * @return <b>wordIndex</b> * 64.
	 */
	public static final int multiplySize(final int wordIndex) {
		return wordIndex << LOG_2_SIZE;
	}

	/**
	 * Equivalent to {@code index % 64} for positive numbers, and modulo of positive
	 * numbers faster.
	 * 
	 * @param index the index to perform the modulo operation upon.
	 * @return the result of the modulo operation.
	 * @see #MOD_SIZE_MASK
	 */
	public static final int modSize(final int index) {
		return index & MOD_SIZE_MASK;
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

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Size: [").append(size).append("] Population: [").append(population()).append(']');
		return builder.toString();
	}

	@Override
	public int hashCode() {
		long hash = size;
		cleanLastWord();
		for (int i = 0; i < wordCount; i++) {
			hash *= 31L;
			hash += getWord(i);
		}
		return (int) (hash ^ (hash >>> Integer.SIZE));
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		return obj instanceof BitSet && Arrays.equals(words, ((BitSet) obj).words);
	}

}