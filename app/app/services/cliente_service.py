from app.core.plans import limite_clientes, validar_limite
from app.core.security import verificar_hash
from app.models.cliente import Cliente
from app.models.usuario import Usuario
from app.schemas.ventas import ClienteCreate, ClienteUpdate
from fastapi import HTTPException
from sqlalchemy import func, text
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session


def crear_cliente(db: Session, usuario: Usuario, datos: ClienteCreate) -> Cliente:
    """Registra un cliente nuevo propiedad del usuario autenticado.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del cliente.
        datos: Datos validados del cliente a crear.

    Returns:
        Cliente: El cliente recién creado y activo.

    Raises:
        HTTPException: 400 si el plan del usuario ya alcanzó su límite de
            clientes.
    """
    clientes_actuales = _contar_clientes(db, usuario.id_usuario)
    maximo = limite_clientes(usuario.plan_suscripcion)
    if not validar_limite(clientes_actuales, 1, maximo):
        raise HTTPException(
            status_code=400, detail="Límite de clientes del plan alcanzado"
        )

    cliente = Cliente(
        id_usuario=usuario.id_usuario,
        nombre_cliente=datos.nombre_cliente,
        telefono=datos.telefono,
        direccion=datos.direccion,
    )
    db.add(cliente)
    db.commit()
    db.refresh(cliente)
    return cliente


def listar_clientes(
    db: Session, usuario: Usuario, solo_activos: bool = True
) -> list[Cliente]:
    """Lista los clientes del usuario, opcionalmente solo los activos.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño de los clientes.
        solo_activos: Si True, excluye los clientes suspendidos.

    Returns:
        list[Cliente]: Clientes del usuario que cumplen el filtro.
    """
    consulta = db.query(Cliente).filter(Cliente.id_usuario == usuario.id_usuario)
    if solo_activos:
        consulta = consulta.filter(Cliente.activo.is_(True))
    return consulta.order_by(Cliente.nombre_cliente).all()


def obtener_cliente(db: Session, usuario: Usuario, id_cliente: int) -> Cliente:
    """Obtiene un cliente del usuario por su identificador.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del cliente.
        id_cliente: Identificador del cliente a consultar.

    Returns:
        Cliente: El cliente encontrado.

    Raises:
        HTTPException: 404 si el cliente no existe o no es del usuario.
    """
    return _obtener_cliente_de_usuario(db, usuario, id_cliente)


def actualizar_cliente(
    db: Session, usuario: Usuario, id_cliente: int, datos: ClienteUpdate
) -> Cliente:
    """Aplica los cambios enviados a un cliente del usuario.

    Solo se editan el nombre, el teléfono y la dirección.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del cliente.
        id_cliente: Identificador del cliente a actualizar.
        datos: Campos que se desean modificar.

    Returns:
        Cliente: El cliente con los cambios aplicados.

    Raises:
        HTTPException: 404 si el cliente no existe o no es del usuario.
    """
    cliente = _obtener_cliente_de_usuario(db, usuario, id_cliente)
    campos = datos.model_dump(exclude_unset=True)
    for campo, valor in campos.items():
        if valor is not None:
            setattr(cliente, campo, valor)
    db.commit()
    db.refresh(cliente)
    return cliente


def suspender_cliente(db: Session, usuario: Usuario, id_cliente: int) -> Cliente:
    """Suspende un cliente (activo = False) de forma reversible.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del cliente.
        id_cliente: Identificador del cliente a suspender.

    Returns:
        Cliente: El cliente con activo en False.

    Raises:
        HTTPException: 404 si el cliente no existe o no es del usuario.
    """
    cliente = _obtener_cliente_de_usuario(db, usuario, id_cliente)
    cliente.activo = False
    db.commit()
    db.refresh(cliente)
    return cliente


def activar_cliente(db: Session, usuario: Usuario, id_cliente: int) -> Cliente:
    """Reactiva un cliente previamente suspendido.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del cliente.
        id_cliente: Identificador del cliente a activar.

    Returns:
        Cliente: El cliente con activo en True.

    Raises:
        HTTPException: 404 si el cliente no existe o no es del usuario.
    """
    cliente = _obtener_cliente_de_usuario(db, usuario, id_cliente)
    cliente.activo = True
    db.commit()
    db.refresh(cliente)
    return cliente


def eliminar_cliente(
    db: Session, usuario: Usuario, id_cliente: int, contrasena: str
) -> None:
    """Borra un cliente de forma permanente tras confirmar la contraseña.

    La eliminación es física e irreversible. Para completarla el usuario
    debe confirmar con su propia contraseña.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario dueño del cliente.
        id_cliente: Identificador del cliente a eliminar.
        contrasena: Contraseña del usuario para confirmar el borrado.

    Raises:
        HTTPException: 404 si el cliente no existe o no es del usuario;
            401 si la contraseña no coincide; 409 si el cliente tiene
            pedidos asociados (integridad referencial).
    """
    cliente = _obtener_cliente_de_usuario(db, usuario, id_cliente)
    if not verificar_hash(contrasena, usuario.contrasena_hash):
        raise HTTPException(status_code=401, detail="Contraseña incorrecta")
    if _contar_pedidos(db, id_cliente) > 0:
        raise HTTPException(
            status_code=409,
            detail="No se puede eliminar: el cliente tiene pedidos asociados",
        )
    db.delete(cliente)
    try:
        db.commit()
    except IntegrityError as exc:
        # Respaldo por si la FK (ON DELETE RESTRICT) se evalúa en la BD.
        db.rollback()
        raise HTTPException(
            status_code=409,
            detail="No se puede eliminar: el cliente tiene pedidos asociados",
        ) from exc


def _obtener_cliente_de_usuario(
    db: Session, usuario: Usuario, id_cliente: int
) -> Cliente:
    """Busca un cliente que pertenezca al usuario o lanza 404."""
    cliente = (
        db.query(Cliente)
        .filter(
            Cliente.id_cliente == id_cliente,
            Cliente.id_usuario == usuario.id_usuario,
        )
        .first()
    )
    if cliente is None:
        raise HTTPException(status_code=404, detail="Cliente no encontrado")
    return cliente


def _contar_clientes(db: Session, id_usuario: int) -> int:
    """Cuenta los clientes registrados por el usuario (activos o no)."""
    total = (
        db.query(func.count(Cliente.id_cliente))
        .filter(Cliente.id_usuario == id_usuario)
        .scalar()
    )
    return int(total or 0)


def _contar_pedidos(db: Session, id_cliente: int) -> int:
    """Cuenta los pedidos asociados a un cliente.

    Se usa SQL crudo porque el modelo ORM de `pedido` pertenece a la
    siguiente parte del módulo de ventas y aún no existe.
    """
    consulta = text("SELECT COUNT(*) FROM pedido WHERE id_cliente = :cid")
    resultado = db.execute(consulta, {"cid": id_cliente}).scalar()
    return int(resultado or 0)
