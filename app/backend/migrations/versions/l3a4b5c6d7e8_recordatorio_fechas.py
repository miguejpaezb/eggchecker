"""recordatorios con fechas exactas

Revision ID: l3a4b5c6d7e8
Revises: k2f3a4b5c6d7
Create Date: 2026-10-18 10:00:00.000000

Guarda las fechas explícitas de un recordatorio en modo `fechas`. Sin esta
columna el listado de dosis sería la única copia del plan y no se podrían
recrear las ocurrencias de una camada nueva en un recordatorio global.
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'l3a4b5c6d7e8'
down_revision: Union[str, Sequence[str], None] = 'k2f3a4b5c6d7'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    inspector = sa.inspect(op.get_bind())
    columnas = {c["name"] for c in inspector.get_columns("recordatorio_sanitario")}
    if "fechas_especificas" not in columnas:
        with op.batch_alter_table("recordatorio_sanitario") as batch_op:
            batch_op.add_column(
                sa.Column("fechas_especificas", sa.String(60), nullable=True)
            )


def downgrade() -> None:
    """Downgrade schema."""
    with op.batch_alter_table("recordatorio_sanitario") as batch_op:
        batch_op.drop_column("fechas_especificas")
