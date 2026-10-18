import { useCallback, useEffect, useState } from 'react';

import {
  actualizarRecordatorio,
  crearRecordatorio,
  eliminarRecordatorio,
  listarRecordatorios,
} from '../services/recordatorioService';

/**
 * Carga y gestiona los recordatorios sanitarios de una camada.
 * @param {number} idCamada - Camada cuyos recordatorios se gestionan.
 * @returns {Object} Listado, estado y acciones del módulo.
 */
function useRecordatorios(idCamada) {
  const [recordatorios, setRecordatorios] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [recarga, setRecarga] = useState(0);

  const refrescar = useCallback(() => setRecarga((valor) => valor + 1), []);

  useEffect(() => {
    let activo = true;
    setCargando(true);
    setError('');

    listarRecordatorios({ idCamada })
      .then((datos) => {
        if (activo) {
          setRecordatorios(datos);
        }
      })
      .catch((err) => {
        if (activo) {
          setError(err.message);
          setRecordatorios([]);
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
  }, [idCamada, recarga]);

  /** Crea una regla nueva y recarga el listado. */
  const crear = useCallback(
    async (datos) => {
      const creado = await crearRecordatorio(datos);
      refrescar();
      return creado;
    },
    [refrescar]
  );

  /** Reemplaza una regla y recarga el listado. */
  const actualizar = useCallback(
    async (idRecordatorio, datos) => {
      const actualizado = await actualizarRecordatorio(idRecordatorio, datos);
      refrescar();
      return actualizado;
    },
    [refrescar]
  );

  /** Desactiva una regla y recarga el listado. */
  const eliminar = useCallback(
    async (idRecordatorio) => {
      await eliminarRecordatorio(idRecordatorio);
      refrescar();
    },
    [refrescar]
  );

  return {
    recordatorios,
    cargando,
    error,
    refrescar,
    crear,
    actualizar,
    eliminar,
  };
}

export default useRecordatorios;
