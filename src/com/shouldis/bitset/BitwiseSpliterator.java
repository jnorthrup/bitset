package com.shouldis.bitset;

import java.util.function.IntConsumer;

/**
 * Implementation of {@link BitSpliterator} that is creates a stream of indices
 * that represents which bits will be <i>live</i> after performing a
 * {@link #bitwiseFunction(long, long)} between the words of 2 {@link BitSet}s
 * at the same word index. The three traditional operations: {@link And},
 * {@link Or}, and {@link XOr} are provided by this class. The two
 * {@link BitSet}s being compared must be of equal size.
 * 
 * @author Aaron Shouldis
 * @see BitSpliterator
 */
public abstract class BitwiseSpliterator extends BitSpliterator {

	/**
	 * The first {@link BitSet} that will have its words operated on against those
	 * of {@link #set2}.
	 */
	protected final BitSet set1;

	/**
	 * The second {@link BitSet} that will have its words operated on against those
	 * of {@link #set1}.
	 */
	protected final BitSet set2;

	/**
	 * Estimation of the density of bits which will be in the <i>live</i> state in
	 * the resulting words of {@link #bitwiseFunction(long, long)}.
	 */
	private final double density;

	/**
	 * Creates a {@link BitwiseSpliterator} with the specified starting and ending
	 * position. This {@link BitwiseSpliterator} will perform bitwise operations on
	 * the words of the specified {@link BitSet}s.
	 * 
	 * @param set1     the first {@link BitSet}, which will be operated on against
	 *                 <b>set2</b>.
	 * @param set2     the second {@link BitSet}, which will be operated on against
	 *                 <b>set1</b>.
	 * @param position (inclusive) the first index to include.
	 * @param end      (exclusive) index after the last index to include.
	 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
	 *                                  to <b>end</b>, or the sizes of <b>set1</b>
	 *                                  and <b>set2</b> differ.
	 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
	 */
	protected BitwiseSpliterator(final BitSet set1, final BitSet set2, final int position, final int end) {
		super(position, end);
		(this.set1 = set1).compareSize(this.set2 = set2);
		density = density();
	}

	/**
	 * Calculates the result of the bitwise operation represented by this
	 * {@link BitwiseSpliterator} between the specified words from {@link #set1} and
	 * {@link #set2}.
	 * 
	 * @param word1 the long word from {@link #set1} to be operated against
	 *              <b>word2</b>.
	 * @param word2 the long word from {@link #set2} to be operated against
	 *              <b>word1</b>.
	 * @return the result of the bitwise operation between <b>word1</b> and
	 *         <b>word2</b>
	 */
	protected abstract long bitwiseFunction(final long word1, final long word2);

	/**
	 * Calculates an estimation of the density of bits which will be in the
	 * <i>live</i> state in the resulting words of
	 * {@link #bitwiseFunction(long, long)}.
	 * <ul>
	 * <li>{@code density(a & b) = density(a) * density(b)}</li>
	 * <li>{@code density(a | b) = density(a) + density(b) - (density(a) * density(b))}</li>
	 * <li>{@code density(a ^ b) = density(a) + density(b) - (2 * density(a) * density(b))}</li>
	 * </ul>
	 * This percentage is calculated on creation of each {@link BitwiseSpliterator},
	 * then used in all subsequent calls to {@link #estimateSize()}.
	 * 
	 * @return an estimation of the density of words returned by the
	 *         {@link #bitwiseFunction(long, long)} of this
	 *         {@link BitwiseSpliterator}.
	 */
	protected abstract double density();

	@Override
	public long estimateSize() {
		return Math.round((end - position) * density);
	}

