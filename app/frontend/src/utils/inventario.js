/**
 * Convierte un valor (string o número) a número, con 0 ante datos inválidos.
 * @param {string|number} valor - Valor a convertir.
 * @returns {number} Valor numérico seguro.
 */
export function aNumero(valor) {
  const numero = Number(valor);
  return Number.isFinite(numero) ? numero : 0;
}

/**
 * Determina el nivel de stock de un insumo.
 * - sin_stock: 0 (gris) · critico: 0 < stock <= mínimo (rojo)
 * - bajo: mínimo < stock < 3×mínimo (amarillo) · optimo: >= 3×mínimo (verde)
 * @param {string|number} stock - Stock actual.
 * @param {string|number} umbral - Stock mínimo.
 * @returns {'sin_stock'|'critico'|'bajo'|'optimo'} Nivel de stock.
 */
export function nivelStock(stock, umbral) {
  const actual = aNumero(stock);
  const minimo = aNumero(umbral);
  if (actual <= 0) {
    return 'sin_stock';
  }
  if (minimo <= 0 || actual <= minimo) {
    return 'critico';
  }
  if (actual < minimo * 3) {
    return 'bajo';
  }
  return 'optimo';
}

/**
 * Indica si un insumo está en nivel crítico o sin stock.
 * @param {Object} insumo - Insumo con stock_actual y umbral_minimo.
 * @returns {boolean} True si el stock está en el mínimo o menos.
 */
export function esCritico(insumo) {
  return aNumero(insumo.stock_actual) <= aNumero(insumo.umbral_minimo);
}

/**
 * Indica si un insumo está en nivel óptimo (>= 3× el mínimo).
 * @param {Object} insumo - Insumo con stock_actual y umbral_minimo.
 * @returns {boolean} True si el stock es óptimo.
 */
export function esOptimo(insumo) {
  return nivelStock(insumo.stock_actual, insumo.umbral_minimo) === 'optimo';
}

/**
 * Calcula el ancho de la barra de stock respecto a 3× el mínimo.
 * @param {string|number} stock - Stock actual.
 * @param {string|number} umbral - Stock mínimo.
 * @returns {number} Porcentaje entre 0 y 100.
 */
export function porcentajeBarra(stock, umbral) {
  const minimo = aNumero(umbral);
  if (minimo <= 0) {
    return 0;
  }
  const porcentaje = (aNumero(stock) / (minimo * 3)) * 100;
  return Math.max(0, Math.min(100, porcentaje));
}

/**
 * Formatea una cantidad con separadores locales y hasta 2 decimales.
 * @param {string|number} valor - Cantidad a formatear.
 * @returns {string} Cantidad legible.
 */
export function formatearCantidad(valor) {
  return aNumero(valor).toLocaleString('es-CO', { maximumFractionDigits: 2 });
}
