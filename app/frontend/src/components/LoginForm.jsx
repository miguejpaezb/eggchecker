import PropTypes from 'prop-types';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { guardarToken, iniciarSesion } from '../services/authService';
import AuthInput from './AuthInput';

function LoginForm({ correoInicial, mensajeExito }) {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    correo_electronico: correoInicial,
    contrasena: '',
  });
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setEnviando(true);
    try {
      const { access_token: accessToken } = await iniciarSesion(form);
      guardarToken(accessToken);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <>
      <h2 className="ec-auth__heading">Bienvenido</h2>
      {mensajeExito && (
        <div className="ec-auth__alert ec-auth__alert--success">
          {mensajeExito}
        </div>
      )}
      {error && (
        <div className="ec-auth__alert ec-auth__alert--error">{error}</div>
      )}
      <form className="ec-auth__form" onSubmit={handleSubmit} noValidate>
        <AuthInput
          id="email"
          name="correo_electronico"
          type="email"
          label="Correo electrónico"
          icon="/assets/icons/mail-icon.svg"
          placeholder="tucorreo@email.com"
          autoComplete="email"
          value={form.correo_electronico}
          onChange={handleChange}
        />
        <AuthInput
          id="password"
          name="contrasena"
          type="password"
          label="Contraseña"
          icon="/assets/icons/lock-icon.svg"
          placeholder="••••••••"
          autoComplete="current-password"
          value={form.contrasena}
          onChange={handleChange}
        />
        <button type="submit" className="ec-auth__submit" disabled={enviando}>
          {enviando ? 'Ingresando…' : 'Iniciar sesión'}
        </button>
        <div className="ec-auth__forgot">
          <a href="#recuperar">Olvidé mi contraseña</a>
        </div>
      </form>
    </>
  );
}

LoginForm.propTypes = {
  correoInicial: PropTypes.string,
  mensajeExito: PropTypes.string,
};

LoginForm.defaultProps = {
  correoInicial: '',
  mensajeExito: '',
};

export default LoginForm;
