"""
Dead-letter publisher.

Publishes the original (raw) message bytes to a `<topic><suffix>` dead-letter
topic so a poison or repeatedly-failing message can be inspected and replayed
without blocking the source partition. The original Avro wire-format bytes are
preserved untouched and enriched with metadata headers (origin topic/partition/
offset and the failure reason), mirroring Spring's DeadLetterPublishingRecoverer.
"""

from confluent_kafka import Producer

from app.core.config import settings
from app.core.logging import logger


class DeadLetterPublisher:

    def __init__(self):
        self._producer = Producer({
            "bootstrap.servers": settings.kafka_bootstrap_servers,
            "acks": "all",
            "retries": 3,
            "enable.idempotence": True,
        })

    def publish(self, msg, error, reason: str) -> bool:
        dlt_topic = f"{msg.topic()}{settings.kafka_dlq_topic_suffix}"

        headers = list(msg.headers() or [])
        headers.extend([
            ("dlt-origin-topic", msg.topic().encode("utf-8")),
            ("dlt-origin-partition", str(msg.partition()).encode("utf-8")),
            ("dlt-origin-offset", str(msg.offset()).encode("utf-8")),
            ("dlt-error-reason", reason.encode("utf-8")),
            ("dlt-error-message", str(error)[:1024].encode("utf-8")),
        ])

        try:
            self._producer.produce(
                topic=dlt_topic,
                value=msg.value(),
                key=msg.key(),
                headers=headers,
            )
            self._producer.flush(10)
            logger.warning(
                f"Routed message to {dlt_topic} (reason={reason}) from "
                f"{msg.topic()} [{msg.partition()}] offset {msg.offset()}"
            )
            return True
        except Exception as publish_error:
            logger.error(f"Failed to publish to dead-letter topic {dlt_topic}: {publish_error}")
            return False


dead_letter_publisher = DeadLetterPublisher()
