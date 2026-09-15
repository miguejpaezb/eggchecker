import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import AuthInput from '../components/AuthInput';
import AuthLayout from '../components/AuthLayout';
import { registrar } from '../services/authService';

const CORREO_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validarFormulario(form) {
  const errores = {};
  if (form.nombre_completo.trim().length < 2) {
    errores.nombre_completo = 'Ingresa tu nombre completo';
  }
  if (!CORREO_VALIDO.test(form.correo_electronico)) {
    errores.correo_electronico = 'Ingresa un correo electrónico válido';
  }
  if (form.contrasena.length < 8) {
    errores.contrasena = 'Debe tener al menos 8 caracteres';
  } else if (!/[A-Z]/.test(form.contrasena)) {
    errores.contrasena = 'Debe incluir al menos una mayúscula';
  } else if (!/[0-9]/.test(form.contrasena)) {
    errores.contrasena = 'Debe incluir al menos un número';
  } else if (!/[^A-Za-z0-9]/.test(form.contrasena)) {
    errores.contrasena = 'Debe incluir al menos un símbolo';
  }
  if (form.confirmar !== form.contrasena) {
    errores.confirmar = 'Las contraseñas no coinciden';
  }
  return errores;
}

function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    nombre_completo: '',
    correo_electronico: '',
    contrasena: '',
    confirmar: '',
  });
  const [errores, setErrores] = useState({});
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    const nuevosErrores = validarFormulario(form);
    setErrores(nuevosErrores);
    if (Object.keys(nuevosErrores).length > 0) {
      return;
    }
    setEnviando(true);
    try {
      await registrar({
        nombre_completo: form.nombre_completo,
        correo_electronico: form.correo_electronico,
        contrasena: form.contrasena,
      });
      navigate('/login', { state: { registrado: true } });
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <AuthLayout register>
      <div className="ec-auth__card">
        <h2 className="ec-auth__heading">Crear cuenta</h2>
        {error && (
          <div className="ec-auth__alert ec-auth__alert--error">{error}</div>
        )}
        <form
          className="ec-auth__form ec-auth__form--register"
          onSubmit={handleSubmit}
          noValidate
        >
          <AuthInput
            id="name"
            name="nombre_completo"
            type="text"
            label="Nombre Completo"
            icon="/assets/icons/user-icon.svg"
            placeholder="Juan Pérez"
            autoComplete="name"
            value={form.nombre_completo}
            error={errores.nombre_completo}
            onChange={handleChange}
          />
          <AuthInput
            id="email"
            name="correo_electronico"
            type="email"
            label="Correo electrónico"
            icon="/assets/icons/mail-icon.svg"
            placeholder="tucorreo@email.com"
            autoComplete="email"
            value={form.correo_electronico}
            error={errores.correo_electronico}
            onChange={handleChange}
          />
          <AuthInput
            id="password"
            name="contrasena"
            type="password"
            label="Contraseña"
            icon="/assets/icons/lock-icon.svg"
            placeholder="••••••••"
            autoComplete="new-password"
            value={form.contrasena}
            error={errores.contrasena}
            onChange={handleChange}
          />
          <AuthInput
            id="confirm_password"
            name="confirmar"
            type="password"
            label="Confirmar contraseña"
            icon="/assets/icons/lock-icon.svg"
            placeholder="••••••••"
            autoComplete="new-password"
            value={form.confirmar}
            error={errores.confirmar}
            onChange={handleChange}
          />
          <button type="submit" className="ec-auth__submit" disabled={enviando}>
            {enviando ? 'Creando cuenta…' : 'Crear cuenta'}
          </button>
        </form>
      </div>
      <div className="ec-auth__footer">
        <p>
          ¿Ya tienes cuenta? <Link to="/login">Iniciar sesión</Link>
        </p>
      </div>
    </AuthLayout>
  );
}

export default Register;
