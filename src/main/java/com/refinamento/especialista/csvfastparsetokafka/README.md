# Kafka Ingestion Benchmark: High-Performance CSV to Topic

Este projeto apresenta uma análise de performance e implementação de uma pipeline de ingestão massiva de dados, movendo mais de 5,2 milhões de registros de um arquivo CSV para o Apache Kafka. O foco principal foi o equilíbrio entre Throughput (vazão), utilização de CPU e eficiência de I/O.

## 🚀 Resultados do Benchmark

* **Volume de Dados:** 5.208.181 registros (CSV).
* **Throughput Máximo (12 Partições):** **361.002 registros/segundo**.
* **Throughput (6 Partições):** **342.345 registros/segundo**.
* **Tempo Total (12 Partições):** ~14,4 segundos.
* **Tempo Total (6 Partições):** ~15,2 segundos.
* **Stack:** Java 21, Spring Boot 3.x, Apache Kafka.

---

## 🧠 Decisões Arquiteturais e Análise de Performance

### 1. O Custo Oculto do `substring()` e Alocação de Objetos

Uma das principais frentes de otimização foi o método de parsing. Inicialmente, avaliamos um parser manual utilizando `substring()` para extrair cada token.

* **O Problema:** O uso extensivo de `line.substring(start, i)` dentro de um loop para 5.2 milhões de linhas gera uma pressão imensa no Garbage Collector (GC). Cada chamada cria um novo objeto `String` na Heap.
* **A Solução:** Evoluímos para um modelo onde priorizamos o tratamento direto de `byte[]` sempre que possível, reduzindo a criação de objetos temporários e mantendo a CPU focada no envio de dados, não na limpeza de memória.

### 2. `byte[]` vs. Serialização Binária (Pseudo-Avro)

Comparamos o envio de dados como `byte[]` contra uma simulação de **Avro (binário)**.

* **Resultado:** Em ambiente **localhost**, o custo de CPU para converter tipos (ex: `Double.parseDouble()`) via código Java superou o ganho de economia de rede. A abordagem de bytes brutos provou-se o "teto" de performance local, atingindo o pico de **361k rec/sec**.

### 3. Concorrência e Threading: O Paradoxo do Paralelismo

Testamos o impacto de `parallelStream()` e Virtual Threads (Java 21).

* **Análise:** Para um parser leve, o modo **sequencial** superou o paralelo. Isso ocorreu devido à redução de contenção de locks no `RecordAccumulator` do Kafka Producer e à eliminação do overhead de troca de contexto, permitindo que um único núcleo de CPU processasse o arquivo de forma linear e ininterrupta.

---

## ⚙️ Kafka Producer Tuning — Final Configuration & Rationale

All configurations below were defined after iterative benchmarking against a single-node Apache Kafka broker running locally.

| Property | Final Value | Why This Won |
|-----------|-------------|--------------|
| `batch.size` | `524288` (512KB) | Large batches significantly reduced request frequency and improved per-request efficiency. |
| `linger.ms` | `100` | Allowed dense batches to form, shifting the trigger from time-based to size-based dispatch under sustained load. |
| `compression.type` | `lz4` | Fast compression with low CPU overhead, improving network efficiency without becoming CPU-bound. |
| `buffer.memory` | `134217728` (128MB) | Prevented producer-side backpressure during peak accumulation phases. |

---

### 🧠 Why This Configuration Was Superior

During experimentation, three important dynamics emerged:

#### 1️⃣ Sender Thread Efficiency Ceiling

The Kafka Producer uses a **single internal Sender I/O thread** responsible for:

* Building produce requests  
* Managing batching  
* Sending data to the broker  

Increasing caller concurrency (`parallelStream`, virtual threads, custom executors) did not significantly increase throughput.

This confirmed that the bottleneck was not application-level parallelism but:

> The efficiency of the Producer's internal dispatch path and the broker append pipeline.

Once saturated, adding more threads only introduced contention.

---

#### 2️⃣ Batch Density > Dispatch Frequency

Earlier tests with smaller batch sizes showed that `linger.ms=0` performed better.

However, after increasing:

* `batch.size` to 512KB  
* `buffer.memory` to 128MB  

Setting `linger.ms=100` allowed the producer to:

* Build denser batches  
* Reduce total request count  
* Reduce syscall overhead  
* Reduce broker-side request handling cost  

Result:

* Similar or slightly better throughput  
* Lower CPU usage  
* Higher throughput per CPU cycle  

This configuration was not just faster — it was more **efficient**.

---

#### 3️⃣ Compression Trade-Off

* `zstd` provided strong compression but higher CPU cost.
* `lz4` provided the best throughput/CPU balance for localhost.

