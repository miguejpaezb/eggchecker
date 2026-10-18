"""Carga un usuario demo con tres meses de datos coherentes.

El seed oficial (`db/eggchecker_seed.sql`) se mantiene intacto para las
pruebas automáticas. Este script crea una base de demostración con un
único usuario (`demo@eggchecker.click`) que ya lleva tres meses usando la
app: tres camadas en producción, recolección diaria, ventas, insumos con
consumo, vacunaciones, vitaminas, recordatorios y notificaciones.

Uso (desde app/backend):

    python scripts/user_demo.py              # recrea la base solo con el demo
    python scripts/user_demo.py --conservar  # agrega el demo a la base actual

Al terminar imprime un resumen y verifica la coherencia de los datos
(stock, mortalidad, totales de producción y ventas).
"""

import argparse
import sqlite3
import sys
from datetime import date, timedelta
from pathlib import Path

_BASE_DIR = Path(__file__).resolve().parents[1]
if str(_BASE_DIR) not in sys.path:
    sys.path.insert(0, str(_BASE_DIR))

# El hash debe ser el mismo que usa la API para poder iniciar sesión.
from app.core.security import obtener_hash, verificar_hash  # noqa: E402

_DDL_PATH = _BASE_DIR / "db" / "eggchecker_ddl.sql"
_DB_POR_DEFECTO = _BASE_DIR / "eggchecker.db"

CORREO = "demo@eggchecker.click"
CLAVE = "Test1234#"
NOMBRE = "Demo"
GRANJA = "Granja de Pruebas"

HOY = date.today()

# Precios de referencia por tipo de huevo (los mismos del DDL de stock).
PRECIOS = {"AA": 400.0, "A": 280.0, "B": 160.0, "No_apto": 100.0}

VISTAS = (
    "v_produccion_detallada",
    "v_alertas_insumo",
    "v_pedidos_pendientes",
)


# ─────────────────────────────────────────────────────────────────────
# Utilidades de fechas
# ─────────────────────────────────────────────────────────────────────


def _fecha(dias_atras: int) -> str:
    """Fecha ISO de hace N días (negativo = futuro)."""
    return (HOY - timedelta(days=dias_atras)).isoformat()


def _sello(dias_atras: int, hora: int = 8, minuto: int = 0) -> str:
    """Sello de fecha y hora de hace N días."""
    return f"{_fecha(dias_atras)} {hora:02d}:{minuto:02d}:00"


# ─────────────────────────────────────────────────────────────────────
# Esquema y catálogos
# ─────────────────────────────────────────────────────────────────────


def _vaciar(conn: sqlite3.Connection) -> None:
    """Elimina tablas y vistas para recrear el esquema desde el DDL."""
    conn.execute("PRAGMA foreign_keys = OFF")
    objetos = conn.execute(
        "SELECT type, name FROM sqlite_master WHERE type IN ('table','view')"
    ).fetchall()
    for tipo, nombre in objetos:
        if not nombre.startswith("sqlite_"):
            conn.execute(f"DROP {tipo.upper()} IF EXISTS {nombre}")
    conn.execute("PRAGMA foreign_keys = ON")


def _crear_esquema(conn: sqlite3.Connection) -> None:
    """Ejecuta el DDL oficial de EggChecker."""
    conn.executescript(_DDL_PATH.read_text(encoding="utf-8"))


def _cargar_catalogos(conn: sqlite3.Connection) -> tuple[dict, dict]:
    """Asegura los catálogos fijos y devuelve sus ids por nombre."""
    tipos = {
        "AA": ("Huevo de primera calidad",),
        "A": ("Huevo de buena calidad",),
        "B": ("Huevo con defectos visibles",),
        "No_apto": ("Huevo roto o contaminado",),
    }
    for nombre, (descripcion,) in tipos.items():
        conn.execute(
            "INSERT OR IGNORE INTO tipo_huevo (nombre_tipo, descripcion) "
            "VALUES (?, ?)",
            (nombre, descripcion),
        )

    categorias = {
        "alimento": "Concentrados, maíz y demás alimentos para las aves",
        "medicamento": "Antibióticos y medicamentos veterinarios",
        "vitamina": "Suplementos vitamínicos y minerales",
        "herramienta": "Equipos y utensilios del galpón",
        "limpieza": "Desinfectantes y materiales de aseo",
        "empaque": "Cartones y embalajes",
    }
    for nombre, descripcion in categorias.items():
        conn.execute(
            "INSERT OR IGNORE INTO categoria_insumo (nombre_categ, descripcion) "
            "VALUES (?, ?)",
            (nombre, descripcion),
        )

    ids_tipos = {
        fila[1]: fila[0]
        for fila in conn.execute("SELECT id_tipo, nombre_tipo FROM tipo_huevo")
    }
    ids_categorias = {
        fila[1]: fila[0]
        for fila in conn.execute(
            "SELECT id_categoria, nombre_categ FROM categoria_insumo"
        )
    }
    return ids_tipos, ids_categorias


# ─────────────────────────────────────────────────────────────────────
# Usuario, camadas e insumos
# ─────────────────────────────────────────────────────────────────────


