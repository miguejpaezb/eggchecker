from app.models.camada import EDAD_DECISION_SEMANAS, Camada
from app.models.notificacion import TIPO_CAMADA_LIMITE, Notificacion
from app.models.usuario import Usuario
from fastapi import HTTPException
from sqlalchemy.orm import Session


def listar_notificaciones(db: Session, usuario: Usuario) -> list[Notificacion]:
    """Lista las notificaciones del usuario, sin leídas primero.

    Antes de listar, genera las notificaciones de camadas que superaron
    la vida productiva y aún no tienen aviso.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de las notificaciones.

    Returns:
        list[Notificacion]: Notificaciones ordenadas (no leídas primero).
    """
    _generar_para_camadas_limite(db, usuario)
    return (
        db.query(Notificacion)
        .filter(
            Notificacion.id_usuario == usuario.id_usuario,
            Notificacion.eliminada == 0,
        )
        .order_by(Notificacion.leida.asc(), Notificacion.fecha_creacion.desc())
        .all()
    )


def marcar_leida(db: Session, usuario: Usuario, id_notificacion: int) -> Notificacion:
    """Marca una notificación del usuario como leída.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la notificación.
        id_notificacion: Identificador de la notificación.

    Returns:
        Notificacion: La notificación ya marcada como leída.

    Raises:
        HTTPException: 404 si la notificación no es del usuario.
    """
    notificacion = _obtener_notificacion(db, usuario, id_notificacion)
    notificacion.leida = 1
    db.commit()
    db.refresh(notificacion)
    return notificacion


def marcar_todas_leidas(db: Session, usuario: Usuario) -> int:
    """Marca como leídas todas las notificaciones del usuario.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de las notificaciones.

    Returns:
        int: Cantidad de notificaciones actualizadas.
    """
    _generar_para_camadas_limite(db, usuario)
    total = (
        db.query(Notificacion)
        .filter(
            Notificacion.id_usuario == usuario.id_usuario,
            Notificacion.eliminada == 0,
            Notificacion.leida == 0,
        )
        .update({Notificacion.leida: 1})
    )
    db.commit()
    return int(total)


def eliminar_notificacion(db: Session, usuario: Usuario, id_notificacion: int) -> None:
    """Elimina (oculta) una notificación del usuario.

    Se marca como eliminada en lugar de borrarla para que la generación
    perezosa no la vuelva a crear.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la notificación.
        id_notificacion: Identificador de la notificación a eliminar.

    Raises:
        HTTPException: 404 si la notificación no es del usuario.
    """
    notificacion = _obtener_notificacion(db, usuario, id_notificacion)
    notificacion.eliminada = 1
    db.commit()


def _generar_para_camadas_limite(db: Session, usuario: Usuario) -> None:
    """Crea el aviso de una camada que superó las 72 semanas.

    Genera una única notificación por camada y tipo; si ya existe, no la
    vuelve a crear. Se ejecuta de forma perezosa al listar.
    """
    camadas = (
        db.query(Camada)
        .filter(
            Camada.id_usuario == usuario.id_usuario,
            Camada.estado == "activa",
            Camada.edad_semanas >= EDAD_DECISION_SEMANAS,
        )
        .all()
    )
    if not camadas:
        return

    existentes = {
        fila[0]
        for fila in db.query(Notificacion.id_camada)
        .filter(
            Notificacion.id_usuario == usuario.id_usuario,
            Notificacion.tipo == TIPO_CAMADA_LIMITE,
        )
        .all()
    }

    creada = False
    for camada in camadas:
        if camada.id_camada in existentes:
            continue
        db.add(
            Notificacion(
                id_usuario=usuario.id_usuario,
                id_camada=camada.id_camada,
                tipo=TIPO_CAMADA_LIMITE,
                titulo="Camada al límite",
                mensaje=(f"Camada {camada.nombre_camada} ha pasado las 72 semanas"),
            )
        )
        creada = True

    if creada:
        db.commit()


def _obtener_notificacion(
    db: Session, usuario: Usuario, id_notificacion: int
) -> Notificacion:
    """Busca una notificación del usuario o lanza 404."""
    notificacion = (
        db.query(Notificacion)
        .filter(
            Notificacion.id_notificacion == id_notificacion,
            Notificacion.id_usuario == usuario.id_usuario,
            Notificacion.eliminada == 0,
        )
        .first()
    )
    if notificacion is None:
        raise HTTPException(status_code=404, detail="Notificación no encontrada")
    return notificacion
