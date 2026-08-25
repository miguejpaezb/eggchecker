from fastapi import APIRouter

router = APIRouter(tags=["health"])


@router.get("/health")
def health_check() -> dict[str, str]:
    """Comprueba que la API está operativa.

    Endpoint de sondeo que verifica que el servidor arrancó sin depender
    de la base de datos.

    Returns:
        dict[str, str]: Estado de la aplicación.
    """
    return {"status": "ok"}
