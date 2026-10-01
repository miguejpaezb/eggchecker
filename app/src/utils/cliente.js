/**
 * Traduce el estado técnico de un cliente a su etiqueta visible.
 * @param {boolean} activo - Si el cliente sigue activo para el usuario.
 * @returns {string} Etiqueta legible del estado.
 */
export function etiquetaEstadoCliente(activo) {
  return activo ? 'Activo' : 'Suspendido';
}

/**
 * Deja solo los dígitos de un texto (para el teléfono).
 * @param {string} valor - Texto tecleado o almacenado.
 * @returns {string} Cadena compuesta únicamente por dígitos 0-9.
 */
export function soloDigitos(valor) {
  return (valor ?? '').replace(/\D/g, '');
}

/**
 * Aplica el formato colombiano de teléfono 3-3-4 solo para mostrar.
 * El valor persistido sigue siendo la cadena de dígitos sin espacios.
 * @param {string} valor - Teléfono en dígitos (o null).
 * @returns {string} Teléfono visible, ej. '300 123 4567'.
 */
export function formatearTelefono(valor) {
  const digitos = soloDigitos(valor);
  if (digitos.length <= 3) {
    return digitos;
  }
  if (digitos.length <= 6) {
    return `${digitos.slice(0, 3)} ${digitos.slice(3)}`;
  }
  return `${digitos.slice(0, 3)} ${digitos.slice(3, 6)} ${digitos.slice(6)}`;
}

/**
 * Formatea la fecha de última compra de un cliente.
 * @param {string|null} fecha - Fecha ISO (YYYY-MM-DD) o null.
 * @returns {string} Fecha legible o 'Sin compras' si aún no ha comprado.
 */
export function formatearUltimaCompra(fecha) {
  if (!fecha) {
    return 'Sin compras';
  }
  return new Date(`${fecha}T00:00:00`).toLocaleDateString('es-CO', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  });
}
