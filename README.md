# BitSet
Compact, fixed-sized set data structure for integers. Many applications of BitSets involve [embarrassingly parallel](https://www.wikipedia.org/wiki/Embarrassingly_parallel) operations. [java.util.BitSet](https://docs.oracle.com/javase/10/docs/api/java/util/BitSet.html) doesn't provide any way to take advantage of this, which inspired this library. This library also provides a variety of tools to efficiently stream, manipulate and randomize BitSets.

## Documentation
This library is fully documented -- the JavaDocs are best viewed [here](https://ashouldis.github.io/bitset/).

## Benchmark
Benchmark of the time to read the state of all bits. Easily parallelized using a **Biterator**.  
![Reading](https://github.com/ashouldis/BitSet/blob/master/benchmark/benchmark_read.png "\Benchmark_Read")  

Benchmark of the time to stream the indices of all bits in the *live* state. Easily parallelized using **LiveBiterator**.  
![Streaming](https://github.com/ashouldis/BitSet/blob/master/benchmark/benchmark_stream.png "\Benchmark_Stream")  

Benchmark of the time to manipulate the state of each bit individually. Easily parallelized using a **Biterator**. **ConcurrentBitSet** is slowed by the number of write operations.  
![Manipulating](https://github.com/ashouldis/BitSet/blob/master/benchmark/benchmark_bit.png "\Benchmark_Bit")  