	@Override
	public long getExactSizeIfKnown() {
		if (position >= end) {
			return 0;
		}
		final int positionWord = BitSet.divideSize(position);
		final int endWord = BitSet.divideSize(end - 1);
		final long positionMask = BitSet.MASK << position;
		final long endMask = BitSet.MASK >>> -end;
		int sum = 0;
		if (positionWord == endWord) {
			sum += Long.bitCount(bitwiseWord(positionWord) & positionMask & endMask);
		} else {
			sum += Long.bitCount(bitwiseWord(positionWord) & positionMask);
			for (int i = positionWord + 1; i < endWord; i++) {
				sum += Long.bitCount(bitwiseWord(i));
			}
			sum += Long.bitCount(bitwiseWord(endWord) & endMask);
		}
		return sum;
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
		long word = bitwiseWord(wordIndex) & (BitSet.MASK << position);
		do {
			action.accept(position);
			word ^= Long.lowestOneBit(word);
			while (word == 0L) {
				if (wordIndex == lastWordIndex) {
					position = end;
					return;
				}
				word = bitwiseWord(++wordIndex);
			}
			position = nextLiveBit(word, wordIndex);
		} while (position < end);
	}

	/**
	 * Calculates the index of the next <i>live</i> bit after the specified
	 * <b>index</b>, including that <b>index</b>. All bits indices, until
	 * {@link #end} will be checked. If no <i>live</i> bits are found, {@link #end}
	 * is returned.
	 * 
	 * @param index (inclusive) the first index to check.
	 * @return the index of the next <i>live</i> bit, or {@link #end} if none were
	 *         found.
	 */
	private final int next(final int index) {
		int wordIndex = BitSet.divideSize(index);
		final int lastWordIndex = BitSet.divideSize(end - 1);
		if (index >= end) {
			return end;
		}
		long word = bitwiseWord(wordIndex) & (BitSet.MASK << index);
		if (word != 0L) {
			return nextLiveBit(word, wordIndex);
		}
		while (++wordIndex <= lastWordIndex) {
			word = bitwiseWord(wordIndex);
			if (word != 0L) {
				return nextLiveBit(word, wordIndex);
			}
		}
		return end;
	}

	/**
	 * Calculates the result of {@link #bitwiseFunction(long, long)} on the words at
	 * the specified <b>wordIndex</b>.
	 * 
	 * @param wordIndex the word index in {@link #set1} and {@link #set2} to
	 *                  process.
	 * @return the result of {@link #bitwiseFunction(long, long)} on the words at
	 *         the specified word index.
	 */
	private final long bitwiseWord(final int wordIndex) {
		return bitwiseFunction(set1.words[wordIndex], set2.words[wordIndex]);
	}

	/**
	 * Implementation of {@link BitwiseSpliterator} used to stream the indices of
	 * bits that result in the <i>live</i> state after the {@code AND} operation,
	 * splitting at appropriate indices to manipulate a {@link BitSet} in parallel.
	 * Words are cached as they are encountered, so any modifications after
	 * iteration begins may not be included.
	 * 
	 * @see BitwiseSpliterator
	 */
	public static final class And extends BitwiseSpliterator {

		/**
		 * Creates a {@link BitwiseSpliterator.And} that will cover indices of bits
		 * resulting in the <i>live</i> state within the specified starting and ending
		 * indices <b>position</b> and <b>end</b> after an {@code AND} operation..
		 * 
		 * @param set1     The first {@link BitSet} that will be operated on against
		 *                 <b>set2</b>.
		 * @param set2     The second {@link BitSet} that will be operated on against
		 *                 <b>set1</b>.
		 * @param position (inclusive) the first index to include.
		 * @param end      (exclusive) the index after the last index to include.
		 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
		 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
		 *                                  to <b>end</b>, or the sizes of <b>set1</b>
		 *                                  and <b>set2</b> differ.
		 */
		public And(final BitSet set1, final BitSet set2, final int position, final int end) {
			super(set1, set2, position, end);
		}

		@Override
		public OfInt trySplit() {
			if (estimateSize() < THRESHOLD) {
				return null;
			}
			return new BitwiseSpliterator.And(set1, set2, position, position = splitIndex());
		}

