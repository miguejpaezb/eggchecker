// Estados posibles de un pedido (coinciden con el CHECK del backend).
export const ESTADOS_PEDIDO = [
  { valor: 'pendiente', label: 'Pendiente' },
  { valor: 'enviado', label: 'En camino' },
  { valor: 'recibido', label: 'Recibido' },
  { valor: 'cancelado', label: 'Cancelado' },
];

/**
 * Traduce el estado técnico de un pedido a su etiqueta visible.
 * @param {string} estado - Estado del pedido.
 * @returns {string} Etiqueta legible del estado.
 */
export function etiquetaEstadoPedido(estado) {
  return ESTADOS_PEDIDO.find((item) => item.valor === estado)?.label ?? estado;
}

/**
 * Formatea un valor monetario en pesos colombianos.
 * @param {number|string} valor - Valor a formatear.
 * @returns {string} Valor en formato COP, ej. '$ 48.000'.
 */
export function formatoMoneda(valor) {
  const numero = Number(valor) || 0;
  return numero.toLocaleString('es-CO', {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 0,
  });
}

/**
 * Fecha de hoy en formato ISO (YYYY-MM-DD) según la zona horaria local.
 * @returns {string} La fecha actual local.
 */
export function hoyISO() {
  const ahora = new Date();
  const local = new Date(ahora.getTime() - ahora.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

/**
 * Formatea una fecha ISO (YYYY-MM-DD) en formato corto DD/MM/AAAA.
 * @param {string} fechaISO - Fecha en formato ISO.
 * @returns {string} Fecha legible o '—' si no es válida.
 */
export function formatearFechaPedido(fechaISO) {
  if (!fechaISO) {
    return '—';
  }
  const [anio, mes, dia] = fechaISO.split('-');
  return `${dia}/${mes}/${anio}`;
}

/**
 * Describe las unidades de un pedido para la tarjeta.
 * @param {Object} pedido - Pedido con unidades_totales y detalles.
 * @returns {string} Ej. '60 und AA' (un solo tipo) o '60 und'.
 */
export function resumenUnidades(pedido) {
  const total = pedido.unidades_totales ?? 0;
  const detalles = pedido.detalles ?? [];
  if (detalles.length === 1) {
    return `${total} und ${detalles[0].nombre_tipo}`;
  }
  return `${total} und`;
}
