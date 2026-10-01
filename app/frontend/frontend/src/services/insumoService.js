import { peticion } from './api';
import { obtenerToken } from './authService';

/** Cabecera de autorización con el token JWT vigente. */
function conAutenticacion(extra = {}) {
  return { Authorization: `Bearer ${obtenerToken()}`, ...extra };
}

/**
 * Lista los insumos del usuario, incluyendo los suspendidos.
 * @returns {Promise<Array<Object>>} Insumos no descontinuados.
 */
export function listarInsumos() {
  return peticion('/insumos?activo=false', { headers: conAutenticacion() });
}

/**
 * Obtiene un insumo por su identificador.
 * @param {number} idInsumo - Identificador del insumo.
 * @returns {Promise<Object>} El insumo encontrado.
 */
export function obtenerInsumo(idInsumo) {
  return peticion(`/insumos/${idInsumo}`, { headers: conAutenticacion() });
}

/**
 * Registra un insumo nuevo.
 * @param {Object} datos - id_categoria, nombre_insumo, unidad_medida,
 *   stock_actual y umbral_minimo.
 * @returns {Promise<Object>} El insumo creado.
 */
export function crearInsumo(datos) {
  return peticion('/insumos', {
    method: 'POST',
    headers: conAutenticacion(),
    body: JSON.stringify(datos),
  });
}

/**
 * Edita el nombre, la unidad de medida y el umbral mínimo de un insumo.
 * @param {number} idInsumo - Identificador del insumo.
 * @param {Object} datos - Campos editables.
 * @returns {Promise<Object>} El insumo actualizado.
 */
export function actualizarInsumo(idInsumo, datos) {
  return peticion(`/insumos/${idInsumo}`, {
    method: 'PATCH',
    headers: conAutenticacion(),
    body: JSON.stringify(datos),
  });
}

/**
 * Registra un movimiento de entrada (suma) o salida (resta) de stock.
 * @param {number} idInsumo - Identificador del insumo.
 * @param {Object} datos - tipo_movimiento, cantidad y observaciones.
 * @returns {Promise<Object>} El movimiento con el stock resultante.
 */
export function registrarMovimiento(idInsumo, datos) {
  return peticion(`/insumos/${idInsumo}/movimientos`, {
    method: 'POST',
    headers: conAutenticacion(),
    body: JSON.stringify(datos),
  });
}

/**
 * Suma stock a un insumo (movimiento de entrada).
 * @param {number} idInsumo - Identificador del insumo.
 * @param {number} cantidad - Cantidad a sumar (positiva).
 * @returns {Promise<Object>} El movimiento con el stock resultante.
 */
export function anadirStock(idInsumo, cantidad) {
  return registrarMovimiento(idInsumo, {
    tipo_movimiento: 'entrada',
    cantidad,
  });
}

/**
 * Suspende un insumo (reversible).
 * @param {number} idInsumo - Identificador del insumo.
 * @returns {Promise<Object>} El insumo suspendido.
 */
export function suspenderInsumo(idInsumo) {
  return peticion(`/insumos/${idInsumo}/suspender`, {
    method: 'POST',
    headers: conAutenticacion(),
  });
}

/**
 * Reactiva un insumo suspendido.
 * @param {number} idInsumo - Identificador del insumo.
 * @returns {Promise<Object>} El insumo reactivado.
 */
export function activarInsumo(idInsumo) {
  return peticion(`/insumos/${idInsumo}/activar`, {
    method: 'POST',
    headers: conAutenticacion(),
  });
}

/**
 * Descontinúa un insumo de forma permanente.
 * @param {number} idInsumo - Identificador del insumo.
 * @returns {Promise<Object>} El insumo descontinuado.
 */
export function descontinuarInsumo(idInsumo) {
  return peticion(`/insumos/${idInsumo}/descontinuar`, {
    method: 'POST',
    headers: conAutenticacion(),
  });
}
