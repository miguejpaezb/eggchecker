import { useCallback, useState } from 'react';

import CamadaCard from '../components/CamadaCard';
import CamadaDetalleModal from '../components/CamadaDetalleModal';
import ConfirmDialog from '../components/ConfirmDialog';
import EditarCamadaModal from '../components/EditarCamadaModal';
import Icon from '../components/Icon';
import MortalidadModal from '../components/MortalidadModal';
import NuevaCamadaModal from '../components/NuevaCamadaModal';
import useCamadas from '../hooks/useCamadas';

const FILTROS = [
  { valor: 'activa', label: 'Activas' },
  { valor: 'retirada', label: 'Retiradas' },
  { valor: '', label: 'Todas' },
];

/**
 * Página de gestión de camadas: listado, filtro, acciones y modales.
 * @returns {JSX.Element} La vista del módulo de camadas.
 */
function Camadas() {
  const {
    camadas,
    cargando,
    error,
    filtroEstado,
    cambiarFiltro,
    agregarCamada,
    editarCamada,
    agregarMortalidad,
    sumarSemana,
    continuarActiva,
    descartar,
  } = useCamadas();
  const [nuevaAbierta, setNuevaAbierta] = useState(false);
  const [seleccionada, setSeleccionada] = useState(null);
  const [camadaEnEdicion, setCamadaEnEdicion] = useState(null);
  const [camadaMortalidad, setCamadaMortalidad] = useState(null);
  const [camadaADescartar, setCamadaADescartar] = useState(null);
  const [descartando, setDescartando] = useState(false);
  const [errorDescartar, setErrorDescartar] = useState('');
  const [errorAccion, setErrorAccion] = useState('');

  const abrirNueva = useCallback(() => setNuevaAbierta(true), []);
  const cerrarNueva = useCallback(() => setNuevaAbierta(false), []);
  const verDetalles = useCallback((camada) => setSeleccionada(camada), []);
  const cerrarDetalle = useCallback(() => setSeleccionada(null), []);
  const abrirEdicion = useCallback((camada) => setCamadaEnEdicion(camada), []);
  const cerrarEdicion = useCallback(() => setCamadaEnEdicion(null), []);
  const abrirMortalidad = useCallback(
    (camada) => setCamadaMortalidad(camada),
    []
  );
  const cerrarMortalidad = useCallback(() => setCamadaMortalidad(null), []);
  const pedirDescartar = useCallback((camada) => {
    setErrorDescartar('');
    setCamadaADescartar(camada);
  }, []);
  const cerrarDescartar = useCallback(() => setCamadaADescartar(null), []);

  const avanzarSemana = useCallback(
    async (camada) => {
      setErrorAccion('');
      try {
        await sumarSemana(camada.id_camada);
      } catch (err) {
        setErrorAccion(err.message);
      }
    },
    [sumarSemana]
  );

  const seguirActiva = useCallback(
    async (camada) => {
      setErrorAccion('');
      try {
        await continuarActiva(camada.id_camada);
      } catch (err) {
        setErrorAccion(err.message);
      }
    },
    [continuarActiva]
  );

  const confirmarDescartar = async () => {
    setErrorDescartar('');
    setDescartando(true);
    try {
      await descartar(camadaADescartar.id_camada);
      setCamadaADescartar(null);
    } catch (err) {
      setErrorDescartar(err.message);
    } finally {
      setDescartando(false);
    }
  };

  return (
    <div>
      <div className="ec-page-head ec-camadas__head">
        <div>
          <h1 className="ec-page-title">Gestión de Camadas</h1>
          <p className="ec-page-subtitle">
            Control de lotes y galpones activos.
          </p>
        </div>

        <div className="ec-camadas__actions">
          <label className="ec-filtro" htmlFor="camadas-filtro">
            <span className="ec-filtro__label">Estado:</span>
            <select
              id="camadas-filtro"
              className="ec-filtro__select"
              value={filtroEstado}
              onChange={(event) => cambiarFiltro(event.target.value)}
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
            className="ec-btn ec-btn--primary"
            onClick={abrirNueva}
          >
            <Icon src="/assets/icons/add-icon.svg" className="ec-btn__icon" />
            Nueva Camada
          </button>
        </div>
      </div>

      {errorAccion && (
        <div className="ec-glass-card ec-camadas__estado ec-camadas__estado--error">
          {errorAccion}
        </div>
      )}

      {cargando && <p className="ec-camadas__estado">Cargando camadas…</p>}

      {!cargando && error && (
        <div className="ec-glass-card ec-camadas__estado ec-camadas__estado--error">
          {error}
        </div>
      )}

      {!cargando && !error && camadas.length === 0 && (
        <div className="ec-glass-card ec-camadas__estado">
          No hay camadas para este filtro.
        </div>
      )}

      {!cargando && !error && camadas.length > 0 && (
        <div className="ec-camadas__grid">
          {camadas.map((camada) => (
            <CamadaCard
              key={camada.id_camada}
              camada={camada}
              onVerDetalles={verDetalles}
              onEditar={abrirEdicion}
              onMortalidad={abrirMortalidad}
              onAvanzarSemana={avanzarSemana}
              onSeguirActiva={seguirActiva}
              onDescartar={pedirDescartar}
            />
          ))}
        </div>
      )}

      <NuevaCamadaModal
        abierto={nuevaAbierta}
        onCerrar={cerrarNueva}
        onCrear={agregarCamada}
      />

      <EditarCamadaModal
        abierto={camadaEnEdicion !== null}
        camada={camadaEnEdicion}
        onCerrar={cerrarEdicion}
        onGuardar={editarCamada}
      />

      <MortalidadModal
        abierto={camadaMortalidad !== null}
        camada={camadaMortalidad}
        onCerrar={cerrarMortalidad}
        onRegistrar={agregarMortalidad}
      />

      <CamadaDetalleModal
        abierto={seleccionada !== null}
        camada={seleccionada}
        onCerrar={cerrarDetalle}
      />

      <ConfirmDialog
        abierto={camadaADescartar !== null}
        titulo="Descartar camada"
        mensaje={
          camadaADescartar
            ? `¿Descartar "${camadaADescartar.nombre_camada}"? Esta acción no se puede revertir.`
            : ''
        }
        textoConfirmar="Descartar"
        peligro
        cargando={descartando}
        error={errorDescartar}
        onConfirmar={confirmarDescartar}
        onCerrar={cerrarDescartar}
      />
    </div>
  );
}

export default Camadas;
