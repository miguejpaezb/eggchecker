import PropTypes from 'prop-types';

/**
 * Interruptor accesible para activar o desactivar una preferencia.
 * @param {Object} props - Propiedades del componente.
 * @param {string} props.id - Identificador del switch.
 * @param {string} props.label - Texto accesible del switch.
 * @param {boolean} props.checked - Estado actual.
 * @param {Function} props.onChange - Acción al cambiar; recibe el nuevo estado.
 * @param {boolean} [props.disabled] - Si el switch está deshabilitado.
 * @returns {JSX.Element} El interruptor.
 */
function Switch({ id, label, checked, onChange, disabled }) {
  return (
    <button
      type="button"
      id={id}
      role="switch"
      aria-label={label}
      aria-checked={checked}
      className={`ec-switch${checked ? ' ec-switch--on' : ''}`}
      onClick={() => onChange(!checked)}
      disabled={disabled}
    >
      <span className="ec-switch__thumb" />
    </button>
  );
}

Switch.propTypes = {
  id: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  checked: PropTypes.bool.isRequired,
  onChange: PropTypes.func.isRequired,
  disabled: PropTypes.bool,
};

Switch.defaultProps = {
  disabled: false,
};

export default Switch;
