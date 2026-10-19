from datetime import date, datetime

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    Date,
    DateTime,
    ForeignKey,
    Integer,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class RecordatorioDosis(Base):
    """Aplicación programada de un recordatorio sanitario en una camada.

    Mapea la tabla `recordatorio_dosis` del DDL. Cada ocurrencia pertenece
    siempre a una camada concreta, incluso cuando la regla es global, para
    poder marcar la aplicación de cada camada por separado y descontar su
    inventario.

    Al aplicarse guarda el movimiento de salida que descontó el stock y el
    evento sanitario que quedó en el historial de la camada.
    """

    __tablename__ = "recordatorio_dosis"
    __table_args__ = (
        UniqueConstraint(
            "id_recordatorio",
            "id_camada",
            "fecha_programada",
            name="uq_dosis_recordatorio_camada_fecha",
        ),
        CheckConstraint("aplicada IN (0,1)", name="chk_dosis_aplicada"),
        CheckConstraint("numero_dosis > 0", name="chk_dosis_numero"),
    )

    id_dosis: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    id_recordatorio: Mapped[int] = mapped_column(
        Integer,
        ForeignKey("recordatorio_sanitario.id_recordatorio"),
        nullable=False,
    )
    id_camada: Mapped[int] = mapped_column(
        Integer, ForeignKey("camada.id_camada"), nullable=False
    )
    fecha_programada: Mapped[date] = mapped_column(Date, nullable=False)
    numero_dosis: Mapped[int] = mapped_column(Integer, nullable=False)
    aplicada: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    fecha_aplicacion: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    id_movimiento: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("movimiento_insumo.id_movimiento"), nullable=True
    )
    id_evento: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("evento_sanitario.id_evento"), nullable=True
    )
