import { peticion } from './api';
import { obtenerToken } from './authService';

/** Cabecera de autorización con el token JWT vigente. */
function conAutenticacion(extra = {}) {
  return { Authorization: `Bearer ${obtenerToken()}`, ...extra };
}

/**
 * Obtiene los indicadores agregados del dashboard del usuario.
 * @returns {Promise<Object>} KPI, resumen semanal, alertas y pedidos recientes.
 */
export function obtenerDashboard() {
  return peticion('/dashboard', { headers: conAutenticacion() });
}

export default obtenerDashboard;
