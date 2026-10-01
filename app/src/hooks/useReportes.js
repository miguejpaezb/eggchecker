import { useCallback, useEffect, useState } from 'react';

import { listarCamadas } from '../services/camadaService';
import { descargarPdf, obtenerConsolidado } from '../services/reporteService';
import { diasDelRango, rangoPreset } from '../utils/reportes';

/**
 * Gestiona el reporte de rentabilidad: rango, camada, datos y descarga.
 * @returns {Object} Estado y acciones del módulo de reportes.
 */
function useReportes() {
  const [preset, setPreset] = useState('mes');
  const [desde, setDesde] = useState(() => rangoPreset('mes').desde);
  const [hasta, setHasta] = useState(() => rangoPreset('mes').hasta);
  const [camada, setCamada] = useState('');
  const [camadas, setCamadas] = useState([]);
  const [reporte, setReporte] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [descargando, setDescargando] = useState(false);
  const [error, setError] = useState('');

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
