"""camada fecha de creacion

Revision ID: c3d4e5f6a7b8
Revises: b7f1a2c9d4e0
Create Date: 2026-09-24 11:00:00.000000

Agrega a `camada` la marca de creación, necesaria para la ventana de 24
horas que permite editar la cantidad inicial. Los registros existentes se
rellenan con su fecha de ingreso.
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'c3d4e5f6a7b8'
down_revision: Union[str, Sequence[str], None] = 'b7f1a2c9d4e0'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    with op.batch_alter_table("camada") as batch_op:
        batch_op.add_column(
            sa.Column(
                "fecha_creacion",
                sa.DateTime(),
                nullable=False,
                server_default=sa.text("CURRENT_TIMESTAMP"),
            )
        )
    op.execute("UPDATE camada SET fecha_creacion = fecha_ingreso")


def downgrade() -> None:
    """Downgrade schema."""
    with op.batch_alter_table("camada") as batch_op:
        batch_op.drop_column("fecha_creacion")
