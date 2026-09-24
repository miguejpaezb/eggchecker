from datetime import date, datetime, timedelta

from sqlalchemy import CheckConstraint, Date, DateTime, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base

# Edad con la que ingresa toda camada nueva: 16 semanas de vida.
EDAD_INICIAL_SEMANAS = 16

# 72 semanas de vida productiva de una ponedora (504 días). A partir de esta
# edad la camada deja de retirarse sola: pasa a pedir decisión semanal.
DIAS_VIDA_PRODUCTIVA = 504

# Edad, en semanas, a partir de la cual se solicita decisión al avicultor.
EDAD_DECISION_SEMANAS = DIAS_VIDA_PRODUCTIVA // 7

# Días entre avisos semanales de decisión una vez superada la vida productiva.
DIAS_AVISO = 7

# Ventana desde la creación en la que se permite editar la cantidad inicial.
HORAS_EDICION_INICIAL = 24


class Camada(Base):
    """Lote de aves ponedoras que un avicultor mantiene en producción.

    Mapea la tabla `camada` del DDL. El ciclo de vida de una camada no
    usa borrado físico (las FKs son RESTRICT); una camada se cierra al
    pasar su estado a 'retirada'. La edad es manual (`edad_semanas`) y
    arranca en 16 semanas.
    """

    __tablename__ = "camada"
    __table_args__ = (
        CheckConstraint("estado IN ('activa','retirada')", name="chk_camada_estado"),
        CheckConstraint("cantidad_inicial > 0", name="chk_camada_cant_ini"),
        CheckConstraint("cantidad_actual >= 0", name="chk_camada_cant_act"),
        CheckConstraint(
            "cantidad_actual <= cantidad_inicial", name="chk_camada_cant_lte"
        ),
        CheckConstraint(
            f"edad_semanas >= {EDAD_INICIAL_SEMANAS}", name="chk_camada_edad"
        ),
    )

    id_camada: Mapped[int] = mapped_column(
        Integer, primary_key=True, autoincrement=True
    )
    id_usuario: Mapped[int] = mapped_column(
        Integer, ForeignKey("usuario.id_usuario"), nullable=False
    )
    nombre_camada: Mapped[str] = mapped_column(String(60), nullable=False)
    fecha_ingreso: Mapped[date] = mapped_column(Date, nullable=False)
    cantidad_inicial: Mapped[int] = mapped_column(Integer, nullable=False)
    cantidad_actual: Mapped[int] = mapped_column(Integer, nullable=False)
    estado: Mapped[str] = mapped_column(String(20), nullable=False, default="activa")
    edad_semanas: Mapped[int] = mapped_column(
        Integer, nullable=False, default=EDAD_INICIAL_SEMANAS
    )
    fecha_proximo_aviso: Mapped[date | None] = mapped_column(Date, nullable=True)
    fecha_creacion: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, default=datetime.now
    )

    @property
    def requiere_decision(self) -> bool:
        """Indica si la camada pide decisión ('seguir activa' o 'descartar').

        Una camada activa que alcanzó la vida productiva pide decisión
        mientras no tenga un aviso agendado a futuro. Al posponer se agenda
        `fecha_proximo_aviso` a 7 días; vencido, vuelve a requerir decisión.
        """
        if self.estado != "activa":
            return False
        if self.edad_semanas < EDAD_DECISION_SEMANAS:
            return False
        if self.fecha_proximo_aviso is None:
            return True
        return self.fecha_proximo_aviso <= date.today()

    @property
    def puede_editar_inicial(self) -> bool:
        """Indica si aún se puede editar la cantidad inicial (24 horas).

        Pasada la ventana de creación, la cantidad inicial queda bloqueada;
        el nombre siempre se puede editar mientras la camada esté activa.
        """
        return datetime.now() - self.fecha_creacion <= timedelta(
            hours=HORAS_EDICION_INICIAL
        )
