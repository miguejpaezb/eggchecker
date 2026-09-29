import { BASE_URL, peticion } from './api';
import { obtenerToken } from './authService';

/** Cabecera de autorización con el token JWT vigente. */
function conAutenticacion(extra = {}) {
  return { Authorization: `Bearer ${obtenerToken()}`, ...extra };
}

/**
 * Arma el query string del reporte omitiendo los valores vacíos.
 * @param {Object} parametros - desde, hasta y camada.
 * @returns {string} Query string listo para la URL.
 */
function construirQuery({ desde, hasta, camada }) {
  const query = new URLSearchParams();
  if (desde) {
    query.set('desde', desde);
  }
  if (hasta) {
    query.set('hasta', hasta);
  }
  if (camada) {
    query.set('camada', camada);
  }
  return query.toString();
}

/**
 * Obtiene el reporte de rentabilidad consolidado del período.
 * @param {Object} parametros - desde, hasta y camada opcional.
 * @returns {Promise<Object>} El consolidado devuelto por la API.
 */
export function obtenerConsolidado({ desde, hasta, camada }) {
  const query = construirQuery({ desde, hasta, camada });
  return peticion(`/reportes/consolidado?${query}`, {
    headers: conAutenticacion(),
  });
}

/**
 * Descarga el reporte consolidado en PDF y dispara la descarga en el navegador.
 * @param {Object} parametros - desde, hasta y camada opcional.
 * @returns {Promise<void>} Se resuelve cuando el archivo se descargó.
 */
export async function descargarPdf({ desde, hasta, camada }) {
  const query = construirQuery({ desde, hasta, camada });
  let respuesta;
  try {
    respuesta = await fetch(`${BASE_URL}/reportes/pdf?${query}`, {
      headers: conAutenticacion(),
    });
  } catch {
    throw new Error('No se pudo conectar con el servidor');
  }
  if (!respuesta.ok) {
    throw new Error('No se pudo generar el reporte en PDF');
  }

  const blob = await respuesta.blob();
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = `reporte_rentabilidad_${desde}_${hasta}.pdf`;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  URL.revokeObjectURL(url);
}
