import { useState } from 'react';

import { recuperar } from '../services/authService';
import AuthInput from './AuthInput';

function RecoverForm() {
  const [correo, setCorreo] = useState('');
  const [error, setError] = useState('');
  const [mensaje, setMensaje] = useState('');
  const [enviando, setEnviando] = useState(false);

  const handleChange = (event) => {
    setCorreo(event.target.value);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setMensaje('');
    setEnviando(true);
    try {
      const respuesta = await recuperar({ correo_electronico: correo });
      setMensaje(respuesta.mensaje);
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <>
      <h2 className="ec-auth__heading">Recuperar contraseña</h2>
      {mensaje && (
        <div className="ec-auth__alert ec-auth__alert--success">{mensaje}</div>
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
