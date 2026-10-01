from sqlalchemy import Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class CategoriaInsumo(Base):
    """Catálogo normalizado de tipos de insumo que un avicultor usa.

    Mapea la tabla `categoria_insumo` del DDL. Es un catálogo global
    (sin `id_usuario`): cualquier avicultor puede registrar insumos en
    cualquiera de estas categorías. El nombre es único en todo el sistema
    (`uq_categoria_nombre`).
    """

    __tablename__ = "categoria_insumo"

    id_categoria: Mapped[int] = mapped_column(
        Integer, primary_key=True, autoincrement=True
    )
    nombre_categ: Mapped[str] = mapped_column(String(60), nullable=False, unique=True)
    descripcion: Mapped[str | None] = mapped_column(String(200), nullable=True)