def _insertar_usuario(conn: sqlite3.Connection) -> int:
    """Crea el avicultor demo con sus preferencias de alertas."""
    cursor = conn.execute(
        """
        INSERT INTO usuario (
            nombre_completo, correo_electronico, contrasena_hash, telefono,
            nombre_granja, plan_suscripcion, fecha_registro, activo,
            notif_camada_limite, notif_produccion_baja, notif_produccion_hora,
            notif_stock_bajo, notif_vacunacion, notif_resumen_semanal,
            notif_resumen_frecuencia, notif_resumen_dia
        ) VALUES (?, ?, ?, ?, ?, 'premium', ?, 1, 1, 1, '05:00', 1, 1, 1,
                  'semanal', 0)
        """,
        (
            NOMBRE,
            CORREO,
            obtener_hash(CLAVE),
            "3001234567",
            GRANJA,
            _fecha(90),
        ),
    )
    return int(cursor.lastrowid)


def _insertar_camadas(conn: sqlite3.Connection, id_usuario: int) -> dict:
    """Tres camadas activas con tres meses de vida en la app."""
    datos = [
        # nombre, días desde el ingreso, aves iniciales, edad en semanas
        ("Galpón Norte", 90, 500, 29),
        ("Galpón Sur", 85, 420, 28),
        ("Galpón Centro", 80, 380, 28),
    ]
    camadas = {}
    for nombre, dias, aves, edad in datos:
        ingreso = _fecha(dias)
        cursor = conn.execute(
            """
            INSERT INTO camada (
                id_usuario, nombre_camada, fecha_ingreso, cantidad_inicial,
                cantidad_actual, estado, edad_semanas, fecha_creacion
            ) VALUES (?, ?, ?, ?, ?, 'activa', ?, ?)
            """,
            (
                id_usuario,
                nombre,
                ingreso,
                aves,
                aves,
                edad,
                f"{ingreso} 00:00:00",
            ),
        )
        camadas[nombre] = {
            "id": int(cursor.lastrowid),
            "inicial": aves,
            "actual": aves,
        }
    return camadas


INSUMOS = [
    {
        "categoria": "alimento",
        "nombre": "Concentrado Ponedora 18%",
        "unidad": "kg",
        "umbral": 100,
        "costo": 2400.0,
        "entradas": [(88, 400), (55, 400), (20, 400)],
        "salidas": [
            (84 - 7 * i, 120, "Consumo semanal de las 3 camadas") for i in range(8)
        ],
    },
    {
        "categoria": "alimento",
        "nombre": "Maíz molido",
        "unidad": "kg",
        "umbral": 80,
        "costo": 1600.0,
        "entradas": [(85, 250), (40, 250)],
        "salidas": [(84 - 7 * i, 40, "Mezcla de ración diaria") for i in range(8)],
    },
    {
        "categoria": "medicamento",
        "nombre": "Newcastle La Sota",
        "unidad": "ml",
        "umbral": 15,
        "costo": 320.0,
        "entradas": [(89, 80)],
        "salidas": [],
    },
    {
        "categoria": "vitamina",
        "nombre": "Vitamina AD3E",
        "unidad": "ml",
        "umbral": 5,
        "costo": 350.0,
        "entradas": [(89, 20)],
        "salidas": [],
    },
    {
        "categoria": "limpieza",
        "nombre": "Amonio cuaternario 5L",
        "unidad": "litro",
        "umbral": 3,
        "costo": 18000.0,
        "entradas": [(86, 6)],
        "salidas": [
            (70, 1.5, "Desinfección de bebederos"),
            (56, 1.5, "Limpieza de comederos"),
            (42, 1.5, "Desinfección del galpón"),
            (21, 1.5, "Limpieza general"),
        ],
    },
    {
        "categoria": "empaque",
        "nombre": "Cartones x30 unidades",
        "unidad": "unidad",
        "umbral": 60,
        "costo": 800.0,
        "entradas": [(88, 300), (45, 200)],
        "salidas": [(84 - 7 * i, 60, "Empaque de pedidos") for i in range(6)],
    },
    {
        "categoria": "medicamento",
        "nombre": "Oxitetraciclina 20%",
        "unidad": "ml",
        "umbral": 10,
        "costo": 180.0,
        "entradas": [(87, 40)],
        "salidas": [(35, 10, "Tratamiento de bronquitis en Galpón Centro")],
    },
]


def _insertar_insumos(
    conn: sqlite3.Connection, id_usuario: int, categorias: dict
) -> dict:
    """Insumos con sus compras y consumos; el stock se calcula al final."""
    insumos = {}
    for datos in INSUMOS:
        cursor = conn.execute(
            """
            INSERT INTO insumo (
                id_usuario, id_categoria, nombre_insumo, unidad_medida,
                stock_actual, umbral_minimo, costo_unitario
            ) VALUES (?, ?, ?, ?, 0, ?, ?)
            """,
            (
                id_usuario,
                categorias[datos["categoria"]],
                datos["nombre"],
                datos["unidad"],
                datos["umbral"],
                datos["costo"],
            ),
        )
        id_insumo = int(cursor.lastrowid)
        insumos[datos["nombre"]] = {"id": id_insumo, **datos}
        for dias, cantidad in datos["entradas"]:
            conn.execute(
                """
                INSERT INTO movimiento_insumo (
                    id_insumo, tipo_movimiento, cantidad, costo_unitario,
                    fecha_movimiento, observaciones
                ) VALUES (?, 'entrada', ?, ?, ?, 'Compra de reposición')
                """,
                (
                    id_insumo,
                    cantidad,
                    datos["costo"],
                    _sello(dias, 9),
                ),
            )
        for dias, cantidad, observacion in datos["salidas"]:
            conn.execute(
                """
                INSERT INTO movimiento_insumo (
                    id_insumo, tipo_movimiento, cantidad, costo_unitario,
                    fecha_movimiento, observaciones
                ) VALUES (?, 'salida', ?, ?, ?, ?)
                """,
                (
                    id_insumo,
                    cantidad,
                    datos["costo"],
                    _sello(dias, 7),
                    observacion,
                ),
            )
    return insumos


