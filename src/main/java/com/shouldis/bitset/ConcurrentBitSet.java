package com.shouldis.bitset;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import com.shouldis.bitset.function.WordBiFunction;
import com.shouldis.bitset.function.WordFunction;

/**
 * Implementation of {@link BitSet} in which all methods capable of reading or
 * writing the state of bits are delegated to concurrent-safe operations.
 * <p>
 * The use of atomic operations allows concurrent modification of this
 * {@link ConcurrentBitSet} without any external synchronization at the cost of
 * some performance. These operations are done by the semantics of
 * {@link VarHandle#setVolatile(Object...)}. Note that operations affecting more
 * than 1 word such as {@link #flip()} and {@link #xOr(BitSet)}, will only be
 * made atomic on a per-word basis.
 * <p>
 * Some operations can leverage {@link VarHandle} better than others -- the
 * following are performed on a perfectly atomic basis:
 * <ul>
 * <li>{@link #get(int)}</li>
 * <li>{@link #set(int)}</li>
 * <li>{@link #clear(int)}</li>
 * <li>{@link #flip(int)}</li>
 * <li>{@link #getWord(int)}</li>
 * <li>{@link #setWord(int, long)}</li>
 * <li>{@link #andWord(int, long)}</li>
 * <li>{@link #orWord(int, long)}</li>
 * <li>{@link #xOrWord(int, long)}</li>
 * <li>{@link #flipWord(int)}</li>
 * <li>{@link #fillWord(int)}</li>
 * <li>{@link #emptyWord(int)}</li>
 * </ul>
 * The other operations must use CAS operations, and can encounter memory
 * contention as a result. In these cases, all methods will retry the operation
 * until it is successful, but {@link #tryApply(int, WordFunction)} and
 * {@link #tryApply(int, WordBiFunction, long)} can instead be used to have
 * control over what happens in the event of memory contention.
 * {@link #tryAdd(int)} and {@link #tryRemove(int)} are also defined, but will
 * return {@code false} under two separate conditions: either encountering
 * memory contention, or when {@link #add(int)} or {@link #remove(int)} would
 * typically return {@code false}.
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
			if ((expected & mask) != BitSet.DEAD) {
				return false;
			}
			replacment = expected | mask;
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, replacment));
		return true;
	}

	@Override
	public boolean remove(final int index) {
		final int wordIndex = BitSet.divideSize(index);
		final long mask = ~BitSet.bitMask(index);
		long expected, replacment;
		do {
			expected = getWord(wordIndex);
			if ((expected | mask) != BitSet.LIVE) {
				return false;
			}
			replacment = expected & mask;
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, replacment));
		return true;
	}

	/**
	 * Tries to set the bit at the specified <b>index</b> to the <i>live</i> state.
	 * If it is not already the <i>live</i> state, it will be changed. Unlike
	 * {@link ConcurrentBitSet#add(int)}, this method will not retry in the event of
	 * memory contention, and will instead return {@code false}.
	 * 
	 * @param index the index of the bit to change to the <i>live</i> state.
	 * @return whether or not this {@link BitSet} was changed as a result.
	 * @throws ArrayIndexOutOfBoundsException if <b>index</b> is negative, or
	 *                                        greater than or equal to
	 *                                        {@link #size}.
	 */
	public boolean tryAdd(final int index) {
		final int wordIndex = BitSet.divideSize(index);
		final long mask = BitSet.bitMask(index);
		final long expected = getWord(wordIndex);
		return (expected & mask) == BitSet.DEAD && HANDLE.compareAndSet(words, wordIndex, expected, expected | mask);
	}

	/**
	 * Tries to set the bit at the specified <b>index</b> to the <i>dead</i> state.
	 * If it is not already in the <i>dead</i> state, it will be changed. Unlike
	 * {@link ConcurrentBitSet#remove(int)}, this method will not retry in the event
	 * of memory contention, and will instead return {@code false}.
	 * 
	 * @param index the index of the bit to change to the <i>dead</i> state.
	 * @return whether or not this {@link BitSet} was changed as a result.
	 * @throws ArrayIndexOutOfBoundsException if <b>index</b> is negative, or
	 *                                        greater than or equal to
	 *                                        {@link #size}.
	 */
	public boolean tryRemove(final int index) {
		final int wordIndex = BitSet.divideSize(index);
		final long mask = ~BitSet.bitMask(index);
		final long expected = getWord(wordIndex);
		return (expected | mask) == BitSet.LIVE && HANDLE.compareAndSet(words, wordIndex, expected, expected & mask);
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
		apply(wordIndex, WordBiFunction.NOT_AND, mask);
	}

	@Override
	public void notOrWord(final int wordIndex, final long mask) {
		apply(wordIndex, WordBiFunction.NOT_OR, mask);
	}

	@Override
	public void notXOrWord(final int wordIndex, final long mask) {
		apply(wordIndex, WordBiFunction.NOT_XOR, mask);
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

	@Override
	public void apply(final int wordIndex, final WordBiFunction function, final long mask) {
		long expected, replacment;
		do {
			expected = getWord(wordIndex);
			replacment = function.apply(expected, mask);
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, replacment));
	}

	/**
	 * Tries to apply the specified {@link WordFunction} <b>function</b> to the word
	 * at the specified <b>wordIndex</b>. While {@link #apply(int, WordFunction)}
	 * will retry in the event of memory contention, this method returns a boolean
	 * representing whether or not the operation was successful.
	 * 
	 * @param wordIndex the index within {@link #words} to apply the function to.
	 * @param function  the {@link WordFunction} to apply at the specified
	 *                  <b>wordIndex</b>.
	 * @return whether or not the operation was successfully applied, meaning it
	 *         didn't encounter any memory contention.
	 */
	public boolean tryApply(final int wordIndex, final WordFunction function) {
		final long expected = getWord(wordIndex);
		return HANDLE.compareAndSet(words, wordIndex, expected, function.apply(expected));
	}

	/**
	 * Tries to apply the specified {@link WordBiFunction} <b>function</b> to the
	 * word at the specified <b>wordIndex</b>. While
	 * {@link #apply(int, WordBiFunction, long)} will retry in the event of memory
	 * contention, this method returns a boolean representing whether or not the
	 * operation was successful.
	 * 
	 * @param wordIndex the index within {@link #words} to apply the function to.
	 * @param function  the {@link WordBiFunction} to apply at the specified
	 *                  <b>wordIndex</b> in conjunction with <b>mask</b>.
	 * @param mask      the mask to use in the specified <b>function</b>.
	 * @return whether or not the operation was successfully applied, meaning it
	 *         didn't encounter any memory contention.
	 */
	public boolean tryApply(final int wordIndex, final WordBiFunction function, final long mask) {
		final long expected = getWord(wordIndex);
		return HANDLE.compareAndSet(words, wordIndex, expected, function.apply(expected, mask));
	}

}