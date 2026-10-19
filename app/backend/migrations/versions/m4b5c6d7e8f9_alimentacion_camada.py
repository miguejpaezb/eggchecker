"""alimentacion por camada

Revision ID: m4b5c6d7e8f9
Revises: l3a4b5c6d7e8
Create Date: 2026-10-19 09:00:00.000000

Crea `alimentacion_camada` para registrar qué alimento (insumos de la
categoría `alimento`) consumió cada camada, con fecha y hora. Cada fila
se acompaña de un movimiento de salida del insumo que descuenta el stock.
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision: str = "m4b5c6d7e8f9"
down_revision: Union[str, Sequence[str], None] = "l3a4b5c6d7e8"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    inspector = sa.inspect(op.get_bind())
    if inspector.has_table("alimentacion_camada"):
        return

    op.create_table(
        "alimentacion_camada",
        sa.Column(
            "id_alimentacion",
            sa.Integer(),
            primary_key=True,
            autoincrement=True,
        ),
        sa.Column("id_camada", sa.Integer(), nullable=False),
        sa.Column("id_insumo", sa.Integer(), nullable=False),
        sa.Column("cantidad", sa.Numeric(10, 2), nullable=False),
        sa.Column(
            "fecha_hora",
            sa.DateTime(),
            nullable=False,
            server_default=sa.func.now(),
        ),
        sa.Column("observaciones", sa.Text(), nullable=True),
        sa.ForeignKeyConstraint(
            ["id_camada"],
            ["camada.id_camada"],
            name="fk_alimentacion_camada",
            ondelete="RESTRICT",
            onupdate="CASCADE",
        ),
        sa.ForeignKeyConstraint(
            ["id_insumo"],
            ["insumo.id_insumo"],
            name="fk_alimentacion_insumo",
            ondelete="RESTRICT",
            onupdate="CASCADE",
        ),
        sa.CheckConstraint("cantidad > 0", name="chk_alimentacion_cantidad"),
    )
    op.create_index("idx_alimentacion_camada", "alimentacion_camada", ["id_camada"])
    op.create_index("idx_alimentacion_insumo", "alimentacion_camada", ["id_insumo"])
    op.create_index("idx_alimentacion_fecha", "alimentacion_camada", ["fecha_hora"])


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_table("alimentacion_camada")
