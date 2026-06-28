"""
Prometheus metrics for the Kafka consumers.

Exposed at /metrics and scraped by Prometheus, then visualised in Grafana
alongside the existing Tempo (traces) and Loki (logs) datasources. Covers
consumer throughput, handler retries, dead-letter routing, commit failures
and per-partition consumer lag.
"""

import json

from prometheus_client import Counter, Gauge

from app.core.logging import logger

messages_consumed = Counter(
    "marl_kafka_messages_consumed_total",
    "Messages polled from Kafka before processing.",
    ["topic"],
)

messages_processed = Counter(
    "marl_kafka_messages_processed_total",
    "Messages processed successfully and committed.",
    ["topic"],
)

processing_retries = Counter(
    "marl_kafka_processing_retries_total",
    "Handler invocations retried after a transient failure.",
    ["topic"],
)

dlt_messages = Counter(
    "marl_kafka_dlt_messages_total",
    "Messages routed to the dead-letter topic.",
    ["topic", "reason"],
)

commit_failures = Counter(
    "marl_kafka_commit_failures_total",
    "Offset commit failures.",
    ["topic"],
)

consumer_lag = Gauge(
    "marl_kafka_consumer_lag",
    "Consumer lag (uncommitted messages) per topic partition.",
    ["topic", "partition"],
)


def update_lag_from_stats(stats_json: str) -> None:
    """Parse the librdkafka statistics payload and publish per-partition lag."""
    try:
        stats = json.loads(stats_json)
    except (TypeError, ValueError) as error:
        logger.debug(f"Could not parse Kafka statistics payload: {error}")
        return

    for topic, topic_stats in stats.get("topics", {}).items():
        for partition, partition_stats in topic_stats.get("partitions", {}).items():
            if partition == "-1":
                continue
            lag = partition_stats.get("consumer_lag")
            if lag is not None and lag >= 0:
                consumer_lag.labels(topic=topic, partition=partition).set(lag)
