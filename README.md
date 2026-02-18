# Java Especialista - High-Performance Distributed Systems Mastery

## 🎯 Mission

This repository serves as a **Mastery Track** for designing high-performance, scalable, and event-driven Java systems. The focus is on **performance, correctness, and production realism**—going beyond tutorials to build robust, scalable architectures.

The core philosophy is:
> "We are not learning frameworks. We are learning how data moves, how threads behave, and how systems fail under pressure."

## 🧠 Core Principles

1. **Performance First**: Every solution considers allocation cost, thread usage, I/O patterns, and GC pressure.
2. **No "Magic" Abstractions**: Understanding what Spring/Frameworks do underneath. Using manual wiring when performance dictates.
3. **Trade-offs**: Always comparing Blocking vs. Non-blocking, Batch vs. Streaming, Imperative vs. Functional.
4. **Mechanics over Abstractions**: Learning the "how" and "why" before applying the "what".

## 📚 Technology Stack

This project leverages bleeding-edge Java features and modern distributed systems patterns:

* **Java 25** (Preview Features):
  * Virtual Threads & Structured Concurrency
  * Scoped Values (replacing ThreadLocal)
  * Sequenced Collections & Advanced Stream Gatherers
  * Vector API (where applicable)
* **Apache Kafka**:
  * KRaft mode (No Zookeeper)
  * Advanced Partitioning Strategies
  * Idempotent Producers & Exactly-Once Semantics
* **Spring Boot 3.5**:
  * Used judiciously, focusing on explicit configuration over auto-configuration magic.

## 📂 Modules & Exercises

### 1. CSV Fast Parse to Kafka (`csvfastparsetokafka`)

A high-throughput ingestion pipeline designed to process large datasets (e.g., 1.7GB+ CSV files) and publish to Kafka with minimal latency.

* **Goal**: Maximize records/sec.
* **Techniques**: Zero-copy parsing, batching strategies, parallel processing with Virtual Threads.
* **Benchmarks**: Comparisons between naive approaches and optimized, memory-aware solutions.

## 🚀 Getting Started

### Prerequisites

* **Java 25** (Ensure preview features are enabled)
* **Docker & Docker Compose** (for Kafka/Redpanda environment)
* **Maven**

### Running the Environment

Start the infrastructure (Kafka, Redpanda Console, interactive tools):

```bash
docker compose up -d
```

### Building the Project

```bash
mvn clean package -DskipTests
```

### Running Tests

We emphasize **Testcontainers** for realistic integration testing:

```bash
mvn test
```

## 📈 Roadmap

* [ ] Advanced Kafka Streams Topologies (State Stores, Reprocessing)
* [ ] Resilient Consumer Patterns (Retry Topics, Circuit Breakers)
* [ ] Distributed Tracing & Observability (OpenTelemetry)
* [ ] High-Performance Database Interactions (Connection Pooling, Batching)

---
*Built for the [Java Especialista] Mastery Track.*
