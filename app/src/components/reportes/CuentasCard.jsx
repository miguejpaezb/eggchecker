import PropTypes from 'prop-types';

import { formatoMoneda } from '../../utils/pedido';
import Icon from '../Icon';

/**
 * Tarjeta "Cuentas del período": ventas, gastos y ganancia.
 * @param {Object} props - Propiedades del componente.
 * @returns {JSX.Element} La tarjeta de cuentas.
 */
function CuentasCard({ ventas, gastos, ganancia }) {
  return (
    <section className="ec-glass-card ec-reportes__card">
      <header className="ec-reportes__card-head">
        <Icon
          src="/assets/icons/reports-icon.svg"
          className="ec-reportes__card-icon"
        />
        <h2 className="ec-reportes__card-title">Cuentas del período</h2>
      </header>

      <div className="ec-reportes__cuentas">
        <div className="ec-reportes__cuenta ec-reportes__cuenta--ventas">
          <div className="ec-reportes__cuenta-head">
            <Icon
              src="/assets/icons/icon_trending_up.svg"
              className="ec-reportes__cuenta-icon"
            />
            <span className="ec-reportes__cuenta-label">Ventas</span>
          </div>
          <span className="ec-reportes__cuenta-valor">
            {formatoMoneda(ventas)}
          </span>
        </div>

        <div className="ec-reportes__cuenta ec-reportes__cuenta--gastos">
          <div className="ec-reportes__cuenta-head">
            <Icon
              src="/assets/icons/icon_trending_down.svg"
              className="ec-reportes__cuenta-icon"
            />
            <span className="ec-reportes__cuenta-label">Gastos</span>
          </div>
          <span className="ec-reportes__cuenta-valor">
            {formatoMoneda(gastos)}
          </span>
        </div>
      </div>

      <div className="ec-reportes__ganancia">
        <span className="ec-reportes__ganancia-label">Ganancias</span>
        <span className="ec-reportes__ganancia-valor">
          {formatoMoneda(ganancia)}
        </span>
      </div>
    </section>
  );
}

CuentasCard.propTypes = {
  ventas: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  gastos: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  ganancia: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
    .isRequired,
};

export default CuentasCard;
