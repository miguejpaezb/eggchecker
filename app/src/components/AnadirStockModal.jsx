import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import { formatearCantidad } from '../utils/inventario';
import InputDecimal from './InputDecimal';
import Modal from './Modal';

/**
 * Modal para añadir stock a un insumo (movimiento de entrada).
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si la modal debe mostrarse.
 * @param {Object|null} props.insumo - Insumo al que se le suma stock.
 * @param {Function} props.onCerrar - Acción al cerrar la modal.
 * @param {Function} props.onAnadir - Suma stock; recibe (id, cantidad).
 * @returns {JSX.Element} La modal de añadir stock.
 */
function AnadirStockModal({ abierto, insumo, onCerrar, onAnadir }) {
  const [cantidad, setCantidad] = useState('');
  const [avisoDecimal, setAvisoDecimal] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (abierto) {
      setCantidad('');
      setAvisoDecimal('');
      setError('');
      setEnviando(false);
    }
  }, [abierto]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    const numero = Number(cantidad);
    if (!(numero > 0)) {
      setError('La cantidad debe ser un número mayor que cero');
      return;
    }

    setEnviando(true);
    try {
      await onAnadir(insumo.id_insumo, numero);
      onCerrar();
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal abierto={abierto} titulo="Añadir Stock" onCerrar={onCerrar}>
      {insumo && (
        <div className="ec-stock-resumen">
          <p className="ec-stock-resumen__nombre">{insumo.nombre_insumo}</p>
          <p className="ec-stock-resumen__dato">
            Stock actual: {formatearCantidad(insumo.stock_actual)}{' '}
            {insumo.unidad_medida}
          </p>
          <p className="ec-stock-resumen__dato">
            Stock mínimo: {formatearCantidad(insumo.umbral_minimo)}{' '}
            {insumo.unidad_medida}
          </p>
        </div>
      )}

      <form className="ec-form" onSubmit={handleSubmit}>
        {error && <p className="ec-form__error">{error}</p>}

        <label className="ec-form__field" htmlFor="anadir-stock-cantidad">
          <span className="ec-form__label">Cantidad a añadir</span>
          <InputDecimal
            id="anadir-stock-cantidad"
            value={cantidad}
            onChange={setCantidad}
            onInvalid={setAvisoDecimal}
            placeholder="Ej. 20"
          />
          <span className="ec-form__hint">
            La cantidad se suma al stock actual, sin importar el mínimo.
          </span>
        </label>

        {avisoDecimal && (
          <p className="ec-form__hint ec-form__hint--aviso">{avisoDecimal}</p>
        )}

        <div className="ec-form__actions">
          <button
            type="button"
            className="ec-btn ec-btn--ghost"
            onClick={onCerrar}
            disabled={enviando}
          >
            Cancelar
          </button>
          <button
            type="submit"
            className="ec-btn ec-btn--primary"
            disabled={enviando}
          >
            {enviando ? 'Guardando…' : 'Añadir stock'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

AnadirStockModal.propTypes = {
  abierto: PropTypes.bool,
  insumo: PropTypes.shape({
    id_insumo: PropTypes.number.isRequired,
    nombre_insumo: PropTypes.string.isRequired,
    unidad_medida: PropTypes.string.isRequired,
    stock_actual: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
      .isRequired,
    umbral_minimo: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
      .isRequired,
  }),
  onCerrar: PropTypes.func.isRequired,
  onAnadir: PropTypes.func.isRequired,
};

AnadirStockModal.defaultProps = {
  abierto: false,
  insumo: null,
};

export default AnadirStockModal;
