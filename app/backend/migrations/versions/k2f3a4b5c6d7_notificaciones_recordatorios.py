"""notificaciones de produccion, recordatorios y resumen

Revision ID: k2f3a4b5c6d7
Revises: j1e2f3a4b5c6
Create Date: 2026-10-18 09:00:00.000000

Prepara el esquema para los avisos nuevos:

* `usuario`: separa la preferencia de "camada al limite" (que era la que
  realmente usaba la app bajo el nombre `notif_produccion_baja`) de la
  alerta real de produccion baja, y agrega la hora del analisis diario y
  la frecuencia/dia del recordatorio de reportes.
* `notificacion`: quita la unicidad por (usuario, camada, tipo), que
  impedia avisar varias veces de la misma camada, y agrega la fecha a la
  que se refiere el aviso, el momento (previo/hoy) y el recordatorio.
* Crea `recordatorio_sanitario` (la regla) y `recordatorio_dosis` (cada
  aplicacion programada por camada).
"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision: str = "k2f3a4b5c6d7"
down_revision: Union[str, Sequence[str], None] = "j1e2f3a4b5c6"
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

_CHK_MOMENTO = "momento IS NULL OR momento IN ('previo','hoy')"
_CHK_FRECUENCIA = "notif_resumen_frecuencia IN ('semanal','quincenal','mensual')"
_CHK_DIA = "notif_resumen_dia BETWEEN 0 AND 6"


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


def _crear_recordatorios() -> None:
    """Crea las tablas de reglas y ocurrencias de recordatorios."""
    op.create_table(
        "recordatorio_sanitario",
        sa.Column(
            "id_recordatorio", sa.Integer(), primary_key=True, autoincrement=True
        ),
        sa.Column("id_usuario", sa.Integer(), nullable=False),
        sa.Column("id_camada", sa.Integer(), nullable=True),
        sa.Column("id_insumo", sa.Integer(), nullable=False),
        sa.Column("modo", sa.String(12), nullable=False),
        sa.Column("intervalo_dias", sa.Integer(), nullable=True),
        sa.Column("dias_semana", sa.String(13), nullable=True),
        sa.Column("fecha_inicio", sa.Date(), nullable=False),
        sa.Column("fecha_fin", sa.Date(), nullable=True),
        sa.Column("cantidad", sa.Numeric(10, 2), nullable=False),
        sa.Column("activo", sa.Boolean(), nullable=False, server_default=sa.text("1")),
        sa.Column(
            "fecha_creacion",
            sa.DateTime(),
            nullable=False,
            server_default=sa.func.now(),
        ),
        sa.ForeignKeyConstraint(
            ["id_usuario"],
            ["usuario.id_usuario"],
            name="fk_recordatorio_usuario",
            ondelete="RESTRICT",
            onupdate="CASCADE",
        ),
        sa.ForeignKeyConstraint(
            ["id_camada"],
            ["camada.id_camada"],
            name="fk_recordatorio_camada",
            ondelete="RESTRICT",
            onupdate="CASCADE",
        ),
        sa.ForeignKeyConstraint(
            ["id_insumo"],
            ["insumo.id_insumo"],
            name="fk_recordatorio_insumo",
            ondelete="RESTRICT",
            onupdate="CASCADE",
        ),
        sa.CheckConstraint(
            "modo IN ('cada_dias','dias_semana','fechas')",
            name="chk_recordatorio_modo",
        ),
        sa.CheckConstraint("cantidad > 0", name="chk_recordatorio_cantidad"),
        sa.CheckConstraint(
            "intervalo_dias IS NULL OR intervalo_dias > 0",
            name="chk_recordatorio_intervalo",
        ),
        sa.CheckConstraint(
            "fecha_fin IS NULL OR fecha_fin >= fecha_inicio",
            name="chk_recordatorio_fechas",
        ),
        sa.CheckConstraint("activo IN (0,1)", name="chk_recordatorio_activo"),
    )
    op.create_index(
        "idx_recordatorio_usuario", "recordatorio_sanitario", ["id_usuario"]
    )
    op.create_index("idx_recordatorio_camada", "recordatorio_sanitario", ["id_camada"])
    op.create_index("idx_recordatorio_insumo", "recordatorio_sanitario", ["id_insumo"])

    op.create_table(
        "recordatorio_dosis",
        sa.Column("id_dosis", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("id_recordatorio", sa.Integer(), nullable=False),
        sa.Column("id_camada", sa.Integer(), nullable=False),
        sa.Column("fecha_programada", sa.Date(), nullable=False),
        sa.Column("numero_dosis", sa.Integer(), nullable=False),
        sa.Column(
            "aplicada", sa.Boolean(), nullable=False, server_default=sa.text("0")
        ),
        sa.Column("fecha_aplicacion", sa.DateTime(), nullable=True),
        sa.Column("id_movimiento", sa.Integer(), nullable=True),
        sa.Column("id_evento", sa.Integer(), nullable=True),
        sa.ForeignKeyConstraint(
            ["id_recordatorio"],
            ["recordatorio_sanitario.id_recordatorio"],
            name="fk_dosis_recordatorio",
            ondelete="RESTRICT",
            onupdate="CASCADE",
        ),
        sa.ForeignKeyConstraint(
            ["id_camada"],
            ["camada.id_camada"],
            name="fk_dosis_camada",
            ondelete="RESTRICT",
            onupdate="CASCADE",
        ),
        sa.ForeignKeyConstraint(
            ["id_movimiento"],
            ["movimiento_insumo.id_movimiento"],
            name="fk_dosis_movimiento",
            ondelete="RESTRICT",
            onupdate="CASCADE",
        ),
        sa.ForeignKeyConstraint(
            ["id_evento"],
            ["evento_sanitario.id_evento"],
            name="fk_dosis_evento",
            ondelete="RESTRICT",
            onupdate="CASCADE",
        ),
        sa.UniqueConstraint(
            "id_recordatorio",
            "id_camada",
            "fecha_programada",
            name="uq_dosis_recordatorio_camada_fecha",
        ),
        sa.CheckConstraint("aplicada IN (0,1)", name="chk_dosis_aplicada"),
        sa.CheckConstraint("numero_dosis > 0", name="chk_dosis_numero"),
    )
    op.create_index(
        "idx_dosis_camada",
        "recordatorio_dosis",
        ["id_camada", "fecha_programada"],
    )
    op.create_index(
        "idx_dosis_fecha", "recordatorio_dosis", ["fecha_programada", "aplicada"]
    )


def upgrade() -> None:
    """Upgrade schema."""
    inspector = sa.inspect(op.get_bind())
    columnas_usuario = {c["name"] for c in inspector.get_columns("usuario")}
    tiene_limite = "notif_camada_limite" in columnas_usuario
    tenia_produccion = "notif_produccion_baja" in columnas_usuario

    _drop_vistas()
    # El rename y las columnas nuevas van en lotes separados: en SQLite el
    # batch reconstruye la tabla y mezclar ambos rompe el reordenamiento
    # de columnas.
    if not tiene_limite:
        with op.batch_alter_table("usuario") as batch_op:
            if tenia_produccion:
                # El nombre anterior describia "camada al limite", que es
                # lo que la app filtraba de verdad.
                batch_op.alter_column(
                    "notif_produccion_baja", new_column_name="notif_camada_limite"
                )
            else:
                batch_op.add_column(
                    sa.Column(
                        "notif_camada_limite",
                        sa.Boolean(),
                        nullable=False,
                        server_default=sa.text("1"),
                    )
                )

    with op.batch_alter_table("usuario") as batch_op:
        if not tenia_produccion or not tiene_limite:
            batch_op.add_column(
                sa.Column(
                    "notif_produccion_baja",
                    sa.Boolean(),
                    nullable=False,
                    server_default=sa.text("1"),
                )
            )
        if "notif_produccion_hora" not in columnas_usuario:
            batch_op.add_column(
                sa.Column(
                    "notif_produccion_hora",
                    sa.Time(),
                    nullable=False,
                    server_default=sa.text("'05:00'"),
                )
            )
        if "notif_resumen_frecuencia" not in columnas_usuario:
            batch_op.add_column(
                sa.Column(
                    "notif_resumen_frecuencia",
                    sa.String(10),
                    nullable=False,
                    server_default=sa.text("'semanal'"),
                )
            )
            batch_op.create_check_constraint(
                "chk_usuario_resumen_frecuencia", _CHK_FRECUENCIA
            )
        if "notif_resumen_dia" not in columnas_usuario:
            batch_op.add_column(
                sa.Column(
                    "notif_resumen_dia",
                    sa.Integer(),
                    nullable=False,
                    server_default=sa.text("0"),
                )
            )
            batch_op.create_check_constraint("chk_usuario_resumen_dia", _CHK_DIA)
    _crear_vistas()

    if not inspector.has_table("recordatorio_sanitario"):
        _crear_recordatorios()

    columnas_notif = {
        c["name"] for c in sa.inspect(op.get_bind()).get_columns("notificacion")
    }
    unicas = {
        u["name"]
        for u in sa.inspect(op.get_bind()).get_unique_constraints("notificacion")
    }
    indices = {i["name"] for i in sa.inspect(op.get_bind()).get_indexes("notificacion")}
    with op.batch_alter_table("notificacion") as batch_op:
        if "uq_notificacion_usuario_camada_tipo" in unicas:
            # Impedia repetir avisos de la misma camada en dias distintos.
            batch_op.drop_constraint(
                "uq_notificacion_usuario_camada_tipo", type_="unique"
            )
        if "fecha_referencia" not in columnas_notif:
            batch_op.add_column(sa.Column("fecha_referencia", sa.Date(), nullable=True))
        if "momento" not in columnas_notif:
            batch_op.add_column(sa.Column("momento", sa.String(10), nullable=True))
            batch_op.create_check_constraint("chk_notificacion_momento", _CHK_MOMENTO)
        if "id_recordatorio" not in columnas_notif:
            batch_op.add_column(
                sa.Column("id_recordatorio", sa.Integer(), nullable=True)
            )
            batch_op.create_foreign_key(
                "fk_notificacion_id_recordatorio",
                "recordatorio_sanitario",
                ["id_recordatorio"],
                ["id_recordatorio"],
                ondelete="RESTRICT",
                onupdate="CASCADE",
            )
        if "idx_notificacion_usuario_tipo_fecha" not in indices:
            batch_op.create_index(
                "idx_notificacion_usuario_tipo_fecha",
                ["id_usuario", "tipo", "fecha_referencia"],
            )
        if "idx_notificacion_recordatorio" not in indices:
            batch_op.create_index("idx_notificacion_recordatorio", ["id_recordatorio"])


def downgrade() -> None:
    """Downgrade schema."""
    inspector = sa.inspect(op.get_bind())
    # SQLite no conserva el nombre de las FK, así que la convención las
    # nombra al reflejar la tabla para poder quitarlas.
    convencion_fk = {"fk": "fk_%(table_name)s_%(column_0_name)s"}
    with op.batch_alter_table(
        "notificacion", naming_convention=convencion_fk
    ) as batch_op:
        batch_op.drop_index("idx_notificacion_recordatorio")
        batch_op.drop_index("idx_notificacion_usuario_tipo_fecha")
        batch_op.drop_constraint("fk_notificacion_id_recordatorio", type_="foreignkey")
        batch_op.drop_column("id_recordatorio")
        batch_op.drop_constraint("chk_notificacion_momento", type_="check")
        batch_op.drop_column("momento")
        batch_op.drop_column("fecha_referencia")
        batch_op.create_unique_constraint(
            "uq_notificacion_usuario_camada_tipo",
            ["id_usuario", "id_camada", "tipo"],
        )

    if inspector.has_table("recordatorio_dosis"):
        op.drop_table("recordatorio_dosis")
    if inspector.has_table("recordatorio_sanitario"):
        op.drop_table("recordatorio_sanitario")

    _drop_vistas()
    with op.batch_alter_table("usuario") as batch_op:
        batch_op.drop_constraint("chk_usuario_resumen_dia", type_="check")
        batch_op.drop_constraint("chk_usuario_resumen_frecuencia", type_="check")
        batch_op.drop_column("notif_resumen_dia")
        batch_op.drop_column("notif_resumen_frecuencia")
        batch_op.drop_column("notif_produccion_hora")
        batch_op.drop_column("notif_produccion_baja")
    # El rename va en su propio lote para no mezclarlo con los drop_column.
    with op.batch_alter_table("usuario") as batch_op:
        batch_op.alter_column(
            "notif_camada_limite", new_column_name="notif_produccion_baja"
        )
    _crear_vistas()
