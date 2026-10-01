export const EDAD_PRODUCCION_SEMANAS = 28;

export const CANTIDADES_CERO = { aa: 0, a: 0, b: 0, no_apto: 0 };

export const TIPOS_HUEVO = [
  {
    clave: 'aa',
    nombre: 'AA',
    titulo: 'Tipo AA - Primera calidad',
    descripcion: 'Cáscara dura, sin defectos, tamaño grande',
    clase: 'aa',
  },
  {
    clave: 'a',
    nombre: 'A',
    titulo: 'Tipo A - Buena calidad',
    descripcion: 'Pequeñas imperfecciones en cáscara o tamaño',
    clase: 'a',
  },
  {
    clave: 'b',
    nombre: 'B',
    titulo: 'Tipo B',
    descripcion: 'Defectos visibles: cáscara rugosa o manchas',
    clase: 'b',
  },
  {
    clave: 'no_apto',
    nombre: 'No_apto',
    titulo: 'No Aptos',
    descripcion: 'Rotos, cáscara blanda o contaminados',
    clase: 'no-apto',
  },
];

/**
 * Indica si una camada está activa y en etapa de producción (>=28 semanas).
 * @param {Object} camada - Camada con estado y edad_semanas.
 * @returns {boolean} True si la camada puede registrar producción.
 */
export function camadaEnProduccion(camada) {
  return (
    camada.estado === 'activa' && camada.edad_semanas >= EDAD_PRODUCCION_SEMANAS
  );
}

/**
 * Suma las cantidades de los cuatro tipos de huevo.
 * @param {Object} cantidades - Cantidades por tipo (aa, a, b, no_apto).
 * @returns {number} Total de huevos.
 */
export function totalHuevos(cantidades) {
  return TIPOS_HUEVO.reduce(
    (total, tipo) => total + (cantidades[tipo.clave] || 0),
    0
  );
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
 * Fecha ISO de hace N días según la zona horaria local.
 * @param {number} dias - Días hacia atrás.
 * @returns {string} La fecha calculada en formato ISO.
 */
export function fechaHaceDiasISO(dias) {
  const ahora = new Date();
  const local = new Date(ahora.getTime() - ahora.getTimezoneOffset() * 60000);
  local.setDate(local.getDate() - dias);
  return local.toISOString().slice(0, 10);
}

/**
 * Formatea una fecha ISO en texto corto (ej. "Sáb 22 Mar").
 * @param {string} fechaISO - Fecha en formato YYYY-MM-DD.
 * @returns {string} Fecha legible o "—" si no es válida.
 */
export function etiquetaFechaCorta(fechaISO) {
  if (!fechaISO) {
    return '—';
  }
  const texto = new Date(`${fechaISO}T00:00:00`)
    .toLocaleDateString('es-CO', {
      weekday: 'short',
      day: '2-digit',
      month: 'short',
    })
    .replace(/\./g, '')
    .replace(/,/g, '');
  return texto
    .split(' ')
    .filter(Boolean)
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(' ');
}

/**
 * Convierte el detalle por tipo de huevo a cantidades editables.
 * @param {Array<Object>} detalle - Filas con nombre_tipo y cantidad.
 * @returns {Object} Cantidades por tipo (aa, a, b, no_apto).
 */
export function cantidadesDesdeDetalle(detalle) {
  const cantidades = { ...CANTIDADES_CERO };
  (detalle || []).forEach((fila) => {
    const tipo = TIPOS_HUEVO.find((item) => item.nombre === fila.nombre_tipo);
    if (tipo) {
      cantidades[tipo.clave] = fila.cantidad;
    }
  });
  return cantidades;
}
