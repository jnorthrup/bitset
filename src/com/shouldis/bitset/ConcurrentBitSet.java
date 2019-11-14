package com.shouldis.bitset;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class is an implementation of {@link BitSet} in which all methods
 * capable of modifying the state of bits are performed as atomic operations.
 * The use of atomic operations allows concurrent modification of this
 * {@link ConcurrentBitSet} without any external synchronization at the cost of
 * processing time.
 * <p>
 * All methods have behavior as specified by {@link BitSet}.
 * 
 * @author Aaron Shouldis
 * @see BitSet
 */
public class ConcurrentBitSet extends BitSet {

	/**
	 * A handle on the integer array methods for direct atomic operations.
	 */
	private static final VarHandle HANDLE = MethodHandles.arrayElementVarHandle(int[].class);

	/**
	 * Creates a {@link ConcurrentBitSet} with the specified <b>size</b>.
	 * 
	 * @param size the number of indices that this {@link BitSet} will hold.
	 * @see BitSet#BitSet(int)
	 */
	public ConcurrentBitSet(int size) {
		super(size);
	}

	/**
	 * Creates a {@link ConcurrentBitSet} which is a clone of the specified
	 * <b>set</b>.
	 * 
	 * @param set the {@link BitSet} to copy.
	 * @see BitSet#BitSet(BitSet)
	 */
	public ConcurrentBitSet(BitSet set) {
		super(set);
	}

	@Override
	public boolean add(int index) {
		int wordIndex = wordIndex(index);
		int mask = bitMask(index);
		int expected, word;
		do {
			expected = words[wordIndex];
			if ((expected & mask) != 0) {
				return false;
			}
			word = expected | mask;
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, word));
		return true;
	}

	@Override
	public boolean remove(int index) {
		int wordIndex = wordIndex(index);
		int mask = bitMask(index);
		int expected, word;
		do {
			expected = words[wordIndex];
			if ((expected & mask) == 0) {
				return false;
			}
			word = expected & ~mask;
		} while (!HANDLE.compareAndSet(words, wordIndex, expected, word));
		return true;
	}

	@Override
	public void set(int index) {
		atomicOr(wordIndex(index), bitMask(index));
	}

	@Override
	public void set(int from, int to) {
		if (from >= to) {
			return;
		}
		int start = wordIndex(from);
		int end = wordIndex(to - 1);
		int startMask = MASK << from;
		int endMask = MASK >>> -to;
		if (start == end) {
			atomicOr(start, startMask & endMask);
		} else {
			atomicOr(start, startMask);
			for (int i = start + 1; i < end; i++) {
				words[i] = MASK;
			}
			atomicOr(end, endMask);
		}
	}

	@Override
	public void clear(int index) {
		atomicAnd(wordIndex(index), ~bitMask(index));
	}

	@Override
	public void clear(int from, int to) {
		if (from >= to) {
			return;
		}
		int start = wordIndex(from);
		int end = wordIndex(to - 1);
		int startMask = MASK << from;
		int endMask = MASK >>> -to;
		if (start == end) {
			atomicAnd(start, ~(startMask & endMask));
		} else {
			atomicAnd(start, ~startMask);
			for (int i = start + 1; i < end; i++) {
				words[i] = 0;
			}
			atomicAnd(end, ~endMask);
		}
	}

	@Override
	public void toggle(int index) {
		atomicXOr(wordIndex(index), bitMask(index));
	}

	@Override
	public void toggle(int from, int to) {
		if (from >= to) {
			return;
		}
		int start = wordIndex(from);
		int end = wordIndex(to - 1);
		int startMask = MASK << from;
		int endMask = MASK >>> -to;
		if (start == end) {
			atomicXOr(start, startMask & endMask);
		} else {
			atomicXOr(start, startMask);
			for (int i = start + 1; i < end; i++) {
				atomicXOr(i, MASK);
			}
			atomicXOr(end, endMask);
		}
	}

	@Override
	public void randomize(int from, int to) {
		if (from >= to) {
			return;
		}
		Random random = ThreadLocalRandom.current();
		int start = wordIndex(from);
		int end = wordIndex(to - 1);
		int startMask = MASK << from;
		int endMask = MASK >>> -to;
		if (start == end) {
			atomicXOr(start, startMask & endMask & random.nextInt());
		} else {
			atomicXOr(start, startMask & random.nextInt());
			for (int i = start + 1; i < end; i++) {
				words[i] = random.nextInt();
			}
			atomicXOr(end, endMask & random.nextInt());
		}
	}

	@Override
	public void and(BitSet set) {
		compareSize(set);
		for (int i = 0; i < words.length; i++) {
			atomicAnd(i, set.words[i]);
		}
	}

	@Override
	public void or(BitSet set) {
		compareSize(set);
		for (int i = 0; i < words.length; i++) {
			atomicOr(i, set.words[i]);
		}
	}

	@Override
	public void xor(BitSet set) {
		compareSize(set);
		for (int i = 0; i < words.length; i++) {
			atomicXOr(i, set.words[i]);
		}
	}

	@Override
	public void not() {
		for (int i = 0; i < words.length; i++) {
			atomicXOr(i, MASK);
		}
	}

	@Override
	protected void cleanLastWord() {
		int hangingBits = modSize(-size);
		if (hangingBits > 0 && words.length > 0) {
			atomicAnd(words.length - 1, MASK >>> hangingBits);
		}
	}

	/**
	 * Atomically changes the integer word at <b>wordIndex</b> within {@link #words}
	 * to the result of an {@code AND} operation between the current value at the
	 * specified <b>wordIndex</b> within {@link #words} and the specified
	 * <b>mask</b>. <br>
	 * {@code words[wordIndex] &= mask;}
	 * 
	 * @param wordIndex the index within {@link #words} to perform the {@code AND}
	 *                  operation upon.
	 * @param mask      the mask to use in the {@code AND} operation on the current
	 *                  value at the specified <b>wordIndex</b>.
	 */
	private void atomicAnd(int wordIndex, int mask) {
		HANDLE.getAndBitwiseAnd(words, wordIndex, mask);
	}

	/**
	 * Atomically changes the integer word at <b>wordIndex</b> within {@link #words}
	 * to the result of an {@code OR} operation between the current value at the
	 * specified <b>wordIndex</b> within {@link #words} and the specified
	 * <b>mask</b>. <br>
	 * {@code words[wordIndex] |= mask;}
	 * 
	 * @param wordIndex the index within {@link #words} to perform the {@code OR}
	 *                  operation upon.
	 * @param mask      the mask to use in the {@code OR} operation on the current
	 *                  value at the specified <b>wordIndex</b>.
	 */
	private void atomicOr(int wordIndex, int mask) {
		HANDLE.getAndBitwiseOr(words, wordIndex, mask);
	}

	/**
	 * Atomically changes the integer word at <b>wordIndex</b> within {@link #words}
	 * to the result of an {@code XOR} operation between the current value at the
	 * specified <b>wordIndex</b> within {@link #words} and the specified
	 * <b>mask</b>. <br>
	 * {@code words[wordIndex] ^= mask;}
	 * 
	 * @param wordIndex the index within {@link #words} to perform the {@code XOR}
	 *                  operation upon.
	 * @param mask      the mask to use in the {@code XOR} operation on the current
	 *                  value at the specified <b>wordIndex</b>.
	 */
	private void atomicXOr(int wordIndex, int mask) {
		HANDLE.getAndBitwiseXor(words, wordIndex, mask);
	}

}