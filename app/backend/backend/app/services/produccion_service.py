from datetime import date, timedelta

from app.models.camada import EDAD_PRODUCCION_SEMANAS, Camada
from app.models.produccion_detalle import ProduccionDetalle
from app.models.produccion_diaria import ProduccionDiaria
from app.models.tipo_huevo import TipoHuevo
from app.models.usuario import Usuario
from app.schemas.produccion import DisponibleTipoResponse, ProduccionCreate
from fastapi import HTTPException
from sqlalchemy import func, text
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

# Regla de negocio RF-17: una cubeta equivale a 30 huevos.
HUEVOS_POR_CUBETA = 30


def registrar_produccion(
    db: Session, usuario: Usuario, datos: ProduccionCreate
) -> ProduccionDiaria:
    """Registra o actualiza la recolección diaria de una camada.

    Replica la lógica del `sp_registrar_produccion` del DDL original:
    valida que la camada esté en etapa de producción, convierte las
    cubetas a huevos, calcula el total en el servidor y guarda la
    cabecera con una fila de detalle por cada tipo con cantidad mayor que
    cero. Solo se admite el día actual y, si ya existe un registro de esa
    camada y fecha, se actualiza en lugar de crear otro.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la producción.
        datos: Cantidades por tipo, fecha y unidad de la recolección.

    Returns:
        ProduccionDiaria: La producción creada o actualizada con su detalle.

    Raises:
        HTTPException: 400 si la fecha no es hoy, si la camada no está en
            etapa de producción, si todas las cantidades están en cero o si
            el total supera las aves actuales de la camada; 404 si la camada
            no existe o no es del usuario.
    """
    if datos.fecha_recoleccion != date.today():
        raise HTTPException(
            status_code=400,
            detail="Solo se puede registrar producción del día actual",
        )

    camada = db.get(Camada, datos.id_camada)
    if camada is None or camada.id_usuario != usuario.id_usuario:
        raise HTTPException(status_code=404, detail="Camada no encontrada")
    if camada.estado != "activa" or camada.edad_semanas < EDAD_PRODUCCION_SEMANAS:
        raise HTTPException(
            status_code=400,
            detail="La camada no está en etapa de producción",
        )

    cantidades = _cantidades_por_tipo(datos)
    total = sum(cantidades.values())
    if total == 0:
        raise HTTPException(
            status_code=400,
            detail="Debe registrar al menos un tipo de huevo",
        )
    if total > camada.cantidad_actual:
        raise HTTPException(
            status_code=400,
            detail="El total no puede superar las aves actuales de la camada",
        )

    tipos = {tipo.nombre_tipo: tipo for tipo in db.query(TipoHuevo).all()}
    produccion = _buscar_produccion_del_dia(db, usuario, datos)
    if produccion is None:
        produccion = ProduccionDiaria(
            id_usuario=usuario.id_usuario,
            id_camada=datos.id_camada,
            fecha_recoleccion=datos.fecha_recoleccion,
        )
        db.add(produccion)

    produccion.total_huevos = total
    produccion.observaciones = datos.observaciones
    _sincronizar_detalle(produccion, tipos, cantidades)

    try:
        db.commit()
    except IntegrityError as exc:
        # El unique uq_prod_usuario_camada_fecha es el respaldo ante dos
        # peticiones concurrentes con la misma camada y fecha.
        db.rollback()
        raise HTTPException(
            status_code=409,
            detail="Ya existe producción registrada para esta camada y fecha",
        ) from exc
    db.refresh(produccion)
    return produccion


def listar_produccion(
    db: Session,
    usuario: Usuario,
    camada_id: int | None = None,
    fecha: date | None = None,
) -> list[ProduccionDiaria]:
    """Lista la producción del usuario con filtros opcionales.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la producción.
        camada_id: Camada por la que filtrar.
        fecha: Fecha de recolección por la que filtrar.

    Returns:
        list[ProduccionDiaria]: Producciones de la más reciente a la más
        antigua.
    """
    consulta = db.query(ProduccionDiaria).filter(
        ProduccionDiaria.id_usuario == usuario.id_usuario
    )
    if camada_id is not None:
        consulta = consulta.filter(ProduccionDiaria.id_camada == camada_id)
    if fecha is not None:
        consulta = consulta.filter(ProduccionDiaria.fecha_recoleccion == fecha)
    return consulta.order_by(
        ProduccionDiaria.fecha_recoleccion.desc(),
        ProduccionDiaria.id_produccion.desc(),
    ).all()


