from datetime import date

from sqlalchemy import (
    CheckConstraint,
    Date,
    ForeignKey,
    Integer,
    Text,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base
from app.models.produccion_detalle import ProduccionDetalle


class ProduccionDiaria(Base):
    """Recolección de huevos de una camada en una fecha concreta.

    Mapea la tabla `produccion_diaria` del DDL. El `total_huevos` siempre
    lo calcula el servidor sumando el detalle por tipo (nunca se recibe
    del cliente) y no puede ser negativo (`chk_prod_total`). Un usuario no
    puede registrar dos veces la misma camada en la misma fecha
    (`uq_prod_usuario_camada_fecha`).
    """

    __tablename__ = "produccion_diaria"
    __table_args__ = (
        UniqueConstraint(
            "id_usuario",
            "id_camada",
            "fecha_recoleccion",
            name="uq_prod_usuario_camada_fecha",
        ),
        CheckConstraint("total_huevos >= 0", name="chk_prod_total"),
    )

    id_produccion: Mapped[int] = mapped_column(
        Integer, primary_key=True, autoincrement=True
    )
    id_usuario: Mapped[int] = mapped_column(
        Integer, ForeignKey("usuario.id_usuario"), nullable=False
    )
    id_camada: Mapped[int] = mapped_column(
        Integer, ForeignKey("camada.id_camada"), nullable=False
    )
    fecha_recoleccion: Mapped[date] = mapped_column(Date, nullable=False)
    total_huevos: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    observaciones: Mapped[str | None] = mapped_column(Text, nullable=True)

    detalles: Mapped[list[ProduccionDetalle]] = relationship(
        ProduccionDetalle,
        cascade="all, delete-orphan",
        back_populates="produccion",
    )
