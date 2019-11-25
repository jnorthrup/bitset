package com.shouldis.bitset;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Pseudo-random number generator using a modified version of the XORShift64
 * algorithm to generate random values at a faster rate than
 * {@link java.util.Random}'s linear congruent generator. This generator has a
 * period of 2<sup>64</sup> -1.
 * 
 * @see DensityXOrShift
 */
public class XOrShift {

	/**
	 * Magic number value used to deterministically add entropy to seeds.
	 */
	private static final long MAGIC_NUMBER = 0xBEEFBEEFBEEFBEEFL;

	/**
	 * Atomic value used to ensure that seeds generated at similar times are
	 * sufficiently different.
	 **/
	private static final AtomicLong SEED_ENTROPY = new AtomicLong(MAGIC_NUMBER);

	/**
	 * Internal value used to store the current value held by this generator.
	 */
	private long state;

	/**
	 * Creates an instance of {@link XOrShift} initialized by the specified
	 * <b>seed</b>. The effect of this is described by {@link #setSeed(long)}.
	 * 
	 * @param state the value used to initialize the state of this generator.
	 */
	public XOrShift(long state) {
		setSeed(state);
	}

	/**
	 * Creates an instance of {@link XOrShift} with a randomly generated seed.
	 */
	public XOrShift() {
		this(generateSeed());
	}

	/**
	 * Returns the next pseudo-random, uniformly distributed long value from this
	 * random number generator's sequence.
	 * 
	 * @return the next randomly generated long.
	 */
	private final long nextRawLong() {
		long word = state;
		word ^= word << 13;
		word ^= word >>> 7;
		word ^= word << 17;
		return state = word;
	}

	/**
	 * Returns the next pseudo-random, uniformly distributed integer value from this
	 * random number generator's sequence.
	 * 
	 * @return the next randomly generated integer.
	 */
	private final int nextRawInt() {
		return (int) (nextRawLong() >>> BitSet.WORD_SIZE);
	}

	/**
	 * Returns the next pseudo-random, uniformly distributed long value from this
	 * random number generator's sequence, but shifted to ensure it is always
	 * positive.
	 * 
	 * @return the next randomly generated long.
	 */
	public final long nextPositiveLong() {
		return nextRawLong() >>> 1;
	}

	/**
	 * Returns the next pseudo-random, uniformly distributed integer value from this
	 * random number generator's sequence, but shifted to ensure it is always
	 * positive.
	 * 
	 * @return the next randomly generated integer.
	 */
	public final int nextPositiveInt() {
		return (int) (nextRawLong() >>> BitSet.WORD_SIZE + 1);
	}

	/**
	 * Returns the next pseudo-random long from this random number generator's
	 * sequence.
	 * 
	 * @return a randomly generated long.
	 */
	public long nextLong() {
		return nextRawLong();
	}

	/**
	 * Returns the next pseudo-random, uniformly distributed long value from this
	 * random number generator's sequence, which will be in the range 0 (inclusive)
	 * to <b>bound</b> (exclusive).
	 * 
	 * @param bound the upper bound of the range of generated longs.
	 * @return the next long between 0 and <b>bound</b>.
	 * @throws IllegalArgumentException if <b>bound</b> is less than 1;
	 */
	public final long nextLong(long bound) {
		if (bound < 1L) {
			throw new IllegalArgumentException(Long.toString(bound));
		}
		long random = nextRawLong();
		long mask = bound - 1L;
		if ((bound & mask) == 0L) {
			random &= mask;
		} else {
			long temp = random >>> 1;
			while (temp + mask - (random = temp % bound) < 0L) {
				temp = nextPositiveLong();
			}
		}
		return random;
	}

	/**
	 * Returns the next pseudo-random integer from this random number generator's
	 * sequence.
	 * 
	 * @return a randomly generated integer.
	 */
	public int nextInt() {
		return nextRawInt();
	}

