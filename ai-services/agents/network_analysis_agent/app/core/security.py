import asyncio
from dataclasses import dataclass
from typing import Dict, Iterable

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jwt import PyJWKClient

from app.core.config import settings

USERNAME_CLAIM = "preferred_username"
KEYCLOAK_CERTS_PATH = "/protocol/openid-connect/certs"
SIGNING_ALGORITHMS = ["RS256"]


@dataclass(frozen=True)
class ServiceCaller:
    username: str


class TrustedIssuerTokenValidator:

    def __init__(self, trusted_issuers: Iterable[str], accepted_audiences: Iterable[str]):
        self._trusted_issuers = frozenset(trusted_issuers)
        self._accepted_audiences = list(accepted_audiences)
        self._signing_key_clients: Dict[str, PyJWKClient] = {}

    async def validate(self, token: str) -> ServiceCaller:
        issuer = jwt.decode(token, options={"verify_signature": False}).get("iss")
        if issuer not in self._trusted_issuers:
            raise jwt.InvalidIssuerError(f"Untrusted issuer: {issuer}")

        signing_key = await asyncio.to_thread(self._signing_key_client(issuer).get_signing_key_from_jwt, token)
        claims = jwt.decode(
            token,
            signing_key.key,
            algorithms=SIGNING_ALGORITHMS,
            issuer=issuer,
            audience=self._accepted_audiences,
            options={"require": ["exp", "iat", "iss", "aud"]},
        )
        return ServiceCaller(username=claims.get(USERNAME_CLAIM) or claims["sub"])

    def _signing_key_client(self, issuer: str) -> PyJWKClient:
        if issuer not in self._signing_key_clients:
            self._signing_key_clients[issuer] = PyJWKClient(issuer + KEYCLOAK_CERTS_PATH, cache_keys=True)
        return self._signing_key_clients[issuer]


token_validator = TrustedIssuerTokenValidator(
    trusted_issuers=settings.trusted_issuers.split(","),
    accepted_audiences=settings.accepted_audiences.split(","),
)
bearer_scheme = HTTPBearer(auto_error=False)


async def require_service_caller(
        credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
) -> ServiceCaller:
    if credentials is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Bearer token required", {"WWW-Authenticate": "Bearer"})
    try:
        return await token_validator.validate(credentials.credentials)
    except jwt.PyJWTError:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Invalid bearer token", {"WWW-Authenticate": "Bearer"})
