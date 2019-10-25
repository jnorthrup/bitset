# BitSet
Many applications of BitSets involve [embarrassingly parallel](https://www.wikipedia.org/wiki/Embarrassingly_parallel) operations. [java.util.BitSet](https://docs.oracle.com/javase/10/docs/api/java/util/BitSet.html) doesn't provide any way to take advantage of this, which inspired this small library.

## Benchmark
Time to compute and send indices of all live bits to `.forEach(IntConsumer)` in random (50% filled) BitSets.  
![Live Index Overhead](https://github.com/ashouldis/BitSet/blob/master/img/overhead.png "Overhead Benchmark")

Time to modify all live bit indices with one bitwise operation in random (50% filled) BitSets.  
![Clear Live Bits](https://github.com/ashouldis/BitSet/blob/master/img/clear.png "Manipulate Benchmark")

Tested on i5-8250U

## Features
* get, set, clear, toggle (index / range)
* and, or, xor, copy, not (against other BitSet)
* empty, fill, not
* nextLive, lastLive, nextDead, lastDead
* add, remove
* density (range)
* randomize (range)
* parallel-capable stream of live or dead indices
* parallel-capable stream of indices in a range
* parallel-capable stream of indices in an array
* completely concurrent-safe implementation

## Class Summary

### BitSet
Featureful BitSet implementation focusing on performance.

### BitSpliterator
Means of reducing ranges of indices into portions that can be safely operated upon in parallel.

### BitwiseSpliterator
Means of efficiently streaming the result of bitwise operations between two BitSets.

### ConcurrentBitSet
BitSet implementation that can be safely operated upon by any number of threads without any external synchronization.