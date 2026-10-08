"""analisis ia: camada y detalle del diagnostico

Revision ID: j1e2f3a4b5c6
Revises: i0d1e2f3a4b5
Create Date: 2026-10-08 12:00:00.000000

Prepara `analisis_ia` para el módulo de análisis de huevos por foto
(RF-33 a RF-36, CU-06). Agrega la camada de origen (historial por
camada), el detalle estructurado del diagnóstico, el proveedor de IA y
la retroalimentación del avicultor. Si la tabla no existe (bases creadas
solo con migraciones), la crea completa.
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'j1e2f3a4b5c6'
down_revision: Union[str, Sequence[str], None] = 'i0d1e2f3a4b5'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

_CHK_CALIDAD = (
    "calidad_general IS NULL OR calidad_general IN ('buena','regular','mala')"
)
_CHK_PUNTAJE = "puntaje_calidad IS NULL OR puntaje_calidad BETWEEN 0 AND 100"


def _columnas_nuevas() -> list[sa.Column]:
    """Columnas que agrega esta migración."""
    return [
        sa.Column("calidad_general", sa.String(10), nullable=True),
        sa.Column("puntaje_calidad", sa.Integer(), nullable=True),
        sa.Column("apto_venta", sa.Boolean(), nullable=True),
        sa.Column("huevos_detectados", sa.Integer(), nullable=True),
        sa.Column("detalle_json", sa.Text(), nullable=True),
        sa.Column("proveedor_ia", sa.String(60), nullable=True),
        sa.Column("diagnostico_correcto", sa.Boolean(), nullable=True),
    ]


def upgrade() -> None:
    """Upgrade schema."""
    inspector = sa.inspect(op.get_bind())

    if not inspector.has_table("analisis_ia"):
        op.create_table(
            "analisis_ia",
            sa.Column("id_analisis", sa.Integer(), primary_key=True,
                      autoincrement=True),
            sa.Column("id_usuario", sa.Integer(),
                      sa.ForeignKey("usuario.id_usuario"), nullable=False),
            sa.Column("id_camada", sa.Integer(),
                      sa.ForeignKey("camada.id_camada", ondelete="SET NULL"),
                      nullable=True),
            sa.Column("fecha_analisis", sa.DateTime(), nullable=False,
                      server_default=sa.func.now()),
            sa.Column("imagen_url", sa.String(255), nullable=False),
            sa.Column("resultado_diagnostico", sa.Text(), nullable=False),
            sa.Column("recomendaciones", sa.Text(), nullable=True),
            *_columnas_nuevas(),
            sa.CheckConstraint(_CHK_CALIDAD, name="chk_analisis_calidad"),
            sa.CheckConstraint(_CHK_PUNTAJE, name="chk_analisis_puntaje"),
        )
        op.create_index("idx_analisis_usuario", "analisis_ia", ["id_usuario"])
        op.create_index("idx_analisis_camada", "analisis_ia", ["id_camada"])
        op.create_index("idx_analisis_fecha", "analisis_ia", ["fecha_analisis"])
        return

    existentes = {c["name"] for c in inspector.get_columns("analisis_ia")}
    with op.batch_alter_table("analisis_ia") as batch_op:
        if "id_camada" not in existentes:
            batch_op.add_column(sa.Column("id_camada", sa.Integer(), nullable=True))
            batch_op.create_foreign_key(
                "fk_analisis_camada", "camada", ["id_camada"], ["id_camada"],
                ondelete="SET NULL",
            )
            batch_op.create_index("idx_analisis_camada", ["id_camada"])
        for columna in _columnas_nuevas():
            if columna.name not in existentes:
                batch_op.add_column(columna)
        if "calidad_general" not in existentes:
            batch_op.create_check_constraint("chk_analisis_calidad", _CHK_CALIDAD)
            batch_op.create_check_constraint("chk_analisis_puntaje", _CHK_PUNTAJE)


def downgrade() -> None:
    """Downgrade schema."""
    with op.batch_alter_table("analisis_ia") as batch_op:
        batch_op.drop_constraint("chk_analisis_puntaje", type_="check")
        batch_op.drop_constraint("chk_analisis_calidad", type_="check")
        for columna in reversed(_columnas_nuevas()):
            batch_op.drop_column(columna.name)
        batch_op.drop_index("idx_analisis_camada")
        batch_op.drop_constraint("fk_analisis_camada", type_="foreignkey")
        batch_op.drop_column("id_camada")
