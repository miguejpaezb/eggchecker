"""Pruebas de la carga del esquema y del seed en SQLite.

Corren contra una base temporal por prueba (tmp_path), de modo que el
eggchecker.db de desarrollo nunca se toca durante pytest.
"""

import sqlite3
from pathlib import Path

from scripts.cargar_seed import cargar_seed

_CONTEOS_ESPERADOS = {
    "usuario": 10,
    "token_recuperacion": 0,
    "categoria_insumo": 6,
    "insumo": 14,
    "movimiento_insumo": 14,
    "camada": 12,
    "evento_sanitario": 10,
    "tipo_huevo": 4,
    "produccion_diaria": 12,
    "produccion_detalle": 48,
    "cliente": 12,
    "pedido": 12,
    "detalle_pedido": 20,
    "analisis_ia": 10,
}

_VISTAS_ESPERADAS = {
    "v_produccion_detallada",
    "v_alertas_insumo",
    "v_pedidos_pendientes",
}


def test_carga_crea_las_14_tablas(tmp_path: Path) -> None:
    """El esquema se crea completo con las 14 tablas del modelo."""
    db_path = tmp_path / "seed.db"
    cargar_seed(db_path)

    with sqlite3.connect(db_path) as conn:
        tablas = [
            fila[0]
            for fila in conn.execute(
                "SELECT name FROM sqlite_master WHERE type = 'table'"
            ).fetchall()
            if not fila[0].startswith("sqlite_")
        ]

    assert len(tablas) == 14


def test_carga_inserta_registros_esperados(tmp_path: Path) -> None:
    """Los datos del seed coinciden con la evidencia por cada tabla."""
    db_path = tmp_path / "seed.db"
    conteos = cargar_seed(db_path)

    assert conteos == _CONTEOS_ESPERADOS


def test_carga_registra_dos_insumos_bajo_umbral(tmp_path: Path) -> None:
    """Existen exactamente 2 insumos con stock bajo el umbral mínimo."""
    db_path = tmp_path / "seed.db"
    cargar_seed(db_path)

    with sqlite3.connect(db_path) as conn:
        bajo_umbral = conn.execute(
            "SELECT COUNT(*) FROM insumo WHERE stock_actual < umbral_minimo"
        ).fetchone()[0]

    assert bajo_umbral == 2


def test_carga_crea_las_tres_vistas(tmp_path: Path) -> None:
    """Las tres vistas del modelo relacional se materializan."""
    db_path = tmp_path / "seed.db"
    cargar_seed(db_path)

    with sqlite3.connect(db_path) as conn:
        vistas = {
            fila[0]
            for fila in conn.execute(
                "SELECT name FROM sqlite_master WHERE type = 'view'"
            ).fetchall()
        }

    assert vistas == _VISTAS_ESPERADAS


def test_carga_es_idempotente(tmp_path: Path) -> None:
    """Recargar sobre la misma base no duplica ni rompe datos."""
    db_path = tmp_path / "seed.db"

    cargar_seed(db_path)
    conteos = cargar_seed(db_path)

    assert conteos == _CONTEOS_ESPERADOS
