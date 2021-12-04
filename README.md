# BitSet
Pure Java implementation of the BitSet data structure prioritizing throughput, performance and extensibility. This library outperforms Java's [java.util.BitSet](https://docs.oracle.com/javase/10/docs/api/java/util/BitSet.html) implementation in most cases, whether by optimizations or tradeoffs.

## Features
* BitSet is made as extensible as possible, leveraging functional decomposition to allow for more specialized subclasses.
* Provides a concurrent implementation, delegating all write operations to thread-safe, atomic operations.
* Provides a immutable implementation, allowing for finalized BitSets when necessary.
* Provides an inlined implementation, reducing the call-stack overhead to improve performance.
* Provides a suite of parallel streams, allowing for manipulation of a BitSet in parallel.
* Provides a new algorithm to optimize randomization to a specified density.
* Provides a variety of functional interfaces, allowing for simplification of more complex operations.
* Matrix representing a 64x64 boolean matrix, allowing matrix operations such as transposition and rotation.

## Design
* Fixed size implementations of BitSet to avoid constant bounds checks, increasing performance while sacrificing the ability for BitSets to grow.
* Given this library's origin in elementary cellular automata, set and un-set bits are referred to as either being in the "live" or "dead" state, avoiding homonyms.
* No compression given that this library's BitSet implementations are fixed size, and specialized in random data.

## [Documentation](https://ashouldis.github.io/bitset/overview-tree)