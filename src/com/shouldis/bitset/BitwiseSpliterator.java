package com.shouldis.bitset;

import java.util.function.IntConsumer;

/**
 * This class is an implementation of {@link BitSpliterator} that is able to
 * return a stream of indices that represents which bits will be <i>live</i>
 * after performing a {@link #bitwiseFunction(int, int)} between the words of 2
 * {@link BitSet}s at the same word index. The three main operations:
 * {@link And}, {@link Or}, and {@link XOr} are provided by this class. The two
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
	 * position. THis {@link BitwiseSpliterator} will perform bitwise operations on
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
	 * @param wordIndex the index of the words within {@link #words} to be taken
	 *                  from {@link #set1} and {@link #set2}.
	 * @return the result of the bitwise operation on the words at the specified
	 *         <b>wordIndex</b>.
	 */
	protected abstract int bitwiseFunction(int word1, int word2);

	/**
	 * Creates a {@link BitwiseSpliterator} which will supply the indices of bits
	 * within the specified range resulting in the <i>live</i> state after the
	 * bitwise 'and' operation is applied to the words from {@link set1} and
	 * {@link set2}.
	 * 
	 * @param set1     the first {@link BitSet} that will have its words operated on
	 *                 against those of {@link #set2}.
	 * @param set2     the second {@link BitSet} that will have its words operated
	 *                 on against those of {@link #set1}.
	 * @param position (inclusive) the first index to include.
	 * @param end      (exclusive) index after the last index to include.
	 * @return A {@link BitwiseSpliterator} representing the indices of the bits
	 *         resulting in the <i>live</i> state after a bitwise 'and' operation.
	 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
	 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
	 *                                  to <b>end</b>, or the sizes of <b>set1</b>
	 *                                  and <b>set2</b> differ.
	 */
	public static final BitwiseSpliterator and(BitSet set1, BitSet set2, int position, int end) {
		return new BitwiseSpliterator.And(set1, set2, position, end);
	}

	/**
	 * Creates a {@link BitwiseSpliterator} which will supply the indices of bits
	 * resulting in the <i>live</i> state after the bitwise 'and' operation is
	 * applied to the words from {@link set1} and {@link set2}.
	 * 
	 * @param set1 the first {@link BitSet} that will have its words operated on
	 *             against those of {@link #set2}.
	 * @param set2 the second {@link BitSet} that will have its words operated on
	 *             against those of {@link #set1}.
	 * @return A {@link BitwiseSpliterator} representing the indices of the bits
	 *         resulting in the <i>live</i> state after a bitwise 'and' operation.
	 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
	 * @throws IllegalArgumentException if the sizes of <b>set1</b> and <b>set2</b>
	 *                                  differ.
	 */
	public static final BitwiseSpliterator and(BitSet set1, BitSet set2) {
		return and(set1, set2, 0, set1.size);
	}

	/**
	 * Creates a {@link BitwiseSpliterator} which will supply the indices of bits
	 * within the specified range resulting in the <i>live</i> state after the
	 * bitwise 'or' operation is applied to the words from {@link set1} and
	 * {@link set2}.
	 * 
	 * @param set1     the first {@link BitSet} that will have its words operated on
	 *                 against those of {@link #set2}.
	 * @param set2     the second {@link BitSet} that will have its words operated
	 *                 on against those of {@link #set1}.
	 * @param position (inclusive) the first index to include.
	 * @param end      (exclusive) index after the last index to include.
	 * @return A {@link BitwiseSpliterator} representing the indices of the bits
	 *         resulting in the <i>live</i> state after a bitwise 'or' operation.
	 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
	 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
	 *                                  to <b>end</b>, or the sizes of <b>set1</b>
	 *                                  and <b>set2</b> differ.
	 */
	public static final BitwiseSpliterator or(BitSet set1, BitSet set2, int position, int end) {
		return new BitwiseSpliterator.Or(set1, set2, position, end);
	}

	/**
	 * Creates a {@link BitwiseSpliterator} which will supply the indices of bits
	 * resulting in the <i>live</i> state after the bitwise 'or' operation is
	 * applied to the words from {@link set1} and {@link set2}.
	 * 
	 * @param set1 the first {@link BitSet} that will have its words operated on
	 *             against those of {@link #set2}.
	 * @param set2 the second {@link BitSet} that will have its words operated on
	 *             against those of {@link #set1}.
	 * @return A {@link BitwiseSpliterator} representing the indices of the bits
	 *         resulting in the <i>live</i> state after a bitwise 'or' operation.
	 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
	 * @throws IllegalArgumentException if the sizes of <b>set1</b> and <b>set2</b>
	 *                                  differ.
	 */
	public static final BitwiseSpliterator or(BitSet set1, BitSet set2) {
		return or(set1, set2, 0, set1.size);
	}

	/**
	 * Creates a {@link BitwiseSpliterator} which will supply the indices of bits
	 * within the specified range resulting in the <i>live</i> state after the
	 * bitwise 'xor' operation is applied to the words from {@link set1} and
	 * {@link set2}.
	 * 
	 * @param set1     the first {@link BitSet} that will have its words operated on
	 *                 against those of {@link #set2}.
	 * @param set2     the second {@link BitSet} that will have its words operated
	 *                 on against those of {@link #set1}.
	 * @param position (inclusive) the first index to include.
	 * @param end      (exclusive) index after the last index to include.
	 * @return A {@link BitwiseSpliterator} representing the indices of the bits
	 *         resulting in the <i>live</i> state after a bitwise 'xor' operation.
	 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
	 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
	 *                                  to <b>end</b>, or the sizes of <b>set1</b>
	 *                                  and <b>set2</b> differ.
	 */
	public static final BitwiseSpliterator xor(BitSet set1, BitSet set2, int position, int end) {
		return new BitwiseSpliterator.XOr(set1, set2, position, end);
	}

	/**
	 * Creates a {@link BitwiseSpliterator} which will supply the indices of bits
	 * resulting in the <i>live</i> state after the bitwise 'xor' operation is
	 * applied to the words from {@link set1} and {@link set2}.
	 * 
	 * @param set1 the first {@link BitSet} that will have its words operated on
	 *             against those of {@link #set2}.
	 * @param set2 the second {@link BitSet} that will have its words operated on
	 *             against those of {@link #set1}.
	 * @return A {@link BitwiseSpliterator} representing the indices of the bits
	 *         resulting in the <i>live</i> state after a bitwise 'xor' operation.
	 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
	 * @throws IllegalArgumentException if the sizes of <b>set1</b> and <b>set2</b>
	 *                                  differ.
	 */
	public static final BitwiseSpliterator xor(BitSet set1, BitSet set2) {
		return xor(set1, set2, 0, set1.size);
	}

	@Override
	public long getExactSizeIfKnown() {
		if (position >= end) {
			return 0;
		}
		int positionWord = BitSet.wordIndex(position);
		int endWord = BitSet.wordIndex(end - 1);
		int positionMask = BitSet.MASK << position;
		int endMask = BitSet.MASK >>> -end;
		int sum = 0;
		if (positionWord == endWord) {
			sum += Integer.bitCount(bitwiseWord(positionWord) & positionMask & endMask);
		} else {
			sum += Integer.bitCount(bitwiseWord(positionWord) & positionMask);
			for (int i = positionWord + 1; i < endWord; i++) {
				sum += Integer.bitCount(bitwiseWord(i));
			}
			sum += Integer.bitCount(bitwiseWord(endWord) & endMask);
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
		int word = bitwiseWord(wordIndex) & (BitSet.MASK << position);
		do {
			action.accept(position);
			word ^= Integer.lowestOneBit(word);
			while (word == 0) {
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
		int word = bitwiseWord(wordIndex) & (BitSet.MASK << index);
		if (word != 0) {
			return nextLiveBit(word, wordIndex);
		}
		while (++wordIndex <= lastWordIndex) {
			word = bitwiseWord(wordIndex);
			if (word != 0) {
				return nextLiveBit(word, wordIndex);
			}
		}
		return end;
	}

	/**
	 * Calculates the result of {@link #bitwiseFunction(int, int)} on the words at
	 * the specified <b>wordIndex</b>.
	 * 
	 * @param wordIndex the word index in {@link #set1} and {@link #set2} to
	 *                  process.
	 * @return the result of {@link #bitwiseFunction(int, int)} on the words at the
	 *         specified word index.
	 */
	private final int bitwiseWord(int wordIndex) {
		return bitwiseFunction(set1.words[wordIndex], set2.words[wordIndex]);
	}

	/**
	 * An implementation of {@link BitwiseSpliterator} used to stream the indices of
	 * bits that result in the <i>live</i> state after the 'and' operation,
	 * splitting at appropriate indices to manipulate a {@link BitSet} in parallel.
	 * Words are cached as they are encountered, so any modifications after
	 * iteration begins may not be included.
	 * 
	 * @see BitwiseSpliterator
	 */
	private static final class And extends BitwiseSpliterator {

		/**
		 * Creates a {@link BitwiseSpliterator.And} that will cover indices of bits
		 * resulting in the <i>live</i> state within the specified starting and ending
		 * indices <b>position</b> and <b>end</b> after an 'and' operation..
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
			float density1 = set1.density(position, end);
			float density2 = set2.density(position, end);
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
		protected int bitwiseFunction(int word1, int word2) {
			return word1 & word2;
		}

	}

	/**
	 * An implementation of {@link BitwiseSpliterator} used to stream the indices of
	 * bits that result in the <i>live</i> state after the 'or' operation, splitting
	 * at appropriate indices to manipulate a {@link BitSet} in parallel. Words are
	 * cached as they are encountered, so any modifications after iteration begins
	 * may not be included.
	 * 
	 * @see BitwiseSpliterator
	 */
	private static final class Or extends BitwiseSpliterator {

		/**
		 * Creates a {@link BitwiseSpliterator.And} that will cover indices of bits
		 * resulting in the <i>live</i> state within the specified starting and ending
		 * indices <b>position</b> and <b>end</b> after an 'or' operation..
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
			float density1 = set1.density(position, end);
			float density2 = set2.density(position, end);
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
		protected int bitwiseFunction(int word1, int word2) {
			return word1 | word2;
		}

	}

	/**
	 * An implementation of {@link BitwiseSpliterator} used to stream the indices of
	 * bits that result in the <i>live</i> state after the 'xor' operation,
	 * splitting at appropriate indices to manipulate a {@link BitSet} in parallel.
	 * Words are cached as they are encountered, so any modifications after
	 * iteration begins may not be included.
	 * 
	 * @see BitwiseSpliterator
	 */
	private static final class XOr extends BitwiseSpliterator {

		/**
		 * Creates a {@link BitwiseSpliterator.And} that will cover indices of bits
		 * resulting in the <i>live</i> state within the specified starting and ending
		 * indices <b>position</b> and <b>end</b> after an 'xor' operation..
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
			float density1 = set1.density(position, end);
			float density2 = set2.density(position, end);
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
		protected int bitwiseFunction(int word1, int word2) {
			return word1 ^ word2;
		}

	}

}