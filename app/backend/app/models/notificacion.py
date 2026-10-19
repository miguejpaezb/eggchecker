from datetime import date, datetime
from decimal import Decimal

from sqlalchemy import (
    CheckConstraint,
    Date,
    DateTime,
    ForeignKey,
    Integer,
    Numeric,
    String,
    Text,
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

# Avisos de producción diaria: por debajo del 85% de postura o sin registro.
TIPO_PRODUCCION_BAJA = "produccion_baja"
TIPO_PRODUCCION_SIN_REGISTRO = "produccion_sin_registro"

# Recordatorio de vacunación o suministro de vitaminas.
TIPO_RECORDATORIO_SANITARIO = "recordatorio_sanitario"

# Recordatorio periódico para revisar los reportes.
TIPO_RESUMEN_PERIODICO = "resumen_periodico"

# Momento del aviso de un recordatorio: el día anterior o el mismo día.
MOMENTO_PREVIO = "previo"
MOMENTO_HOY = "hoy"
MOMENTOS = (MOMENTO_PREVIO, MOMENTO_HOY)


class Notificacion(Base):
    """Aviso persistente dirigido a un avicultor.

    Mapea la tabla `notificacion` del DDL. Cada aviso pertenece a un
    usuario y, opcionalmente, a una camada, un insumo o un recordatorio.
    Una notificación no se borra sola: el usuario la marca como leída o la
    elimina.

    No hay una restricción única en la base porque un mismo tipo puede
    repetirse en fechas distintas (avisos diarios de producción, dosis de
    un recordatorio). La deduplicación se hace en el servicio con
    `fecha_referencia` y `momento`.
    """

    __tablename__ = "notificacion"
    __table_args__ = (
        CheckConstraint("leida IN (0,1)", name="chk_notificacion_leida"),
        CheckConstraint("eliminada IN (0,1)", name="chk_notificacion_eliminada"),
        CheckConstraint(
            "momento IS NULL OR momento IN ('previo','hoy')",
            name="chk_notificacion_momento",
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
    id_recordatorio: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("recordatorio_sanitario.id_recordatorio"), nullable=True
    )
    tipo: Mapped[str] = mapped_column(String(40), nullable=False)
    titulo: Mapped[str] = mapped_column(String(80), nullable=False)
    mensaje: Mapped[str] = mapped_column(Text, nullable=False)
    leida: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    eliminada: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    stock_referencia: Mapped[Decimal | None] = mapped_column(
        Numeric(10, 2), nullable=True
    )
    # Fecha del evento al que se refiere el aviso (ej. el día evaluado de
    # producción o la ocurrencia del recordatorio). Deduplica avisos
    # repetidos de días anteriores.
    fecha_referencia: Mapped[date | None] = mapped_column(Date, nullable=True)
    momento: Mapped[str | None] = mapped_column(String(10), nullable=True)
    fecha_creacion: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, default=datetime.now
    )
