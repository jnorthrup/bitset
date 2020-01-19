package com.shouldis.bitset.parallel;

import com.shouldis.bitset.BitSet;

/**
 * Implementation of {@link Biterator} used to allow parallel processing when
 * the number of live indices is expensive to compute, but the density of them
 * can be estimated in order to split efficiently. For best results, the density
 * of the {@link BitSet}(s) encountered should be loosely homogeneous.
 * <ul>
 * <li>{@code density(a & b) = density(a) * density(b)}</li>
 * <li>{@code density(a | b) = density(a) + density(b) - (density(a) * density(b))}</li>
 * <li>{@code density(a ^ b) = density(a) + density(b) - (2 * density(a) * density(b))}</li>
 * <li>{@code density(~a) = 1 - density(a)}</li>
 * </ul>
 * 
 * @author Aaron Shouldis
 */
public abstract class DensityBiterator extends Biterator {

	/**
	 * Estimation of the density of bits which will be in the <i>live</i> state in
	 * the words encountered by this {@link DensityBiterator}. Initialized to
	 * {@link Double#NaN} normally, and updated when {@link #updateDensity()} is
	 * called.
	 */
	private double density;

	/**
	 * Creates a {@link DensityBiterator} with the specified starting and ending
	 * position.
	 * 
	 * @param position (inclusive) the first index to include.
	 * @param end      (exclusive) index after the last index to include.
	 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
	 *                                  to <b>end</b>.
	 */
	protected DensityBiterator(final int position, final int end) {
		this(position, end, Double.NaN);
	}

	/**
	 * Creates a {@link DensityBiterator} with the specified starting and ending
	 * position, and the specified <b>density</b>. This constructor should be used
	 * when splitting an existing {@link DensityBiterator} with a known density.
	 * 
	 * @param position (inclusive) the first index to include.
	 * @param end      (exclusive) index after the last index to include.
	 * @param density  the density to initialize this {@link DensityBiterator} with.
	 * @throws IllegalArgumentException if <b>position</b> is greater than or equal
	 *                                  to <b>end</b>.
	 */
	protected DensityBiterator(final int position, final int end, final double density) {
		super(position, end);
		this.density = density;
	}

	/**
	 * Calculates an estimation of the density of bits which will be in the
	 * <i>live</i> state when encountered by this {@link Biterator}. This should
	 * never be called by client code. This density is cached inside
	 * {@link #density}, as it is typically an expensive to calculate.
	 * {@link #getDensity()} should be used unless calculating and updating the
	 * density is necessary, in which case {@link #updateDensity()} should be used
	 * to store the new density.
	 * 
	 * @return an estimation of the density of words encountered by this
	 *         {@link DensityBiterator}.
	 */
	protected abstract double calculateDensity();

	@Override
	public long estimateSize() {
		return (long) ((end - position) * getDensity());
	}

	@Override
	public int characteristics() {
		return DISTINCT | ORDERED | NONNULL | IMMUTABLE;
	}

	public final double updateDensity() {
		return (density = calculateDensity());
	}

	public final double getDensity() {
		if (Double.isNaN(density)) {
			updateDensity();
		}
		return density;
	}

}