from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker

from app.core.config import get_settings

settings = get_settings()

# SQLite exige desactivar la verificación de hilos cuando la API atiende
# requests concurrentes; MySQL no acepta este parámetro de conexión.
_connect_args = (
    {"check_same_thread": False} if settings.DATABASE_URL.startswith("sqlite") else {}
)

engine = create_engine(settings.DATABASE_URL, connect_args=_connect_args)

SessionLocal = sessionmaker(bind=engine, autocommit=False, autoflush=False)


def get_db() -> Session:
    """Provee una sesión de base de datos por request.

    Yields:
        Session: Sesión lista para usar en la operación actual.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
