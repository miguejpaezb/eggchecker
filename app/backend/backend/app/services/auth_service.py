from datetime import UTC, date, datetime, timedelta

from app.core.config import get_settings
from app.core.plans import ia_incluida, limite_aves, limite_clientes
from app.core.security import (
    crear_token_acceso,
    generar_token_recuperacion,
    hash_token,
    obtener_hash,
    verificar_hash,
)
from app.models.token_recuperacion import TokenRecuperacion
from app.models.usuario import Usuario
from app.schemas.auth import LoginRequest, RecuperarRequest, RegistroRequest
from fastapi import HTTPException
from sqlalchemy import text
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

settings = get_settings()


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


def solicitar_recuperacion(db: Session, datos: RecuperarRequest) -> None:
    """Registra una solicitud de recuperación y envía el enlace por correo.

    Valida que el correo exista, invalida tokens previos, genera un token
    opaco y guarda solo su hash. El token crudo nunca se devuelve al cliente.

    Args:
        db: Sesión de base de datos.
        datos: Correo del usuario que solicita la recuperación.

    Raises:
        HTTPException: 404 si el correo no está registrado; 500 si el
            envío del correo falla.
    """
    usuario = _buscar_por_correo(db, datos.correo_electronico)
    if usuario is None:
        raise HTTPException(
            status_code=404,
            detail=(
                "No encontramos una cuenta con ese correo. " "¿Deseas registrarte?"
            ),
        )

    _invalidar_tokens_previos(db, usuario.id_usuario)

    token = generar_token_recuperacion()
    ahora = datetime.now(UTC).replace(tzinfo=None)
    expira = ahora + timedelta(minutes=settings.RECOVERY_TOKEN_EXPIRE_MINUTES)
    db.add(
        TokenRecuperacion(
            id_usuario=usuario.id_usuario,
            token_hash=hash_token(token),
            fecha_creacion=ahora,
            fecha_expiracion=expira,
        )
    )

    try:
        _enviar_correo_recuperacion(usuario, token)
    except Exception as exc:
        db.rollback()
        raise HTTPException(
            status_code=500,
            detail=(
                "No se pudo enviar el correo de recuperación. "
                "Intenta de nuevo más tarde."
            ),
        ) from exc
    db.commit()


def _invalidar_tokens_previos(db: Session, id_usuario: int) -> None:
    """Marca como usados los tokens de recuperación vigentes del usuario."""
    db.query(TokenRecuperacion).filter(
        TokenRecuperacion.id_usuario == id_usuario,
        TokenRecuperacion.usado.is_(False),
    ).update({TokenRecuperacion.usado: True})


def _enviar_correo_recuperacion(usuario: Usuario, token: str) -> None:
    """Envía el enlace de recuperación al correo del usuario (mock en v1).

    En v1 no se envía correo real: el enlace se armará con `FRONTEND_URL`
    y el token, y se enviará por SMTP cuando se configure el servidor.

    Args:
        usuario: Usuario que solicitó la recuperación.
        token: Token crudo que debe viajar en el enlace.
    """
    # TODO(Miguel): construir el enlace con FRONTEND_URL y enviarlo por
    # SMTP real. Por ahora es un no-op para no filtrar el token.
    return None


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
        "aves_actuales": _sumar_aves_activas(db, usuario.id_usuario),
        "clientes_actuales": _contar_tabla(db, "cliente", usuario.id_usuario),
    }


def _buscar_por_correo(db: Session, correo: str) -> Usuario | None:
    """Busca un usuario por su correo electrónico."""
    return db.query(Usuario).filter(Usuario.correo_electronico == correo).first()


def _contar_tabla(db: Session, tabla: str, id_usuario: int) -> int:
    """Cuenta los registros de una tabla propiedad del usuario.

    Se usa SQL crudo porque el modelo ORM de cliente aún no existe; el
    conteo de aves activas se resuelve en _sumar_aves_activas.
    """
    consulta = text(f"SELECT COUNT(*) FROM {tabla} WHERE id_usuario = :uid")
    resultado = db.execute(consulta, {"uid": id_usuario}).scalar()
    return int(resultado or 0)


def _sumar_aves_activas(db: Session, id_usuario: int) -> int:
    """Suma las aves de las camadas activas del usuario.

    Regla RF-06: aves_actuales = SUM(cantidad_actual) de las camadas con
    estado 'activa'. Se usa SQL crudo porque el modelo ORM de camada aún
    no existe.
    """
    consulta = text(
        "SELECT COALESCE(SUM(cantidad_actual), 0) FROM camada "
        "WHERE id_usuario = :uid AND estado = 'activa'"
    )
    resultado = db.execute(consulta, {"uid": id_usuario}).scalar()
    return int(resultado or 0)
