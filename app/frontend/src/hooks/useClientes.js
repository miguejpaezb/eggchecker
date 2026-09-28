import { useCallback, useEffect, useMemo, useState } from 'react';

import {
  activarCliente,
  actualizarCliente,
  crearCliente,
  eliminarCliente,
  listarClientes,
  suspenderCliente,
} from '../services/clienteService';

/**
 * Gestiona los clientes: listado, filtros y acciones.
 * @returns {Object} Estado de los clientes y acciones sobre ellos.
 */
function useClientes() {
  const [clientes, setClientes] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [filtroEstado, setFiltroEstado] = useState('activos');
  const [recarga, setRecarga] = useState(0);

  const refrescar = useCallback(() => setRecarga((valor) => valor + 1), []);

  useEffect(() => {
    let activo = true;
    setCargando(true);
    setError('');

    listarClientes()
      .then((datos) => {
        if (activo) {
          setClientes(datos);
        }
      })
      .catch((err) => {
        if (activo) {
          setError(err.message);
          setClientes([]);
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

  const clientesFiltrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return clientes
      .filter(
        (cliente) =>
          !texto ||
          cliente.nombre_cliente.toLowerCase().includes(texto) ||
          (cliente.direccion ?? '').toLowerCase().includes(texto)
      )
      .filter((cliente) => {
        if (filtroEstado === 'activos') {
          return cliente.activo;
        }
        if (filtroEstado === 'suspendidos') {
          return !cliente.activo;
        }
        return true;
      })
      .sort((a, b) => a.nombre_cliente.localeCompare(b.nombre_cliente, 'es'));
  }, [clientes, busqueda, filtroEstado]);

  const agregarCliente = useCallback(
    async (datos) => {
      const creado = await crearCliente(datos);
      refrescar();
      return creado;
    },
    [refrescar]
  );

  const editarCliente = useCallback(
    async (idCliente, datos) => {
      const actualizado = await actualizarCliente(idCliente, datos);
      refrescar();
      return actualizado;
    },
    [refrescar]
  );

  const suspender = useCallback(
    async (idCliente) => {
      const cliente = await suspenderCliente(idCliente);
      refrescar();
      return cliente;
    },
    [refrescar]
  );

  const activar = useCallback(
    async (idCliente) => {
      const cliente = await activarCliente(idCliente);
      refrescar();
      return cliente;
    },
    [refrescar]
  );

  const eliminar = useCallback(
    async (idCliente, contrasena) => {
      await eliminarCliente(idCliente, contrasena);
      refrescar();
    },
    [refrescar]
  );

  return {
    clientes,
    clientesFiltrados,
    cargando,
    error,
    busqueda,
    cambiarBusqueda: setBusqueda,
    filtroEstado,
    cambiarFiltroEstado: setFiltroEstado,
    agregarCliente,
    editarCliente,
    suspender,
    activar,
    eliminar,
  };
}

export default useClientes;
