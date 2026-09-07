from datetime import date, timedelta

from app.core.plans import limite_aves, validar_limite
from app.models.camada import Camada
from app.models.usuario import Usuario
from app.schemas.camada import CamadaCreate, CamadaUpdate, MortalidadRequest
from fastapi import HTTPException
from sqlalchemy import func
from sqlalchemy.orm import Session

# 72 semanas de vida productiva de una ponedora; regla RF-12 a confirmar
# con el PO. Define la fecha de retiro estimada de cada camada.
DIAS_VIDA_PRODUCTIVA = 504


def crear_camada(db: Session, usuario: Usuario, datos: CamadaCreate) -> Camada:
    """Registra una camada nueva si no supera el límite de aves del plan.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la camada.
        datos: Datos validados de la camada a crear.

    Returns:
        Camada: La camada recién creada con cantidad_actual igual a la inicial.

    Raises:
        HTTPException: 400 si el plan del usuario no admite más aves.
    """
    aves_actuales = _sumar_aves_activas(db, usuario.id_usuario)
    maximo = limite_aves(usuario.plan_suscripcion)
    if not validar_limite(aves_actuales, datos.cantidad_inicial, maximo):
        raise HTTPException(status_code=400, detail="Límite de aves del plan alcanzado")

    camada = Camada(
        id_usuario=usuario.id_usuario,
        nombre_camada=datos.nombre_camada,
        fecha_ingreso=datos.fecha_ingreso,
        cantidad_inicial=datos.cantidad_inicial,
        cantidad_actual=datos.cantidad_inicial,
        estado=datos.estado,
    )
    db.add(camada)
    db.commit()
    db.refresh(camada)
    return camada


def listar_camadas(db: Session, usuario: Usuario, estado: str | None) -> list[Camada]:
    """Lista las camadas del usuario, opcionalmente filtradas por estado.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de las camadas.
        estado: Estado por el que filtrar ('activa' o 'retirada').

    Returns:
        list[Camada]: Camadas del usuario que cumplen el filtro.
    """
    consulta = db.query(Camada).filter(Camada.id_usuario == usuario.id_usuario)
    if estado is not None:
        consulta = consulta.filter(Camada.estado == estado)
    return consulta.all()


def obtener_camada(db: Session, usuario: Usuario, id_camada: int) -> Camada:
    """Obtiene una camada del usuario por su identificador.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la camada.
        id_camada: Identificador de la camada a consultar.

    Returns:
        Camada: La camada encontrada.

    Raises:
        HTTPException: 404 si la camada no existe o no es del usuario.
    """
    return _obtener_camada_de_usuario(db, usuario, id_camada)


def actualizar_camada(
    db: Session, usuario: Usuario, id_camada: int, datos: CamadaUpdate
) -> Camada:
    """Aplica los cambios enviados a una camada del usuario.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la camada.
        id_camada: Identificador de la camada a actualizar.
        datos: Campos que se desean modificar.

    Returns:
        Camada: La camada con los cambios aplicados.

    Raises:
        HTTPException: 404 si la camada no existe o no es del usuario;
            400 si la cantidad_actual resultante queda fuera del rango
            permitido (0 ≤ cantidad_actual ≤ cantidad_inicial).
    """
    camada = _obtener_camada_de_usuario(db, usuario, id_camada)
    for campo, valor in datos.model_dump(exclude_unset=True).items():
        if valor is not None:
            setattr(camada, campo, valor)
    if not 0 <= camada.cantidad_actual <= camada.cantidad_inicial:
        raise HTTPException(
            status_code=400,
            detail="La cantidad actual debe estar entre 0 y la cantidad inicial",
        )
    db.commit()
    db.refresh(camada)
    return camada


def retirar_camada(db: Session, usuario: Usuario, id_camada: int) -> Camada:
    """Cierra el ciclo de una camada activa cambiando su estado a retirada.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la camada.
        id_camada: Identificador de la camada a retirar.

    Returns:
        Camada: La camada con estado 'retirada'.

    Raises:
        HTTPException: 404 si la camada no existe o no es del usuario;
            400 si la camada ya estaba retirada.
    """
    camada = _obtener_camada_de_usuario(db, usuario, id_camada)
    if camada.estado == "retirada":
        raise HTTPException(status_code=400, detail="La camada ya está retirada")
    camada.estado = "retirada"
    db.commit()
    db.refresh(camada)
    return camada


def registrar_mortalidad(
    db: Session,
    usuario: Usuario,
    id_camada: int,
    datos: MortalidadRequest,
) -> Camada:
    """Descuenta la mortalidad informada de la cantidad actual de una camada.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la camada.
        id_camada: Identificador de la camada afectada.
        datos: Cantidad de aves muertas a descontar.

    Returns:
        Camada: La camada con la cantidad_actual ya descontada.

    Raises:
        HTTPException: 404 si la camada no existe o no es del usuario;
            400 si la mortalidad excede la cantidad actual.
    """
    camada = _obtener_camada_de_usuario(db, usuario, id_camada)
    if camada.cantidad_actual - datos.cantidad < 0:
        raise HTTPException(
            status_code=400,
            detail="La mortalidad excede la cantidad actual",
        )
    camada.cantidad_actual -= datos.cantidad
    db.commit()
    db.refresh(camada)
    return camada


def calcular_edad_dias(camada: Camada) -> int:
    """Calcula los días transcurridos desde el ingreso de la camada.

    Args:
        camada: Camada de la que se calcula la edad.

    Returns:
        int: Días entre la fecha de ingreso y hoy.
    """
    return (date.today() - camada.fecha_ingreso).days


def calcular_fecha_retiro(camada: Camada) -> date:
    """Calcula la fecha estimada de retiro de la camada.

    Args:
        camada: Camada de la que se calcula el retiro.

    Returns:
        date: Fecha de ingreso más los días de vida productiva.
    """
    return camada.fecha_ingreso + timedelta(days=DIAS_VIDA_PRODUCTIVA)


def _obtener_camada_de_usuario(db: Session, usuario: Usuario, id_camada: int) -> Camada:
    """Busca una camada que pertenezca al usuario o lanza 404."""
    camada = (
        db.query(Camada)
        .filter(
            Camada.id_camada == id_camada,
            Camada.id_usuario == usuario.id_usuario,
        )
        .first()
    )
    if camada is None:
        raise HTTPException(status_code=404, detail="Camada no encontrada")
    return camada


def _sumar_aves_activas(db: Session, id_usuario: int) -> int:
    """Suma las aves de las camadas activas de un usuario.

    Regla RF-06: aves_actuales = SUM(cantidad_actual) de las camadas con
    estado 'activa', usado para validar el límite del plan al crear camada.
    """
    total = (
        db.query(func.coalesce(func.sum(Camada.cantidad_actual), 0))
        .filter(
            Camada.id_usuario == id_usuario,
            Camada.estado == "activa",
        )
        .scalar()
    )
    return int(total or 0)
