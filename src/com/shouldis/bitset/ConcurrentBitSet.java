package com.shouldis.bitset;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Implementation of {@link BitSet} in which all methods capable of reading or
 * writing the state of bits such as {@link #getWord(int)},
 * {@link #setWord(int, long)}, {@link #andWord(int, long)},
 * {@link #orWord(int, long)}, {@link #xorWord(int, long)} are delegated to
 * atomic-operations.
 * <p>
 * The use of atomic operations allows concurrent modification of this
 * {@link ConcurrentBitSet} without any external synchronization at the cost of
 * processing time. These operations are done by the semantics of
 * {@link VarHandle#setVolatile(Object...)}.
 * <p>
 * All methods have behavior as specified by {@link BitSet}.
 * 
 * @author Aaron Shouldis
 * @see BitSet
 */
public final class ConcurrentBitSet extends BitSet {

	/**
	 * A handle on the long array methods for direct, atomic operations.
	 */
	private static final VarHandle HANDLE = MethodHandles.arrayElementVarHandle(long[].class);

	/**
	 * Creates a {@link ConcurrentBitSet} with the specified <b>size</b>.
	 * 
	 * @param size the number of indices that this {@link BitSet} will hold.
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
	 * @see BitSet#BitSet(BitSet)
	 */
	public ConcurrentBitSet(final BitSet set) {
		super(set);
	}

	@Override
	public boolean add(final int index) {
		final int wordIndex = divideSize(index);
		final long mask = bitMask(index);
		long expected, word;
		do {
			expected = getWord(wordIndex);
			if ((expected & mask) != 0L) {
				return false;
			}
			word = expected | mask;
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, word));
		return true;
	}

	@Override
	public boolean remove(final int index) {
		final int wordIndex = divideSize(index);
		final long mask = bitMask(index);
		long expected, word;
		do {
			expected = getWord(wordIndex);
			if ((expected & mask) == 0L) {
				return false;
			}
			word = expected & ~mask;
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, word));
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
	public void setWordSegment(final int wordIndex, final long word, final long mask) {
		long expected, newWord;
		do {
			expected = getWord(wordIndex);
			newWord = (mask & word) | (~mask & expected);
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, newWord));
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
	public void xorWord(final int wordIndex, final long mask) {
		HANDLE.getAndBitwiseXor(words, wordIndex, mask);
	}

	@Override
	public void notAndWord(final int wordIndex, final long mask) {
		long expected, word;
		do {
			expected = getWord(wordIndex);
			word = ~(expected & mask);
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, word));
	}

	@Override
	public void notOrWord(final int wordIndex, final long mask) {
		long expected, word;
		do {
			expected = getWord(wordIndex);
			word = ~(expected | mask);
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, word));
	}

	@Override
	public void notXOrWord(final int wordIndex, final long mask) {
		long expected, word;
		do {
			expected = getWord(wordIndex);
			word = ~(expected ^ mask);
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, word));
	}

	@Override
	public void toggleWord(final int wordIndex) {
		xorWord(wordIndex, MASK);
	}

	@Override
	public void fillWord(final int wordIndex) {
		setWord(wordIndex, MASK);
	}

	@Override
	public void emptyWord(final int wordIndex) {
		setWord(wordIndex, 0L);
	}

}