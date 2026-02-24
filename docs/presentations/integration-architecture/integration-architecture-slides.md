---
marp: true
theme: default
paginate: true
backgroundColor: #0D1117
color: #E6EDF3
style: |
  section {
    font-family: 'San Francisco', 'Helvetica Neue', Arial, sans-serif;
  }
  h1, h2, h3 {
    color: #58A6FF;
  }
  strong {
    color: #79C0FF;
  }
  table {
    font-size: 0.75em;
    color: #E6EDF3;
  }
  th {
    background-color: #161B22;
    color: #58A6FF;
  }
  td {
    background-color: #0D1117;
  }
  code {
    background-color: #161B22;
    color: #79C0FF;
  }
  blockquote {
    color: #8B949E;
    border-left-color: #58A6FF;
  }
  a {
    color: #58A6FF;
  }
  footer {
    color: #8B949E;
  }
---

<!-- _class: lead -->

# Banking Integration Architecture

## Full Context Summary

**Wallace Espindola**
2026-02-24 | Banking, Integration, Kafka, Oracle, CDC

---

## Agenda

1. Integration Scenario & Payload Sizing
2. Throughput Analysis (5 Streams)
3. Integration Options
4. Oracle-to-Oracle Architecture
5. Latency Factor Model
6. Banking Industry Reality
7. Maturity Model & Common Mistakes
8. Golden Triangle of Banking Architecture

---

## 1. Integration Scenario

**5 topics/streams** with the following message profile:

- **100 fields** per message
- **10 characters** per field
- Raw payload: `100 × 10 = 1,000 bytes (1 KB)`

**Traffic rates under evaluation:**

| Rate (msgs/min/stream) | Total msgs/min (5 streams) |
|------------------------:|---------------------------:|
| 100                     | 500                        |
| 1,000                   | 5,000                      |
| 10,000                  | 50,000                     |
| 100,000                 | 500,000                    |

---

## Message Size by Protocol

Each protocol adds overhead on top of the raw 1 KB payload:

| Protocol          | Msg Size | Overhead | Overhead % |
|-------------------|----------|----------|------------|
| **Kafka + Avro**  | 1,185 B  | 185 B    | 18.5%      |
| **Pulsar + Avro** | 1,250 B  | 250 B    | 25.0%      |
| **gRPC + Protobuf** | 1,300 B | 300 B   | 30.0%      |
| **REST + Protobuf** | 1,900 B | 900 B   | 90.0%      |

> REST + Protobuf is nearly **2x** the raw payload due to HTTP/1.1 header overhead.
> Kafka + Avro is the **most wire-efficient** combination.

---

## Overhead Breakdown

- **Avro binary encoding:** ~100 B (length-prefixed fields + schema fingerprint)
- **Kafka record framing:** ~85 B (timestamp, key, headers, CRC)
- **Pulsar message metadata:** ~150 B (publish time, sequence ID, properties)
- **Protobuf encoding:** ~200–300 B (field tags + length delimiters)
- **gRPC framing:** ~100 B (HTTP/2 headers + 5-byte length-prefix)
- **REST/HTTP headers:** ~600–700 B (Content-Type, Host, Connection, etc.)

---

## 2. Throughput Table (5 Streams)

**Formula:** `MB/min = total_msgs × msg_size / 1,000,000`

| Rate/stream | Total/min | Kafka+Avro | Pulsar+Avro | gRPC+PB | REST+PB |
|------------:|----------:|-----------:|------------:|--------:|--------:|
| 100         | 500       | 0.59 MB   | 0.63 MB     | 0.65 MB | 0.95 MB |
| 1,000       | 5,000     | 5.93 MB   | 6.25 MB     | 6.50 MB | 9.50 MB |
| 10,000      | 50,000    | 59.25 MB  | 62.50 MB    | 65.0 MB | 95.0 MB |
| 100,000     | 500,000   | 592.5 MB  | 625.0 MB    | 650 MB  | 950 MB  |

---

## Bandwidth at Peak (100K msgs/min/stream)

| Metric           | Kafka+Avro | Pulsar+Avro | gRPC+PB  | REST+PB    |
|------------------|------------|-------------|----------|------------|
| **MB/min**       | 592.5      | 625.0       | 650.0    | 950.0      |
| **GB/hour**      | 35.6       | 37.5        | 39.0     | 57.0       |
| **GB/day**       | 853        | 900         | 936      | 1,368      |
| **Network Mbps** | ~79        | ~83         | ~87      | ~127       |

> At 500K msgs/min, Kafka+Avro needs **~79 Mbps** — well within 1 Gbps.
> REST+Protobuf needs **~127 Mbps** — 60% more for the same data.

---

## 3. Integration Options

