# BitSet
Many applications of BitSets involve [embarrassingly parallel](https://www.wikipedia.org/wiki/Embarrassingly_parallel) operations. [java.util.BitSet](https://docs.oracle.com/javase/10/docs/api/java/util/BitSet.html) doesn't provide any way to take advantage of this, which inspired this library. This library also provides a variety of tools to stream, manipulate and randomize BitSets.

## Benchmark
Time to stream the indices of all live bits to `.forEach(IntConsumer)` in randomized BitSets.  
![Live Index Overhead](https://github.com/ashouldis/BitSet/blob/master/benchmark.png "\Benchmark")

Tested on i5-8250U

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
Variant of XOrShift whose generated bits have a specified chance of being in the live state.