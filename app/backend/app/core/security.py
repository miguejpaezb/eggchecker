from datetime import UTC, datetime, timedelta
from typing import Any

from fastapi import HTTPException
from jose import JWTError, jwt
from passlib.context import CryptContext

from app.core.config import get_settings

settings = get_settings()

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def obtener_hash(password: str) -> str:
    """Genera el hash de una contraseña con bcrypt.

    Args:
        password: Contraseña en texto plano.

    Returns:
        str: Hash seguro listo para persistir.
    """
    return pwd_context.hash(password)


def verificar_hash(password: str, hashed: str) -> bool:
    """Compara una contraseña contra su hash almacenado.

    Args:
        password: Contraseña en texto plano.
        hashed: Hash guardado en la base de datos.

    Returns:
        bool: True si la contraseña coincide con el hash.
    """
    return pwd_context.verify(password, hashed)


def crear_token_acceso(sub: str) -> str:
    """Crea un token JWT de acceso firmado con la llave del entorno.

    Args:
        sub: Identificador del sujeto del token (usuario).

    Returns:
        str: Token JWT con fecha de expiración.
    """
    expira = datetime.now(UTC) + timedelta(minutes=settings.JWT_EXPIRE_MINUTES)
    payload: dict[str, Any] = {"sub": sub, "exp": expira}
    return jwt.encode(
        payload, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM
    )


def verificar_token(token: str) -> dict[str, Any]:
    """Valida un token JWT y devuelve su contenido decodificado.

    Args:
        token: Token JWT a verificar.

    Returns:
        dict[str, Any]: Payload decodificado del token.

    Raises:
        HTTPException: Si el token es inválido o expiró.
    """
    try:
        return jwt.decode(
            token,
            settings.JWT_SECRET_KEY,
            algorithms=[settings.JWT_ALGORITHM],
        )
    except JWTError as exc:
        raise HTTPException(
            status_code=401, detail="Token inválido o expirado"
        ) from exc
