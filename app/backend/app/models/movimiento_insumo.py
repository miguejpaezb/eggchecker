from datetime import datetime
from decimal import Decimal

from sqlalchemy import (
    CheckConstraint,
    DateTime,
    ForeignKey,
    Integer,
    Numeric,
    String,
    Text,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class MovimientoInsumo(Base):
    """Trazabilidad de cada entrada o salida de stock de un insumo.

    Mapea la tabla `movimiento_insumo` del DDL. Cada movimiento pertenece
    a un insumo (y por extensión a su usuario); no guarda `id_usuario`
    propio. Solo existen 'entrada' y 'salida' (`chk_movimiento_tipo`) y la
    cantidad siempre es positiva (`chk_movimiento_cantidad`).
    """

    __tablename__ = "movimiento_insumo"
    __table_args__ = (
        CheckConstraint(
            "tipo_movimiento IN ('entrada','salida')",
            name="chk_movimiento_tipo",
        ),
        CheckConstraint("cantidad > 0", name="chk_movimiento_cantidad"),
    )

    id_movimiento: Mapped[int] = mapped_column(
        Integer, primary_key=True, autoincrement=True
    )
    id_insumo: Mapped[int] = mapped_column(
        Integer, ForeignKey("insumo.id_insumo"), nullable=False
    )
    tipo_movimiento: Mapped[str] = mapped_column(String(10), nullable=False)
    cantidad: Mapped[Decimal] = mapped_column(Numeric(10, 2), nullable=False)
    fecha_movimiento: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, default=datetime.now
    )
    observaciones: Mapped[str | None] = mapped_column(Text, nullable=True)
