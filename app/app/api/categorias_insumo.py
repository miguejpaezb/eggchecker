from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_usuario
from app.core.database import get_db
from app.models.usuario import Usuario
from app.schemas.inventario import (
    CategoriaCreate,
    CategoriaResponse,
    CategoriaUpdate,
)
from app.services import categoria_service

router = APIRouter(tags=["categorias-insumo"])


@router.post(
    "/categorias-insumo",
    response_model=CategoriaResponse,
    status_code=status.HTTP_201_CREATED,
)
def crear_categoria(
    datos: CategoriaCreate,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> CategoriaResponse:
    """Registra una categoría de insumo en el catálogo global.

    Args:
        datos: Datos validados de la categoría a crear.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        CategoriaResponse: La categoría creada.
    """
    return categoria_service.crear_categoria(db, datos)


@router.get("/categorias-insumo", response_model=list[CategoriaResponse])
def listar_categorias(
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> list[CategoriaResponse]:
    """Lista las categorías del catálogo global de insumos.

    Args:
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        list[CategoriaResponse]: Categorías ordenadas por nombre.
    """
    return categoria_service.listar_categorias(db)


@router.get("/categorias-insumo/{id_categoria}", response_model=CategoriaResponse)
def obtener_categoria(
    id_categoria: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> CategoriaResponse:
    """Obtiene una categoría del catálogo por su identificador.

    Args:
        id_categoria: Identificador de la categoría a consultar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        CategoriaResponse: La categoría encontrada.
    """
    return categoria_service.obtener_categoria(db, id_categoria)


@router.patch("/categorias-insumo/{id_categoria}", response_model=CategoriaResponse)
def actualizar_categoria(
    id_categoria: int,
    datos: CategoriaUpdate,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> CategoriaResponse:
    """Actualiza los campos enviados de una categoría del catálogo.

    Args:
        id_categoria: Identificador de la categoría a actualizar.
        datos: Campos que se desean modificar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.

    Returns:
        CategoriaResponse: La categoría actualizada.
    """
    return categoria_service.actualizar_categoria(db, id_categoria, datos)


@router.delete(
    "/categorias-insumo/{id_categoria}",
    status_code=status.HTTP_204_NO_CONTENT,
)
def eliminar_categoria(
    id_categoria: int,
    usuario: Usuario = Depends(get_current_usuario),
    db: Session = Depends(get_db),
) -> None:
    """Elimina una categoría que no tenga insumos asociados.

    Args:
        id_categoria: Identificador de la categoría a eliminar.
        usuario: Usuario autenticado mediante JWT.
        db: Sesión de base de datos.
    """
    categoria_service.eliminar_categoria(db, id_categoria)
