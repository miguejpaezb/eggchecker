import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import Modal from './Modal';

/**
 * Modal para registrar mortalidad, con validación y confirmación previa.
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si la modal debe mostrarse.
 * @param {Object|null} props.camada - Camada afectada por la mortalidad.
 * @param {Function} props.onCerrar - Acción al cerrar la modal.
 * @param {Function} props.onRegistrar - Registra la mortalidad; recibe (id, cantidad).
 * @returns {JSX.Element} La modal de mortalidad.
 */
function MortalidadModal({ abierto, camada, onCerrar, onRegistrar }) {
  const [cantidad, setCantidad] = useState('');
  const [confirmando, setConfirmando] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (abierto) {
      setCantidad('');
      setConfirmando(false);
      setEnviando(false);
      setError('');
    }
  }, [abierto]);

  const handleCerrar = () => {
    setConfirmando(false);
    onCerrar();
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    setError('');

    if (!camada) {
      return;
    }

    const numero = Number(cantidad);
    if (!Number.isInteger(numero) || numero <= 0) {
      setError('La cantidad debe ser un número entero mayor que cero');
      return;
    }
    if (numero > camada.cantidad_actual) {
      setError('La cantidad no puede superar las aves actuales');
      return;
    }
    setConfirmando(true);
  };

  const handleConfirmar = async () => {
    if (!camada) {
      return;
    }

    setError('');
    setEnviando(true);
    try {
      await onRegistrar(camada.id_camada, Number(cantidad));
      setConfirmando(false);
      onCerrar();
    } catch (err) {
      setError(err.message);
      setConfirmando(false);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal
      abierto={abierto}
      titulo="Registrar Mortalidad"
      onCerrar={handleCerrar}
    >
      {confirmando && camada ? (
        <>
          <p className="ec-confirm__mensaje">
            ¿Confirmas registrar {cantidad} aves muertas en{' '}
            <strong>{camada.nombre_camada}</strong>? Se descontarán de la
            cantidad actual.
          </p>
          {error && <p className="ec-form__error">{error}</p>}
          <div className="ec-form__actions">
            <button
              type="button"
              className="ec-btn ec-btn--ghost"
              onClick={() => setConfirmando(false)}
              disabled={enviando}
            >
              Volver
            </button>
            <button
              type="button"
              className="ec-btn ec-btn--danger"
              onClick={handleConfirmar}
              disabled={enviando}
            >
              {enviando ? 'Procesando…' : 'Confirmar'}
            </button>
          </div>
        </>
      ) : (
        <form className="ec-form" onSubmit={handleSubmit}>
          {error && <p className="ec-form__error">{error}</p>}

          <label className="ec-form__field" htmlFor="mortalidad-cantidad">
            <span className="ec-form__label">Cantidad de aves muertas</span>
            <input
              id="mortalidad-cantidad"
              className="ec-form__input"
              type="number"
              min="1"
              step="1"
              value={cantidad}
              onChange={(event) => setCantidad(event.target.value)}
              placeholder="Ej. 3"
              required
            />
            <span className="ec-form__hint">
              Aves actuales: {camada ? camada.cantidad_actual : 0}
            </span>
          </label>

          <div className="ec-form__actions">
            <button
              type="button"
              className="ec-btn ec-btn--ghost"
              onClick={handleCerrar}
            >
              Cancelar
            </button>
            <button type="submit" className="ec-btn ec-btn--primary">
              Continuar
            </button>
          </div>
        </form>
      )}
    </Modal>
  );
}

MortalidadModal.propTypes = {
  abierto: PropTypes.bool,
  camada: PropTypes.shape({
    id_camada: PropTypes.number.isRequired,
    nombre_camada: PropTypes.string.isRequired,
    cantidad_actual: PropTypes.number.isRequired,
  }),
  onCerrar: PropTypes.func.isRequired,
  onRegistrar: PropTypes.func.isRequired,
};

MortalidadModal.defaultProps = {
  abierto: false,
  camada: null,
};

export default MortalidadModal;
