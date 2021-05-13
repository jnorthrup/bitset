package com.shouldis.bitset;

import java.util.Objects;

/**
 * Functional interface used in conjunction with
 * {@link BitSet#apply(int, WordFunction)}. {@link #apply(long)} should
 * represent a function used to manipulate the bits within the words of a
 * {@link BitSet}. Useful for performing more complex operations outside the
 * suite of typical bitwise operations.
 * 
 * @author Aaron Shouldis
 */
public interface WordFunction {

	/**
	 * Function used to perform operations on the words within a {@link BitSet}.
	 * 
	 * @param word the original word to manipulate.
	 * @return the manipulated <b>word</b>.
	 */
	public long apply(long word);

	/**
	 * {@link WordFunction} used to reverse the order of bits within the argument
	 * <b>word</b>.
	 */
	public static final WordFunction REVERSE = Long::reverse;

	/**
	 * {@link WordFunction} used to reverse the order of bits within the argument
	 * <b>word</b>. Functionally the same operation as {@link #REVERSE}, but only
	 * appropriate for the last word of {@link BitSet} of the specified <b>size</b>.
	 * 
	 * @param size the size of {@link BitSet}s this function will be appropriate for
	 *             manipulating.
	 * @return the function used to reverse the final word in <b>set</b>.
	 */
	public static WordFunction hangingReverse(final int size) {
		final long hanging = BitSet.modSize(-size);
		final long mask = BitSet.LIVE >>> hanging;
		return (final long word) -> mask & (Long.reverse(word) >>> hanging);
	}

	/**
	 * {@link WordFunction} used to shift the bits within the argument <b>word</b>
	 * to the right.
	 * 
	 * @param distance how far to shift the bits to the right.
	 * @return a function representing a shift by <b>distance</b> bits.
	 */
	public static WordFunction shiftR(final int distance) {
		return (final long word) -> word >>> distance;
	}

	/**
	 * {@link WordFunction} used to shift the bits within the argument <b>word</b>
	 * to the right. Functionally the same operation as {@link #shiftR(int)}, but
	 * only appropriate for the last word of {@link BitSet} of the specified
	 * <b>size</b>.
	 * 
	 * @param size     the size of {@link BitSet}s this function will be appropriate
	 *                 for manipulating.
	 * @param distance how far to shift the bits to the right.
	 * @return the function used to shift the final word in <b>set</b>.
	 */
	public static WordFunction hangingShiftR(final int size, final int distance) {
		final long mask = BitSet.LIVE >>> BitSet.modSize(-size);
		return (final long word) -> (mask & word) >>> distance;
	}

	/**
	 * {@link WordFunction} used to shift the bits within the argument <b>word</b>
	 * to the left.
	 * 
	 * @param distance how far to shift the bits to the left.
	 * @return a function representing a shift by <b>distance</b> bits.
	 */
	public static WordFunction shiftL(final int distance) {
		return (final long word) -> word << distance;
	}

	/**
	 * {@link WordFunction} used to shift the bits within the argument <b>word</b>
	 * to the left. Functionally the same operation as {@link #shiftL(int)}, but
	 * only appropriate for the last word of {@link BitSet} of the specified
	 * <b>size</b>.
	 * 
	 * @param size     the size of {@link BitSet}s this function will be appropriate
	 *                 for manipulating.
	 * @param distance how far to shift the bits to the right.
	 * @return the function used to shift the final word in <b>set</b>.
	 */
	public static WordFunction hangingShiftL(final int size, final int distance) {
		final long mask = BitSet.LIVE >>> BitSet.modSize(-size);
		return (final long word) -> mask & (word << distance);
	}

	/**
	 * {@link WordFunction} used to rotate the bits within the argument <b>word</b>
	 * to the right.
	 * 
	 * @param distance how far to rotate the bits to the right.
	 * @return a function representing a shift by <b>distance</b> bits.
	 */
	public static WordFunction rotateR(final int distance) {
		return (final long word) -> (word >>> distance) | (word << -distance);
	}

	/**
	 * {@link WordFunction} used to rotate the bits within the argument <b>word</b>
	 * to the right. Functionally the same operation as {@link #rotateR(int)}, but
	 * only appropriate for the last word of {@link BitSet} of the specified
	 * <b>size</b>.
	 * 
	 * @param size     the size of {@link BitSet}s this function will be appropriate
	 *                 for manipulating.
	 * @param distance how far to shift the bits to the right.
	 * @return the function used to shift the final word in <b>set</b>.
	 */
	public static WordFunction hangingRotateR(final int size, final int distance) {
		final long hanging = BitSet.modSize(-size);
		final long mask = BitSet.LIVE >>> hanging;
		return (final long word) -> mask & ((word >>> distance) | (word << -(hanging + distance)));
	}

	/**
	 * {@link WordFunction} used to rotate the bits within the argument <b>word</b>
	 * to the left.
	 * 
	 * @param distance how far to rotate the bits to the left.
	 * @return a function representing a shift by <b>distance</b> bits.
	 */
	public static WordFunction rotateL(final int distance) {
		return (final long word) -> (word << distance) | (word >>> -distance);
	}

	/**
	 * {@link WordFunction} used to rotate the bits within the argument <b>word</b>
	 * to the left. Functionally the same operation as {@link #rotateL(int)}, but
	 * only appropriate for the last word of {@link BitSet} of the specified
	 * <b>size</b>.
	 * 
	 * @param size     the size of {@link BitSet}s this function will be appropriate
	 *                 for manipulating.
	 * @param distance how far to shift the bits to the right.
	 * @return the function used to shift the final word in <b>set</b>.
	 */
	public static WordFunction hangingRotateL(final int size, final int distance) {
		final long hanging = BitSet.modSize(-size);
		final long mask = BitSet.LIVE >>> hanging;
		return (final long word) -> mask & ((word << distance) | (word >>> -(hanging + distance)));
	}

	/**
	 * Creates a {@link WordFunction} which performs the two specified
	 * {@link WordFunction}s <b>first</b> and <b>second</b> in sequence.
	 * 
	 * @param first  the first {@link WordFunction} to be applied to the word
	 *               argument.
	 * @param second the second {@link WordFunction} to be applied to the result of
	 *               <b>first</b>.
	 * @return a {@link WordFunction} which applies both <b>first</b> and
	 *         <b>second</b>.
	 * 
	 * @throws NullPointerException if <b>first</b> or <b>second</b> are null.
	 */
	public static WordFunction combine(final WordFunction first, final WordFunction second) {
		Objects.requireNonNull(first);
		Objects.requireNonNull(second);
		return (final long word) -> second.apply(first.apply(word));
	}

}