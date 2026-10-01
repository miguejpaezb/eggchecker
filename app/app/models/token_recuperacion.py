from datetime import datetime

from sqlalchemy import Boolean, DateTime, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class TokenRecuperacion(Base):
    """Token de recuperación de contraseña ligado a un único usuario.

    Guarda únicamente el hash SHA-256 del token (nunca el valor crudo). El
    vínculo con `id_usuario` garantiza que un token solo pueda restablecer
    la contraseña de la cuenta que lo solicitó, nunca la de otra.
    """

    __tablename__ = "token_recuperacion"

    id_token: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    id_usuario: Mapped[int] = mapped_column(
        Integer,
        ForeignKey("usuario.id_usuario", ondelete="CASCADE", onupdate="CASCADE"),
        nullable=False,
        index=True,
    )
    token_hash: Mapped[str] = mapped_column(String(64), nullable=False, index=True)
    fecha_creacion: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    fecha_expiracion: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    usado: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