def obtener_produccion(
    db: Session, usuario: Usuario, id_produccion: int
) -> ProduccionDiaria:
    """Obtiene una producción del usuario con su detalle por tipo.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la producción.
        id_produccion: Identificador de la producción a consultar.

    Returns:
        ProduccionDiaria: La producción encontrada.

    Raises:
        HTTPException: 404 si la producción no existe o no es del usuario.
    """
    return _obtener_produccion_de_usuario(db, usuario, id_produccion)


def obtener_resumen(db: Session, usuario: Usuario) -> dict:
    """Calcula los acumulados de producción del usuario (RF-18).

    Args:
        db: Sesión de base de datos.
        usuario: Usuario del que se calcula el resumen.

    Returns:
        dict: Totales de hoy, últimos 7 días y mes actual, más el desglose
        del mes en curso por tipo de huevo (los cuatro tipos incluidos).
    """
    hoy = date.today()
    inicio_mes = hoy.replace(day=1)

    por_tipo = {
        tipo.nombre_tipo: 0
        for tipo in db.query(TipoHuevo).order_by(TipoHuevo.id_tipo).all()
    }
    filas = (
        db.query(TipoHuevo.nombre_tipo, func.sum(ProduccionDetalle.cantidad))
        .join(
            ProduccionDetalle,
            ProduccionDetalle.id_tipo == TipoHuevo.id_tipo,
        )
        .join(
            ProduccionDiaria,
            ProduccionDetalle.id_produccion == ProduccionDiaria.id_produccion,
        )
        .filter(
            ProduccionDiaria.id_usuario == usuario.id_usuario,
            ProduccionDiaria.fecha_recoleccion >= inicio_mes,
        )
        .group_by(TipoHuevo.nombre_tipo)
        .all()
    )
    for nombre, cantidad in filas:
        por_tipo[nombre] = int(cantidad or 0)

    return {
        "total_hoy": _total_produccion(db, usuario, hoy),
        "total_semana": _total_produccion(db, usuario, hoy - timedelta(days=6)),
        "total_mes": _total_produccion(db, usuario, inicio_mes),
        "por_tipo": por_tipo,
    }


def obtener_disponibles(db: Session, usuario: Usuario) -> dict:
    """Calcula el inventario de huevos disponibles del usuario (RF-16).

    El disponible es un valor computado (no hay tabla física):
    `disponible = producido - vendido` por tipo, acotado a cero. Lo
    vendido se lee de `detalle_pedido`/`pedido` excluyendo los pedidos
    cancelados (vista `v_produccion_detallada`).

    Args:
        db: Sesión de base de datos.
        usuario: Usuario del que se calculan los disponibles.

    Returns:
        dict: Total disponible y el detalle por tipo, con los cuatro tipos
        del catálogo siempre presentes.
    """
    producido = dict(
        db.query(
            ProduccionDetalle.id_tipo,
            func.coalesce(func.sum(ProduccionDetalle.cantidad), 0),
        )
        .join(
            ProduccionDiaria,
            ProduccionDetalle.id_produccion == ProduccionDiaria.id_produccion,
        )
        .filter(ProduccionDiaria.id_usuario == usuario.id_usuario)
        .group_by(ProduccionDetalle.id_tipo)
        .all()
    )
    vendido = _vendido_por_tipo(db, usuario.id_usuario)

    por_tipo: list[DisponibleTipoResponse] = []
    total_disponible = 0
    for tipo in db.query(TipoHuevo).order_by(TipoHuevo.id_tipo).all():
        en_produccion = int(producido.get(tipo.id_tipo, 0))
        en_ventas = int(vendido.get(tipo.id_tipo, 0))
        disponible = max(en_produccion - en_ventas, 0)
        total_disponible += disponible
        por_tipo.append(
            DisponibleTipoResponse(
                id_tipo=tipo.id_tipo,
                nombre_tipo=tipo.nombre_tipo,
                producido=en_produccion,
                vendido=en_ventas,
                disponible=disponible,
            )
        )

    return {"total_disponible": total_disponible, "por_tipo": por_tipo}


