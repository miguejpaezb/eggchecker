import PropTypes from 'prop-types';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import AuthCard from '../components/AuthCard';
import AuthLayout from '../components/AuthLayout';
import LoginForm from '../components/LoginForm';
import RegisterForm from '../components/RegisterForm';

function Auth({ mode }) {
  const navigate = useNavigate();
  const [correoRegistrado, setCorreoRegistrado] = useState('');
  const esRegistro = mode === 'register';

  const alternar = () => {
    if (esRegistro) {
      navigate('/login');
      return;
    }
    setCorreoRegistrado('');
    navigate('/register');
  };

  const handleRegistrado = (correo) => {
    setCorreoRegistrado(correo);
    navigate('/login');
  };

  return (
    <AuthLayout register={esRegistro}>
      <AuthCard>
        <div key={mode} className="ec-auth__form-swap">
          {esRegistro ? (
            <RegisterForm onRegistrado={handleRegistrado} />
          ) : (
            <LoginForm
              correoInicial={correoRegistrado}
              mensajeExito={
                correoRegistrado
                  ? 'Cuenta creada con éxito. Inicia sesión para continuar.'
                  : ''
              }
            />
          )}
        </div>
      </AuthCard>
      <div className="ec-auth__footer">
        <p>
          {esRegistro ? '¿Ya tienes cuenta? ' : '¿No tienes cuenta? '}
          <button type="button" className="ec-auth__switch" onClick={alternar}>
            {esRegistro ? 'Iniciar sesión' : 'Crear cuenta'}
          </button>
        </p>
      </div>
    </AuthLayout>
  );
}

Auth.propTypes = {
  mode: PropTypes.oneOf(['login', 'register']).isRequired,
};

export default Auth;
