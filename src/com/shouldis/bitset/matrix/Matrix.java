package com.shouldis.bitset.matrix;

import java.io.Serializable;
import java.util.Objects;

import com.shouldis.bitset.BitSet;
import com.shouldis.bitset.ConcurrentBitSet;
import com.shouldis.bitset.ImmutableBitSet;

/**
 * 64 by 64 bit matrix backed by either a {@link BitSet},
 * {@link ConcurrentBitSet} or {@link ImmutableBitSet}.
 * 
 * @author Aaron Shouldis
 */
public final class Matrix implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * The number of bits contained in a {@link Matrix}
	 */
	public static final int MATRIX_SIZE = Long.SIZE * Long.SIZE;

	/**
	 * The {@link BitSet} storing the bits contained in this {@link BitSet}. (X, Y)
	 * coordinates are mapped to indices through {@link #index(int, int)}.
	 */
	private final BitSet bits;

	/**
	 * Internal, private constructor.
	 * 
	 * @param bits the {@link BitSet} to initialize this {@link Matrix} with.
	 * 
	 * @throws NullPointerException if <b>bits</b> is null.
	 */
	private Matrix(final BitSet bits) {
		this.bits = Objects.requireNonNull(bits);
	}

	/**
	 * Creates a {@link Matrix} backed by a {@link BitSet} with size equal to
	 * {@link #MATRIX_SIZE}. All bits are initialized to the <i>dead</I> state.
	 * 
	 * @return the created {@link Matrix}.
	 */
	public static Matrix create() {
		return new Matrix(new BitSet(MATRIX_SIZE));
	}

	/**
	 * Creates a {@link Matrix} backed by a {@link BitSet} with size equal to
	 * {@link #MATRIX_SIZE}. All bits copy the state of the bits in the specified
	 * {@link Matrix} <b>matrix</b>.
	 * 
	 * @param matrix the {@link Matrix} to copy.
	 * 
	 * @return the created {@link Matrix} copy of <b>matrix</b>.
	 */
	public static Matrix create(final Matrix matrix) {
		return new Matrix(new BitSet(matrix.bits()));
	}

	/**
	 * Creates a {@link Matrix} backed by a {@link ConcurrentBitSet} with size equal
	 * to {@link #MATRIX_SIZE}. All bits are initialized to the <i>dead</I> state.
	 * 
	 * @return the created {@link Matrix}.
	 */
	public static Matrix createConcurrent() {
		return new Matrix(new ConcurrentBitSet(MATRIX_SIZE));
	}

	/**
	 * Creates a {@link Matrix} backed by a {@link ConcurrentBitSet} with size equal
	 * to {@link #MATRIX_SIZE}. All bits copy the state of the bits in the specified
	 * {@link Matrix} <b>matrix</b>.
	 * 
	 * @param matrix the {@link Matrix} to copy.
	 * 
	 * @return the created {@link Matrix} copy of <b>matrix</b>.
	 */
	public static Matrix createConcurrent(final Matrix matrix) {
		return new Matrix(new ConcurrentBitSet(matrix.bits()));
	}

	/**
	 * Creates a {@link Matrix} backed by an {@link ImmutableBitSet} with size equal
	 * to {@link #MATRIX_SIZE}. All bits copy the state of the bits in the specified
	 * {@link Matrix} <b>matrix</b>.
	 * 
	 * @param matrix the {@link Matrix} to copy.
	 * 
	 * @return the created {@link Matrix} copy of <b>matrix</b>.
	 */
	public static Matrix createImmutable(final Matrix matrix) {
		return new Matrix(new ImmutableBitSet(matrix.bits()));
	}

	/**
	 * Resolves the x-coordinate corresponding to the specified <b>index</b>. If the
	 * index is out of the [0, {@link #MATRIX_SIZE}), the result is undefined.
	 * 
	 * @param index the bit index to map to an x-coordinate.
	 * @return the x-coordinate representation of <b>index</b>.
	 */
	public static int x(final int index) {
		return BitSet.modSize(index);
	}

	/**
	 * Resolves the y-coordinate corresponding to the specified <b>index</b>. If the
	 * index is out of the range [0, {@link #MATRIX_SIZE}), the result is undefined.
	 * 
	 * @param index the bit index to map to an y-coordinate.
	 * @return the y-coordinate representation of <b>index</b>.
	 */
	public static int y(final int index) {
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
	public static int index(final int x, final int y) {
		return x + BitSet.multiplySize(y);
	}

	/**
	 * Resolves an x-coordinate and a y-coordinate to a bit index through wrapping.
	 * {@link #wrap(int)} is used to bound the coordinates in the range [0,
	 * {@link Long#SIZE}) as if this {@link Matrix} was a torus.
	 * 
	 * @param x the x-coordinate to resolve to a bit index.
	 * @param y the y-coordinate to resolve to a bit index.
	 * @return the bit index representing the specified <b>x</b> and <b>y</b>
	 *         coordinate after using {@link #wrap(int)}.
	 */
	public static int wrapIndex(final int x, final int y) {
		return index(wrap(x), wrap(y));
	}

	/**
	 * Resolves an x-coordinate or y-coordinate to a x-coordinate or y-coordinate
	 * bounding it within the range [0, {@link Long#SIZE}) as if this {@link Matrix}
	 * was a torus.
	 * 
	 * @param coordinate the coordinate to bound within the range [0,
	 *                   {@link Long#SIZE}).
	 * @return <b>coordinate</b> after being bound within the range [0,
	 *         {@link Long#SIZE}).
	 */
	public static int wrap(int coordinate) {
		coordinate = BitSet.modSize(coordinate);
		if (coordinate < 0) {
			coordinate += Long.SIZE;
		}
		return coordinate;
	}

	/**
	 * Returns a reference to the {@link BitSet} backing this {@link Matrix}. If
	 * {@link #createConcurrent()} or {@link #createConcurrent(Matrix)} were used to
	 * create this {@link Matrix}, a {@link ConcurrentBitSet} will be returned.
	 * 
	 * @return the {@link BitSet} representing the bits of this {@link Matrix}.
	 */
	public BitSet bits() {
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
		return obj instanceof Matrix && bits.equals(((Matrix) obj).bits());
	}

}