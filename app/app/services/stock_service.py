from datetime import datetime
from decimal import Decimal

from app.models.stock_produccion import StockProduccion
from app.models.tipo_huevo import TipoHuevo
from app.models.usuario import Usuario
from app.schemas.ventas import StockResponse, StockTipoResponse
from fastapi import HTTPException
from sqlalchemy.orm import Session

# Los huevos No_apto (rotos o dañados) no son consumibles ni vendibles, así
# que no cuentan en el stock disponible ni se pueden incluir en un pedido.
TIPO_HUEVO_NO_VENDIBLE = "No_apto"


def obtener_stock(db: Session, usuario: Usuario) -> StockResponse:
    """Devuelve el stock disponible del usuario por tipo de huevo.

    Excluye el tipo No_apto, que no es vendible.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del stock.

    Returns:
        StockResponse: Total disponible y detalle por tipo vendible.
    """
    registros = _mapa_stock(db, usuario.id_usuario)
    por_tipo: list[StockTipoResponse] = []
    total = 0
    for tipo in db.query(TipoHuevo).order_by(TipoHuevo.id_tipo).all():
        if tipo.nombre_tipo == TIPO_HUEVO_NO_VENDIBLE:
            continue
        registro = registros.get(tipo.id_tipo)
        cantidad = registro.cantidad_actual if registro else 0
        valor = registro.valor_unidad if registro else Decimal("0.00")
        total += cantidad
        por_tipo.append(
            StockTipoResponse(
                id_tipo=tipo.id_tipo,
                nombre_tipo=tipo.nombre_tipo,
                cantidad_actual=cantidad,
                valor_unidad=valor,
            )
        )
    return StockResponse(total_disponible=total, por_tipo=por_tipo)


def obtener_o_crear(db: Session, id_usuario: int, id_tipo: int) -> StockProduccion:
    """Recupera la fila de stock de un usuario y tipo, creándola si falta.

    Args:
        db: Sesión de base de datos.
        id_usuario: Usuario dueño del stock.
        id_tipo: Tipo de huevo del registro.

    Returns:
        StockProduccion: El registro existente o uno nuevo en cero.
    """
    _validar_tipo_existe(db, id_tipo)
    registro = (
        db.query(StockProduccion)
        .filter(
            StockProduccion.id_usuario == id_usuario,
            StockProduccion.id_tipo == id_tipo,
        )
        .first()
    )
    if registro is None:
        registro = StockProduccion(
            id_usuario=id_usuario,
            id_tipo=id_tipo,
            cantidad_actual=0,
            valor_unidad=Decimal("0.00"),
        )
        db.add(registro)
        db.flush()
    return registro


def aplicar_delta(
    db: Session,
    id_usuario: int,
    id_tipo: int,
    delta: int,
    valor_unidad: Decimal | None = None,
) -> StockProduccion:
    """Suma o resta huevos al stock de un tipo y actualiza la fecha.

    Args:
        db: Sesión de base de datos.
        id_usuario: Usuario dueño del stock.
        id_tipo: Tipo de huevo afectado.
        delta: Variación (positiva suma, negativa resta).
        valor_unidad: Nuevo precio de referencia, si se desea fijar.

    Returns:
        StockProduccion: El registro actualizado.

    Raises:
        HTTPException: 400 si la resta dejaría el stock en negativo.
    """
    registro = obtener_o_crear(db, id_usuario, id_tipo)
    nueva_cantidad = registro.cantidad_actual + delta
    if nueva_cantidad < 0:
        raise HTTPException(
            status_code=400,
            detail="No hay suficientes huevos en stock para ese ajuste",
        )
    registro.cantidad_actual = nueva_cantidad
    if valor_unidad is not None:
        registro.valor_unidad = valor_unidad
    registro.ultima_modificacion = datetime.now()
    db.flush()
    return registro


def _mapa_stock(db: Session, id_usuario: int) -> dict[int, StockProduccion]:
    """Devuelve el stock del usuario indexado por id_tipo."""
    filas = (
        db.query(StockProduccion).filter(StockProduccion.id_usuario == id_usuario).all()
    )
    return {fila.id_tipo: fila for fila in filas}


def _validar_tipo_existe(db: Session, id_tipo: int) -> None:
    """Comprueba que el tipo de huevo exista o lanza 404."""
    if db.get(TipoHuevo, id_tipo) is None:
        raise HTTPException(status_code=404, detail="Tipo de huevo no encontrado")
