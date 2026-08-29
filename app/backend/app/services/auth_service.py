from datetime import date

from app.core.plans import ia_incluida, limite_aves, limite_clientes
from app.core.security import crear_token_acceso, obtener_hash, verificar_hash
from app.models.usuario import Usuario
from app.schemas.auth import LoginRequest, RecuperarRequest, RegistroRequest
from fastapi import HTTPException
from sqlalchemy import text
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session


def registrar_usuario(db: Session, datos: RegistroRequest) -> Usuario:
    """Registra un avicultor nuevo en la base de datos.

    Args:
        db: Sesión de base de datos.
        datos: Datos validados del registro.

    Returns:
        Usuario: El usuario recién creado, sin el hash de contraseña.

    Raises:
        HTTPException: 409 si el correo ya está registrado.
    """
    if _buscar_por_correo(db, datos.correo_electronico) is not None:
        raise HTTPException(status_code=409, detail="El correo ya está registrado")

    usuario = Usuario(
        nombre_completo=datos.nombre_completo,
        correo_electronico=datos.correo_electronico,
        contrasena_hash=obtener_hash(datos.contrasena),
        telefono=datos.telefono,
        plan_suscripcion=datos.plan_suscripcion,
        fecha_registro=date.today(),
    )
    db.add(usuario)
    try:
        db.commit()
    except IntegrityError as exc:
        # Carrera por correo duplicado entre dos registros simultáneos.
        db.rollback()
        raise HTTPException(
            status_code=409, detail="El correo ya está registrado"
        ) from exc
    db.refresh(usuario)
    return usuario


def autenticar_usuario(db: Session, datos: LoginRequest) -> str:
    """Valida las credenciales y devuelve un token JWT de acceso.

    Args:
        db: Sesión de base de datos.
        datos: Credenciales del inicio de sesión.

    Returns:
        str: Token JWT firmado para el usuario autenticado.

    Raises:
        HTTPException: 401 si las credenciales son inválidas.
    """
    usuario = _buscar_por_correo(db, datos.correo_electronico)
    if (
        usuario is None
        or not verificar_hash(datos.contrasena, usuario.contrasena_hash)
        or not usuario.activo
    ):
        raise HTTPException(status_code=401, detail="Credenciales inválidas")
    return crear_token_acceso(sub=str(usuario.id_usuario))


def solicitar_recuperacion(db: Session, datos: RecuperarRequest) -> str:
    """Genera un token mock de recuperación de contraseña.

    Args:
        db: Sesión de base de datos.
        datos: Correo del usuario que solicita la recuperación.

    Returns:
        str: Token de recuperación (sin envío real de correo en v1).
    """
    # No se revela si el correo existe: siempre se devuelve un token.
    usuario = _buscar_por_correo(db, datos.correo_electronico)
    sub = str(usuario.id_usuario) if usuario else "no-registrado"
    return crear_token_acceso(sub=sub)


def obtener_perfil(db: Session, usuario: Usuario) -> dict:
    """Construye el perfil del usuario con los límites de su plan.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario autenticado.

    Returns:
        dict: Perfil con uso actual de aves y clientes.
    """
    return {
        "id_usuario": usuario.id_usuario,
        "nombre_completo": usuario.nombre_completo,
        "correo_electronico": usuario.correo_electronico,
        "telefono": usuario.telefono,
        "plan_suscripcion": usuario.plan_suscripcion,
        "fecha_registro": usuario.fecha_registro,
        "activo": usuario.activo,
        "plan": usuario.plan_suscripcion,
        "aves_max": limite_aves(usuario.plan_suscripcion),
        "clientes_max": limite_clientes(usuario.plan_suscripcion),
        "ia_incluida": ia_incluida(usuario.plan_suscripcion),
        "aves_actuales": _contar_tabla(db, "camada", usuario.id_usuario),
        "clientes_actuales": _contar_tabla(db, "cliente", usuario.id_usuario),
    }


def _buscar_por_correo(db: Session, correo: str) -> Usuario | None:
    """Busca un usuario por su correo electrónico."""
    return db.query(Usuario).filter(Usuario.correo_electronico == correo).first()


def _contar_tabla(db: Session, tabla: str, id_usuario: int) -> int:
    """Cuenta los registros de una tabla propiedad del usuario.

    Se usa SQL crudo porque los modelos ORM de camada y cliente pertenecen
    a otros módulos y aún no existen.
    """
    consulta = text(f"SELECT COUNT(*) FROM {tabla} WHERE id_usuario = :uid")
    resultado = db.execute(consulta, {"uid": id_usuario}).scalar()
    return int(resultado or 0)
