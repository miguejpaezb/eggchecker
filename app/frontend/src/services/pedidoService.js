import { peticion } from './api';
import { obtenerToken } from './authService';

/** Cabecera de autorización con el token JWT vigente. */
function conAutenticacion(extra = {}) {
  return { Authorization: `Bearer ${obtenerToken()}`, ...extra };
}

/**
 * Obtiene el stock de huevos del usuario por tipo.
 * @returns {Promise<Object>} Total disponible y detalle por tipo.
 */
export function obtenerStock() {
  return peticion('/ventas/stock', { headers: conAutenticacion() });
}

/**
 * Lista los pedidos del usuario.
 * @returns {Promise<Array<Object>>} Pedidos con su cliente y detalle.
 */
export function listarPedidos() {
  return peticion('/ventas/pedidos', { headers: conAutenticacion() });
}

/**
 * Obtiene un pedido por su identificador.
 * @param {number} idPedido - Identificador del pedido.
 * @returns {Promise<Object>} El pedido encontrado con su detalle.
 */
export function obtenerPedido(idPedido) {
  return peticion(`/ventas/pedidos/${idPedido}`, {
    headers: conAutenticacion(),
  });
}

/**
 * Registra un pedido nuevo.
 * @param {Object} datos - id_cliente y detalles (id_tipo, cantidad, precio).
 * @returns {Promise<Object>} El pedido creado.
 */
export function crearPedido(datos) {
  return peticion('/ventas/pedidos', {
    method: 'POST',
    headers: conAutenticacion(),
    body: JSON.stringify(datos),
  });
}

/**
 * Edita un pedido pendiente.
 * @param {number} idPedido - Identificador del pedido.
 * @param {Object} datos - Cliente y/o líneas nuevas.
 * @returns {Promise<Object>} El pedido actualizado.
 */
export function actualizarPedido(idPedido, datos) {
  return peticion(`/ventas/pedidos/${idPedido}`, {
    method: 'PATCH',
    headers: conAutenticacion(),
    body: JSON.stringify(datos),
  });
}

/**
 * Avanza el estado de un pedido a 'enviado' o 'recibido'.
 * @param {number} idPedido - Identificador del pedido.
 * @param {string} estado - Estado destino.
 * @returns {Promise<Object>} El pedido actualizado.
 */
export function cambiarEstadoPedido(idPedido, estado) {
  return peticion(`/ventas/pedidos/${idPedido}/estado`, {
    method: 'PATCH',
    headers: conAutenticacion(),
    body: JSON.stringify({ estado }),
  });
}

/**
 * Cancela un pedido pendiente o en camino y repone el stock.
 * @param {number} idPedido - Identificador del pedido.
 * @returns {Promise<Object>} El pedido cancelado.
 */
export function cancelarPedido(idPedido) {
  return peticion(`/ventas/pedidos/${idPedido}/cancelar`, {
    method: 'POST',
    headers: conAutenticacion(),
  });
}

/**
 * Elimina un pedido tras confirmar la contraseña y repone el stock.
 * @param {number} idPedido - Identificador del pedido.
 * @param {string} contrasena - Contraseña del usuario que confirma.
 * @returns {Promise<null>} Respuesta sin contenido.
 */
export function eliminarPedido(idPedido, contrasena) {
  return peticion(`/ventas/pedidos/${idPedido}/eliminar`, {
    method: 'POST',
    headers: conAutenticacion(),
    body: JSON.stringify({ contrasena }),
  });
}
