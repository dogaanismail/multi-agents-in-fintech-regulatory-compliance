import asyncio
import time
from typing import AsyncGenerator

import httpx

from app.core.config import settings

TOKEN_EXPIRY_MARGIN_SECONDS = 30
SERVICE_TOKEN_PATH = "/realms/bank-internal/protocol/openid-connect/token"


class ServiceTokenProvider:

    def __init__(
            self,
            token_url: str,
            client_id: str,
            client_secret: str,
            http_transport: httpx.AsyncBaseTransport | None = None,
    ):
        self._token_url = token_url
        self._http_transport = http_transport
        self._client_id = client_id
        self._client_secret = client_secret
        self._access_token: str | None = None
        self._refresh_at = 0.0
        self._lock = asyncio.Lock()

    async def access_token(self) -> str:
        async with self._lock:
            if self._access_token is None or time.monotonic() >= self._refresh_at:
                await self._request_access_token()
            return self._access_token

    def invalidate(self) -> None:
        self._access_token = None

    async def _request_access_token(self) -> None:
        async with httpx.AsyncClient(timeout=5.0, transport=self._http_transport) as token_client:
            response = await token_client.post(self._token_url, data={
                "grant_type": "client_credentials",
                "client_id": self._client_id,
                "client_secret": self._client_secret,
            })
            response.raise_for_status()
            token_response = response.json()
        self._access_token = token_response["access_token"]
        self._refresh_at = time.monotonic() + token_response["expires_in"] - TOKEN_EXPIRY_MARGIN_SECONDS


class ServiceTokenAuth(httpx.Auth):

    def __init__(self, service_token_provider: ServiceTokenProvider):
        self._service_token_provider = service_token_provider

    async def async_auth_flow(self, request: httpx.Request) -> AsyncGenerator[httpx.Request, httpx.Response]:
        request.headers["Authorization"] = f"Bearer {await self._service_token_provider.access_token()}"
        response = yield request
        if response.status_code == httpx.codes.UNAUTHORIZED:
            self._service_token_provider.invalidate()
            request.headers["Authorization"] = f"Bearer {await self._service_token_provider.access_token()}"
            yield request


service_token_auth = ServiceTokenAuth(ServiceTokenProvider(
    token_url=settings.keycloak_url + SERVICE_TOKEN_PATH,
    client_id=settings.service_client_id,
    client_secret=settings.service_client_secret,
))
