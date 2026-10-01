import { peticion } from './api';
import { obtenerToken } from './authService';

/** Cabecera de autorización con el token JWT vigente. */
function conAutenticacion(extra = {}) {
  return { Authorization: `Bearer ${obtenerToken()}`, ...extra };
}

/**
 * Lista la producción de una camada, de la más reciente a la más antigua.
 * @param {number} idCamada - Identificador de la camada.
 * @returns {Promise<Array<Object>>} Producciones de la camada.
 */
export function listarProduccion(idCamada) {
  return peticion(`/produccion?camada=${idCamada}`, {
    headers: conAutenticacion(),
  });
}

/**
 * Obtiene una producción con su detalle por tipo de huevo.
 * @param {number} idProduccion - Identificador de la producción.
 * @returns {Promise<Object>} Producción con su detalle.
 */
export function obtenerProduccion(idProduccion) {
  return peticion(`/produccion/${idProduccion}`, {
    headers: conAutenticacion(),
  });
}

/**
 * Registra o actualiza la recolección del día para una camada.
 * @param {Object} datos - id_camada, fecha_recoleccion, unidad y cantidades.
 * @returns {Promise<Object>} La producción creada o actualizada.
 */
export function registrarProduccion(datos) {
  return peticion('/produccion', {
    method: 'POST',
    headers: conAutenticacion(),
    body: JSON.stringify(datos),
  });
}
