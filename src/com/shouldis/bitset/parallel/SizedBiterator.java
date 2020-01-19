package com.shouldis.bitset.parallel;

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
	 * Creates a {@link SizedBiterator} with the specified starting and ending
	 * position.
	 * 
	 * @param position (inclusive) the first index to include.
	 * @param end      (exclusive) index after the last index to include.
	 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
	 *                                  to <b>end</b>.
	 */
	protected SizedBiterator(final int position, final int end) {
		super(position, end);
	}

	@Override
	public long estimateSize() {
		return end - position;
	}

	@Override
	public int characteristics() {
		return SIZED | SUBSIZED | DISTINCT | ORDERED | NONNULL | IMMUTABLE;
	}

}