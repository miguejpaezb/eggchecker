"""perfil de usuario: granja y preferencias de notificacion

Revision ID: i0d1e2f3a4b5
Revises: h9c0d1e2f3a4
Create Date: 2026-09-29 12:00:00.000000

Agrega a `usuario` el nombre de la granja y las cuatro preferencias de
alertas que el avicultor controla desde la sección de Perfil. El envío
real de las notificaciones (correo/teléfono) aún no está implementado;
por ahora solo se persiste la preferencia.
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'i0d1e2f3a4b5'
down_revision: Union[str, Sequence[str], None] = 'h9c0d1e2f3a4'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

# SQLite recrea la tabla al alterarla; si una vista la referencia, el
# rebuild falla. Se retiran y recrean las tres vistas que unen `usuario`.
_VISTA_PRODUCCION = """
CREATE VIEW v_produccion_detallada AS
SELECT
    pd.id_produccion,
    u.nombre_completo          AS avicultor,
    c.nombre_camada,
    pd.fecha_recoleccion,
    pd.total_huevos,
    th.nombre_tipo             AS tipo_huevo,
    pdd.cantidad               AS cantidad_tipo
FROM  produccion_diaria  pd
JOIN  usuario            u   ON pd.id_usuario    = u.id_usuario
JOIN  camada             c   ON pd.id_camada     = c.id_camada
JOIN  produccion_detalle pdd ON pd.id_produccion = pdd.id_produccion
JOIN  tipo_huevo         th  ON pdd.id_tipo      = th.id_tipo
ORDER BY pd.fecha_recoleccion DESC, th.nombre_tipo ASC
"""

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


def _drop_vistas() -> None:
    """Elimina las vistas que referencian la tabla usuario."""
    op.execute("DROP VIEW IF EXISTS v_produccion_detallada")
    op.execute("DROP VIEW IF EXISTS v_alertas_insumo")
    op.execute("DROP VIEW IF EXISTS v_pedidos_pendientes")


def _crear_vistas() -> None:
    """Recrea las vistas que referencian la tabla usuario."""
    op.execute(_VISTA_PRODUCCION)
    op.execute(_VISTA_ALERTAS)
    op.execute(_VISTA_PEDIDOS)


def upgrade() -> None:
    """Upgrade schema."""
    _drop_vistas()
    with op.batch_alter_table("usuario") as batch_op:
        batch_op.add_column(sa.Column("nombre_granja", sa.String(100), nullable=True))
        batch_op.add_column(
            sa.Column(
                "notif_produccion_baja",
                sa.Boolean(),
                nullable=False,
                server_default=sa.text("1"),
            )
        )
        batch_op.add_column(
            sa.Column(
                "notif_stock_bajo",
                sa.Boolean(),
                nullable=False,
                server_default=sa.text("1"),
            )
        )
        batch_op.add_column(
            sa.Column(
                "notif_vacunacion",
                sa.Boolean(),
                nullable=False,
                server_default=sa.text("0"),
            )
        )
        batch_op.add_column(
            sa.Column(
                "notif_resumen_semanal",
                sa.Boolean(),
                nullable=False,
                server_default=sa.text("1"),
            )
        )
    _crear_vistas()


def downgrade() -> None:
    """Downgrade schema."""
    _drop_vistas()
    with op.batch_alter_table("usuario") as batch_op:
        batch_op.drop_column("notif_resumen_semanal")
        batch_op.drop_column("notif_vacunacion")
        batch_op.drop_column("notif_stock_bajo")
        batch_op.drop_column("notif_produccion_baja")
        batch_op.drop_column("nombre_granja")
    _crear_vistas()
