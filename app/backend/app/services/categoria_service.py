from app.models.categoria_insumo import CategoriaInsumo
from app.models.insumo import Insumo
from app.schemas.inventario import CategoriaCreate, CategoriaUpdate
from fastapi import HTTPException
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session


def crear_categoria(db: Session, datos: CategoriaCreate) -> CategoriaInsumo:
    """Registra una categoría nueva en el catálogo global de insumos.

    Args:
        db: Sesión de base de datos.
        datos: Datos validados de la categoría a crear.

    Returns:
        CategoriaInsumo: La categoría recién creada.

    Raises:
        HTTPException: 409 si el nombre de la categoría ya existe.
    """
    if _buscar_por_nombre(db, datos.nombre_categ) is not None:
        raise HTTPException(status_code=409, detail="La categoría ya existe")

    categoria = CategoriaInsumo(
        nombre_categ=datos.nombre_categ,
        descripcion=datos.descripcion,
    )
    db.add(categoria)
    try:
        db.commit()
    except IntegrityError as exc:
        # Carrera por nombre duplicado entre dos creaciones simultáneas.
        db.rollback()
        raise HTTPException(status_code=409, detail="La categoría ya existe") from exc
    db.refresh(categoria)
    return categoria


def listar_categorias(db: Session) -> list[CategoriaInsumo]:
    """Lista todas las categorías del catálogo ordenadas por nombre.

    Args:
        db: Sesión de base de datos.

    Returns:
        list[CategoriaInsumo]: Categorías del catálogo global.
    """
    return db.query(CategoriaInsumo).order_by(CategoriaInsumo.nombre_categ).all()


def obtener_categoria(db: Session, id_categoria: int) -> CategoriaInsumo:
    """Obtiene una categoría del catálogo por su identificador.

    Args:
        db: Sesión de base de datos.
        id_categoria: Identificador de la categoría a consultar.

    Returns:
        CategoriaInsumo: La categoría encontrada.

    Raises:
        HTTPException: 404 si la categoría no existe.
    """
    categoria = db.get(CategoriaInsumo, id_categoria)
    if categoria is None:
        raise HTTPException(status_code=404, detail="Categoría no encontrada")
    return categoria


def actualizar_categoria(
    db: Session, id_categoria: int, datos: CategoriaUpdate
) -> CategoriaInsumo:
    """Aplica los cambios enviados a una categoría del catálogo.

    Args:
        db: Sesión de base de datos.
        id_categoria: Identificador de la categoría a actualizar.
        datos: Campos que se desean modificar.

    Returns:
        CategoriaInsumo: La categoría con los cambios aplicados.

    Raises:
        HTTPException: 404 si la categoría no existe;
            409 si el nombre nuevo ya pertenece a otra categoría.
    """
    categoria = obtener_categoria(db, id_categoria)
    campos = datos.model_dump(exclude_unset=True)
    if (
        "nombre_categ" in campos
        and campos["nombre_categ"] != categoria.nombre_categ
        and _buscar_por_nombre(db, campos["nombre_categ"]) is not None
    ):
        raise HTTPException(status_code=409, detail="La categoría ya existe")
    for campo, valor in campos.items():
        setattr(categoria, campo, valor)
    db.commit()
    db.refresh(categoria)
    return categoria


def eliminar_categoria(db: Session, id_categoria: int) -> None:
    """Elimina una categoría siempre que no tenga insumos asociados.

    Args:
        db: Sesión de base de datos.
        id_categoria: Identificador de la categoría a eliminar.

    Raises:
        HTTPException: 404 si la categoría no existe;
            409 si tiene insumos asociados (FK RESTRICT).
    """
    categoria = obtener_categoria(db, id_categoria)
    tiene_insumos = (
        db.query(Insumo.id_insumo).filter(Insumo.id_categoria == id_categoria).first()
    )
    if tiene_insumos is not None:
        raise HTTPException(
            status_code=409, detail="No se puede eliminar: tiene insumos asociados"
        )
    db.delete(categoria)
    db.commit()


def _buscar_por_nombre(db: Session, nombre: str) -> CategoriaInsumo | None:
    """Busca una categoría por su nombre exacto en el catálogo."""
    return (
        db.query(CategoriaInsumo).filter(CategoriaInsumo.nombre_categ == nombre).first()
    )
