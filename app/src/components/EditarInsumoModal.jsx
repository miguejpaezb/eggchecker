import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import InputDecimal from './InputDecimal';
import Modal from './Modal';

/**
 * Modal para editar nombre, unidad de medida y stock mínimo de un insumo.
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si la modal debe mostrarse.
 * @param {Object|null} props.insumo - Insumo a editar.
 * @param {Function} props.onCerrar - Acción al cerrar la modal.
 * @param {Function} props.onGuardar - Guarda los cambios; recibe (id, datos).
 * @returns {JSX.Element} La modal de edición de insumo.
 */
function EditarInsumoModal({ abierto, insumo, onCerrar, onGuardar }) {
  const [nombre, setNombre] = useState('');
  const [unidad, setUnidad] = useState('');
  const [umbral, setUmbral] = useState('');
  const [avisoDecimal, setAvisoDecimal] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (abierto && insumo) {
      setNombre(insumo.nombre_insumo);
      setUnidad(insumo.unidad_medida);
      setUmbral(String(insumo.umbral_minimo));
      setAvisoDecimal('');
      setError('');
      setEnviando(false);
    }
  }, [abierto, insumo]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    const umbralNumero = Number(umbral);
    if (!(umbralNumero > 0)) {
      setError('El stock mínimo debe ser un número mayor que cero');
      return;
    }

    setEnviando(true);
    try {
      await onGuardar(insumo.id_insumo, {
        nombre_insumo: nombre.trim(),
        unidad_medida: unidad.trim(),
        umbral_minimo: umbralNumero,
      });
      onCerrar();
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal abierto={abierto} titulo="Editar Insumo" onCerrar={onCerrar}>
      <form className="ec-form" onSubmit={handleSubmit}>
        {error && <p className="ec-form__error">{error}</p>}

        <label className="ec-form__field" htmlFor="editar-insumo-nombre">
          <span className="ec-form__label">Nombre del insumo</span>
          <input
            id="editar-insumo-nombre"
            className="ec-form__input"
            type="text"
            value={nombre}
            onChange={(event) => setNombre(event.target.value)}
            maxLength={80}
            required
          />
        </label>

        <label className="ec-form__field" htmlFor="editar-insumo-unidad">
          <span className="ec-form__label">Unidad de medida</span>
          <input
            id="editar-insumo-unidad"
            className="ec-form__input"
            type="text"
            value={unidad}
            onChange={(event) => setUnidad(event.target.value)}
            maxLength={20}
            required
          />
        </label>

        <label className="ec-form__field" htmlFor="editar-insumo-umbral">
          <span className="ec-form__label">Stock mínimo</span>
          <InputDecimal
            id="editar-insumo-umbral"
            value={umbral}
            onChange={setUmbral}
            onInvalid={setAvisoDecimal}
          />
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
            {enviando ? 'Guardando…' : 'Guardar cambios'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

EditarInsumoModal.propTypes = {
  abierto: PropTypes.bool,
  insumo: PropTypes.shape({
    id_insumo: PropTypes.number.isRequired,
    nombre_insumo: PropTypes.string.isRequired,
    unidad_medida: PropTypes.string.isRequired,
    umbral_minimo: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
      .isRequired,
  }),
  onCerrar: PropTypes.func.isRequired,
  onGuardar: PropTypes.func.isRequired,
};

EditarInsumoModal.defaultProps = {
  abierto: false,
  insumo: null,
};

export default EditarInsumoModal;
