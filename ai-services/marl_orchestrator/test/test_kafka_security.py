import os
import sys

import pytest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.core.config import settings
from app.infrastructure.kafka.kafka_config import kafka_security_config


@pytest.mark.unit
class TestKafkaSecurityConfig:

    def __init__(self):
        pass

    def test_plaintext_adds_no_security_settings(self, monkeypatch):
        monkeypatch.setattr(settings, "kafka_security_protocol", "PLAINTEXT")

        assert kafka_security_config() == {}

    def test_sasl_passes_the_scram_credentials_to_librdkafka(self, monkeypatch):
        monkeypatch.setattr(settings, "kafka_security_protocol", "SASL_PLAINTEXT")
        monkeypatch.setattr(settings, "kafka_sasl_mechanism", "SCRAM-SHA-512")
        monkeypatch.setattr(settings, "kafka_sasl_username", "marl-orchestrator")
        monkeypatch.setattr(settings, "kafka_sasl_password", "secret")

        assert kafka_security_config() == {
            "security.protocol": "SASL_PLAINTEXT",
            "sasl.mechanisms": "SCRAM-SHA-512",
            "sasl.username": "marl-orchestrator",
            "sasl.password": "secret",
        }
