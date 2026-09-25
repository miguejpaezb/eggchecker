import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import { obtenerCamada } from '../services/camadaService';
import {
  calcularViabilidad,
  etiquetaEstado,
  formatearCantidad,
  formatearFecha,
} from '../utils/camada';
import Modal from './Modal';

/**
 * Modal con los detalles completos de una camada.
 * Consulta el detalle al servidor para obtener edad y retiro estimado.
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si la modal debe mostrarse.
 * @param {Object|null} props.camada - Camada seleccionada en el listado.
 * @param {Function} props.onCerrar - Acción al cerrar la modal.
 * @returns {JSX.Element} La modal con el detalle de la camada.
 */
function CamadaDetalleModal({ abierto, camada, onCerrar }) {
  const [detalle, setDetalle] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!abierto || !camada) {
      return undefined;
    }

    let activo = true;
    setCargando(true);
    setError('');
    setDetalle(null);

    obtenerCamada(camada.id_camada)
      .then((datos) => {
        if (activo) {
          setDetalle(datos);
        }
      })
      .catch((err) => {
        if (activo) {
          setError(err.message);
        }
      })
      .finally(() => {
        if (activo) {
          setCargando(false);
        }
      });

    return () => {
      activo = false;
    };
  }, [abierto, camada]);

  const titulo = camada ? camada.nombre_camada : 'Detalle de la camada';
  const viabilidad = detalle
    ? calcularViabilidad(detalle.cantidad_actual, detalle.cantidad_inicial)
    : 0;

  return (
    <Modal abierto={abierto} titulo={titulo} onCerrar={onCerrar}>
      {cargando && <p className="ec-modal__mensaje">Cargando…</p>}

      {!cargando && error && <p className="ec-form__error">{error}</p>}

      {!cargando && !error && detalle && (
        <>
          <span
            className={`ec-camada-badge ec-camada-badge--${detalle.estado}`}
          >
            {etiquetaEstado(detalle.estado)}
          </span>

          <dl className="ec-detalle">
            <div className="ec-detalle__fila">
              <dt>Fecha de ingreso:</dt>
              <dd>{formatearFecha(detalle.fecha_ingreso)}</dd>
            </div>
            <div className="ec-detalle__fila">
              <dt>Aves iniciales:</dt>
              <dd>{formatearCantidad(detalle.cantidad_inicial)}</dd>
            </div>
            <div className="ec-detalle__fila">
              <dt>Aves actuales:</dt>
              <dd>{formatearCantidad(detalle.cantidad_actual)}</dd>
            </div>
            <div className="ec-detalle__fila">
              <dt>Edad:</dt>
              <dd>
                {detalle.edad_semanas} semanas ({detalle.edad_dias} días)
              </dd>
            </div>
            <div className="ec-detalle__fila">
              <dt>Viabilidad:</dt>
              <dd className="ec-camada-card__viabilidad">
                {viabilidad.toFixed(1)}%
              </dd>
            </div>
            <div className="ec-detalle__fila">
              <dt>Retiro estimado:</dt>
              <dd>{formatearFecha(detalle.fecha_retiro_estimada)}</dd>
            </div>
          </dl>

          {detalle.requiere_decision && (
            <p className="ec-detalle__aviso">
              Esta camada superó las 72 semanas de vida productiva. Decide si
              descartarla o seguir activa.
            </p>
          )}
        </>
      )}
    </Modal>
  );
}

CamadaDetalleModal.propTypes = {
  abierto: PropTypes.bool,
  camada: PropTypes.shape({
    id_camada: PropTypes.number.isRequired,
    nombre_camada: PropTypes.string.isRequired,
  }),
  onCerrar: PropTypes.func.isRequired,
};

CamadaDetalleModal.defaultProps = {
  abierto: false,
  camada: null,
};

export default CamadaDetalleModal;
