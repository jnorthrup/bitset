package com.shouldis.bitset.random;

import java.util.concurrent.atomic.AtomicLong;

import com.shouldis.bitset.BitSet;
import com.shouldis.bitset.ConcurrentBitSet;

/**
 * Pseudo-random number generator using a modified version of the (not
 * cryptography-secure) XORShift64 algorithm to generate uniform random numbers.
 * This generator has a period of 2<sup>64</sup> -1.
 * <p>
 * In addition to generating random numbers, this class can be used to generate
 * and randomize {@link BitSet}s, such that each bit has equal chance of being
 * in the <i>live</i> or <i>dead</i> state. {@link DensityRandom} can be used to
 * generate bits efficiently with a non-uniform chance of being in the
 * <i>live</i> or <i>dead</i> state, whereas this implementation randomizes
 * {@link BitSet}s uniformly.
 * 
 * @author Aaron Shouldis
 * @see DensityRandom
 */
public class Random {

	/**
	 * Magic number value used to generate and randomize seeds.
	 */
	private static final long MAGIC_NUMBER = 0xA33AA9B7ACAB7991L;

	/**
	 * Atomic value used to ensure that seeds generated at similar times are
	 * different. This value is changed each time a seed is generated and consumed.
	 **/
	private static final AtomicLong SEED_ENTROPY = new AtomicLong(MAGIC_NUMBER);

	/**
	 * Internal value used to store the current value held by this generator.
	 */
	private long state;

	/**
	 * Creates an instance of {@link Random} initialized by the specified
	 * <b>seed</b> such that generators with the same seed produce the same
	 * sequence. The effect of this is described by {@link #setSeed(long)}.
	 * 
	 * @param seed the value used to initialize the state of this generator.
	 */
	public Random(final long seed) {
		setSeed(seed);
	}

	/**
	 * Creates an instance of {@link Random} with a randomly generated seed. This
	 * seed will be drawn from the current value of {@link #MAGIC_NUMBER} and
	 * {@link System#nanoTime()}.
	 */
	public Random() {
		this(generateSeed());
	}

	/**
	 * Returns the next pseudo-random, uniformly distributed long value from this
	 * random number generator's sequence using the XORShift64 algorithm.
	 * 
	 * @return the next randomly generated long in the sequence.
	 */
	private final long nextRawLong() {
		long word = state;
		word ^= word << 13;
		word ^= word >>> 7;
		word ^= word << 17;
		return state = word;
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
	public final long nextLong(final long bound) {
		if (bound < 1L) {
			throw new IllegalArgumentException(Long.toString(bound));
		}
		long random = nextRawLong();
		final long mask = bound - 1L;
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
	 * Returns the next pseudo-random, uniformly distributed integer value from this
	 * random number generator's sequence.
	 * 
	 * @return the next randomly generated integer.
	 */
	private final int nextRawInt() {
		return (int) (nextRawLong() >>> Integer.SIZE);
	}

	/**
	 * Returns the next pseudo-random, uniformly distributed integer value from this
	 * random number generator's sequence shifted to ensure its always positive.
	 * 
	 * @return the next randomly generated integer.
	 */
	public final int nextPositiveInt() {
		return (int) (nextRawLong() >>> 33);
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
	public final int nextInt(final int bound) {
		if (bound < 1) {
			throw new IllegalArgumentException(Integer.toString(bound));
		}
		int random = nextRawInt();
		final int mask = bound - 1;
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
	 * filled with {@link #nextLong()}.
	 * 
	 * @param size the size of generated {@link BitSet}.
	 * @return a randomly generated {@link BitSet} with the specified <b>size</b>.
	 * @throws IllegalArgumentException if <b>size</b> is less than 0.
	 */
	public final BitSet nextBitSet(final int size) {
		final BitSet set = new BitSet(size);
		for (int i = 0; i < set.wordCount; i++) {
			set.setWord(i, nextLong());
		}
		return set;
	}

	/**
	 * Generates a {@link ConcurrentBitSet} with the specified <b>size</b>, with
	 * each word filled with {@link #nextLong()}.
	 * 
	 * @param size the size of the generated {@link ConcurrentBitSet}.
	 * @return a randomly generated {@link ConcurrentBitSet} with the specified
	 *         <b>size</b>.
	 * @throws IllegalArgumentException if <b>size</b> is less than 0.
	 */
	public final ConcurrentBitSet nextConcurrentBitSet(final int size) {
		final ConcurrentBitSet set = new ConcurrentBitSet(size);
		for (int i = 0; i < set.wordCount; i++) {
			set.setWord(i, nextLong());
		}
		return set;
	}

	/**
	 * Changes the seed of this random number generator. If the specified
	 * <b>seed</b> is 0, then a random seed with be generated and used instead.
	 * 
	 * @param seed the seed used to change to state of this {@link Random}.
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
	 * Randomly generates a seed using {@link #SEED_ENTROPY} and the
	 * {@link System#nanoTime()}.
	 * 
	 * @return a randomly generated seed.
	 */
	protected static final long generateSeed() {
		long current, next;
		do {
			next = current = SEED_ENTROPY.get();
			do {
				next ^= System.nanoTime();
			} while (next == 0L);
			next *= MAGIC_NUMBER;
		} while (!SEED_ENTROPY.compareAndSet(current, next));
		return next;
	}

}