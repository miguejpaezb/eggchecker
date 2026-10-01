from datetime import date
from decimal import Decimal
from typing import TYPE_CHECKING

from sqlalchemy import (
    CheckConstraint,
    Date,
    ForeignKey,
    Integer,
    Numeric,
    String,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base
from app.models.cliente import Cliente

if TYPE_CHECKING:
    from app.models.detalle_pedido import DetallePedido


class Pedido(Base):
    """Solicitud de compra de un cliente.

    Mapea la tabla `pedido` del DDL. Cada pedido pertenece a un cliente y
    a un usuario. El `valor_total` lo calcula el servidor sumando el
    detalle (`cantidad * precio_unitario`) y se congela al crear o editar
    el pedido. Los estados posibles son 'pendiente', 'enviado',
    'recibido' y 'cancelado' (`chk_pedido_estado`).
    """

    __tablename__ = "pedido"
    __table_args__ = (
        CheckConstraint(
            "estado_pedido IN ('pendiente','enviado','recibido','cancelado')",
            name="chk_pedido_estado",
        ),
        CheckConstraint("valor_total >= 0", name="chk_pedido_valor"),
    )

    id_pedido: Mapped[int] = mapped_column(
        Integer, primary_key=True, autoincrement=True
    )
    id_cliente: Mapped[int] = mapped_column(
        Integer, ForeignKey("cliente.id_cliente"), nullable=False
    )
    id_usuario: Mapped[int] = mapped_column(
        Integer, ForeignKey("usuario.id_usuario"), nullable=False
    )
    fecha_pedido: Mapped[date] = mapped_column(Date, nullable=False)
    estado_pedido: Mapped[str] = mapped_column(
        String(20), nullable=False, default="pendiente"
    )
    valor_total: Mapped[Decimal] = mapped_column(
        Numeric(12, 2), nullable=False, default=0
    )

    cliente: Mapped[Cliente] = relationship(Cliente)
    detalles: Mapped[list["DetallePedido"]] = relationship(
        "DetallePedido",
        cascade="all, delete-orphan",
        back_populates="pedido",
    )
