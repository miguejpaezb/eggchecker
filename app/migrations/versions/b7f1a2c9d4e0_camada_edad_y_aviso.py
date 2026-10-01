"""camada edad y aviso

Revision ID: b7f1a2c9d4e0
Revises: dc5b8cc494d5
Create Date: 2026-09-24 10:00:00.000000

Agrega a `camada` la edad manual en semanas (inicia en 16) y la fecha del
próximo aviso semanal de decisión cuando supera la vida productiva.
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'b7f1a2c9d4e0'
down_revision: Union[str, Sequence[str], None] = 'dc5b8cc494d5'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    with op.batch_alter_table("camada") as batch_op:
        batch_op.add_column(
            sa.Column(
                "edad_semanas",
                sa.Integer(),
                nullable=False,
                server_default="16",
            )
        )
        batch_op.add_column(sa.Column("fecha_proximo_aviso", sa.Date(), nullable=True))
        batch_op.create_check_constraint("chk_camada_edad", "edad_semanas >= 16")


def downgrade() -> None:
    """Downgrade schema."""
    with op.batch_alter_table("camada") as batch_op:
        batch_op.drop_constraint("chk_camada_edad", type_="check")
        batch_op.drop_column("fecha_proximo_aviso")
        batch_op.drop_column("edad_semanas")