# ─────────────────────────────────────────────────────────────────────
# Sanidad: mortalidad, vacunación y vitaminas
# ─────────────────────────────────────────────────────────────────────


def _evento(
    conn: sqlite3.Connection,
    id_camada: int,
    tipo: str,
    dias_atras: int,
    descripcion: str,
    mortalidad: int = 0,
) -> int:
    """Inserta un evento sanitario y devuelve su id."""
    cursor = conn.execute(
        """
        INSERT INTO evento_sanitario (
            id_camada, tipo_evento, fecha_evento, descripcion, mortalidad
        ) VALUES (?, ?, ?, ?, ?)
        """,
        (id_camada, tipo, _fecha(dias_atras), descripcion, mortalidad),
    )
    return int(cursor.lastrowid)


def _salida_insumo(
    conn: sqlite3.Connection,
    insumo: dict,
    cantidad: float,
    dias_atras: int,
    observacion: str,
) -> int:
    """Registra una salida de inventario y devuelve el id del movimiento."""
    cursor = conn.execute(
        """
        INSERT INTO movimiento_insumo (
            id_insumo, tipo_movimiento, cantidad, costo_unitario,
            fecha_movimiento, observaciones
        ) VALUES (?, 'salida', ?, ?, ?, ?)
        """,
        (
            insumo["id"],
            cantidad,
            insumo["costo"],
            _sello(dias_atras, 8),
            observacion,
        ),
    )
    return int(cursor.lastrowid)


def _insertar_sanidad(conn: sqlite3.Connection, camadas: dict, insumos: dict) -> None:
    """Mortalidad, vacunaciones y vitaminas del período."""
    # Mortalidad: cada evento descuenta aves de la camada.
    mortalidad = [
        ("Galpón Norte", 40, 5, "Cinco aves muertas por golpe de calor"),
        ("Galpón Norte", 12, 3, "Tres aves muertas, causa indeterminada"),
        ("Galpón Sur", 30, 6, "Seis aves muertas por congestión"),
        ("Galpón Centro", 20, 4, "Cuatro aves muertas por estrés"),
        ("Galpón Centro", 6, 2, "Dos aves muertas por depredador"),
    ]
    for nombre, dias, aves, descripcion in mortalidad:
        _evento(conn, camadas[nombre]["id"], "mortalidad", dias, descripcion, aves)
        camadas[nombre]["actual"] -= aves
        conn.execute(
            "UPDATE camada SET cantidad_actual = ? WHERE id_camada = ?",
            (camadas[nombre]["actual"], camadas[nombre]["id"]),
        )

    # Vacunación Newcastle (dosis 1) en las tres camadas.
    for nombre in camadas:
        _evento(
            conn,
            camadas[nombre]["id"],
            "vacunacion",
            55,
            f"Vacunación Newcastle dosis 1 — {nombre}",
        )
        _salida_insumo(
            conn,
            insumos["Newcastle La Sota"],
            8,
            55,
            f"Vacunación Newcastle: {nombre}",
        )

    # Enfermedad tratada con oxitetraciclina.
    _evento(
        conn,
        camadas["Galpón Centro"]["id"],
        "enfermedad",
        35,
        "Brote leve de bronquitis infecciosa, tratamiento con oxitetraciclina",
    )


# ─────────────────────────────────────────────────────────────────────
# Producción y ventas
# ─────────────────────────────────────────────────────────────────────


def _tasa_postura(nombre: str, dias_atras: int) -> float:
    """Postura diaria con una caída marcada ayer en Galpón Sur."""
    if nombre == "Galpón Sur" and dias_atras == 1:
        return 0.78
    base = {"Galpón Norte": 0.91, "Galpón Sur": 0.89, "Galpón Centro": 0.87}
    return base[nombre] - ((dias_atras * 7) % 5) * 0.008


def _reparto(total: int) -> dict:
    """Divide la recolección por tipo de huevo sin perder unidades."""
    aa = round(total * 0.55)
    a = round(total * 0.30)
    b = round(total * 0.12)
    return {"AA": aa, "A": a, "B": b, "No_apto": total - aa - a - b}


def _observacion(dias_atras: int) -> str | None:
    """Nota ocasional en la recolección."""
    notas = {
        7: "Clima lluvioso, leve baja en la postura",
        18: "Revisión de nidos y limpieza de bandejas",
        31: "Se ajustó la iluminación del galpón",
        47: "Semana estable, sin novedades",
    }
    return notas.get(dias_atras)


