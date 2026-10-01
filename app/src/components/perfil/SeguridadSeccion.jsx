import PropTypes from 'prop-types';
import { useState } from 'react';

/**
 * Sección para cambiar la contraseña del usuario.
 * @param {Object} props - Propiedades del componente.
 * @param {Function} props.onGuardar - Cambia la contraseña.
 * @returns {JSX.Element} El formulario de seguridad.
 */
function SeguridadSeccion({ onGuardar }) {
  const [actual, setActual] = useState('');
  const [nueva, setNueva] = useState('');
  const [confirmar, setConfirmar] = useState('');
  const [error, setError] = useState('');
  const [exito, setExito] = useState('');
  const [enviando, setEnviando] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setExito('');

    if (nueva !== confirmar) {
      setError('Las contraseñas nuevas no coinciden');
      return;
    }

    setEnviando(true);
    try {
      await onGuardar({
        contrasena_actual: actual,
        contrasena_nueva: nueva,
      });
      setActual('');
      setNueva('');
      setConfirmar('');
      setExito('Contraseña actualizada correctamente');
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <form className="ec-perfil__form" onSubmit={handleSubmit}>
      <section className="ec-perfil__bloque">
        <h2 className="ec-perfil__seccion-titulo">Seguridad de la Cuenta</h2>
        <h3 className="ec-perfil__subtitulo">Cambio de Contraseña</h3>

        {error && <p className="ec-form__error">{error}</p>}
        {exito && <p className="ec-perfil__exito">{exito}</p>}

        <label className="ec-form__field" htmlFor="seguridad-actual">
          <span className="ec-form__label">Contraseña Actual</span>
          <input
            id="seguridad-actual"
            className="ec-form__input"
            type="password"
            value={actual}
            onChange={(event) => setActual(event.target.value)}
            autoComplete="current-password"
            required
          />
        </label>

        <label className="ec-form__field" htmlFor="seguridad-nueva">
          <span className="ec-form__label">Nueva Contraseña</span>
          <input
            id="seguridad-nueva"
            className="ec-form__input"
            type="password"
            value={nueva}
            onChange={(event) => setNueva(event.target.value)}
            autoComplete="new-password"
            minLength={8}
            maxLength={72}
            required
          />
        </label>

        <label className="ec-form__field" htmlFor="seguridad-confirmar">
          <span className="ec-form__label">Confirmar Contraseña</span>
          <input
            id="seguridad-confirmar"
            className="ec-form__input"
            type="password"
            value={confirmar}
            onChange={(event) => setConfirmar(event.target.value)}
            autoComplete="new-password"
            minLength={8}
            maxLength={72}
            required
          />
        </label>

        <div className="ec-form__actions">
          <button
            type="submit"
            className="ec-btn ec-btn--primary"
            disabled={enviando}
          >
            {enviando ? 'Actualizando…' : 'Actualizar Contraseña'}
          </button>
        </div>
      </section>
    </form>
  );
}

SeguridadSeccion.propTypes = {
  onGuardar: PropTypes.func.isRequired,
};

export default SeguridadSeccion;
