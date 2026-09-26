from sqlalchemy import Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class TipoHuevo(Base):
    """Catálogo fijo de clasificaciones de huevo (AA, A, B, No_apto).

    Mapea la tabla `tipo_huevo` del DDL. Es un catálogo global (sin
    `id_usuario`): la producción diaria clasifica cada recolección usando
    estos tipos. El nombre es único en todo el sistema
    (`uq_tipo_huevo_nombre`).
    """

    __tablename__ = "tipo_huevo"

    id_tipo: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    nombre_tipo: Mapped[str] = mapped_column(String(20), nullable=False, unique=True)
    descripcion: Mapped[str | None] = mapped_column(String(200), nullable=True)