Under local conditions (low network latency), CPU becomes the limiting factor faster than network bandwidth.

---

# 📈 Performance Evolution Timeline

This section summarizes how each experiment shaped the final configuration.

---

## 🔹 Phase 1 — Baseline Sequential Producer

**Configuration:**

* Default batch  
* Default buffer  
* `linger.ms=0`  
* `zstd` compression  
* 6 partitions  

**Result:** ~340k records/sec  

**Observation:**  
CPU usage peaked but machine cores were not fully saturated.  
Throughput appeared bounded by broker append path and producer dispatch efficiency.

---

## 🔹 Phase 2 — Partition Scaling (6 → 12)

**Result:** ~342k → ~361k records/sec (~5.5% gain)

**Conclusion:**  
Increasing partitions improved parallelism at the broker log level, but gains were modest due to:

* Single broker  
* Shared network threads  
* Shared disk subsystem  

Partitioning helps — but does not scale linearly on a single node.

---

## 🔹 Phase 3 — Concurrency Experiments

Tested:

* `parallelStream()`  
* Explicit ForkJoin parallelism tuning  
* Virtual threads with backpressure semaphore  

**Findings:**

* Parallel stream yielded negligible improvement.  
* Virtual threads reduced throughput drastically (~74k records/sec).  
* Sequential processing outperformed parallel under low parsing complexity.

**Conclusion:**

The bottleneck was not parsing or caller concurrency.

It was:

> Producer Sender thread efficiency + broker ingestion path.

Wrapping a non-blocking `send()` call in additional scheduling layers introduced overhead and reduced throughput.

---

## 🔹 Phase 4 — Serialization Strategy

Compared:

* Raw `byte[]`  
* Binary encoding (Pseudo-Avro simulation)  

**Result:**  
Binary conversion reduced throughput to ~266k records/sec.

**Conclusion:**  

On localhost, CPU cost of serialization outweighed network savings.

The system was CPU-bound, not network-bound.

---

## 🔹 Phase 5 — Batch & Buffer Scaling (Final Optimization)

Changes:

* `batch.size` → 512KB  
* `buffer.memory` → 128MB  
* `compression` → `lz4`  
* `linger.ms` → 100  

**Final Result:**

* ~361k records/sec  
* Lower CPU usage than previous high-throughput configuration  
* Improved efficiency per request  

This configuration shifted the system from:

Request-frequency optimized  
→ Batch-density optimized  

Which proved superior under sustained ingestion load.

---

# 🏁 Final Architectural Insight

This benchmark revealed a key principle:

> In high-throughput ingestion systems, efficiency per request matters more than raw parallelism.

On a single-node broker:

* Throughput is bounded by sender thread efficiency and broker append path.
* Increasing application-level concurrency does not overcome internal Kafka limits.
* Proper batching and memory sizing provide more gains than thread multiplication.

---

## 💡 Production Consideration

In a multi-broker cluster environment:

Throughput would scale horizontally via:

* Increasing broker count  
* Increasing partition count  
* Running multiple producer instances  

This would distribute leadership and log append operations across machines, overcoming the single-node ceiling observed in this benchmark.

---

## ⚖️ Visão Sênior: Localhost vs. Ambiente de Nuvem

Este benchmark revela trade-offs essenciais para decisões de arquitetura em grandes corporações (como Accenture ou Magalu):

1. **Gargalo de CPU vs. Rede:** No localhost, o objetivo é reduzir o processamento do Java (CPU-Bound). Na nuvem, o objetivo seria reduzir o tamanho da mensagem (I/O-Bound) via Avro para economizar custos de transferência de dados (*egress*).
2. **Eficiência de Memória:** O custo de alocação de Strings (via `substring`) é aceitável em aplicações de baixa carga, mas torna-se proibitivo em pipelines de Big Data, onde o processamento orientado a bytes é a norma.
3. **Escalabilidade:** Escalabilidade da Ingestão: Enquanto o modo sequencial venceu localmente (devido ao baixo overhead), em um cenário de nuvem a ingestão escalaria através do aumento do número de partições do tópico e da execução de múltiplas instâncias do Producer em paralelo (ex: em um cluster Kubernetes). Isso permitiria distribuir a carga de escrita entre diferentes brokers, superando o limite de vazão de uma única CPU ou placa de rede.

---

### Como Rodar

1. Certifique-se de ter um broker Kafka em `localhost:9092`.
2. Configure o tópico com **12 partições** para performance máxima.
3. O arquivo `books_large.csv` deve estar na raiz ou selecione outro csv.
4. Execute `IngestionBenchmark.java`.
