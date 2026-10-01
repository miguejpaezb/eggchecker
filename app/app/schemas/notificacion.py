from datetime import datetime

from pydantic import BaseModel, ConfigDict


class NotificacionResponse(BaseModel):
    """Aviso persistente tal como se entrega al frontend."""

    id_notificacion: int
    id_camada: int | None
    id_insumo: int | None
    tipo: str
    titulo: str
    mensaje: str
    leida: bool
    fecha_creacion: datetime

    model_config = ConfigDict(from_attributes=True)
