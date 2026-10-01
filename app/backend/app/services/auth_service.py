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
from app.models.camada import Camada
from app.models.produccion_diaria import ProduccionDiaria
from app.models.token_recuperacion import TokenRecuperacion
from app.models.usuario import Usuario
from app.schemas.auth import (
    CambiarContrasenaRequest,
    LoginRequest,
    NotificacionesUpdateRequest,
    PerfilUpdateRequest,
    RecuperarRequest,
    RegistroRequest,
)
from fastapi import HTTPException
from sqlalchemy import func, text
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

    Incluye el histórico solicitado en la sección de Perfil: total de
    huevos producidos, aves gestionadas (suma inicial de las camadas) y
    las preferencias de notificaciones.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario autenticado.

    Returns:
        dict: Perfil con uso actual e histórico del usuario.
    """
    return {
        "id_usuario": usuario.id_usuario,
        "nombre_completo": usuario.nombre_completo,
        "correo_electronico": usuario.correo_electronico,
        "telefono": usuario.telefono,
        "nombre_granja": usuario.nombre_granja,
        "plan_suscripcion": usuario.plan_suscripcion,
        "fecha_registro": usuario.fecha_registro,
        "activo": usuario.activo,
        "plan": usuario.plan_suscripcion,
        "aves_max": limite_aves(usuario.plan_suscripcion),
        "clientes_max": limite_clientes(usuario.plan_suscripcion),
        "ia_incluida": ia_incluida(usuario.plan_suscripcion),
        "aves_actuales": _sumar_aves_activas(db, usuario.id_usuario),
        "clientes_actuales": _contar_tabla(db, "cliente", usuario.id_usuario),
        "total_huevos_producidos": _sumar_huevos_producidos(db, usuario.id_usuario),
        "total_aves_gestionadas": _sumar_aves_gestionadas(db, usuario.id_usuario),
        "notif_produccion_baja": usuario.notif_produccion_baja,
        "notif_stock_bajo": usuario.notif_stock_bajo,
        "notif_vacunacion": usuario.notif_vacunacion,
        "notif_resumen_semanal": usuario.notif_resumen_semanal,
    }


def actualizar_perfil(
    db: Session, usuario: Usuario, datos: PerfilUpdateRequest
) -> dict:
    """Actualiza los datos personales y de la granja del avicultor.

    Si el correo cambia, exige y valida la contraseña actual, y verifica
    que el nuevo correo no esté en uso. El cliente cierra la sesión tras
    este cambio para forzar un nuevo inicio de sesión.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario autenticado.
        datos: Datos validados del formulario de perfil.

    Returns:
        dict: Perfil actualizado.

    Raises:
        HTTPException: 401 si el correo cambia sin contraseña válida; 409
            si el nuevo correo ya está registrado.
    """
    if datos.correo_electronico != usuario.correo_electronico:
        if not datos.contrasena_actual or not verificar_hash(
            datos.contrasena_actual, usuario.contrasena_hash
        ):
            raise HTTPException(
                status_code=401,
                detail="La contraseña actual es incorrecta",
            )
        if _buscar_por_correo(db, datos.correo_electronico) is not None:
            raise HTTPException(status_code=409, detail="El correo ya está registrado")

    usuario.nombre_completo = datos.nombre_completo
    usuario.correo_electronico = datos.correo_electronico
    usuario.telefono = datos.telefono
    usuario.nombre_granja = datos.nombre_granja
    try:
        db.commit()
    except IntegrityError as exc:
        # Carrera por correo duplicado entre dos actualizaciones.
        db.rollback()
        raise HTTPException(
            status_code=409, detail="El correo ya está registrado"
        ) from exc
    db.refresh(usuario)
    return obtener_perfil(db, usuario)


def actualizar_notificaciones(
    db: Session, usuario: Usuario, datos: NotificacionesUpdateRequest
) -> dict:
    """Guarda las preferencias de alertas del avicultor.

    El envío real de correo/teléfono aún no está implementado; por ahora
    solo se persiste qué alertas quiere recibir.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario autenticado.
        datos: Preferencias de alertas validadas.

    Returns:
        dict: Perfil actualizado con las nuevas preferencias.
    """
    usuario.notif_produccion_baja = datos.notif_produccion_baja
    usuario.notif_stock_bajo = datos.notif_stock_bajo
    usuario.notif_vacunacion = datos.notif_vacunacion
    usuario.notif_resumen_semanal = datos.notif_resumen_semanal
    db.commit()
    db.refresh(usuario)
    return obtener_perfil(db, usuario)


def cambiar_contrasena(
    db: Session, usuario: Usuario, datos: CambiarContrasenaRequest
) -> None:
    """Cambia la contraseña del usuario validando la actual.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario autenticado.
        datos: Contraseña actual y nueva.

    Raises:
        HTTPException: 401 si la contraseña actual no coincide.
    """
    if not verificar_hash(datos.contrasena_actual, usuario.contrasena_hash):
        raise HTTPException(
            status_code=401, detail="La contraseña actual es incorrecta"
        )
    usuario.contrasena_hash = obtener_hash(datos.contrasena_nueva)
    db.commit()


def _buscar_por_correo(db: Session, correo: str) -> Usuario | None:
    """Busca un usuario por su correo electrónico."""
    return db.query(Usuario).filter(Usuario.correo_electronico == correo).first()


def _sumar_huevos_producidos(db: Session, id_usuario: int) -> int:
    """Suma todos los huevos recolectados por el usuario (histórico)."""
    total = (
        db.query(func.coalesce(func.sum(ProduccionDiaria.total_huevos), 0))
        .filter(ProduccionDiaria.id_usuario == id_usuario)
        .scalar()
    )
    return int(total or 0)


def _sumar_aves_gestionadas(db: Session, id_usuario: int) -> int:
    """Suma las aves iniciales de todas las camadas del usuario (histórico)."""
    total = (
        db.query(func.coalesce(func.sum(Camada.cantidad_inicial), 0))
        .filter(Camada.id_usuario == id_usuario)
        .scalar()
    )
    return int(total or 0)


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
