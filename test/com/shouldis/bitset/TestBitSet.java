package com.shouldis.bitset;

import java.util.Random;
import java.util.stream.IntStream;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestBitSet {

	private static final Random RANDOM = new Random();
	private static BitSet[] SETS = new BitSet[6];

	@BeforeClass
	public static void initialize() {
		SETS[0] = new BitSet(1);
		SETS[1] = new BitSet(20);
		SETS[2] = new BitSet(50);
		SETS[3] = new BitSet(200);
		SETS[4] = new BitSet(256);
		SETS[5] = new BitSet(Integer.MAX_VALUE);
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
	public void testConstructorInt() {
		int size = 1000;
		BitSet set = new BitSet(size);
		Assert.assertEquals(set.size, size);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorIntNegative() {
		int size = -1000;
		new BitSet(size);
	}

	@Test
	public void testConstructorBitSet() {
		BitSet copy;
		for (BitSet set : SETS) {
			copy = new BitSet(set);
			Assert.assertEquals(set, copy);
		}
	}

	@Test(expected = NullPointerException.class)
	public void testConstructorBitSetNull() {
		new BitSet(null);
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
	public void testGet() {
		int index;
		for (BitSet set : SETS) {
			index = RANDOM.nextInt(set.size);
			set.set(index);
			Assert.assertTrue(set.get(index));
		}
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testGetTooBig() {
		BitSet set = SETS[0];
		set.get(set.size + BitSet.WORD_SIZE);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testGetNegative() {
		BitSet set = SETS[0];
		set.get(-1);
	}

	@Test
	public void testGetRange() {
		int bound;
		int count;
		for (BitSet set : SETS) {
			bound = RANDOM.nextInt(set.size);
			count = 0;
			for (int i = 0; i < bound; i++) {
				if (set.get(i)) {
					count++;
				}
			}
			Assert.assertEquals(set.get(0, bound), count);
		}
	}

	@Test
	public void testGetRangeSingle() {
		for (BitSet set : SETS) {
			set.set(0);
			Assert.assertEquals(set.get(0, 0), 0);
		}
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testGetRangeBig() {
		BitSet set = SETS[0];
		set.get(0, set.size + BitSet.WORD_SIZE);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testGetRangeNegative() {
		BitSet set = SETS[0];
		set.get(-1, set.size);
	}

	@Test
	public void testGetRangeCross() {
		for (BitSet set : SETS) {
			set.fill();
			Assert.assertEquals(set.get(set.size, 0), 0);
		}
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
			copy = new BitSet(set);
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
			copy = new BitSet(set);
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
			copy = new BitSet(set);
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
	public void testNextLive() {
		for (BitSet set : SETS) {
			set.empty();
			set.set(set.size - 1);
			Assert.assertEquals(set.nextLive(0), set.size - 1);
			Assert.assertEquals(set.nextLive(set.size - 1), set.size - 1);
		}
	}

	@Test
	public void testNextLiveTooBig() {
		for (BitSet set : SETS) {
			Assert.assertEquals(set.nextLive(set.size + BitSet.WORD_SIZE), -1);
		}
	}

	public void testNextLiveNegative() {
		for (BitSet set : SETS) {
			Assert.assertEquals(set.nextLive(-1), -1);
		}
	}

	@Test
	public void testNextLiveNone() {
		for (BitSet set : SETS) {
			set.empty();
			Assert.assertEquals(set.nextLive(0), -1);
		}
	}

	@Test
	public void testNextDead() {
		for (BitSet set : SETS) {
			set.fill();
			set.clear(set.size - 1);
			Assert.assertEquals(set.nextDead(0), set.size - 1);
			Assert.assertEquals(set.nextDead(set.size - 1), set.size - 1);
		}
	}

	@Test
	public void testNextDeadTooBig() {
		for (BitSet set : SETS) {
			Assert.assertEquals(set.nextDead(set.size + BitSet.WORD_SIZE), -1);
		}
	}

	@Test
	public void testNextDeadNegative() {
		for (BitSet set : SETS) {
			Assert.assertEquals(set.nextDead(-1), -1);
		}
	}

	@Test
	public void testNextDeadNone() {
		for (BitSet set : SETS) {
			set.fill();
			Assert.assertEquals(set.nextDead(0), -1);
		}
	}

	@Test
	public void testLastLive() {
		for (BitSet set : SETS) {
			set.empty();
			set.set(0);
			Assert.assertEquals(set.lastLive(set.size - 1), 0);
			Assert.assertEquals(set.lastLive(0), 0);
		}
	}

	@Test
	public void testLastLiveTooBig() {
		for (BitSet set : SETS) {
			Assert.assertEquals(set.lastLive(set.size + BitSet.WORD_SIZE), -1);
		}
	}

	@Test
	public void testLastLiveNegative() {
		for (BitSet set : SETS) {
			Assert.assertEquals(set.lastLive(-1), -1);
		}
	}

	@Test
	public void testLastLiveNone() {
		for (BitSet set : SETS) {
			set.empty();
			Assert.assertEquals(set.lastLive(set.size - 1), -1);
		}
	}

	@Test
	public void testLastDead() {
		for (BitSet set : SETS) {
			set.fill();
			set.clear(0);
			Assert.assertEquals(set.lastDead(set.size - 1), 0);
			Assert.assertEquals(set.lastDead(0), 0);
		}
	}

	@Test
	public void testLastDeadTooBig() {
		for (BitSet set : SETS) {
			Assert.assertEquals(set.lastDead(set.size + BitSet.WORD_SIZE), -1);
		}
	}

	@Test
	public void testLastDeadNegative() {
		for (BitSet set : SETS) {
			Assert.assertEquals(set.lastDead(-1), -1);
		}
	}

	@Test
	public void testLastDeadNone() {
		for (BitSet set : SETS) {
			set.fill();
			Assert.assertEquals(set.lastDead(set.size - 1), -1);
		}
	}

	@Test
	public void testOr() {
		BitSet other, original;
		for (BitSet set : SETS) {
			other = new BitSet(set.size);
			other.randomize(0, other.size);
			original = new BitSet(set);
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
			other = new BitSet(set.size);
			other.randomize(0, other.size);
			original = new BitSet(set);
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
			other = new BitSet(set.size);
			other.randomize(0, other.size);
			original = new BitSet(set);
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
			other = new BitSet(set.size);
			other.randomize(0, other.size);
			set.not(other);
			for (int i = 0; i < set.size; i++) {
				Assert.assertEquals(set.get(i), !other.get(i));
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

	@Test
	public void testCopy() {
		BitSet other;
		for (BitSet set : SETS) {
			other = new BitSet(set.size);
			other.randomize(0, other.size);
			set.copy(other);
			for (int i = 0; i < set.size; i++) {
				Assert.assertEquals(set.get(i), other.get(i));
			}
		}
	}

	@Test(expected = NullPointerException.class)
	public void testCopyNull() {
		SETS[0].copy(null);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testCopySmaller() {
		SETS[4].copy(SETS[3]);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testCopyLarger() {
		SETS[4].copy(SETS[5]);
	}

	@Test
	public void testFill() {
		for (BitSet set : SETS) {
			set.fill();
			Assert.assertEquals(set.population(), set.size);
		}
	}

	@Test
	public void testEmpty() {
		for (BitSet set : SETS) {
			set.fill();
			Assert.assertEquals(set.population(), set.size);
		}
	}

	@Test
	public void testNot() {
		BitSet original;
		for (BitSet set : SETS) {
			original = new BitSet(set);
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

	@Test
	public void testLive() {
		IntStream stream;
		for (BitSet set : SETS) {
			stream = new BitSpliterator.Live(set, 0, set.size).stream();
			Assert.assertEquals(stream.count(), set.population());
			stream = new BitSpliterator.Live(set, 0, set.size).stream();
			stream.forEach(i -> Assert.assertTrue(set.get(i)));
		}
	}

	@Test
	public void testLiveRange() {
		int bound;
		int fromGet, fromCount;
		for (BitSet set : SETS) {
			bound = RANDOM.nextInt(set.size) + 1;
			fromGet = set.get(0, bound);
			fromCount = (int) new BitSpliterator.Live(set, 0, bound).stream().count();
			Assert.assertEquals(fromGet, fromCount);
		}
	}

	@Test
	public void testLiveParallel() {
		for (BitSet set : SETS) {
			new BitSpliterator.Live(set, 0, set.size).stream().parallel().forEach(set::clear);
			Assert.assertEquals(set.population(), 0);
		}
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testLiveTooBig() {
		BitSet set = SETS[4];
		new BitSpliterator.Live(set, 0, set.size + BitSet.WORD_SIZE).stream().forEach(i -> i++);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testLiveNegative() {
		BitSet set = SETS[4];
		new BitSpliterator.Dead(set, -1, set.size).stream().forEach(i -> i++);
	}

	@Test
	public void testDead() {
		IntStream stream;
		for (BitSet set : SETS) {
			stream = new BitSpliterator.Dead(set, 0, set.size).stream();
			Assert.assertEquals(stream.count(), set.size - set.population());
			stream = new BitSpliterator.Dead(set, 0, set.size).stream();
			stream.forEach(i -> Assert.assertFalse(set.get(i)));
		}
	}

	@Test
	public void testDeadRange() {
		int bound;
		int fromGet, fromCount;
		for (BitSet set : SETS) {
			bound = RANDOM.nextInt(set.size) + 1;
			fromGet = bound - set.get(0, bound);
			fromCount = (int) new BitSpliterator.Dead(set, 0, bound).stream().count();
			Assert.assertEquals(fromGet, fromCount);
		}
	}

	@Test
	public void testDeadParallel() {
		for (BitSet set : SETS) {
			new BitSpliterator.Dead(set, 0, set.size).stream().parallel().forEach(set::set);
			Assert.assertEquals(set.population(), set.size);
		}
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testDeadTooBig() {
		BitSet set = SETS[4];
		new BitSpliterator.Dead(set, 0, set.size + BitSet.WORD_SIZE).stream().forEach(i -> i++);
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testDeadNegative() {
		BitSet set = SETS[4];
		new BitSpliterator.Dead(set, -1, set.size).stream().forEach(i -> i++);
	}

	@Test
	public void testCleanLastWord() {
		int hash;
		for (BitSet set : SETS) {
			set.fill();
			Assert.assertEquals(set.size, set.population());
			if (BitSet.modSize(set.size) != 0) {
				hash = set.hashCode();
				set.toggle(set.size);
				Assert.assertEquals(hash, set.hashCode());
			}
		}
	}

	@Test
	public void testPopulation() {
		for (BitSet set : SETS) {
			Assert.assertEquals(new BitSpliterator.Live(set, 0, set.size).stream().count(), set.population());
			Assert.assertEquals(set.get(0, set.size), set.population());
		}
	}

	@Test
	public void testIdentifier() {
		long identifier;
		for (BitSet set : SETS) {
			identifier = set.identifier();
			set.toggle(RANDOM.nextInt(set.size));
			Assert.assertNotEquals(identifier, set.identifier());
		}
		identifier = new BitSet(100).identifier();
		Assert.assertNotEquals(new BitSet(10).identifier(), identifier);
	}

	@Test
	public void testToString() {
		String toString;
		for (BitSet set : SETS) {
			toString = set.toString();
			Assert.assertTrue(toString.contains(String.valueOf(set.population())));
			Assert.assertTrue(toString.contains(String.valueOf(set.size)));
		}
	}

	@Test
	public void testEquals() {
		BitSet equal;
		for (BitSet set : SETS) {
			equal = new BitSet(set);
			Assert.assertEquals(equal, set);
		}
	}

	@Test
	public void testEqualsNull() {
		for (BitSet set : SETS) {
			Assert.assertNotEquals(set, null);
		}
	}

	@Test
	public void testEqualsExact() {
		BitSet equal;
		for (BitSet set : SETS) {
			equal = set;
			Assert.assertEquals(equal, set);
		}
	}

	@Test
	public void testEqualsChanged() {
		BitSet equal;
		for (BitSet set : SETS) {
			equal = new BitSet(set);
			equal.toggle(0);
			Assert.assertNotEquals(equal, set);
		}
	}

}
