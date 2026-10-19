import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import useRecordatorios from '../../hooks/useRecordatorios';
import { listarDosis } from '../../services/recordatorioService';
import {
  formatearFecha,
  resumenModo,
  textoCantidad,
} from '../../utils/recordatorios';
import ConfirmDialog from '../ConfirmDialog';
import Modal from '../Modal';
import RecordatorioFormModal from './RecordatorioFormModal';

/**
 * Modal para gestionar los recordatorios sanitarios de una camada.
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.camada - Camada desde cuyo menú se abrió.
 * @param {Function} props.onCerrar - Acción al cerrar.
 * @returns {JSX.Element} El modal de gestión.
 */
function RecordatoriosCamadaModal({ camada, onCerrar }) {
  const { recordatorios, cargando, error, crear, actualizar, eliminar } =
    useRecordatorios(camada.id_camada);
  const [formAbierto, setFormAbierto] = useState(false);
  const [enEdicion, setEnEdicion] = useState(null);
  const [aEliminar, setAEliminar] = useState(null);
  const [eliminando, setEliminando] = useState(false);
  const [errorEliminar, setErrorEliminar] = useState('');
  const [historialDe, setHistorialDe] = useState(null);
  const [historial, setHistorial] = useState([]);
  const [cargandoHistorial, setCargandoHistorial] = useState(false);
  const [errorHistorial, setErrorHistorial] = useState('');

  useEffect(() => {
    if (!historialDe) {
      return undefined;
    }

    let activo = true;
    setCargandoHistorial(true);
    setErrorHistorial('');
    setHistorial([]);

    listarDosis(historialDe.id_recordatorio)
      .then((datos) => {
        if (activo) {
          setHistorial(datos);
        }
      })
      .catch((err) => {
        if (activo) {
          setErrorHistorial(err.message);
        }
      })
      .finally(() => {
        if (activo) {
          setCargandoHistorial(false);
        }
      });

    return () => {
      activo = false;
    };
  }, [historialDe]);

  const abrirNuevo = () => {
    setEnEdicion(null);
    setFormAbierto(true);
  };

  const abrirEdicion = (recordatorio) => {
    setEnEdicion(recordatorio);
    setFormAbierto(true);
  };

  const guardar = async (datos) => {
    if (enEdicion) {
      await actualizar(enEdicion.id_recordatorio, datos);
    } else {
      await crear(datos);
    }
    setFormAbierto(false);
    setEnEdicion(null);
  };

  const confirmarEliminar = async () => {
    setEliminando(true);
    setErrorEliminar('');
    try {
      await eliminar(aEliminar.id_recordatorio);
      setAEliminar(null);
    } catch (err) {
      setErrorEliminar(err.message);
    } finally {
      setEliminando(false);
    }
  };

  return (
    <>
      <Modal abierto titulo="Recordatorios sanitarios" onCerrar={onCerrar}>
        <p className="ec-form__hint">
          Vacunas y vitaminas de {camada.nombre_camada}. Un recordatorio de toda
          la granja aplica a todas las camadas activas.
        </p>

        {cargando && <p className="ec-form__hint">Cargando recordatorios…</p>}
        {!cargando && error && <p className="ec-form__error">{error}</p>}

        {!cargando && !error && recordatorios.length === 0 && (
          <p className="ec-form__hint">No hay recordatorios programados.</p>
        )}

        {!cargando && !error && recordatorios.length > 0 && (
          <div className="ec-recordatorios__lista">
            {recordatorios.map((recordatorio) => (
              <article
                key={recordatorio.id_recordatorio}
                className="ec-recordatorio-card"
              >
                <div className="ec-recordatorio-card__head">
                  <div>
                    <h3 className="ec-recordatorio-card__titulo">
                      {recordatorio.nombre_insumo}
                    </h3>
                    <p className="ec-recordatorio-card__detalle">
                      {textoCantidad(
                        recordatorio.cantidad,
                        recordatorio.unidad_medida
                      )}{' '}
                      · {resumenModo(recordatorio)}
                    </p>
                  </div>
                  {!recordatorio.id_camada && (
                    <span className="ec-recordatorio-card__badge">
                      Toda la granja
                    </span>
                  )}
                </div>
                <p className="ec-recordatorio-card__meta">
                  Próxima: {formatearFecha(recordatorio.proxima_dosis)} ·{' '}
                  {recordatorio.dosis_pendientes} pendiente(s)
                </p>
                <div className="ec-recordatorio-card__acciones">
                  <button
                    type="button"
                    className="ec-recordatorio-card__accion"
                    onClick={() => setHistorialDe(recordatorio)}
                  >
                    Historial
                  </button>
                  <button
                    type="button"
                    className="ec-recordatorio-card__accion"
                    onClick={() => abrirEdicion(recordatorio)}
                  >
                    Editar
                  </button>
                  <button
                    type="button"
                    className="ec-recordatorio-card__accion ec-recordatorio-card__accion--peligro"
                    onClick={() => {
                      setErrorEliminar('');
                      setAEliminar(recordatorio);
                    }}
                  >
                    Eliminar
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}

        <div className="ec-form__actions">
          <button
            type="button"
            className="ec-btn ec-btn--ghost"
            onClick={onCerrar}
          >
            Cerrar
          </button>
          <button
            type="button"
            className="ec-btn ec-btn--primary"
            onClick={abrirNuevo}
          >
            Nuevo recordatorio
          </button>
        </div>
      </Modal>

      <RecordatorioFormModal
        abierto={formAbierto}
        camada={camada}
        recordatorio={enEdicion}
        onCerrar={() => {
          setFormAbierto(false);
          setEnEdicion(null);
        }}
        onGuardar={guardar}
      />

      <ConfirmDialog
        abierto={aEliminar !== null}
        titulo="Eliminar recordatorio"
        mensaje="Se desactivará el recordatorio y se descartarán las dosis pendientes. El historial de aplicaciones se conserva."
        textoConfirmar="Eliminar"
        peligro
        cargando={eliminando}
        error={errorEliminar}
        onConfirmar={confirmarEliminar}
        onCerrar={() => setAEliminar(null)}
      />

      <Modal
        abierto={historialDe !== null}
        titulo="Historial de dosis"
        onCerrar={() => setHistorialDe(null)}
      >
        {historialDe && (
          <p className="ec-recordatorio-card__detalle">
            {historialDe.nombre_insumo} ·{' '}
            {historialDe.id_camada
              ? historialDe.nombre_camada
              : 'Toda la granja'}
          </p>
        )}

        {cargandoHistorial && <p className="ec-form__hint">Cargando…</p>}
        {!cargandoHistorial && errorHistorial && (
          <p className="ec-form__error">{errorHistorial}</p>
        )}
        {!cargandoHistorial && !errorHistorial && historial.length === 0 && (
          <p className="ec-form__hint">Todavía no hay dosis generadas.</p>
        )}

        {!cargandoHistorial && !errorHistorial && historial.length > 0 && (
          <ul className="ec-recordatorios__historial">
            {historial.map((dosis) => (
              <li key={dosis.id_dosis} className="ec-recordatorios__dosis">
                <span>{formatearFecha(dosis.fecha_programada)}</span>
                <span>{dosis.nombre_camada}</span>
                <span
                  className={
                    dosis.aplicada
                      ? 'ec-recordatorios__dosis--aplicada'
                      : 'ec-recordatorios__dosis--pendiente'
                  }
                >
                  {dosis.aplicada ? 'Aplicada' : 'Pendiente'}
                </span>
              </li>
            ))}
          </ul>
        )}
      </Modal>
    </>
  );
}

RecordatoriosCamadaModal.propTypes = {
  camada: PropTypes.shape({
    id_camada: PropTypes.number.isRequired,
    nombre_camada: PropTypes.string.isRequired,
  }).isRequired,
  onCerrar: PropTypes.func.isRequired,
};

export default RecordatoriosCamadaModal;
