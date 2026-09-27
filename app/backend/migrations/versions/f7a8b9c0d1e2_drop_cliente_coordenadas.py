"""quitar coordenadas del cliente

Revision ID: f7a8b9c0d1e2
Revises: e6f7a8b9c0d1
Create Date: 2026-09-26 10:00:00.000000

Elimina `latitud` y `longitud` de `cliente`: la ubicación geográfica del
cliente se descartó para esta versión, así que no se persiste.
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'f7a8b9c0d1e2'
down_revision: Union[str, Sequence[str], None] = 'e6f7a8b9c0d1'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    with op.batch_alter_table("cliente") as batch_op:
        batch_op.drop_column("longitud")
        batch_op.drop_column("latitud")


def downgrade() -> None:
    """Downgrade schema."""
    with op.batch_alter_table("cliente") as batch_op:
        batch_op.add_column(
            sa.Column("latitud", sa.Numeric(10, 7), nullable=True)
        )
        batch_op.add_column(
            sa.Column("longitud", sa.Numeric(10, 7), nullable=True)
        )
