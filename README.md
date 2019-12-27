# BitSet
Many applications of BitSets involve [embarrassingly parallel](https://www.wikipedia.org/wiki/Embarrassingly_parallel) operations. [java.util.BitSet](https://docs.oracle.com/javase/10/docs/api/java/util/BitSet.html) doesn't provide any way to take advantage of this, which inspired this library. This library also provides a variety of tools to stream, manipulate and randomize BitSets.

## Documentation
This library is fully documented -- the JavaDocs are best viewed [here](https://ashouldis.github.io/BitSet/).

## Class Summary

### BitSet
BitSet implementation focusing on performance.

### BitSpliterator
Means of reducing ranges of indices into portions that can be safely operated upon in parallel.

### BitwiseSpliterator
Means of efficiently streaming the result of bitwise operations between two BitSets.

### ConcurrentBitSet
BitSet implementation that can be safely operated upon by any number of threads without external synchronization.

### XOrShift
Implementation of the XOrShift64 pseudo-random number generation algorithm providing means to randomize BitSets efficiently.

### DensityXOrShift
Variant of XOrShift whose generated bits have a specified chance of being in the *live* state, or "density".

## Example Usage

Create a **BitSet** of size 100,000,000:

```java
BitSet bitSet = new BitSet(100000000);
```

Stream *live* indices:

```java
bitSet.live().forEach(i -> {
	// do something
});
```

Stream *dead* indices in parallel:

```java
bitSet.dead().parallel().forEach(i -> {
	// do something else
});
```

Toggle the state of indices in an array in parallel:

```java
BitSpliterator array = new BitSpliterator.Array([1, 2, 3, 5, 8, 13, ...]);
array.stream().parallel().forEach(bitSet::toggle);
```

Randomize a **BitSet** such that each bit has a 50% chance of being in the *live* state.

```java
XOrShift random = new XOrShift();
bitSet.randomize(random);
```

Create a **ConcurrentBitSet** with each bit having a 22% chance of being in the *live* state.

```java
DensityXOrShift random22 = new DensityXOrShift(0.22, 0.001);
ConcurrentBitSet dense = random.nextConcurrentBitSet();
```

Randomize a **BitSet** such that each bit in first half has a 33% chance of being in the *live* state.

```java
DensityXOrShift random33 = new DensityXOrShift(0.33, 0.001);
bitSet.randomize(random33, 0, bitSet.size / 2);
```

## Benchmark
Benchmark of the time to read the state of all bits. Easily parallelized using a **BitSpliterator**. Made faster than **java.util.BitSet** in all cases by avoiding range and invariant checks.  
![Reading](https://github.com/ashouldis/BitSet/blob/master/benchmark/benchmark_read.png "\Benchmark_Read")  

Benchmark of the time to stream the indices of all bits in the *live* state. Easily parallelized using **BitSpliterator.Live**. Non-parallel implementations also benefit from **BitSpliterator.Live** by reducing the number of lookup/read operations.  
![Streaming](https://github.com/ashouldis/BitSet/blob/master/benchmark/benchmark_stream.png "\Benchmark_Stream")  

Benchmark of the time to manipulate the state of each bit individually. Easily parallelized using a **BitSpliterator**. **ConcurrentBitSet** heavily taxed by extreme number of write operations. Again, made innately faster than **java.util.BitSet** by avoiding range and invariant checks.  
![Manipulating](https://github.com/ashouldis/BitSet/blob/master/benchmark/benchmark_bit.png "\Benchmark_Bit")  

Benchmark of the time to manipulate the state of bits in a range sequentially. Does not benefit from being parallelized, and each implementation operates the same except **ConcurrentBitSet**.  
![Range Manipulating](https://github.com/ashouldis/BitSet/blob/master/benchmark/benchmark_range.png "\Benchmark_Range")  

Benchmark of the time to create a randomized BitSet with each bit having a 50% chance of being in the *live* state.  
![Random Initialization](https://github.com/ashouldis/BitSet/blob/master/benchmark/benchmark_random_init.png "\Benchmark_Random_Init")  

Benchmark of the time to randomize the state of all bits. **java.util.BitSet** provides no way to do this other than performing a random test for each bit.  
![Randomization](https://github.com/ashouldis/BitSet/blob/master/benchmark/benchmark_randomize.png "\Benchmark_Randomize")  

<center>Tested on i5-8250U with BitSets of the maximum size: 2^31 -1</center>