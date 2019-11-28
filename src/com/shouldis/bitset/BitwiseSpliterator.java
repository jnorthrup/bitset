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
	protected BitwiseSpliterator(BitSet set1, BitSet set2, int position, int end) {
		super(position, end);
		set1.compareSize(set2);
		this.set1 = set1;
		this.set2 = set2;
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
	protected abstract long bitwiseFunction(long word1, long word2);

	@Override
	public long getExactSizeIfKnown() {
		if (position >= end) {
			return 0;
		}
		int positionWord = BitSet.wordIndex(position);
		int endWord = BitSet.wordIndex(end - 1);
		long positionMask = BitSet.MASK << position;
		long endMask = BitSet.MASK >>> -end;
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
	public boolean tryAdvance(IntConsumer action) {
		position = next(position);
		if (position < end) {
			action.accept(position++);
			return true;
		}
		return false;
	}

	@Override
	public void forEachRemaining(IntConsumer action) {
		position = next(position);
		if (position >= end) {
			position = end;
			return;
		}
		int wordIndex = BitSet.wordIndex(position);
		int lastWordIndex = BitSet.wordIndex(end - 1);
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
	private final int next(int index) {
		int wordIndex = BitSet.wordIndex(index);
		int lastWordIndex = BitSet.wordIndex(end - 1);
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
	private final long bitwiseWord(int wordIndex) {
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
		public And(BitSet set1, BitSet set2, int position, int end) {
			super(set1, set2, position, end);
		}

		@Override
		public long estimateSize() {
			double density1 = set1.density(position, end);
			double density2 = set2.density(position, end);
			return Math.round((end - position) * density1 * density2);
		}

		@Override
		public OfInt trySplit() {
			if (estimateSize() < THRESHOLD) {
				return null;
			}
			return new BitwiseSpliterator.And(set1, set2, position, position = splitIndex());
		}

		@Override
		protected long bitwiseFunction(long word1, long word2) {
			return word1 & word2;
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
		public Or(BitSet set1, BitSet set2, int position, int end) {
			super(set1, set2, position, end);
		}

		@Override
		public long estimateSize() {
			double density1 = set1.density(position, end);
			double density2 = set2.density(position, end);
			return Math.round((end - position) * (density1 + density2 - (density1 * density2)));
		}

		@Override
		public OfInt trySplit() {
			if (estimateSize() < THRESHOLD) {
				return null;
			}
			return new BitwiseSpliterator.Or(set1, set2, position, position = splitIndex());
		}

		@Override
		protected long bitwiseFunction(long word1, long word2) {
			return word1 | word2;
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
		public XOr(BitSet set1, BitSet set2, int position, int end) {
			super(set1, set2, position, end);
		}

		@Override
		public long estimateSize() {
			double density1 = set1.density(position, end);
			double density2 = set2.density(position, end);
			return Math.round((end - position) * (density1 + density2 - (2.0f * density1 * density2)));
		}

		@Override
		public OfInt trySplit() {
			if (estimateSize() < THRESHOLD) {
				return null;
			}
			return new BitwiseSpliterator.XOr(set1, set2, position, position = splitIndex());
		}

		@Override
		protected long bitwiseFunction(long word1, long word2) {
			return word1 ^ word2;
		}

	}

}