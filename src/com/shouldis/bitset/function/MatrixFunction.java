package com.shouldis.bitset.function;

import java.util.Objects;

import com.shouldis.bitset.BitSet;
import com.shouldis.bitset.ConcurrentBitSet;
import com.shouldis.bitset.ImmutableBitSet;
import com.shouldis.bitset.InlineBitSet;
import com.shouldis.bitset.Matrix;

/**
 * Functional interface used to apply functions to a {@link Matrix}.
 * 
 * @author Aaron Shouldis
 */
public interface MatrixFunction {

	/**
	 * Function used to perform operations on a {@link Matrix}.
	 * 
	 * @param matrix the {@link Matrix} to manipulate.
	 * @return the manipulated <b>matrix</b>.
	 */
	public Matrix apply(Matrix matrix);

	/**
	 * {@link MatrixFunction} that returns a copy of the specified {@link Matrix}
	 * backed by a {@link BitSet} using {@link Matrix#create(Matrix)}.
	 */
	public static final MatrixFunction COPY = Matrix::create;

	/**
	 * {@link MatrixFunction} that returns a copy of the specified {@link Matrix}
	 * backed by a {@link ConcurrentBitSet} using
	 * {@link Matrix#createConcurrent(Matrix)}.
	 */
	public static final MatrixFunction CONCURRENT_COPY = Matrix::createConcurrent;

	/**
	 * {@link MatrixFunction} that returns a copy of the specified {@link Matrix}
	 * backed by an {@link ImmutableBitSet} using
	 * {@link Matrix#createImmutable(Matrix)}.
	 */
	public static final MatrixFunction IMMUTABLE_COPY = Matrix::createImmutable;

	/**
	 * {@link MatrixFunction} that returns a copy of the specified {@link Matrix}
	 * backed by an {@link InlineBitSet} using {@link Matrix#createInline(Matrix)}.
	 */
	public static final MatrixFunction INLINE_COPY = Matrix::createInline;

	/**
	 * {@link MatrixFunction} that performs a transpose operation on the specified
	 * {@link Matrix}.
	 */
	public static final MatrixFunction TRANSPOSE = (final Matrix matrix) -> {
		long xOrMask, mask = BitSet.LIVE >>> Integer.SIZE;
		final BitSet bits = matrix.bits();
		for (int blockSize = Integer.SIZE; blockSize != 0; mask ^= mask << (blockSize >>>= 1)) {
			for (int i = 0; i < Long.SIZE; i = ((i | blockSize) + 1) & ~blockSize) {
				xOrMask = mask & (bits.getWord(i) ^ (bits.getWord(i | blockSize) >>> blockSize));
				bits.xOrWord(i, xOrMask);
				bits.xOrWord(i | blockSize, xOrMask << blockSize);
			}
		}
		return matrix;
	};

	/**
	 * {@link MatrixFunction} that flips the x-coordinate of all bits in the
	 * specified {@link Matrix}, mirroring it along the y-axis.
	 */
	public static final MatrixFunction FLIP_X = MatrixFunction.of(WordFunction.REVERSE);

	/**
	 * {@link MatrixFunction} that flips the y-coordinate of all bits in the
	 * specified {@link Matrix}, mirroring it along the x-axis.
	 */
	public static final MatrixFunction FLIP_Y = (final Matrix matrix) -> {
		long swap;
		final BitSet bits = matrix.bits();
		for (int i = 0; i < Integer.SIZE; i++) {
			swap = bits.getWord(i);
			bits.setWord(i, bits.getWord(63 - i));
			bits.setWord(63 - i, swap);
		}
		return matrix;
	};

	/**
	 * {@link MatrixFunction} that flips the x-coordinate and y-coordinate of all
	 * bits in the specified {@link Matrix}, effectively rotating it 180 degrees.
	 */
	public static final MatrixFunction FLIP = MatrixFunction.combine(FLIP_X, FLIP_Y);

	/**
	 * {@link MatrixFunction} that rotates the specified {@link Matrix} 90 degrees
	 * to the right (clockwise).
	 */
	public static final MatrixFunction ROTATE_R = MatrixFunction.combine(TRANSPOSE, FLIP_Y);

	/**
	 * {@link MatrixFunction} that rotates the specified {@link Matrix} 90 degrees
	 * to the left (counter-clockwise).
	 */
	public static final MatrixFunction ROTATE_L = MatrixFunction.combine(TRANSPOSE, FLIP_X);

	/**
	 * Creates a {@link MatrixFunction} which performs the specified
	 * {@link WordFunction} <b>function</b> on each word contained in the
	 * {@link Matrix} provided.
	 * 
	 * @param function the {@link WordFunction} to be applied to the {@link Matrix}.
	 * @return a {@link MatrixFunction} which applies the specified <b>function</b>.
	 */
	public static MatrixFunction of(final WordFunction function) {
		return MatrixFunction.of(BitSetFunction.of(function));
	}

	/**
	 * Creates a {@link MatrixFunction} which performs the specified
	 * {@link BitSetFunction} <b>function</b> on the {@link BitSet} used by the
	 * {@link Matrix} provided.
	 * 
	 * @param function the {@link WordFunction} to be applied to the {@link Matrix}.
	 * @return a {@link MatrixFunction} which applies the specified <b>function</b>.
	 */
	public static MatrixFunction of(final BitSetFunction function) {
		return (final Matrix matrix) -> {
			function.apply(matrix.bits());
			return matrix;
		};
	}

	/**
	 * Creates a {@link MatrixFunction} which performs the two specified
	 * {@link MatrixFunction}s <b>first</b> and <b>second</b> in sequence.
	 * 
	 * @param first  the first {@link MatrixFunction} to be applied to the word
	 *               argument.
	 * @param second the second {@link MatrixFunction} to be applied to the result
	 *               of <b>first</b>.
	 * @return a {@link MatrixFunction} which applies both <b>first</b> and
	 *         <b>second</b>.
	 * 
	 * @throws NullPointerException if <b>first</b> or <b>second</b> are null.
	 */
	public static MatrixFunction combine(final MatrixFunction first, final MatrixFunction second) {
		Objects.requireNonNull(first);
		Objects.requireNonNull(second);
		return (final Matrix matrix) -> second.apply(first.apply(matrix));
	}

}