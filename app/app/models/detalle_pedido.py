from decimal import Decimal
from typing import TYPE_CHECKING

from sqlalchemy import CheckConstraint, ForeignKey, Integer, Numeric, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base
from app.models.tipo_huevo import TipoHuevo

if TYPE_CHECKING:
    from app.models.pedido import Pedido


class DetallePedido(Base):
    """Línea de un pedido: tipo de huevo, cantidad y precio por unidad.

    Mapea la tabla `detalle_pedido` del DDL. El `precio_unitario` se
    congela al crear el pedido para que un cambio posterior del
    `valor_unidad` del stock no altere los registros históricos.
    """

    __tablename__ = "detalle_pedido"
    __table_args__ = (
        UniqueConstraint("id_pedido", "id_tipo", name="uq_detped_pedido_tipo"),
        CheckConstraint("cantidad > 0", name="chk_detped_cantidad"),
        CheckConstraint("precio_unitario > 0", name="chk_detped_precio"),
    )

    id_detalle_pedido: Mapped[int] = mapped_column(
        Integer, primary_key=True, autoincrement=True
    )
    id_pedido: Mapped[int] = mapped_column(
        Integer,
        ForeignKey("pedido.id_pedido", ondelete="CASCADE"),
        nullable=False,
    )
    id_tipo: Mapped[int] = mapped_column(
        Integer, ForeignKey("tipo_huevo.id_tipo"), nullable=False
    )
    cantidad: Mapped[int] = mapped_column(Integer, nullable=False)
    precio_unitario: Mapped[Decimal] = mapped_column(Numeric(10, 2), nullable=False)

    tipo: Mapped[TipoHuevo] = relationship(TipoHuevo)
    pedido: Mapped["Pedido"] = relationship(back_populates="detalles")