		@Override
		protected long bitwiseFunction(final long word1, final long word2) {
			return word1 & word2;
		}

		@Override
		protected double density() {
			return set1.density(position, end) * set2.density(position, end);
		}

	}

	/**
	 * Implementation of {@link BitwiseSpliterator} used to stream the indices of
	 * bits that result in the <i>live</i> state after the {@code OR} operation,
	 * splitting at appropriate indices to manipulate a {@link BitSet} in parallel.
	 * Words are cached as they are encountered, so any modifications after
	 * iteration begins are not accounted for.
	 * 
	 * @see BitwiseSpliterator
	 */
	public static final class Or extends BitwiseSpliterator {

		/**
		 * Creates a {@link BitwiseSpliterator.And} that will cover indices of bits
		 * resulting in the <i>live</i> state within the specified starting and ending
		 * indices <b>position</b> and <b>end</b> after an {@code OR} operation..
		 * 
		 * @param set1     The first {@link BitSet} that will be operated on against
		 *                 <b>set2</b>.
		 * @param set2     The second {@link BitSet} that will be operated on against
		 *                 <b>set1</b>.
		 * @param position (inclusive) the first index to include.
		 * @param end      (exclusive) the index after the last index to include.
		 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
		 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
		 *                                  to <b>end</b>, or the sizes of <b>set1</b>
		 *                                  and <b>set2</b> differ.
		 */
		public Or(final BitSet set1, final BitSet set2, final int position, final int end) {
			super(set1, set2, position, end);
		}

		@Override
		public OfInt trySplit() {
			if (estimateSize() < THRESHOLD) {
				return null;
			}
			return new BitwiseSpliterator.Or(set1, set2, position, position = splitIndex());
		}

		@Override
		protected long bitwiseFunction(final long word1, final long word2) {
			return word1 | word2;
		}

		@Override
		protected double density() {
			final double density1 = set1.density(position, end);
			final double density2 = set2.density(position, end);
			return density1 + density2 - (density1 * density2);
		}

	}

	/**
	 * Implementation of {@link BitwiseSpliterator} used to stream the indices of
	 * bits that result in the <i>live</i> state after the {@code XOR} operation,
	 * splitting at appropriate indices to manipulate a {@link BitSet} in parallel.
	 * Words are cached as they are encountered, so any modifications after
	 * iteration begins may not be included.
	 * 
	 * @see BitwiseSpliterator
	 */
	public static final class XOr extends BitwiseSpliterator {

		/**
		 * Creates a {@link BitwiseSpliterator.And} that will cover indices of bits
		 * resulting in the <i>live</i> state within the specified starting and ending
		 * indices <b>position</b> and <b>end</b> after an {@code XOR} operation..
		 * 
		 * @param set1     The first {@link BitSet} that will be operated on against
		 *                 <b>set2</b>.
		 * @param set2     The second {@link BitSet} that will be operated on against
		 *                 <b>set1</b>.
		 * @param position (inclusive) the first index to include.
		 * @param end      (exclusive) the index after the last index to include.
		 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
		 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
		 *                                  to <b>end</b>, or the sizes of <b>set1</b>
		 *                                  and <b>set2</b> differ.
		 */
		public XOr(final BitSet set1, final BitSet set2, final int position, final int end) {
			super(set1, set2, position, end);
		}

		@Override
		public OfInt trySplit() {
			if (estimateSize() < THRESHOLD) {
				return null;
			}
			return new BitwiseSpliterator.XOr(set1, set2, position, position = splitIndex());
		}

		@Override
		protected long bitwiseFunction(final long word1, final long word2) {
			return word1 ^ word2;
		}

		@Override
		protected double density() {
			final double density1 = set1.density(position, end);
			final double density2 = set2.density(position, end);
			return density1 + density2 - (2.0f * density1 * density2);
		}

	}

}