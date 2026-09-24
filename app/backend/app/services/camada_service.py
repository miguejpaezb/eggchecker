from datetime import date, timedelta

from app.core.plans import limite_aves, validar_limite
from app.models.camada import (
    DIAS_AVISO,
    DIAS_VIDA_PRODUCTIVA,
    EDAD_DECISION_SEMANAS,
    EDAD_INICIAL_SEMANAS,
    Camada,
)
from app.models.evento_sanitario import EventoSanitario
from app.models.usuario import Usuario
from app.schemas.camada import CamadaCreate, CamadaUpdate, MortalidadRequest
from fastapi import HTTPException
from sqlalchemy import func, or_
from sqlalchemy.orm import Session


def crear_camada(db: Session, usuario: Usuario, datos: CamadaCreate) -> Camada:
    """Registra una camada nueva si cumple las reglas de negocio.

    La fecha de ingreso solo puede ser hoy o ayer y toda camada nueva
    ingresa con 16 semanas de vida (regla de negocio).

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la camada.
        datos: Datos validados de la camada a crear.

    Returns:
        Camada: La camada recién creada con cantidad_actual igual a la inicial.

    Raises:
        HTTPException: 400 si la fecha de ingreso no es hoy/ayer o si el
            plan del usuario no admite más aves.
    """
    _validar_fecha_ingreso(datos.fecha_ingreso)

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
        edad_semanas=EDAD_INICIAL_SEMANAS,
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


def listar_alertas(db: Session, usuario: Usuario) -> list[Camada]:
    """Lista las camadas activas que piden decisión al avicultor.

    Una camada pide decisión cuando alcanzó la vida productiva y ya venció
    (o no tiene) su próximo aviso semanal.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de las camadas.

    Returns:
        list[Camada]: Camadas activas con decisión pendiente.
    """
    hoy = date.today()
    return (
        db.query(Camada)
        .filter(
            Camada.id_usuario == usuario.id_usuario,
            Camada.estado == "activa",
            Camada.edad_semanas >= EDAD_DECISION_SEMANAS,
            or_(
                Camada.fecha_proximo_aviso.is_(None),
                Camada.fecha_proximo_aviso <= hoy,
            ),
        )
        .all()
    )


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
    """Edita el nombre y, dentro de 24 horas, la cantidad inicial.

    El nombre se puede cambiar siempre que la camada esté activa. La
    cantidad inicial solo se puede cambiar dentro de las primeras 24 horas
    desde su creación: la diferencia respecto al valor guardado se suma
    (o resta) tanto a la cantidad inicial como a la cantidad actual.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la camada.
        id_camada: Identificador de la camada a actualizar.
        datos: Campos editables de la camada.

    Returns:
        Camada: La camada con los cambios aplicados.

    Raises:
        HTTPException: 404 si la camada no es del usuario; 400 si no está
            activa, si la ventana de 24 horas expiró, si el ajuste dejaría
            la cantidad actual en negativo o si se excede el límite de aves
            del plan.
    """
    camada = _obtener_camada_de_usuario(db, usuario, id_camada)
    if camada.estado != "activa":
        raise HTTPException(
            status_code=400,
            detail="Solo se pueden editar camadas activas",
        )

    cambios = datos.model_dump(exclude_unset=True)

    if cambios.get("cantidad_inicial") is not None:
        _ajustar_cantidad_inicial(db, usuario, camada, cambios["cantidad_inicial"])

    if cambios.get("nombre_camada") is not None:
        camada.nombre_camada = cambios["nombre_camada"]

    db.commit()
    db.refresh(camada)
    return camada


def _ajustar_cantidad_inicial(
    db: Session, usuario: Usuario, camada: Camada, nueva_inicial: int
) -> None:
    """Aplica el ajuste por diferencia a las cantidades de la camada.

    Valida la ventana de 24 horas, evita cantidades actuales negativas y
    respeta el límite de aves del plan.
    """
    if not camada.puede_editar_inicial:
        raise HTTPException(
            status_code=400,
            detail=(
                "La cantidad inicial no se puede editar: la camada se "
                "registró hace más de 24 horas"
            ),
        )

    diferencia = nueva_inicial - camada.cantidad_inicial
    nueva_actual = camada.cantidad_actual + diferencia
    if nueva_actual < 0:
        raise HTTPException(
            status_code=400,
            detail="El ajuste dejaría la cantidad actual en negativo",
        )

    _validar_limite_plan(db, usuario, camada, nueva_actual)
    camada.cantidad_inicial = nueva_inicial
    camada.cantidad_actual = nueva_actual


def avanzar_semana(db: Session, usuario: Usuario, id_camada: int) -> Camada:
    """Suma una semana de vida a una camada activa de forma manual.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la camada.
        id_camada: Identificador de la camada a avanzar.

    Returns:
        Camada: La camada con una semana más de edad.

    Raises:
        HTTPException: 404 si la camada no es del usuario; 400 si no está
            activa.
    """
    camada = _obtener_camada_de_usuario(db, usuario, id_camada)
    if camada.estado != "activa":
        raise HTTPException(
            status_code=400,
            detail="Solo se puede avanzar semana en camadas activas",
        )
    camada.edad_semanas += 1
    db.commit()
    db.refresh(camada)
    return camada


