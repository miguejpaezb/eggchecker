from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.auth import router as auth_router
from app.api.camadas import router as camadas_router
from app.api.health import router as health_router
from app.api.usuarios import router as usuarios_router


def create_app() -> FastAPI:
    """Crea la instancia de la aplicación FastAPI.

    Configura los orígenes CORS permitidos para el frontend en desarrollo
    y registra los routers de la API bajo el prefijo /api.

    Returns:
        FastAPI: La aplicación lista para servir por uvicorn.
    """
    app = FastAPI(title="EggChecker API", version="0.1.0")

    # El shell React en desarrollo es el único consumidor directo de la API.
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["http://localhost:5173"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    app.include_router(health_router, prefix="/api")
    app.include_router(auth_router, prefix="/api")
    app.include_router(usuarios_router, prefix="/api")
    app.include_router(camadas_router, prefix="/api")

    return app


app = create_app()
