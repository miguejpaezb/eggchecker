import { useState } from 'react';
import { Link } from 'react-router-dom';

import { recuperar } from '../services/authService';
import AuthInput from './AuthInput';

function RecoverForm() {
  const [correo, setCorreo] = useState('');
  const [error, setError] = useState(null);
  const [mensaje, setMensaje] = useState('');
  const [enviando, setEnviando] = useState(false);

  const handleChange = (event) => {
    setCorreo(event.target.value);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    setMensaje('');
    setEnviando(true);
    try {
      const respuesta = await recuperar({ correo_electronico: correo });
      setMensaje(respuesta.mensaje);
    } catch (err) {
      setError({ mensaje: err.message, sinCuenta: err.status === 404 });
    } finally {
      setEnviando(false);
    }
  };

  // El backend marca el correo inexistente con 404; en ese caso se ofrece
  // el enlace de registro dentro del propio mensaje.
  const partesRegistro =
    error?.sinCuenta && error.mensaje.includes('registrarte')
      ? error.mensaje.split('registrarte')
      : null;

  return (
    <>
      <h2 className="ec-auth__heading">Recuperar contraseña</h2>
      {mensaje && (
        <div className="ec-auth__alert ec-auth__alert--success">{mensaje}</div>
      )}
      {error && (
        <div className="ec-auth__alert ec-auth__alert--error">
          {partesRegistro ? (
            <>
              {partesRegistro[0]}
              <Link to="/register">registrarte</Link>
              {partesRegistro[1]}
            </>
          ) : (
            error.mensaje
          )}
        </div>
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
          value={correo}
          onChange={handleChange}
        />
        <button type="submit" className="ec-auth__submit" disabled={enviando}>
          {enviando ? 'Enviando…' : 'Enviar solicitud'}
        </button>
      </form>
    </>
  );
}

export default RecoverForm;
