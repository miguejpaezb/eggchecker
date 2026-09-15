import PropTypes from 'prop-types';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import AuthCard from '../components/AuthCard';
import AuthLayout from '../components/AuthLayout';
import LoginForm from '../components/LoginForm';
import RecoverForm from '../components/RecoverForm';
import RegisterForm from '../components/RegisterForm';

function Auth({ mode }) {
  const navigate = useNavigate();
  const [correoRegistrado, setCorreoRegistrado] = useState('');
  const esRegistro = mode === 'register';
  const esRecuperar = mode === 'recuperar';
  const esLogin = mode === 'login';

  const irA = (destino) => {
    setCorreoRegistrado('');
    navigate(destino);
  };

  const handleRegistrado = (correo) => {
    setCorreoRegistrado(correo);
    navigate('/login');
  };

  const renderFormulario = () => {
    if (esRegistro) {
      return <RegisterForm onRegistrado={handleRegistrado} />;
    }
    if (esRecuperar) {
      return <RecoverForm />;
    }
    return (
      <LoginForm
        correoInicial={correoRegistrado}
        mensajeExito={
          correoRegistrado
            ? 'Cuenta creada con éxito. Inicia sesión para continuar.'
            : ''
        }
      />
    );
  };

  const pregunta = esLogin ? '¿No tienes cuenta? ' : '¿Ya tienes cuenta? ';
  const etiquetaToggle = esLogin ? 'Crear cuenta' : 'Iniciar sesión';
  const destinoToggle = esLogin ? '/register' : '/login';

  return (
    <AuthLayout register={esRegistro}>
      <AuthCard>
        <div key={mode} className="ec-auth__form-swap">
          {renderFormulario()}
        </div>
      </AuthCard>
      <div className="ec-auth__footer">
        {esRecuperar ? (
          <p>
            <button
              type="button"
              className="ec-auth__switch"
              onClick={() => irA('/login')}
            >
              Volver a iniciar sesión
            </button>
          </p>
        ) : (
          <p>
            {pregunta}
            <button
              type="button"
              className="ec-auth__switch"
              onClick={() => irA(destinoToggle)}
            >
              {etiquetaToggle}
            </button>
          </p>
        )}
      </div>
    </AuthLayout>
  );
}

Auth.propTypes = {
  mode: PropTypes.oneOf(['login', 'register', 'recuperar']).isRequired,
};

export default Auth;
