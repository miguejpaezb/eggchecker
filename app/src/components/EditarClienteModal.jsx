import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import InputTelefono from './InputTelefono';
import Modal from './Modal';

/**
 * Modal para editar nombre, teléfono y dirección de un cliente.
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si la modal debe mostrarse.
 * @param {Object|null} props.cliente - Cliente a editar.
 * @param {Function} props.onCerrar - Acción al cerrar la modal.
 * @param {Function} props.onGuardar - Guarda los cambios; recibe (id, datos).
 * @returns {JSX.Element} La modal de edición de cliente.
 */
function EditarClienteModal({ abierto, cliente, onCerrar, onGuardar }) {
  const [nombre, setNombre] = useState('');
  const [telefono, setTelefono] = useState('');
  const [direccion, setDireccion] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (abierto && cliente) {
      setNombre(cliente.nombre_cliente);
      setTelefono(cliente.telefono ?? '');
      setDireccion(cliente.direccion ?? '');
      setError('');
      setEnviando(false);
    }
  }, [abierto, cliente]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    if (!nombre.trim()) {
      setError('El nombre del cliente es obligatorio');
      return;
    }

    setEnviando(true);
    try {
      await onGuardar(cliente.id_cliente, {
        nombre_cliente: nombre.trim(),
        telefono: telefono.trim() || null,
        direccion: direccion.trim() || null,
      });
      onCerrar();
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal abierto={abierto} titulo="Editar Cliente" onCerrar={onCerrar}>
      <form className="ec-form" onSubmit={handleSubmit}>
        {error && <p className="ec-form__error">{error}</p>}

        <label className="ec-form__field" htmlFor="editar-cliente-nombre">
          <span className="ec-form__label">Nombre del cliente</span>
          <input
            id="editar-cliente-nombre"
            className="ec-form__input"
            type="text"
            value={nombre}
            onChange={(event) => setNombre(event.target.value)}
            maxLength={80}
            required
          />
        </label>

        <label className="ec-form__field" htmlFor="editar-cliente-telefono">
          <span className="ec-form__label">Teléfono (opcional)</span>
          <InputTelefono
            id="editar-cliente-telefono"
            value={telefono}
            onChange={setTelefono}
            placeholder="Ej. 300 123 4567"
          />
        </label>

        <label className="ec-form__field" htmlFor="editar-cliente-direccion">
          <span className="ec-form__label">Dirección (opcional)</span>
          <input
            id="editar-cliente-direccion"
            className="ec-form__input"
            type="text"
            value={direccion}
            onChange={(event) => setDireccion(event.target.value)}
            maxLength={200}
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
            className="ec-btn ec-btn--primary"
            disabled={enviando}
          >
            {enviando ? 'Guardando…' : 'Guardar cambios'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

EditarClienteModal.propTypes = {
  abierto: PropTypes.bool,
  cliente: PropTypes.shape({
    id_cliente: PropTypes.number.isRequired,
    nombre_cliente: PropTypes.string.isRequired,
    telefono: PropTypes.string,
    direccion: PropTypes.string,
  }),
  onCerrar: PropTypes.func.isRequired,
  onGuardar: PropTypes.func.isRequired,
};

EditarClienteModal.defaultProps = {
  abierto: false,
  cliente: null,
};

export default EditarClienteModal;
