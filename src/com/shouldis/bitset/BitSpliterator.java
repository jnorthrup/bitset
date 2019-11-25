package com.shouldis.bitset;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Implementation of {@link Spliterator} that is meant to be
 * used in conjunction with {@link BitSet}s. This class allows for parallel
 * processing on the indices of a {@link BitSet} such that those operations will
 * never enter race conditions on the underlying integers in
 * {@link BitSet#words}. This behavior can only be guaranteed if the indices
 * returned by the {@link BitSpliterator} are manipulated, and not some offset
 * or translation. This bounding of indices to specific threads is needed
 * because of the non-atomic nature of modifying integers. Using a generic
 * parallel {@link IntStream} would cause changes to the underlying integer
 * words of a {@link BitSet} to be potentially overridden by other threads.
 * <p>
 * This class offers 4 implementations:
 * <ul>
 * <li>{@link BitSpliterator.Array}</li>
 * <li>{@link BitSpliterator.Range}</li>
 * <li>{@link BitSpliterator.Live}</li>
 * <li>{@link BitSpliterator.Dead}</li>
 * </ul>
 *
 * @author Aaron Shouldis
 * @see BitSet
 */
public abstract class BitSpliterator implements Spliterator.OfInt {

	/**
	 * The minimum threshold of remaining indices for which a {@link BitSpliterator}
	 * will refuse a call to {@link Spliterator#trySplit()}.
	 */
	protected static final int THRESHOLD = BitSet.WORD_SIZE << 2;

	/**
	 * The next index this {@link BitSpliterator} will produce.
	 */
	protected int position;

	/**
	 * The boundary index that this {@link BitSpliterator} will stop upon reaching.
	 */
	protected final int end;
	
	/**
	 * Creates a {@link BitSpliterator} with the specified starting and ending
	 * position.
	 * 
	 * @param position (inclusive) the first index to include.
	 * @param end      (exclusive) index after the last index to include.
	 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
	 *                                  to <b>end</b>.
	 */
	protected BitSpliterator(int position, int end) {
		this.position = position;
		this.end = end;
		if (position >= end) {
			StringBuilder builder = new StringBuilder();
			builder.append(position).append(" >= ").append(end);
			throw new IllegalArgumentException(builder.toString());
		}
	}

	/**
	 * Returns a stream operating off of this {@link BitSpliterator}. Defaulting to
	 * serial, {@link Stream#parallel()} may be called to safely process the indices
	 * produced in parallel safely.
	 * 
	 * @return a stream representation of this {@link BitSpliterator}.
	 */
	public final IntStream stream() {
		return StreamSupport.intStream(this, false);
	}

	/**
	 * Calculates an appropriate place to split this {@link BitSpliterator}
	 * considering the current {@link #position} and {@link #end}.
	 * 
	 * @return an appropriate place to split this {@link BitSpliterator}
	 */
	protected int splitIndex() {
		int middle = middle();
		return middle - BitSet.modSize(middle);
	}

	/**
	 * Calculates the index in the middle index of this {@link BitSpliterator} using
	 * the current {@link #position} and {@link #end}. Performs calculations such
	 * that there cannot be an overflow due to large integers.
	 * 
	 * @return the middle index of this {@link BitSpliterator}.
	 */
	protected int middle() {
		int middle = (position >> 1) + (end >> 1);
		return middle + (((position % 2) + (end % 2)) >> 1);
	}

	/**
	 * Calculates the index of the next <i>live</i> bit within a specified
	 * <b>word</b> that is at the specified <b>wordIndex</b> within
	 * {@link BitSet#words}. This index will represent its bit index in the
	 * underlying integer array as well as the offset within the integer word.
	 * 
	 * @param word      the integer word to be checked for a <i>live</i> bit.
	 * @param wordIndex the index of the word within {@link BitSet#words}.
	 * @return the index of the next <i>live</i> bit within the specified word, or
	 *         {@link #end} if none are found.
	 */
	protected int nextLiveBit(int word, int wordIndex) {
		int index = BitSet.wordStart(wordIndex) + Integer.numberOfTrailingZeros(word);
		return index < end ? index : end;
	}

	@Override
	public long estimateSize() {
		return end - position;
	}

	@Override
	public int characteristics() {
		return SIZED | SUBSIZED | DISTINCT | ORDERED | IMMUTABLE | NONNULL;
	}

	/**
	 * Implementation of {@link BitSpliterator} used to stream all values within
	 * a specified integer array representing indices in an order appropriate to
	 * manipulate a {@link BitSet} in parallel. The contents of that specified
	 * arrays must be in ascending order; behavior is undefined otherwise.
	 * 
	 * @see BitSpliterator
	 */
	public static final class Array extends BitSpliterator {

		/**
		 * The indices to be processed by this {@link BitSpliterator.Array}.
		 */
		private final int[] items;

		/**
		 * Creates a {@link BitSpliterator.Array} with the specified starting and ending
		 * indices <b>position</b> and <b>end</b>. The values within the specified range
		 * of indices within the array <b>items</b> will be processed.
		 * 
		 * @param items    the array of indices to be processed.
		 * @param position (inclusive) the first index to include.
		 * @param end      (exclusive) the index after the last index to include.
		 * @throws NullPointerException     if <b>items</b> is null.
		 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
		 *                                  to <b>end</b>.
		 */
		public Array(int[] items, int position, int end) {
			super(position, end);
			this.items = Objects.requireNonNull(items);
		}

		/**
		 * Creates a {@link BitSpliterator.Array} covering the entirety of the specified
		 * array. The values within the specified array <b>items</b> will be processed.
		 * 
		 * @param items the array of indices to be processed.
		 * @throws NullPointerException if <b>items</b> is null.
		 */
		public Array(int[] items) {
			this(items, 0, items.length);
		}

		@Override
		public Spliterator.OfInt trySplit() {
			if (estimateSize() < THRESHOLD) {
				return null;
			}
			return new BitSpliterator.Array(items, position, position = splitIndex());
		}

		@Override
		public boolean tryAdvance(IntConsumer action) {
			if (position < end) {
				action.accept(items[position++]);
				return true;
			}
			return false;
		}

		@Override
		public void forEachRemaining(IntConsumer action) {
			while (position < end) {
				action.accept(items[position++]);
			}
		}

		@Override
		protected int splitIndex() {
			int middle = middle();
			int wordIndex = BitSet.wordIndex(items[middle++]);
			while (BitSet.wordIndex(items[middle]) <= wordIndex) {
				middle++;
			}
			return middle;
		}
	}

	/**
	 * Implementation of {@link BitSpliterator} used to stream all indices of a
	 * specified range, splitting at appropriate indices to manipulate a
	 * {@link BitSet} in parallel.
	 * 
	 * @see BitSpliterator
	 */
	public static final class Range extends BitSpliterator {

		/**
		 * Creates a {@link BitSpliterator.Range} with the specified starting and ending
		 * at the specified <b>position</b> and <b>end</b> indices.
		 * 
		 * @param position (inclusive) the first index to include.
		 * @param end      (exclusive) the index after the last index to include.
		 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
		 *                                  to <b>end</b>.
		 */
		public Range(int position, int end) {
			super(position, end);
		}

		@Override
		public Spliterator.OfInt trySplit() {
			if (estimateSize() < THRESHOLD) {
				return null;
			}
			return new BitSpliterator.Range(position, position = splitIndex());
		}

		@Override
		public boolean tryAdvance(IntConsumer action) {
			if (position < end) {
				action.accept(position++);
				return true;
			}
			return false;
		}

		@Override
		public void forEachRemaining(IntConsumer action) {
			while (position < end) {
				action.accept(position++);
			}
		}
	}

	/**
	 * Implementation of {@link BitSpliterator} used to stream the indices of
	 * <i>live</i> bits within a {@link BitSet}, splitting at appropriate indices to
	 * manipulate a {@link BitSet} in parallel. Words are cached as they are
	 * encountered, so any modifications after iteration begins may not be included.
	 * 
	 * @see BitSpliterator
	 */
	public static final class Live extends BitSpliterator {

		/**
		 * The {@link BitSet} that the <i>live</i> bit indices will be calculated from.
		 */
		private final BitSet set;

		/**
		 * Creates a {@link BitSpliterator.Live} that will cover all <i>live</i> bits
		 * within <b>set</b> in the specified starting and ending indices
		 * <b>position</b> and <b>end</b>.
		 * 
		 * @param set      The {@link BitSet} that the <i>live</i> bit indices will be
		 *                 calculated from.
		 * @param position (inclusive) the first index to include.
		 * @param end      (exclusive) the index after the last index to include.
		 * @throws NullPointerException     if <b>set</b> is null.
		 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
		 *                                  to <b>end</b>.
		 */
		public Live(BitSet set, int position, int end) {
			super(position, end);
			this.set = Objects.requireNonNull(set);
		}

		/**
		 * Creates a {@link BitSpliterator.Live} that will cover all <i>live</i> bits
		 * within the specified {@link BitSet} <b>set</b>.
		 * 
		 * @param set The {@link BitSet} that the <i>live</i> bit indices will be
		 *            calculated from.
		 * @throws NullPointerException if <b>set</b> is null.
		 */
		public Live(BitSet set) {
			this(set, 0, set.size);
		}

		@Override
		public long estimateSize() {
			return set.get(position, end);
		}

		@Override
		public Spliterator.OfInt trySplit() {
			if (estimateSize() < THRESHOLD) {
				return null;
			}
			return new BitSpliterator.Live(set, position, position = splitIndex());
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
			int word = set.words[wordIndex] & (BitSet.MASK << position);
			do {
				action.accept(position);
				word ^= Integer.lowestOneBit(word);
				while (word == 0) {
					if (wordIndex == lastWordIndex) {
						position = end;
						return;
					}
					word = set.words[++wordIndex];
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
		private int next(int index) {
			int wordIndex = BitSet.wordIndex(index);
			int lastWordIndex = BitSet.wordIndex(end - 1);
			if (index >= end) {
				return end;
			}
			int word = set.words[wordIndex] & (BitSet.MASK << index);
			if (word != 0) {
				return nextLiveBit(word, wordIndex);
			}
			while (++wordIndex <= lastWordIndex) {
				word = set.words[wordIndex];
				if (word != 0) {
					return nextLiveBit(word, wordIndex);
				}
			}
			return end;
		}
	}

	/**
	 * Implementation of {@link BitSpliterator} used to stream the indices of
	 * <i>dead</i> bits within a {@link BitSet}, splitting at appropriate indices to
	 * manipulate a {@link BitSet} in parallel. Words are cached as they are
	 * encountered, so any modifications after iteration begins may not be included.
	 * 
	 * @see BitSpliterator
	 */
	public static final class Dead extends BitSpliterator {

		/**
		 * The {@link BitSet} that the <i>dead</i> bit indices will be calculated from.
		 */
		private final BitSet set;

		/**
		 * Creates a {@link BitSpliterator.Dead} that will cover all <i>dead</i> bits
		 * within the specified starting and ending indices <b>position</b> and
		 * <b>end</b>.
		 * 
		 * @param set      The {@link BitSet} that the <i>dead</i> bit indices will be
		 *                 calculated from.
		 * @param position (inclusive) the first index to include.
		 * @param end      (exclusive) the index after the last index to include.
		 * @throws NullPointerException     if <b>set</b> is null.
		 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
		 *                                  to <b>end</b>.
		 */
		public Dead(BitSet set, int position, int end) {
			super(position, end);
			this.set = Objects.requireNonNull(set);
		}

		/**
		 * Creates a {@link BitSpliterator.Dead} that will cover all <i>dead</i> bits
		 * within the specified {@link BitSet} <b>set</b>.
		 * 
		 * @param set The {@link BitSet} that the <i>dead</i> bit indices will be
		 *            calculated from.
		 * @throws NullPointerException if <b>set</b> is null.
		 */
		public Dead(BitSet set) {
			this(set, 0, set.size);
		}

		@Override
		public long estimateSize() {
			return super.estimateSize() - set.get(position, end);
		}

		@Override
		public Spliterator.OfInt trySplit() {
			if (estimateSize() < THRESHOLD) {
				return null;
			}
			return new BitSpliterator.Dead(set, position, position = splitIndex());
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
			int word = ~set.words[wordIndex] & (BitSet.MASK << position);
			do {
				action.accept(position);
				word ^= Integer.lowestOneBit(word);
				while (word == 0) {
					if (wordIndex == lastWordIndex) {
						position = end;
						return;
					}
					word = ~set.words[++wordIndex];
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
		private int next(int index) {
			int wordIndex = BitSet.wordIndex(index);
			int lastWordIndex = BitSet.wordIndex(end - 1);
			if (index >= end) {
				return end;
			}
			int word = ~set.words[wordIndex] & (BitSet.MASK << index);
			if (word != 0) {
				return nextLiveBit(word, wordIndex);
			}
			while (++wordIndex <= lastWordIndex) {
				word = ~set.words[wordIndex];
				if (word != 0) {
					return nextLiveBit(word, wordIndex);
				}
			}
			return end;
		}

	}

}