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
	 * {@link BitSetFunction} that transforms each bit in the specified
	 * {@link BitSet} into the complement of its current state.
	 */
	public static final BitSetFunction TOGGLE = (final BitSet set) -> {
		for (int i = 0; i < set.wordCount; i++) {
			set.toggleWord(i);
		}
		return set;
	};

	/**
	 * {@link BitSetFunction} that transforms each bit in the specified
	 * {@link BitSet} to the <i>live</i> state.
	 */
	public static final BitSetFunction FILL = (final BitSet set) -> {
		for (int i = 0; i < set.wordCount; i++) {
			set.fillWord(i);
		}
		return set;
	};

	/**
	 * {@link BitSetFunction} that transforms each bit in the specified
	 * {@link BitSet} to the <i>dead</i> state.
	 */
	public static final BitSetFunction EMPTY = (final BitSet set) -> {
		for (int i = 0; i < set.wordCount; i++) {
			set.emptyWord(i);
		}
		return set;
	};

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
	 * 
	 * @throws NullPointerException if <b>first</b> or <b>second</b> are null.
	 */
	public static BitSetFunction combine(final BitSetFunction first, final BitSetFunction second) {
		return (final BitSet set) -> {
			return second.apply(first.apply(set));
		};
	}

	/**
	 * Creates a {@link BitSetFunction} which performs the specified array of
	 * {@link BitSetFunction}s <b>functions</b> in sequence starting at index 0.
	 * 
	 * @param functions the list of {@link BitSetFunction}s to sequence.
	 * @return a sequenced {@link BitSetFunction} containing each of the
	 *         {@link BitSetFunction}s in <b>functions</b>.
	 * 
	 * @throws NullPointerException if <b>functions</b>, or any of the
	 *                              {@link BitSetFunction}s in <b>functions</b> are
	 *                              null.
	 */
	public static BitSetFunction sequence(final BitSetFunction... functions) {
		BitSetFunction aggregate = functions[0];
		for (int i = 1; i < functions.length; i++) {
			aggregate = combine(aggregate, functions[i]);
		}
		return aggregate;
	}

}