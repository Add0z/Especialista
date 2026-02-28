🟢 Project 1 — High-Performance File Ingestion Pipeline

Goal: Understand Java Streams, Gatherers, and memory efficiency.

Build a system that:

Reads multi-GB CSV files

Parses them using streaming (no full materialization)

Applies functional transformations

Emits results to Kafka

You will learn:

Allocation-free parsing

Gatherers vs Collectors

Backpressure simulation

Why map/filter chains can still be expensive

🟢 Project 2 — Kafka Producer With Throughput Tuning

Goal: Understand batching, compression, and network behavior.

Create a producer that:

Sends millions of synthetic events

Experiments with linger, batch size, compression

Measures latency vs throughput

You will learn:

Kafka is a filesystem + network system

Why batching beats parallelism

Serialization cost dominance
