import PropTypes from 'prop-types';

import { formatearTelefono, soloDigitos } from '../utils/cliente';

/**
 * Campo de teléfono que solo acepta dígitos y muestra el formato 3-3-4.
 * Al padre le entrega el valor sin espacios (solo dígitos).
 * @param {Object} props - Propiedades del componente.
 * @param {string} props.id - Identificador del input.
 * @param {string} props.value - Teléfono actual en dígitos.
 * @param {Function} props.onChange - Acción al cambiar con el valor digitado.
 * @param {string} [props.placeholder] - Texto de ayuda.
 * @returns {JSX.Element} El input de teléfono controlado.
 */
function InputTelefono({ id, value, onChange, placeholder }) {
  const handleChange = (event) => {
    onChange(soloDigitos(event.target.value));
  };

  return (
    <input
      id={id}
      className="ec-form__input"
      type="tel"
      inputMode="numeric"
      autoComplete="tel"
      value={formatearTelefono(value)}
      onChange={handleChange}
      placeholder={placeholder}
    />
  );
}

InputTelefono.propTypes = {
  id: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
};

InputTelefono.defaultProps = {
  placeholder: '',
};

export default InputTelefono;
