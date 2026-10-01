// Nombres cortos de los meses para el formato "Ene 2026".
const MESES = [
  'Ene',
  'Feb',
  'Mar',
  'Abr',
  'May',
  'Jun',
  'Jul',
  'Ago',
  'Sep',
  'Oct',
  'Nov',
  'Dic',
];

/**
 * Formatea una fecha ISO (YYYY-MM-DD) como mes corto y año.
 * @param {string} fechaISO - Fecha en formato ISO.
 * @returns {string} Ej. 'Ene 2026' o '—' si no es válida.
 */
export function formatearMesAnio(fechaISO) {
  if (!fechaISO) {
    return '—';
  }
  const [anio, mes] = fechaISO.split('-');
  const indice = Number(mes) - 1;
  if (Number.isNaN(indice) || !MESES[indice]) {
    return fechaISO;
  }
  return `${MESES[indice]} ${anio}`;
}

/**
 * Devuelve el nombre visible del plan con la primera letra en mayúscula.
 * @param {string} plan - Clave del plan ('gratuito' o 'premium').
 * @returns {string} Ej. 'Plan Gratuito'.
 */
export function textoPlan(plan) {
  if (!plan) {
    return 'Plan Gratuito';
  }
  return `Plan ${plan.charAt(0).toUpperCase()}${plan.slice(1)}`;
}

/**
 * Describe las ventajas del plan del usuario.
 * @param {string} plan - Clave del plan ('gratuito' o 'premium').
 * @returns {string} Descripción legible del plan.
 */
export function descripcionPlan(plan) {
  if (plan === 'premium') {
    return 'Gestión ilimitada de camadas e Inteligencia Artificial activada.';
  }
  return (
    'Gestión de hasta 300 aves y 10 clientes. ' +
    'La Inteligencia Artificial no está incluida.'
  );
}
