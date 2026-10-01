import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import Modal from './Modal';

/**
 * Modal de confirmación para eliminar un cliente.
 * Exige la contraseña del usuario antes de borrar de forma permanente.
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si la modal debe mostrarse.
 * @param {Object|null} props.cliente - Cliente a eliminar.
 * @param {Function} props.onCerrar - Acción al cancelar/cerrar.
 * @param {Function} props.onEliminar - Borra el cliente; recibe la contraseña.
 * @returns {JSX.Element} La modal de confirmación de borrado.
 */
function EliminarClienteModal({ abierto, cliente, onCerrar, onEliminar }) {
  const [contrasena, setContrasena] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (abierto) {
      setContrasena('');
      setError('');
      setEnviando(false);
    }
  }, [abierto]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    if (!contrasena) {
      setError('Ingresa tu contraseña para confirmar');
      return;
    }

    setEnviando(true);
    try {
      await onEliminar(contrasena);
      onCerrar();
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal abierto={abierto} titulo="Eliminar cliente" onCerrar={onCerrar}>
      <form className="ec-form" onSubmit={handleSubmit}>
        <p className="ec-confirm__mensaje">
          {cliente
            ? `¿Eliminar "${cliente.nombre_cliente}"? Esta acción borra el cliente de la base de datos y no se puede revertir.`
            : ''}
        </p>

        {error && <p className="ec-form__error">{error}</p>}

        <label className="ec-form__field" htmlFor="eliminar-cliente-contrasena">
          <span className="ec-form__label">Contraseña de tu usuario</span>
          <input
            id="eliminar-cliente-contrasena"
            className="ec-form__input"
            type="password"
            value={contrasena}
            onChange={(event) => setContrasena(event.target.value)}
            autoComplete="current-password"
            placeholder="Confirma tu contraseña"
            required
          />
        </label>

        <div className="ec-form__actions">
          <button
            type="button"
            className="ec-btn ec-btn--ghost"
            onClick={onCerrar}
            disabled={enviando}
          >
            Cancelar
          </button>
          <button
            type="submit"
            className="ec-btn ec-btn--danger"
            disabled={enviando}
          >
            {enviando ? 'Eliminando…' : 'Eliminar cliente'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

EliminarClienteModal.propTypes = {
  abierto: PropTypes.bool,
  cliente: PropTypes.shape({
    nombre_cliente: PropTypes.string.isRequired,
  }),
  onCerrar: PropTypes.func.isRequired,
  onEliminar: PropTypes.func.isRequired,
};

EliminarClienteModal.defaultProps = {
  abierto: false,
  cliente: null,
};

export default EliminarClienteModal;
