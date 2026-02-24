"""
Generate Integration Architecture PowerPoint presentation.

Uses the slides-creator skill's PowerPointGenerator to create a professional
.pptx file from the integration architecture content.
"""

import sys
import os

# Add the skills utils to path
sys.path.insert(0, os.path.join(
    os.path.dirname(__file__), '..', '..', '.agents', 'skills', 'slides-creator', 'utils'
))

from pptx_generator import PowerPointGenerator, PptxConfig


def build_slides():
    """Build slide data for the Integration Architecture presentation."""
    slides = []

    # Slide 1: Title
    slides.append({
        'type': 'title',
        'content': {
            'title': 'Integration Architecture',
            'author': 'Wallace Espindola',
            'date': '2026-02-24',
            'tags': ['Integration', 'Kafka', 'Oracle', 'CDC']
        }
    })

    # Slide 2: Agenda
    slides.append({
        'type': 'content',
        'content': {
            'heading': 'Agenda',
            'body': '',
            'bullet_points': [
                'Integration Scenario & Payload Sizing',
                'Throughput Analysis (5 Streams)',
                'Integration Options (Streaming, API, DB)',
                'Oracle-to-Oracle Architecture & Latency Model',
                'High Maturity Model & Golden Triangle'
            ]
        }
    })

    # Slide 3: Integration Scenario
    slides.append({
        'type': 'content',
        'content': {
            'heading': '1. Integration Scenario',
            'body': '5 topics/streams with the following message profile:',
            'bullet_points': [
                '100 fields per message, 10 characters per field',
                'Raw payload: 100 x 10 = 1,000 bytes (1 KB)',
                'Traffic rates: 100 / 1K / 10K / 100K msgs/min/stream',
                'Total across 5 streams: 500 to 500,000 msgs/min'
            ]
        }
    })

    # Slide 4: Message Size by Protocol
    slides.append({
        'type': 'content',
        'content': {
            'heading': 'Message Size by Protocol',
            'body': 'Each protocol adds overhead on top of the raw 1 KB payload:',
            'bullet_points': [
                'Kafka + Avro: 1,185 B (18.5% overhead) - most efficient',
                'Pulsar + Avro: 1,250 B (25.0% overhead)',
                'gRPC + Protobuf: 1,300 B (30.0% overhead)',
                'REST + Protobuf: 1,900 B (90.0% overhead) - least efficient'
            ]
        }
    })

    # Slide 5: Overhead Breakdown
    slides.append({
        'type': 'content',
        'content': {
            'heading': 'Protocol Overhead Breakdown',
            'body': 'Where do the extra bytes come from?',
            'bullet_points': [
                'Avro binary encoding: ~100 B (field lengths + schema fingerprint)',
                'Kafka record framing: ~85 B (timestamp, key, headers, CRC)',
                'Pulsar metadata: ~150 B (publish time, sequence ID, properties)',
                'Protobuf encoding: ~200-300 B (field tags + length delimiters)',
                'REST/HTTP headers: ~600-700 B (Content-Type, Host, etc.)'
            ]
        }
    })

    # Slide 6: Throughput Table
    slides.append({
        'type': 'content',
        'content': {
            'heading': '2. Throughput Analysis (5 Streams)',
            'body': 'Formula: MB/min = total_msgs x msg_size / 1,000,000',
            'bullet_points': [
                '100/min/stream -> 500 total: 0.59 MB/min (Kafka+Avro)',
                '1K/min/stream -> 5,000 total: 5.93 MB/min (Kafka+Avro)',
                '10K/min/stream -> 50,000 total: 59.25 MB/min (Kafka+Avro)',
                '100K/min/stream -> 500,000 total: 592.5 MB/min (Kafka+Avro)'
            ]
        }
    })

    # Slide 7: Peak Bandwidth
    slides.append({
        'type': 'content',
        'content': {
            'heading': 'Bandwidth at Peak (100K msgs/min/stream)',
            'body': 'At 500K msgs/min across 5 streams:',
            'bullet_points': [
                'Kafka+Avro: 592.5 MB/min | 35.6 GB/hr | ~79 Mbps',
                'Pulsar+Avro: 625 MB/min | 37.5 GB/hr | ~83 Mbps',
                'gRPC+Protobuf: 650 MB/min | 39 GB/hr | ~87 Mbps',
                'REST+Protobuf: 950 MB/min | 57 GB/hr | ~127 Mbps',
                'Kafka+Avro needs ~79 Mbps - well within 1 Gbps link'
            ]
        }
    })

    # Slide 8: Integration Options
    slides.append({
        'type': 'content',
        'content': {
            'heading': '3. Integration Options',
            'body': 'Three categories of integration approaches:',
            'bullet_points': [
                'Streaming: Kafka+Avro, Pulsar, Redpanda, NATS, RabbitMQ, Redis',
                'API: gRPC+Protobuf, REST+Protobuf',
                'Database: Kafka Connect+CDC, Oracle GoldenGate, Data Guard'
            ]
        }
    })

    # Slide 9: Oracle to Oracle
    slides.append({
        'type': 'code',
        'content': {
            'language': 'text',
            'code': (
                'Oracle (Write DB)\n'
                '      |\n'
                '      v\n'
                '  CDC (Debezium / GoldenGate)\n'
                '      |\n'
                '      v\n'
                '  Kafka / Event Backbone\n'
                '      |\n'
                '      v\n'
                'Oracle (Read DB)\n'
                '\n'
                'Guarantees:\n'
                '  - Idempotency\n'
                '  - Sequence numbers for ordering\n'
                '  - Schema Registry governance\n'
                '\n'
                'Principles:\n'
                '  - Single writer\n'
                '  - Upsert sinks\n'
                '  - Replay capability\n'
                '  - Deterministic ordering'
            )
        }
    })

    # Slide 10: Latency Factor Model
    slides.append({
        'type': 'content',
        'content': {
            'heading': '5. Latency Factor Model',
            'body': 'Baseline: Factor 1 = Same database (write + read, < 1 ms)',
            'bullet_points': [
                'Oracle RAC: 1-2x (1-2 ms) | Data Guard: 5-50x (5-50 ms)',
                'GoldenGate: 2-30x (2-30 ms) | Kafka CDC: 10-100x',
                'gRPC Streaming: 1-10x (1-10 ms) | REST API: 2-40x',
                'DB Polling: 50-3,000+x (50 ms to 3+ seconds)',
                'gRPC Streaming offers lowest latency for API integration'
            ]
        }
    })

    # Slide 11: Finance Industry Reality
    slides.append({
        'type': 'content',
        'content': {
            'heading': '6. Finance Industry Reality',
            'body': 'Banks optimize for four core principles:',
            'bullet_points': [
                'Determinism - predictable, reproducible outcomes',
                'Auditability - full traceability of every transaction',
                'Replayability - reconstruct state from event history',
                'Isolation - failure containment, no cascading'
            ]
        }
    })

    # Slide 12: Most Typical Stack
    slides.append({
        'type': 'content',
        'content': {
            'heading': 'Most Typical Stack',
            'body': 'Three layers form the foundation:',
            'bullet_points': [
                'System of Record: Oracle Database + Data Guard',
                'Integration: GoldenGate / CDC + Kafka backbone',
                'Consumption: APIs, Fraud systems, Analytics, Reporting'
            ]
        }
    })

    # Slide 13: Maturity Model
    slides.append({
        'type': 'content',
        'content': {
            'heading': '7. Integration Maturity Model',
            'body': 'Six levels of integration maturity:',
            'bullet_points': [
                'Level 0: Shared DB (simple, tightly coupled)',
                'Level 1: DB Replication (Data Guard HA/DR)',
                'Level 2: CDC Integration (Oracle -> Kafka -> Consumers)',
                'Level 3: Event Backbone (enterprise streaming)',
                'Level 4-5: Domain Events / Streaming Bank (event-driven)'
            ]
        }
    })

    # Slide 14: Five Mistakes
    slides.append({
        'type': 'content',
        'content': {
            'heading': '8. Five Banking Architecture Mistakes',
            'body': 'Common pitfalls and their solutions:',
            'bullet_points': [
                '1. Dual writes without coordination -> inconsistency',
                '2. Treating replication as integration -> tight coupling',
                '3. No replay/audit capability -> cannot reconstruct state',
                '4. Expecting exactly-once delivery -> a transport myth',
                '5. Latency coupling assumptions -> downstream is not constant'
            ]
        }
    })

    # Slide 15: Banking Solution
    slides.append({
        'type': 'content',
        'content': {
            'heading': 'The Banking Solution',
            'body': 'Three principles to avoid the five mistakes:',
            'bullet_points': [
                'At-least-once delivery (never lose a message)',
                'Idempotent consumers (safe to process duplicates)',
                'Ordered events (deterministic state reconstruction)'
            ]
        }
    })

    # Slide 16: Golden Triangle
    slides.append({
        'type': 'code',
        'content': {
            'language': 'text',
            'code': (
                '    Golden Triangle of Banking Architecture\n'
                '\n'
                '    +---------------------------+\n'
                '    |  Experience Layer (APIs)   |  <- Agility\n'
                '    +-------------+-------------+\n'
                '                  |\n'
                '    +-------------v-------------+\n'
                '    | Integration Backbone       |  <- Decoupling\n'
                '    |       (Kafka)              |\n'
                '    +-------------+-------------+\n'
                '                  |\n'
                '    +-------------v-------------+\n'
                '    |  System of Record          |  <- Correctness\n'
                '    |      (Oracle)              |\n'
                '    +---------------------------+\n'
                '\n'
                'Oracle -> correctness & transactional integrity\n'
                'Kafka  -> decoupling & event distribution\n'
                'APIs   -> agility & consumer experience'
            )
        }
    })

    # Slide 17: Final Position
    slides.append({
        'type': 'content',
        'content': {
            'heading': '10. Final Architectural Position',
            'body': 'Architecture aligns with modern Tier-1 banking patterns:',
            'bullet_points': [
                'Oracle as system of record with one-direction data flow',
                'CDC replication feeding an event backbone',
                'Idempotent processing with schema governance',
                'Corresponds to Banking Maturity Level 2-3'
            ]
        }
    })

    # Slide 18: Key Takeaways
    slides.append({
        'type': 'conclusion',
        'content': {
            'heading': 'Key Takeaways',
            'takeaways': [
                'Kafka + Avro is the most efficient wire protocol (18.5% overhead)',
                'CDC + Event Backbone is the standard banking integration pattern',
                'Design for idempotency, ordering, and replay',
                'Avoid dual writes and exactly-once assumptions',
                'Target Maturity Level 2-3 for modern banking'
            ],
            'cta': 'Wallace Espindola | github.com/wallaceespindola | linkedin.com/in/wallaceespindola'
        }
    })

    return slides


def main():
    """Generate the PPTX presentation."""
    config = PptxConfig(theme='technical')
    generator = PowerPointGenerator(config)

    slides_data = build_slides()
    output_path = os.path.join(
        os.path.dirname(__file__),
        'banking-integration-architecture.pptx'
    )

    metadata = {
        'title': 'Banking Integration Architecture',
        'author': 'Wallace Espindola',
        'subject': 'Banking Integration Architecture - Full Context Summary'
    }

    success = generator.generate_presentation(slides_data, output_path, metadata)

    if success:
        print(f"Slides generated: {len(slides_data)} slides")
        print(f"Output: {output_path}")
    else:
        print("Failed to generate presentation")
        sys.exit(1)


if __name__ == '__main__':
    main()
