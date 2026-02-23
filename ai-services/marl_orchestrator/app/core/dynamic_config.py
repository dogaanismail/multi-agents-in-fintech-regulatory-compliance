"""
Dynamic Configuration Cache

In-memory cache of MARL configuration parameters sourced from the
configuration-service REST API.  Values are refreshed every
REFRESH_INTERVAL_SECONDS (default 5 min) and fall back transparently to
pydantic-settings env-var values when the service is unreachable.

Usage
─────
    from app.core.dynamic_config import dynamic_config

    interval = dynamic_config.get_int(
        "TRAINING_INTERVAL_SECONDS",
        fallback=settings.training_interval_seconds,
    )

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

from datetime import datetime, timezone
from typing import Optional

from app.core.config import settings
from app.core.logging import logger


class DynamicConfig:
    """
    Singleton in-memory store for configuration-service values.

    Design decisions
    ────────────────
    - All typed accessors accept a mandatory `fallback` argument: callers
      pass ``settings.<field>`` as the fallback so env-var behaviour is
      preserved when the service is unavailable or a key is missing.
    - Cache keys are normalised to UPPER_CASE to match the configKey convention
      used in the configuration-service database.
    - asyncio is single-threaded so no locking is required.
    - The refresh interval is itself a dynamic config value
      (CONFIG_REFRESH_INTERVAL_SECONDS).  On first boot it falls back to
      ``settings.config_refresh_interval_seconds`` (env-var / pydantic default).
      After the first successful refresh the cached value drives the TTL so
      compliance officers can change it at runtime without a restart.
    """

    def __init__(self) -> None:
        self._cache: dict[str, str] = {}
        self._last_refreshed: Optional[datetime] = None
        self._is_service_healthy: bool = False

    @property
    def _refresh_interval(self) -> int:
        """
        The effective TTL in seconds.

        Reads CONFIG_REFRESH_INTERVAL_SECONDS from the live cache so a
        compliance officer can change the polling frequency at runtime.
        Falls back to the pydantic-settings env-var default when the cache
        is empty (first boot or degraded state).
        """
        return self.get_int(
            "CONFIG_REFRESH_INTERVAL_SECONDS",
            settings.config_refresh_interval_seconds,
        )

    # ─────────────────────────────────────────────────────────────────────────
    # Cache management
    # ─────────────────────────────────────────────────────────────────────────

    async def refresh(self) -> bool:
        """
        Fetch the latest config values from configuration-service and update cache.

        Returns True when the refresh succeeded, False when the service was
        unreachable.  The cache retains its previous values on failure so the
        service continues to operate with the last-known-good configuration.
        """
        # Lazy import avoids a circular dependency at module import time
        from app.integration.configuration_service_client import configuration_service_client

        data = await configuration_service_client.fetch_all_configurations()

        if data is not None:
            self._cache = data
            self._last_refreshed = datetime.now(timezone.utc)
            self._is_service_healthy = True
            logger.info(
                f"✅ Dynamic config cache refreshed — "
                f"{len(data)} entries loaded from configuration-service"
            )
            return True

        # Refresh failed — keep stale cache and log appropriate warning
        self._is_service_healthy = False
        if self._cache:
            logger.warning(
                "⚠️  configuration-service refresh failed — "
                "continuing with stale cache values"
            )
        else:
            logger.warning(
                "⚠️  configuration-service refresh failed — "
                "falling back to env-var defaults for all parameters"
            )
        return False

    async def refresh_if_stale(self) -> None:
        """Refresh the cache only when the TTL has expired (or on first call)."""
        if self._last_refreshed is None:
            await self.refresh()
            return

        age = (datetime.now(timezone.utc) - self._last_refreshed).total_seconds()
        if age >= self._refresh_interval:
            logger.debug("🔄 Dynamic config TTL expired — refreshing cache…")
            await self.refresh()

    # ─────────────────────────────────────────────────────────────────────────
    # Internal helpers
    # ─────────────────────────────────────────────────────────────────────────

    def _raw(self, key: str) -> Optional[str]:
        """Return the raw string value from cache, or None when key is absent."""
        return self._cache.get(key.upper())

    # ─────────────────────────────────────────────────────────────────────────
    # Typed accessors
    # ─────────────────────────────────────────────────────────────────────────

    def get_float(self, key: str, fallback: float) -> float:
        """Return a float config value, using `fallback` when key is missing or unparseable."""
        raw = self._raw(key)
        if raw is None:
            return fallback
        try:
            return float(raw)
        except (ValueError, TypeError):
            logger.warning(
                f"⚠️  Cannot parse config '{key}' as float "
                f"(value={raw!r}) — using fallback {fallback}"
            )
            return fallback

    def get_int(self, key: str, fallback: int) -> int:
        """Return an integer config value, using `fallback` when key is missing or unparseable."""
        raw = self._raw(key)
        if raw is None:
            return fallback
        try:
            # Accept "300.0" style values that PostgreSQL may store
            return int(float(raw))
        except (ValueError, TypeError):
            logger.warning(
                f"⚠️  Cannot parse config '{key}' as int "
                f"(value={raw!r}) — using fallback {fallback}"
            )
            return fallback

    def get_bool(self, key: str, fallback: bool) -> bool:
        """Return a boolean config value, using `fallback` when key is missing."""
        raw = self._raw(key)
        if raw is None:
            return fallback
        return raw.strip().lower() in ("true", "1", "yes")

    def get_str(self, key: str, fallback: str) -> str:
        """Return a string config value, using `fallback` when key is missing."""
        raw = self._raw(key)
        return raw if raw is not None else fallback

    # ─────────────────────────────────────────────────────────────────────────
    # Status properties
    # ─────────────────────────────────────────────────────────────────────────

    @property
    def is_service_healthy(self) -> bool:
        """True when the last refresh attempt succeeded."""
        return self._is_service_healthy

    @property
    def last_refreshed(self) -> Optional[datetime]:
        """UTC timestamp of the last successful cache refresh, or None."""
        return self._last_refreshed

    @property
    def cache_size(self) -> int:
        """Number of config entries currently in cache."""
        return len(self._cache)


# ─────────────────────────────────────────────────────────────────────────────
# Singleton
# ─────────────────────────────────────────────────────────────────────────────
dynamic_config = DynamicConfig()
