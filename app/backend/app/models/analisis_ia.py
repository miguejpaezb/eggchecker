from datetime import datetime

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    DateTime,
    ForeignKey,
    Integer,
    String,
    Text,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class AnalisisIA(Base):
    """Resultado de un análisis de calidad de huevos por fotografía.

    Mapea la tabla `analisis_ia` del DDL (RF-33 a RF-36, CU-06). Cada fila
    guarda la imagen analizada, el diagnóstico de la IA y las
    recomendaciones personalizadas. `id_camada` es opcional: si se indica,
    el análisis aparece en el historial de esa camada.

    Las filas del seed solo traen las cinco columnas originales; por eso
    las columnas agregadas para el detalle del diagnóstico son nulas.
    """

    __tablename__ = "analisis_ia"
    __table_args__ = (
        CheckConstraint(
            "calidad_general IS NULL OR "
            "calidad_general IN ('buena','regular','mala')",
            name="chk_analisis_calidad",
        ),
        CheckConstraint(
            "puntaje_calidad IS NULL OR puntaje_calidad BETWEEN 0 AND 100",
            name="chk_analisis_puntaje",
        ),
    )

    id_analisis: Mapped[int] = mapped_column(
        Integer, primary_key=True, autoincrement=True
    )
    id_usuario: Mapped[int] = mapped_column(
        Integer, ForeignKey("usuario.id_usuario"), nullable=False, index=True
    )
    id_camada: Mapped[int | None] = mapped_column(
        Integer,
        ForeignKey("camada.id_camada", ondelete="SET NULL"),
        nullable=True,
        index=True,
    )
    fecha_analisis: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, default=datetime.now
    )
    # Ruta relativa del archivo dentro de IA_UPLOAD_DIR, o URL externa en
    # los registros históricos del seed.
    imagen_url: Mapped[str] = mapped_column(String(255), nullable=False)
    resultado_diagnostico: Mapped[str] = mapped_column(Text, nullable=False)
    # Recomendaciones separadas por saltos de línea.
    recomendaciones: Mapped[str | None] = mapped_column(Text, nullable=True)
    calidad_general: Mapped[str | None] = mapped_column(String(10), nullable=True)
    puntaje_calidad: Mapped[int | None] = mapped_column(Integer, nullable=True)
    apto_venta: Mapped[bool | None] = mapped_column(Boolean, nullable=True)
    huevos_detectados: Mapped[int | None] = mapped_column(Integer, nullable=True)
    # Diagnóstico completo de la IA (anomalías, distribución, confianza).
    detalle_json: Mapped[str | None] = mapped_column(Text, nullable=True)
    proveedor_ia: Mapped[str | None] = mapped_column(String(60), nullable=True)
    # Retroalimentación del avicultor: ¿el diagnóstico fue correcto?
    # Sirve para construir un dataset propio y entrenar un modelo después.
    diagnostico_correcto: Mapped[bool | None] = mapped_column(Boolean, nullable=True)