def _buscar_produccion_del_dia(
    db: Session, usuario: Usuario, datos: ProduccionCreate
) -> ProduccionDiaria | None:
    """Busca la producción de una camada en la fecha indicada, si existe."""
    return (
        db.query(ProduccionDiaria)
        .filter(
            ProduccionDiaria.id_usuario == usuario.id_usuario,
            ProduccionDiaria.id_camada == datos.id_camada,
            ProduccionDiaria.fecha_recoleccion == datos.fecha_recoleccion,
        )
        .first()
    )


def _sincronizar_detalle(
    produccion: ProduccionDiaria,
    tipos: dict[str, TipoHuevo],
    cantidades: dict[str, int],
) -> None:
    """Crea, actualiza o elimina las filas de detalle de una producción.

    Se trabaja en sitio (sin borrar todo el detalle) para no chocar con el
    unique `uq_prodet_prod_tipo` al reemplazar las cantidades del día.
    """
    existentes = {detalle.id_tipo: detalle for detalle in produccion.detalles}
    for nombre, tipo in tipos.items():
        cantidad = cantidades[nombre]
        detalle = existentes.pop(tipo.id_tipo, None)
        if cantidad > 0 and detalle is None:
            produccion.detalles.append(
                ProduccionDetalle(id_tipo=tipo.id_tipo, cantidad=cantidad)
            )
        elif cantidad > 0:
            detalle.cantidad = cantidad
        elif detalle is not None:
            produccion.detalles.remove(detalle)


def _cantidades_por_tipo(datos: ProduccionCreate) -> dict[str, int]:
    """Devuelve las cantidades por nombre de tipo, ya en unidades."""
    base = {
        "AA": datos.aa,
        "A": datos.a,
        "B": datos.b,
        "No_apto": datos.no_apto,
    }
    if datos.unidad == "cubeta":
        return {
            nombre: cantidad * HUEVOS_POR_CUBETA for nombre, cantidad in base.items()
        }
    return base


def _total_produccion(db: Session, usuario: Usuario, desde: date) -> int:
    """Suma los huevos producidos desde una fecha hasta hoy (inclusive)."""
    total = (
        db.query(func.coalesce(func.sum(ProduccionDiaria.total_huevos), 0))
        .filter(
            ProduccionDiaria.id_usuario == usuario.id_usuario,
            ProduccionDiaria.fecha_recoleccion >= desde,
            ProduccionDiaria.fecha_recoleccion <= date.today(),
        )
        .scalar()
    )
    return int(total or 0)


def _vendido_por_tipo(db: Session, id_usuario: int) -> dict[int, int]:
    """Suma los huevos vendidos por tipo, excluyendo pedidos cancelados.

    Se usa SQL parametrizado porque las tablas de ventas todavía no tienen
    modelo ORM (las implementará el módulo de Ventas).
    """
    filas = db.execute(
        text("""
            SELECT dp.id_tipo AS id_tipo,
                   COALESCE(SUM(dp.cantidad), 0) AS vendido
            FROM detalle_pedido dp
            JOIN pedido p ON dp.id_pedido = p.id_pedido
            WHERE p.id_usuario = :id_usuario
              AND p.estado_pedido != 'cancelado'
            GROUP BY dp.id_tipo
            """),
        {"id_usuario": id_usuario},
    ).all()
    return {int(fila.id_tipo): int(fila.vendido) for fila in filas}


def _obtener_produccion_de_usuario(
    db: Session, usuario: Usuario, id_produccion: int
) -> ProduccionDiaria:
    """Busca una producción que pertenezca al usuario o lanza 404."""
    produccion = (
        db.query(ProduccionDiaria)
        .filter(
            ProduccionDiaria.id_produccion == id_produccion,
            ProduccionDiaria.id_usuario == usuario.id_usuario,
        )
        .first()
    )
    if produccion is None:
        raise HTTPException(status_code=404, detail="Producción no encontrada")
    return produccion
