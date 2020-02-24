package com.shouldis.bitset.parallel;

import java.util.function.IntConsumer;

import com.shouldis.bitset.BitSet;

/**
 * Implementation of {@link Biterator} that creates a stream of indices that
 * represents which bits will be <i>live</i> after performing a
 * {@link #bitwiseFunction(long, long)} between the words of 2 {@link BitSet}s
 * at the same word index. The three main operations: {@link And}, {@link Or},
 * and {@link XOr} are provided by this class, as well as their inversions.
 * 
 * @author Aaron Shouldis
 * @see Biterator
 */
public abstract class FunctionBiterator extends Biterator {

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
	 * Creates a {@link FunctionBiterator} with the specified starting and ending
	 * position. This {@link FunctionBiterator} will perform bitwise operations on
	 * the words of the specified {@link BitSet}s.
	 * 
	 * @param set1     the first {@link BitSet}, which will be operated on against
	 *                 <b>set2</b>.
	 * @param set2     the second {@link BitSet}, which will be operated on against
	 *                 <b>set1</b>.
	 * @param position (inclusive) the first index to include.
	 * @param end      (exclusive) index after the last index to include.
	 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
	 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
	 *                                  to <b>end</b>, or the sizes of <b>set1</b>
	 *                                  and <b>set2</b> differ.
	 * @throws IllegalArgumentException if <b>position</b> is less than 0.
	 */
	protected FunctionBiterator(final BitSet set1, final BitSet set2, final int position, final int end) {
		super(position, end);
		this.set1 = set1;
		this.set2 = set2;
		set1.compareSize(set2);
		if (end >= set1.size) {
			final StringBuilder builder = new StringBuilder();
			builder.append(end).append(" >= ").append(set1.size);
			throw new IllegalArgumentException(builder.toString());
		}
	}

	/**
	 * Calculates the result of the bitwise operation represented by this
	 * {@link FunctionBiterator} between the specified words from {@link #set1} and
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
		return bitwiseFunction(set1.getWord(wordIndex), set2.getWord(wordIndex));
	}

	/**
	 * Implementation of {@link FunctionBiterator} used to stream the indices of
	 * bits that result in the <i>live</i> state after the {@code AND} operation,
	 * splitting at appropriate indices to manipulate a {@link BitSet} in parallel.
	 * Words are cached as they are encountered, so any modifications after
	 * iteration begins may not be included.
	 * 
	 * @see FunctionBiterator
	 */
	public static final class And extends FunctionBiterator {

		/**
		 * Creates a {@link FunctionBiterator.And} that will cover indices of bits
		 * resulting in the <i>live</i> state within the specified starting and ending
		 * indices <b>position</b> and <b>end</b> after an {@code AND} operation.
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
		 * @throws IllegalArgumentException if <b>position</b> is less than 0.
		 */
		public And(final BitSet set1, final BitSet set2, final int position, final int end) {
			super(set1, set2, position, end);
		}

		/**
		 * Creates a {@link FunctionBiterator.And} that will cover all indices of bits
		 * resulting in the <i>live</i> state within the specified {@link BitSet}s
		 * <b>set1</b> and <b>set2</b> after an {@code AND} operation.
		 * 
		 * @param set1 The first {@link BitSet} that will be operated on against
		 *             <b>set2</b>.
		 * @param set2 The second {@link BitSet} that will be operated on against
		 *             <b>set1</b>.
		 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
		 * @throws IllegalArgumentException if the sizes of <b>set1</b> and <b>set2</b>
		 *                                  differ.
		 */
		public And(final BitSet set1, final BitSet set2) {
			this(set1, set2, 0, set1.size);
		}

		@Override
		public OfInt trySplit() {
			if (estimateSize() < THRESHOLD) {
				return null;
			}
			return new FunctionBiterator.And(set1, set2, position, position = splitIndex());
		}

		@Override
		protected long bitwiseFunction(final long word1, final long word2) {
			return word1 & word2;
		}

	}

	/**
	 * Implementation of {@link FunctionBiterator} used to stream the indices of
	 * bits that result in the <i>live</i> state after the {@code OR} operation,
	 * splitting at appropriate indices to manipulate a {@link BitSet} in parallel.
	 * Words are cached as they are encountered, so any modifications after
	 * iteration begins are not accounted for.
	 * 
	 * @see FunctionBiterator
	 */
	public static final class Or extends FunctionBiterator {

