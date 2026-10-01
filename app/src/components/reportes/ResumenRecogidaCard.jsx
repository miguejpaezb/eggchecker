import PropTypes from 'prop-types';

import { formatoNumero } from '../../utils/reportes';
import Icon from '../Icon';

/**
 * Tarjeta "Resumen de recogida": cubetas totales, por tipo y no aptos.
 * @param {Object} props - Propiedades del componente.
 * @returns {JSX.Element} La tarjeta de resumen de recogida.
 */
function ResumenRecogidaCard({ produccion, camada }) {
  return (
    <section className="ec-glass-card ec-reportes__card">
      <header className="ec-reportes__card-head">
        <Icon
          src="/assets/icons/egg-icon.svg"
          className="ec-reportes__card-icon"
        />
        <h2 className="ec-reportes__card-title">Resumen de Recogida</h2>
      </header>

      {camada && (
        <p className="ec-reportes__camada-nombre">{camada.nombre_camada}</p>
      )}
      <p className="ec-reportes__recogida-texto">Tus gallinas recogieron:</p>

      <div className="ec-reportes__cubetas">
        <span className="ec-reportes__cubetas-numero">
          {produccion.cubetas_completas}
        </span>
        <div className="ec-reportes__cubetas-texto">
          <span className="ec-reportes__cubetas-label">Cubetas</span>
          <span className="ec-reportes__cubetas-unidad">completas</span>
        </div>
      </div>

      <div className="ec-reportes__tipos">
        {produccion.cubetas_por_tipo.map((item) => (
          <div key={item.nombre_tipo} className="ec-reportes__tipo">
            <span className="ec-reportes__tipo-valor">{item.cubetas}</span>
            <span className="ec-reportes__tipo-label">
              Cub. {item.nombre_tipo}
            </span>
          </div>
        ))}
      </div>

      <div className="ec-reportes__noaptos">
        <Icon
          src="/assets/icons/warning-icon.svg"
          className="ec-reportes__noaptos-icon"
        />
        <span>
          Hubo {formatoNumero(produccion.huevos_no_aptos)} huevos rotos o
          dañados
        </span>
      </div>
    </section>
  );
}

ResumenRecogidaCard.propTypes = {
  produccion: PropTypes.shape({
    cubetas_completas: PropTypes.number.isRequired,
    huevos_no_aptos: PropTypes.number.isRequired,
    cubetas_por_tipo: PropTypes.arrayOf(
      PropTypes.shape({
        nombre_tipo: PropTypes.string.isRequired,
        cubetas: PropTypes.number.isRequired,
      })
    ).isRequired,
  }).isRequired,
  camada: PropTypes.shape({
    nombre_camada: PropTypes.string.isRequired,
  }),
};

ResumenRecogidaCard.defaultProps = {
  camada: null,
};

export default ResumenRecogidaCard;
