import { formatoNumero } from './reportes';

/**
 * Formatea un porcentaje con separador de miles y decimales fijos.
 * @param {number|string} valor - Valor porcentual a formatear.
 * @param {number} [decimales] - Decimales a mostrar.
 * @returns {string} Porcentaje legible, ej. '79,00%'.
 */
export function formatoPorcentaje(valor, decimales = 2) {
  const numero = Number(valor) || 0;
  return `${numero.toLocaleString('es-CO', {
    minimumFractionDigits: decimales,
    maximumFractionDigits: decimales,
  })}%`;
}

/**
 * Describe la variación de producción frente a ayer.
 * @param {number|string|null} valor - Variación porcentual, o null sin base.
 * @returns {string} Texto con flecha, ej. '↑ +5,2% vs ayer'.
 */
export function textoVariacion(valor) {
  if (valor === null || valor === undefined) {
    return 'Sin registro de ayer';
  }
  const numero = Number(valor);
  const flecha = numero < 0 ? '↓' : '↑';
  const signo = numero > 0 ? '+' : '';
  return `${flecha} ${signo}${formatoNumero(numero, 1)}% vs ayer`;
}

/**
 * Describe el día de mayor recolección de la semana.
 * @param {Object|null} mejorDia - Día con etiqueta y total de huevos.
 * @returns {string} Ej. '68 - Vie' o 'Sin registros'.
 */
export function textoMejorDia(mejorDia) {
  if (!mejorDia) {
    return 'Sin registros';
  }
  return `${formatoNumero(mejorDia.total_huevos)} - ${mejorDia.etiqueta}`;
}
