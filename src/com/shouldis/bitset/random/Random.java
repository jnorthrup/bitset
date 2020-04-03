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
	 * Changes the state of all bits in the specified range within the specified
	 * {@link BitSet} <b>set</b> randomly according to the density of this
	 * {@link Random}. In contrast to {@link #xOrRandomize(BitSet)} and
	 * {@link #xOrRandomize(BitSet, int, int)}, this method preserves that density.
	 * When using a uniformly distributed {@link Random} (as opposed to
	 * {@link DensityRandom}), this is unnecessary. No action is taken if
	 * <b>from</b> is greater than or equal to <b>to</b>.
	 * 
	 * @param set  the {@link BitSet} to randomize.
	 * @param from (inclusive) the index of the first bit to be randomized.
	 * @param to   (exclusive) the end of the range of bits to be randomized.
	 * @throws ArrayIndexOutOfBoundsException if <b>from</b> or <b>to</b> are
	 *                                        outside of the range 0 to
	 *                                        {@link BitSet#size}.
	 * @throws NullPointerException           if <b>set</b> is null.
	 * @see DensityRandom
	 */
	public final void randomize(final BitSet set, final int from, final int to) {
		if (from >= to) {
			return;
		}
		final int start = BitSet.divideSize(from);
		final int end = BitSet.divideSize(to - 1);
		final long startMask = BitSet.MASK << from;
		final long endMask = BitSet.MASK >>> -to;
		if (start == end) {
			set.setWordSegment(start, nextLong(), startMask & endMask);
		} else {
			set.setWordSegment(start, nextLong(), startMask);
			for (int i = start + 1; i < end; i++) {
				set.setWord(i, nextLong());
			}
			set.setWordSegment(end, nextLong(), endMask);
		}
	}

	/**
	 * Changes the state of all bits in the specified {@link BitSet} <b>set</b>
	 * randomly according to the density of this {@link Random}. In contrast to
	 * {@link #xOrRandomize(BitSet)} and {@link #xOrRandomize(BitSet, int, int)},
	 * this method preserves that density. When using a uniformly distributed
	 * {@link Random} (as opposed to {@link DensityRandom}), this is unnecessary.
	 * 
	 * @param set the instance of {@link BitSet} to randomize.
	 * @throws NullPointerException if <b>set</b> is null.
	 * @see DensityRandom
	 */
	public final void randomize(final BitSet set) {
		for (int i = 0; i < set.wordCount; i++) {
			set.setWord(i, nextLong());
		}
	}

	/**
	 * Performs an {@code XOR} operation on all bits in the specified range against
	 * random bits generated by this {@link Random}. When using a non-uniformly
	 * distributed {@link Random}, namely {@link DensityRandom}, the resulting
	 * density of the {@link BitSet} <b>set</b> can be estimated with the function:
	 * <br>
	 * {@code density(a) + density(b) - (2 * density(a) * density(b))} <br>
	 * No action is taken if <b>from</b> is greater than or equal to <b>to</b>.
	 * 
	 * @param set  the instance of {@link BitSet} to randomize.
	 * @param from (inclusive) the index of the first bit to be randomized.
	 * @param to   (exclusive) the end of the range of bits to be randomized.
	 * @throws ArrayIndexOutOfBoundsException if <b>from</b> or <b>to</b> are
	 *                                        outside of the range 0 to
	 *                                        {@link BitSet#size}.
	 * @throws NullPointerException           if <b>set</b> is null.
	 * @see DensityRandom
	 */
	public final void xOrRandomize(final BitSet set, final int from, final int to) {
		if (from >= to) {
			return;
		}
		final int start = BitSet.divideSize(from);
		final int end = BitSet.divideSize(to - 1);
		final long startMask = BitSet.MASK << from;
		final long endMask = BitSet.MASK >>> -to;
		if (start == end) {
			set.xOrWord(start, nextLong() & startMask & endMask);
		} else {
			set.xOrWord(start, nextLong() & startMask);
			for (int i = start + 1; i < end; i++) {
				set.xOrWord(i, nextLong());
			}
			set.xOrWord(end, nextLong() & endMask);
		}
	}

	/**
	 * Performs an {@code XOR} operation on all bits against random bits in the
	 * specified {@link BitSet} <b>set</b>. When using a non-uniformly distributed
	 * {@link Random}, namely {@link DensityRandom}, the resulting density of this
	 * {@link BitSet} can be estimated with the function: <br>
	 * {@code density(a) + density(b) - (2 * density(a) * density(b))}
	 * 
	 * @param set the instance of {@link BitSet} to randomize.
	 * @throws NullPointerException if <b>set</b> is null.
	 * @see DensityRandom
	 */
	public final void xOrRandomize(final BitSet set) {
		for (int i = 0; i < set.wordCount; i++) {
			set.xOrWord(i, nextLong());
		}
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