from datetime import date

from sqlalchemy import Boolean, CheckConstraint, Date, Integer, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class Usuario(Base):
    """Entidad central del sistema: avicultor registrado en EggChecker.

    Mapea la tabla `usuario` del DDL y es la raíz del modelo de datos: las
    demás entidades (camadas, insumos, producción, ventas, análisis IA)
    cuelgan de ella vía `id_usuario`.
    """

    __tablename__ = "usuario"
    __table_args__ = (
        UniqueConstraint("correo_electronico", name="uq_usuario_correo"),
        CheckConstraint(
            "plan_suscripcion IN ('gratuito','premium')", name="chk_usuario_plan"
        ),
    )

    id_usuario: Mapped[int] = mapped_column(
        Integer, primary_key=True, autoincrement=True
    )
    nombre_completo: Mapped[str] = mapped_column(String(100), nullable=False)
    correo_electronico: Mapped[str] = mapped_column(
        String(100), nullable=False, unique=True, index=True
    )
    contrasena_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    telefono: Mapped[str | None] = mapped_column(String(20), nullable=True)
    plan_suscripcion: Mapped[str] = mapped_column(
        String(20), nullable=False, default="gratuito"
    )
    fecha_registro: Mapped[date] = mapped_column(Date, nullable=False)
    activo: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