def _crear_pedido(
    conn: sqlite3.Connection,
    id_usuario: int,
    tipos: dict,
    clientes: list,
    stock: dict,
    dias_atras: int,
) -> dict | None:
    """Crea un pedido con parte del stock disponible; None si es cancelado."""
    lineas = {}
    for tipo, factor in (("AA", 0.55), ("A", 0.50), ("B", 0.40)):
        cantidad = int(stock[tipo] * factor)
        if cantidad >= 10:
            lineas[tipo] = cantidad
    if sum(lineas.values()) < 60:
        return None

    if dias_atras <= 3:
        estado = "pendiente"
    elif dias_atras == 12:
        estado = "cancelado"
    elif dias_atras == 15:
        estado = "enviado"
    else:
        estado = "recibido"

    cliente = clientes[(dias_atras // 3) % len(clientes)]
    valor = sum(lineas[tipo] * PRECIOS[tipo] for tipo in lineas)
    cursor = conn.execute(
        """
        INSERT INTO pedido (
            id_cliente, id_usuario, fecha_pedido, estado_pedido, valor_total
        ) VALUES (?, ?, ?, ?, ?)
        """,
        (cliente["id"], id_usuario, _fecha(dias_atras), estado, valor),
    )
    id_pedido = int(cursor.lastrowid)
    for tipo, cantidad in lineas.items():
        conn.execute(
            """
            INSERT INTO detalle_pedido (
                id_pedido, id_tipo, cantidad, precio_unitario
            ) VALUES (?, ?, ?, ?)
            """,
            (id_pedido, tipos[tipo], cantidad, PRECIOS[tipo]),
        )
    if estado == "cancelado":
        return None
    return {"lineas": lineas}


def _insertar_produccion_y_ventas(
    conn: sqlite3.Connection,
    id_usuario: int,
    tipos: dict,
    camadas: dict,
    clientes: list,
) -> tuple[dict, dict, int, tuple | None]:
    """Sesenta días de recolección y las ventas que la consumen."""
    producido = {"AA": 0, "A": 0, "B": 0, "No_apto": 0}
    vendido = {"AA": 0, "A": 0, "B": 0, "No_apto": 0}
    stock = {"AA": 0, "A": 0, "B": 0}
    pedidos = 0
    ayer_sur = None

    # Del día más antiguo al de hoy, para que el stock cuadre.
    for dias_atras in range(59, -1, -1):
        for nombre, camada in camadas.items():
            aves = camada["actual"]
            tasa = _tasa_postura(nombre, dias_atras)
            huevos = round(aves * tasa)
            reparto = _reparto(huevos)
            cursor = conn.execute(
                """
                INSERT INTO produccion_diaria (
                    id_usuario, id_camada, fecha_recoleccion, total_huevos,
                    observaciones
                ) VALUES (?, ?, ?, ?, ?)
                """,
                (
                    id_usuario,
                    camada["id"],
                    _fecha(dias_atras),
                    huevos,
                    _observacion(dias_atras),
                ),
            )
            id_produccion = int(cursor.lastrowid)
            for tipo, cantidad in reparto.items():
                if cantidad > 0:
                    conn.execute(
                        """
                        INSERT INTO produccion_detalle (
                            id_produccion, id_tipo, cantidad
                        ) VALUES (?, ?, ?)
                        """,
                        (id_produccion, tipos[tipo], cantidad),
                    )
                producido[tipo] += cantidad
                if tipo != "No_apto":
                    stock[tipo] += cantidad
            if nombre == "Galpón Sur" and dias_atras == 1:
                ayer_sur = (huevos, aves)

        vender = 57 >= dias_atras >= 3 and dias_atras % 3 == 0
        if vender:
            pedido = _crear_pedido(conn, id_usuario, tipos, clientes, stock, dias_atras)
            if pedido is not None:
                for tipo, cantidad in pedido["lineas"].items():
                    vendido[tipo] += cantidad
                    stock[tipo] -= cantidad
                pedidos += 1

    return producido, vendido, pedidos, ayer_sur


def _insertar_stock(
    conn: sqlite3.Connection, id_usuario: int, tipos: dict, producido, vendido
) -> None:
    """Stock de huevos: producido menos vendido (sin pedidos cancelados)."""
    for tipo, id_tipo in tipos.items():
        cantidad = producido[tipo] - vendido[tipo]
        if cantidad < 0:
            raise SystemExit(
                f"Datos incoherentes: stock negativo de {tipo} ({cantidad})"
            )
        conn.execute(
            """
            INSERT INTO stock_produccion (
                id_usuario, id_tipo, cantidad_actual, valor_unidad,
                ultima_modificacion
            ) VALUES (?, ?, ?, ?, ?)
            """,
            (id_usuario, id_tipo, cantidad, PRECIOS[tipo], _sello(0, 6)),
        )


# ─────────────────────────────────────────────────────────────────────
# Clientes
# ─────────────────────────────────────────────────────────────────────


def _insertar_clientes(conn: sqlite3.Connection, id_usuario: int) -> list:
    """Cuatro clientes habituales de la granja."""
    datos = [
        ("Tienda La Cosecha", "3204440011", "Cra 3 # 8-15, Bogotá"),
        ("Supermercado El Trigal", "3158881122", "Av 68 # 22-10, Bogotá"),
        ("Restaurante El Buen Sabor", "3118887766", "Cl 10 # 8-20, Chía"),
        ("Minimercado Los Andes", "3002223344", "Cr 15 # 22-05, Soacha"),
    ]
    clientes = []
    for nombre, telefono, direccion in datos:
        cursor = conn.execute(
            """
            INSERT INTO cliente (
                id_usuario, nombre_cliente, telefono, direccion,
                fecha_ultima_compra
            ) VALUES (?, ?, ?, ?, NULL)
            """,
            (id_usuario, nombre, telefono, direccion),
        )
        clientes.append({"id": int(cursor.lastrowid), "nombre": nombre})
    return clientes


def _actualizar_ultima_compra(conn: sqlite3.Connection) -> None:
    """Marca la fecha del último pedido entregado de cada cliente."""
    conn.execute("""
        UPDATE cliente SET fecha_ultima_compra = (
            SELECT MAX(p.fecha_pedido) FROM pedido p
            WHERE p.id_cliente = cliente.id_cliente
              AND p.estado_pedido != 'cancelado'
        )
        """)


# ─────────────────────────────────────────────────────────────────────
# Recordatorios sanitarios y dosis
# ─────────────────────────────────────────────────────────────────────


def _recordatorio(
    conn: sqlite3.Connection,
    id_usuario: int,
    id_camada: int | None,
    id_insumo: int,
    intervalo: int,
    inicio: str,
    fin: str | None,
    cantidad: float,
    creado: str,
) -> int:
    """Crea una regla de recordatorio cada N días."""
    cursor = conn.execute(
        """
        INSERT INTO recordatorio_sanitario (
            id_usuario, id_camada, id_insumo, modo, intervalo_dias,
            dias_semana, fechas_especificas, fecha_inicio, fecha_fin,
            cantidad, activo, fecha_creacion
        ) VALUES (?, ?, ?, 'cada_dias', ?, NULL, NULL, ?, ?, ?, 1, ?)
        """,
        (
            id_usuario,
            id_camada,
            id_insumo,
            intervalo,
            inicio,
            fin,
            cantidad,
            creado,
        ),
    )
    return int(cursor.lastrowid)


def _dosis(
    conn: sqlite3.Connection,
    id_recordatorio: int,
    id_camada: int,
    fecha: str,
    numero: int,
    aplicada: bool,
    id_movimiento: int | None = None,
    id_evento: int | None = None,
) -> None:
    """Inserta una ocurrencia del recordatorio."""
    conn.execute(
        """
        INSERT INTO recordatorio_dosis (
            id_recordatorio, id_camada, fecha_programada, numero_dosis,
            aplicada, fecha_aplicacion, id_movimiento, id_evento
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (
            id_recordatorio,
            id_camada,
            fecha,
            numero,
            1 if aplicada else 0,
            f"{fecha} 08:00:00" if aplicada else None,
            id_movimiento,
            id_evento,
        ),
    )


def _insertar_recordatorios(
    conn: sqlite3.Connection,
    id_usuario: int,
    camadas: dict,
    insumos: dict,
) -> dict:
    """Tres planes: vitamina global, refuerzo y una dosis para mañana."""
    vitamina = insumos["Vitamina AD3E"]
    newcastle = insumos["Newcastle La Sota"]

    # 1) Vitamina global cada 15 días; aplicadas las tres primeras.
    global_id = _recordatorio(
        conn,
        id_usuario,
        None,
        vitamina["id"],
        15,
        _fecha(45),
        None,
        2,
        _sello(45, 10),
    )
    # Días de la ocurrencia contados hacia atrás (negativo = futuro).
    fechas_global = [45, 30, 15, 0, -15, -30, -45]
    for nombre, camada in camadas.items():
        for indice, dias in enumerate(fechas_global, start=1):
            aplicada = dias in (45, 30, 15)
            movimiento = None
            evento = None
            if aplicada:
                movimiento = _salida_insumo(
                    conn,
                    vitamina,
                    2,
                    dias,
                    f"Recordatorio sanitario: Vitamina AD3E en {nombre}",
                )
                evento = _evento(
                    conn,
                    camada["id"],
                    "otro",
                    dias,
                    f"Vitamina AD3E: 2 ml (recordatorio #{global_id})",
                )
            _dosis(
                conn,
                global_id,
                camada["id"],
                _fecha(dias),
                indice,
                aplicada,
                movimiento,
                evento,
            )

    # 2) Refuerzo de Newcastle en Galpón Norte cada 30 días.
    norte = camadas["Galpón Norte"]
    refuerzo_id = _recordatorio(
        conn,
        id_usuario,
        norte["id"],
        newcastle["id"],
        30,
        _fecha(30),
        None,
        8,
        _sello(30, 10),
    )
    movimiento_refuerzo = _salida_insumo(
        conn,
        newcastle,
        8,
        30,
        "Recordatorio sanitario: Newcastle en Galpón Norte",
    )
    evento_refuerzo = _evento(
        conn,
        norte["id"],
        "vacunacion",
        30,
        f"Newcastle La Sota: 8 ml (recordatorio #{refuerzo_id})",
    )
    _dosis(
        conn,
        refuerzo_id,
        norte["id"],
        _fecha(30),
        1,
        True,
        movimiento_refuerzo,
        evento_refuerzo,
    )
    _dosis(conn, refuerzo_id, norte["id"], _fecha(0), 2, False)
    _dosis(conn, refuerzo_id, norte["id"], _fecha(-30), 3, False)

    # 3) Vitamina de mañana en Galpón Sur: genera el aviso previo.
    sur = camadas["Galpón Sur"]
    manana_id = _recordatorio(
        conn,
        id_usuario,
        sur["id"],
        vitamina["id"],
        1,
        _fecha(-1),
        _fecha(-1),
        1,
        _sello(0, 18),
    )
    _dosis(conn, manana_id, sur["id"], _fecha(-1), 1, False)

    return {
        "global": global_id,
        "refuerzo": refuerzo_id,
        "manana": manana_id,
    }


# ─────────────────────────────────────────────────────────────────────
# Notificaciones y análisis
# ─────────────────────────────────────────────────────────────────────


def _insertar_notificaciones(
    conn: sqlite3.Connection,
    id_usuario: int,
    camadas: dict,
    insumos: dict,
    recordatorios: dict,
    ayer_sur: tuple | None,
) -> None:
    """Avisos listos para ver en el panel de la app."""
    norte = camadas["Galpón Norte"]
    sur = camadas["Galpón Sur"]
    vitamina = insumos["Vitamina AD3E"]
    amonio = insumos["Amonio cuaternario 5L"]

    avisos = [
        {
            "tipo": "stock_bajo",
            "id_insumo": vitamina["id"],
            "titulo": "Stock por debajo del mínimo",
            "mensaje": (
                f"{vitamina['nombre']} está por debajo del mínimo "
                f"({2:.2f} de {vitamina['umbral']:.2f} {vitamina['unidad']})"
            ),
            "stock_referencia": 2,
            "fecha_creacion": _sello(2, 6),
        },
        {
            "tipo": "sin_stock",
            "id_insumo": amonio["id"],
            "titulo": "Sin stock",
            "mensaje": f"{amonio['nombre']} se quedó sin stock",
            "stock_referencia": 0,
            "fecha_creacion": _sello(1, 6),
        },
        {
            "tipo": "recordatorio_sanitario",
            "id_camada": norte["id"],
            "id_recordatorio": recordatorios["refuerzo"],
            "momento": "hoy",
            "fecha_referencia": _fecha(0),
            "titulo": "Recordatorio: Newcastle La Sota",
            "mensaje": (
                "Hoy: aplicar 8.00 ml de Newcastle La Sota a Galpón Norte "
                "(dosis 2 de 3)."
            ),
            "fecha_creacion": _sello(0, 5, 5),
        },
        {
            "tipo": "recordatorio_sanitario",
            "id_camada": sur["id"],
            "id_recordatorio": recordatorios["manana"],
            "momento": "previo",
            "fecha_referencia": _fecha(-1),
            "titulo": "Recordatorio: Vitamina AD3E",
            "mensaje": (
                "Mañana: aplicar 1.00 ml de Vitamina AD3E a Galpón Sur "
                "(dosis 1 de 1)."
            ),
            "fecha_creacion": _sello(0, 5, 6),
        },
    ]

    if ayer_sur is not None:
        huevos, aves = ayer_sur
        tasa = huevos / aves if aves else 0
        avisos.append(
            {
                "tipo": "produccion_baja",
                "id_camada": sur["id"],
                "fecha_referencia": _fecha(1),
                "titulo": "Producción baja",
                "mensaje": (
                    f"Galpón Sur: {huevos} huevos ayer para {aves} aves "
                    f"({tasa:.0%} de postura). Revisa la camada."
                ),
                "fecha_creacion": _sello(1, 5, 10),
            }
        )

    # Recordatorio de reportes del último lunes, ya leído.
    lunes = HOY - timedelta(days=HOY.weekday())
    desde = lunes - timedelta(days=6)
    avisos.append(
        {
            "tipo": "resumen_periodico",
            "fecha_referencia": lunes.isoformat(),
            "titulo": "Recordatorio de reportes",
            "mensaje": (
                f"Revisa el reporte del {desde:%d/%m} al {lunes:%d/%m}. "
                "Puedes abrirlo cuando quieras."
            ),
            "leida": 1,
            "fecha_creacion": f"{lunes.isoformat()} 05:00:00",
        }
    )

    for aviso in avisos:
        conn.execute(
            """
            INSERT INTO notificacion (
                id_usuario, id_camada, id_insumo, id_recordatorio, tipo,
                titulo, mensaje, leida, eliminada, stock_referencia,
                fecha_referencia, momento, fecha_creacion
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?)
            """,
            (
                id_usuario,
                aviso.get("id_camada"),
                aviso.get("id_insumo"),
                aviso.get("id_recordatorio"),
                aviso["tipo"],
                aviso["titulo"],
                aviso["mensaje"],
                aviso.get("leida", 0),
                aviso.get("stock_referencia"),
                aviso.get("fecha_referencia"),
                aviso.get("momento"),
                aviso["fecha_creacion"],
            ),
        )


def _insertar_analisis(
    conn: sqlite3.Connection, id_usuario: int, camadas: dict
) -> None:
    """Dos análisis de IA del último mes (sin foto en disco)."""
    datos = [
        (
            camadas["Galpón Norte"]["id"],
            20,
            "Muestra 30 huevos: 88% AA, 9% A, 3% No_apto. Cáscara firme.",
            "Mantener la dieta actual y revisar el calcio cada 15 días.",
            "buena",
            88,
        ),
        (
            camadas["Galpón Sur"]["id"],
            6,
            "Muestra 25 huevos: 79% AA, 16% A, 5% B. Leve rugosidad.",
            "Reforzar vitamina AD3E y revisar la ventilación del galpón.",
            "regular",
            79,
        ),
    ]
    for id_camada, dias, resultado, recomendaciones, calidad, puntaje in datos:
        conn.execute(
            """
            INSERT INTO analisis_ia (
                id_usuario, id_camada, fecha_analisis, imagen_url,
                resultado_diagnostico, recomendaciones, calidad_general,
                puntaje_calidad, apto_venta, huevos_detectados, proveedor_ia
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 30, 'demo')
            """,
            (
                id_usuario,
                id_camada,
                _sello(dias, 9, 30),
                f"uploads/analisis/demo/demo_{dias}.jpg",
                resultado,
                recomendaciones,
                calidad,
                puntaje,
            ),
        )


# ─────────────────────────────────────────────────────────────────────
# Verificación de coherencia
# ─────────────────────────────────────────────────────────────────────


def _verificar(
    conn: sqlite3.Connection, id_usuario: int, esperar_unico: bool = True
) -> list[str]:
    """Comprueba que los datos del demo cuadren entre sí.

    Args:
        conn: Conexión a la base.
        id_usuario: Usuario demo cuyos datos se verifican.
        esperar_unico: Si True, exige que el demo sea el único usuario.

    Returns:
        list[str]: Comprobaciones superadas, listas para imprimir.

    Raises:
        SystemExit: Si alguna comprobación falla.
    """
    comprobaciones = []

    def revisar(condicion: bool, mensaje: str) -> None:
        if not condicion:
            raise SystemExit(f"Datos incoherentes: {mensaje}")
        comprobaciones.append(mensaje)

    usuarios = conn.execute("SELECT COUNT(*) FROM usuario").fetchone()[0]
    if esperar_unico:
        revisar(usuarios == 1, "La base tiene un único usuario")
    else:
        revisar(
            usuarios >= 1,
            f"El demo convive con {usuarios - 1} usuario(s) existente(s)",
        )

    hash_guardado = conn.execute(
        "SELECT contrasena_hash FROM usuario WHERE id_usuario = ?",
        (id_usuario,),
    ).fetchone()[0]

    revisar(
        verificar_hash(CLAVE, hash_guardado),
        "La contraseña del demo valida con bcrypt",
    )

    for id_camada, inicial, actual, muertes in conn.execute(
        """
        SELECT c.id_camada, c.cantidad_inicial, c.cantidad_actual,
               COALESCE(SUM(e.mortalidad), 0)
        FROM camada c
        LEFT JOIN evento_sanitario e
               ON e.id_camada = c.id_camada AND e.tipo_evento = 'mortalidad'
        WHERE c.id_usuario = ?
        GROUP BY c.id_camada
        """,
        (id_usuario,),
    ):
        revisar(
            inicial - muertes == actual,
            f"Camada {id_camada}: aves = iniciales - mortalidad",
        )

    for id_insumo, stock, resumen in conn.execute(
        """
        SELECT i.id_insumo, i.stock_actual,
               COALESCE(SUM(CASE WHEN m.tipo_movimiento = 'entrada'
                                 THEN m.cantidad ELSE -m.cantidad END), 0)
        FROM insumo i
        LEFT JOIN movimiento_insumo m ON m.id_insumo = i.id_insumo
        WHERE i.id_usuario = ?
        GROUP BY i.id_insumo
        """,
        (id_usuario,),
    ):
        revisar(
            abs(float(stock) - float(resumen)) < 0.01,
            f"Insumo {id_insumo}: stock = entradas - salidas",
        )

    for id_produccion, total, detalle in conn.execute(
        """
        SELECT p.id_produccion, p.total_huevos,
               COALESCE(SUM(d.cantidad), 0)
        FROM produccion_diaria p
        LEFT JOIN produccion_detalle d ON d.id_produccion = p.id_produccion
        WHERE p.id_usuario = ?
        GROUP BY p.id_produccion
        """,
        (id_usuario,),
    ):
        revisar(
            total == detalle,
            f"Producción {id_produccion}: total = suma del detalle",
        )

    for id_tipo, stock, producido, vendido in conn.execute(
        """
        SELECT s.id_tipo, s.cantidad_actual,
               COALESCE((SELECT SUM(d.cantidad)
                         FROM produccion_detalle d
                         JOIN produccion_diaria p
                           ON p.id_produccion = d.id_produccion
                         WHERE d.id_tipo = s.id_tipo
                           AND p.id_usuario = ?), 0),
               COALESCE((SELECT SUM(dp.cantidad)
                         FROM detalle_pedido dp
                         JOIN pedido p ON p.id_pedido = dp.id_pedido
                         WHERE dp.id_tipo = s.id_tipo
                           AND p.id_usuario = ?
                           AND p.estado_pedido != 'cancelado'), 0)
        FROM stock_produccion s
        WHERE s.id_usuario = ?
        """,
        (id_usuario, id_usuario, id_usuario),
    ):
        revisar(
            stock == producido - vendido,
            f"Tipo {id_tipo}: stock de huevos = producido - vendido",
        )

    for id_pedido, total, detalle in conn.execute(
        """
        SELECT p.id_pedido, p.valor_total,
               COALESCE(SUM(d.cantidad * d.precio_unitario), 0)
        FROM pedido p
        LEFT JOIN detalle_pedido d ON d.id_pedido = p.id_pedido
        WHERE p.id_usuario = ?
        GROUP BY p.id_pedido
        """,
        (id_usuario,),
    ):
        revisar(
            abs(float(total) - float(detalle)) < 0.01,
            f"Pedido {id_pedido}: valor total = suma del detalle",
        )

    for id_dosis, id_movimiento, id_evento in conn.execute(
        """
        SELECT d.id_dosis, d.id_movimiento, d.id_evento
        FROM recordatorio_dosis d
        JOIN recordatorio_sanitario r
          ON r.id_recordatorio = d.id_recordatorio
        WHERE r.id_usuario = ? AND d.aplicada = 1
        """,
        (id_usuario,),
    ):
        revisar(
            id_movimiento is not None and id_evento is not None,
            f"Dosis {id_dosis}: aplicada con movimiento y evento",
        )

    for id_dosis, insumo_movimiento, insumo_recordatorio in conn.execute(
        """
        SELECT d.id_dosis, m.id_insumo, r.id_insumo
        FROM recordatorio_dosis d
        JOIN recordatorio_sanitario r ON r.id_recordatorio = d.id_recordatorio
        JOIN movimiento_insumo m ON m.id_movimiento = d.id_movimiento
        WHERE r.id_usuario = ? AND d.aplicada = 1
        """,
        (id_usuario,),
    ):
        revisar(
            insumo_movimiento == insumo_recordatorio,
            f"Dosis {id_dosis}: descuenta el insumo del recordatorio",
        )

    repetidas = conn.execute(
        """
        SELECT COUNT(*) FROM (
            SELECT d.id_recordatorio, d.id_camada, d.fecha_programada
            FROM recordatorio_dosis d
            JOIN recordatorio_sanitario r
              ON r.id_recordatorio = d.id_recordatorio
            WHERE r.id_usuario = ?
            GROUP BY d.id_recordatorio, d.id_camada, d.fecha_programada
            HAVING COUNT(*) > 1
        )
        """,
        (id_usuario,),
    ).fetchone()[0]
    revisar(repetidas == 0, "No hay dosis duplicadas por camada y fecha")

    return comprobaciones


# ─────────────────────────────────────────────────────────────────────
# Carga principal
# ─────────────────────────────────────────────────────────────────────


def cargar_demo(db_path: Path, conservar: bool = False) -> dict:
    """Crea (o completa) la base con el usuario demo y sus tres meses.

    Args:
        db_path: Ruta del archivo SQLite.
        conservar: Si True, agrega el demo sin borrar el resto de la base.

    Returns:
        dict: Conteos por tabla y comprobaciones de coherencia.

    Raises:
        SystemExit: Si el demo ya existe y se pidió conservar la base.
    """
    db_path.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(db_path)
    try:
        conn.execute("PRAGMA foreign_keys = ON")
        if conservar:
            existe = conn.execute(
                "SELECT 1 FROM usuario WHERE correo_electronico = ?", (CORREO,)
            ).fetchone()
            if existe:
                raise SystemExit(
                    f"El usuario {CORREO} ya existe en {db_path}. "
                    "Ejecuta sin --conservar para recrear la base."
                )
        else:
            _vaciar(conn)
            _crear_esquema(conn)

        tipos, categorias = _cargar_catalogos(conn)
        id_usuario = _insertar_usuario(conn)
        camadas = _insertar_camadas(conn, id_usuario)
        insumos = _insertar_insumos(conn, id_usuario, categorias)
        _insertar_sanidad(conn, camadas, insumos)
        clientes = _insertar_clientes(conn, id_usuario)

        producido, vendido, pedidos, ayer_sur = _insertar_produccion_y_ventas(
            conn, id_usuario, tipos, camadas, clientes
        )
        _insertar_stock(conn, id_usuario, tipos, producido, vendido)
        _actualizar_ultima_compra(conn)

        recordatorios = _insertar_recordatorios(conn, id_usuario, camadas, insumos)
        _insertar_notificaciones(
            conn, id_usuario, camadas, insumos, recordatorios, ayer_sur
        )
        _insertar_analisis(conn, id_usuario, camadas)

        # El stock de insumos se calcula con todos los movimientos ya creados.
        conn.execute("""
            UPDATE insumo SET stock_actual = COALESCE((
                SELECT SUM(CASE WHEN m.tipo_movimiento = 'entrada'
                                THEN m.cantidad ELSE -m.cantidad END)
                FROM movimiento_insumo m
                WHERE m.id_insumo = insumo.id_insumo
            ), 0)
            """)

        comprobaciones = _verificar(conn, id_usuario, esperar_unico=not conservar)
        conn.commit()

        conteos = {
            tabla: conn.execute(f"SELECT COUNT(*) FROM {tabla}").fetchone()[0]
            for tabla in (
                "usuario",
                "camada",
                "insumo",
                "movimiento_insumo",
                "evento_sanitario",
                "produccion_diaria",
                "produccion_detalle",
                "stock_produccion",
                "cliente",
                "pedido",
                "detalle_pedido",
                "recordatorio_sanitario",
                "recordatorio_dosis",
                "notificacion",
                "analisis_ia",
            )
        }
        conteos["pedidos_generados"] = pedidos
        return {"conteos": conteos, "comprobaciones": comprobaciones}
    finally:
        conn.close()


def main() -> None:
    """Punto de entrada del script de datos demo."""
    parser = argparse.ArgumentParser(
        description="Carga el usuario demo con tres meses de datos."
    )
    parser.add_argument(
        "--db",
        type=Path,
        default=_DB_POR_DEFECTO,
        help="Ruta de la base SQLite (por defecto app/backend/eggchecker.db).",
    )
    parser.add_argument(
        "--conservar",
        action="store_true",
        help="Agrega el demo sin borrar los datos existentes.",
    )
    args = parser.parse_args()

    resultado = cargar_demo(args.db, conservar=args.conservar)
    print(f"Base demo cargada en: {args.db}")
    print(f"Usuario: {CORREO} / {CLAVE}")
    print(f"Granja: {GRANJA}")
    print("\nRegistros:")
    for tabla, cantidad in resultado["conteos"].items():
        print(f"  {tabla}: {cantidad}")
    print("\nCoherencia verificada:")
    for comprobacion in resultado["comprobaciones"]:
        print(f"  ✓ {comprobacion}")


if __name__ == "__main__":
    main()
