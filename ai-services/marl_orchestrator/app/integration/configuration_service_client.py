"""
Configuration Service Client

Fetches MARL configuration parameters from the Spring Boot configuration-service
REST API at runtime.  All methods are designed to never raise — on any failure
they log a warning and return None so the caller can fall back to env-var defaults.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

import os
from typing import Optional

import httpx

from app.core.logging import logger


class ConfigurationServiceClient:
    """
    Async HTTP client for the configuration-service REST API.

    The base URL is read from the CONFIGURATION_SERVICE_URL environment variable
    (default: http://localhost:5009) so the same image works in both local and
    Docker Compose environments.
    """

    def __init__(self) -> None:
        self._base_url: str = os.getenv(
            "CONFIGURATION_SERVICE_URL", "http://localhost:5009"
        ).rstrip("/")
        self._timeout: float = 5.0  # seconds — short to avoid blocking startup

    # ─────────────────────────────────────────────────────────────────────────
    # Public API
    # ─────────────────────────────────────────────────────────────────────────

    async def fetch_all_configurations(self) -> Optional[dict[str, str]]:
        """
        Fetch all configuration entries from GET /api/v1/configurations.

        Returns a dict of  configKey → configValue  on success, or None when
        the service is unreachable or returns an error status.
        """
        url = f"{self._base_url}/api/v1/configurations"
        try:
            async with httpx.AsyncClient(timeout=self._timeout) as client:
                response = await client.get(url)
                response.raise_for_status()

                entries: list[dict] = response.json()
                result: dict[str, str] = {
                    item["configKey"]: item["configValue"]
                    for item in entries
                    if "configKey" in item and "configValue" in item
                }
                logger.debug(
                    f"🔧 configuration-service: fetched {len(result)} config entries"
                )
                return result

        except httpx.HTTPStatusError as exc:
            logger.warning(
                f"⚠️  configuration-service HTTP {exc.response.status_code} "
                f"at {url} — using env-var fallbacks"
            )
            return None

        except httpx.RequestError as exc:
            logger.warning(
                f"⚠️  configuration-service unreachable "
                f"({type(exc).__name__}: {exc}) — using env-var fallbacks"
            )
            return None

        except Exception as exc:
            logger.warning(
                f"⚠️  configuration-service unexpected error: {exc} — using env-var fallbacks"
            )
            return None

    async def is_healthy(self) -> bool:
        """
        Lightweight reachability probe.
        Returns True when configuration-service responds with HTTP 2xx.
        """
        url = f"{self._base_url}/api/v1/configurations"
        try:
            async with httpx.AsyncClient(timeout=2.0) as client:
                response = await client.get(url)
                return response.is_success
        except Exception:
            return False


# ─────────────────────────────────────────────────────────────────────────────
# Singleton
# ─────────────────────────────────────────────────────────────────────────────
configuration_service_client = ConfigurationServiceClient()
