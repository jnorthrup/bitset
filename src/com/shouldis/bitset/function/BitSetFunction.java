package com.shouldis.bitset.function;

import java.util.Objects;

import com.shouldis.bitset.BitSet;
import com.shouldis.bitset.ConcurrentBitSet;
import com.shouldis.bitset.ImmutableBitSet;
import com.shouldis.bitset.InlineBitSet;

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
	public BitSet apply(BitSet set);

	/**
	 * {@link BitSetFunction} that returns a copy of the specified {@link BitSet}
	 * using {@link BitSet#BitSet(BitSet)}.
	 */
	public static final BitSetFunction COPY = BitSet::new;

	/**
	 * {@link BitSetFunction} that returns a copy of the specified {@link BitSet}
	 * using {@link ConcurrentBitSet#BitSet(BitSet)}.
	 */
	public static final BitSetFunction CONCURRENT_COPY = ConcurrentBitSet::new;

	/**
	 * {@link BitSetFunction} that returns a copy of the specified {@link BitSet}
	 * using {@link ImmutableBitSet#BitSet(BitSet)}.
	 */
	public static final BitSetFunction IMMUTABLE_COPY = ImmutableBitSet::new;

	/**
	 * {@link BitSetFunction} that returns a copy of the specified {@link BitSet}
	 * using {@link InlineBitSet#BitSet(BitSet)}.
	 */
	public static final BitSetFunction INLINE_COPY = ImmutableBitSet::new;

	/**
	 * Creates a {@link BitSetFunction} which performs the specified
	 * {@link WordFunction} <b>function</b> on each word contained in the
	 * {@link BitSet} provided.
	 * 
	 * @param function the {@link WordFunction} to be applied to the {@link BitSet}.
	 * @return a {@link BitSetFunction} which applies the specified <b>function</b>.
	 */
	public static BitSetFunction of(final WordFunction function) {
		return (final BitSet set) -> {
			for (int i = 0; i < set.wordCount; i++) {
				set.apply(i, function);
			}
			return set;
		};
	}

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
		Objects.requireNonNull(first);
		Objects.requireNonNull(second);
		return (final BitSet set) -> second.apply(first.apply(set));
	}

}