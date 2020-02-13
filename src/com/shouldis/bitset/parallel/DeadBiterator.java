package com.shouldis.bitset.parallel;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntConsumer;

import com.shouldis.bitset.BitSet;

/**
 * Implementation of {@link Biterator} used to stream the indices of <i>dead</i>
 * bits within a {@link BitSet}, splitting at appropriate indices to manipulate
 * a {@link BitSet} in parallel. Words are cached as they are encountered, so
 * any modifications after iteration begins may not be included.
 * 
 * @author Aaron Shouldis
 * @see Biterator
 */
public class DeadBiterator extends Biterator {

	/**
	 * The {@link BitSet} that the <i>dead</i> bit indices will be calculated from.
	 */
	private final BitSet set;

	/**
	 * Creates a {@link DeadBiterator} that will cover all <i>dead</i> bits within
	 * the specified starting and ending indices <b>position</b> and <b>end</b>.
	 * 
	 * @param set      The {@link BitSet} that the <i>dead</i> bit indices will be
	 *                 calculated from.
	 * @param position (inclusive) the first index to include.
	 * @param end      (exclusive) the index after the last index to include.
	 * @throws NullPointerException     if <b>set</b> is null.
	 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
	 *                                  to <b>end</b>.
	 */
	public DeadBiterator(final BitSet set, final int position, final int end) {
		super(position, end);
		this.set = Objects.requireNonNull(set);
	}

	/**
	 * Creates a {@link DeadBiterator} that will cover all <i>dead</i> bits within
	 * the specified {@link BitSet} <b>set</b>.
	 * 
	 * @param set The {@link BitSet} that the <i>dead</i> bit indices will be
	 *            calculated from.
	 * @throws NullPointerException if <b>set</b> is null.
	 */
	public DeadBiterator(final BitSet set) {
		this(set, 0, set.size);
	}

	@Override
	public Spliterator.OfInt trySplit() {
		if (estimateSize() < THRESHOLD) {
			return null;
		}
		return new DeadBiterator(set, position, position = splitIndex());
	}

	@Override
	public boolean tryAdvance(final IntConsumer action) {
		position = next(position);
		if (position < end) {
			action.accept(position++);
			return true;
		}
		return false;
	}

	@Override
	public void forEachRemaining(final IntConsumer action) {
		position = next(position);
		if (position >= end) {
			position = end;
			return;
		}
		int wordIndex = BitSet.divideSize(position);
		final int lastWordIndex = BitSet.divideSize(end - 1);
		long word = ~set.getWord(wordIndex) & (BitSet.MASK << position);
		do {
			action.accept(position);
			word ^= Long.lowestOneBit(word);
			while (word == 0L) {
				if (wordIndex == lastWordIndex) {
					position = end;
					return;
				}
				word = ~set.getWord(++wordIndex);
			}
			position = nextLiveBit(word, wordIndex);
		} while (position < end);
	}

	/**
	 * Calculates the index of the next <i>dead</i> bit after the specified
	 * <b>index</b>, including that <b>index</b>. All bits indices until
	 * {@link #end} will be checked. If no <i>live</i> bits are found, {@link #end}
	 * is returned.
	 * 
	 * @param index (inclusive) the first index to check.
	 * @return the index of the next <i>dead</i> bit, or {@link #end} if none were
	 *         found.
	 */
	private final int next(final int index) {
		int wordIndex = BitSet.divideSize(index);
		final int lastWordIndex = BitSet.divideSize(end - 1);
		if (index >= end) {
			return end;
		}
		long word = ~set.getWord(wordIndex) & (BitSet.MASK << index);
		if (word != 0L) {
			return nextLiveBit(word, wordIndex);
		}
		while (++wordIndex <= lastWordIndex) {
			word = ~set.getWord(wordIndex);
			if (word != 0L) {
				return nextLiveBit(word, wordIndex);
			}
		}
		return end;
	}

}
