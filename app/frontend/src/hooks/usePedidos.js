import { useCallback, useEffect, useMemo, useState } from 'react';

import {
  actualizarPedido,
  cambiarEstadoPedido,
  cancelarPedido,
  crearPedido,
  eliminarPedido,
  listarPedidos,
  obtenerStock,
} from '../services/pedidoService';

/**
 * Gestiona los pedidos: listado, stock, filtros y acciones.
 * @returns {Object} Estado de los pedidos y acciones sobre ellos.
 */
function usePedidos() {
  const [pedidos, setPedidos] = useState([]);
  const [stockTotal, setStockTotal] = useState(0);
  const [stockPorTipo, setStockPorTipo] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [fecha, setFecha] = useState('');
  const [filtroEstado, setFiltroEstado] = useState('pendiente');
  const [recarga, setRecarga] = useState(0);

  const refrescar = useCallback(() => setRecarga((valor) => valor + 1), []);

  useEffect(() => {
    let activo = true;
    setCargando(true);
    setError('');

    Promise.all([listarPedidos(), obtenerStock()])
      .then(([datosPedidos, datosStock]) => {
        if (activo) {
          setPedidos(datosPedidos);
          setStockTotal(datosStock.total_disponible);
          setStockPorTipo(datosStock.por_tipo);
        }
      })
      .catch((err) => {
        if (activo) {
          setError(err.message);
          setPedidos([]);
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

  const pedidosFiltrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    return pedidos
      .filter(
        (pedido) =>
          filtroEstado === 'todos' || pedido.estado_pedido === filtroEstado
      )
      .filter(
        (pedido) =>
          !texto ||
          pedido.cliente_nombre.toLowerCase().includes(texto) ||
          (pedido.cliente_direccion ?? '').toLowerCase().includes(texto)
      )
      .filter((pedido) => !fecha || pedido.fecha_pedido === fecha);
  }, [pedidos, busqueda, fecha, filtroEstado]);

  const agregarPedido = useCallback(
    async (datos) => {
      const creado = await crearPedido(datos);
      refrescar();
      return creado;
    },
    [refrescar]
  );

  const editarPedido = useCallback(
    async (idPedido, datos) => {
      const actualizado = await actualizarPedido(idPedido, datos);
      refrescar();
      return actualizado;
    },
    [refrescar]
  );

  const cambiarEstado = useCallback(
    async (idPedido, estado) => {
      const actualizado = await cambiarEstadoPedido(idPedido, estado);
      refrescar();
      return actualizado;
    },
    [refrescar]
  );

  const cancelar = useCallback(
    async (idPedido) => {
      const actualizado = await cancelarPedido(idPedido);
      refrescar();
      return actualizado;
    },
    [refrescar]
  );

  const eliminar = useCallback(
    async (idPedido, contrasena) => {
      await eliminarPedido(idPedido, contrasena);
      refrescar();
    },
    [refrescar]
  );

  return {
    pedidos,
    pedidosFiltrados,
    stockTotal,
    stockPorTipo,
    cargando,
    error,
    busqueda,
    cambiarBusqueda: setBusqueda,
    fecha,
    cambiarFecha: setFecha,
    filtroEstado,
    cambiarFiltroEstado: setFiltroEstado,
    agregarPedido,
    editarPedido,
    cambiarEstado,
    cancelar,
    eliminar,
  };
}

export default usePedidos;
