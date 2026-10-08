"""Lógica del módulo de análisis de huevos con IA (RF-33 a RF-36, CU-06)."""

import json
import uuid
from datetime import date, datetime, time, timedelta
from pathlib import Path

from app.core.config import Settings
from app.core.plans import ia_incluida
from app.models.analisis_ia import AnalisisIA
from app.models.camada import Camada
from app.models.produccion_diaria import ProduccionDiaria
from app.models.usuario import Usuario
from app.schemas.analisis_ia import (
    AnalisisResponse,
    AnomaliaIA,
    DiagnosticoIA,
    DistribucionTipos,
    EstadoIAResponse,
)
from app.services import camada_service
from app.services.ia_proveedor import ProveedorIA, ProveedorIAError
from app.services.recomendaciones_ia import ContextoGranja, generar_recomendaciones
from fastapi import HTTPException
from sqlalchemy import func
from sqlalchemy.orm import Session

# Firmas binarias de los formatos aceptados: (prefijo, desplazamiento, MIME).
_FIRMAS = (
    (b"\xff\xd8\xff", 0, "image/jpeg"),
    (b"\x89PNG\r\n\x1a\n", 0, "image/png"),
    (b"WEBP", 8, "image/webp"),
)
_EXTENSION = {"image/jpeg": "jpg", "image/png": "png", "image/webp": "webp"}

# Días de producción con los que se calcula la postura reciente.
DIAS_POSTURA = 7

MENSAJE_SOLO_PREMIUM = (
    "El análisis de huevos con IA está disponible en el plan Premium."
)


def verificar_acceso(usuario: Usuario) -> None:
    """Exige el plan Premium (CU-06: actor Usuario Premium).

    Raises:
        HTTPException: 403 si el plan del usuario no incluye IA.
    """
    if not ia_incluida(usuario.plan_suscripcion):
        raise HTTPException(status_code=403, detail=MENSAJE_SOLO_PREMIUM)


def contar_analisis_hoy(db: Session, usuario: Usuario) -> int:
    """Cuenta los análisis que el usuario hizo hoy."""
    inicio = datetime.combine(date.today(), time.min)
    return (
        db.query(func.count(AnalisisIA.id_analisis))
        .filter(
            AnalisisIA.id_usuario == usuario.id_usuario,
            AnalisisIA.fecha_analisis >= inicio,
        )
        .scalar()
        or 0
    )


def obtener_estado(
    db: Session, usuario: Usuario, settings: Settings, proveedor: ProveedorIA
) -> EstadoIAResponse:
    """Informa si el usuario puede analizar fotos y con qué proveedor.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario autenticado.
        settings: Configuración del servidor.
        proveedor: Proveedor de IA activo.

    Returns:
        EstadoIAResponse: Disponibilidad, modo y cupo del día.
    """
    disponible = ia_incluida(usuario.plan_suscripcion)
    usados = contar_analisis_hoy(db, usuario) if disponible else 0
    if not disponible:
        mensaje = MENSAJE_SOLO_PREMIUM
    elif proveedor.es_demo:
        mensaje = (
            "Modo demostración: el servidor no tiene configurada la llave de "
            "Gemini y devolverá un diagnóstico de ejemplo."
        )
    else:
        mensaje = "Toma una foto de los huevos sobre la bandeja, con buena luz."
    return EstadoIAResponse(
        disponible=disponible,
        plan=usuario.plan_suscripcion,
        modo_demo=proveedor.es_demo,
        proveedor=proveedor.nombre,
        limite_diario=settings.IA_LIMITE_DIARIO,
        usados_hoy=usados,
        mensaje=mensaje,
    )


def validar_imagen(contenido: bytes, max_mb: int) -> str:
    """Comprueba tamaño y formato real de la foto (no el que dice el cliente).

    Args:
        contenido: Bytes recibidos.
        max_mb: Tamaño máximo en megabytes.

    Returns:
        str: Tipo MIME detectado.

    Raises:
        HTTPException: 400 si está vacía; 413 si es muy grande; 415 si no
            es JPEG, PNG o WEBP.
    """
    if not contenido:
        raise HTTPException(status_code=400, detail="La imagen está vacía")
    if len(contenido) > max_mb * 1024 * 1024:
        raise HTTPException(
            status_code=413, detail=f"La imagen supera el máximo de {max_mb} MB"
        )
    for firma, desplazamiento, mime in _FIRMAS:
        if contenido[desplazamiento : desplazamiento + len(firma)] == firma:
            return mime
    raise HTTPException(
        status_code=415, detail="Formato no admitido: usa una foto JPG, PNG o WEBP"
    )


