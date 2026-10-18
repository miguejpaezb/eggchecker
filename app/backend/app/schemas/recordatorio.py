from datetime import date, datetime
from decimal import Decimal

from app.models.recordatorio_sanitario import (
    MODO_CADA_DIAS,
    MODO_DIAS_SEMANA,
    MODOS,
)
from pydantic import BaseModel, Field, field_validator, model_validator

# Una fecha explícita por dosis: inicio + hasta 3 refuerzos.
_MAXIMO_FECHAS = 4


class RecordatorioRequest(BaseModel):
    """Regla de vacunación o suministro de vitaminas.

    `id_camada` nulo crea un recordatorio global para todas las camadas
    activas. Los campos que aplican dependen de `modo`:

    * `cada_dias`: `intervalo_dias` (cada cuántos días).
    * `dias_semana`: `dias_semana` (0 = lunes … 6 = domingo).
    * `fechas`: `fechas` (1 a 4 fechas exactas); `fecha_inicio` se toma de
      la primera fecha.
    """

    id_camada: int | None = None
    id_insumo: int
    modo: str
    intervalo_dias: int | None = Field(default=None, ge=1, le=365)
    dias_semana: list[int] | None = None
    fechas: list[date] | None = None
    fecha_inicio: date | None = None
    fecha_fin: date | None = None
    cantidad: Decimal = Field(gt=0, max_digits=10, decimal_places=2)
    activo: bool = True

    @field_validator("modo")
    @classmethod
    def _modo_valido(cls, valor: str) -> str:
        """Solo acepta los modos de programación soportados."""
        if valor not in MODOS:
            raise ValueError("El modo debe ser cada_dias, dias_semana o fechas")
        return valor

    @model_validator(mode="after")
    def _validar_segun_modo(self) -> "RecordatorioRequest":
        """Exige los campos del modo y descarta los de los demás modos."""
        if self.modo == MODO_CADA_DIAS:
            if self.intervalo_dias is None:
                raise ValueError("El modo cada_dias requiere intervalo_dias")
            self.dias_semana = None
            self.fechas = None
        elif self.modo == MODO_DIAS_SEMANA:
            if not self.dias_semana:
                raise ValueError("El modo dias_semana requiere dias_semana")
            if any(dia < 0 or dia > 6 for dia in self.dias_semana):
                raise ValueError("Los días de la semana van de 0 (lunes) a 6")
            self.dias_semana = sorted(set(self.dias_semana))
            self.intervalo_dias = None
            self.fechas = None
        else:
            if not self.fechas:
                raise ValueError("El modo fechas requiere al menos una fecha")
            if len(self.fechas) > _MAXIMO_FECHAS:
                raise ValueError(f"Se permiten hasta {_MAXIMO_FECHAS} fechas")
            fechas = sorted(set(self.fechas))
            if fechas[0] < date.today():
                raise ValueError("Las fechas del recordatorio no pueden ser pasadas")
            self.fechas = fechas
            self.fecha_inicio = fechas[0]
            self.intervalo_dias = None
            self.dias_semana = None

        if self.fecha_inicio is None:
            raise ValueError("La fecha de inicio es obligatoria")
        if self.fecha_fin is not None and self.fecha_fin < self.fecha_inicio:
            raise ValueError("La fecha de fin no puede ser anterior al inicio")
        return self


class RecordatorioResponse(BaseModel):
    """Recordatorio con los datos del insumo y su próxima aplicación."""

    id_recordatorio: int
    id_camada: int | None
    nombre_camada: str | None
    id_insumo: int
    nombre_insumo: str
    unidad_medida: str
    stock_actual: Decimal
    cantidad: Decimal
    modo: str
    intervalo_dias: int | None
    dias_semana: list[int] | None
    fechas: list[date] | None
    fecha_inicio: date
    fecha_fin: date | None
    activo: bool
    proxima_dosis: date | None
    dosis_pendientes: int
    fecha_creacion: datetime


class DosisResponse(BaseModel):
    """Aplicación programada de un recordatorio en una camada."""

    id_dosis: int
    id_recordatorio: int
    id_camada: int
    nombre_camada: str
    fecha_programada: date
    numero_dosis: int
    aplicada: bool
    fecha_aplicacion: datetime | None
    id_movimiento: int | None
    id_evento: int | None
