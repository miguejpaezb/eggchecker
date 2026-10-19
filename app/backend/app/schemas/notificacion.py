from datetime import date, datetime

from pydantic import BaseModel, ConfigDict


class NotificacionResponse(BaseModel):
    """Aviso persistente tal como se entrega al frontend."""

    id_notificacion: int
    id_camada: int | None
    id_insumo: int | None
    id_recordatorio: int | None
    tipo: str
    titulo: str
    mensaje: str
    leida: bool
    fecha_referencia: date | None
    momento: str | None
    fecha_creacion: datetime

    model_config = ConfigDict(from_attributes=True)
