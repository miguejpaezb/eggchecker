import { peticion } from './api';
import { obtenerToken } from './authService';

/** Cabecera de autorización con el token JWT vigente. */
function conAutenticacion(extra = {}) {
  return { Authorization: `Bearer ${obtenerToken()}`, ...extra };
}

/**
 * Lista las camadas del usuario, opcionalmente filtradas por estado.
 * @param {string} [estado] - 'activa' o 'retirada'; se omite para listar todo.
 * @returns {Promise<Array<Object>>} Camadas que cumplen el filtro.
 */
export function listarCamadas(estado) {
  const ruta = estado ? `/camadas?estado=${estado}` : '/camadas';
  return peticion(ruta, { headers: conAutenticacion() });
}

/**
 * Lista las camadas activas que piden decisión (aviso semanal).
 * @returns {Promise<Array<Object>>} Camadas con decisión pendiente.
 */
export function listarAlertas() {
  return peticion('/camadas/alertas', { headers: conAutenticacion() });
}

/**
 * Obtiene una camada con su edad y fecha de retiro estimada.
 * @param {number} idCamada - Identificador de la camada a consultar.
 * @returns {Promise<Object>} Camada con campos calculados por el servidor.
 */
export function obtenerCamada(idCamada) {
  return peticion(`/camadas/${idCamada}`, { headers: conAutenticacion() });
}

/**
 * Registra una camada nueva para el usuario autenticado.
 * @param {Object} datos - nombre_camada, fecha_ingreso, cantidad_inicial y estado.
 * @returns {Promise<Object>} La camada recién creada.
 */
export function crearCamada(datos) {
  return peticion('/camadas', {
    method: 'POST',
    headers: conAutenticacion(),
    body: JSON.stringify(datos),
  });
}

/**
 * Edita el nombre, la fecha de ingreso y la cantidad inicial de una camada.
 * @param {number} idCamada - Identificador de la camada a editar.
 * @param {Object} datos - Campos editables de la camada.
 * @returns {Promise<Object>} La camada actualizada.
 */
export function actualizarCamada(idCamada, datos) {
  return peticion(`/camadas/${idCamada}`, {
    method: 'PATCH',
    headers: conAutenticacion(),
    body: JSON.stringify(datos),
  });
}

/**
 * Registra la mortalidad de una camada activa.
 * @param {number} idCamada - Identificador de la camada afectada.
 * @param {number} cantidad - Aves muertas a descontar (entero > 0).
 * @returns {Promise<Object>} La camada con la cantidad actual descontada.
 */
export function registrarMortalidad(idCamada, cantidad) {
  return peticion(`/camadas/${idCamada}/mortalidad`, {
    method: 'POST',
    headers: conAutenticacion(),
    body: JSON.stringify({ cantidad }),
  });
}

/**
 * Suma una semana de vida a una camada activa.
 * @param {number} idCamada - Identificador de la camada a avanzar.
 * @returns {Promise<Object>} La camada con una semana más de edad.
 */
export function avanzarSemana(idCamada) {
  return peticion(`/camadas/${idCamada}/avanzar-semana`, {
    method: 'POST',
    headers: conAutenticacion(),
  });
}

/**
 * Posponer una semana la decisión de una camada que pide aviso.
 * @param {number} idCamada - Identificador de la camada que sigue activa.
 * @returns {Promise<Object>} La camada con el próximo aviso agendado.
 */
export function seguirActiva(idCamada) {
  return peticion(`/camadas/${idCamada}/seguir-activa`, {
    method: 'POST',
    headers: conAutenticacion(),
  });
}

/**
 * Descarta una camada activa (estado 'retirada', irreversible).
 * @param {number} idCamada - Identificador de la camada a descartar.
 * @returns {Promise<Object>} La camada con estado 'retirada'.
 */
export function descartarCamada(idCamada) {
  return peticion(`/camadas/${idCamada}/descartar`, {
    method: 'POST',
    headers: conAutenticacion(),
  });
}
