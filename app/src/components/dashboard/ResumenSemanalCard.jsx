import PropTypes from 'prop-types';

import { formatoMoneda } from '../../utils/pedido';
import { formatoNumero } from '../../utils/reportes';
import { formatoPorcentaje, textoMejorDia } from '../../utils/dashboard';

/**
 * Tarjeta "Resumen semanal": totales, valor producido, postura y mortalidad.
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.semana - Resumen de los últimos siete días.
 * @returns {JSX.Element} La tarjeta del resumen semanal.
 */
function ResumenSemanalCard({ semana }) {
  return (
    <section className="ec-glass-card ec-dashboard__card">
      <h3 className="ec-dashboard__card-titulo">Resumen semanal</h3>

      <dl className="ec-dashboard__resumen">
        <div className="ec-dashboard__resumen-fila">
          <dt>Total huevos (7d):</dt>
          <dd>{formatoNumero(semana.total_huevos)}</dd>
        </div>
        <div className="ec-dashboard__resumen-fila">
          <dt>Mejor día:</dt>
          <dd className="ec-dashboard__resumen-valor--verde">
            {textoMejorDia(semana.mejor_dia)}
          </dd>
        </div>
        <div className="ec-dashboard__resumen-fila">
          <dt>Valor producido:</dt>
          <dd>{formatoMoneda(semana.valor_producido)}</dd>
        </div>
        <div className="ec-dashboard__resumen-fila">
          <dt>Tasa postura:</dt>
          <dd className="ec-dashboard__resumen-valor--verde">
            {formatoPorcentaje(semana.tasa_postura)}
          </dd>
        </div>
        <div className="ec-dashboard__resumen-fila">
          <dt>Mortalidad:</dt>
          <dd className="ec-dashboard__resumen-valor--rojo">
            {formatoNumero(semana.mortalidad)}{' '}
            {semana.mortalidad === 1 ? 'ave' : 'aves'}
          </dd>
        </div>
      </dl>
    </section>
  );
}

ResumenSemanalCard.propTypes = {
  semana: PropTypes.shape({
    total_huevos: PropTypes.number.isRequired,
    mejor_dia: PropTypes.shape({
      etiqueta: PropTypes.string.isRequired,
      total_huevos: PropTypes.number.isRequired,
    }),
    valor_producido: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
      .isRequired,
    tasa_postura: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
      .isRequired,
    mortalidad: PropTypes.number.isRequired,
  }).isRequired,
};

export default ResumenSemanalCard;
