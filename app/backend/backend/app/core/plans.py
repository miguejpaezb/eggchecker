from dataclasses import dataclass
from enum import Enum


class PlanEnum(Enum):
    """Planes de suscripción disponibles en EggChecker."""

    GRATUITO = "gratuito"
    PREMIUM = "premium"


@dataclass(frozen=True)
class Plan:
    """Definición de un plan de suscripción y sus límites de uso."""

    clave: str
    aves_max: int | None
    clientes_max: int | None
    ia_incluida: bool


# TODO(Miguel): ampliar a los 4 planes del Anexo A (productor/pro/empresarial)
# cuando se haga schema evolution en S1.
_PLANES: dict[str, Plan] = {
    PlanEnum.GRATUITO.value: Plan(
        clave="gratuito", aves_max=300, clientes_max=10, ia_incluida=False
    ),
    PlanEnum.PREMIUM.value: Plan(
        clave="premium", aves_max=None, clientes_max=None, ia_incluida=True
    ),
}


def obtener_plan(clave: str) -> Plan:
    """Obtiene la definición de un plan de suscripción por su clave.

    Args:
        clave: Clave del plan ('gratuito' o 'premium').

    Returns:
        Plan: Definición del plan con sus límites.

    Raises:
        ValueError: Si la clave no corresponde a ningún plan conocido.
    """
    if clave not in _PLANES:
        raise ValueError(f"Plan desconocido: {clave}")
    return _PLANES[clave]


def limite_aves(clave: str) -> int | None:
    """Devuelve el máximo de aves permitido por un plan.

    Args:
        clave: Clave del plan de suscripción.

    Returns:
        int | None: Cantidad máxima de aves; None si es ilimitado.
    """
    return obtener_plan(clave).aves_max


def limite_clientes(clave: str) -> int | None:
    """Devuelve el máximo de clientes permitido por un plan.

    Args:
        clave: Clave del plan de suscripción.

    Returns:
        int | None: Cantidad máxima de clientes; None si es ilimitado.
    """
    return obtener_plan(clave).clientes_max


def ia_incluida(clave: str) -> bool:
    """Indica si un plan incluye el módulo de inteligencia (IA).

    Args:
        clave: Clave del plan de suscripción.

    Returns:
        bool: True si el plan incluye IA, False en caso contrario.
    """
    return obtener_plan(clave).ia_incluida


def validar_limite(actual: int, nuevo: int, maximo: int | None) -> bool:
    """Comprueba que agregar una cantidad no supere el máximo del plan.

    Args:
        actual: Cantidad actual registrada.
        nuevo: Cantidad nueva que se quiere agregar.
        maximo: Máximo permitido por el plan; None significa ilimitado.

    Returns:
        bool: False si el máximo existe y se supera, True en caso contrario.
    """
    if maximo is None:
        return True
    return actual + nuevo <= maximo