def _contexto(db: Session, usuario: Usuario, camada: Camada | None) -> ContextoGranja:
    """Reúne los datos de la camada que personalizan las recomendaciones."""
    if camada is None:
        return ContextoGranja()

    desde = date.today() - timedelta(days=DIAS_POSTURA)
    filas = (
        db.query(ProduccionDiaria.fecha_recoleccion, ProduccionDiaria.total_huevos)
        .filter(
            ProduccionDiaria.id_usuario == usuario.id_usuario,
            ProduccionDiaria.id_camada == camada.id_camada,
            ProduccionDiaria.fecha_recoleccion >= desde,
        )
        .all()
    )
    postura = None
    if filas and camada.cantidad_actual > 0:
        dias = len({f.fecha_recoleccion for f in filas})
        total = sum(f.total_huevos for f in filas)
        postura = total / dias / camada.cantidad_actual

    return ContextoGranja(
        nombre_camada=camada.nombre_camada,
        edad_semanas=camada.edad_semanas,
        aves_actuales=camada.cantidad_actual,
        postura_promedio=postura,
    )


def _texto_diagnostico(diagnostico: DiagnosticoIA) -> str:
    """Resumen legible, en el mismo formato de los registros históricos."""
    total = diagnostico.huevos_detectados
    if not diagnostico.es_imagen_de_huevos or total == 0:
        return diagnostico.resumen
    partes = []
    for clave in ("AA", "A", "B", "No_apto"):
        cantidad = getattr(diagnostico.distribucion, clave)
        if cantidad:
            partes.append(f"{round(cantidad * 100 / total)}% {clave}")
    distribucion = f": {', '.join(partes)}" if partes else ""
    return f"Muestra {total} huevos{distribucion}. {diagnostico.resumen}"


def _carpeta_base(settings: Settings) -> Path:
    """Carpeta raíz donde se guardan las fotos."""
    return Path(settings.IA_UPLOAD_DIR).resolve()


def _guardar_imagen(
    contenido: bytes, mime: str, usuario: Usuario, settings: Settings
) -> str:
    """Guarda la foto y devuelve su ruta relativa a la carpeta base."""
    relativa = f"u{usuario.id_usuario}/{uuid.uuid4().hex}.{_EXTENSION[mime]}"
    destino = _carpeta_base(settings) / relativa
    destino.parent.mkdir(parents=True, exist_ok=True)
    destino.write_bytes(contenido)
    return relativa


def ruta_imagen(analisis: AnalisisIA, settings: Settings) -> Path | None:
    """Ubica el archivo de la foto, si se guardó en este servidor.

    Los registros del seed traen URLs externas de ejemplo: para ellos no
    hay archivo local. También se descarta cualquier ruta que intente
    salir de la carpeta base.
    """
    if "://" in analisis.imagen_url:
        return None
    base = _carpeta_base(settings)
    ruta = (base / analisis.imagen_url).resolve()
    if base not in ruta.parents or not ruta.is_file():
        return None
    return ruta


def analizar(
    db: Session,
    usuario: Usuario,
    contenido: bytes,
    id_camada: int | None,
    settings: Settings,
    proveedor: ProveedorIA,
) -> AnalisisIA:
    """Analiza la foto, personaliza las recomendaciones y guarda el historial.

    Implementa RF-33 (cargar imagen), RF-34 (diagnóstico), RF-35
    (recomendaciones personalizadas) y RF-36 (historial por camada).

    Args:
        db: Sesión de base de datos.
        usuario: Usuario autenticado.
        contenido: Bytes de la foto.
        id_camada: Camada a la que pertenecen los huevos (opcional).
        settings: Configuración del servidor.
        proveedor: Motor de diagnóstico.

    Returns:
        AnalisisIA: El análisis guardado.

    Raises:
        HTTPException: 403 sin plan Premium; 429 si se alcanzó el límite
            diario; 400/413/415 si la imagen no es válida; 404 si la
            camada no es del usuario; 422 si la foto no muestra huevos;
            503 si la IA no responde.
    """
    verificar_acceso(usuario)
    if contar_analisis_hoy(db, usuario) >= settings.IA_LIMITE_DIARIO:
        raise HTTPException(
            status_code=429,
            detail=(
                f"Alcanzaste el límite de {settings.IA_LIMITE_DIARIO} análisis "
                "por día. Intenta de nuevo mañana."
            ),
        )
    mime = validar_imagen(contenido, settings.IA_MAX_MB)
    camada = (
        camada_service.obtener_camada(db, usuario, id_camada)
        if id_camada is not None
        else None
    )

    try:
        diagnostico = proveedor.diagnosticar(contenido, mime)
    except ProveedorIAError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

    if not diagnostico.es_imagen_de_huevos:
        raise HTTPException(
            status_code=422,
            detail=(
                "No se reconocen huevos en la foto. Toma la foto de cerca, con "
                "buena luz y con los huevos sobre la bandeja."
            ),
        )

    recomendaciones = generar_recomendaciones(
        diagnostico, _contexto(db, usuario, camada)
    )
    relativa = _guardar_imagen(contenido, mime, usuario, settings)

    analisis = AnalisisIA(
        id_usuario=usuario.id_usuario,
        id_camada=camada.id_camada if camada else None,
        fecha_analisis=datetime.now(),
        imagen_url=relativa,
        resultado_diagnostico=_texto_diagnostico(diagnostico),
        recomendaciones="\n".join(recomendaciones),
        calidad_general=diagnostico.calidad_general,
        puntaje_calidad=diagnostico.puntaje_calidad,
        apto_venta=diagnostico.apto_venta,
        huevos_detectados=diagnostico.huevos_detectados,
        detalle_json=diagnostico.model_dump_json(),
        proveedor_ia=proveedor.nombre,
    )
    try:
        db.add(analisis)
        db.commit()
    except Exception:
        db.rollback()
        (_carpeta_base(settings) / relativa).unlink(missing_ok=True)
        raise
    db.refresh(analisis)
    return analisis


