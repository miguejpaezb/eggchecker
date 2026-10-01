import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import ConfirmDialog from '../components/ConfirmDialog';
import EliminarPedidoModal from '../components/EliminarPedidoModal';
import Icon from '../components/Icon';
import NuevoPedidoModal from '../components/NuevoPedidoModal';
import PedidoAccionesMenu from '../components/PedidoAccionesMenu';
import PedidoCard from '../components/PedidoCard';
import PedidoDetalleModal from '../components/PedidoDetalleModal';
import useClientes from '../hooks/useClientes';
import usePedidos from '../hooks/usePedidos';

const FILTROS_ESTADO = [
  { valor: 'pendiente', label: 'Pendientes' },
  { valor: 'enviado', label: 'En camino' },
  { valor: 'recibido', label: 'Recibidos' },
  { valor: 'cancelado', label: 'Cancelados' },
  { valor: 'todos', label: 'Todos' },
];

/**
 * Página de Ventas: pedidos, stock y acciones asociadas.
 * @returns {JSX.Element} La vista del módulo de ventas.
 */
function Ventas() {
  const {
    pedidosFiltrados,
    stockTotal,
    stockPorTipo,
    cargando,
    error,
    busqueda,
    cambiarBusqueda,
    fecha,
    cambiarFecha,
    filtroEstado,
    cambiarFiltroEstado,
    agregarPedido,
    editarPedido,
    cambiarEstado,
    cancelar,
    eliminar,
  } = usePedidos();

  const { clientes, cargando: cargandoClientes } = useClientes();
  const clientesActivos = useMemo(
    () => clientes.filter((cliente) => cliente.activo),
    [clientes]
  );

  const location = useLocation();
  const navigate = useNavigate();
  const [nuevoAbierto, setNuevoAbierto] = useState(false);
  const [pedidoEnEdicion, setPedidoEnEdicion] = useState(null);
  const [clienteInicial, setClienteInicial] = useState(null);
  const [pedidoDetalle, setPedidoDetalle] = useState(null);
  const [pedidoACancelar, setPedidoACancelar] = useState(null);
  const [cancelando, setCancelando] = useState(false);
  const [errorCancelar, setErrorCancelar] = useState('');
  const [pedidoAEliminar, setPedidoAEliminar] = useState(null);
  const [errorAccion, setErrorAccion] = useState('');

  useEffect(() => {
    const idObjetivo = location.state?.clienteId;
    if (!idObjetivo || cargandoClientes) {
      return undefined;
    }

    const encontrado = clientesActivos.find(
      (cliente) => cliente.id_cliente === idObjetivo
    );
    if (encontrado) {
      setPedidoEnEdicion(null);
      setClienteInicial(encontrado);
      setNuevoAbierto(true);
    }
    navigate(location.pathname, { replace: true, state: null });
    return undefined;
  }, [location, clientesActivos, cargandoClientes, navigate]);

  const abrirNuevo = () => {
    setPedidoEnEdicion(null);
    setClienteInicial(null);
    setNuevoAbierto(true);
  };

  const abrirEdicion = (pedido) => {
    setClienteInicial(null);
    setPedidoEnEdicion(pedido);
    setNuevoAbierto(true);
  };

  const guardarPedido = (datos, idPedido) =>
    idPedido ? editarPedido(idPedido, datos) : agregarPedido(datos);

  const ejecutarEstado = async (pedido, estado) => {
    setErrorAccion('');
    try {
      await cambiarEstado(pedido.id_pedido, estado);
    } catch (err) {
      setErrorAccion(err.message);
    }
  };

  const confirmarCancelar = async () => {
    setErrorCancelar('');
    setCancelando(true);
    try {
      await cancelar(pedidoACancelar.id_pedido);
      setPedidoACancelar(null);
    } catch (err) {
      setErrorCancelar(err.message);
    } finally {
      setCancelando(false);
    }
  };

  const confirmarEliminar = async (contrasena) => {
    await eliminar(pedidoAEliminar.id_pedido, contrasena);
    setPedidoAEliminar(null);
  };

  const limpiarFiltros = () => {
    cambiarBusqueda('');
    cambiarFecha('');
    cambiarFiltroEstado('pendiente');
  };

  return (
    <div>
      <div className="ec-page-head ec-ventas__head">
        <div>
          <h1 className="ec-page-title">Ventas</h1>
          <p className="ec-page-subtitle">
            Pedidos y disponibilidad de huevos.
          </p>
        </div>

        <div className="ec-glass-card ec-ventas__resumen">
          <label className="ec-ventas__resumen-bloque" htmlFor="ventas-filtro">
            <span className="ec-ventas__resumen-label">Filtro</span>
            <select
              id="ventas-filtro"
              className="ec-ventas__resumen-select"
              value={filtroEstado}
              onChange={(event) => cambiarFiltroEstado(event.target.value)}
            >
              {FILTROS_ESTADO.map((filtro) => (
                <option key={filtro.valor} value={filtro.valor}>
                  {filtro.label}
                </option>
              ))}
            </select>
          </label>

          <div className="ec-ventas__resumen-bloque">
            <span className="ec-ventas__resumen-label">Stock Total</span>
            <span className="ec-ventas__resumen-valor">{stockTotal}</span>
          </div>
        </div>
      </div>

      <div className="ec-glass-card ec-ventas__filtros">
        <div className="ec-buscador">
          <input
            id="ventas-busqueda"
            className="ec-buscador__input"
            type="text"
            value={busqueda}
            onChange={(event) => cambiarBusqueda(event.target.value)}
            placeholder="Buscar cliente..."
            aria-label="Buscar cliente"
          />
          <svg
            className="ec-buscador__icono"
            viewBox="0 0 20 20"
            fill="currentColor"
            aria-hidden="true"
          >
            <path
              fillRule="evenodd"
              d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"
              clipRule="evenodd"
            />
          </svg>
        </div>

        <div className="ec-ventas__filtros-derecha">
          <input
            id="ventas-fecha"
            className="ec-ventas__fecha"
            type="date"
            value={fecha}
            onChange={(event) => cambiarFecha(event.target.value)}
            aria-label="Filtrar por fecha"
          />

          <button
            type="button"
            className="ec-ventas__limpiar"
            onClick={limpiarFiltros}
            aria-label="Limpiar filtros"
            title="Limpiar filtros"
          >
            <Icon
              src="/assets/icons/icon_clear.svg"
              className="ec-ventas__limpiar-icon"
            />
          </button>

          <button
            type="button"
            className="ec-btn ec-btn--primary ec-ventas__btn-nuevo"
            onClick={abrirNuevo}
          >
            <Icon src="/assets/icons/add-icon.svg" className="ec-btn__icon" />
            Nuevo Pedido
          </button>
        </div>

        <div className="ec-ventas__acciones">
          <button
            type="button"
            className="ec-btn ec-btn--primary"
            onClick={abrirNuevo}
          >
            <Icon src="/assets/icons/add-icon.svg" className="ec-btn__icon" />
            Nuevo Pedido
          </button>
        </div>
      </div>

      {errorAccion && (
        <div className="ec-glass-card ec-camadas__estado ec-camadas__estado--error">
          {errorAccion}
        </div>
      )}

      {cargando && <p className="ec-camadas__estado">Cargando pedidos…</p>}

      {!cargando && error && (
        <div className="ec-glass-card ec-camadas__estado ec-camadas__estado--error">
          {error}
        </div>
      )}

      {!cargando && !error && pedidosFiltrados.length === 0 && (
        <div className="ec-glass-card ec-camadas__estado">
          No hay pedidos para este filtro.
        </div>
      )}

      {!cargando && !error && pedidosFiltrados.length > 0 && (
        <div className="ec-ventas__grid">
          {pedidosFiltrados.map((pedido) => (
            <PedidoCard
              key={pedido.id_pedido}
              pedido={pedido}
              onVerDetalles={setPedidoDetalle}
            >
              <PedidoAccionesMenu
                pedido={pedido}
                onMarcarEnviado={(item) => ejecutarEstado(item, 'enviado')}
                onMarcarRecibido={(item) => ejecutarEstado(item, 'recibido')}
                onEditar={abrirEdicion}
                onCancelar={(item) => {
                  setErrorCancelar('');
                  setPedidoACancelar(item);
                }}
                onEliminar={setPedidoAEliminar}
              />
            </PedidoCard>
          ))}
        </div>
      )}

      <NuevoPedidoModal
        abierto={nuevoAbierto}
        clientes={clientesActivos}
        stockPorTipo={stockPorTipo}
        pedido={pedidoEnEdicion}
        clienteInicial={clienteInicial}
        onCerrar={() => setNuevoAbierto(false)}
        onGuardar={guardarPedido}
      />

      <PedidoDetalleModal
        abierto={pedidoDetalle !== null}
        pedido={pedidoDetalle}
        onCerrar={() => setPedidoDetalle(null)}
      />

      <EliminarPedidoModal
        abierto={pedidoAEliminar !== null}
        pedido={pedidoAEliminar}
        onCerrar={() => setPedidoAEliminar(null)}
        onEliminar={confirmarEliminar}
      />

      <ConfirmDialog
        abierto={pedidoACancelar !== null}
        titulo="Cancelar pedido"
        mensaje={
          pedidoACancelar
            ? `¿Cancelar el pedido de "${pedidoACancelar.cliente_nombre}"? Quedará registrado como cancelado y se repondrá el stock.`
            : ''
        }
        textoConfirmar="Cancelar pedido"
        peligro
        cargando={cancelando}
        error={errorCancelar}
        onConfirmar={confirmarCancelar}
        onCerrar={() => setPedidoACancelar(null)}
      />
    </div>
  );
}

export default Ventas;
