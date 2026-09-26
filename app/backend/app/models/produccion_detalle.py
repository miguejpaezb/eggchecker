from typing import TYPE_CHECKING

from sqlalchemy import CheckConstraint, ForeignKey, Integer, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base
from app.models.tipo_huevo import TipoHuevo

if TYPE_CHECKING:
    from app.models.produccion_diaria import ProduccionDiaria


class ProduccionDetalle(Base):
    """Desglose por tipo de huevo de un registro de producción.

    Mapea la tabla `produccion_detalle` del DDL, que resuelve la relación
    M:N entre producción y tipos de huevo. Solo se crea una fila por tipo
    con `cantidad > 0`; la cantidad se guarda ya en unidades (las cubetas
    se convierten antes de insertar).
    """

    __tablename__ = "produccion_detalle"
    __table_args__ = (
        UniqueConstraint("id_produccion", "id_tipo", name="uq_prodet_prod_tipo"),
        CheckConstraint("cantidad >= 0", name="chk_prodet_cantidad"),
    )

    id_detalle: Mapped[int] = mapped_column(
        Integer, primary_key=True, autoincrement=True
    )
    id_produccion: Mapped[int] = mapped_column(
        Integer,
        ForeignKey("produccion_diaria.id_produccion", ondelete="CASCADE"),
        nullable=False,
    )
    id_tipo: Mapped[int] = mapped_column(
        Integer, ForeignKey("tipo_huevo.id_tipo"), nullable=False
    )
    cantidad: Mapped[int] = mapped_column(Integer, nullable=False, default=0)

    tipo: Mapped[TipoHuevo] = relationship(TipoHuevo)
    produccion: Mapped["ProduccionDiaria"] = relationship(back_populates="detalles")
