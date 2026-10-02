"""Verificación de humo del CRUD del módulo de clientes.

Levanta la API con una base SQLite temporal cargada con el seed de
desarrollo y recorre el ciclo CRUD completo del módulo de clientes:
crear, listar, obtener, actualizar y eliminar. Cada paso imprime el
método, la ruta, el código de estado y el cuerpo JSON, de modo que sirve
como comprobación end-to-end y como evidencia de las solicitudes y
respuestas de la API.

Uso (desde app/backend):
    python -m scripts.smoke_crud
"""

import json
import tempfile
from pathlib import Path

from app.core.database import get_db
from app.main import create_app
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from scripts.cargar_seed import cargar_seed

CLAVE = "Test1234!"
CORREO = "smoke.crud@test.com"


class PasoFallidoError(RuntimeError):
    """Error cuando un paso del CRUD no devuelve el estado esperado."""


def _construir_cliente(db_path: Path):
    """Crea un TestClient con la API apuntando a una base temporal.

    Args:
        db_path: Ruta de la base SQLite ya cargada con el seed.

    Returns:
        tuple[TestClient, Engine]: Cliente de pruebas y motor a disponer
        al finalizar para liberar el archivo de la base.
    """
    engine = create_engine(
        f"sqlite:///{db_path}", connect_args={"check_same_thread": False}
    )
    session_fabrica = sessionmaker(bind=engine, autocommit=False, autoflush=False)

    def override_get_db():
        db = session_fabrica()
        try:
            yield db
        finally:
            db.close()

    app = create_app()
    app.dependency_overrides[get_db] = override_get_db
    return TestClient(app), engine


def _verificar(numero: int, metodo: str, ruta: str, respuesta, esperado: int) -> dict:
    """Imprime el resultado de un paso y valida el estado esperado.

    Args:
        numero: Número consecutivo del paso.
        metodo: Verbo HTTP ejecutado.
        ruta: Ruta solicitada a la API.
        respuesta: Respuesta devuelta por el TestClient.
        esperado: Código de estado que se espera recibir.

    Returns:
        dict: Cuerpo JSON de la respuesta, o un diccionario vacío si no hay.

    Raises:
        PasoFallidoError: Si el código de estado no coincide con el esperado.
    """
    estado = "OK" if respuesta.status_code == esperado else "FALLO"
    print(f"[{numero}] {metodo} {ruta} -> {respuesta.status_code} ({estado})")

    cuerpo = respuesta.json() if respuesta.content else {}
    if cuerpo:
        print(f"    {json.dumps(cuerpo, ensure_ascii=False)}")

    if respuesta.status_code != esperado:
        mensaje = f"Se esperaba {esperado} y se obtuvo {respuesta.status_code}"
        raise PasoFallidoError(mensaje)
    return cuerpo


def _ejecutar(cliente: TestClient) -> None:
    """Recorre el flujo de autenticación y el CRUD de clientes."""
    _verificar(
        1,
        "POST",
        "/api/auth/registro",
        cliente.post(
            "/api/auth/registro",
            json={
                "nombre_completo": "Cliente Smoke",
                "correo_electronico": CORREO,
                "contrasena": CLAVE,
            },
        ),
        201,
    )

    login = _verificar(
        2,
        "POST",
        "/api/auth/login",
        cliente.post(
            "/api/auth/login",
            json={"correo_electronico": CORREO, "contrasena": CLAVE},
        ),
        200,
    )
    headers = {"Authorization": f"Bearer {login['access_token']}"}

    creado = _verificar(
        3,
        "POST",
        "/api/clientes",
        cliente.post(
            "/api/clientes",
            headers=headers,
            json={
                "nombre_cliente": "Tienda El Huevo",
                "telefono": "3001234567",
                "direccion": "Calle 1 #2-3",
            },
        ),
        201,
    )
    id_cliente = creado["id_cliente"]

    _verificar(
        4,
        "GET",
        "/api/clientes",
        cliente.get("/api/clientes", headers=headers),
        200,
    )

    _verificar(
        5,
        "GET",
        f"/api/clientes/{id_cliente}",
        cliente.get(f"/api/clientes/{id_cliente}", headers=headers),
        200,
    )

    _verificar(
        6,
        "PATCH",
        f"/api/clientes/{id_cliente}",
        cliente.patch(
            f"/api/clientes/{id_cliente}",
            headers=headers,
            json={"direccion": "Carrera 4 #5-6"},
        ),
        200,
    )

    _verificar(
        7,
        "POST",
        f"/api/clientes/{id_cliente}/eliminar",
        cliente.post(
            f"/api/clientes/{id_cliente}/eliminar",
            headers=headers,
            json={"contrasena": CLAVE},
        ),
        204,
    )

    _verificar(
        8,
        "GET",
        f"/api/clientes/{id_cliente}",
        cliente.get(f"/api/clientes/{id_cliente}", headers=headers),
        404,
    )


def main() -> int:
    """Ejecuta el CRUD de clientes sobre una base temporal.

    Returns:
        int: 0 si todos los pasos pasan; 1 si alguno falla.
    """
    print("Verificación CRUD del módulo de clientes")
    print("-" * 46)
    try:
        with tempfile.TemporaryDirectory() as carpeta:
            db_path = Path(carpeta) / "smoke.db"
            cargar_seed(db_path)
            cliente, engine = _construir_cliente(db_path)
            try:
                _ejecutar(cliente)
            finally:
                cliente.close()
                engine.dispose()
    except PasoFallidoError as exc:
        print(f"\nFALLO: {exc}")
        return 1

    print("-" * 46)
    print("CRUD de clientes verificado correctamente.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
