import { peticion } from './api';
import { obtenerToken } from './authService';

/** Cabecera de autorización con el token JWT vigente. */
function conAutenticacion(extra = {}) {
  return { Authorization: `Bearer ${obtenerToken()}`, ...extra };
}

/**
 * Lista las notificaciones del usuario (no leídas primero).
 * @returns {Promise<Array<Object>>} Notificaciones del usuario.
 */
export function listarNotificaciones() {
  return peticion('/notificaciones', { headers: conAutenticacion() });
}

/**
 * Marca una notificación como leída.
 * @param {number} idNotificacion - Identificador de la notificación.
 * @returns {Promise<Object>} La notificación actualizada.
 */
export function marcarLeida(idNotificacion) {
  return peticion(`/notificaciones/${idNotificacion}/leer`, {
    method: 'POST',
    headers: conAutenticacion(),
  });
}

/**
 * Marca como leídas todas las notificaciones del usuario.
 * @returns {Promise<null>} Respuesta vacía (204).
 */
export function marcarTodasLeidas() {
  return peticion('/notificaciones/leer-todas', {
    method: 'POST',
    headers: conAutenticacion(),
  });
}

/**
 * Elimina una notificación del usuario.
 * @param {number} idNotificacion - Identificador de la notificación.
 * @returns {Promise<null>} Respuesta vacía (204).
 */
export function eliminarNotificacion(idNotificacion) {
  return peticion(`/notificaciones/${idNotificacion}`, {
    method: 'DELETE',
    headers: conAutenticacion(),
  });
}
