import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';

import AuthInput from '../components/AuthInput';
import AuthLayout from '../components/AuthLayout';
import { guardarToken, iniciarSesion } from '../services/authService';

function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const registrado = Boolean(location.state?.registrado);
  const [form, setForm] = useState({
    correo_electronico: '',
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
    <AuthLayout>
      <div className="ec-auth__card">
        <h2 className="ec-auth__heading">Bienvenido</h2>
        {registrado && (
          <div className="ec-auth__alert ec-auth__alert--success">
            Cuenta creada con éxito. Inicia sesión para continuar.
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
      </div>
      <div className="ec-auth__footer">
        <p>
          ¿No tienes cuenta? <Link to="/register">Crear cuenta</Link>
        </p>
      </div>
    </AuthLayout>
  );
}

export default Login;
