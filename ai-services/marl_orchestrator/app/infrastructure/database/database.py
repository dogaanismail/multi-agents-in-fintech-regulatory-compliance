"""
Async SQLAlchemy database engine and session factory.

Author: Ismail Dogan
Master's Thesis: Multi-Agent System for Fintech Regulatory Compliance
"""

from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker
from sqlalchemy.orm import DeclarativeBase

from app.core.config import settings
from app.core.logging import logger


# ─────────────────────────────────────────────
# Declarative base for all ORM models
# ─────────────────────────────────────────────
class Base(DeclarativeBase):
    pass


# ─────────────────────────────────────────────
# Async engine (asyncpg driver)
# ─────────────────────────────────────────────
engine = create_async_engine(
    settings.database_url,
    echo=False,          # Set to True for SQL query logging in debug mode
    pool_size=5,
    max_overflow=10,
    pool_pre_ping=True,  # Verify connection health before use
)

# ─────────────────────────────────────────────
# Session factory
# ─────────────────────────────────────────────
AsyncSessionLocal = async_sessionmaker(
    bind=engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autocommit=False,
    autoflush=False,
)


async def get_db_session() -> AsyncSession:
    """
    FastAPI dependency — provides an async DB session.

    Usage:
        @router.get("/example")
        async def example(db: AsyncSession = Depends(get_db_session)):
            ...
    """
    async with AsyncSessionLocal() as session:
        yield session


async def init_db() -> None:
    """
    Create all tables defined by the ORM models if they do not already exist.

    Called once at application startup.  Safe to call multiple times
    (CREATE TABLE IF NOT EXISTS semantics from SQLAlchemy).
    """
    from . import models  # noqa: F401 – import to register models with Base.metadata

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    logger.info("✅ MARL Orchestrator database tables initialised")
