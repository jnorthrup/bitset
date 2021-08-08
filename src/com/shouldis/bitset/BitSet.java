package com.shouldis.bitset;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import com.shouldis.bitset.function.WordBiFunction;
import com.shouldis.bitset.function.WordFunction;

/**
 * Representation a fixed number of bits stored within an internal long array,
 * mapping positive integer indices to individual bits and facilitating the
 * manipulation of those individual bits or ranges of bits, as well as
 * retrieving their boolean values.
 * <p>
 * The bits represented by this {@link BitSet} will either be in the <i>live</i>
 * state (<code>1, true</code>), or the <i>dead</i> state
 * (<code>0, false</code>).
 * <p>
 * {@link BitSet} on its own is thread-safe only for read operations, although a
 * {@link com.shouldis.bitset.parallel.Biterator} may be used to stream indices
 * in an order and grouping appropriate for parallel manipulation of bits.
 * Alternatively, a {@link ConcurrentBitSet} may be used to make all operations
 * thread-safe, requiring no external synchronization at the cost of
 * performance.
 * <p>
 * While this implementation of {@link BitSet} is made extensible, flexible, and
 * readable by how verbose it is made by functional decomposition -- this
 * impacts performance because of the overhead created by the call stack. When
 * no extra functionality or modified behavior is needed, {@link InlineBitSet}
 * performs the same operations, but "hot" methods are made faster by inlining.
 * <p>
 * If {@link #size} isn't a multiple of 64, there will be hanging bits that
 * exist on the end of the last long within {@link #words}, which are not
 * accounted for by {@link #size}. No exception will be thrown when these bit
 * indices are manipulated or read, and in the aggregating functions
 * {@link #population()}, {@link #hashCode()}, etc., hanging bits can have their
 * effect on those aggregating functions made consistent by calling
 * {@link #clearHanging()}. Otherwise, accessing a negative index, or any index
 * greater than or equal to {@link #size} will cause an
 * {@link IndexOutOfBoundsException} to be thrown.
 * 
 * @author Aaron Shouldis
 * @see ConcurrentBitSet
 */
