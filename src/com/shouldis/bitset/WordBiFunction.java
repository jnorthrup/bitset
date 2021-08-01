package com.shouldis.bitset;

import java.util.Objects;

/**
 * Functional interface used in conjunction with
 * {@link BitSet#apply(int, WordBiFunction, long)}. {@link #apply(long, long)}
 * should represent a function used to manipulate the bits within the words of a
 * {@link BitSet}.
 * 
 * @author Aaron Shouldis
 */
public interface WordBiFunction {

	/**
	 * Function used to perform operations on the words within a {@link BitSet}.
	 * 
	 * @param word the original word to manipulate.
	 * @param mask the second word typically used as a mask in
	 *             {@link WordBiFunction}s.
	 * @return the manipulated <b>word</b>.
	 */
	public long apply(long word, long mask);

	/**
	 * {@link WordBiFunction} used to perform the binary {@code AND} operation
	 * between the bits of <b>word</b> and <b>mask</b>.
	 */
	public static final WordBiFunction AND = (final long word, final long mask) -> word & mask;

	/**
	 * {@link WordBiFunction} used to perform the binary {@code OR} operation
	 * between the bits of <b>word</b> and <b>mask</b>.
	 */
	public static final WordBiFunction OR = (final long word, final long mask) -> word | mask;

	/**
	 * {@link WordBiFunction} used to perform the binary {@code XOR} operation
	 * between the bits of <b>word</b> and <b>mask</b>.
	 */
	public static final WordBiFunction XOR = (final long word, final long mask) -> word ^ mask;

	/**
	 * {@link WordBiFunction} used to perform the binary {@code NOT AND} operation
	 * between the bits of <b>word</b> and <b>mask</b>.
	 */
	public static final WordBiFunction NOT_AND = (final long word, final long mask) -> ~(word & mask);

	/**
	 * {@link WordBiFunction} used to perform the binary {@code NOT OR} operation
	 * between the bits of <b>word</b> and <b>mask</b>.
	 */
	public static final WordBiFunction NOT_OR = (final long word, final long mask) -> ~(word | mask);

	/**
	 * {@link WordBiFunction} used to perform the binary {@code NOT XOR} operation
	 * between the bits of <b>word</b> and <b>mask</b>.
	 */
	public static final WordBiFunction NOT_XOR = (final long word, final long mask) -> ~(word ^ mask);

	/**
	 * Creates a {@link WordBiFunction} which performs the two specified
	 * {@link WordFunction}s <b>first</b> and <b>second</b> in sequence.
	 * 
	 * @param first  the first {@link WordBiFunction} to be applied to the
	 *               arguments.
	 * @param second the second {@link WordBiFunction} to be applied to the result
	 *               of <b>first</b>.
	 * @return a {@link WordBiFunction} which applies both <b>first</b> and
	 *         <b>second</b>.
	 * 
	 * @throws NullPointerException if <b>first</b> or <b>second</b> are null.
	 */
	public static WordBiFunction combine(final WordBiFunction first, final WordBiFunction second) {
		Objects.requireNonNull(first);
		Objects.requireNonNull(second);
		return (final long word, final long mask) -> second.apply(first.apply(word, mask), mask);
	}

}