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
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base

# Tipos de notificación por nivel de stock de un insumo.
TIPO_STOCK_MINIMO = "stock_minimo"
TIPO_STOCK_BAJO = "stock_bajo"
TIPO_SIN_STOCK = "sin_stock"
TIPOS_STOCK = (TIPO_STOCK_MINIMO, TIPO_STOCK_BAJO, TIPO_SIN_STOCK)

# Tipo de notificación de camada que superó la vida productiva (72 semanas).
TIPO_CAMADA_LIMITE = "camada_limite"


class Notificacion(Base):
    """Aviso persistente dirigido a un avicultor.

    Mapea la tabla `notificacion` del DDL. Cada aviso pertenece a un
    usuario y, opcionalmente, a una camada. Una notificación no se borra
    sola: el usuario la marca como leída o la elimina.
    """

    __tablename__ = "notificacion"
    __table_args__ = (
        CheckConstraint("leida IN (0,1)", name="chk_notificacion_leida"),
        CheckConstraint("eliminada IN (0,1)", name="chk_notificacion_eliminada"),
        UniqueConstraint(
            "id_usuario",
            "id_camada",
            "tipo",
            name="uq_notificacion_usuario_camada_tipo",
        ),
    )

    id_notificacion: Mapped[int] = mapped_column(
        Integer, primary_key=True, autoincrement=True
    )
    id_usuario: Mapped[int] = mapped_column(
        Integer, ForeignKey("usuario.id_usuario"), nullable=False
    )
    id_camada: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("camada.id_camada"), nullable=True
    )
    id_insumo: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("insumo.id_insumo"), nullable=True
    )
    tipo: Mapped[str] = mapped_column(String(40), nullable=False)
    titulo: Mapped[str] = mapped_column(String(80), nullable=False)
    mensaje: Mapped[str] = mapped_column(Text, nullable=False)
    leida: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    eliminada: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    stock_referencia: Mapped[Decimal | None] = mapped_column(
        Numeric(10, 2), nullable=True
    )
    fecha_creacion: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, default=datetime.now
    )