public class BitSet implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Long mask with all bits in the <i>live</i> state. Used for readability in
	 * place of {@code -1L} (0xFFFFFFFFFFFFFFFF).
	 */
	public static final long LIVE = -1L;

	/**
	 * Long mask with all bits in the <i>dead</i> state. Used for readability in
	 * place of {@code 0L} (0x0000000000000000).
	 */
	public static final long DEAD = 0L;

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
		int wordCount = BitSet.divideSize(size);
		if (wordCount < 0) {
			throw new IllegalArgumentException(Integer.toString(size));
		}
		if (BitSet.modSize(size) > 0) {
			wordCount++;
		}
		this.size = size;
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
	public boolean get(final int index) {
		return (getWord(BitSet.divideSize(index)) & BitSet.bitMask(index)) != DEAD;
	}

	/**
	 * Calculates the number of <i>live</i> bits in the specified range
	 * [<b>from</b>, <b>to</b>).
	 * 
	 * @param from (inclusive) the index of the first bit to be checked.
	 * @param to   (exclusive) the end of the range of bits to be checked.
	 * @return the number of <i>live</i> bits inside the specified range, or 0 if
	 *         <b>from</b> is greater than or equal to <b>to</b>.
	 * @throws ArrayIndexOutOfBoundsException if <b>from</b> or <b>to</b> are
	 *                                        outside of the range [0,
	 *                                        {@link #size}).
	 */
	public int get(final int from, final int to) {
		Objects.checkFromToIndex(from, to, size);
		final int start = BitSet.divideSize(from);
		final int end = BitSet.divideSize(to - 1);
		final long startMask = LIVE << from;
		final long endMask = LIVE >>> -to;
		int sum;
		if (start == end) {
			sum = Long.bitCount(getWord(start) & startMask & endMask);
		} else {
			sum = Long.bitCount(getWord(start) & startMask);
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
	public void set(final int index) {
		orWord(BitSet.divideSize(index), BitSet.bitMask(index));
	}

	/**
	 * Changes the state of all bits in the specified range [<b>from</b>, <b>to</b>)
	 * to the <i>live</i> state. No action is taken if <b>from</b> is greater than
	 * or equal to <b>to</b>. {@link ConcurrentBitSet} will only perform this
	 * atomically on each word within the range individually.
	 * 
	 * @param from (inclusive) the index of the first bit to be changed to the
	 *             <i>live</i> state.
	 * @param to   (exclusive) the end of the range of bits to be changed to the
	 *             <i>live</i> state.
	 * @throws ArrayIndexOutOfBoundsException if <b>from</b> or <b>to</b> are
	 *                                        outside of the range [0,
	 *                                        {@link #size}).
	 */
	public void set(final int from, final int to) {
		Objects.checkFromToIndex(from, to, size);
		final int start = BitSet.divideSize(from);
		final int end = BitSet.divideSize(to - 1);
		final long startMask = LIVE << from;
		final long endMask = LIVE >>> -to;
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
	public void clear(final int index) {
		andWord(BitSet.divideSize(index), ~BitSet.bitMask(index));
	}

	/**
	 * Changes the state of all bits in the specified range [<b>from</b>, <b>to</b>)
	 * to the <i>dead</i> state. No action is taken if <b>from</b> is greater than
	 * or equal to <b>to</b>. {@link ConcurrentBitSet} will only perform this
	 * atomically on each word within the range individually.
	 * 
	 * @param from (inclusive) the index of the first bit to be cleared.
	 * @param to   (exclusive) the end of the range of bits to be cleared.
	 * @throws ArrayIndexOutOfBoundsException if <b>from</b> or <b>to</b> are
	 *                                        outside of the range [0,
	 *                                        {@link #size}).
	 */
	public void clear(final int from, final int to) {
		Objects.checkFromToIndex(from, to, size);
		final int start = BitSet.divideSize(from);
		final int end = BitSet.divideSize(to - 1);
		final long startMask = LIVE << from;
		final long endMask = LIVE >>> -to;
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
	 * @param index the index of the bit to flip.
	 * @throws ArrayIndexOutOfBoundsException if <b>index</b> is negative or greater
	 *                                        than or equal to {@link #size}.
	 */
	public void flip(final int index) {
		xOrWord(BitSet.divideSize(index), BitSet.bitMask(index));
	}

	/**
	 * Changes the state of all bits in the specified range [<b>from</b>, <b>to</b>)
	 * to their opposites through an {@code XOR} operation. No action is taken if
	 * <b>from</b> is greater than or equal to <b>to</b>. {@link ConcurrentBitSet}
	 * will only perform this atomically on each word within the range individually.
	 * 
	 * @param from (inclusive) the index of the first bit to flip.
	 * @param to   (exclusive) the end of the range of bits to flip.
	 * @throws ArrayIndexOutOfBoundsException if <b>from</b> or <b>to</b> are
	 *                                        outside of the range [0,
	 *                                        {@link #size}).
	 */
	public void flip(final int from, final int to) {
		Objects.checkFromToIndex(from, to, size);
		final int start = BitSet.divideSize(from);
		final int end = BitSet.divideSize(to - 1);
		final long startMask = LIVE << from;
		final long endMask = LIVE >>> -to;
		if (start == end) {
			xOrWord(start, startMask & endMask);
		} else {
			xOrWord(start, startMask);
			for (int i = start + 1; i < end; i++) {
				flipWord(i);
			}
			xOrWord(end, endMask);
		}
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
		final int wordIndex = BitSet.divideSize(index);
		final long mask = BitSet.bitMask(index);
		if ((getWord(wordIndex) & mask) != DEAD) {
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
		final int wordIndex = BitSet.divideSize(index);
		final long mask = ~BitSet.bitMask(index);
		if ((getWord(wordIndex) | mask) != LIVE) {
			return false;
		}
		andWord(wordIndex, mask);
		return true;
	}

	/**
	 * Returns the long word at the specified <b>wordIndex</b> within
	 * {@link #words}.
	 * 
	 * @param wordIndex the index within {@link #words} to read.
	 * @return the raw contents of {@link #words} at the specified <b>wordIndex</b>.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range [0, {@link #wordCount}).
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
	 *                                        range [0, {@link #wordCount}).
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
	 *                                        range [0, {@link #wordCount}).
	 */
	public void andWord(final int wordIndex, final long mask) {
		setWord(wordIndex, getWord(wordIndex) & mask);
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
	 *                                        range [0, {@link #wordCount}).
	 */
	public void orWord(final int wordIndex, final long mask) {
		setWord(wordIndex, getWord(wordIndex) | mask);
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
	 *                                        range [0, {@link #wordCount}).
	 */
	public void xOrWord(final int wordIndex, final long mask) {
		setWord(wordIndex, getWord(wordIndex) ^ mask);
	}

	/**
	 * Changes the long word at <b>wordIndex</b> within {@link #words} to the result
	 * of a {@code NOT AND} operation between the current value at the specified
	 * <b>wordIndex</b> within {@link #words} and the specified <b>mask</b>. <br>
	 * {@code words[wordIndex] = ~(words[wordIndex] & mask);}
	 * 
	 * @param wordIndex the index within {@link #words} to perform the
	 *                  {@code NOT AND} operation upon.
	 * @param mask      the mask to use in the {@code NOT AND} operation on the
	 *                  current value at the specified <b>wordIndex</b>.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range [0, {@link #wordCount}).
	 */
	public void notAndWord(final int wordIndex, final long mask) {
		setWord(wordIndex, ~(getWord(wordIndex) & mask));
	}

	/**
	 * Changes the long word at <b>wordIndex</b> within {@link #words} to the result
	 * of a {@code NOT OR} operation between the current value at the specified
	 * <b>wordIndex</b> within {@link #words} and the specified <b>mask</b>. <br>
	 * Performs {@code words[wordIndex] = ~(words[wordIndex] | mask);}
	 * 
	 * @param wordIndex the index within {@link #words} to perform the
	 *                  {@code NOT OR} operation upon.
	 * @param mask      the mask to use in the {@code NOT OR} operation on the
	 *                  current value at the specified <b>wordIndex</b>.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range [0, {@link #wordCount}).
	 */
	public void notOrWord(final int wordIndex, final long mask) {
		setWord(wordIndex, ~(getWord(wordIndex) | mask));
	}

	/**
	 * Changes the long word at <b>wordIndex</b> within {@link #words} to the result
	 * of a {@code NOT XOR} operation between the current value at the specified
	 * <b>wordIndex</b> within {@link #words} and the specified <b>mask</b>. <br>
	 * {@code words[wordIndex] = ~(words[wordIndex] ^ mask);}
	 * 
	 * @param wordIndex the index within {@link #words} to perform the
	 *                  {@code NOT XOR} operation upon.
	 * @param mask      the mask to use in the {@code NOT XOR} operation on the
	 *                  current value at the specified <b>wordIndex</b>.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range [0, {@link #wordCount}).
	 */
	public void notXOrWord(final int wordIndex, final long mask) {
		setWord(wordIndex, ~(getWord(wordIndex) ^ mask));
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
	 *                                        range [0, {@link #wordCount}).
	 */
	public void setWordSegment(final int wordIndex, final long word, final long mask) {
		setWord(wordIndex, (mask & word) | (~mask & getWord(wordIndex)));
	}

	/**
	 * Changes the long word at <b>wordIndex</b> within {@link #words} to the
	 * complement of its current state.
	 * 
	 * @param wordIndex the index within {@link #words} to perform the flip
	 *                  operation upon.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range [0, {@link #wordCount}).
	 */
	public void flipWord(final int wordIndex) {
		xOrWord(wordIndex, LIVE);
	}

	/**
	 * Changes the long word at <b>wordIndex</b> within {@link #words} to
	 * {@link #LIVE}, setting all bits to the <i>live</i> state.
	 * 
	 * @param wordIndex the index within {@link #words} to perform the fill
	 *                  operation upon.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range [0, {@link #wordCount}).
	 */
	public void fillWord(final int wordIndex) {
		setWord(wordIndex, LIVE);
	}

	/**
	 * Changes the long word at <b>wordIndex</b> within {@link #words} to 0,
	 * removing all bits in the <i>live</i> state.
	 * 
	 * @param wordIndex the index within {@link #words} to perform the empty
	 *                  operation upon.
	 * @throws ArrayIndexOutOfBoundsException if <b>wordIndex</b> is outside of the
	 *                                        range [0, {@link #wordCount}).
	 */
	public void emptyWord(final int wordIndex) {
		setWord(wordIndex, DEAD);
	}

	/**
	 * Applies the specified {@link WordFunction} <b>function</b> to the word at the
	 * specified <b>wordIndex</b>.
	 * 
	 * @param wordIndex the index within {@link #words} to apply the function to.
	 * @param function  the {@link WordFunction} to apply at the specified
	 *                  <b>wordIndex</b>.
	 */
	public void apply(final int wordIndex, final WordFunction function) {
		setWord(wordIndex, function.apply(getWord(wordIndex)));
	}

	/**
	 * Applies the specified {@link WordBiFunction} <b>function</b> to the word at
	 * the specified <b>wordIndex</b> using <b>mask</b> as the second
	 * {@link WordBiFunction} argument.
	 * 
	 * @param wordIndex the index within {@link #words} to apply the function to.
	 * @param function  the {@link WordBiFunction} to apply at the specified
	 *                  <b>wordIndex</b>.
	 * @param mask      the mask to use in the specified <b>function</b>.
	 */
	public void apply(final int wordIndex, final WordBiFunction function, final long mask) {
		setWord(wordIndex, function.apply(getWord(wordIndex), mask));
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
	 * Transforms each bit in this {@link BitSet} into the complement of its current
	 * state.
	 */
	public final void flip() {
		for (int i = 0; i < wordCount; i++) {
			flipWord(i);
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
	public final void and(final BitSet set) {
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
		for (int i = 0; i < wordCount; i++) {
			setWord(i, ~set.getWord(i));
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
		for (int i = 0; i < wordCount; i++) {
			setWord(i, set.getWord(i));
		}
	}

	/**
	 * Changes the state of any hanging bits to the <i>dead</i> state in order to
	 * maintain their effect on aggregating functions ({@link #population()},
	 * {@link #density()}, etc).
	 */
	public final void clearHanging() {
		final int hanging = BitSet.modSize(-size);
		if (hanging > 0) {
			andWord(wordCount - 1, LIVE >>> hanging);
		}
	}

	/**
	 * Calculates the number of <i>live</i> bits within this {@link BitSet}.
	 * {@link #clearHanging()} can be used to stop the interference of hanging bits.
	 * In certain cases, hanging bits can cause an integer overflow.
	 * 
	 * @return the number of <i>live</i> bits.
	 */
	public final int population() {
		int population = 0;
		for (int i = 0; i < wordCount; i++) {
			population += Long.bitCount(getWord(i));
		}
		return population;
	}

	/**
	 * Calculates what percentage of bits in this {@link BitSet} are in the
	 * <i>live</i> state in the specified range [<b>from</b>, <b>to</b>).
	 * {@link #clearHanging()} can be used to stop the interference of hanging bits.
	 * 
	 * @param from (inclusive) the index of the first bit to be checked.
	 * @param to   (exclusive) the end of the range of bits to be checked.
	 * @return the percentage of <i>live</i> bits.
	 * @throws ArrayIndexOutOfBoundsException if <b>from</b> or <b>to</b> are
	 *                                        outside of the range [0,
	 *                                        {@link #size}).
	 */
	public final double density(final int from, final int to) {
		return get(from, to) / (double) (to - from);
	}

	/**
	 * Calculates what percentage of bits in this {@link BitSet} are in the
	 * <i>live</i> state. {@link #clearHanging()} can be used to stop the
	 * interference of hanging bits.
	 *
	 * @return the percentage of <i>live</i> bits.
	 */
	public final double density() {
		return population() / (double) size;
	}

	/**
	 * Calculates <b>n</b> / 64. Typically used to translate the index of a bit to
	 * the index of the word that bit belongs to within {@link #words}.
	 * 
	 * @param n the number to divide by 64.
	 * @return <b>n</b> / 64.
	 */
	public static final int divideSize(final int n) {
		return n >>> LOG_2_SIZE;
	}

	/**
	 * Calculates <b>n</b> * 64. Typically used to translate the index of a word
	 * within {@link #words} to the index of that word's first bit.
	 * 
	 * @param n the number to multiply by 64.
	 * @return <b>n</b> * 64.
	 */
	public static final int multiplySize(final int n) {
		return n << LOG_2_SIZE;
	}

	/**
	 * Calculates <b>n</b> % 64 for positive numbers. Also calculates 64 -
	 * (-<b>n</b> % 64) for negative numbers as a side effect.
	 * 
	 * @param n the number to modulo by 64.
	 * @return the result of the modulo operation.
	 */
	public static final int modSize(final int n) {
		return n & MOD_SIZE_MASK;
	}

	/**
	 * Calculates a mask to represent the bit at which a specific index will be
	 * stored within a long word.
	 * 
	 * @param index the index to represent as a bit.
	 * @return the bit that represents the position of an index within a word.
	 */
	public static final long bitMask(final int index) {
		return 1L << index;
	}

	@Override
	public int hashCode() {
		long hash = size;
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