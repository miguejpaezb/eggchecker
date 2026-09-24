import PropTypes from 'prop-types';
import { useState } from 'react';

function AuthInput({
  id,
  name,
  label,
  icon,
  type,
  value,
  placeholder,
  autoComplete,
  error,
  onChange,
}) {
  const [visible, setVisible] = useState(false);
  const esContrasena = type === 'password';
  const tipoInput = esContrasena && visible ? 'text' : type;
  const claseGrupo = error ? 'ec-input ec-input--invalid' : 'ec-input';

  return (
    <div className="ec-field">
      <label htmlFor={id} className="ec-field__label">
        {label}
      </label>
      <div className={claseGrupo}>
        <img src={icon} alt="" className="ec-input__icon" />
        <input
          id={id}
          name={name}
          type={tipoInput}
          value={value}
          placeholder={placeholder}
          autoComplete={autoComplete}
          onChange={onChange}
          className="ec-input__control"
        />
        {esContrasena && (
          <button
            type="button"
            className="ec-input__toggle"
            aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            onClick={() => setVisible((prev) => !prev)}
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
              <path
                fillRule="evenodd"
                d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z"
                clipRule="evenodd"
              />
            </svg>
          </button>
        )}
      </div>
      {error && <p className="ec-field__error">{error}</p>}
    </div>
  );
}

AuthInput.propTypes = {
  id: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  icon: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  value: PropTypes.string.isRequired,
  type: PropTypes.string,
  placeholder: PropTypes.string,
  autoComplete: PropTypes.string,
  error: PropTypes.string,
};

AuthInput.defaultProps = {
  type: 'text',
  placeholder: '',
  autoComplete: 'off',
  error: '',
};

export default AuthInput;
