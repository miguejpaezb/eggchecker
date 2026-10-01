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

# SQLite no puede alterar columnas in situ: Alembic recrea la tabla y, si
# una vista la referencia, el rebuild falla. Se retira y recrea alrededor.
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
    op.execute("DROP VIEW IF EXISTS v_pedidos_pendientes")
    with op.batch_alter_table("cliente") as batch_op:
        batch_op.drop_column("longitud")
        batch_op.drop_column("latitud")
    op.execute(_VISTA_PEDIDOS)


def downgrade() -> None:
    """Downgrade schema."""
    op.execute("DROP VIEW IF EXISTS v_pedidos_pendientes")
    with op.batch_alter_table("cliente") as batch_op:
        batch_op.add_column(
            sa.Column("latitud", sa.Numeric(10, 7), nullable=True)
        )
        batch_op.add_column(
            sa.Column("longitud", sa.Numeric(10, 7), nullable=True)
        )
    op.execute(_VISTA_PEDIDOS)
