import PropTypes from 'prop-types';

import {
  calcularViabilidad,
  etiquetaEstado,
  formatearCantidad,
} from '../utils/camada';
import CamadaAccionesMenu from './CamadaAccionesMenu';

/**
 * Tarjeta resumen de una camada del listado.
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.camada - Camada devuelta por la API.
 * @param {Function} props.onVerDetalles - Acción al pulsar "Ver Detalles".
 * @param {Function} props.onEditar - Abrir la edición de la camada.
 * @param {Function} props.onMortalidad - Abrir el registro de mortalidad.
 * @param {Function} props.onAvanzarSemana - Sumar una semana de vida.
 * @param {Function} props.onSeguirActiva - Posponer la decisión una semana.
 * @param {Function} props.onDescartar - Descartar la camada.
 * @returns {JSX.Element} La tarjeta de la camada.
 */
function CamadaCard({
  camada,
  onVerDetalles,
  onEditar,
  onMortalidad,
  onAvanzarSemana,
  onSeguirActiva,
  onDescartar,
}) {
  const viabilidad = calcularViabilidad(
    camada.cantidad_actual,
    camada.cantidad_inicial
  );

  return (
    <article className="ec-camada-card ec-glass-card">
      <div
        className={`ec-camada-card__bar ec-camada-card__bar--${camada.estado}`}
      />
      <div className="ec-camada-card__body">
        <div className="ec-camada-card__head">
          <h3 className="ec-camada-card__name">{camada.nombre_camada}</h3>
          <div className="ec-camada-card__badges">
            {camada.requiere_decision && (
              <span className="ec-camada-badge ec-camada-badge--aviso">
                Requiere decisión
              </span>
            )}
            <span
              className={`ec-camada-badge ec-camada-badge--${camada.estado}`}
            >
              {etiquetaEstado(camada.estado)}
            </span>
          </div>
        </div>

        <dl className="ec-camada-card__stats">
          <div className="ec-camada-card__stat">
            <dt>Aves actuales:</dt>
            <dd>{formatearCantidad(camada.cantidad_actual)}</dd>
          </div>
          <div className="ec-camada-card__stat">
            <dt>Edad:</dt>
            <dd>
              {camada.edad_semanas}{' '}
              {camada.edad_semanas === 1 ? 'Semana' : 'Semanas'}
            </dd>
          </div>
          <div className="ec-camada-card__stat">
            <dt>Viabilidad:</dt>
            <dd className="ec-camada-card__viabilidad">
              {viabilidad.toFixed(1)}%
            </dd>
          </div>
        </dl>

        <div className="ec-camada-card__footer">
          <button
            type="button"
            className="ec-camada-card__detail"
            onClick={() => onVerDetalles(camada)}
          >
            Ver Detalles
          </button>
          <CamadaAccionesMenu
            camada={camada}
            onEditar={onEditar}
            onMortalidad={onMortalidad}
            onAvanzarSemana={onAvanzarSemana}
            onSeguirActiva={onSeguirActiva}
            onDescartar={onDescartar}
          />
        </div>
      </div>
    </article>
  );
}

CamadaCard.propTypes = {
  camada: PropTypes.shape({
    id_camada: PropTypes.number.isRequired,
    nombre_camada: PropTypes.string.isRequired,
    fecha_ingreso: PropTypes.string.isRequired,
    cantidad_inicial: PropTypes.number.isRequired,
    cantidad_actual: PropTypes.number.isRequired,
    estado: PropTypes.string.isRequired,
    edad_semanas: PropTypes.number.isRequired,
    requiere_decision: PropTypes.bool,
  }).isRequired,
  onVerDetalles: PropTypes.func.isRequired,
  onEditar: PropTypes.func.isRequired,
  onMortalidad: PropTypes.func.isRequired,
  onAvanzarSemana: PropTypes.func.isRequired,
  onSeguirActiva: PropTypes.func.isRequired,
  onDescartar: PropTypes.func.isRequired,
};

export default CamadaCard;
