import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import { formatearFecha } from '../utils/camada';
import Modal from './Modal';

/**
 * Modal para editar una camada: el nombre siempre y, dentro de las
 * primeras 24 horas, la cantidad inicial de aves.
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si la modal debe mostrarse.
 * @param {Object|null} props.camada - Camada a editar.
 * @param {Function} props.onCerrar - Acción al cerrar la modal.
 * @param {Function} props.onGuardar - Guarda los cambios; recibe (id, datos).
 * @returns {JSX.Element} La modal de edición.
 */
function EditarCamadaModal({ abierto, camada, onCerrar, onGuardar }) {
  const [nombreCamada, setNombreCamada] = useState('');
  const [cantidadInicial, setCantidadInicial] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  const puedeEditarInicial = Boolean(camada?.puede_editar_inicial);

  useEffect(() => {
    if (abierto && camada) {
      setNombreCamada(camada.nombre_camada);
      setCantidadInicial(String(camada.cantidad_inicial));
      setError('');
      setEnviando(false);
    }
  }, [abierto, camada]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    if (!camada) {
      return;
    }

    const cambios = { nombre_camada: nombreCamada.trim() };

    if (puedeEditarInicial) {
      const inicial = Number(cantidadInicial);
      if (!Number.isInteger(inicial) || inicial <= 0) {
        setError('La cantidad inicial debe ser un entero mayor que cero');
        return;
      }
      cambios.cantidad_inicial = inicial;
    }

    setEnviando(true);
    try {
      await onGuardar(camada.id_camada, cambios);
      onCerrar();
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal abierto={abierto} titulo="Editar Camada" onCerrar={onCerrar}>
      <form className="ec-form" onSubmit={handleSubmit}>
        {error && <p className="ec-form__error">{error}</p>}

        <label className="ec-form__field" htmlFor="editar-nombre">
          <span className="ec-form__label">Nombre de la camada</span>
          <input
            id="editar-nombre"
            className="ec-form__input"
            type="text"
            value={nombreCamada}
            onChange={(event) => setNombreCamada(event.target.value)}
            maxLength={60}
            required
          />
        </label>

        <div className="ec-form__field">
          <span className="ec-form__label">Fecha de ingreso</span>
          <span className="ec-form__valor">
            {camada ? formatearFecha(camada.fecha_ingreso) : '—'}
          </span>
        </div>

        <label className="ec-form__field" htmlFor="editar-cantidad">
          <span className="ec-form__label">Cantidad inicial de aves</span>
          <input
            id="editar-cantidad"
            className="ec-form__input"
            type="number"
            min="1"
            step="1"
            value={cantidadInicial}
            onChange={(event) => setCantidadInicial(event.target.value)}
            disabled={!puedeEditarInicial}
            required={puedeEditarInicial}
          />
          {!puedeEditarInicial && (
            <span className="ec-form__hint ec-form__hint--aviso">
              No se puede cambiar la cantidad inicial: la camada se registró
              hace más de 24 horas.
            </span>
          )}
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

EditarCamadaModal.propTypes = {
  abierto: PropTypes.bool,
  camada: PropTypes.shape({
    id_camada: PropTypes.number.isRequired,
    nombre_camada: PropTypes.string.isRequired,
    fecha_ingreso: PropTypes.string.isRequired,
    cantidad_inicial: PropTypes.number.isRequired,
    puede_editar_inicial: PropTypes.bool,
  }),
  onCerrar: PropTypes.func.isRequired,
  onGuardar: PropTypes.func.isRequired,
};

EditarCamadaModal.defaultProps = {
  abierto: false,
  camada: null,
};

export default EditarCamadaModal;
