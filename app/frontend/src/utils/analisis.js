// Lado mayor, en píxeles, con el que se envía la foto (igual que en Android).
export const LADO_MAXIMO_FOTO = 1280;

// Calidad JPEG de la foto comprimida.
const CALIDAD_JPEG = 0.85;

const CALIDADES = {
  buena: { texto: 'Buena', clase: 'buena' },
  regular: { texto: 'Regular', clase: 'regular' },
  mala: { texto: 'Mala', clase: 'mala' },
};

const ANOMALIAS = {
  grieta: 'Grieta o fisura',
  cascara_rota: 'Cáscara rota',
  suciedad: 'Suciedad en la cáscara',
  mancha_sangre: 'Mancha de sangre',
  deformidad: 'Forma irregular',
  cascara_rugosa_delgada: 'Cáscara rugosa o delgada',
  tamano_anormal: 'Tamaño anormal',
  color_irregular: 'Color irregular',
};

/**
 * Texto y modificador CSS de la calidad general del diagnóstico.
 * @param {string|null} calidad - 'buena', 'regular', 'mala' o null.
 * @returns {{texto: string, clase: string}} Etiqueta y clase.
 */
export function estiloCalidad(calidad) {
  return CALIDADES[calidad] || { texto: 'Sin detalle', clase: 'neutra' };
}

/**
 * Nombre legible de un tipo de anomalía de la API.
 * @param {string} tipo - Tipo devuelto por el backend.
 * @returns {string} Nombre para mostrar.
 */
export function etiquetaAnomalia(tipo) {
  return ANOMALIAS[tipo] || 'Otra anomalía';
}

/**
 * Porcentaje entero a partir de una fracción (0,85 → "85 %").
 * @param {number} fraccion - Valor entre 0 y 1.
 * @returns {string} Porcentaje formateado.
 */
export function porcentaje(fraccion) {
  return `${Math.round(fraccion * 100)} %`;
}

/**
 * Fecha y hora legibles a partir del ISO del backend.
 * @param {string} iso - Fecha ISO (ej. 2026-10-08T14:30:12).
 * @returns {string} Ej.: "8 oct 2026, 2:30 p. m.".
 */
export function formatearFechaHora(iso) {
  const fecha = new Date(iso);
  if (Number.isNaN(fecha.getTime())) {
    return iso;
  }
  return fecha.toLocaleString('es-CO', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

/**
 * Reduce la foto a LADO_MAXIMO_FOTO px y la convierte a JPEG.
 * El navegador aplica la orientación EXIF al dibujar la imagen, así que la
 * foto llega derecha aunque se haya tomado con el celular de lado.
 * @param {File} archivo - Imagen elegida por el usuario.
 * @returns {Promise<Blob>} Foto comprimida en JPEG.
 */
export function comprimirFoto(archivo) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(archivo);
    const imagen = new Image();
    imagen.onload = () => {
      const escala = Math.min(
        1,
        LADO_MAXIMO_FOTO / Math.max(imagen.naturalWidth, imagen.naturalHeight)
      );
      const lienzo = document.createElement('canvas');
      lienzo.width = Math.round(imagen.naturalWidth * escala);
      lienzo.height = Math.round(imagen.naturalHeight * escala);
      lienzo
        .getContext('2d')
        .drawImage(imagen, 0, 0, lienzo.width, lienzo.height);
      URL.revokeObjectURL(url);
      lienzo.toBlob(
        (blob) =>
          blob ? resolve(blob) : reject(new Error('No se pudo leer la foto')),
        'image/jpeg',
        CALIDAD_JPEG
      );
    };
    imagen.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('No se pudo leer la foto. Usa una imagen JPG o PNG.'));
    };
    imagen.src = url;
  });
}
