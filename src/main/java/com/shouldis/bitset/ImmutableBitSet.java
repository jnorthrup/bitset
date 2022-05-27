package com.shouldis.bitset;

/**
 * Implementation of {@link BitSet} causing {@link #setWord(int, long)}, which
 * controls all write operations, to throw an
 * {@link UnsupportedOperationException}. Normal operations against the
 * (protected visibility) array {@link BitSet#words} will still be able to
 * modify {@link ImmutableBitSet}s.
 * 
 * The state of all bits are immutable once initialized.
 * 
 * @author Aaron Shouldis
 * @see BitSet
 */
public final class ImmutableBitSet extends BitSet {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates an {@link ImmutableBitSet} which is a clone of the specified
	 * <b>set</b>.
	 * 
	 * @param set the {@link BitSet} to copy.
	 * @see BitSet#BitSet(BitSet)
	 */
	public ImmutableBitSet(final BitSet set) {
		super(set.size);
		for (int i = 0; i < wordCount; i++) {
			super.setWord(i, set.getWord(i));
		}
	}

	@Override
	public void setWord(final int wordIndex, final long word) {
		throw new UnsupportedOperationException();
	}

}