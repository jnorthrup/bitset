package com.shouldis.bitset.parallel;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntConsumer;

import com.shouldis.bitset.BitSet;

/**
 * Implementation of {@link SizedBiterator} used to stream all values within a
 * specified integer array representing indices in an order appropriate to
 * manipulate a {@link BitSet} in parallel. The contents of the supplied arrays
 * must be distinct, and sorted in ascending order; behavior is undefined
 * otherwise.
 * 
 * @author Aaron Shouldis
 * @see SizedBiterator
 */
public final class ArrayBiterator extends SizedBiterator {

	/**
	 * The indices to be processed by this {@link ArrayBiterator}.
	 */
	private final int[] items;

	/**
	 * Creates a {@link ArrayBiterator} with the specified starting and ending
	 * indices <b>position</b> and <b>end</b>. The values within the specified range
	 * of indices [<b>from</b>, <b>to</b>) within the array <b>items</b> will be
	 * processed.
	 * 
	 * @param items    the array of indices to be processed.
	 * @param position (inclusive) the first index to include.
	 * @param end      (exclusive) the index after the last index to include.
	 * @throws NullPointerException      if <b>items</b> is null.
	 * @throws IndexOutOfBoundsException if <b>position</b> is greater than or equal
	 *                                   to <b>end</b>, or less than 0.
	 * @throws IndexOutOfBoundsException if <b>end</b> is greater than
	 *                                   <b>items</b>.length.
	 */
	public ArrayBiterator(final int[] items, final int position, final int end) {
		super(position, end);
		this.items = items;
		Objects.checkFromToIndex(position, end, items.length);
	}

	/**
	 * Creates a {@link ArrayBiterator} covering the entirety of the specified
	 * array. The values within the specified array <b>items</b> will be processed.
	 * 
	 * @param items the array of indices to be processed.
	 * @throws NullPointerException if <b>items</b> is null.
	 */
	public ArrayBiterator(final int[] items) {
		this(items, 0, items.length);
	}

	@Override
	protected int splitIndex() {
		int middle = middle();
		final int wordIndex = BitSet.divideSize(items[middle++]);
		while (BitSet.divideSize(items[middle]) <= wordIndex) {
			middle++;
		}
		return middle;
	}

	@Override
	public boolean tryAdvance(final IntConsumer action) {
		if (position < end) {
			action.accept(items[position++]);
			return true;
		}
		return false;
	}

	@Override
	public void forEachRemaining(final IntConsumer action) {
		while (position < end) {
			action.accept(items[position++]);
		}
	}

	@Override
	public Spliterator.OfInt trySplit() {
		if (estimateSize() < Biterator.THRESHOLD) {
			return null;
		}
		return new ArrayBiterator(items, position, position = splitIndex());
	}

}