### Streaming / Event-Driven
- Kafka + Avro
- Pulsar
- Redpanda
- NATS JetStream
- RabbitMQ
- Redis Streams

### API-Based
- gRPC + Protobuf
- REST + Protobuf

### Database Integration
- Kafka Connect + CDC
- Oracle GoldenGate
- Oracle Data Guard / Active Data Guard

---

## 4. Oracle → Oracle Integration

**Recommended architecture:**

```
Oracle (Write DB)
      ↓
  CDC (Debezium / GoldenGate)
      ↓
  Kafka / Event Backbone
      ↓
Oracle (Read DB)
```

**Key guarantees:** Idempotency, sequence ordering, schema governance

**Design principles:** Single writer, upsert sinks, replay capability, deterministic ordering

---

## 5. Latency Factor Model

Baseline: **Factor 1 = Same database (write + read)**

| Integration      | Factor    | Typical Latency |
|------------------|-----------|-----------------|
| Same DB          | 1         | < 1 ms          |
| Oracle RAC       | 1–2       | 1–2 ms          |
| Data Guard       | 5–50      | 5–50 ms         |
| GoldenGate       | 2–30      | 2–30 ms         |
| Kafka CDC        | 10–100    | 10–100 ms       |
| Pulsar CDC       | 10–100    | 10–100 ms       |
| gRPC Streaming   | 1–10      | 1–10 ms         |
| REST API         | 2–40      | 2–40 ms         |
| DB Polling       | 50–3,000+ | 50 ms – 3+ sec  |

---

## 6. Banking Industry Reality

Banks optimize for:

- **Determinism** — predictable, reproducible outcomes
- **Auditability** — full traceability of every transaction
- **Replayability** — reconstruct state from event history
- **Isolation** — failure containment, no cascading

### Most Typical Stack

| Layer            | Technology            |
|------------------|-----------------------|
| System of Record | Oracle DB + Data Guard |
| Integration      | GoldenGate / CDC + Kafka |
| Consumption      | APIs, Fraud, Analytics, Reporting |

---

## 7. Banking Integration Maturity Model

| Level | Name           | Description                              |
|------:|----------------|------------------------------------------|
| 0     | Shared DB      | Simple but tightly coupled               |
| 1     | DB Replication | Data Guard HA/DR                         |
| 2     | CDC Integration| Oracle → Kafka → Consumers               |
| 3     | Event Backbone | Enterprise streaming architecture        |
| 4     | Domain Events  | Business events replace table replication |
| 5     | Streaming Bank | Event-driven enterprise                  |

> Target: **Level 2–3** for modern banking architectures.

---

## 8. Five Banking Architecture Mistakes

1. **Dual writes** — writing to two systems without coordination
2. **Treating replication as integration** — replication copies data; integration decouples
3. **No replay/audit capability** — cannot reconstruct state without event logs
4. **Expecting exactly-once delivery** — a myth at the transport layer
5. **Latency coupling assumptions** — downstream is not constant-time

### The Fix

- At-least-once delivery
- Idempotent consumers
- Ordered events

---

## 9. Golden Triangle of Banking Architecture

```
      ┌─────────────────────────┐
      │  Experience Layer (APIs) │  ← Agility
      └────────────┬────────────┘
                   │
      ┌────────────▼────────────┐
      │ Integration Backbone    │  ← Decoupling
      │       (Kafka)           │
      └────────────┬────────────┘
                   │
      ┌────────────▼────────────┐
      │  System of Record       │  ← Correctness
      │      (Oracle)           │
      └─────────────────────────┘
```

---

## Architectural Roles

| Layer                   | Responsibility                          |
|-------------------------|-----------------------------------------|
| **Oracle (Record)**     | Correctness and transactional integrity |
| **Kafka (Backbone)**    | Decoupling and event distribution       |
| **APIs (Experience)**   | Agility and consumer experience         |

---

## 10. Final Architectural Position

The described architecture aligns with **modern Tier-1 banking patterns**:

- Oracle as system of record
- One-direction data flow
- CDC replication
- Event backbone
- Idempotent processing
- Schema governance

**Banking Maturity Level: 2–3**

---

<!-- _class: lead -->

# Key Takeaways

- **Kafka + Avro** is the most efficient wire protocol (18.5% overhead)
- **CDC + Event Backbone** is the standard integration pattern for banks
- Design for **idempotency, ordering, and replay**
- Avoid dual writes and exactly-once assumptions
- Target **Maturity Level 2–3** for modern apps

---

<!-- _class: lead -->

# Thank You

**Wallace Espindola**

GitHub: [github.com/wallaceespindola](https://github.com/wallaceespindola)
LinkedIn: [linkedin.com/in/wallaceespindola](https://www.linkedin.com/in/wallaceespindola)
