import asyncio
from dataclasses import dataclass
from typing import Callable, Dict, Iterable

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jwt import PyJWKClient

from app.core.config import settings

MARL_READ = "marl.read"
MARL_TRAIN = "marl.train"
PERMISSIONS_CLAIM = "permissions"
USERNAME_CLAIM = "preferred_username"
KEYCLOAK_CERTS_PATH = "/protocol/openid-connect/certs"
SIGNING_ALGORITHMS = ["RS256"]


@dataclass(frozen=True)
class AuthenticatedCaller:
    username: str
    permissions: frozenset


class TrustedIssuerTokenValidator:

    def __init__(self, trusted_issuers: Iterable[str], accepted_audiences: Iterable[str]):
        self._trusted_issuers = frozenset(trusted_issuers)
        self._accepted_audiences = list(accepted_audiences)
        self._signing_key_clients: Dict[str, PyJWKClient] = {}

    async def validate(self, token: str) -> AuthenticatedCaller:
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
        return AuthenticatedCaller(
            username=claims.get(USERNAME_CLAIM) or claims["sub"],
            permissions=frozenset(claims.get(PERMISSIONS_CLAIM, [])),
        )

    def _signing_key_client(self, issuer: str) -> PyJWKClient:
        if issuer not in self._signing_key_clients:
            self._signing_key_clients[issuer] = PyJWKClient(issuer + KEYCLOAK_CERTS_PATH, cache_keys=True)
        return self._signing_key_clients[issuer]


token_validator = TrustedIssuerTokenValidator(
    trusted_issuers=settings.trusted_issuers.split(","),
    accepted_audiences=settings.accepted_audiences.split(","),
)
bearer_scheme = HTTPBearer(auto_error=False)


async def authenticated_caller(
        credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
) -> AuthenticatedCaller:
    if credentials is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Bearer token required", {"WWW-Authenticate": "Bearer"})
    try:
        return await token_validator.validate(credentials.credentials)
    except jwt.PyJWTError:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Invalid bearer token", {"WWW-Authenticate": "Bearer"})


def require_permission(permission: str) -> Callable:
    async def caller_with_permission(
            caller: AuthenticatedCaller = Depends(authenticated_caller)) -> AuthenticatedCaller:
        if permission not in caller.permissions:
            raise HTTPException(status.HTTP_403_FORBIDDEN, "Access denied")
        return caller

    return caller_with_permission
