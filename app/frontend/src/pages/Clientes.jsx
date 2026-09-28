import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import ClienteAccionesMenu from '../components/ClienteAccionesMenu';
import ClienteCard from '../components/ClienteCard';
import ClienteDetalleModal from '../components/ClienteDetalleModal';
import ConfirmDialog from '../components/ConfirmDialog';
import EditarClienteModal from '../components/EditarClienteModal';
import EliminarClienteModal from '../components/EliminarClienteModal';
import Icon from '../components/Icon';
import NuevoClienteModal from '../components/NuevoClienteModal';
import useClientes from '../hooks/useClientes';

const FILTROS = [
  { valor: 'activos', label: 'Activos' },
  { valor: 'suspendidos', label: 'Suspendidos' },
  { valor: 'todos', label: 'Todos' },
];

/**
 * Página de Clientes: listado, búsqueda, filtros y acciones de clientes.
 * @returns {JSX.Element} La vista del módulo de clientes.
 */
function Clientes() {
  const {
    clientesFiltrados,
    cargando,
    error,
    busqueda,
    cambiarBusqueda,
    filtroEstado,
    cambiarFiltroEstado,
    agregarCliente,
    editarCliente,
    suspender,
    activar,
    eliminar,
  } = useClientes();

  const navigate = useNavigate();
  const [nuevoAbierto, setNuevoAbierto] = useState(false);
  const [clienteEnEdicion, setClienteEnEdicion] = useState(null);
  const [clienteDetalle, setClienteDetalle] = useState(null);
  const [clienteASuspender, setClienteASuspender] = useState(null);
  const [suspendiendo, setSuspender] = useState(false);
  const [errorSuspender, setErrorSuspender] = useState('');
  const [clienteAEliminar, setClienteAEliminar] = useState(null);
  const [errorAccion, setErrorAccion] = useState('');

  const registrarVenta = useCallback(
    (cliente) => {
      navigate('/ventas', { state: { clienteId: cliente.id_cliente } });
    },
    [navigate]
  );

  const manejarActivar = async (cliente) => {
    setErrorAccion('');
    try {
      await activar(cliente.id_cliente);
    } catch (err) {
      setErrorAccion(err.message);
    }
  };

  const confirmarSuspender = async () => {
    setErrorSuspender('');
    setSuspender(true);
    try {
      await suspender(clienteASuspender.id_cliente);
      setClienteASuspender(null);
    } catch (err) {
      setErrorSuspender(err.message);
    } finally {
      setSuspender(false);
    }
  };

  const confirmarEliminar = async (contrasena) => {
    await eliminar(clienteAEliminar.id_cliente, contrasena);
    setClienteAEliminar(null);
  };

  const limpiarFiltros = () => {
    cambiarBusqueda('');
    cambiarFiltroEstado('activos');
  };

  return (
    <div>
      <div className="ec-page-head ec-clientes__head">
        <div>
          <h1 className="ec-page-title">Clientes</h1>
          <p className="ec-page-subtitle">
            Compradores de tus productos avícolas.
          </p>
        </div>
        <button
          type="button"
          className="ec-btn ec-btn--primary ec-clientes__btn-nuevo"
          onClick={() => setNuevoAbierto(true)}
        >
          <Icon src="/assets/icons/add-icon.svg" className="ec-btn__icon" />
          Añadir Cliente
        </button>
      </div>

      <div className="ec-glass-card ec-clientes__filtros">
        <div className="ec-buscador">
          <input
            id="clientes-busqueda"
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

        <div className="ec-clientes__filtros-derecha">
          <label className="ec-filtro" htmlFor="clientes-filtro-estado">
            <span className="ec-filtro__label">Mostrar:</span>
            <select
              id="clientes-filtro-estado"
              className="ec-filtro__select"
              value={filtroEstado}
              onChange={(event) => cambiarFiltroEstado(event.target.value)}
            >
              {FILTROS.map((filtro) => (
                <option key={filtro.valor} value={filtro.valor}>
                  {filtro.label}
                </option>
              ))}
            </select>
          </label>

          <button
            type="button"
            className="ec-clientes__limpiar"
            onClick={limpiarFiltros}
            aria-label="Limpiar filtros"
            title="Limpiar filtros"
          >
            <Icon
              src="/assets/icons/icon_clear.svg"
              className="ec-clientes__limpiar-icon"
            />
          </button>
        </div>

        <div className="ec-clientes__acciones">
          <button
            type="button"
            className="ec-btn ec-btn--primary"
            onClick={() => setNuevoAbierto(true)}
          >
            <Icon src="/assets/icons/add-icon.svg" className="ec-btn__icon" />
            Añadir Cliente
          </button>
        </div>
      </div>

      {errorAccion && (
        <div className="ec-glass-card ec-camadas__estado ec-camadas__estado--error">
          {errorAccion}
        </div>
      )}

      {cargando && <p className="ec-camadas__estado">Cargando clientes…</p>}

      {!cargando && error && (
        <div className="ec-glass-card ec-camadas__estado ec-camadas__estado--error">
          {error}
        </div>
      )}

      {!cargando && !error && clientesFiltrados.length === 0 && (
        <div className="ec-glass-card ec-camadas__estado">
          No hay clientes para este filtro.
        </div>
      )}

      {!cargando && !error && clientesFiltrados.length > 0 && (
        <div className="ec-clientes__grid">
          {clientesFiltrados.map((cliente) => (
            <ClienteCard
              key={cliente.id_cliente}
              cliente={cliente}
              onVerDetalles={setClienteDetalle}
            >
              <ClienteAccionesMenu
                cliente={cliente}
                onRegistrarVenta={registrarVenta}
                onEditar={setClienteEnEdicion}
                onSuspender={(item) => {
                  setErrorSuspender('');
                  setClienteASuspender(item);
                }}
                onActivar={manejarActivar}
                onEliminar={setClienteAEliminar}
              />
            </ClienteCard>
          ))}
        </div>
      )}

      <NuevoClienteModal
        abierto={nuevoAbierto}
        onCerrar={() => setNuevoAbierto(false)}
        onCrear={agregarCliente}
      />

      <EditarClienteModal
        abierto={clienteEnEdicion !== null}
        cliente={clienteEnEdicion}
        onCerrar={() => setClienteEnEdicion(null)}
        onGuardar={editarCliente}
      />

      <ClienteDetalleModal
        abierto={clienteDetalle !== null}
        cliente={clienteDetalle}
        onCerrar={() => setClienteDetalle(null)}
      />

      <EliminarClienteModal
        abierto={clienteAEliminar !== null}
        cliente={clienteAEliminar}
        onCerrar={() => setClienteAEliminar(null)}
        onEliminar={confirmarEliminar}
      />

      <ConfirmDialog
        abierto={clienteASuspender !== null}
        titulo="Suspender cliente"
        mensaje={
          clienteASuspender
            ? `¿Suspender "${clienteASuspender.nombre_cliente}"? Podrás reactivarlo cuando quieras.`
            : ''
        }
        textoConfirmar="Suspender"
        cargando={suspendiendo}
        error={errorSuspender}
        onConfirmar={confirmarSuspender}
        onCerrar={() => setClienteASuspender(null)}
      />
    </div>
  );
}

export default Clientes;
