"""stock de produccion y estados de pedido

Revision ID: g8b9c0d1e2f3
Revises: f7a8b9c0d1e2
Create Date: 2026-09-26 12:00:00.000000

Agrega la tabla `stock_produccion` (existencias de huevos por usuario y
tipo, con su valor por unidad) y amplía los estados de `pedido` a
'pendiente', 'enviado', 'recibido' y 'cancelado'. Los pedidos con el
estado anterior 'entregado' pasan a 'recibido'.
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'g8b9c0d1e2f3'
down_revision: Union[str, Sequence[str], None] = 'f7a8b9c0d1e2'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

# SQLite no puede alterar CHECK/columnas in situ: Alembic recrea la tabla
# y, si una vista la referencia, el rebuild falla. Se retira y recrea.
_VISTA_PEDIDOS = """
CREATE VIEW v_pedidos_pendientes AS
SELECT
    p.id_pedido,
    u.nombre_completo  AS avicultor,
    cl.nombre_cliente  AS cliente,
    cl.telefono        AS telefono_cliente,
    p.fecha_pedido,
    p.valor_total,
    COUNT(dp.id_detalle_pedido) AS num_items
FROM  pedido         p
JOIN  usuario        u  ON p.id_usuario = u.id_usuario
JOIN  cliente        cl ON p.id_cliente = cl.id_cliente
JOIN  detalle_pedido dp ON p.id_pedido  = dp.id_pedido
WHERE p.estado_pedido = 'pendiente'
GROUP BY p.id_pedido, u.nombre_completo, cl.nombre_cliente,
         cl.telefono, p.fecha_pedido, p.valor_total
ORDER BY p.fecha_pedido ASC
"""


def upgrade() -> None:
    """Upgrade schema."""
    op.create_table(
        "stock_produccion",
        sa.Column("id_stock", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("id_usuario", sa.Integer(), nullable=False),
        sa.Column("id_tipo", sa.Integer(), nullable=False),
        sa.Column(
            "cantidad_actual", sa.Integer(), nullable=False, server_default="0"
        ),
        sa.Column(
            "valor_unidad",
            sa.Numeric(12, 2),
            nullable=False,
            server_default="0.00",
        ),
        sa.Column(
            "ultima_modificacion",
            sa.DateTime(),
            nullable=False,
            server_default=sa.text("CURRENT_TIMESTAMP"),
        ),
        sa.ForeignKeyConstraint(
            ["id_usuario"],
            ["usuario.id_usuario"],
            name="fk_stock_usuario",
            ondelete="RESTRICT",
            onupdate="CASCADE",
        ),
        sa.ForeignKeyConstraint(
            ["id_tipo"],
            ["tipo_huevo.id_tipo"],
            name="fk_stock_tipo",
            ondelete="RESTRICT",
            onupdate="CASCADE",
        ),
        sa.UniqueConstraint("id_usuario", "id_tipo", name="uq_stock_usuario_tipo"),
        sa.CheckConstraint("cantidad_actual >= 0", name="chk_stock_cantidad"),
        sa.CheckConstraint("valor_unidad >= 0", name="chk_stock_valor"),
    )
    op.create_index("idx_stock_usuario", "stock_produccion", ["id_usuario"])
    op.create_index("idx_stock_tipo", "stock_produccion", ["id_tipo"])

    # El estado antiguo 'entregado' no entra en el CHECK nuevo, así que
    # primero se retira el constraint, luego se migran los datos y por
    # último se agrega el constraint definitivo.
    op.execute("DROP VIEW IF EXISTS v_pedidos_pendientes")
    with op.batch_alter_table("pedido") as batch_op:
        batch_op.drop_constraint("chk_pedido_estado", type_="check")
    op.execute(
        "UPDATE pedido SET estado_pedido = 'recibido' "
        "WHERE estado_pedido = 'entregado'"
    )
    with op.batch_alter_table("pedido") as batch_op:
        batch_op.create_check_constraint(
            "chk_pedido_estado",
            "estado_pedido IN ('pendiente','enviado','recibido','cancelado')",
        )
    op.execute(_VISTA_PEDIDOS)


def downgrade() -> None:
    """Downgrade schema."""
    op.execute("DROP VIEW IF EXISTS v_pedidos_pendientes")
    with op.batch_alter_table("pedido") as batch_op:
        batch_op.drop_constraint("chk_pedido_estado", type_="check")
    op.execute(
        "UPDATE pedido SET estado_pedido = 'entregado' "
        "WHERE estado_pedido = 'recibido'"
    )
    with op.batch_alter_table("pedido") as batch_op:
        batch_op.create_check_constraint(
            "chk_pedido_estado",
            "estado_pedido IN ('pendiente','entregado','cancelado')",
        )
    op.execute(_VISTA_PEDIDOS)

    op.drop_index("idx_stock_tipo", table_name="stock_produccion")
    op.drop_index("idx_stock_usuario", table_name="stock_produccion")
    op.drop_table("stock_produccion")
