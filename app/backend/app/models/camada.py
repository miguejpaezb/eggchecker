from datetime import date

from sqlalchemy import CheckConstraint, Date, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class Camada(Base):
    """Lote de aves ponedoras que un avicultor mantiene en producción.

    Mapea la tabla `camada` del DDL. El ciclo de vida de una camada no
    usa borrado físico (las FKs son RESTRICT); una camada se cierra al
    pasar su estado a 'retirada'.
    """

    __tablename__ = "camada"
    __table_args__ = (
        CheckConstraint("estado IN ('activa','retirada')", name="chk_camada_estado"),
        CheckConstraint("cantidad_inicial > 0", name="chk_camada_cant_ini"),
        CheckConstraint("cantidad_actual >= 0", name="chk_camada_cant_act"),
        CheckConstraint(
            "cantidad_actual <= cantidad_inicial", name="chk_camada_cant_lte"
        ),
    )

    id_camada: Mapped[int] = mapped_column(
        Integer, primary_key=True, autoincrement=True
    )
    id_usuario: Mapped[int] = mapped_column(
        Integer, ForeignKey("usuario.id_usuario"), nullable=False
    )
    nombre_camada: Mapped[str] = mapped_column(String(60), nullable=False)
    fecha_ingreso: Mapped[date] = mapped_column(Date, nullable=False)
    cantidad_inicial: Mapped[int] = mapped_column(Integer, nullable=False)
    cantidad_actual: Mapped[int] = mapped_column(Integer, nullable=False)
    estado: Mapped[str] = mapped_column(String(20), nullable=False, default="activa")