		/**
		 * Creates a {@link FunctionBiterator.Or} that will cover indices of bits
		 * resulting in the <i>live</i> state within the specified starting and ending
		 * indices <b>position</b> and <b>end</b> after an {@code OR} operation.
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
		 * @throws IllegalArgumentException if <b>position</b> is less than 0.
		 */
		public Or(final BitSet set1, final BitSet set2, final int position, final int end) {
			super(set1, set2, position, end);
		}

		/**
		 * Creates a {@link FunctionBiterator.Or} that will cover all indices of bits
		 * resulting in the <i>live</i> state within the specified {@link BitSet}s
		 * <b>set1</b> and <b>set2</b> after an {@code OR} operation.
		 * 
		 * @param set1 The first {@link BitSet} that will be operated on against
		 *             <b>set2</b>.
		 * @param set2 The second {@link BitSet} that will be operated on against
		 *             <b>set1</b>.
		 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
		 * @throws IllegalArgumentException if the sizes of <b>set1</b> and <b>set2</b>
		 *                                  differ.
		 */
		public Or(final BitSet set1, final BitSet set2) {
			this(set1, set2, 0, set1.size);
		}

		@Override
		public OfInt trySplit() {
			if (estimateSize() < THRESHOLD) {
				return null;
			}
			return new FunctionBiterator.Or(set1, set2, position, position = splitIndex());
		}

		@Override
		protected long bitwiseFunction(final long word1, final long word2) {
			return word1 | word2;
		}

	}

	/**
	 * Implementation of {@link FunctionBiterator} used to stream the indices of
	 * bits that result in the <i>live</i> state after the {@code XOR} operation,
	 * splitting at appropriate indices to manipulate a {@link BitSet} in parallel.
	 * Words are cached as they are encountered, so any modifications after
	 * iteration begins may not be included.
	 * 
	 * @see FunctionBiterator
	 */
	public static final class XOr extends FunctionBiterator {

		/**
		 * Creates a {@link FunctionBiterator.And} that will cover indices of bits
		 * resulting in the <i>live</i> state within the specified starting and ending
		 * indices <b>position</b> and <b>end</b> after an {@code XOR} operation.
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
		 * @throws IllegalArgumentException if <b>position</b> is less than 0.
		 */
		public XOr(final BitSet set1, final BitSet set2, final int position, final int end) {
			super(set1, set2, position, end);
		}

		/**
		 * Creates a {@link FunctionBiterator.XOr} that will cover all indices of bits
		 * resulting in the <i>live</i> state within the specified {@link BitSet}s
		 * <b>set1</b> and <b>set2</b> after an {@code XOR} operation.
		 * 
		 * @param set1 The first {@link BitSet} that will be operated on against
		 *             <b>set2</b>.
		 * @param set2 The second {@link BitSet} that will be operated on against
		 *             <b>set1</b>.
		 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
		 * @throws IllegalArgumentException if the sizes of <b>set1</b> and <b>set2</b>
		 *                                  differ.
		 */
		public XOr(final BitSet set1, final BitSet set2) {
			this(set1, set2, 0, set1.size);
		}

		@Override
		public OfInt trySplit() {
			if (estimateSize() < THRESHOLD) {
				return null;
			}
			return new FunctionBiterator.XOr(set1, set2, position, position = splitIndex());
		}

		@Override
		protected long bitwiseFunction(final long word1, final long word2) {
			return word1 ^ word2;
		}
	}

	/**
	 * Implementation of {@link FunctionBiterator} used to stream the indices of
	 * bits that result in the <i>dead</i> state after the {@code AND} operation,
	 * splitting at appropriate indices to manipulate a {@link BitSet} in parallel.
	 * Words are cached as they are encountered, so any modifications after
	 * iteration begins may not be included.
	 * 
	 * @see FunctionBiterator
	 */
	public static final class NotAnd extends FunctionBiterator {

		/**
		 * Creates a {@link FunctionBiterator.NotAnd} that will cover indices of bits
		 * resulting in the <i>dead</i> state within the specified starting and ending
		 * indices <b>position</b> and <b>end</b> after an {@code AND} operation.
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
		 * @throws IllegalArgumentException if <b>position</b> is less than 0.
		 */
		public NotAnd(final BitSet set1, final BitSet set2, final int position, final int end) {
			super(set1, set2, position, end);
		}

		/**
		 * Creates a {@link FunctionBiterator.NotAnd} that will cover all indices of
		 * bits resulting in the <i>dead</i> state within the specified {@link BitSet}s
		 * <b>set1</b> and <b>set2</b> after an {@code AND} operation.
		 * 
		 * @param set1 The first {@link BitSet} that will be operated on against
		 *             <b>set2</b>.
		 * @param set2 The second {@link BitSet} that will be operated on against
		 *             <b>set1</b>.
		 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
		 * @throws IllegalArgumentException if the sizes of <b>set1</b> and <b>set2</b>
		 *                                  differ.
		 */
		public NotAnd(final BitSet set1, final BitSet set2) {
			this(set1, set2, 0, set1.size);
		}

