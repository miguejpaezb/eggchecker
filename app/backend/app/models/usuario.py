from datetime import date, time

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    Date,
    Integer,
    String,
    Time,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base

# Frecuencias del recordatorio de reportes que el avicultor puede elegir.
FRECUENCIAS_RESUMEN = ("semanal", "quincenal", "mensual")

# Hora por defecto del analisis diario de produccion (5 de la manana).
HORA_PRODUCCION_DEFECTO = time(5, 0)


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
        CheckConstraint(
            "notif_resumen_frecuencia IN ('semanal','quincenal','mensual')",
            name="chk_usuario_resumen_frecuencia",
        ),
        CheckConstraint(
            "notif_resumen_dia BETWEEN 0 AND 6", name="chk_usuario_resumen_dia"
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
    nombre_granja: Mapped[str | None] = mapped_column(String(100), nullable=True)
    plan_suscripcion: Mapped[str] = mapped_column(
        String(20), nullable=False, default="gratuito"
    )
    fecha_registro: Mapped[date] = mapped_column(Date, nullable=False)
    activo: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    # Preferencias de alertas del avicultor; el envío real aún no existe.
    notif_camada_limite: Mapped[bool] = mapped_column(
        Boolean, nullable=False, default=True
    )
    notif_produccion_baja: Mapped[bool] = mapped_column(
        Boolean, nullable=False, default=True
    )
    notif_produccion_hora: Mapped[time] = mapped_column(
        Time, nullable=False, default=HORA_PRODUCCION_DEFECTO
    )
    notif_stock_bajo: Mapped[bool] = mapped_column(
        Boolean, nullable=False, default=True
    )
    notif_vacunacion: Mapped[bool] = mapped_column(
        Boolean, nullable=False, default=False
    )
    notif_resumen_semanal: Mapped[bool] = mapped_column(
        Boolean, nullable=False, default=True
    )
    notif_resumen_frecuencia: Mapped[str] = mapped_column(
        String(10), nullable=False, default="semanal"
    )
    # Día de la semana del recordatorio: 0 = lunes … 6 = domingo.
    notif_resumen_dia: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
