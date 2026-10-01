import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import InputTelefono from '../InputTelefono';

/**
 * Sección para editar los datos personales, la granja y el correo.
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.perfil - Perfil actual del usuario.
 * @param {Function} props.onGuardar - Guarda los cambios; recibe los datos.
 * @returns {JSX.Element} El formulario de edición de perfil.
 */
function EditarPerfilSeccion({ perfil, onGuardar }) {
  const [nombre, setNombre] = useState('');
  const [correo, setCorreo] = useState('');
  const [telefono, setTelefono] = useState('');
  const [granja, setGranja] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [error, setError] = useState('');
  const [exito, setExito] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (perfil) {
      setNombre(perfil.nombre_completo ?? '');
      setCorreo(perfil.correo_electronico ?? '');
      setTelefono(perfil.telefono ?? '');
      setGranja(perfil.nombre_granja ?? '');
      setContrasena('');
    }
  }, [perfil]);

  const correoActual = perfil?.correo_electronico ?? '';
  const cambioCorreo = correo.trim().toLowerCase() !== correoActual;

  const limpiarEstado = () => {
    if (perfil) {
      setNombre(perfil.nombre_completo ?? '');
      setCorreo(perfil.correo_electronico ?? '');
      setTelefono(perfil.telefono ?? '');
      setGranja(perfil.nombre_granja ?? '');
    }
    setContrasena('');
    setError('');
    setExito('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setExito('');

    if (!nombre.trim()) {
      setError('El nombre completo es obligatorio');
      return;
    }
    if (cambioCorreo && !contrasena) {
      setError('Ingresa tu contraseña actual para cambiar el correo');
      return;
    }

    setEnviando(true);
    try {
      await onGuardar({
        nombre_completo: nombre.trim(),
        correo_electronico: correo.trim().toLowerCase(),
        telefono: telefono.trim() || null,
        nombre_granja: granja.trim() || null,
        contrasena_actual: cambioCorreo ? contrasena : null,
      });
      setContrasena('');
      setExito('Cambios guardados correctamente');
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <form className="ec-perfil__form" onSubmit={handleSubmit}>
      {error && <p className="ec-form__error">{error}</p>}
      {exito && <p className="ec-perfil__exito">{exito}</p>}

      <section className="ec-perfil__bloque">
        <h2 className="ec-perfil__seccion-titulo">Información Personal</h2>

        <label className="ec-form__field" htmlFor="perfil-nombre">
          <span className="ec-form__label">Nombre Completo</span>
          <input
            id="perfil-nombre"
            className="ec-form__input"
            type="text"
            value={nombre}
            onChange={(event) => setNombre(event.target.value)}
            maxLength={100}
            required
          />
        </label>

        <label className="ec-form__field" htmlFor="perfil-correo">
          <span className="ec-form__label">Correo electrónico</span>
          <input
            id="perfil-correo"
            className="ec-form__input"
            type="email"
            value={correo}
            onChange={(event) => setCorreo(event.target.value)}
            maxLength={100}
            required
          />
        </label>

        {cambioCorreo && (
          <label className="ec-form__field" htmlFor="perfil-contrasena">
            <span className="ec-form__label">
              Contraseña actual (para cambiar el correo)
            </span>
            <input
              id="perfil-contrasena"
              className="ec-form__input"
              type="password"
              value={contrasena}
              onChange={(event) => setContrasena(event.target.value)}
              autoComplete="current-password"
            />
          </label>
        )}

        <label className="ec-form__field" htmlFor="perfil-telefono">
          <span className="ec-form__label">Número de teléfono</span>
          <InputTelefono
            id="perfil-telefono"
            value={telefono}
            onChange={setTelefono}
            placeholder="Ej. 300 123 4567"
          />
        </label>
      </section>

      <section className="ec-perfil__bloque">
        <h2 className="ec-perfil__seccion-titulo">Datos de la granja</h2>
        <label className="ec-form__field" htmlFor="perfil-granja">
          <span className="ec-form__label">Nombre de la granja</span>
          <input
            id="perfil-granja"
            className="ec-form__input"
            type="text"
            value={granja}
            onChange={(event) => setGranja(event.target.value)}
            maxLength={100}
            placeholder="Ej. Granja La Esperanza"
          />
        </label>
      </section>

      <div className="ec-form__actions">
        <button
          type="button"
          className="ec-btn ec-btn--ghost"
          onClick={limpiarEstado}
          disabled={enviando}
        >
          Cancelar
        </button>
        <button
          type="submit"
          className="ec-btn ec-btn--primary"
          disabled={enviando}
        >
          {enviando ? 'Guardando…' : 'Guardar Cambios'}
        </button>
      </div>
    </form>
  );
}

EditarPerfilSeccion.propTypes = {
  perfil: PropTypes.shape({
    nombre_completo: PropTypes.string,
    correo_electronico: PropTypes.string,
    telefono: PropTypes.string,
    nombre_granja: PropTypes.string,
  }),
  onGuardar: PropTypes.func.isRequired,
};

EditarPerfilSeccion.defaultProps = {
  perfil: null,
};

export default EditarPerfilSeccion;
