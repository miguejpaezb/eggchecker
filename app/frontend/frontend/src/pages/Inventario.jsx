import { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import AnadirStockModal from '../components/AnadirStockModal';
import CategoriasModal from '../components/CategoriasModal';
import ConfirmDialog from '../components/ConfirmDialog';
import EditarInsumoModal from '../components/EditarInsumoModal';
import Icon from '../components/Icon';
import InsumoAccionesMenu from '../components/InsumoAccionesMenu';
import InsumoCard from '../components/InsumoCard';
import NuevoInsumoModal from '../components/NuevoInsumoModal';
import useInsumos from '../hooks/useInsumos';

const FILTROS_STOCK = [
  { valor: '', label: 'Todos' },
  { valor: 'critico', label: 'Crítico / Bajo' },
  { valor: 'optimo', label: 'Óptimo' },
];

/**
 * Página de Inventario: listado, búsqueda, filtros y acciones de insumos.
 * @returns {JSX.Element} La vista del módulo de inventario.
 */
function Inventario() {
  const {
    insumos,
    categorias,
    insumosFiltrados,
    cargando,
    error,
    busqueda,
    cambiarBusqueda,
    filtroStock,
    cambiarFiltroStock,
    filtroCategoria,
    cambiarFiltroCategoria,
    agregarInsumo,
    editarInsumo,
    sumarStock,
    suspender,
    activar,
    descontinuar,
    agregarCategoria,
    editarCategoria,
    borrarCategoria,
  } = useInsumos();

  const location = useLocation();
  const navigate = useNavigate();
  const [nuevoAbierto, setNuevoAbierto] = useState(false);
  const [categoriasAbiertas, setCategoriasAbiertas] = useState(false);
  const [insumoEnEdicion, setInsumoEnEdicion] = useState(null);
  const [insumoStock, setInsumoStock] = useState(null);
  const [insumoADescontinuar, setInsumoADescontinuar] = useState(null);
  const [descontinuando, setDescontinuando] = useState(false);
  const [errorDescontinuar, setErrorDescontinuar] = useState('');
  const [errorAccion, setErrorAccion] = useState('');

  useEffect(() => {
    const idObjetivo = location.state?.insumoId;
    if (!idObjetivo || cargando) {
      return undefined;
    }

    const encontrado = insumos.find(
      (insumo) => insumo.id_insumo === idObjetivo
    );
    if (encontrado) {
      setInsumoStock(encontrado);
    }
    navigate(location.pathname, { replace: true, state: null });
    return undefined;
  }, [location, insumos, cargando, navigate]);

  const nombreCategoria = useCallback(
    (idCategoria) =>
      categorias.find((item) => item.id_categoria === idCategoria)
        ?.nombre_categ ?? 'Sin categoría',
    [categorias]
  );

  const manejarSuspender = async (insumo) => {
    setErrorAccion('');
    try {
      await suspender(insumo.id_insumo);
    } catch (err) {
      setErrorAccion(err.message);
    }
  };

  const manejarActivar = async (insumo) => {
    setErrorAccion('');
    try {
      await activar(insumo.id_insumo);
    } catch (err) {
      setErrorAccion(err.message);
    }
  };

  const confirmarDescontinuar = async () => {
    setErrorDescontinuar('');
    setDescontinuando(true);
    try {
      await descontinuar(insumoADescontinuar.id_insumo);
      setInsumoADescontinuar(null);
    } catch (err) {
      setErrorDescontinuar(err.message);
    } finally {
      setDescontinuando(false);
    }
  };

  const limpiarFiltros = () => {
    cambiarBusqueda('');
    cambiarFiltroStock('');
    cambiarFiltroCategoria('');
  };

  return (
    <div>
      <div className="ec-page-head ec-inventario__head">
        <div>
          <h1 className="ec-page-title">Inventario</h1>
          <p className="ec-page-subtitle">
            Existencias de insumos y materiales.
          </p>
        </div>
        <button
          type="button"
          className="ec-btn ec-btn--primary ec-inventario__btn-nuevo"
          onClick={() => setNuevoAbierto(true)}
        >
          <Icon src="/assets/icons/add-icon.svg" className="ec-btn__icon" />
          Añadir Insumo
        </button>
      </div>

      <div className="ec-glass-card ec-inventario__filtros">
        <div className="ec-buscador">
          <input
            id="inventario-busqueda"
            className="ec-buscador__input"
            type="text"
            value={busqueda}
            onChange={(event) => cambiarBusqueda(event.target.value)}
            placeholder="Buscar insumo..."
            aria-label="Buscar insumo"
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

        <div className="ec-inventario__filtros-derecha">
          <label className="ec-filtro" htmlFor="filtro-stock">
            <span className="ec-filtro__label">Stock:</span>
            <select
              id="filtro-stock"
              className="ec-filtro__select"
              value={filtroStock}
              onChange={(event) => cambiarFiltroStock(event.target.value)}
            >
              {FILTROS_STOCK.map((filtro) => (
                <option key={filtro.valor} value={filtro.valor}>
                  {filtro.label}
                </option>
              ))}
            </select>
          </label>

          <label className="ec-filtro" htmlFor="filtro-categoria">
            <span className="ec-filtro__label">Categoría:</span>
            <select
              id="filtro-categoria"
              className="ec-filtro__select"
              value={filtroCategoria}
              onChange={(event) => cambiarFiltroCategoria(event.target.value)}
            >
              <option value="">Todas</option>
              {categorias.map((categoria) => (
                <option
                  key={categoria.id_categoria}
                  value={categoria.id_categoria}
                >
                  {categoria.nombre_categ}
                </option>
              ))}
            </select>
          </label>

          <button
            type="button"
            className="ec-inventario__limpiar"
            onClick={limpiarFiltros}
            aria-label="Limpiar filtros"
            title="Limpiar filtros"
          >
            <Icon
              src="/assets/icons/icon_clear.svg"
              className="ec-inventario__limpiar-icon"
            />
          </button>

          <button
            type="button"
            className="ec-btn ec-btn--secondary ec-inventario__btn-categoria"
            onClick={() => setCategoriasAbiertas(true)}
          >
            <Icon
              src="/assets/icons/add-icon.svg"
              className="ec-btn__icon-dark"
            />
            Categorías
          </button>
        </div>

        <div className="ec-inventario__acciones">
          <button
            type="button"
            className="ec-btn ec-btn--secondary"
            onClick={() => setCategoriasAbiertas(true)}
          >
            <Icon
              src="/assets/icons/add-icon.svg"
              className="ec-btn__icon-dark"
            />
            Categorías
          </button>
          <button
            type="button"
            className="ec-btn ec-btn--primary"
            onClick={() => setNuevoAbierto(true)}
          >
            <Icon src="/assets/icons/add-icon.svg" className="ec-btn__icon" />
            Añadir Insumo
          </button>
        </div>
      </div>

      {errorAccion && (
        <div className="ec-glass-card ec-camadas__estado ec-camadas__estado--error">
          {errorAccion}
        </div>
      )}

      {cargando && <p className="ec-camadas__estado">Cargando insumos…</p>}

      {!cargando && error && (
        <div className="ec-glass-card ec-camadas__estado ec-camadas__estado--error">
          {error}
        </div>
      )}

      {!cargando && !error && insumosFiltrados.length === 0 && (
        <div className="ec-glass-card ec-camadas__estado">
          No hay insumos para este filtro.
        </div>
      )}

      {!cargando && !error && insumosFiltrados.length > 0 && (
        <div className="ec-inventario__grid">
          {insumosFiltrados.map((insumo) => (
            <InsumoCard
              key={insumo.id_insumo}
              insumo={insumo}
              categoria={nombreCategoria(insumo.id_categoria)}
            >
              <InsumoAccionesMenu
                insumo={insumo}
                onEditar={setInsumoEnEdicion}
                onAnadirStock={setInsumoStock}
                onSuspender={manejarSuspender}
                onActivar={manejarActivar}
                onDescontinuar={(item) => {
                  setErrorDescontinuar('');
                  setInsumoADescontinuar(item);
                }}
              />
            </InsumoCard>
          ))}
        </div>
      )}

      <NuevoInsumoModal
        abierto={nuevoAbierto}
        categorias={categorias}
        onCerrar={() => setNuevoAbierto(false)}
        onCrear={agregarInsumo}
      />

      <EditarInsumoModal
        abierto={insumoEnEdicion !== null}
        insumo={insumoEnEdicion}
        onCerrar={() => setInsumoEnEdicion(null)}
        onGuardar={editarInsumo}
      />

      <AnadirStockModal
        abierto={insumoStock !== null}
        insumo={insumoStock}
        onCerrar={() => setInsumoStock(null)}
        onAnadir={sumarStock}
      />

      <CategoriasModal
        abierto={categoriasAbiertas}
        categorias={categorias}
        onCerrar={() => setCategoriasAbiertas(false)}
        onCrear={agregarCategoria}
        onEditar={editarCategoria}
        onEliminar={borrarCategoria}
      />

      <ConfirmDialog
        abierto={insumoADescontinuar !== null}
        titulo="Descontinuar insumo"
        mensaje={
          insumoADescontinuar
            ? `¿Descontinuar "${insumoADescontinuar.nombre_insumo}"? No se podrá volver a usar; sus registros se conservan.`
            : ''
        }
        textoConfirmar="Descontinuar"
        peligro
        cargando={descontinuando}
        error={errorDescontinuar}
        onConfirmar={confirmarDescontinuar}
        onCerrar={() => setInsumoADescontinuar(null)}
      />
    </div>
  );
}

export default Inventario;
