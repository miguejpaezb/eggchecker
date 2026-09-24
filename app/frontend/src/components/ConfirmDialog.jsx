import PropTypes from 'prop-types';

import Modal from './Modal';

/**
 * Diálogo de confirmación reutilizable para acciones sensibles.
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si el diálogo debe mostrarse.
 * @param {string} props.titulo - Título del diálogo.
 * @param {string} props.mensaje - Texto de la advertencia.
 * @param {string} [props.textoConfirmar] - Texto del botón de confirmación.
 * @param {boolean} [props.peligro] - Si la acción es destructiva (rojo).
 * @param {boolean} [props.cargando] - Si la acción está en curso.
 * @param {string} [props.error] - Mensaje de error a mostrar.
 * @param {Function} props.onConfirmar - Acción al confirmar.
 * @param {Function} props.onCerrar - Acción al cancelar/cerrar.
 * @returns {JSX.Element} El diálogo de confirmación.
 */
function ConfirmDialog({
  abierto,
  titulo,
  mensaje,
  textoConfirmar,
  peligro,
  cargando,
  error,
  onConfirmar,
  onCerrar,
}) {
  return (
    <Modal abierto={abierto} titulo={titulo} onCerrar={onCerrar}>
      <p className="ec-confirm__mensaje">{mensaje}</p>
      {error && <p className="ec-form__error">{error}</p>}
      <div className="ec-form__actions">
        <button
          type="button"
          className="ec-btn ec-btn--ghost"
          onClick={onCerrar}
          disabled={cargando}
        >
          Cancelar
        </button>
        <button
          type="button"
          className={`ec-btn ${peligro ? 'ec-btn--danger' : 'ec-btn--primary'}`}
          onClick={onConfirmar}
          disabled={cargando}
        >
          {cargando ? 'Procesando…' : textoConfirmar}
        </button>
      </div>
    </Modal>
  );
}

ConfirmDialog.propTypes = {
  abierto: PropTypes.bool,
  titulo: PropTypes.string.isRequired,
  mensaje: PropTypes.string.isRequired,
  textoConfirmar: PropTypes.string,
  peligro: PropTypes.bool,
  cargando: PropTypes.bool,
  error: PropTypes.string,
  onConfirmar: PropTypes.func.isRequired,
  onCerrar: PropTypes.func.isRequired,
};

ConfirmDialog.defaultProps = {
  abierto: false,
  textoConfirmar: 'Confirmar',
  peligro: false,
  cargando: false,
  error: '',
};

export default ConfirmDialog;
