"""insumo descontinuado y notificacion de stock

Revision ID: e6f7a8b9c0d1
Revises: d5e6f7a8b9c0
Create Date: 2026-09-24 13:00:00.000000

Agrega a `insumo` la marca de descontinuado (distinta de la suspensión
temporal) y a `notificacion` la referencia al insumo y el stock que se
avisó (para no repetir el aviso hasta que cambie el stock).
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'e6f7a8b9c0d1'
down_revision: Union[str, Sequence[str], None] = 'd5e6f7a8b9c0'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    with op.batch_alter_table("insumo") as batch_op:
        batch_op.add_column(
            sa.Column(
                "descontinuado",
                sa.Boolean(),
                nullable=False,
                server_default="0",
            )
        )
        batch_op.create_check_constraint(
            "chk_insumo_descontinuado", "descontinuado IN (0,1)"
        )

    with op.batch_alter_table("notificacion") as batch_op:
        batch_op.add_column(sa.Column("id_insumo", sa.Integer(), nullable=True))
        batch_op.add_column(
            sa.Column("stock_referencia", sa.Numeric(10, 2), nullable=True)
        )
        batch_op.create_foreign_key(
            "fk_notificacion_insumo",
            "insumo",
            ["id_insumo"],
            ["id_insumo"],
            ondelete="RESTRICT",
            onupdate="CASCADE",
        )


def downgrade() -> None:
    """Downgrade schema."""
    with op.batch_alter_table("notificacion") as batch_op:
        batch_op.drop_constraint("fk_notificacion_insumo", type_="foreignkey")
        batch_op.drop_column("stock_referencia")
        batch_op.drop_column("id_insumo")

    with op.batch_alter_table("insumo") as batch_op:
        batch_op.drop_constraint("chk_insumo_descontinuado", type_="check")
        batch_op.drop_column("descontinuado")
