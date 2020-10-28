package com.shouldis.bitset.parallel;

import java.util.Objects;
import java.util.Spliterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.shouldis.bitset.BitSet;

/**
 * Implementation of {@link Spliterator} that is meant to be used in conjunction
 * with {@link BitSet}s. This class allows for parallel processing on the
 * indices of a {@link BitSet} such that those operations will never enter race
 * conditions on the underlying longs within {@link BitSet#words}. This behavior
 * can only be guaranteed if the indices returned by the {@link Biterator} are
 * manipulated, and not some offset or translation. This bounding of indices to
 * specific threads is needed because of the non-atomic nature of modifying a
 * long. Using a generic parallel {@link IntStream} would cause changes to the
 * underlying long words of a {@link BitSet} to be potentially overridden by
 * other threads.
 *
 * @author Aaron Shouldis
 * @see BitSet
 * @see SizedBiterator
 */
public abstract class Biterator implements Spliterator.OfInt {

	/**
	 * The minimum threshold of remaining indices for which a {@link Biterator} will
	 * refuse a call to {@link Spliterator#trySplit()}. Equal to the size of 4 long
	 * words to ensure splitting is worthwhile, and leaves each process with at
	 * least 1 word to process.
	 */
	protected static final int THRESHOLD = BitSet.multiplySize(4);

	/**
	 * The next index this {@link Biterator} will produce.
	 */
	protected int position;

	/**
	 * The boundary index that this {@link Biterator} will stop upon reaching.
	 */
	protected final int end;

	/**
	 * Creates a {@link Biterator} with the specified range [<b>position</b>,
	 * <b>end</b>).
	 * 
	 * @param position (inclusive) the first index to include.
	 * @param end      (exclusive) index after the last index to include.
	 * @throws IndexOutOfBoundsException if <b>position</b> is greater than or equal
	 *                                   to <b>end</b>, or less than 0.
	 */
	protected Biterator(final int position, final int end) {
		this.position = position;
		this.end = end;
		Objects.checkIndex(position, end);
	}

	/**
	 * Returns a stream operating off of this {@link Biterator}. Defaulting to
	 * serial, {@link Stream#parallel()} may be called to safely process the indices
	 * produced in parallel safely.
	 * 
	 * @return a stream representation of this {@link Biterator}.
	 */
	public final IntStream stream() {
		return StreamSupport.intStream(this, false);
	}

	/**
	 * Calculates the index in the middle index of this {@link Biterator} using the
	 * current {@link #position} and {@link #end}. Performs calculations such that
	 * there cannot be an overflow due to large integers.
	 * 
	 * @return the middle index of this {@link Biterator}.
	 */
	protected final int middle() {
		return (position & end) + ((position ^ end) >>> 1);
	}

	/**
	 * Calculates an appropriate place to split this {@link Biterator} considering
	 * only the current {@link #position} and {@link #end}.
	 * 
	 * @return an appropriate place to split this {@link Biterator}
	 */
	protected int splitIndex() {
		final int middle = middle();
		return middle - BitSet.modSize(middle);
	}

	/**
	 * Calculates the index of the next <i>live</i> bit within a specified
	 * <b>word</b> that is at the specified <b>wordIndex</b> within
	 * {@link BitSet#words}. This index will represent its bit index in the
	 * underlying long array as well as the offset within the integer <b>word</b>.
	 * 
	 * @param word      the long word to be checked for a <i>live</i> bit.
	 * @param wordIndex the index of the word within {@link BitSet#words}.
	 * @return the index of the next <i>live</i> bit within the specified word, or
	 *         {@link #end} if none are found.
	 */
	protected final int nextLiveBit(final long word, final int wordIndex) {
		final int index = BitSet.multiplySize(wordIndex) + Long.numberOfTrailingZeros(word);
		return index < end ? index : end;
	}

	@Override
	public int characteristics() {
		return DISTINCT | ORDERED | NONNULL | IMMUTABLE;
	}

	@Override
	public long estimateSize() {
		return end - position;
	}

}