"""costo unitario de insumos

Revision ID: h9c0d1e2f3a4
Revises: g8b9c0d1e2f3
Create Date: 2026-09-28 12:00:00.000000

Agrega `costo_unitario` a `insumo` (costo de referencia vigente por
unidad) y a `movimiento_insumo` (costo congelado al momento del
movimiento). Es el insumo de datos del "costo de insumos" de los
reportes de rentabilidad (RF-37).
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'h9c0d1e2f3a4'
down_revision: Union[str, Sequence[str], None] = 'g8b9c0d1e2f3'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

# SQLite no puede alterar CHECK/columnas in situ: Alembic recrea la tabla
# y, si una vista la referencia, el rebuild falla. Se retira y recrea.
_VISTA_ALERTAS = """
CREATE VIEW v_alertas_insumo AS
SELECT
    i.id_insumo,
    u.nombre_completo  AS avicultor,
    ci.nombre_categ    AS categoria,
    i.nombre_insumo,
    i.stock_actual,
    i.umbral_minimo,
    (i.umbral_minimo - i.stock_actual) AS deficit
FROM  insumo           i
JOIN  usuario          u  ON i.id_usuario   = u.id_usuario
JOIN  categoria_insumo ci ON i.id_categoria = ci.id_categoria
WHERE i.stock_actual < i.umbral_minimo
  AND i.activo = 1
ORDER BY deficit DESC
"""


def upgrade() -> None:
    """Upgrade schema."""
    op.execute("DROP VIEW IF EXISTS v_alertas_insumo")
    with op.batch_alter_table("insumo") as batch_op:
        batch_op.add_column(
            sa.Column(
                "costo_unitario",
                sa.Numeric(12, 2),
                nullable=False,
                server_default="0.00",
            )
        )
        batch_op.create_check_constraint(
            "chk_insumo_costo", "costo_unitario >= 0"
        )
    with op.batch_alter_table("movimiento_insumo") as batch_op:
        batch_op.add_column(
            sa.Column("costo_unitario", sa.Numeric(12, 2), nullable=True)
        )
        batch_op.create_check_constraint(
            "chk_movimiento_costo",
            "costo_unitario IS NULL OR costo_unitario >= 0",
        )
    op.execute(_VISTA_ALERTAS)


def downgrade() -> None:
    """Downgrade schema."""
    op.execute("DROP VIEW IF EXISTS v_alertas_insumo")
    with op.batch_alter_table("movimiento_insumo") as batch_op:
        batch_op.drop_constraint("chk_movimiento_costo", type_="check")
        batch_op.drop_column("costo_unitario")
    with op.batch_alter_table("insumo") as batch_op:
        batch_op.drop_constraint("chk_insumo_costo", type_="check")
        batch_op.drop_column("costo_unitario")
    op.execute(_VISTA_ALERTAS)
