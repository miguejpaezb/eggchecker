import PropTypes from 'prop-types';

// Solo dígitos y un punto decimal opcional (sin coma ni otros caracteres).
const PATRON_DECIMAL = /^\d*\.?\d*$/;

/**
 * Campo de texto que solo acepta dígitos y punto decimal.
 * @param {Object} props - Propiedades del componente.
 * @param {string} props.id - Identificador del input.
 * @param {string} props.value - Valor actual del campo.
 * @param {Function} props.onChange - Acción al cambiar con un valor válido.
 * @param {Function} props.onInvalid - Acción al teclear un carácter inválido.
 * @param {string} [props.placeholder] - Texto de ayuda.
 * @returns {JSX.Element} El input decimal controlado.
 */
function InputDecimal({ id, value, onChange, onInvalid, placeholder }) {
  const handleChange = (event) => {
    const texto = event.target.value;
    if (PATRON_DECIMAL.test(texto)) {
      onChange(texto);
      onInvalid('');
    } else {
      onInvalid(
        'Usa punto (.) para los decimales, no coma ni otros caracteres'
      );
    }
  };

  return (
    <input
      id={id}
      className="ec-form__input"
      type="text"
      inputMode="decimal"
      value={value}
      onChange={handleChange}
      placeholder={placeholder}
      required
    />
  );
}

InputDecimal.propTypes = {
  id: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  onInvalid: PropTypes.func,
  placeholder: PropTypes.string,
};

InputDecimal.defaultProps = {
  onInvalid: () => {},
  placeholder: '',
};

export default InputDecimal;
