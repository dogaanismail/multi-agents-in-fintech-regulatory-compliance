#!/usr/bin/env python3
"""
Dead-letter topic tool — inspect and replay parked messages.

Both the Java services (Spring DeadLetterPublishingRecoverer) and the Python
MARL orchestrator park failed messages on a `<topic>.DLT` topic, preserving the
original Avro wire-format bytes plus metadata headers describing where the
message came from and why it failed.

Usage:
    python dlt-tool.py inspect <dlt-topic> [--max N]
    python dlt-tool.py replay  <dlt-topic> [--to <topic>] [--max N]

    inspect  Print each parked message: key, origin topic/partition/offset and
             the recorded error, without consuming the topic.
    replay   Re-publish the original bytes back to the source topic (taken from
             the metadata header, or --to) so the owning consumer reprocesses it.

Environment:
    KAFKA_BOOTSTRAP_SERVERS   default localhost:9092

Replay is safe to run repeatedly only because the consumers are idempotent;
it re-delivers under at-least-once semantics.
"""

import argparse
import os
import sys
import uuid

from confluent_kafka import Consumer, Producer

BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
IDLE_POLLS_BEFORE_STOP = 5

JAVA_DLT_ORIGIN_TOPIC = "kafka_dlt-original-topic"
PY_DLT_ORIGIN_TOPIC = "dlt-origin-topic"


def _decode(value):
    if value is None:
        return ""
    if isinstance(value, bytes):
        return value.decode("utf-8", errors="replace")
    return str(value)


def _headers_dict(headers):
    return {key: value for key, value in (headers or [])}


def _origin_topic(headers, dlt_topic, override):
    if override:
        return override
    decoded = {key: _decode(value) for key, value in headers.items()}
    return (
        decoded.get(PY_DLT_ORIGIN_TOPIC)
        or decoded.get(JAVA_DLT_ORIGIN_TOPIC)
        or (dlt_topic[:-4] if dlt_topic.endswith(".DLT") else dlt_topic)
    )


def _create_consumer():
    return Consumer({
        "bootstrap.servers": BOOTSTRAP_SERVERS,
        "group.id": f"dlt-tool-{uuid.uuid4()}",
        "auto.offset.reset": "earliest",
        "enable.auto.commit": False,
    })


def _iterate(dlt_topic, max_messages):
    consumer = _create_consumer()
    consumer.subscribe([dlt_topic])
    idle = 0
    seen = 0
    try:
        while True:
            if max_messages and seen >= max_messages:
                break
            msg = consumer.poll(timeout=1.0)
            if msg is None:
                idle += 1
                if idle >= IDLE_POLLS_BEFORE_STOP:
                    break
                continue
            if msg.error():
                continue
            idle = 0
            seen += 1
            yield msg
    finally:
        consumer.close()


def inspect(dlt_topic, max_messages):
    count = 0
    for msg in _iterate(dlt_topic, max_messages):
        headers = {key: _decode(value) for key, value in _headers_dict(msg.headers()).items()}
        origin = (
            headers.get(PY_DLT_ORIGIN_TOPIC)
            or headers.get(JAVA_DLT_ORIGIN_TOPIC)
            or "?"
        )
        partition = headers.get("dlt-origin-partition") or headers.get("kafka_dlt-original-partition") or "?"
        offset = headers.get("dlt-origin-offset") or headers.get("kafka_dlt-original-offset") or "?"
        reason = headers.get("dlt-error-reason") or "?"
        error = headers.get("dlt-error-message") or headers.get("kafka_dlt-exception-message") or ""
        count += 1
        print(f"[{count}] key={_decode(msg.key())} origin={origin}[{partition}]@{offset} "
              f"reason={reason} error={error[:200]}")
    print(f"\n{count} message(s) in {dlt_topic}")


def replay(dlt_topic, target, max_messages):
    producer = Producer({
        "bootstrap.servers": BOOTSTRAP_SERVERS,
        "acks": "all",
        "enable.idempotence": True,
    })
    replayed = 0
    for msg in _iterate(dlt_topic, max_messages):
        headers = _headers_dict(msg.headers())
        destination = _origin_topic(headers, dlt_topic, target)
        forwarded = [(key, value) for key, value in headers.items() if not key.startswith(("dlt-", "kafka_dlt-"))]
        producer.produce(topic=destination, value=msg.value(), key=msg.key(), headers=forwarded)
        producer.poll(0)
        replayed += 1
        print(f"replayed key={_decode(msg.key())} -> {destination}")
    producer.flush(10)
    print(f"\nReplayed {replayed} message(s) from {dlt_topic}")


def main():
    parser = argparse.ArgumentParser(description="Inspect and replay Kafka dead-letter messages.")
    sub = parser.add_subparsers(dest="command", required=True)

    inspect_parser = sub.add_parser("inspect", help="Print parked messages and their metadata.")
    inspect_parser.add_argument("topic")
    inspect_parser.add_argument("--max", type=int, default=0, help="Stop after N messages (0 = all).")

    replay_parser = sub.add_parser("replay", help="Re-publish parked messages to the source topic.")
    replay_parser.add_argument("topic")
    replay_parser.add_argument("--to", default=None, help="Override the destination topic.")
    replay_parser.add_argument("--max", type=int, default=0, help="Stop after N messages (0 = all).")

    args = parser.parse_args()
    if args.command == "inspect":
        inspect(args.topic, args.max)
    elif args.command == "replay":
        replay(args.topic, args.to, args.max)
    else:
        parser.print_help()
        sys.exit(1)


if __name__ == "__main__":
    main()
