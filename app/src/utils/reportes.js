// Regla de negocio RF-17: una cubeta equivale a 30 huevos.
export const HUEVOS_POR_CUBETA = 30;

// Presets rápidos del rango del reporte.
export const PRESETS = [
  { valor: 'hoy', label: 'Hoy' },
  { valor: 'semana', label: 'Última semana' },
  { valor: 'mes', label: 'Este mes' },
];

/**
 * Convierte una fecha a ISO local (YYYY-MM-DD) sin desfase de zona horaria.
 * @param {Date} fecha - Fecha a convertir.
 * @returns {string} La fecha en formato ISO local.
 */
function isoLocal(fecha) {
  const local = new Date(fecha.getTime() - fecha.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

/**
 * Calcula el rango desde/hasta de un preset de período.
 * @param {string} preset - 'hoy', 'semana' o 'mes'.
 * @returns {{desde: string, hasta: string}} Rango en ISO.
 */
export function rangoPreset(preset) {
  const hoy = new Date();
  if (preset === 'semana') {
    const inicio = new Date(hoy);
    inicio.setDate(hoy.getDate() - 6);
    return { desde: isoLocal(inicio), hasta: isoLocal(hoy) };
  }
  if (preset === 'mes') {
    const inicio = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
    return { desde: isoLocal(inicio), hasta: isoLocal(hoy) };
  }
  return { desde: isoLocal(hoy), hasta: isoLocal(hoy) };
}

/**
 * Formatea una fecha ISO (YYYY-MM-DD) en formato corto DD/MM/AAAA.
 * @param {string} fechaISO - Fecha en formato ISO.
 * @returns {string} Fecha legible o '—' si no es válida.
 */
export function formatearFecha(fechaISO) {
  if (!fechaISO) {
    return '—';
  }
  const [anio, mes, dia] = fechaISO.split('-');
  return `${dia}/${mes}/${anio}`;
}

/**
 * Formatea un texto descriptivo del rango seleccionado.
 * @param {string} desde - Fecha inicial ISO.
 * @param {string} hasta - Fecha final ISO.
 * @returns {string} Ej. '28/09/2026' o '01/09/2026 - 28/09/2026'.
 */
export function formatearRango(desde, hasta) {
  if (!desde || !hasta) {
    return '—';
  }
  if (desde === hasta) {
    return formatearFecha(desde);
  }
  return `${formatearFecha(desde)} - ${formatearFecha(hasta)}`;
}

/**
 * Cuenta los días (inclusive) de un rango ISO.
 * @param {string} desde - Fecha inicial ISO.
 * @param {string} hasta - Fecha final ISO.
 * @returns {number} Cantidad de días del rango.
 */
export function diasDelRango(desde, hasta) {
  if (!desde || !hasta) {
    return 0;
  }
  const inicio = new Date(`${desde}T00:00:00`);
  const fin = new Date(`${hasta}T00:00:00`);
  return Math.floor((fin - inicio) / 86400000) + 1;
}

/**
 * Convierte huevos a cubetas completas (enteras).
 * @param {number|string} huevos - Cantidad de huevos.
 * @returns {number} Cubetas completas.
 */
export function aCubetas(huevos) {
  return Math.floor((Number(huevos) || 0) / HUEVOS_POR_CUBETA);
}

/**
 * Formatea un número con separadores de miles y decimales configurables.
 * @param {number|string} valor - Valor a formatear.
 * @param {number} [decimales] - Decimales a mostrar.
 * @returns {string} Número legible.
 */
export function formatoNumero(valor, decimales = 0) {
  const numero = Number(valor) || 0;
  return numero.toLocaleString('es-CO', {
    minimumFractionDigits: decimales,
    maximumFractionDigits: decimales,
  });
}
