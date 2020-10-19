package com.shouldis.bitset;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Implementation of {@link BitSet} in which all methods capable of reading or
 * writing the state of bits are delegated to atomic operations.
 * <p>
 * The use of atomic operations allows concurrent modification of this
 * {@link ConcurrentBitSet} without any external synchronization at the cost of
 * processing time. These operations are done by the semantics of
 * {@link VarHandle#setVolatile(Object...)}. Note that operations affecting more
 * than 1 word such as {@link #not()} and {@link #xOr(BitSet)}, will only be
 * made atomic on a per-word basis.
 * 
 * @author Aaron Shouldis
 * @see BitSet
 */
public final class ConcurrentBitSet extends BitSet {

	private static final long serialVersionUID = 1L;

	/**
	 * A handle on the long array methods for direct, atomic operations.
	 */
	private static final VarHandle HANDLE = MethodHandles.arrayElementVarHandle(long[].class);

	/**
	 * Creates a {@link ConcurrentBitSet} with the specified <b>size</b>.
	 * 
	 * @param size the number of indices that this {@link BitSet} will hold.
	 * @throws IllegalArgumentException if <b>size</b> is less than 0.
	 * @see BitSet#BitSet(int)
	 */
	public ConcurrentBitSet(final int size) {
		super(size);
	}

	/**
	 * Creates a {@link ConcurrentBitSet} which is a clone of the specified
	 * <b>set</b>.
	 * 
	 * @param set the {@link BitSet} to copy.
	 * @throws NullPointerException if <b>set</b> is null.
	 * @see BitSet#BitSet(BitSet)
	 */
	public ConcurrentBitSet(final BitSet set) {
		super(set);
	}

	@Override
	public boolean add(final int index) {
		final int wordIndex = BitSet.divideSize(index);
		final long mask = BitSet.bitMask(index);
		long expected, replacment;
		do {
			expected = getWord(wordIndex);
			if ((expected & mask) != 0L) {
				return false;
			}
			replacment = expected | mask;
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, replacment));
		return true;
	}

	@Override
	public boolean remove(final int index) {
		final int wordIndex = BitSet.divideSize(index);
		final long mask = BitSet.bitMask(index);
		long expected, replacment;
		do {
			expected = getWord(wordIndex);
			if ((expected & mask) == 0L) {
				return false;
			}
			replacment = expected & ~mask;
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, replacment));
		return true;
	}

	@Override
	public long getWord(final int wordIndex) {
		return (long) HANDLE.getVolatile(words, wordIndex);
	}

	@Override
	public void setWord(final int wordIndex, final long word) {
		HANDLE.setVolatile(words, wordIndex, word);
	}

	@Override
	public void andWord(final int wordIndex, final long mask) {
		HANDLE.getAndBitwiseAnd(words, wordIndex, mask);
	}

	@Override
	public void orWord(final int wordIndex, final long mask) {
		HANDLE.getAndBitwiseOr(words, wordIndex, mask);
	}

	@Override
	public void xOrWord(final int wordIndex, final long mask) {
		HANDLE.getAndBitwiseXor(words, wordIndex, mask);
	}

	@Override
	public void notAndWord(final int wordIndex, final long mask) {
		long expected, replacment;
		do {
			expected = getWord(wordIndex);
			replacment = ~(expected & mask);
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, replacment));
	}

	@Override
	public void notOrWord(final int wordIndex, final long mask) {
		long expected, replacment;
		do {
			expected = getWord(wordIndex);
			replacment = ~(expected | mask);
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, replacment));
	}

	@Override
	public void notXOrWord(final int wordIndex, final long mask) {
		long expected, replacment;
		do {
			expected = getWord(wordIndex);
			replacment = ~(expected ^ mask);
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, replacment));
	}

	@Override
	public void setWordSegment(final int wordIndex, final long word, final long mask) {
		long expected, replacment;
		do {
			expected = getWord(wordIndex);
			replacment = (mask & word) | (~mask & expected);
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, replacment));
	}

	@Override
	public void apply(final int wordIndex, final WordFunction function) {
		long expected, replacment;
		do {
			expected = getWord(wordIndex);
			replacment = function.apply(expected);
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, replacment));
	}

}