export const BASE_URL = '/api';

/**
 * Extrae un mensaje legible del cuerpo de error de la API.
 * FastAPI devuelve `detail` como texto o como lista de errores de validación.
 * @param {Response} respuesta - Respuesta HTTP no exitosa.
 * @returns {Promise<string>} Mensaje para mostrar al usuario.
 */
async function extraerMensaje(respuesta) {
  try {
    const cuerpo = await respuesta.json();
    if (typeof cuerpo.detail === 'string') {
      return cuerpo.detail;
    }
    if (Array.isArray(cuerpo.detail)) {
      return cuerpo.detail.map((item) => item.msg).join('. ');
    }
    return 'Ocurrió un error inesperado';
  } catch {
    return 'Ocurrió un error inesperado';
  }
}

/**
 * Realiza una petición JSON a la API y devuelve el cuerpo parseado.
 * @param {string} ruta - Ruta relativa al prefijo /api.
 * @param {Object} [opciones] - Opciones de fetch (method, body, etc.).
 * @returns {Promise<Object|null>} Respuesta parseada o null si no hay cuerpo.
 * @throws {Error} Con el mensaje de error devuelto por la API.
 */
export async function peticion(ruta, opciones = {}) {
  const { headers, ...resto } = opciones;
  let respuesta;
  try {
    respuesta = await fetch(`${BASE_URL}${ruta}`, {
      headers: { 'Content-Type': 'application/json', ...headers },
      ...resto,
    });
  } catch {
    throw new Error('No se pudo conectar con el servidor');
  }

  if (!respuesta.ok) {
    throw new Error(await extraerMensaje(respuesta));
  }
  if (respuesta.status === 204) {
    return null;
  }
  return respuesta.json();
}
