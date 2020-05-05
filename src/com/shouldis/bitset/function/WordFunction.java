package com.shouldis.bitset.function;

import com.shouldis.bitset.BitSet;

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
	public long apply(final long word);

	/**
	 * {@link WordFunction} used to invert the bits within the argument <b>word</b>.
	 */
	public static final WordFunction NOT = (final long word) -> {
		return ~word;
	};

	/**
	 * {@link WordFunction} used to reverse the order of bits within the argument
	 * <b>word</b>.
	 */
	public static final WordFunction REVERSE = Long::reverse;

	/**
	 * {@link WordFunction} used to reverse the order of bits within the argument
	 * <b>word</b>. Performs the operations of {@link #REVERSE}, but only
	 * appropriate for the last word of the specified {@link BitSet} <b>set</b>, or
	 * {@link BitSet}s of the same size.
	 * 
	 * @param set the {@link BitSet} to generate the reverse function for.
	 * @return the function used to reverse the final word in <b>set</b>.
	 */
	public static WordFunction hangingReverse(final BitSet set) {
		final long hanging = BitSet.modSize(-set.size);
		final long mask = BitSet.MASK >>> hanging;
		return (final long word) -> {
			return mask & (Long.reverse(word) >>> hanging);
		};
	}

	/**
	 * {@link WordFunction} used to shift the bits within the argument <b>word</b>
	 * to the right.
	 * 
	 * @param distance how far to shift the bits to the right.
	 * @return a function representing a shift by <b>distance</b> bits.
	 */
	public static WordFunction shiftR(final int distance) {
		return (final long word) -> {
			return word >>> distance;
		};
	}

	/**
	 * {@link WordFunction} used to shift the bits within the argument <b>word</b>
	 * to the right. Performs the operations of {@link #shiftR(int)}, but only
	 * appropriate for the last word of the specified {@link BitSet} <b>set</b>, or
	 * {@link BitSet}s of the same size.
	 * 
	 * @param set      the {@link BitSet} to generate the right shift function for.
	 * @param distance how far to shift the bits to the right.
	 * @return the function used to shift the final word in <b>set</b>.
	 */
	public static WordFunction hangingShiftR(final BitSet set, final int distance) {
		final long mask = BitSet.MASK >>> BitSet.modSize(-set.size);
		return (final long word) -> {
			return (mask & word) >>> distance;
		};
	}

	/**
	 * {@link WordFunction} used to shift the bits within the argument <b>word</b>
	 * to the left.
	 * 
	 * @param distance how far to shift the bits to the left.
	 * @return a function representing a shift by <b>distance</b> bits.
	 */
	public static WordFunction shiftL(final int distance) {
		return (final long word) -> {
			return word << distance;
		};
	}

	/**
	 * {@link WordFunction} used to shift the bits within the argument <b>word</b>
	 * to the left. Performs the operations of {@link #shiftL(int)}, but only
	 * appropriate for the last word of the specified {@link BitSet} <b>set</b>, or
	 * {@link BitSet}s of the same size.
	 * 
	 * @param set      the {@link BitSet} to generate the left shift function for.
	 * @param distance how far to shift the bits to the left.
	 * @return the function used to shift the final word in <b>set</b>.
	 */
	public static WordFunction hangingShiftL(final BitSet set, final int distance) {
		final long mask = BitSet.MASK >>> BitSet.modSize(-set.size);
		return (final long word) -> {
			return mask & (word << distance);
		};
	}

	/**
	 * {@link WordFunction} used to rotate the bits within the argument <b>word</b>
	 * to the right.
	 * 
	 * @param distance how far to rotate the bits to the right.
	 * @return a function representing a shift by <b>distance</b> bits.
	 */
	public static WordFunction rotateR(final int distance) {
		return (final long word) -> {
			return Long.rotateRight(word, distance);
		};
	}

	/**
	 * {@link WordFunction} used to rotate the bits within the argument <b>word</b>
	 * to the right. Performs the operations of {@link #rotateR(int)}, but only
	 * appropriate for the last word of the specified {@link BitSet} <b>set</b>, or
	 * {@link BitSet}s of the same size.
	 * 
	 * @param set      the {@link BitSet} to generate the right rotate function for.
	 * @param distance how far to rotate the bits to the right.
	 * @return the function used to shift the final word in <b>set</b>.
	 */
	public static WordFunction hangingRotateR(final BitSet set, final int distance) {
		final long hanging = BitSet.modSize(-set.size);
		final long mask = BitSet.MASK >>> hanging;
		return (final long word) -> {
			return mask & ((word >>> distance) | (word << -(hanging + distance)));
		};
	}

	/**
	 * {@link WordFunction} used to rotate the bits within the argument <b>word</b>
	 * to the left.
	 * 
	 * @param distance how far to rotate the bits to the left.
	 * @return a function representing a shift by <b>distance</b> bits.
	 */
	public static WordFunction rotateL(final int distance) {
		return (final long word) -> {
			return Long.rotateLeft(word, distance);
		};
	}

	/**
	 * {@link WordFunction} used to rotate the bits within the argument <b>word</b>
	 * to the left. Performs the operations of {@link #rotateL(int)}, but only
	 * appropriate for the last word of the specified {@link BitSet} <b>set</b>, or
	 * {@link BitSet}s of the same size.
	 * 
	 * @param set      the {@link BitSet} to generate the left rotate function for.
	 * @param distance how far to rotate the bits to the left.
	 * @return the function used to shift the final word in <b>set</b>.
	 */
	public static WordFunction hangingRotateL(final BitSet set, final int distance) {
		final long hanging = BitSet.modSize(-set.size);
		final long mask = BitSet.MASK >>> hanging;
		return (final long word) -> {
			return mask & ((word << distance) | (word >>> -(hanging + distance)));
		};
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
	 */
	public static WordFunction combine(final WordFunction first, final WordFunction second) {
		return (final long word) -> {
			return second.apply(first.apply(word));
		};
	}

}