
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

Why map/filter chains can still be expensiv

----

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
----

🟡 Project 3 — Custom Parallel Consumer (Without Frameworks First)

Goal: Understand controlled concurrency.

Implement:

Manual poll loop

Work delegation to virtual threads

Offset control + retry topic

Then compare with the Parallel Consumer library.

You will learn:

Why naive parallelism breaks ordering

Offset commit as a correctness boundary

Virtual threads ≠ free performance
----

🟡 Project 4 — Stateful Stream Processing Engine

Goal: Learn Kafka Streams internals by rebuilding a mini version.

Build:

Event aggregation with windowing

Local state store (MapDB/RocksDB)

Changelog topic for recovery

You will learn:

State synchronization cost

Repartition topics

Why stateful streaming is hard
----

🟠 Project 5 — Reactive HTTP → Kafka Bridge

Goal: Use WebFlux only where justified.

Create a service that:

Accepts bursts of HTTP traffic

Converts requests into Kafka events

Applies backpressure when Kafka slows

You will learn:

Event-loop vs thread-per-request

Where reactive helps (and where it hurts)

Coordinating two different backpressure models
----

🟠 Project 6 — Idempotent Processing + Exactly-Once Simulation

Goal: Kill the “exactly-once” illusion.

Implement:

Deduplication store

Replay-safe consumers

Crash/restart simulation

You will learn:

Why EOS is mostly application design

Log-based recovery patterns
----

🔴 Project 7 — Queryable Streaming Views (Materialized Views System)

Goal: Efficient data storage + streaming fusion.

Build:

A streaming pipeline that maintains queryable projections

Postgres read model optimized for queries

Compare push vs pull computation

You will learn:

Storage layout for streaming systems

Avoiding ORM inefficiencies

Designing for reprocessing
----

🔴 Project 8 — Failure Lab (Break Everything Intentionally)

Goal: Production realism.

Simulate:

Consumer rebalance storms

Poison messages

Disk slowdown

GC pauses

Partial outages

You will learn:

Observability-driven design

How systems degrade

Real scalability constraints
----

🟣 Project 9 — Avro & Schema Evolution Lab

Goal: Master binary serialization, schema governance, and safe evolution in distributed systems.

Build:

A Kafka-based system using Apache Avro

Integrate with Confluent Schema Registry

Produce and consume versioned events

Simulate schema evolution scenarios (add fields, remove fields, change types)

Test different compatibility modes (BACKWARD, FORWARD, FULL)

You will learn:

Binary serialization efficiency vs JSON

How schema IDs are embedded in Kafka messages

Backward and forward compatibility mechanics

Safe producer and consumer upgrade strategies

Why schema compatibility does not guarantee semantic compatibility

How to migrate schemas without breaking live consumers
---
