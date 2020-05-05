package com.shouldis.bitset.function;

import com.shouldis.bitset.BitSet;
import com.shouldis.bitset.ConcurrentBitSet;

/**
 * Functional interface used to apply functions to a {@link BitSet}.
 * 
 * @author Aaron Shouldis
 */
public interface BitSetFunction {

	/**
	 * Function used to perform operations on a {@link BitSet}.
	 * 
	 * @param set the {@link BitSet} to manipulate.
	 * @return the manipulated <b>set</b>.
	 */
	public BitSet apply(final BitSet set);

	/**
	 * {@link BitSetFunction} that returns a {@link BitSet} copy of the specified
	 * {@link BitSet} through {@link BitSet#BitSet(BitSet)}.
	 */
	public static final BitSetFunction COPY = BitSet::new;

	/**
	 * {@link BitSetFunction} that returns a {@link ConcurrentBitSet} copy of the
	 * specified {@link BitSet} through
	 * {@link ConcurrentBitSet#ConcurrentBitSet(BitSet)}.
	 */
	public static final BitSetFunction CONCURRENT_COPY = ConcurrentBitSet::new;

	/**
	 * Creates a {@link BitSetFunction} which performs the two specified
	 * {@link BitSetFunction}s <b>first</b> and <b>second</b> in sequence.
	 * 
	 * @param first  the first {@link BitSetFunction} to be applied to the word
	 *               argument.
	 * @param second the second {@link BitSetFunction} to be applied to the result
	 *               of <b>first</b>.
	 * @return a {@link BitSetFunction} which applies both <b>first</b> and
	 *         <b>second</b>.
	 */
	public static BitSetFunction combine(final BitSetFunction first, final BitSetFunction second) {
		return (final BitSet set) -> {
			return second.apply(first.apply(set));
		};
	}

}