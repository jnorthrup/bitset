package com.shouldis.bitset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestBitSpliterator {

	private static BitSet[] SETS = new BitSet[6];

	private static final BiConsumer<BitSet, IntConsumer> SERIAL_ON_ALL = (BitSet set, IntConsumer function) -> {
		new BitSpliterator.Range(0, set.size).stream().forEach(function);
	};

	private static final BiConsumer<BitSet, IntConsumer> PARALLEL_ON_ALL = (BitSet set, IntConsumer function) -> {
		new BitSpliterator.Range(0, set.size).stream().parallel().forEach(function);
	};

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
	}

	@Before
	public void randomize() {
		for (BitSet set : SETS) {
			set.randomize(0, set.size);
		}
	}

	@Test
	public void stressToggle() throws InterruptedException {
		ExecutorService ex;
		int initial;
		for (BitSet set : SETS) {
			ex = Executors.newCachedThreadPool();
			initial = set.population();
			for (int i = 0; i < 4; i++) {
				ex.execute(() -> SERIAL_ON_ALL.accept(set, set::toggle));
			}
			ex.shutdown();
			ex.awaitTermination(5, TimeUnit.MINUTES);
			Assert.assertEquals(initial, set.population());
		}
	}

	@Test
	public void stressToggleParallel() throws InterruptedException {
		ExecutorService ex;
		int initial;
		for (BitSet set : SETS) {
			ex = Executors.newCachedThreadPool();
			initial = set.population();
			for (int i = 0; i < 4; i++) {
				ex.execute(() -> PARALLEL_ON_ALL.accept(set, set::toggle));
			}
			ex.shutdown();
			ex.awaitTermination(5, TimeUnit.MINUTES);
			Assert.assertEquals(initial, set.population());
		}
	}

	@Test
	public void stressRemove() throws InterruptedException {
		ExecutorService ex;
		final LongAdder adder = new LongAdder();
		for (BitSet set : SETS) {
			set.randomize(0, set.size);
			int initial = set.population();
			ex = Executors.newCachedThreadPool();
			for (int i = 0; i < 4; i++) {
				ex.execute(() -> SERIAL_ON_ALL.accept(set, (int j) -> {
					if (set.remove(j)) {
						adder.increment();
					}
				}));
			}
			ex.shutdown();
			ex.awaitTermination(5, TimeUnit.MINUTES);
			Assert.assertEquals(initial, adder.intValue());
			adder.reset();
		}
	}

	@Test
	public void stressRemoveParallel() throws InterruptedException {
		ExecutorService ex;
		final LongAdder adder = new LongAdder();
		for (BitSet set : SETS) {
			set.randomize(0, set.size);
			int initial = set.population();
			ex = Executors.newCachedThreadPool();
			for (int i = 0; i < 4; i++) {
				ex.execute(() -> PARALLEL_ON_ALL.accept(set, (int j) -> {
					if (set.remove(j)) {
						adder.increment();
					}
				}));
			}
			ex.shutdown();
			ex.awaitTermination(5, TimeUnit.MINUTES);
			Assert.assertEquals(initial, adder.intValue());
			adder.reset();
		}
	}

	@Test
	public void stressAdd() throws InterruptedException {
		ExecutorService ex;
		final LongAdder adder = new LongAdder();
		for (BitSet set : SETS) {
			set.randomize(0, set.size);
			int initial = set.population();
			ex = Executors.newCachedThreadPool();
			for (int i = 0; i < 4; i++) {
				ex.execute(() -> SERIAL_ON_ALL.accept(set, (int j) -> {
					if (set.add(j)) {
						adder.increment();
					}
				}));
			}
			ex.shutdown();
			ex.awaitTermination(10, TimeUnit.MINUTES);
			Assert.assertEquals(set.size - initial, adder.intValue());
			adder.reset();
		}
	}

	@Test
	public void stressAddParallel() throws InterruptedException {
		ExecutorService ex;
		final LongAdder adder = new LongAdder();
		for (BitSet set : SETS) {
			set.randomize(0, set.size);
			int initial = set.population();
			ex = Executors.newCachedThreadPool();
			for (int i = 0; i < 4; i++) {
				ex.execute(() -> PARALLEL_ON_ALL.accept(set, (int j) -> {
					if (set.add(j)) {
						adder.increment();
					}
				}));
			}
			ex.shutdown();
			ex.awaitTermination(10, TimeUnit.MINUTES);
			Assert.assertEquals(set.size - initial, adder.intValue());
			adder.reset();
		}
	}

}
