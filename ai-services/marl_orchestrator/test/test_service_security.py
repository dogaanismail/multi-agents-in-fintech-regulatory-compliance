import time
from types import SimpleNamespace

import httpx
import jwt
import pytest
from cryptography.hazmat.primitives.asymmetric import rsa
from fastapi import Depends, FastAPI
from fastapi.testclient import TestClient

from app.core import security
from app.core.security import AuthenticatedCaller, TrustedIssuerTokenValidator, require_permission
from app.core.service_token import ServiceTokenAuth, ServiceTokenProvider

STAFF_ISSUER = "http://keycloak:8180/realms/bank-staff"
CUSTOMER_ISSUER = "http://keycloak:8180/realms/bank-customers"
SIGNING_KEY = rsa.generate_private_key(public_exponent=65537, key_size=2048)

pytestmark = pytest.mark.unit


def create_token(issuer=STAFF_ISSUER, audience=("bank-platform",), permissions=("marl.read",), expires_in=300):
    now = int(time.time())
    claims = {
        "iss": issuer,
        "aud": list(audience),
        "sub": "officer-id",
        "preferred_username": "officer",
        "permissions": list(permissions),
        "iat": now,
        "exp": now + expires_in,
    }
    return jwt.encode(claims, SIGNING_KEY, algorithm="RS256")


def create_token_validator(monkeypatch):
    token_validator = TrustedIssuerTokenValidator([STAFF_ISSUER], ["marl-orchestrator", "bank-platform"])
    signing_key_client = SimpleNamespace(
        get_signing_key_from_jwt=lambda _: SimpleNamespace(key=SIGNING_KEY.public_key()))
    monkeypatch.setattr(token_validator, "_signing_key_client", lambda _: signing_key_client)
    return token_validator


async def test_should_describe_the_caller_of_a_valid_token(monkeypatch):
    caller = await create_token_validator(monkeypatch).validate(create_token(permissions=("marl.read", "marl.train")))

    assert caller == AuthenticatedCaller(username="officer", permissions=frozenset({"marl.read", "marl.train"}))


async def test_should_reject_a_token_addressed_to_another_service(monkeypatch):
    with pytest.raises(jwt.InvalidAudienceError):
        await create_token_validator(monkeypatch).validate(create_token(audience=("ledger-service",)))


async def test_should_reject_a_token_from_an_untrusted_issuer(monkeypatch):
    with pytest.raises(jwt.InvalidIssuerError):
        await create_token_validator(monkeypatch).validate(create_token(issuer=CUSTOMER_ISSUER))


async def test_should_reject_an_expired_token(monkeypatch):
    with pytest.raises(jwt.ExpiredSignatureError):
        await create_token_validator(monkeypatch).validate(create_token(expires_in=-60))


def create_protected_client(monkeypatch):
    monkeypatch.setattr(security, "token_validator", create_token_validator(monkeypatch))
    protected_app = FastAPI()

    @protected_app.post("/trigger", dependencies=[Depends(require_permission("marl.train"))])
    async def trigger():
        return {"triggered": True}

    return TestClient(protected_app)


def test_should_answer_a_request_without_a_token_with_401(monkeypatch):
    assert create_protected_client(monkeypatch).post("/trigger").status_code == 401


def test_should_answer_a_caller_without_the_permission_with_403(monkeypatch):
    response = create_protected_client(monkeypatch).post(
        "/trigger", headers={"Authorization": f"Bearer {create_token(permissions=('marl.read',))}"})

    assert response.status_code == 403


def test_should_serve_a_caller_holding_the_permission(monkeypatch):
    response = create_protected_client(monkeypatch).post(
        "/trigger", headers={"Authorization": f"Bearer {create_token(permissions=('marl.train',))}"})

    assert response.status_code == 200


async def test_should_reuse_one_service_token_and_fetch_a_new_one_after_a_401():
    issued_tokens = []

    def keycloak(request: httpx.Request) -> httpx.Response:
        assert b"grant_type=client_credentials" in request.content
        issued_tokens.append(f"service-token-{len(issued_tokens) + 1}")
        return httpx.Response(200, json={"access_token": issued_tokens[-1], "expires_in": 300})

    seen_tokens = []

    def agent(request: httpx.Request) -> httpx.Response:
        seen_tokens.append(request.headers["Authorization"])
        return httpx.Response(401 if len(seen_tokens) == 3 else 200)

    service_token_provider = ServiceTokenProvider(
        "http://keycloak/token", "marl-orchestrator", "secret", http_transport=httpx.MockTransport(keycloak))
    async with httpx.AsyncClient(
            auth=ServiceTokenAuth(service_token_provider), transport=httpx.MockTransport(agent)) as agent_client:
        await agent_client.get("http://agent/predict")
        await agent_client.get("http://agent/predict")
        final_response = await agent_client.get("http://agent/predict")

    assert final_response.status_code == 200
    assert seen_tokens == ["Bearer service-token-1", "Bearer service-token-1", "Bearer service-token-1",
                           "Bearer service-token-2"]
