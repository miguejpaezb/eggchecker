from datetime import datetime
from decimal import Decimal

from sqlalchemy import (
    CheckConstraint,
    DateTime,
    ForeignKey,
    Integer,
    Numeric,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base
from app.models.tipo_huevo import TipoHuevo


class StockProduccion(Base):
    """Existencias disponibles de huevos del avicultor por tipo.

    Mapea la tabla `stock_produccion`. Hay una sola fila por usuario y
    tipo (`uq_stock_usuario_tipo`): se actualiza (upsert) al registrar
    producción (suma) y ventas (resta), nunca se insertan duplicados.
    `valor_unidad` es el precio de referencia vigente del tipo y
    `ultima_modificacion` la fecha y hora del último cambio.
    """

    __tablename__ = "stock_produccion"
    __table_args__ = (
        UniqueConstraint("id_usuario", "id_tipo", name="uq_stock_usuario_tipo"),
        CheckConstraint("cantidad_actual >= 0", name="chk_stock_cantidad"),
        CheckConstraint("valor_unidad >= 0", name="chk_stock_valor"),
    )

    id_stock: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    id_usuario: Mapped[int] = mapped_column(
        Integer, ForeignKey("usuario.id_usuario"), nullable=False
    )
    id_tipo: Mapped[int] = mapped_column(
        Integer, ForeignKey("tipo_huevo.id_tipo"), nullable=False
    )
    cantidad_actual: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    valor_unidad: Mapped[Decimal] = mapped_column(
        Numeric(12, 2), nullable=False, default=0
    )
    ultima_modificacion: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, default=datetime.now
    )

    tipo: Mapped[TipoHuevo] = relationship(TipoHuevo)
