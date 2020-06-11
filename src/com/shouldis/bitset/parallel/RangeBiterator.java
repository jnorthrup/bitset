package com.shouldis.bitset.parallel;

import java.util.Spliterator;
import java.util.function.IntConsumer;

import com.shouldis.bitset.BitSet;

/**
 * Implementation of {@link SizedBiterator} used to stream all indices of a
 * specified range, splitting at appropriate indices to manipulate a
 * {@link BitSet} in parallel.
 * 
 * @author Aaron Shouldis
 * @see SizedBiterator
 */
public final class RangeBiterator extends SizedBiterator {

	/**
	 * Creates a {@link RangeBiterator} with the specified range [<b>position</b>,
	 * <b>end</b>).
	 * 
	 * @param position (inclusive) the first index to include.
	 * @param end      (exclusive) the index after the last index to include.
	 * @throws IndexOutOfBoundsException if <b>position</b> is greater than or equal
	 *                                   to <b>end</b>, or less than 0.
	 */
	public RangeBiterator(final int position, final int end) {
		super(position, end);
	}

	@Override
	public boolean tryAdvance(final IntConsumer action) {
		if (position < end) {
			action.accept(position++);
			return true;
		}
		return false;
	}

	@Override
	public void forEachRemaining(final IntConsumer action) {
		while (position < end) {
			action.accept(position++);
		}
	}

	@Override
	public Spliterator.OfInt trySplit() {
		if (estimateSize() < THRESHOLD) {
			return null;
		}
		return new RangeBiterator(position, position = splitIndex());
	}

}