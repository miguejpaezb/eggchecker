from datetime import datetime

from app.models.alimentacion_camada import AlimentacionCamada
from app.models.camada import Camada
from app.models.categoria_insumo import CategoriaInsumo
from app.models.insumo import Insumo
from app.models.usuario import Usuario
from app.schemas.alimentacion import AlimentacionCreate
from app.schemas.inventario import MovimientoCreate
from app.services import insumo_service, notificacion_service
from fastapi import HTTPException
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

# Categoría del catálogo fijo que se puede registrar como alimento.
CATEGORIA_ALIMENTO = "alimento"


def registrar_alimentacion(
    db: Session,
    usuario: Usuario,
    id_camada: int,
    datos: AlimentacionCreate,
) -> dict:
    """Registra el alimento de una camada y descuenta el inventario.

    Crea el movimiento de salida del insumo y la fila de alimentación en
    una sola transacción. La fecha y hora las pone el servidor.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la camada y del insumo.
        id_camada: Camada alimentada.
        datos: Insumo, cantidad y observaciones opcionales.

    Returns:
        dict: Registro creado con los datos del insumo y la camada.

    Raises:
        HTTPException: 404 si la camada o el insumo no son del usuario;
            400 si la camada no está activa, si el insumo no es alimento,
            si está inactivo o si la cantidad supera el stock.
    """
    camada = _obtener_camada(db, usuario, id_camada, exigir_activa=True)
    insumo = _validar_insumo_alimento(db, usuario, datos.id_insumo)
    if datos.cantidad > insumo.stock_actual:
        raise HTTPException(
            status_code=400,
            detail="La cantidad supera el stock disponible del alimento",
        )

    insumo_service.preparar_movimiento(
        db,
        insumo,
        MovimientoCreate(
            tipo_movimiento="salida",
            cantidad=datos.cantidad,
            observaciones=(f"Alimentación camada — {camada.nombre_camada}"),
        ),
    )
    registro = AlimentacionCamada(
        id_camada=camada.id_camada,
        id_insumo=insumo.id_insumo,
        cantidad=datos.cantidad,
        fecha_hora=datetime.now(),
        observaciones=datos.observaciones,
    )
    db.add(registro)
    try:
        db.commit()
    except IntegrityError as exc:
        # El check de cantidad y el stock no deben quedar a medias.
        db.rollback()
        raise HTTPException(
            status_code=400,
            detail="No se pudo registrar la alimentación",
        ) from exc
    db.refresh(registro)
    notificacion_service.sincronizar_aviso_stock(db, insumo)
    return _serializar(registro, camada, insumo)


def listar_alimentacion(db: Session, usuario: Usuario, id_camada: int) -> list[dict]:
    """Lista el historial de alimentación de una camada.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la camada.
        id_camada: Camada consultada.

    Returns:
        list[dict]: Registros del más reciente al más antiguo.

    Raises:
        HTTPException: 404 si la camada no es del usuario.
    """
    camada = _obtener_camada(db, usuario, id_camada, exigir_activa=False)
    filas = (
        db.query(AlimentacionCamada, Insumo)
        .join(Insumo, Insumo.id_insumo == AlimentacionCamada.id_insumo)
        .filter(AlimentacionCamada.id_camada == camada.id_camada)
        .order_by(
            AlimentacionCamada.fecha_hora.desc(),
            AlimentacionCamada.id_alimentacion.desc(),
        )
        .all()
    )
    return [_serializar(registro, camada, insumo) for registro, insumo in filas]


def _obtener_camada(
    db: Session,
    usuario: Usuario,
    id_camada: int,
    exigir_activa: bool,
) -> Camada:
    """Busca la camada del usuario y, si aplica, exige que esté activa."""
    camada = db.get(Camada, id_camada)
    if camada is None or camada.id_usuario != usuario.id_usuario:
        raise HTTPException(status_code=404, detail="Camada no encontrada")
    if exigir_activa and camada.estado != "activa":
        raise HTTPException(
            status_code=400,
            detail="Solo se alimentan camadas activas",
        )
    return camada


def _validar_insumo_alimento(db: Session, usuario: Usuario, id_insumo: int) -> Insumo:
    """Valida que el insumo sea un alimento activo del usuario."""
    insumo = db.get(Insumo, id_insumo)
    if insumo is None or insumo.id_usuario != usuario.id_usuario:
        raise HTTPException(status_code=404, detail="Insumo no encontrado")
    if insumo.descontinuado or not insumo.activo:
        raise HTTPException(
            status_code=400,
            detail="El insumo está inactivo o descontinuado",
        )
    categoria = db.get(CategoriaInsumo, insumo.id_categoria)
    if categoria is None or categoria.nombre_categ != CATEGORIA_ALIMENTO:
        raise HTTPException(
            status_code=400,
            detail="El insumo debe ser de categoría alimento",
        )
    return insumo


def _serializar(registro: AlimentacionCamada, camada: Camada, insumo: Insumo) -> dict:
    """Arma la respuesta del registro de alimentación."""
    return {
        "id_alimentacion": registro.id_alimentacion,
        "id_camada": registro.id_camada,
        "nombre_camada": camada.nombre_camada,
        "id_insumo": registro.id_insumo,
        "nombre_insumo": insumo.nombre_insumo,
        "unidad_medida": insumo.unidad_medida,
        "cantidad": registro.cantidad,
        "fecha_hora": registro.fecha_hora,
        "observaciones": registro.observaciones,
    }
