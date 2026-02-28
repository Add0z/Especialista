🟡 Project 3 — Parallel Kafka: Producer & Consumer Concurrency

# 🎯 Goal

Understand the real boundaries of parallelism in Kafka.

This project explores:

- Horizontal scaling via multiple producer instances
- Consumer-side parallelism and its correctness implications
- Ordering guarantees vs throughput trade-offs
- Offset commit as a correctness boundary
- Why concurrency ≠ free performance

Instead of treating producer and consumer separately, this project analyzes parallelism end-to-end.

---

# 🟢 Part 1 — Parallel Producers (Single JVM Scaling)

## Objective

Understand how Kafka write throughput scales when using multiple producer instances in the same JVM.

### Key Question

If a single producer saturates at ~360k rec/sec, what happens when we run 2, 4, or 8 producers concurrently?

---

## Architecture

                    +----------------+
                    |    CSV File    |
                    +--------+-------+
                             |
           -----------------------------------
            |                |               |

    +---------------+ +---------------+ +---------------+
    | Producer 1 | | Producer 2 | | Producer N |
    | (own instance)| | (own instance)| | (own instance)|
    +-------+-------+ +-------+-------+ +-------+-------+
          \ | /             \ | /             \ | /
    --------------------------------------------------
                            \ | /
    +-------------------------------------------------+
    |                  Kafka Topic                    |
    |                (12 partitions)                  |
    +-------------------------------------------------+

Each producer:

- Owns its own `KafkaProducer` instance
- Has its own internal Sender thread
- Writes to the same topic

---

## Implementation Plan

- Create N independent producer instances.
- Partition input data across producers.
- Measure throughput at:
  - 1 producer
  - 2 producers
  - 4 producers
  - 8 producers

---

## Metrics to Capture

- Total records/sec
- Broker CPU usage
- Network throughput
- Scaling curve shape
- Saturation point

---

## What You Will Learn

- A single KafkaProducer has a single Sender I/O thread.
- Throughput increases by multiplying producers.
- Scaling eventually becomes broker-bound.
- Partition count limits parallelism ceiling.
- Client-side parallelism shifts bottlenecks to the broker.

---

# 🔵 Part 2 — Manual Parallel Consumer

## Objective

Understand controlled concurrency and offset correctness.

This phase focuses on implementing a custom parallel consumer without frameworks first.

---

## Naïve Approach

```java
while (true) {
ConsumerRecords<K, V> records = consumer.poll(...);

for (record : records) {
    executor.submit(() -> process(record));
}

consumer.commitSync();  // ❌ dangerous


}
```

This breaks ordering and can cause data loss.

---

## Correctness-Focused Approach

Implement:

- Manual poll loop
- Work delegation to virtual threads or executor pool
- Offset tracking per partition
- Commit only after processing completion
- Retry topic
- Dead-letter topic

---

## Experiments

Test:

- Commit before processing ❌
- Commit after processing ❌
- Commit after future completion ✅
- Out-of-order completion
- Retry handling

---

## What You Will Learn

- Offset commit defines the correctness boundary.
- Naïve parallelism breaks ordering guarantees.
- Virtual threads do not eliminate correctness complexity.
- Partition-based ordering is fundamental in Kafka.

---

# 🔵 Part 3 — Parallel Consumer Library Comparison

Compare your manual implementation with the Confluent Parallel Consumer library.

---

## What to Compare

| Feature | Manual Implementation | Parallel Consumer |
|----------|----------------------|------------------|
| Offset tracking | Manual | Built-in |
| Ordering control | Complex | Configurable |
| Retry handling | Custom | Supported |
| Code complexity | High | Lower |
| Fine-grained control | Maximum | Structured |

---

# 🧠 Core Concepts Explored

This project unifies:

- Producer parallel scaling
- Consumer concurrency control
- Partition-level ordering semantics
- Backpressure and work delegation
- Throughput vs correctness trade-offs

---

# 📈 Expected Insights

By the end of Project 3, you should understand:

- Why single-producer throughput has a ceiling.
- How horizontal producer scaling shifts bottlenecks.
- Why consumer parallelism is harder than producer scaling.
- Why offset management is more important than raw speed.
- Why distributed systems are about correctness boundaries, not just performance.

---

# 🏁 Deliverables

- Throughput scaling graph (1 → 8 producers)
- Broker CPU vs producer count analysis
- Manual parallel consumer implementation
- Failure scenario tests
- Comparative analysis with the Parallel Consumer library

---

# 🚀 Why This Matters

Most engineers learn:

- How to use Kafka.
- How to configure Kafka.

This project teaches:

- How Kafka scales.
- Where Kafka breaks.
- How correctness and parallelism interact.
- Why concurrency decisions define system reliability.

This transitions from performance tuning to distributed systems engineering.
