package com.shouldis.bitset;

/**
 * Implementation of {@link BitSet} in which most methods are manually inlined
 * to increase performance by reducing the overhead from the call-stack at the
 * cost of readability. However, methods still have the same behavior as their
 * {@link BitSet} counterpart they override other than not delegating operations
 * to other methods such as {@link BitSet#bitMask(int)} and
 * {@link BitSet#setWord(int, long)}. {@link Long#bitCount(long)} intentionally
 * isn't inlined to retain existing VM optimizations.
 * 
 * @author Aaron Shouldis
 * @see BitSet
 */
public final class InlineBitSet extends BitSet {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a {@link InlineBitSet} with the specified <b>size</b>.
	 * 
	 * @param size the number of indices that this {@link InlineBitSet} will hold.
	 * @throws IllegalArgumentException if <b>size</b> is less than 0.
	 * @see BitSet#BitSet(int)
	 */
	public InlineBitSet(final int size) {
		super(size);
	}

	/**
	 * Creates a {@link InlineBitSet} which is a clone of the specified <b>set</b>.
	 * 
	 * @param set the {@link BitSet} to copy.
	 * @throws NullPointerException if <b>set</b> is null.
	 * @see BitSet#BitSet(BitSet)
	 */
	public InlineBitSet(final BitSet set) {
		super(set);
	}

	@Override
	public boolean get(final int index) {
		return (words[index >>> LOG_2_SIZE] & (1L << index)) != DEAD;
	}

	@Override
	public int get(final int from, final int to) {
		final int start = from >>> LOG_2_SIZE;
		final int end = (to - 1) << LOG_2_SIZE;
		final long startMask = LIVE << from;
		final long endMask = LIVE >>> -to;
		int sum;
		if (start == end) {
			sum = Long.bitCount(words[start] & startMask & endMask);
		} else {
			sum = Long.bitCount(words[start] & startMask);
			for (int i = start + 1; i < end; i++) {
				sum += Long.bitCount(words[i]);
			}
			sum += Long.bitCount(words[end] & endMask);
		}
		return sum;
	}

	@Override
	public void set(final int index) {
		words[index >>> LOG_2_SIZE] |= (1L << index);
	}

	@Override
	public void set(final int from, final int to) {
		final int start = from >>> LOG_2_SIZE;
		final int end = (to - 1) << LOG_2_SIZE;
		final long startMask = LIVE << from;
		final long endMask = LIVE >>> -to;
		if (start == end) {
			words[start] |= startMask & endMask;
		} else {
			words[start] |= startMask;
			for (int i = start + 1; i < end; i++) {
				words[i] = LIVE;
			}
			words[end] |= endMask;
		}
	}

	@Override
	public void clear(final int index) {
		words[index >>> LOG_2_SIZE] &= ~(1L << index);
	}

	@Override
	public void clear(final int from, final int to) {
		final int start = from >>> LOG_2_SIZE;
		final int end = (to - 1) << LOG_2_SIZE;
		final long startMask = LIVE << from;
		final long endMask = LIVE >>> -to;
		if (start == end) {
			words[start] &= ~(startMask & endMask);
		} else {
			words[start] &= ~startMask;
			for (int i = start + 1; i < end; i++) {
				words[i] = DEAD;
			}
			words[end] &= ~endMask;
		}
	}

	@Override
	public void flip(final int index) {
		words[index >>> LOG_2_SIZE] ^= (1L << index);
	}

	@Override
	public void flip(final int from, final int to) {
		final int start = from >>> LOG_2_SIZE;
		final int end = (to - 1) << LOG_2_SIZE;
		final long startMask = LIVE << from;
		final long endMask = LIVE >>> -to;
		if (start == end) {
			words[start] ^= startMask & endMask;
		} else {
			words[start] ^= startMask;
			for (int i = start + 1; i < end; i++) {
				words[i] ^= LIVE;
			}
			words[end] ^= endMask;
		}
	}

	@Override
	public boolean add(final int index) {
		final int wordIndex = index >>> LOG_2_SIZE;
		final long mask = 1L << index;
		if ((words[wordIndex] & mask) != DEAD) {
			return false;
		}
		words[wordIndex] |= mask;
		return true;
	}

	@Override
	public boolean remove(final int index) {
		final int wordIndex = index >>> LOG_2_SIZE;
		final long mask = ~(1L << index);
		if ((words[wordIndex] | mask) != LIVE) {
			return false;
		}
		words[wordIndex] &= mask;
		return true;
	}

	@Override
	public void andWord(final int wordIndex, final long mask) {
		words[wordIndex] &= mask;
	}

	@Override
	public void orWord(final int wordIndex, final long mask) {
		words[wordIndex] |= mask;
	}

	@Override
	public void xOrWord(final int wordIndex, final long mask) {
		words[wordIndex] ^= mask;
	}

	@Override
	public void notAndWord(final int wordIndex, final long mask) {
		words[wordIndex] = ~(words[wordIndex] & mask);
	}

	@Override
	public void notOrWord(final int wordIndex, final long mask) {
		words[wordIndex] = ~(words[wordIndex] | mask);
	}

	@Override
	public void notXOrWord(final int wordIndex, final long mask) {
		words[wordIndex] = ~(words[wordIndex] ^ mask);
	}

	@Override
	public void setWordSegment(final int wordIndex, final long word, final long mask) {
		words[wordIndex] = (mask & word) | (~mask & words[wordIndex]);
	}

	@Override
	public void flipWord(final int wordIndex) {
		words[wordIndex] = ~words[wordIndex];
	}

	@Override
	public void fillWord(final int wordIndex) {
		words[wordIndex] = LIVE;
	}

	@Override
	public void emptyWord(final int wordIndex) {
		words[wordIndex] = DEAD;
	}

	@Override
	public void apply(final int wordIndex, final WordFunction function) {
		words[wordIndex] = function.apply(words[wordIndex]);
	}

	@Override
	public void apply(final int wordIndex, final WordBiFunction function, final long mask) {
		words[wordIndex] = function.apply(words[wordIndex], mask);
	}

}