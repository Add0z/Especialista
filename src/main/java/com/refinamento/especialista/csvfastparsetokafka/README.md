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

## ⚙️ Tuning do Kafka Producer

As configurações abaixo foram refinadas para estabilizar o throughput máximo:

| Propriedade | Valor | Motivação Técnica |
| :--- | :--- | :--- |
| `batch-size` | `524288` (512KB) | Maximizar o agrupamento de registros por pacote TCP. |
| `linger.ms` | `100` | Garante que o lote seja disparado por tamanho e não por tempo. |
| `compression.type` | `lz4` | Melhor equilíbrio entre redução de payload e baixo custo de CPU. |
| `buffer.memory` | `134217728` (128MB) | Margem para evitar backpressure durante picos de envio. |

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
