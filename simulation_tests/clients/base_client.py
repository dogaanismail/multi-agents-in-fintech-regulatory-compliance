"""
Base HTTP Client
================
Thin async wrapper around httpx.AsyncClient.
All domain-specific clients inherit from this.

Key design decisions:
  - Uses httpx (not requests) for native asyncio compatibility
  - Timeout is configurable; default from SIM_CONFIG
  - Raises ClientError on non-2xx status so callers handle it uniformly
  - Logs at DEBUG level so the runner can optionally silence noise
"""

import logging
from typing import Any, Dict, Optional

import httpx

from simulation_tests.config import SIM_CONFIG

logger = logging.getLogger(__name__)


class ClientError(Exception):
    """Raised when the API returns a non-2xx status."""

    def __init__(self, url: str, status: int, body: str):
        super().__init__(f"HTTP {status} from {url}: {body[:200]}")
        self.url = url
        self.status = status
        self.body = body


class BaseClient:
    """
    Async HTTP base client.

    Usage (in an async context):
    ----------------------------
    async with CustomerClient() as client:
        response = await client.post("/api/v1/customers", payload)
    """

    def __init__(
        self,
        base_url: str,
        timeout: int = SIM_CONFIG.request_timeout_seconds,
    ):
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout
        self._http: Optional[httpx.AsyncClient] = None

    async def __aenter__(self):
        self._http = httpx.AsyncClient(
            base_url=self._base_url,
            timeout=self._timeout,
            headers={"Content-Type": "application/json"},
        )
        return self

    async def __aexit__(self, *args):
        if self._http:
            await self._http.aclose()

    async def get(self, path: str, params: Optional[dict] = None) -> dict:
        assert self._http, "Client not started — use `async with` context."
        url = path
        logger.debug("GET %s%s", self._base_url, url)
        resp = await self._http.get(url, params=params)
        self._raise_for_status(resp)
        return resp.json()

    async def post(self, path: str, body: dict) -> dict:
        assert self._http, "Client not started — use `async with` context."
        logger.debug("POST %s%s  body=%s", self._base_url, path, str(body)[:120])
        resp = await self._http.post(path, json=body)
        self._raise_for_status(resp)
        return resp.json()

    async def health(self) -> dict:
        """Check /health endpoint. Returns the JSON body."""
        try:
            return await self.get("/health")
        except Exception as exc:
            return {"status": "unreachable", "error": str(exc)}

    @staticmethod
    def _raise_for_status(resp: httpx.Response) -> None:
        if resp.status_code >= 400:
            raise ClientError(
                url=str(resp.url),
                status=resp.status_code,
                body=resp.text,
            )
