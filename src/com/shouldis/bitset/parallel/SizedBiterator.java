package com.shouldis.bitset.parallel;

import java.util.Objects;

/**
 * Implementation of {@link Biterator} that is meant to be used when streaming a
 * known number of indices. Classes extending {@link SizedBiterator} should
 * implement {@link #tryAdvance(java.util.function.IntConsumer)},
 * {@link #trySplit()}, and
 * {@link #forEachRemaining(java.util.function.IntConsumer)}.
 * 
 * @author Aaron Shouldis
 */
public abstract class SizedBiterator extends Biterator {

	/**
	 * Creates a {@link SizedBiterator} with the specified range [<b>position</b>,
	 * <b>end</b>).
	 * 
	 * @param position (inclusive) the first index to include.
	 * @param end      (exclusive) index after the last index to include.
	 * @throws IndexOutOfBoundsException if <b>position</b> is greater than or equal
	 *                                   to <b>end</b>, or less than 0.
	 */
	protected SizedBiterator(final int position, final int end) {
		super(position, end);
		Objects.checkIndex(position, end);
	}

	@Override
	public int characteristics() {
		return SIZED | SUBSIZED | DISTINCT | ORDERED | NONNULL | IMMUTABLE;
	}

}