	/**
	 * Returns the next pseudo-random, uniformly distributed integer value from this
	 * random number generator's sequence, which will be in the range 0 (inclusive)
	 * to <b>bound</b> (exclusive).
	 * 
	 * @param bound the upper bound of the range of generated integers.
	 * @return the next integer between 0 and <b>bound</b>.
	 * @throws IllegalArgumentException if <b>bound</b> is less than 1;
	 */
	public final int nextInt(int bound) {
		if (bound < 1) {
			throw new IllegalArgumentException(Integer.toString(bound));
		}
		int random = nextRawInt();
		int mask = bound - 1;
		if ((bound & mask) == 0) {
			random &= mask;
		} else {
			int temp = random >>> 1;
			while (temp + mask - (random = temp % bound) < 0) {
				temp = nextPositiveInt();
			}
		}
		return random;
	}

	/**
	 * Returns the next pseudo-random boolean from this random number generator's
	 * sequence.
	 * 
	 * @return a randomly generated boolean.
	 */
	public boolean nextBoolean() {
		return nextRawLong() < 0L;
	}

	/**
	 * Returns the next pseudo-random double from this random number generator's
	 * sequence, with values ranging from 0.0 (inclusive) to 1.0 (exclusive).
	 * 
	 * @return a randomly generated float.
	 */
	public final double nextDouble() {
		return (nextRawLong() >>> 11) * 0x1.0p-53;
	}

	/**
	 * Returns the next pseudo-random float from this random number generator's
	 * sequence, with values ranging from 0.0 (inclusive) to 1.0 (exclusive).
	 * 
	 * @return a randomly generated float.
	 */
	public final float nextFloat() {
		return (nextRawLong() >>> 40) * 0x1.0p-24f;
	}

	/**
	 * Generates a {@link BitSet} with the specified <b>size</b>, with each word
	 * filled with {@link #nextInt()}.
	 * 
	 * @param size the size of generated {@link BitSet}.
	 * @return a randomly generated {@link BitSet} with the specified <b>size</b>.
	 */
	public final BitSet nextBitSet(int size) {
		BitSet set = new BitSet(size);
		for (int i = 0; i < set.words.length; i++) {
			set.words[i] = nextInt();
		}
		return set;
	}

	/**
	 * Generates a {@link ConcurrentBitSet} with the specified <b>size</b>, with
	 * each word filled with {@link #nextInt()}.
	 * <p>
	 * This is more efficient than initializing a {@link ConcurrentBitSet} with
	 * {@link ConcurrentBitSet#randomize(XOrShift)}.
	 * 
	 * @param size the size of the generated {@link ConcurrentBitSet}.
	 * @return a randomly generated {@link ConcurrentBitSet} with the specified
	 *         <b>size</b>.
	 */
	public final ConcurrentBitSet nextConcurrentBitSet(int size) {
		ConcurrentBitSet set = new ConcurrentBitSet(size);
		for (int i = 0; i < set.words.length; i++) {
			set.words[i] = nextInt();
		}
		return set;
	}

	/**
	 * Changes the seed of this random number generator. If the specified
	 * <b>seed</b> is 0, then a random seed with be generated and used instead.
	 * 
	 * @param seed the seed used to change to state of this {@link XOrShift}.
	 */
	public final void setSeed(long seed) {
		if (seed == 0L) {
			seed = generateSeed();
		} else if (seed != MAGIC_NUMBER) {
			seed ^= MAGIC_NUMBER;
		}
		state = seed;
	}

	/**
	 * Randomly generates a seed using entropy from {@link #SEED_ENTROPY} and the
	 * {@link System#nanoTime()}.
	 * 
	 * @return a randomly generated seed.
	 */
	protected static final long generateSeed() {
		long current, next;
		do {
			current = SEED_ENTROPY.get();
			next = current * MAGIC_NUMBER;
		} while (!SEED_ENTROPY.compareAndSet(current, next));
		do {
			next ^= MAGIC_NUMBER ^ System.nanoTime();
		} while (next == 0);
		return next;
	}

}