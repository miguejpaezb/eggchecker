from datetime import datetime
from decimal import Decimal

from sqlalchemy import (
    CheckConstraint,
    DateTime,
    ForeignKey,
    Integer,
    Numeric,
    Text,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class AlimentacionCamada(Base):
    """Alimento que consumió una camada en una fecha y hora concretas.

    Mapea la tabla `alimentacion_camada` del DDL. Cada fila se registra
    junto con un movimiento de salida del insumo, que es el que descuenta
    el stock. Una camada puede tener varias filas el mismo día, con el
    mismo insumo o con varios.
    """

    __tablename__ = "alimentacion_camada"
    __table_args__ = (
        CheckConstraint("cantidad > 0", name="chk_alimentacion_cantidad"),
    )

    id_alimentacion: Mapped[int] = mapped_column(
        Integer, primary_key=True, autoincrement=True
    )
    id_camada: Mapped[int] = mapped_column(
        Integer, ForeignKey("camada.id_camada"), nullable=False
    )
    id_insumo: Mapped[int] = mapped_column(
        Integer, ForeignKey("insumo.id_insumo"), nullable=False
    )
    cantidad: Mapped[Decimal] = mapped_column(Numeric(10, 2), nullable=False)
    fecha_hora: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, default=datetime.now
    )
    observaciones: Mapped[str | None] = mapped_column(Text, nullable=True)
