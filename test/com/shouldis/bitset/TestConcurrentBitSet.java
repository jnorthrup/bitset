package com.shouldis.bitset;

import java.util.Random;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestConcurrentBitSet {

	private static final Random RANDOM = new Random();
	private static ConcurrentBitSet[] SETS = new ConcurrentBitSet[6];

	@BeforeClass
	public static void initialize() {
		SETS[0] = new ConcurrentBitSet(1);
		SETS[1] = new ConcurrentBitSet(20);
		SETS[2] = new ConcurrentBitSet(50);
		SETS[3] = new ConcurrentBitSet(200);
		SETS[4] = new ConcurrentBitSet(256);
		SETS[5] = new ConcurrentBitSet(Integer.MAX_VALUE);
	}

	@AfterClass
	public static void teardown() {
		SETS = null;
		System.gc();
	}

	@Before
	public void randomize() {
		for (BitSet set : SETS) {
			set.randomize(0, set.size);
		}
	}

	@Test
	public void testAdd() {
		for (BitSet set : SETS) {
			set.empty();
			Assert.assertTrue(set.add(0));
			Assert.assertEquals(set.population(), 1);
			Assert.assertTrue(set.get(0));
		}
	}

	@Test
	public void testAddFull() {
		for (BitSet set : SETS) {
			set.empty();
			Assert.assertTrue(set.add(0));
			Assert.assertFalse(set.add(0));
		}

	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testAddTooBig() {
		BitSet set = SETS[0];
		set.add(set.size + BitSet.WORD_SIZE);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testAddNegative() {
		BitSet set = SETS[0];
		set.add(-1);
	}

	@Test
	public void testRemove() {
		for (BitSet set : SETS) {
			set.fill();
			Assert.assertTrue(set.remove(0));
			Assert.assertEquals(set.population(), set.size - 1);
			Assert.assertFalse(set.get(0));
		}
	}

	@Test
	public void testRemoveEmpty() {
		for (BitSet set : SETS) {
			set.fill();
			Assert.assertTrue(set.remove(0));
			Assert.assertFalse(set.remove(0));
		}
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testRemoveTooBig() {
		BitSet set = SETS[0];
		set.remove(set.size + BitSet.WORD_SIZE);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testRemoveNegative() {
		BitSet set = SETS[0];
		set.remove(-1);
	}

	@Test
	public void testSet() {
		for (BitSet set : SETS) {
			set.empty();
			set.set(0);
			Assert.assertEquals(set.population(), 1);
			Assert.assertTrue(set.get(0));
		}
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testSetTooBig() {
		BitSet set = SETS[0];
		set.set(set.size + BitSet.WORD_SIZE);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testSetNegative() {
		BitSet set = SETS[0];
		set.set(-1);
	}

	@Test
	public void testSetRange() {
		int bound;
		BitSet copy;
		for (BitSet set : SETS) {
			bound = RANDOM.nextInt(set.size);
			copy = new ConcurrentBitSet(set);
			copy.set(0, bound);
			for (int i = 0; i < bound; i++) {
				set.set(i);
			}
			Assert.assertEquals(set, copy);
		}
	}

	@Test
	public void testSetRangeSingle() {
		for (BitSet set : SETS) {
			set.empty();
			set.set(0, 0);
			Assert.assertFalse(set.get(0));
		}
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testSetRangeBig() {
		BitSet set = SETS[0];
		set.set(0, set.size + BitSet.WORD_SIZE);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testSetRangeNegative() {
		BitSet set = SETS[0];
		set.set(-1, set.size);
	}

	@Test
	public void testSetRangeCross() {
		for (BitSet set : SETS) {
			set.empty();
			set.set(set.size, 0);
			Assert.assertEquals(set.population(), 0);
		}
	}

	@Test
	public void testClear() {
		for (BitSet set : SETS) {
			set.fill();
			set.clear(0);
			Assert.assertFalse(set.get(0));
		}
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testClearTooBig() {
		BitSet set = SETS[0];
		set.clear(set.size + BitSet.WORD_SIZE);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testClearNegative() {
		BitSet set = SETS[0];
		set.clear(-1);
	}

	@Test
	public void testClearRange() {
		int bound;
		BitSet copy;
		for (BitSet set : SETS) {
			bound = RANDOM.nextInt(set.size);
			copy = new ConcurrentBitSet(set);
			copy.clear(0, bound);
			for (int i = 0; i < bound; i++) {
				set.clear(i);
			}
			Assert.assertEquals(set, copy);
		}
	}

	@Test
	public void testClearRangeSingle() {
		for (BitSet set : SETS) {
			set.fill();
			set.clear(0, 0);
			Assert.assertTrue(set.get(0));
		}
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testClearRangeBig() {
		BitSet set = SETS[0];
		set.clear(0, set.size + BitSet.WORD_SIZE);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testClearRangeNegative() {
		BitSet set = SETS[0];
		set.clear(-1, set.size);
	}

	@Test
	public void testClearRangeCross() {
		for (BitSet set : SETS) {
			set.fill();
			set.clear(set.size, 0);
			Assert.assertEquals(set.population(), set.size);
		}
	}

	@Test
	public void testToggle() {
		for (BitSet set : SETS) {
			set.clear(0);
			set.toggle(0);
			Assert.assertTrue(set.get(0));
			set.toggle(0);
			Assert.assertFalse(set.get(0));
		}
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testToggleTooBig() {
		BitSet set = SETS[0];
		set.toggle(set.size + BitSet.WORD_SIZE);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testToggleNegative() {
		BitSet set = SETS[0];
		set.toggle(-1);
	}

	@Test
	public void testToggleRange() {
		int bound;
		BitSet copy;
		for (BitSet set : SETS) {
			bound = RANDOM.nextInt(set.size);
			copy = new ConcurrentBitSet(set);
			copy.toggle(0, bound);
			for (int i = 0; i < bound; i++) {
				set.toggle(i);
			}
			Assert.assertEquals(set, copy);
		}
	}

	@Test
	public void testToggleRangeSingle() {
		for (BitSet set : SETS) {
			set.empty();
			set.toggle(0, 0);
			Assert.assertFalse(set.get(0));
			set.set(0);
			set.toggle(0, 0);
			Assert.assertTrue(set.get(0));
		}
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testToggleRangeBig() {
		BitSet set = SETS[0];
		set.toggle(0, set.size + BitSet.WORD_SIZE);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testToggleRangeNegative() {
		BitSet set = SETS[0];
		set.toggle(-1, set.size);
	}

	@Test
	public void testToggleRangeCross() {
		for (BitSet set : SETS) {
			set.empty();
			set.toggle(set.size, 0);
			Assert.assertEquals(set.population(), 0);
		}
	}

	@Test
	public void testRandomize() {
		BitSet set = SETS[5];
		set.randomize(0, set.size);
		int population = set.population();
		float percentage = population / (float) set.size;
		Assert.assertTrue(Math.abs(percentage - 0.5) < 0.01);
	}

	@Test
	public void testRandomizeCross() {
		BitSet set = SETS[5];
		long identifier = set.identifier();
		set.randomize(set.size, 0);
		Assert.assertEquals(identifier, set.identifier());
	}

	@Test
	public void testOr() {
		BitSet other, original;
		for (BitSet set : SETS) {
			other = new ConcurrentBitSet(set.size);
			other.randomize(0, other.size);
			original = new ConcurrentBitSet(set);
			set.or(other);
			for (int i = 0; i < set.size; i++) {
				if (original.get(i) || other.get(i)) {
					Assert.assertTrue(set.get(i));
				} else {
					Assert.assertFalse(set.get(i));
				}
			}
		}
	}

	@Test(expected = NullPointerException.class)
	public void testOrNull() {
		SETS[0].or(null);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testOrSmaller() {
		SETS[4].or(SETS[3]);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testOrLarger() {
		SETS[4].or(SETS[5]);
	}

	@Test
	public void testXOr() {
		BitSet other, original;
		for (BitSet set : SETS) {
			other = new ConcurrentBitSet(set.size);
			other.randomize(0, other.size);
			original = new ConcurrentBitSet(set);
			set.xor(other);
			for (int i = 0; i < set.size; i++) {
				if (original.get(i) ^ other.get(i)) {
					Assert.assertTrue(set.get(i));
				} else {
					Assert.assertFalse(set.get(i));
				}
			}
		}
	}

	@Test(expected = NullPointerException.class)
	public void testXOrNull() {
		SETS[0].xor(null);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testXOrSmaller() {
		SETS[4].xor(SETS[3]);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testXOrLarger() {
		SETS[4].xor(SETS[5]);
	}

	@Test
	public void testAnd() {
		BitSet other, original;
		for (BitSet set : SETS) {
			other = new ConcurrentBitSet(set.size);
			other.randomize(0, other.size);
			original = new ConcurrentBitSet(set);
			set.and(other);
			for (int i = 0; i < set.size; i++) {
				if (original.get(i) && other.get(i)) {
					Assert.assertTrue(set.get(i));
				} else {
					Assert.assertFalse(set.get(i));
				}
			}
		}
	}

	@Test(expected = NullPointerException.class)
	public void testAndNull() {
		SETS[0].and(null);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testAndSmaller() {
		SETS[4].and(SETS[3]);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testAndLarger() {
		SETS[4].and(SETS[5]);
	}

	@Test
	public void testNotBitSet() {
		BitSet other;
		for (BitSet set : SETS) {
			other = new ConcurrentBitSet(set.size);
			other.randomize(0, other.size);
			set.not(other);
			for (int i = 0; i < set.size; i++) {
				Assert.assertEquals(set.get(i), !other.get(i));
			}
		}
	}

	@Test
	public void testNot() {
		BitSet original;
		for (BitSet set : SETS) {
			original = new ConcurrentBitSet(set);
			set.not();
			for (int i = 0; i < set.size; i++) {
				if (original.get(i)) {
					Assert.assertFalse(set.get(i));
				} else {
					Assert.assertTrue(set.get(i));
				}
			}
		}
	}

	@Test(expected = NullPointerException.class)
	public void testNotNull() {
		SETS[0].not(null);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testNotSmaller() {
		SETS[4].not(SETS[3]);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testNotLarger() {
		SETS[4].not(SETS[5]);
	}

}