def listar(
    db: Session, usuario: Usuario, id_camada: int | None, limite: int
) -> list[AnalisisIA]:
    """Historial de análisis del usuario, del más reciente al más antiguo.

    Args:
        db: Sesión de base de datos.
        usuario: Usuario autenticado.
        id_camada: Si se indica, solo el historial de esa camada.
        limite: Máximo de registros.

    Returns:
        list[AnalisisIA]: Análisis encontrados.
    """
    verificar_acceso(usuario)
    consulta = db.query(AnalisisIA).filter(AnalisisIA.id_usuario == usuario.id_usuario)
    if id_camada is not None:
        camada_service.obtener_camada(db, usuario, id_camada)
        consulta = consulta.filter(AnalisisIA.id_camada == id_camada)
    return (
        consulta.order_by(
            AnalisisIA.fecha_analisis.desc(), AnalisisIA.id_analisis.desc()
        )
        .limit(limite)
        .all()
    )


def obtener(db: Session, usuario: Usuario, id_analisis: int) -> AnalisisIA:
    """Busca un análisis del usuario.

    Raises:
        HTTPException: 404 si no existe o es de otro usuario.
    """
    verificar_acceso(usuario)
    analisis = db.get(AnalisisIA, id_analisis)
    if analisis is None or analisis.id_usuario != usuario.id_usuario:
        raise HTTPException(status_code=404, detail="Análisis no encontrado")
    return analisis


def registrar_retroalimentacion(
    db: Session, usuario: Usuario, id_analisis: int, correcto: bool
) -> AnalisisIA:
    """Guarda si el avicultor considera correcto el diagnóstico."""
    analisis = obtener(db, usuario, id_analisis)
    analisis.diagnostico_correcto = correcto
    db.commit()
    db.refresh(analisis)
    return analisis


def eliminar(
    db: Session, usuario: Usuario, id_analisis: int, settings: Settings
) -> None:
    """Borra un análisis y su foto."""
    analisis = obtener(db, usuario, id_analisis)
    ruta = ruta_imagen(analisis, settings)
    db.delete(analisis)
    db.commit()
    if ruta is not None:
        ruta.unlink(missing_ok=True)


def nombres_camadas(db: Session, analisis: list[AnalisisIA]) -> dict[int, str]:
    """Nombres de las camadas referidas por una lista de análisis."""
    ids = {a.id_camada for a in analisis if a.id_camada is not None}
    if not ids:
        return {}
    filas = (
        db.query(Camada.id_camada, Camada.nombre_camada)
        .filter(Camada.id_camada.in_(ids))
        .all()
    )
    return {f.id_camada: f.nombre_camada for f in filas}


def a_respuesta(
    analisis: AnalisisIA, nombres: dict[int, str], settings: Settings
) -> AnalisisResponse:
    """Convierte la fila en la respuesta de la API.

    Los registros del seed no tienen `detalle_json`; en ese caso las
    anomalías quedan vacías y la distribución en null.
    """
    detalle: dict = {}
    if analisis.detalle_json:
        try:
            detalle = json.loads(analisis.detalle_json)
        except json.JSONDecodeError:
            detalle = {}

    anomalias = [AnomaliaIA.model_validate(a) for a in detalle.get("anomalias", [])]
    distribucion = (
        DistribucionTipos.model_validate(detalle["distribucion"])
        if "distribucion" in detalle
        else None
    )
    recomendaciones = [
        r for r in (analisis.recomendaciones or "").split("\n") if r.strip()
    ]
    return AnalisisResponse(
        id_analisis=analisis.id_analisis,
        id_camada=analisis.id_camada,
        nombre_camada=nombres.get(analisis.id_camada) if analisis.id_camada else None,
        fecha_analisis=analisis.fecha_analisis,
        tiene_imagen=ruta_imagen(analisis, settings) is not None,
        resultado_diagnostico=analisis.resultado_diagnostico,
        recomendaciones=recomendaciones,
        calidad_general=analisis.calidad_general,
        puntaje_calidad=analisis.puntaje_calidad,
        apto_venta=analisis.apto_venta,
        huevos_detectados=analisis.huevos_detectados,
        anomalias=anomalias,
        distribucion=distribucion,
        confianza_general=detalle.get("confianza_general"),
        proveedor_ia=analisis.proveedor_ia,
        modo_demo=analisis.proveedor_ia == "demo",
        diagnostico_correcto=analisis.diagnostico_correcto,
    )
