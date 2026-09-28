from datetime import date

from sqlalchemy import Boolean, Date, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class Cliente(Base):
    """Comprador del avicultor.

    Mapea la tabla `cliente` del DDL. Cada cliente pertenece a un usuario
    (`id_usuario`). No se borra al suspenderse: la suspensión solo deja
    `activo` en False. `fecha_ultima_compra` se actualiza al registrar una
    venta (módulo de ventas); mientras no haya compras queda en None.
    """

    __tablename__ = "cliente"

    id_cliente: Mapped[int] = mapped_column(
        Integer, primary_key=True, autoincrement=True
    )
    id_usuario: Mapped[int] = mapped_column(
        Integer, ForeignKey("usuario.id_usuario"), nullable=False
    )
    nombre_cliente: Mapped[str] = mapped_column(String(80), nullable=False)
    telefono: Mapped[str | None] = mapped_column(String(20), nullable=True)
    direccion: Mapped[str | None] = mapped_column(String(200), nullable=True)
    fecha_ultima_compra: Mapped[date | None] = mapped_column(Date, nullable=True)
    activo: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
