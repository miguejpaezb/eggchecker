import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import Modal from './Modal';

/** Devuelve una fecha en formato ISO local (YYYY-MM-DD). */
function fechaIsoLocal(fecha) {
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

/** Devuelve la fecha de hoy en formato ISO local (YYYY-MM-DD). */
function fechaHoyLocal() {
  return fechaIsoLocal(new Date());
}

/** Devuelve la fecha de ayer en formato ISO local (YYYY-MM-DD). */
function fechaAyerLocal() {
  const hoy = new Date();
  return fechaIsoLocal(
    new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() - 1)
  );
}

/**
 * Modal con el formulario para registrar una camada nueva.
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si la modal debe mostrarse.
 * @param {Function} props.onCerrar - Acción al cerrar la modal.
 * @param {Function} props.onCrear - Crea la camada; recibe los datos del formulario.
 * @returns {JSX.Element} La modal con el formulario de creación.
 */
function NuevaCamadaModal({ abierto, onCerrar, onCrear }) {
  const [nombreCamada, setNombreCamada] = useState('');
  const [fechaIngreso, setFechaIngreso] = useState(fechaHoyLocal());
  const [cantidadInicial, setCantidadInicial] = useState('');
  const [estado, setEstado] = useState('activa');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (abierto) {
      setNombreCamada('');
      setFechaIngreso(fechaHoyLocal());
      setCantidadInicial('');
      setEstado('activa');
      setError('');
      setEnviando(false);
    }
  }, [abierto]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    const hoy = fechaHoyLocal();
    const ayer = fechaAyerLocal();
    if (fechaIngreso !== hoy && fechaIngreso !== ayer) {
      setError('La fecha de ingreso solo puede ser hoy o ayer');
      return;
    }

    setEnviando(true);

    try {
      await onCrear({
        nombre_camada: nombreCamada.trim(),
        fecha_ingreso: fechaIngreso,
        cantidad_inicial: Number(cantidadInicial),
        estado,
      });
      onCerrar();
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  const ayer = fechaAyerLocal();

  return (
    <Modal abierto={abierto} titulo="Nueva Camada" onCerrar={onCerrar}>
      <form className="ec-form" onSubmit={handleSubmit}>
        {error && <p className="ec-form__error">{error}</p>}

        <label className="ec-form__field" htmlFor="camada-nombre">
          <span className="ec-form__label">Nombre de la camada</span>
          <input
            id="camada-nombre"
            className="ec-form__input"
            type="text"
            value={nombreCamada}
            onChange={(event) => setNombreCamada(event.target.value)}
            placeholder="Ej. Lote A - Galpón 1"
            maxLength={60}
            required
          />
        </label>

        <label className="ec-form__field" htmlFor="camada-fecha">
          <span className="ec-form__label">Fecha de ingreso</span>
          <input
            id="camada-fecha"
            className="ec-form__input"
            type="date"
            value={fechaIngreso}
            min={ayer}
            max={fechaHoyLocal()}
            onChange={(event) => setFechaIngreso(event.target.value)}
            required
          />
          {fechaIngreso === ayer && (
            <span className="ec-form__hint ec-form__hint--aviso">
              Estás registrando la camada con la fecha de ayer. Esta fecha no se
              podrá modificar más adelante.
            </span>
          )}
        </label>

        <label className="ec-form__field" htmlFor="camada-cantidad">
          <span className="ec-form__label">Cantidad inicial de aves</span>
          <input
            id="camada-cantidad"
            className="ec-form__input"
            type="number"
            min="1"
            step="1"
            value={cantidadInicial}
            onChange={(event) => setCantidadInicial(event.target.value)}
            placeholder="Ej. 500"
            required
          />
        </label>

        <label className="ec-form__field" htmlFor="camada-estado">
          <span className="ec-form__label">Estado</span>
          <select
            id="camada-estado"
            className="ec-form__input"
            value={estado}
            onChange={(event) => setEstado(event.target.value)}
          >
            <option value="activa">Activa</option>
            <option value="retirada">Retirada</option>
          </select>
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
            {enviando ? 'Guardando…' : 'Registrar camada'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

NuevaCamadaModal.propTypes = {
  abierto: PropTypes.bool,
  onCerrar: PropTypes.func.isRequired,
  onCrear: PropTypes.func.isRequired,
};

NuevaCamadaModal.defaultProps = {
  abierto: false,
};

export default NuevaCamadaModal;
