import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import {
  aplicarDosis,
  aplicarLote,
  listarDosis,
  obtenerRecordatorio,
} from '../../services/recordatorioService';
import {
  formatearFecha,
  formatoCantidad,
  textoCantidad,
} from '../../utils/recordatorios';
import ConfirmDialog from '../ConfirmDialog';
import Modal from '../Modal';

/**
 * Modal para registrar la aplicación de las dosis de un recordatorio.
 * Se abre desde el botón "Ver" del aviso y funciona en toda la app.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si el modal debe mostrarse.
 * @param {Object|null} props.solicitud - Recordatorio y fecha a aplicar.
 * @param {Function} props.onCerrar - Acción al cerrar.
 * @param {Function} props.onAplicado - Se llama tras registrar la aplicación.
 * @returns {JSX.Element} El modal de aplicación.
 */
function AplicarDosisModal({ abierto, solicitud, onCerrar, onAplicado }) {
  const [regla, setRegla] = useState(null);
  const [dosis, setDosis] = useState([]);
  const [cantidad, setCantidad] = useState('');
  const [seleccion, setSeleccion] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [errorCarga, setErrorCarga] = useState('');
  const [errorValidacion, setErrorValidacion] = useState('');
  const [confirmando, setConfirmando] = useState(false);
  const [aplicando, setAplicando] = useState(false);
  const [errorAplicar, setErrorAplicar] = useState('');

  useEffect(() => {
    if (!abierto || !solicitud) {
      return undefined;
    }

    let activo = true;
    setCargando(true);
    setErrorCarga('');
    setErrorValidacion('');
    setRegla(null);
    setDosis([]);
    setSeleccion([]);

    Promise.all([
      obtenerRecordatorio(solicitud.idRecordatorio),
      listarDosis(solicitud.idRecordatorio, {
        desde: solicitud.fecha,
        hasta: solicitud.fecha,
        soloPendientes: true,
      }),
    ])
      .then(([recordatorio, pendientes]) => {
        if (!activo) {
          return;
        }
        setRegla(recordatorio);
        setDosis(pendientes);
        setCantidad(formatoCantidad(recordatorio.cantidad));
        setSeleccion(pendientes.map((item) => item.id_dosis));
      })
      .catch((err) => {
        if (activo) {
          setErrorCarga(err.message);
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
  }, [abierto, solicitud]);

  const numeroCantidad = Number(cantidad);
  const cantidadValida = Number.isFinite(numeroCantidad) && numeroCantidad > 0;
  const total = cantidadValida ? numeroCantidad * seleccion.length : 0;

  const alternar = (idDosis) => {
    setSeleccion((previas) =>
      previas.includes(idDosis)
        ? previas.filter((id) => id !== idDosis)
        : [...previas, idDosis]
    );
    setErrorValidacion('');
  };

  const pedirConfirmacion = () => {
    if (seleccion.length === 0) {
      setErrorValidacion('Marca al menos una camada');
      return;
    }
    if (!cantidadValida) {
      setErrorValidacion('Escribe una cantidad mayor que cero');
      return;
    }
    setErrorValidacion('');
    setErrorAplicar('');
    setConfirmando(true);
  };

  const aplicar = async () => {
    setAplicando(true);
    setErrorAplicar('');
    try {
      if (seleccion.length === 1) {
        await aplicarDosis(seleccion[0], numeroCantidad);
      } else {
        await aplicarLote(seleccion, numeroCantidad);
      }
      setConfirmando(false);
      onAplicado();
      onCerrar();
    } catch (err) {
      setErrorAplicar(err.message);
    } finally {
      setAplicando(false);
    }
  };

  const cerrando = () => {
    setConfirmando(false);
    setErrorAplicar('');
    onCerrar();
  };

  const sinPendientes = !cargando && !errorCarga && dosis.length === 0;

  return (
    <>
      <Modal
        abierto={abierto}
        titulo="Registrar aplicación"
        onCerrar={cerrando}
      >
        {cargando && <p className="ec-form__hint">Cargando…</p>}

        {!cargando && errorCarga && (
          <p className="ec-form__error">{errorCarga}</p>
        )}

        {sinPendientes && (
          <>
            <p className="ec-form__hint">
              No hay dosis pendientes para esta fecha.
            </p>
            <div className="ec-form__actions">
              <button
                type="button"
                className="ec-btn ec-btn--ghost"
                onClick={cerrando}
              >
                Cerrar
              </button>
            </div>
          </>
        )}

        {!cargando && !errorCarga && !sinPendientes && regla && (
          <>
            <p className="ec-aplicar__insumo">{regla.nombre_insumo}</p>
            <p className="ec-aplicar__meta">
              Programado para {formatearFecha(solicitud?.fecha)} ·{' '}
              {textoCantidad(regla.cantidad, regla.unidad_medida)} por camada
            </p>
            <p className="ec-aplicar__stock">
              Stock actual:{' '}
              {textoCantidad(regla.stock_actual, regla.unidad_medida)}
            </p>

            <label className="ec-form__field" htmlFor="aplicar-cantidad">
              <span className="ec-form__label">
                Cantidad por camada ({regla.unidad_medida})
              </span>
              <input
                id="aplicar-cantidad"
                className="ec-form__input"
                type="number"
                min="0.01"
                step="0.01"
                value={cantidad}
                onChange={(event) => {
                  setCantidad(event.target.value);
                  setErrorValidacion('');
                }}
              />
            </label>

            <fieldset className="ec-aplicar__camadas">
              <legend className="ec-form__label">Camadas</legend>
              {dosis.map((item) => (
                <label
                  key={item.id_dosis}
                  className="ec-aplicar__camada"
                  htmlFor={`aplicar-${item.id_dosis}`}
                >
                  <input
                    id={`aplicar-${item.id_dosis}`}
                    type="checkbox"
                    checked={seleccion.includes(item.id_dosis)}
                    onChange={() => alternar(item.id_dosis)}
                  />
                  <span className="ec-aplicar__camada-nombre">
                    {item.nombre_camada}
                  </span>
                  <span className="ec-aplicar__camada-dosis">
                    dosis {item.numero_dosis} de {dosis.length}
                  </span>
                </label>
              ))}
            </fieldset>

            <p className="ec-aplicar__total">
              Total a descontar: {textoCantidad(total, regla.unidad_medida)} (
              {seleccion.length} camada(s))
            </p>

            {errorValidacion && (
              <p className="ec-form__error">{errorValidacion}</p>
            )}

            <div className="ec-form__actions">
              <button
                type="button"
                className="ec-btn ec-btn--ghost"
                onClick={cerrando}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="ec-btn ec-btn--primary"
                onClick={pedirConfirmacion}
              >
                Registrar aplicación
              </button>
            </div>
          </>
        )}
      </Modal>

      <ConfirmDialog
        abierto={confirmando}
        titulo="Confirmar aplicación"
        mensaje={`Se registrará la aplicación en ${seleccion.length} camada(s) y se descontarán ${textoCantidad(total, regla?.unidad_medida || '')} de ${regla?.nombre_insumo || ''}. Esta acción no se puede deshacer.`}
        textoConfirmar="Aplicar"
        cargando={aplicando}
        error={errorAplicar}
        onConfirmar={aplicar}
        onCerrar={() => setConfirmando(false)}
      />
    </>
  );
}

AplicarDosisModal.propTypes = {
  abierto: PropTypes.bool,
  solicitud: PropTypes.shape({
    idRecordatorio: PropTypes.number.isRequired,
    fecha: PropTypes.string.isRequired,
  }),
  onCerrar: PropTypes.func.isRequired,
  onAplicado: PropTypes.func.isRequired,
};

AplicarDosisModal.defaultProps = {
  abierto: false,
  solicitud: null,
};

export default AplicarDosisModal;
