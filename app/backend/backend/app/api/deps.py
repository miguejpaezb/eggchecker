from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.security import verificar_token
from app.models.usuario import Usuario

security = HTTPBearer(auto_error=False)


def get_current_usuario(
    credenciales: HTTPAuthorizationCredentials | None = Depends(security),
    db: Session = Depends(get_db),
) -> Usuario:
    """Resuelve el usuario autenticado a partir del token JWT.

    Args:
        credenciales: Credenciales Bearer extraídas del header Authorization.
        db: Sesión de base de datos.

    Returns:
        Usuario: El usuario dueño del token.

    Raises:
        HTTPException: 401 si falta el token, es inválido o el usuario
            no existe o está inactivo.
    """
    if credenciales is None:
        raise HTTPException(status_code=401, detail="No autenticado")

    payload = verificar_token(credenciales.credentials)
    usuario = db.get(Usuario, int(payload["sub"]))
    if usuario is None or not usuario.activo:
        raise HTTPException(status_code=401, detail="No autenticado")
    return usuario
