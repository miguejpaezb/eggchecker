import { peticion } from './api';
import { obtenerToken } from './authService';

/** Cabecera de autorización con el token JWT vigente. */
function conAutenticacion(extra = {}) {
  return { Authorization: `Bearer ${obtenerToken()}`, ...extra };
}

/**
 * Lista los clientes del usuario, incluyendo los suspendidos.
 * @returns {Promise<Array<Object>>} Clientes activos y suspendidos.
 */
export function listarClientes() {
  return peticion('/clientes?activo=false', { headers: conAutenticacion() });
}

/**
 * Obtiene un cliente por su identificador.
 * @param {number} idCliente - Identificador del cliente.
 * @returns {Promise<Object>} El cliente encontrado.
 */
export function obtenerCliente(idCliente) {
  return peticion(`/clientes/${idCliente}`, { headers: conAutenticacion() });
}

/**
 * Registra un cliente nuevo.
 * @param {Object} datos - nombre_cliente, telefono y direccion.
 * @returns {Promise<Object>} El cliente creado.
 */
export function crearCliente(datos) {
  return peticion('/clientes', {
    method: 'POST',
    headers: conAutenticacion(),
    body: JSON.stringify(datos),
  });
}

/**
 * Edita el nombre, el teléfono y la dirección de un cliente.
 * @param {number} idCliente - Identificador del cliente.
 * @param {Object} datos - Campos editables.
 * @returns {Promise<Object>} El cliente actualizado.
 */
export function actualizarCliente(idCliente, datos) {
  return peticion(`/clientes/${idCliente}`, {
    method: 'PATCH',
    headers: conAutenticacion(),
    body: JSON.stringify(datos),
  });
}

/**
 * Suspende un cliente (reversible).
 * @param {number} idCliente - Identificador del cliente.
 * @returns {Promise<Object>} El cliente suspendido.
 */
export function suspenderCliente(idCliente) {
  return peticion(`/clientes/${idCliente}/suspender`, {
    method: 'POST',
    headers: conAutenticacion(),
  });
}

/**
 * Reactiva un cliente suspendido.
 * @param {number} idCliente - Identificador del cliente.
 * @returns {Promise<Object>} El cliente reactivado.
 */
export function activarCliente(idCliente) {
  return peticion(`/clientes/${idCliente}/activar`, {
    method: 'POST',
    headers: conAutenticacion(),
  });
}

/**
 * Elimina un cliente de forma permanente tras confirmar la contraseña.
 * @param {number} idCliente - Identificador del cliente.
 * @param {string} contrasena - Contraseña del usuario que confirma.
 * @returns {Promise<null>} Respuesta sin contenido.
 */
export function eliminarCliente(idCliente, contrasena) {
  return peticion(`/clientes/${idCliente}/eliminar`, {
    method: 'POST',
    headers: conAutenticacion(),
    body: JSON.stringify({ contrasena }),
  });
}
