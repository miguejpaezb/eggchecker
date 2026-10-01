from decimal import Decimal

from sqlalchemy import Boolean, CheckConstraint, ForeignKey, Integer, Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class Insumo(Base):
    """Recurso de inventario que un avicultor mantiene para su producción.

    Mapea la tabla `insumo` del DDL. Cada insumo pertenece a un usuario
    (`id_usuario`) y a una categoría del catálogo global. El stock nunca
    baja de cero (`chk_insumo_stock`) y la alerta de umbral se dispara
    cuando el stock cae por debajo de `umbral_minimo`. Los insumos no se
    borran físicamente: se desactivan con `activo = False`.
    """

    __tablename__ = "insumo"
    __table_args__ = (
        CheckConstraint("stock_actual >= 0", name="chk_insumo_stock"),
        CheckConstraint("umbral_minimo >= 0", name="chk_insumo_umbral"),
        CheckConstraint("descontinuado IN (0,1)", name="chk_insumo_descontinuado"),
    )

    id_insumo: Mapped[int] = mapped_column(
        Integer, primary_key=True, autoincrement=True
    )
    id_usuario: Mapped[int] = mapped_column(
        Integer, ForeignKey("usuario.id_usuario"), nullable=False
    )
    id_categoria: Mapped[int] = mapped_column(
        Integer, ForeignKey("categoria_insumo.id_categoria"), nullable=False
    )
    nombre_insumo: Mapped[str] = mapped_column(String(80), nullable=False)
    unidad_medida: Mapped[str] = mapped_column(String(20), nullable=False)
    stock_actual: Mapped[Decimal] = mapped_column(
        Numeric(10, 2), nullable=False, default=0
    )
    umbral_minimo: Mapped[Decimal] = mapped_column(
        Numeric(10, 2), nullable=False, default=0
    )
    activo: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    descontinuado: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
