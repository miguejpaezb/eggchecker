import { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import { listarCamadas } from '../services/camadaService';
import { descargarPdf, obtenerConsolidado } from '../services/reporteService';
import { diasDelRango, rangoPreset } from '../utils/reportes';

/**
 * Gestiona el reporte de rentabilidad: rango, camada, datos y descarga.
 * @returns {Object} Estado y acciones del módulo de reportes.
 */
function useReportes() {
  const location = useLocation();
  const navigate = useNavigate();
  // Un aviso de resumen puede abrir la página con el período ya calculado.
  const rangoRecibido =
    location.state?.desde && location.state?.hasta ? location.state : null;
  const rangoInicial = rangoRecibido || rangoPreset('mes');

  const [preset, setPreset] = useState(rangoRecibido ? 'personalizado' : 'mes');
  const [desde, setDesde] = useState(rangoInicial.desde);
  const [hasta, setHasta] = useState(rangoInicial.hasta);
  const [camada, setCamada] = useState('');
  const [camadas, setCamadas] = useState([]);
  const [reporte, setReporte] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [descargando, setDescargando] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!rangoRecibido) {
      return undefined;
    }
    // Aplica el rango del aviso (también si ya estamos en Reportes) y
    // limpia el estado de navegación para no reutilizarlo al recargar.
    setPreset('personalizado');
    setDesde(rangoRecibido.desde);
    setHasta(rangoRecibido.hasta);
    navigate(location.pathname, { replace: true, state: null });
    return undefined;
  }, [rangoRecibido, location.pathname, navigate]);

  useEffect(() => {
    let activo = true;
    listarCamadas()
      .then((datos) => {
        if (activo) {
          setCamadas(datos);
        }
      })
      .catch(() => {
        if (activo) {
          setCamadas([]);
        }
      });
    return () => {
      activo = false;
    };
  }, []);

  useEffect(() => {
    let activo = true;
    setCargando(true);
    setError('');
    obtenerConsolidado({ desde, hasta, camada: camada || undefined })
      .then((datos) => {
        if (activo) {
          setReporte(datos);
        }
      })
      .catch((err) => {
        if (activo) {
          setError(err.message);
          setReporte(null);
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
  }, [desde, hasta, camada]);

  const cambiarPreset = useCallback((nuevo) => {
    setPreset(nuevo);
    const rango = rangoPreset(nuevo);
    setDesde(rango.desde);
    setHasta(rango.hasta);
  }, []);

  const cambiarDesde = useCallback((valor) => {
    if (!valor) {
      return;
    }
    setPreset('personalizado');
    setDesde(valor);
  }, []);

  const cambiarHasta = useCallback((valor) => {
    if (!valor) {
      return;
    }
    setPreset('personalizado');
    setHasta(valor);
  }, []);

  const cambiarCamada = useCallback((valor) => setCamada(valor), []);

  const descargar = useCallback(async () => {
    setDescargando(true);
    setError('');
    try {
      await descargarPdf({ desde, hasta, camada: camada || undefined });
    } catch (err) {
      setError(err.message);
    } finally {
      setDescargando(false);
    }
  }, [desde, hasta, camada]);

  return {
    preset,
    desde,
    hasta,
    camada,
    camadas,
    reporte,
    cargando,
    descargando,
    error,
    dias: diasDelRango(desde, hasta),
    cambiarPreset,
    cambiarDesde,
    cambiarHasta,
    cambiarCamada,
    descargar,
  };
}

export default useReportes;
