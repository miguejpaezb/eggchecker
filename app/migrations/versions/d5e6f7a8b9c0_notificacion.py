"""tabla notificacion

Revision ID: d5e6f7a8b9c0
Revises: c3d4e5f6a7b8
Create Date: 2026-09-24 12:00:00.000000

Crea la tabla `notificacion` para los avisos persistentes al avicultor,
como la camada que supera las 72 semanas de vida productiva.
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'd5e6f7a8b9c0'
down_revision: Union[str, Sequence[str], None] = 'c3d4e5f6a7b8'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.create_table(
        "notificacion",
        sa.Column("id_notificacion", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("id_usuario", sa.Integer(), nullable=False),
        sa.Column("id_camada", sa.Integer(), nullable=True),
        sa.Column("tipo", sa.String(length=40), nullable=False),
        sa.Column("titulo", sa.String(length=80), nullable=False),
        sa.Column("mensaje", sa.Text(), nullable=False),
        sa.Column("leida", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("eliminada", sa.Integer(), nullable=False, server_default="0"),
        sa.Column(
            "fecha_creacion",
            sa.DateTime(),
            nullable=False,
            server_default=sa.text("CURRENT_TIMESTAMP"),
        ),
        sa.ForeignKeyConstraint(
            ["id_usuario"],
            ["usuario.id_usuario"],
            name="fk_notificacion_usuario",
            ondelete="RESTRICT",
            onupdate="CASCADE",
        ),
        sa.ForeignKeyConstraint(
            ["id_camada"],
            ["camada.id_camada"],
            name="fk_notificacion_camada",
            ondelete="RESTRICT",
            onupdate="CASCADE",
        ),
        sa.CheckConstraint("leida IN (0,1)", name="chk_notificacion_leida"),
        sa.CheckConstraint("eliminada IN (0,1)", name="chk_notificacion_eliminada"),
        sa.UniqueConstraint(
            "id_usuario",
            "id_camada",
            "tipo",
            name="uq_notificacion_usuario_camada_tipo",
        ),
    )
    op.create_index(
        "idx_notificacion_usuario",
        "notificacion",
        ["id_usuario", "leida"],
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_index("idx_notificacion_usuario", table_name="notificacion")
    op.drop_table("notificacion")
