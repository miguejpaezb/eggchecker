import { peticion } from './api';
import { obtenerToken } from './authService';

/** Cabecera de autorización con el token JWT vigente. */
function conAutenticacion(extra = {}) {
  return { Authorization: `Bearer ${obtenerToken()}`, ...extra };
}

/**
 * Lista los recordatorios sanitarios activos del usuario.
 * @param {Object} [opciones] - Filtros del listado.
 * @param {number} [opciones.idCamada] - Camada cuyos recordatorios se listan.
 * @param {boolean} [opciones.incluirGlobales] - Sumar los de toda la granja.
 * @returns {Promise<Array<Object>>} Recordatorios con su próxima dosis.
 */
export function listarRecordatorios({ idCamada, incluirGlobales = true } = {}) {
  const parametros = new URLSearchParams();
  if (idCamada) {
    parametros.set('id_camada', String(idCamada));
  }
  parametros.set('incluir_globales', String(incluirGlobales));
  return peticion(`/recordatorios?${parametros.toString()}`, {
    headers: conAutenticacion(),
  });
}

/**
 * Consulta un recordatorio con su próxima dosis.
 * @param {number} idRecordatorio - Identificador del recordatorio.
 * @returns {Promise<Object>} El recordatorio consultado.
 */
export function obtenerRecordatorio(idRecordatorio) {
  return peticion(`/recordatorios/${idRecordatorio}`, {
    headers: conAutenticacion(),
  });
}

/**
 * Crea un recordatorio sanitario (sin id_camada es global).
 * @param {Object} datos - Regla del recordatorio.
 * @returns {Promise<Object>} El recordatorio creado.
 */
export function crearRecordatorio(datos) {
  return peticion('/recordatorios', {
    method: 'POST',
    headers: conAutenticacion(),
    body: JSON.stringify(datos),
  });
}

/**
 * Reemplaza un recordatorio y recalcula sus dosis futuras.
 * @param {number} idRecordatorio - Identificador del recordatorio.
 * @param {Object} datos - Regla completa con los cambios.
 * @returns {Promise<Object>} El recordatorio actualizado.
 */
export function actualizarRecordatorio(idRecordatorio, datos) {
  return peticion(`/recordatorios/${idRecordatorio}`, {
    method: 'PUT',
    headers: conAutenticacion(),
    body: JSON.stringify(datos),
  });
}

/**
 * Desactiva un recordatorio y descarta sus dosis pendientes.
 * @param {number} idRecordatorio - Identificador del recordatorio.
 * @returns {Promise<null>} Respuesta vacía (204).
 */
export function eliminarRecordatorio(idRecordatorio) {
  return peticion(`/recordatorios/${idRecordatorio}`, {
    method: 'DELETE',
    headers: conAutenticacion(),
  });
}

/**
 * Lista las dosis programadas de un recordatorio.
 * @param {number} idRecordatorio - Identificador del recordatorio.
 * @param {Object} [opciones] - Rango y estado de las dosis.
 * @param {string} [opciones.desde] - Fecha mínima en ISO.
 * @param {string} [opciones.hasta] - Fecha máxima en ISO.
 * @param {boolean} [opciones.soloPendientes] - Excluir las aplicadas.
 * @returns {Promise<Array<Object>>} Dosis ordenadas por fecha y camada.
 */
export function listarDosis(
  idRecordatorio,
  { desde, hasta, soloPendientes = false } = {}
) {
  const parametros = new URLSearchParams();
  if (desde) {
    parametros.set('desde', desde);
  }
  if (hasta) {
    parametros.set('hasta', hasta);
  }
  parametros.set('solo_pendientes', String(soloPendientes));
  return peticion(
    `/recordatorios/${idRecordatorio}/dosis?${parametros.toString()}`,
    { headers: conAutenticacion() }
  );
}

/**
 * Devuelve una dosis con el insumo y su stock actual.
 * @param {number} idDosis - Identificador de la dosis.
 * @returns {Promise<Object>} Detalle de la dosis.
 */
export function obtenerDosis(idDosis) {
  return peticion(`/recordatorios/dosis/${idDosis}`, {
    headers: conAutenticacion(),
  });
}

/**
 * Registra la aplicación de una dosis y descuenta el inventario.
 * @param {number} idDosis - Identificador de la dosis.
 * @param {number} [cantidad] - Cantidad usada; sin ella se usa la del plan.
 * @returns {Promise<Object>} Dosis aplicada con el stock resultante.
 */
export function aplicarDosis(idDosis, cantidad) {
  return peticion(`/recordatorios/dosis/${idDosis}/aplicar`, {
    method: 'POST',
    headers: conAutenticacion(),
    body: JSON.stringify({ cantidad }),
  });
}

/**
 * Aplica varias dosis en una sola transacción.
 * @param {Array<number>} idsDosis - Dosis a aplicar.
 * @param {number} [cantidad] - Cantidad usada por camada.
 * @returns {Promise<Array<Object>>} Resultado de cada aplicación.
 */
export function aplicarLote(idsDosis, cantidad) {
  return peticion('/recordatorios/dosis/aplicar-lote', {
    method: 'POST',
    headers: conAutenticacion(),
    body: JSON.stringify({ ids_dosis: idsDosis, cantidad }),
  });
}
