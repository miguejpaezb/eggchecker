from datetime import date

from sqlalchemy import CheckConstraint, Date, ForeignKey, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class EventoSanitario(Base):
    """Evento de salud de una camada: vacunación, enfermedad o mortalidad.

    Mapea la tabla `evento_sanitario` del DDL. `mortalidad` solo aplica a
    los eventos de tipo 'mortalidad' y debe ser mayor o igual a cero.
    """

    __tablename__ = "evento_sanitario"
    __table_args__ = (
        CheckConstraint(
            "tipo_evento IN ('vacunacion','mortalidad','enfermedad','otro')",
            name="chk_evento_tipo",
        ),
        CheckConstraint("mortalidad >= 0", name="chk_evento_mortalidad"),
    )

    id_evento: Mapped[int] = mapped_column(
        Integer, primary_key=True, autoincrement=True
    )
    id_camada: Mapped[int] = mapped_column(
        Integer, ForeignKey("camada.id_camada"), nullable=False
    )
    tipo_evento: Mapped[str] = mapped_column(String(20), nullable=False)
    fecha_evento: Mapped[date] = mapped_column(Date, nullable=False)
    descripcion: Mapped[str] = mapped_column(Text, nullable=False)
    mortalidad: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
