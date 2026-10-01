import { useCallback, useEffect, useState } from 'react';

import {
  actualizarCamada,
  avanzarSemana,
  crearCamada,
  descartarCamada,
  listarCamadas,
  registrarMortalidad,
  seguirActiva,
} from '../services/camadaService';

/**
 * Gestiona el listado de camadas: filtro por estado, carga y acciones.
 * @returns {Object} Estado del listado y acciones sobre las camadas.
 */
function useCamadas() {
  const [camadas, setCamadas] = useState([]);
  const [filtroEstado, setFiltroEstado] = useState('activa');
  const [recarga, setRecarga] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  const refrescar = useCallback(() => setRecarga((valor) => valor + 1), []);

  useEffect(() => {
    let activo = true;
    setCargando(true);
    setError('');

    listarCamadas(filtroEstado || undefined)
      .then((datos) => {
        if (activo) {
          setCamadas(datos);
        }
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
  }, [filtroEstado, recarga]);

  /** Registra una camada y refresca el listado. */
  const agregarCamada = useCallback(
    async (datos) => {
      const creada = await crearCamada(datos);
      refrescar();
      return creada;
    },
    [refrescar]
  );

  /** Edita una camada y refresca el listado. */
  const editarCamada = useCallback(
    async (idCamada, datos) => {
      const actualizada = await actualizarCamada(idCamada, datos);
      refrescar();
      return actualizada;
    },
    [refrescar]
  );

  /** Registra mortalidad y refresca el listado. */
  const agregarMortalidad = useCallback(
    async (idCamada, cantidad) => {
      const camada = await registrarMortalidad(idCamada, cantidad);
      refrescar();
      return camada;
    },
    [refrescar]
  );

  /** Suma una semana de vida y refresca el listado. */
  const sumarSemana = useCallback(
    async (idCamada) => {
      const camada = await avanzarSemana(idCamada);
      refrescar();
      return camada;
    },
    [refrescar]
  );

  /** Posponer la decisión de una camada y refresca el listado. */
  const continuarActiva = useCallback(
    async (idCamada) => {
      const camada = await seguirActiva(idCamada);
      refrescar();
      return camada;
    },
    [refrescar]
  );

  /** Descarta una camada y refresca el listado. */
  const descartar = useCallback(
    async (idCamada) => {
      const camada = await descartarCamada(idCamada);
      refrescar();
      return camada;
    },
    [refrescar]
  );

  return {
    camadas,
    cargando,
    error,
    filtroEstado,
    cambiarFiltro: setFiltroEstado,
    refrescar,
    agregarCamada,
    editarCamada,
    agregarMortalidad,
    sumarSemana,
    continuarActiva,
    descartar,
  };
}

export default useCamadas;
