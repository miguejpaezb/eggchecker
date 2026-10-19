import { hoyIso, sumarDias } from './recordatorios';

/**
 * Etiqueta del momento de un aviso de recordatorio.
 * @param {string|null} momento - 'previo', 'hoy' o null.
 * @returns {string} Texto para el chip.
 */
export function etiquetaMomento(momento) {
  if (momento === 'previo') {
    return 'Mañana';
  }
  if (momento === 'hoy') {
    return 'Hoy';
  }
  return '';
}

/**
 * Días que cubre el reporte según la frecuencia del resumen.
 * @param {string|null} frecuencia - 'semanal', 'quincenal' o 'mensual'.
 * @returns {number} Días del rango.
 */
export function diasPorFrecuencia(frecuencia) {
  if (frecuencia === 'quincenal') {
    return 14;
  }
  if (frecuencia === 'mensual') {
    return 30;
  }
  return 7;
}

/**
 * Rango del reporte que anuncia un recordatorio de resumen.
 * @param {string|null} frecuencia - Frecuencia configurada por el usuario.
 * @param {string|null} fechaReferencia - Último día del rango.
 * @returns {{desde: string, hasta: string}} Rango en ISO.
 */
export function rangoResumen(frecuencia, fechaReferencia) {
  const hasta = fechaReferencia || hoyIso();
  const dias = diasPorFrecuencia(frecuencia);
  return { desde: sumarDias(hasta, -(dias - 1)), hasta };
}
