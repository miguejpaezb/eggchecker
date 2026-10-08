import { useCallback, useEffect, useRef, useState } from 'react';

import {
  analizarFoto,
  eliminarAnalisis,
  listarAnalisis,
  obtenerEstadoIA,
  obtenerImagenAnalisis,
  retroalimentarAnalisis,
} from '../services/analisisService';
import { listarCamadas } from '../services/camadaService';
import { comprimirFoto } from '../utils/analisis';

// Código HTTP con el que el servidor indica que se agotó el cupo del día.
const LIMITE_DIARIO = 429;

/** Reemplaza un análisis por su versión nueva si coincide el id. */
function reemplazar(actual, nuevo) {
  return actual && actual.id_analisis === nuevo.id_analisis ? nuevo : actual;
}

/**
 * Gestiona el módulo Análisis IA (RF-33 a RF-36, CU-06): estado del plan,
 * foto, diagnóstico, historial por camada, detalle y retroalimentación.
 * @returns {Object} Estado y acciones de la página.
 */
function useAnalisisIA() {
  const [estadoIA, setEstadoIA] = useState(null);
  const [cargandoEstado, setCargandoEstado] = useState(true);
  const [errorEstado, setErrorEstado] = useState('');
  const [camadas, setCamadas] = useState([]);
  const [camadaAnalisis, setCamadaAnalisis] = useState('');
  const [filtro, setFiltro] = useState('');

  const [foto, setFoto] = useState(null);
  const [preparando, setPreparando] = useState(false);
  const [analizando, setAnalizando] = useState(false);
  const [errorAnalisis, setErrorAnalisis] = useState('');
  const [resultado, setResultado] = useState(null);

  const [historial, setHistorial] = useState([]);
  const [cargandoHistorial, setCargandoHistorial] = useState(false);
  const [errorHistorial, setErrorHistorial] = useState('');
  const [recargas, setRecargas] = useState(0);

  const [detalle, setDetalle] = useState(null);
  const [imagenDetalle, setImagenDetalle] = useState('');
  const [cargandoImagen, setCargandoImagen] = useState(false);
  const [confirmarEliminar, setConfirmarEliminar] = useState(false);
  const [eliminando, setEliminando] = useState(false);
  const [errorEliminar, setErrorEliminar] = useState('');

  const urlFoto = useRef('');

  const cargarEstado = useCallback(() => {
    setCargandoEstado(true);
    setErrorEstado('');
    return obtenerEstadoIA()
      .then(setEstadoIA)
      .catch((err) => setErrorEstado(err.message))
      .finally(() => setCargandoEstado(false));
  }, []);

  useEffect(() => {
    cargarEstado();
  }, [cargarEstado]);

  const disponible = Boolean(estadoIA && estadoIA.disponible);

  useEffect(() => {
    if (!disponible) {
      return undefined;
    }
    let activo = true;
    listarCamadas()
      .then((datos) => activo && setCamadas(datos))
      .catch(() => activo && setCamadas([]));
    return () => {
      activo = false;
    };
  }, [disponible]);

  useEffect(() => {
    if (!disponible) {
      return undefined;
    }
    let activo = true;
    setCargandoHistorial(true);
    setErrorHistorial('');
    listarAnalisis(filtro || undefined)
      .then((datos) => activo && setHistorial(datos))
      .catch((err) => activo && setErrorHistorial(err.message))
      .finally(() => activo && setCargandoHistorial(false));
    return () => {
      activo = false;
    };
  }, [disponible, filtro, recargas]);

  const liberarFoto = useCallback(() => {
    if (urlFoto.current) {
      URL.revokeObjectURL(urlFoto.current);
      urlFoto.current = '';
    }
  }, []);

  // Libera la vista previa al salir de la página.
  useEffect(() => liberarFoto, [liberarFoto]);

  const elegirFoto = useCallback(
    async (archivo) => {
      if (!archivo) {
        return;
      }
      setPreparando(true);
      setErrorAnalisis('');
      setResultado(null);
      try {
        const blob = await comprimirFoto(archivo);
        liberarFoto();
        urlFoto.current = URL.createObjectURL(blob);
        setFoto({ blob, url: urlFoto.current });
      } catch (err) {
        setErrorAnalisis(err.message);
      } finally {
        setPreparando(false);
      }
    },
    [liberarFoto]
  );

  const descartarFoto = useCallback(() => {
    liberarFoto();
    setFoto(null);
    setErrorAnalisis('');
  }, [liberarFoto]);

  const analizar = useCallback(async () => {
    if (!foto || analizando) {
      return;
    }
    setAnalizando(true);
    setErrorAnalisis('');
    try {
      const nuevo = await analizarFoto(foto.blob, camadaAnalisis);
      setResultado(nuevo);
      liberarFoto();
      setFoto(null);
      setEstadoIA((actual) =>
        actual ? { ...actual, usados_hoy: actual.usados_hoy + 1 } : actual
      );
      setRecargas((n) => n + 1);
    } catch (err) {
      setErrorAnalisis(err.message);
      if (err.status === LIMITE_DIARIO) {
        cargarEstado();
      }
    } finally {
      setAnalizando(false);
    }
  }, [foto, analizando, camadaAnalisis, liberarFoto, cargarEstado]);

  const nuevoAnalisis = useCallback(() => {
    setResultado(null);
    setErrorAnalisis('');
  }, []);

  const retroalimentar = useCallback(async (idAnalisis, correcto) => {
    try {
      const actualizado = await retroalimentarAnalisis(idAnalisis, correcto);
      setResultado((actual) => reemplazar(actual, actualizado));
      setDetalle((actual) => reemplazar(actual, actualizado));
      setHistorial((lista) =>
        lista.map((item) => reemplazar(item, actualizado))
      );
    } catch {
      // La opinión es opcional: si falla, no se interrumpe al usuario.
    }
  }, []);

  const abrirDetalle = useCallback((analisis) => {
    setDetalle(analisis);
    setImagenDetalle('');
    setErrorEliminar('');
    if (!analisis.tiene_imagen) {
      return;
    }
    setCargandoImagen(true);
    obtenerImagenAnalisis(analisis.id_analisis)
      .then(setImagenDetalle)
      .catch(() => setImagenDetalle(''))
      .finally(() => setCargandoImagen(false));
  }, []);

  const cerrarDetalle = useCallback(() => {
    setDetalle(null);
    setConfirmarEliminar(false);
    setImagenDetalle((url) => {
      if (url) {
        URL.revokeObjectURL(url);
      }
      return '';
    });
  }, []);

  const eliminar = useCallback(async () => {
    if (!detalle) {
      return;
    }
    setEliminando(true);
    setErrorEliminar('');
    try {
      await eliminarAnalisis(detalle.id_analisis);
      const id = detalle.id_analisis;
      setHistorial((lista) => lista.filter((a) => a.id_analisis !== id));
      setResultado((actual) =>
        actual && actual.id_analisis === id ? null : actual
      );
      cerrarDetalle();
    } catch (err) {
      setErrorEliminar(err.message);
    } finally {
      setEliminando(false);
    }
  }, [detalle, cerrarDetalle]);

  return {
    estadoIA,
    cargandoEstado,
    errorEstado,
    camadas,
    camadaAnalisis,
    filtro,
    foto,
    preparando,
    analizando,
    errorAnalisis,
    resultado,
    historial,
    cargandoHistorial,
    errorHistorial,
    detalle,
    imagenDetalle,
    cargandoImagen,
    confirmarEliminar,
    eliminando,
    errorEliminar,
    cargarEstado,
    setCamadaAnalisis,
    setFiltro,
    elegirFoto,
    descartarFoto,
    analizar,
    nuevoAnalisis,
    retroalimentar,
    abrirDetalle,
    cerrarDetalle,
    pedirEliminar: () => setConfirmarEliminar(true),
    cancelarEliminar: () => setConfirmarEliminar(false),
    eliminar,
  };
}

export default useAnalisisIA;
