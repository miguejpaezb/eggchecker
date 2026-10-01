import { peticion } from './api';
import { obtenerToken } from './authService';

/** Cabecera de autorización con el token JWT vigente. */
function conAutenticacion(extra = {}) {
  return { Authorization: `Bearer ${obtenerToken()}`, ...extra };
}

/**
 * Lista el catálogo global de categorías de insumo.
 * @returns {Promise<Array<Object>>} Categorías ordenadas por nombre.
 */
export function listarCategorias() {
  return peticion('/categorias-insumo', { headers: conAutenticacion() });
}

/**
 * Crea una categoría de insumo nueva.
 * @param {Object} datos - nombre_categ y descripcion.
 * @returns {Promise<Object>} La categoría creada.
 */
export function crearCategoria(datos) {
  return peticion('/categorias-insumo', {
    method: 'POST',
    headers: conAutenticacion(),
    body: JSON.stringify(datos),
  });
}

/**
 * Edita una categoría de insumo existente.
 * @param {number} idCategoria - Identificador de la categoría.
 * @param {Object} datos - Campos a modificar.
 * @returns {Promise<Object>} La categoría actualizada.
 */
export function actualizarCategoria(idCategoria, datos) {
  return peticion(`/categorias-insumo/${idCategoria}`, {
    method: 'PATCH',
    headers: conAutenticacion(),
    body: JSON.stringify(datos),
  });
}

/**
 * Elimina una categoría sin insumos asociados.
 * @param {number} idCategoria - Identificador de la categoría.
 * @returns {Promise<null>} Respuesta vacía (204).
 */
export function eliminarCategoria(idCategoria) {
  return peticion(`/categorias-insumo/${idCategoria}`, {
    method: 'DELETE',
    headers: conAutenticacion(),
  });
}
