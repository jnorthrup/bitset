package com.shouldis.bitset.chunk;

import com.shouldis.bitset.BitSet;
import com.shouldis.bitset.ConcurrentBitSet;
import com.shouldis.bitset.ImmutableBitSet;
import com.shouldis.bitset.WordFunction;

/**
 * Functional interface used to apply functions to a {@link Chunk}.
 * 
 * @author Aaron Shouldis
 */
public interface ChunkFunction {

	/**
	 * Function used to perform operations on a {@link Chunk}.
	 * 
	 * @param chunk the {@link BitSet} to manipulate.
	 * @return the manipulated <b>chunk</b>.
	 */
	public Chunk apply(final Chunk chunk);

	/**
	 * {@link ChunkFunction} that returns a copy of the specified {@link Chunk}
	 * backed by a {@link BitSet} using {@link Chunk#create(Chunk)}.
	 */
	public static final ChunkFunction COPY = Chunk::create;

	/**
	 * {@link ChunkFunction} that returns a copy of the specified {@link Chunk}
	 * backed by a {@link ConcurrentBitSet} using
	 * {@link Chunk#createConcurrent(Chunk)}.
	 */
	public static final ChunkFunction CONCURRENT_COPY = Chunk::createConcurrent;

	/**
	 * {@link ChunkFunction} that returns a copy of the specified {@link Chunk}
	 * backed by an {@link ImmutableBitSet} using
	 * {@link Chunk#createImmutable(Chunk)}.
	 */
	public static final ChunkFunction IMMUTABLE_COPY = Chunk::createImmutable;

	/**
	 * {@link ChunkFunction} that performs a transpose operation on the specified
	 * {@link Chunk}.
	 */
	public static final ChunkFunction TRANSPOSE = (final Chunk chunk) -> {
		final BitSet bits = chunk.bits();
		long xOrMask, mask = BitSet.MASK >>> Integer.SIZE;
		for (int blockSize = Integer.SIZE; blockSize != 0; mask ^= mask << (blockSize >>>= 1)) {
			for (int i = 0; i < Long.SIZE; i = ((i | blockSize) + 1) & ~blockSize) {
				xOrMask = mask & (bits.getWord(i) ^ (bits.getWord(i | blockSize) >>> blockSize));
				bits.xOrWord(i, xOrMask);
				bits.xOrWord(i | blockSize, xOrMask << blockSize);
			}
		}
		return chunk;
	};

	/**
	 * {@link ChunkFunction} that flips the x-coordinate of all bits in the
	 * specified {@link Chunk}, mirroring it along the y-axis.
	 */
	public static final ChunkFunction FLIP_X = (final Chunk chunk) -> {
		final BitSet bits = chunk.bits();
		for (int i = 0; i < Long.SIZE; i++) {
			bits.apply(i, WordFunction.REVERSE);
		}
		return chunk;
	};

	/**
	 * {@link ChunkFunction} that flips the y-coordinate of all bits in the
	 * specified {@link Chunk}, mirroring it along the x-axis.
	 */
	public static final ChunkFunction FLIP_Y = (final Chunk chunk) -> {
		final BitSet bits = chunk.bits();
		long swap;
		for (int i = 0; i < Integer.SIZE; i++) {
			swap = bits.getWord(i);
			bits.setWord(i, bits.getWord(63 - i));
			bits.setWord(63 - i, swap);
		}
		return chunk;
	};

	/**
	 * {@link ChunkFunction} that flips the x-coordinate and y-coordinate of all
	 * bits in the specified {@link Chunk}, effectively rotating it 180 degrees.
	 */
	public static final ChunkFunction FLIP = combine(FLIP_X, FLIP_Y);

	/**
	 * {@link ChunkFunction} that rotates the specified {@link Chunk} 90 degrees to
	 * the right (clockwise).
	 */
	public static final ChunkFunction ROTATE_R = combine(TRANSPOSE, FLIP_Y);

	/**
	 * {@link ChunkFunction} that rotates the specified {@link Chunk} 90 degrees to
	 * the left (counter-clockwise).
	 */
	public static final ChunkFunction ROTATE_L = combine(TRANSPOSE, FLIP_X);

	/**
	 * Creates a {@link ChunkFunction} which performs the two specified
	 * {@link ChunkFunction}s <b>first</b> and <b>second</b> in sequence.
	 * 
	 * @param first  the first {@link ChunkFunction} to be applied to the word
	 *               argument.
	 * @param second the second {@link ChunkFunction} to be applied to the result of
	 *               <b>first</b>.
	 * @return a {@link ChunkFunction} which applies both <b>first</b> and
	 *         <b>second</b>.
	 * 
	 * @throws NullPointerException if <b>first</b> or <b>second</b> are null.
	 */
	public static ChunkFunction combine(final ChunkFunction first, final ChunkFunction second) {
		return (final Chunk chunk) -> {
			return second.apply(first.apply(chunk));
		};
	}

	/**
	 * Creates a {@link ChunkFunction} which performs the specified array of
	 * {@link ChunkFunction}s <b>functions</b> in sequence starting at index 0.
	 * 
	 * @param functions the list of {@link ChunkFunction}s to sequence.
	 * @return a sequenced {@link ChunkFunction} containing each of the
	 *         {@link ChunkFunction}s in <b>functions</b>.
	 * 
	 * @throws NullPointerException if <b>functions</b>, or any of the
	 *                              {@link ChunkFunction}s in <b>functions</b> are
	 *                              null.
	 */
	public static ChunkFunction sequence(final ChunkFunction... functions) {
		ChunkFunction aggregate = functions[0];
		for (int i = 1; i < functions.length; i++) {
			aggregate = combine(aggregate, functions[i]);
		}
		return aggregate;
	}

}