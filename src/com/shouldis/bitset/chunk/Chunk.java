package com.shouldis.bitset.chunk;

import java.io.Serializable;
import java.util.Objects;

import com.shouldis.bitset.BitSet;
import com.shouldis.bitset.ConcurrentBitSet;

/**
 * 64 by 64 bit matrix backed by either a {@link BitSet} or
 * {@link ConcurrentBitSet}.
 * 
 * @author Aaron Shouldis
 */
public final class Chunk implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * The number of bits contained in a {@link Chunk}
	 */
	public static final int CHUNK_SIZE = Long.SIZE * Long.SIZE;

	/**
	 * The {@link BitSet} storing the bits contained in this {@link BitSet}. (X, Y)
	 * coordinates are mapped to indices through {@link #index(int, int)}.
	 */
	private final BitSet bits;

	/**
	 * Internal, private constructor.
	 * 
	 * @param bits the {@link BitSet} to initialize this {@link Chunk} with.
	 * 
	 * @throws NullPointerException if <b>bits</b> is null.
	 */
	private Chunk(final BitSet bits) {
		this.bits = Objects.requireNonNull(bits);
	}

	/**
	 * Creates a {@link Chunk} backed by a {@link BitSet} with size equal to
	 * {@link #CHUNK_SIZE}. All bits are initialized to the <i>dead</I> state.
	 * 
	 * @return the created {@link Chunk}.
	 */
	public static final Chunk create() {
		return new Chunk(new BitSet(CHUNK_SIZE));
	}

	/**
	 * Creates a {@link Chunk} backed by a {@link BitSet} with size equal to
	 * {@link #CHUNK_SIZE}. All bits copy the state of the bits in the specified
	 * {@link Chunk} <b>chunk</b>.
	 * 
	 * @param chunk the {@link Chunk} to copy.
	 * 
	 * @return the created {@link Chunk} copy of <b>chunk</b>.
	 */
	public static final Chunk create(final Chunk chunk) {
		return new Chunk(chunk.bits().copy());
	}

	/**
	 * Creates a {@link Chunk} backed by a {@link ConcurrentBitSet} with size equal
	 * to {@link #CHUNK_SIZE}. All bits are initialized to the <i>dead</I> state.
	 * 
	 * @return the created {@link Chunk}.
	 */
	public static final Chunk createConcurrent() {
		return new Chunk(new ConcurrentBitSet(CHUNK_SIZE));
	}

	/**
	 * Creates a {@link Chunk} backed by a {@link ConcurrentBitSet} with size equal
	 * to {@link #CHUNK_SIZE}. All bits copy the state of the bits in the specified
	 * {@link Chunk} <b>chunk</b>.
	 * 
	 * @param chunk the {@link Chunk} to copy.
	 * 
	 * @return the created {@link Chunk} copy of <b>chunk</b>.
	 */
	public static final Chunk createConcurrent(final Chunk chunk) {
		return new Chunk(chunk.bits().concurrentCopy());
	}

	/**
	 * Resolves the x-coordinate corresponding to the specified <b>index</b>. If the
	 * index is out of the [0, {@link #CHUNK_SIZE}), the result is undefined.
	 * 
	 * @param index the bit index to map to an x-coordinate.
	 * @return the x-coordinate representation of <b>index</b>.
	 */
	public static final int x(final int index) {
		return BitSet.modSize(index);
	}

	/**
	 * Resolves the y-coordinate corresponding to the specified <b>index</b>. If the
	 * index is out of the range [0, {@link #CHUNK_SIZE}), the result is undefined.
	 * 
	 * @param index the bit index to map to an y-coordinate.
	 * @return the y-coordinate representation of <b>index</b>.
	 */
	public static final int y(final int index) {
		return BitSet.divideSize(index);
	}

	/**
	 * Resolves an x-coordinate and a y-coordinate to a bit index.
	 * 
	 * @param x the x-coordinate to resolve to a bit index.
	 * @param y the y-coordinate to resolve to a bit index.
	 * @return the bit index representing the specified <b>x</b> and <b>y</b>
	 *         coordinate.
	 */
	public static final int index(final int x, final int y) {
		return x + BitSet.multiplySize(y);
	}

	/**
	 * Resolves an x-coordinate and a y-coordinate to a bit index through wrapping.
	 * {@link #wrap(int)} is used to bound the coordinates in the range [0,
	 * {@link Long#SIZE}) as if this {@link Chunk} was a torus.
	 * 
	 * @param x the x-coordinate to resolve to a bit index.
	 * @param y the y-coordinate to resolve to a bit index.
	 * @return the bit index representing the specified <b>x</b> and <b>y</b>
	 *         coordinate after using {@link #wrap(int)}.
	 */
	public static final int wrapIndex(final int x, final int y) {
		return index(wrap(x), wrap(y));
	}

	/**
	 * Resolves an x-coordinate or y-coordinate to a x-coordinate or y-coordinate
	 * bounding it within the range [0, {@link Long#SIZE}) as if this {@link Chunk}
	 * was a torus.
	 * 
	 * @param coordinate the coordinate to bound within the range [0,
	 *                   {@link Long#SIZE}).
	 * @return <b>coordinate</b> after being bound within the range [0,
	 *         {@link Long#SIZE}).
	 */
	public static final int wrap(int coordinate) {
		coordinate = BitSet.modSize(coordinate);
		if (coordinate < 0) {
			coordinate += Long.SIZE;
		}
		return coordinate;
	}

	/**
	 * Returns a reference to the {@link BitSet} backing this {@link Chunk}. If
	 * {@link #createConcurrent()} or {@link #createConcurrent(Chunk)} were used to
	 * create this {@link Chunk}, a {@link ConcurrentBitSet} will be returned.
	 * 
	 * @return the {@link BitSet} representing the bits of this {@link Chunk}.
	 */
	public final BitSet bits() {
		return bits;
	}

	@Override
	public String toString() {
		return bits.toString();
	}

	@Override
	public int hashCode() {
		return bits.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		return obj instanceof Chunk && bits.equals(((Chunk) obj).bits());
	}

}