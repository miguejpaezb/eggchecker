import { useCallback, useEffect, useState } from 'react';

import { obtenerDashboard } from '../services/dashboardService';

/**
 * Carga los indicadores del dashboard y expone su estado de petición.
 * @returns {Object} Datos del dashboard, carga, error y recarga.
 */
function useDashboard() {
  const [datos, setDatos] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [recarga, setRecarga] = useState(0);

  useEffect(() => {
    let activo = true;
    setCargando(true);
    setError('');
    obtenerDashboard()
      .then((respuesta) => {
        if (activo) {
          setDatos(respuesta);
        }
      })
      .catch((err) => {
        if (activo) {
          setError(err.message);
          setDatos(null);
        }
      })
      .finally(() => {
        if (activo) {
          setCargando(false);
        }
      });
    return () => {
      activo = false;
    };
  }, [recarga]);

  const recargar = useCallback(() => setRecarga((valor) => valor + 1), []);

  return { datos, cargando, error, recargar };
}

export default useDashboard;
