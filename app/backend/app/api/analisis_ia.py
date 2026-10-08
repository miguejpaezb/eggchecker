from typing import Annotated

from fastapi import (
    APIRouter,
    Depends,
    File,
    Form,
    HTTPException,
    Query,
    UploadFile,
    status,
)
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session

from app.api.deps import get_current_usuario
from app.core.config import Settings, get_settings
from app.core.database import get_db
from app.models.usuario import Usuario
from app.schemas.analisis_ia import (
    AnalisisResponse,
    EstadoIAResponse,
    RetroalimentacionRequest,
)
from app.services import analisis_ia_service
from app.services.ia_proveedor import ProveedorIA, crear_proveedor

router = APIRouter(tags=["analisis-ia"])


def get_proveedor_ia(settings: Settings = Depends(get_settings)) -> ProveedorIA:
    """Entrega el proveedor de IA según la configuración.

    Se declara como dependencia para que las pruebas lo reemplacen con
    `app.dependency_overrides` sin llamar a internet.
    """
    return crear_proveedor(settings)


@router.get("/analisis-ia/estado", response_model=EstadoIAResponse)
def estado_analisis(
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
    proveedor: ProveedorIA = Depends(get_proveedor_ia),
) -> EstadoIAResponse:
    """Indica si el usuario puede usar el análisis IA y su cupo del día.

    Args:
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.
        settings: Configuración del servidor.
        proveedor: Proveedor de IA activo.

    Returns:
        EstadoIAResponse: Disponibilidad, modo demostración y cupo.
    """
    return analisis_ia_service.obtener_estado(db, usuario, settings, proveedor)


@router.post(
    "/analisis-ia",
    response_model=AnalisisResponse,
    status_code=status.HTTP_201_CREATED,
)
def crear_analisis(
    imagen: Annotated[UploadFile, File()],
    id_camada: int | None = Form(default=None),
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
    proveedor: ProveedorIA = Depends(get_proveedor_ia),
) -> AnalisisResponse:
    """Analiza una foto de huevos y guarda el resultado (RF-33 a RF-36).

    Args:
        imagen: Foto JPG, PNG o WEBP enviada como multipart/form-data.
        id_camada: Camada de origen de los huevos (opcional).
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.
        settings: Configuración del servidor.
        proveedor: Proveedor de IA activo.

    Returns:
        AnalisisResponse: Diagnóstico con anomalías y recomendaciones.
    """
    # Se lee un byte más del máximo para detectar archivos demasiado grandes
    # sin cargar en memoria un archivo enorme.
    contenido = imagen.file.read(settings.IA_MAX_MB * 1024 * 1024 + 1)
    analisis = analisis_ia_service.analizar(
        db, usuario, contenido, id_camada, settings, proveedor
    )
    nombres = analisis_ia_service.nombres_camadas(db, [analisis])
    return analisis_ia_service.a_respuesta(analisis, nombres, settings)


@router.get("/analisis-ia", response_model=list[AnalisisResponse])
def listar_analisis(
    id_camada: int | None = Query(default=None),
    limite: int = Query(default=50, ge=1, le=200),
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
) -> list[AnalisisResponse]:
    """Historial de análisis, opcionalmente de una sola camada (RF-36).

    Args:
        id_camada: Camada por la que filtrar el historial.
        limite: Máximo de registros a devolver.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.
        settings: Configuración del servidor.

    Returns:
        list[AnalisisResponse]: Análisis del más reciente al más antiguo.
    """
    lista = analisis_ia_service.listar(db, usuario, id_camada, limite)
    nombres = analisis_ia_service.nombres_camadas(db, lista)
    return [analisis_ia_service.a_respuesta(a, nombres, settings) for a in lista]


@router.get("/analisis-ia/{id_analisis}", response_model=AnalisisResponse)
def obtener_analisis(
    id_analisis: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
) -> AnalisisResponse:
    """Detalle de un análisis del usuario.

    Args:
        id_analisis: Identificador del análisis.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.
        settings: Configuración del servidor.

    Returns:
        AnalisisResponse: El análisis solicitado.
    """
    analisis = analisis_ia_service.obtener(db, usuario, id_analisis)
    nombres = analisis_ia_service.nombres_camadas(db, [analisis])
    return analisis_ia_service.a_respuesta(analisis, nombres, settings)


@router.get("/analisis-ia/{id_analisis}/imagen")
def imagen_analisis(
    id_analisis: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
) -> FileResponse:
    """Devuelve la foto analizada (solo a su dueño).

    Args:
        id_analisis: Identificador del análisis.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.
        settings: Configuración del servidor.

    Returns:
        FileResponse: La imagen guardada.
    """
    analisis = analisis_ia_service.obtener(db, usuario, id_analisis)
    ruta = analisis_ia_service.ruta_imagen(analisis, settings)
    if ruta is None:
        raise HTTPException(status_code=404, detail="Imagen no disponible")
    return FileResponse(ruta)


@router.patch(
    "/analisis-ia/{id_analisis}/retroalimentacion",
    response_model=AnalisisResponse,
)
def retroalimentar_analisis(
    id_analisis: int,
    datos: RetroalimentacionRequest,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
) -> AnalisisResponse:
    """Registra si el avicultor considera correcto el diagnóstico.

    Args:
        id_analisis: Identificador del análisis.
        datos: Opinión del avicultor.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.
        settings: Configuración del servidor.

    Returns:
        AnalisisResponse: El análisis actualizado.
    """
    analisis = analisis_ia_service.registrar_retroalimentacion(
        db, usuario, id_analisis, datos.diagnostico_correcto
    )
    nombres = analisis_ia_service.nombres_camadas(db, [analisis])
    return analisis_ia_service.a_respuesta(analisis, nombres, settings)


@router.delete("/analisis-ia/{id_analisis}", status_code=status.HTTP_204_NO_CONTENT)
def eliminar_analisis(
    id_analisis: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
) -> None:
    """Elimina un análisis y su foto.

    Args:
        id_analisis: Identificador del análisis.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.
        settings: Configuración del servidor.
    """
    analisis_ia_service.eliminar(db, usuario, id_analisis, settings)
