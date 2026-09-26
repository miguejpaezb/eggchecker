import { useCallback, useEffect, useMemo, useState } from 'react';

import { listarCamadas } from '../services/camadaService';
import {
  listarProduccion,
  obtenerProduccion,
  registrarProduccion,
} from '../services/produccionService';
import {
  CANTIDADES_CERO,
  camadaEnProduccion,
  cantidadesDesdeDetalle,
  fechaHaceDiasISO,
  hoyISO,
  totalHuevos,
} from '../utils/produccion';

/**
 * Gestiona la recolección diaria: camadas en producción, cantidades por tipo,
 * el registro del día y los acumulados del historial.
 * @returns {Object} Estado y acciones del módulo de producción.
 */
function useProduccion() {
  const [camadas, setCamadas] = useState([]);
  const [camadaSeleccionada, setCamadaSeleccionada] = useState(null);
  const [fecha, setFecha] = useState(hoyISO());
  const [cantidades, setCantidades] = useState({ ...CANTIDADES_CERO });
  const [producciones, setProducciones] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [cargandoDia, setCargandoDia] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState('');
  const [mensaje, setMensaje] = useState('');
  const [recarga, setRecarga] = useState(0);

  const hoy = useMemo(() => hoyISO(), []);

  useEffect(() => {
    let activo = true;
    setCargando(true);
    setError('');

    listarCamadas('activa')
      .then((datos) => {
        if (!activo) {
          return;
        }
        const enProduccion = datos.filter(camadaEnProduccion);
        setCamadas(enProduccion);
        setCamadaSeleccionada((actual) => actual ?? enProduccion[0] ?? null);
      })
      .catch((err) => {
        if (activo) {
          setError(err.message);
          setCamadas([]);
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
  }, []);

  useEffect(() => {
    if (!camadaSeleccionada) {
      setProducciones([]);
      setCantidades({ ...CANTIDADES_CERO });
      return undefined;
    }

    let activo = true;
    const cargar = async () => {
      setCargandoDia(true);
      setError('');
      try {
        const lista = await listarProduccion(camadaSeleccionada.id_camada);
        if (!activo) {
          return;
        }
        setProducciones(lista);
        const delDia = lista.find((item) => item.fecha_recoleccion === fecha);
        if (!delDia) {
          setCantidades({ ...CANTIDADES_CERO });
          return;
        }
        const detalle = await obtenerProduccion(delDia.id_produccion);
        if (activo) {
          setCantidades(cantidadesDesdeDetalle(detalle.detalle));
        }
      } catch (err) {
        if (activo) {
          setError(err.message);
          setCantidades({ ...CANTIDADES_CERO });
        }
      } finally {
        if (activo) {
          setCargandoDia(false);
        }
      }
    };

    cargar();
    return () => {
      activo = false;
    };
  }, [camadaSeleccionada, fecha, recarga]);

  const esHoy = fecha === hoy;
  const editable = Boolean(camadaSeleccionada) && esHoy && !cargandoDia;
  const limite = camadaSeleccionada ? camadaSeleccionada.cantidad_actual : 0;
  const total = totalHuevos(cantidades);
  const puedeGuardar = editable && total > 0 && total <= limite && !guardando;

  const seleccionarCamada = useCallback((camada) => {
    setCamadaSeleccionada(camada);
    setFecha(hoyISO());
    setError('');
    setMensaje('');
  }, []);

  const cambiarFecha = useCallback((nuevaFecha) => {
    if (!nuevaFecha || nuevaFecha > hoyISO()) {
      return;
    }
    setFecha(nuevaFecha);
    setError('');
    setMensaje('');
  }, []);

  const incrementar = useCallback(
    (clave) => {
      if (!editable) {
        return;
      }
      setCantidades((actual) => {
        const siguiente = { ...actual, [clave]: actual[clave] + 1 };
        if (totalHuevos(siguiente) > limite) {
          return actual;
        }
        return siguiente;
      });
      setMensaje('');
    },
    [editable, limite]
  );

  const decrementar = useCallback(
    (clave) => {
      if (!editable) {
        return;
      }
      setCantidades((actual) => ({
        ...actual,
        [clave]: Math.max(0, actual[clave] - 1),
      }));
      setMensaje('');
    },
    [editable]
  );

  const guardar = useCallback(async () => {
    if (!puedeGuardar) {
      return;
    }
    setGuardando(true);
    setError('');
    setMensaje('');
    try {
      await registrarProduccion({
        id_camada: camadaSeleccionada.id_camada,
        fecha_recoleccion: fecha,
        unidad: 'unidad',
        aa: cantidades.aa,
        a: cantidades.a,
        b: cantidades.b,
        no_apto: cantidades.no_apto,
      });
      setMensaje('Producción guardada correctamente');
      setRecarga((valor) => valor + 1);
    } catch (err) {
      setError(err.message);
    } finally {
      setGuardando(false);
    }
  }, [puedeGuardar, camadaSeleccionada, fecha, cantidades]);

  const registrosRecientes = useMemo(
    () =>
      [...producciones]
        .sort((a, b) => b.fecha_recoleccion.localeCompare(a.fecha_recoleccion))
        .slice(0, 3),
    [producciones]
  );

  const promedio7Dias = useMemo(() => {
    const desde = fechaHaceDiasISO(6);
    const enVentana = producciones.filter(
      (item) => item.fecha_recoleccion >= desde && item.fecha_recoleccion <= hoy
    );
    if (enVentana.length === 0) {
      return 0;
    }
    const suma = enVentana.reduce((acc, item) => acc + item.total_huevos, 0);
    return Math.floor(suma / enVentana.length);
  }, [producciones, hoy]);

  return {
    camadas,
    camadaSeleccionada,
    seleccionarCamada,
    fecha,
    cambiarFecha,
    hoy,
    cantidades,
    incrementar,
    decrementar,
    total,
    limite,
    esHoy,
    editable,
    puedeGuardar,
    registrosRecientes,
    promedio7Dias,
    cargando,
    cargandoDia,
    guardando,
    error,
    mensaje,
    guardar,
  };
}

export default useProduccion;
