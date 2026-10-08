import { BASE_URL, peticion } from './api';
import { obtenerToken } from './authService';

/** Cabecera de autorización con el token JWT vigente. */
function conAutenticacion(extra = {}) {
  return { Authorization: `Bearer ${obtenerToken()}`, ...extra };
}

/**
 * Lee el mensaje `detail` de un error de FastAPI.
 * @param {Response} respuesta - Respuesta HTTP no exitosa.
 * @returns {Promise<string>} Mensaje para mostrar.
 */
async function mensajeDeError(respuesta) {
  try {
    const cuerpo = await respuesta.json();
    if (typeof cuerpo.detail === 'string') {
      return cuerpo.detail;
    }
  } catch {
    // Cuerpo vacío o no JSON: se usa el mensaje genérico.
  }
  return 'No se pudo analizar la foto';
}

/**
 * Plan, modo demostración y cupo diario del usuario (GET /analisis-ia/estado).
 * @returns {Promise<Object>} Estado del módulo.
 */
export function obtenerEstadoIA() {
  return peticion('/analisis-ia/estado', { headers: conAutenticacion() });
}

/**
 * Envía la foto para analizarla (RF-33 a RF-36).
 * Usa multipart/form-data, por eso no pasa por `peticion` (que fija JSON).
 * @param {Blob} foto - Foto JPEG ya comprimida.
 * @param {number|string} [idCamada] - Camada de origen de los huevos.
 * @returns {Promise<Object>} El análisis guardado.
 */
export async function analizarFoto(foto, idCamada) {
  const datos = new FormData();
  datos.append('imagen', foto, 'huevos.jpg');
  if (idCamada) {
    datos.append('id_camada', String(idCamada));
  }

  let respuesta;
  try {
    respuesta = await fetch(`${BASE_URL}/analisis-ia`, {
      method: 'POST',
      headers: conAutenticacion(),
      body: datos,
    });
  } catch {
    throw new Error('No se pudo conectar con el servidor');
  }
  if (!respuesta.ok) {
    const error = new Error(await mensajeDeError(respuesta));
    error.status = respuesta.status;
    throw error;
  }
  return respuesta.json();
}

/**
 * Historial de análisis, de toda la granja o de una camada.
 * @param {number|string} [idCamada] - Camada por la que filtrar.
 * @returns {Promise<Array<Object>>} Análisis del más reciente al más antiguo.
 */
export function listarAnalisis(idCamada) {
  const ruta = idCamada ? `/analisis-ia?id_camada=${idCamada}` : '/analisis-ia';
  return peticion(ruta, { headers: conAutenticacion() });
}

/**
 * Descarga la foto de un análisis (requiere token) y la devuelve como URL local.
 * Quien la use debe liberarla con URL.revokeObjectURL.
 * @param {number} idAnalisis - Identificador del análisis.
 * @returns {Promise<string>} URL `blob:` de la imagen.
 */
export async function obtenerImagenAnalisis(idAnalisis) {
  const respuesta = await fetch(
    `${BASE_URL}/analisis-ia/${idAnalisis}/imagen`,
    {
      headers: conAutenticacion(),
    }
  );
  if (!respuesta.ok) {
    throw new Error('No se pudo cargar la foto');
  }
  return URL.createObjectURL(await respuesta.blob());
}

/**
 * Registra si el diagnóstico fue correcto.
 * @param {number} idAnalisis - Identificador del análisis.
 * @param {boolean} correcto - Opinión del avicultor.
 * @returns {Promise<Object>} El análisis actualizado.
 */
export function retroalimentarAnalisis(idAnalisis, correcto) {
  return peticion(`/analisis-ia/${idAnalisis}/retroalimentacion`, {
    method: 'PATCH',
    headers: conAutenticacion(),
    body: JSON.stringify({ diagnostico_correcto: correcto }),
  });
}

/**
 * Elimina un análisis y su foto.
 * @param {number} idAnalisis - Identificador del análisis.
 * @returns {Promise<null>} Sin contenido.
 */
export function eliminarAnalisis(idAnalisis) {
  return peticion(`/analisis-ia/${idAnalisis}`, {
    method: 'DELETE',
    headers: conAutenticacion(),
  });
}
