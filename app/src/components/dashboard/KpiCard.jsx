import PropTypes from 'prop-types';

import Icon from '../Icon';

/**
 * Tarjeta de indicador (KPI) del dashboard.
 * @param {Object} props - Propiedades del componente.
 * @param {string} props.titulo - Título del indicador.
 * @param {string|number} props.valor - Valor principal.
 * @param {string} [props.detalle] - Texto secundario bajo el valor.
 * @param {string} props.icono - Ruta pública del SVG.
 * @param {string} [props.tono] - Color de acento: yellow, brown, green o red.
 * @returns {JSX.Element} La tarjeta del indicador.
 */
function KpiCard({ titulo, valor, detalle, icono, tono }) {
  return (
    <div
      className={`ec-glass-card ec-dashboard__kpi ec-dashboard__kpi--${tono}`}
    >
      <div className="ec-dashboard__kpi-info">
        <p className="ec-dashboard__kpi-titulo">{titulo}</p>
        <h3 className="ec-dashboard__kpi-valor">{valor}</h3>
        {detalle && <p className="ec-dashboard__kpi-detalle">{detalle}</p>}
      </div>
      <div className="ec-dashboard__kpi-icono">
        <Icon src={icono} className="ec-dashboard__kpi-icono-svg" />
      </div>
    </div>
  );
}

KpiCard.propTypes = {
  titulo: PropTypes.string.isRequired,
  valor: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  detalle: PropTypes.string,
  icono: PropTypes.string.isRequired,
  tono: PropTypes.oneOf(['yellow', 'brown', 'green', 'red']),
};

KpiCard.defaultProps = {
  detalle: '',
  tono: 'yellow',
};

export default KpiCard;