		@Override
		public OfInt trySplit() {
			if (estimateSize() < THRESHOLD) {
				return null;
			}
			return new FunctionBiterator.NotAnd(set1, set2, position, position = splitIndex());
		}

		@Override
		protected long bitwiseFunction(final long word1, final long word2) {
			return ~(word1 & word2);
		}

	}

	/**
	 * Implementation of {@link FunctionBiterator} used to stream the indices of
	 * bits that result in the <i>dead</i> state after the {@code OR} operation,
	 * splitting at appropriate indices to manipulate a {@link BitSet} in parallel.
	 * Words are cached as they are encountered, so any modifications after
	 * iteration begins are not accounted for.
	 * 
	 * @see FunctionBiterator
	 */
	public static final class NotOr extends FunctionBiterator {

		/**
		 * Creates a {@link FunctionBiterator.NotOr} that will cover indices of bits
		 * resulting in the <i>dead</i> state within the specified starting and ending
		 * indices <b>position</b> and <b>end</b> after an {@code OR} operation.
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
		 * @throws IllegalArgumentException if <b>position</b> is less than 0.
		 */
		public NotOr(final BitSet set1, final BitSet set2, final int position, final int end) {
			super(set1, set2, position, end);
		}

		/**
		 * Creates a {@link FunctionBiterator.NotOr} that will cover all indices of bits
		 * resulting in the <i>dead</i> state within the specified {@link BitSet}s
		 * <b>set1</b> and <b>set2</b> after an {@code OR} operation.
		 * 
		 * @param set1 The first {@link BitSet} that will be operated on against
		 *             <b>set2</b>.
		 * @param set2 The second {@link BitSet} that will be operated on against
		 *             <b>set1</b>.
		 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
		 * @throws IllegalArgumentException if the sizes of <b>set1</b> and <b>set2</b>
		 *                                  differ.
		 */
		public NotOr(final BitSet set1, final BitSet set2) {
			this(set1, set2, 0, set1.size);
		}

		@Override
		public OfInt trySplit() {
			if (estimateSize() < THRESHOLD) {
				return null;
			}
			return new FunctionBiterator.NotOr(set1, set2, position, position = splitIndex());
		}

		@Override
		protected long bitwiseFunction(final long word1, final long word2) {
			return ~(word1 | word2);
		}

	}

	/**
	 * Implementation of {@link FunctionBiterator} used to stream the indices of
	 * bits that result in the <i>dead</i> state after the {@code XOR} operation,
	 * splitting at appropriate indices to manipulate a {@link BitSet} in parallel.
	 * Words are cached as they are encountered, so any modifications after
	 * iteration begins may not be included.
	 * 
	 * @see FunctionBiterator
	 */
	public static final class NotXOr extends FunctionBiterator {

		/**
		 * Creates a {@link FunctionBiterator.NotXOr} that will cover indices of bits
		 * resulting in the <i>dead</i> state within the specified starting and ending
		 * indices <b>position</b> and <b>end</b> after an {@code XOR} operation.
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
		 * @throws IllegalArgumentException if <b>position</b> is less than 0.
		 */
		public NotXOr(final BitSet set1, final BitSet set2, final int position, final int end) {
			super(set1, set2, position, end);
		}

		/**
		 * Creates a {@link FunctionBiterator.XOr} that will cover all indices of bits
		 * resulting in the <i>dead</i> state within the specified {@link BitSet}s
		 * <b>set1</b> and <b>set2</b> after an {@code XOR} operation.
		 * 
		 * @param set1 The first {@link BitSet} that will be operated on against
		 *             <b>set2</b>.
		 * @param set2 The second {@link BitSet} that will be operated on against
		 *             <b>set1</b>.
		 * @throws NullPointerException     if <b>set1</b> or <b>set2</b> are null.
		 * @throws IllegalArgumentException if the sizes of <b>set1</b> and <b>set2</b>
		 *                                  differ.
		 */
		public NotXOr(final BitSet set1, final BitSet set2) {
			this(set1, set2, 0, set1.size);
		}

		@Override
		public OfInt trySplit() {
			if (estimateSize() < THRESHOLD) {
				return null;
			}
			return new FunctionBiterator.NotXOr(set1, set2, position, position = splitIndex());
		}

		@Override
		protected long bitwiseFunction(final long word1, final long word2) {
			return ~(word1 ^ word2);
		}
	}

}