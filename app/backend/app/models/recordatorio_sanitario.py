from datetime import date, datetime
from decimal import Decimal

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    Date,
    DateTime,
    ForeignKey,
    Integer,
    Numeric,
    String,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base

# Formas de programar las aplicaciones de un recordatorio.
MODO_CADA_DIAS = "cada_dias"
MODO_DIAS_SEMANA = "dias_semana"
MODO_FECHAS = "fechas"
MODOS = (MODO_CADA_DIAS, MODO_DIAS_SEMANA, MODO_FECHAS)

# Categorías de insumo que se pueden asociar a un recordatorio sanitario.
CATEGORIAS_SANITARIAS = ("medicamento", "vitamina")

# Días de ocurrencias que se materializan por delante de hoy.
DIAS_HORIZONTE = 90


class RecordatorioSanitario(Base):
    """Regla de vacunación o suministro de vitaminas de una camada.

    Mapea la tabla `recordatorio_sanitario` del DDL. Si `id_camada` es
    nulo, el recordatorio es global y aplica a las camadas activas del
    usuario. `id_insumo` señala el medicamento o vitamina a usar: la dosis
    se expresa en la unidad de medida del insumo y su aplicación descuenta
    inventario a través de un movimiento de salida.

    Las ocurrencias concretas se guardan en `recordatorio_dosis`.
    """

    __tablename__ = "recordatorio_sanitario"
    __table_args__ = (
        CheckConstraint(
            "modo IN ('cada_dias','dias_semana','fechas')",
            name="chk_recordatorio_modo",
        ),
        CheckConstraint("cantidad > 0", name="chk_recordatorio_cantidad"),
        CheckConstraint(
            "intervalo_dias IS NULL OR intervalo_dias > 0",
            name="chk_recordatorio_intervalo",
        ),
        CheckConstraint(
            "fecha_fin IS NULL OR fecha_fin >= fecha_inicio",
            name="chk_recordatorio_fechas",
        ),
        CheckConstraint("activo IN (0,1)", name="chk_recordatorio_activo"),
    )

    id_recordatorio: Mapped[int] = mapped_column(
        Integer, primary_key=True, autoincrement=True
    )
    id_usuario: Mapped[int] = mapped_column(
        Integer, ForeignKey("usuario.id_usuario"), nullable=False
    )
    id_camada: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("camada.id_camada"), nullable=True
    )
    id_insumo: Mapped[int] = mapped_column(
        Integer, ForeignKey("insumo.id_insumo"), nullable=False
    )
    modo: Mapped[str] = mapped_column(String(12), nullable=False)
    # Cada cuántos días se repite (modo `cada_dias`).
    intervalo_dias: Mapped[int | None] = mapped_column(Integer, nullable=True)
    # Días de la semana separados por coma, 0 = lunes (modo `dias_semana`).
    dias_semana: Mapped[str | None] = mapped_column(String(13), nullable=True)
    # Fechas exactas en ISO separadas por coma (modo `fechas`).
    fechas_especificas: Mapped[str | None] = mapped_column(String(60), nullable=True)
    fecha_inicio: Mapped[date] = mapped_column(Date, nullable=False)
    fecha_fin: Mapped[date | None] = mapped_column(Date, nullable=True)
    cantidad: Mapped[Decimal] = mapped_column(Numeric(10, 2), nullable=False)
    activo: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    fecha_creacion: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, default=datetime.now
    )
