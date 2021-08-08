package com.shouldis.bitset.function;

import java.util.Objects;

import com.shouldis.bitset.BitSet;

/**
 * Functional interface used to apply functions to a {@link BitSet} using a
 * second {@link BitSet}.
 * 
 * @author Aaron Shouldis
 */
public interface BitSetBiFunction {

	/**
	 * Function used to perform operations on a {@link BitSet} using a second
	 * {@link BitSet}.
	 * 
	 * @param set  the {@link BitSet} to manipulate.
	 * @param mask the second {@link BitSet} typically used as a mask in
	 *             {@link BitSetBiFunction}s.
	 * @return the manipulated <b>set</b>.
	 */
	public BitSet apply(BitSet set, BitSet mask);

	public static BitSetBiFunction of(final WordBiFunction function) {
		return (final BitSet set, final BitSet mask) -> {
			for (int i = 0; i < set.wordCount; i++) {
				set.apply(i, function, mask.getWord(i));
			}
			return set;
		};
	}

	/**
	 * Creates a {@link BitSetBiFunction} which performs the two specified
	 * {@link BitSetBiFunction}s <b>first</b> and <b>second</b> in sequence.
	 * 
	 * @param first  the first {@link BitSetBiFunction} to be applied to the word
	 *               argument.
	 * @param second the second {@link BitSetBiFunction} to be applied to the result
	 *               of <b>first</b>.
	 * @return a {@link BitSetBiFunction} which applies both <b>first</b> and
	 *         <b>second</b>.
	 * 
	 * @throws NullPointerException if <b>first</b> or <b>second</b> are null.
	 */
	public static BitSetBiFunction combine(final BitSetBiFunction first, final BitSetBiFunction second) {
		Objects.requireNonNull(first);
		Objects.requireNonNull(second);
		return (final BitSet set, final BitSet mask) -> second.apply(first.apply(set, mask), mask);
	}

}