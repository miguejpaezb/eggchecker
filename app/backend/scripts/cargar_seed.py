"""Carga el esquema y los datos de prueba de EggChecker en SQLite.

Script de bootstrap para desarrollo y QA: crea las 13 tablas (con sus
indices y vistas) y las llena con los registros de la evidencia
GA6-220501096-AA2-EV03. La recarga es idempotente: si la base ya tiene
tablas, las elimina y vuelve a crearlas desde cero.

Uso (desde app/backend):
    python scripts/cargar_seed.py [--db RUTA]

Por defecto usa app/backend/eggchecker.db, la misma base que levanta la
API en local (ver app/core/config.py).
"""

import argparse
import sqlite3
from pathlib import Path

# Las tablas se listan en el mismo orden del modelo relacional de la
# evidencia; el script de seed las inserta todas por nombre.
_TABLAS = (
    "usuario",
    "categoria_insumo",
    "insumo",
    "movimiento_insumo",
    "camada",
    "evento_sanitario",
    "tipo_huevo",
    "produccion_diaria",
    "produccion_detalle",
    "cliente",
    "pedido",
    "detalle_pedido",
    "analisis_ia",
)

_BASE_DIR = Path(__file__).resolve().parents[1]
_DDL_PATH = _BASE_DIR / "db" / "eggchecker_ddl.sql"
_SEED_PATH = _BASE_DIR / "db" / "eggchecker_seed.sql"


def _leer_sql(ruta: Path) -> str:
    """Lee un archivo SQL de la carpeta db/."""
    return ruta.read_text(encoding="utf-8")


def _eliminar_tablas(conn: sqlite3.Connection) -> None:
    """Elimina tablas y vistas existentes para recargar desde cero."""
    objetos = conn.execute(
        "SELECT type, name FROM sqlite_master WHERE type IN ('table', 'view')"
    ).fetchall()
    for tipo, nombre in objetos:
        if not nombre.startswith("sqlite_"):
            conn.execute(f"DROP {tipo.upper()} IF EXISTS {nombre}")


def _contar_registros(conn: sqlite3.Connection) -> dict[str, int]:
    """Cuenta los registros de las 13 tablas del esquema."""
    conteos: dict[str, int] = {}
    for tabla in _TABLAS:
        conteos[tabla] = conn.execute(f"SELECT COUNT(*) FROM {tabla}").fetchone()[0]
    return conteos


def cargar_seed(db_path: Path) -> dict[str, int]:
    """Construye la base de datos y devuelve el conteo por tabla.

    Args:
        db_path: Ruta del archivo SQLite a crear o recargar.

    Returns:
        Diccionario con la cantidad de registros de cada tabla.

    Raises:
        sqlite3.Error: Si el DDL o el seed fallan al ejecutarse.
    """
    db_path.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(db_path)
    try:
        conn.execute("PRAGMA foreign_keys = OFF")
        _eliminar_tablas(conn)
        conn.execute("PRAGMA foreign_keys = ON")
        conn.executescript(_leer_sql(_DDL_PATH))
        conn.executescript(_leer_sql(_SEED_PATH))
        return _contar_registros(conn)
    finally:
        conn.close()


def main() -> None:
    """Punto de entrada del script de carga de datos."""
    parser = argparse.ArgumentParser(
        description="Carga el esquema y el seed de EggChecker en SQLite."
    )
    parser.add_argument(
        "--db",
        type=Path,
        default=_BASE_DIR / "eggchecker.db",
        help="Ruta de la base SQLite (por defecto app/backend/eggchecker.db).",
    )
    args = parser.parse_args()

    conteos = cargar_seed(args.db)
    print(f"Base cargada en: {args.db}")
    print(f"Tablas creadas: {len(_TABLAS)}")
    for tabla, cantidad in conteos.items():
        print(f"  {tabla}: {cantidad}")


if __name__ == "__main__":
    main()
