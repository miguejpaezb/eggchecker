import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import InputTelefono from './InputTelefono';
import Modal from './Modal';

/**
 * Modal con el formulario para registrar un cliente nuevo.
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si la modal debe mostrarse.
 * @param {Function} props.onCerrar - Acción al cerrar la modal.
 * @param {Function} props.onCrear - Crea el cliente; recibe los datos.
 * @returns {JSX.Element} La modal de creación de cliente.
 */
function NuevoClienteModal({ abierto, onCerrar, onCrear }) {
  const [nombre, setNombre] = useState('');
  const [telefono, setTelefono] = useState('');
  const [direccion, setDireccion] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (abierto) {
      setNombre('');
      setTelefono('');
      setDireccion('');
      setError('');
      setEnviando(false);
    }
  }, [abierto]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    if (!nombre.trim()) {
      setError('El nombre del cliente es obligatorio');
      return;
    }

    setEnviando(true);
    try {
      await onCrear({
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
    <Modal abierto={abierto} titulo="Añadir Cliente" onCerrar={onCerrar}>
      <form className="ec-form" onSubmit={handleSubmit}>
        {error && <p className="ec-form__error">{error}</p>}

        <label className="ec-form__field" htmlFor="cliente-nombre">
          <span className="ec-form__label">Nombre del cliente</span>
          <input
            id="cliente-nombre"
            className="ec-form__input"
            type="text"
            value={nombre}
            onChange={(event) => setNombre(event.target.value)}
            placeholder="Ej. Tienda La Cosecha"
            maxLength={80}
            required
          />
        </label>

        <label className="ec-form__field" htmlFor="cliente-telefono">
          <span className="ec-form__label">Teléfono (opcional)</span>
          <InputTelefono
            id="cliente-telefono"
            value={telefono}
            onChange={setTelefono}
            placeholder="Ej. 300 123 4567"
          />
        </label>

        <label className="ec-form__field" htmlFor="cliente-direccion">
          <span className="ec-form__label">Dirección (opcional)</span>
          <input
            id="cliente-direccion"
            className="ec-form__input"
            type="text"
            value={direccion}
            onChange={(event) => setDireccion(event.target.value)}
            placeholder="Ej. Cra 3 # 8-15, Bogotá"
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
            {enviando ? 'Guardando…' : 'Registrar cliente'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

NuevoClienteModal.propTypes = {
  abierto: PropTypes.bool,
  onCerrar: PropTypes.func.isRequired,
  onCrear: PropTypes.func.isRequired,
};

NuevoClienteModal.defaultProps = {
  abierto: false,
};

export default NuevoClienteModal;
