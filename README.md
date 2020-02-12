# BitSet
Compact, fixed-sized set data structure for integers. Many applications of BitSets involve [embarrassingly parallel](https://www.wikipedia.org/wiki/Embarrassingly_parallel) operations. [java.util.BitSet](https://docs.oracle.com/javase/10/docs/api/java/util/BitSet.html) doesn't provide any way to take advantage of this, which inspired this library. This library also provides a variety of tools to efficiently stream, manipulate and randomize BitSets.

## Documentation
This library is fully documented -- the JavaDocs are best viewed [here](https://ashouldis.github.io/BitSet/).

## Class Summary

### BitSet
BitSet implementation focusing on performance.

### ConcurrentBitSet
BitSet implementation that can be safely operated upon by any number of threads without synchronization.

### Random
Implementation of the XOrShift64 pseudo-random number generation algorithm providing means to randomize BitSets efficiently.

### DensityRandom
Implementation of Random that efficiently generates bits that have a specified chance of being in the *live* state, or "density".

### Biterator
Means of reducing ranges of indices into portions that can be safely operated upon in parallel.

### FunctionBiterator
Means of efficiently streaming the result of bitwise operations between two BitSets in-place.

## Benchmark
Benchmark of the time to read the state of all bits. Easily parallelized using a **Biterator**. Made faster than **java.util.BitSet** in all cases by avoiding range and invariant checks.  
![Reading](https://github.com/ashouldis/BitSet/blob/master/benchmark/benchmark_read.png "\Benchmark_Read")  

Benchmark of the time to stream the indices of all bits in the *live* state. Easily parallelized using **BitSpliterator.Live**. Non-parallel implementations also benefit from **BitSpliterator.Live** by reducing the number of read operations.  
![Streaming](https://github.com/ashouldis/BitSet/blob/master/benchmark/benchmark_stream.png "\Benchmark_Stream")  

Benchmark of the time to manipulate the state of each bit individually. Easily parallelized using a **BitSpliterator**. **ConcurrentBitSet** is heavily taxed by the number of write operations. Again, made innately faster than **java.util.BitSet** by avoiding range and invariant checks.  
![Manipulating](https://github.com/ashouldis/BitSet/blob/master/benchmark/benchmark_bit.png "\Benchmark_Bit")  