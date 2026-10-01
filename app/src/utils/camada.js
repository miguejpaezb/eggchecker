const ETIQUETAS_ESTADO = {
  activa: 'Activa',
  retirada: 'Retirada',
};

/**
 * Traduce el estado técnico de una camada a su etiqueta visible.
 * @param {string} estado - 'activa' o 'retirada'.
 * @returns {string} Etiqueta legible del estado.
 */
export function etiquetaEstado(estado) {
  return ETIQUETAS_ESTADO[estado] ?? estado;
}

/**
 * Calcula la viabilidad como porcentaje de aves que siguen vivas.
 * @param {number} cantidadActual - Aves vivas hoy.
 * @param {number} cantidadInicial - Aves al ingresar la camada.
 * @returns {number} Porcentaje de viabilidad (0-100).
 */
export function calcularViabilidad(cantidadActual, cantidadInicial) {
  if (!cantidadInicial) {
    return 0;
  }
  return (cantidadActual / cantidadInicial) * 100;
}

/**
 * Formatea una cantidad de aves con separador de miles local.
 * @param {number} cantidad - Cantidad a formatear.
 * @returns {string} Cantidad con separador de miles.
 */
export function formatearCantidad(cantidad) {
  return cantidad.toLocaleString('es-CO');
}

/**
 * Formatea una fecha ISO (YYYY-MM-DD) en texto largo en español.
 * @param {string} fecha - Fecha en formato ISO.
 * @returns {string} Fecha legible, o '—' si no es válida.
 */
export function formatearFecha(fecha) {
  if (!fecha) {
    return '—';
  }
  return new Date(`${fecha}T00:00:00`).toLocaleDateString('es-CO', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  });
}
