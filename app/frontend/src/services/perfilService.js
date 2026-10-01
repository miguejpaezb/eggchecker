import { peticion } from './api';
import { obtenerToken } from './authService';

/**
 * Actualiza los datos personales y de la granja del usuario.
 * @param {Object} datos - Nombre, correo, teléfono, granja y contraseña actual.
 * @returns {Promise<Object>} Perfil actualizado.
 */
export function actualizarPerfil(datos) {
  return peticion('/usuarios/me', {
    method: 'PUT',
    headers: { Authorization: `Bearer ${obtenerToken()}` },
    body: JSON.stringify(datos),
  });
}

/**
 * Guarda las preferencias de alertas del usuario.
 * @param {Object} datos - Los cuatro switches de notificaciones.
 * @returns {Promise<Object>} Perfil con las preferencias actualizadas.
 */
export function actualizarNotificaciones(datos) {
  return peticion('/usuarios/me/notificaciones', {
    method: 'PUT',
    headers: { Authorization: `Bearer ${obtenerToken()}` },
    body: JSON.stringify(datos),
  });
}

/**
 * Cambia la contraseña del usuario autenticado.
 * @param {Object} datos - Contraseña actual y nueva.
 * @returns {Promise<Object>} Mensaje de confirmación.
 */
export function cambiarContrasena(datos) {
  return peticion('/usuarios/me/contrasena', {
    method: 'PUT',
    headers: { Authorization: `Bearer ${obtenerToken()}` },
    body: JSON.stringify(datos),
  });
}