def seguir_activa(db: Session, usuario: Usuario, id_camada: int) -> Camada:
    """Posponer una semana la decisión de una camada que pide aviso.

    Registra el próximo aviso a 7 días; la camada sigue activa y volverá a
    pedir decisión la próxima semana.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la camada.
        id_camada: Identificador de la camada que sigue activa.

    Returns:
        Camada: La camada con el próximo aviso agendado.

    Raises:
        HTTPException: 404 si la camada no es del usuario; 400 si no está
            activa o si no pide decisión.
    """
    camada = _obtener_camada_de_usuario(db, usuario, id_camada)
    if camada.estado != "activa":
        raise HTTPException(
            status_code=400,
            detail="Solo se puede continuar una camada activa",
        )
    if not camada.requiere_decision:
        raise HTTPException(
            status_code=400,
            detail="La camada no requiere decisión",
        )
    camada.fecha_proximo_aviso = date.today() + timedelta(days=DIAS_AVISO)
    db.commit()
    db.refresh(camada)
    return camada


def descartar_camada(db: Session, usuario: Usuario, id_camada: int) -> Camada:
    """Descarta una camada activa cambiando su estado a retirada.

    La acción es irreversible: una camada descartada no puede reactivarse.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la camada.
        id_camada: Identificador de la camada a descartar.

    Returns:
        Camada: La camada con estado 'retirada'.

    Raises:
        HTTPException: 404 si la camada no es del usuario; 400 si ya estaba
            retirada.
    """
    camada = _obtener_camada_de_usuario(db, usuario, id_camada)
    if camada.estado == "retirada":
        raise HTTPException(
            status_code=400,
            detail="La camada ya está retirada y no se puede reactivar",
        )
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
    """Registra mortalidad, descuenta aves y deja el evento en el historial.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de la camada.
        id_camada: Identificador de la camada afectada.
        datos: Cantidad de aves muertas a descontar.

    Returns:
        Camada: La camada con la cantidad_actual ya descontada.

    Raises:
        HTTPException: 404 si la camada no es del usuario; 400 si la
            mortalidad excede la cantidad actual o si la camada no está
            activa.
    """
    camada = _obtener_camada_de_usuario(db, usuario, id_camada)
    if camada.estado != "activa":
        raise HTTPException(
            status_code=400,
            detail="Solo se puede registrar mortalidad en camadas activas",
        )
    if datos.cantidad > camada.cantidad_actual:
        raise HTTPException(
            status_code=400,
            detail="La mortalidad excede la cantidad actual",
        )

    camada.cantidad_actual -= datos.cantidad
    evento = EventoSanitario(
        id_camada=camada.id_camada,
        tipo_evento="mortalidad",
        fecha_evento=date.today(),
        descripcion=f"Mortalidad registrada: {datos.cantidad} aves",
        mortalidad=datos.cantidad,
    )
    db.add(evento)
    db.commit()
    db.refresh(camada)
    return camada


def calcular_edad_dias(camada: Camada) -> int:
    """Calcula la edad de la camada en días a partir de sus semanas.

    Args:
        camada: Camada de la que se calcula la edad.

    Returns:
        int: Edad en días (semanas de vida multiplicadas por 7).
    """
    return camada.edad_semanas * 7


def calcular_fecha_retiro(camada: Camada) -> date:
    """Calcula la fecha estimada de retiro a las 72 semanas de vida.

    Es solo una referencia informativa: la camada nunca se retira sola y
    puede extenderse más allá de la vida productiva.

    Args:
        camada: Camada de la que se calcula el retiro.

    Returns:
        date: Hoy más los días de vida productiva restantes (mínimo hoy).
    """
    dias_restantes = max(0, DIAS_VIDA_PRODUCTIVA - camada.edad_semanas * 7)
    return date.today() + timedelta(days=dias_restantes)


def _validar_fecha_ingreso(fecha_ingreso: date) -> None:
    """Valida que la fecha de ingreso sea hoy o ayer.

    Evita registrar camadas con fechas antiguas (más de un día) o futuras.
    """
    hoy = date.today()
    if fecha_ingreso not in (hoy, hoy - timedelta(days=1)):
        raise HTTPException(
            status_code=400,
            detail="La fecha de ingreso solo puede ser hoy o ayer",
        )


def _validar_limite_plan(
    db: Session, usuario: Usuario, camada: Camada, nueva_aves: int
) -> None:
    """Valida que el nuevo total de aves de la camada no exceda el plan."""
    aves_otras = _sumar_aves_activas(db, usuario.id_usuario) - camada.cantidad_actual
    maximo = limite_aves(usuario.plan_suscripcion)
    if not validar_limite(aves_otras, nueva_aves, maximo):
        raise HTTPException(status_code=400, detail="Límite de aves del plan alcanzado")


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
    estado 'activa', usado para validar el límite del plan.
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
