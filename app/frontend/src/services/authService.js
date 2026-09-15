import { peticion } from './api';

const TOKEN_KEY = 'ec_access_token';

/**
 * Registra un avicultor nuevo.
 * @param {Object} datos - nombre_completo, correo_electronico y contrasena.
 * @returns {Promise<Object>} Usuario creado sin el hash de contraseña.
 */
export function registrar(datos) {
  return peticion('/auth/registro', {
    method: 'POST',
    body: JSON.stringify(datos),
  });
}

/**
 * Inicia sesión con las credenciales del usuario.
 * @param {Object} credenciales - correo_electronico y contrasena.
 * @returns {Promise<Object>} Token de acceso y su tipo.
 */
export function iniciarSesion(credenciales) {
  return peticion('/auth/login', {
    method: 'POST',
    body: JSON.stringify(credenciales),
  });
}

/**
 * Guarda el token de acceso en el almacenamiento local.
 * @param {string} token - Token JWT devuelto por el login.
 */
export function guardarToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

/**
 * Recupera el token de acceso almacenado.
 * @returns {string|null} Token JWT o null si no existe.
 */
export function obtenerToken() {
  return localStorage.getItem(TOKEN_KEY);
}

/** Elimina el token de acceso almacenado. */
export function cerrarSesion() {
  localStorage.removeItem(TOKEN_KEY);
}

/**
 * Consulta el perfil del usuario autenticado.
 * @returns {Promise<Object>} Perfil con nombre, plan y uso actual.
 */
export function obtenerPerfil() {
  return peticion('/usuarios/me', {
    headers: { Authorization: `Bearer ${obtenerToken()}` },
  });
}
