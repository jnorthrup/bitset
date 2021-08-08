# BitSet
This library is a pure Java implementation of the BitSet data structure prioritizing throughput, performance and extensibility. This library outperforms Java's [java.util.BitSet](https://docs.oracle.com/javase/10/docs/api/java/util/BitSet.html) implementation in most cases.

# Advancements
* BitSet is made as extensible as possible, leveraging functional decomposition to allow for specialized subclasses.
* Provides a concurrent implementation, delegating all write operations to thread-safe, atomic operations.
* Provides a immutable implementation, allowing for finalized BitSets when needed.
* Provides an inlined implementation, reducing the call-stack overhead to improve performance.
* Provides a suite of parallel streams, allowing for manipulation of a BitSet in parallel.
* Provides a new algorithm to optimize randomization to a specified density.
* Provides functional interfaces, allowing for simplification of more complex operations.
* Fixed size implementation to avoid constant bounds checks, increasing performance.
* Matrix representing a 64x64 boolean matrix, allowing for traditional matrix functions such as transpose, rotate and flip.

## [Documentation](https://ashouldis.github.io/bitset/)