import PropTypes from 'prop-types';

import Icon from '../Icon';

/**
 * Tarjeta "Salud y bajas": mortalidad, causa principal y vacunación.
 * @param {Object} props - Propiedades del componente.
 * @returns {JSX.Element} La tarjeta de salud y bajas.
 */
function SaludBajasCard({ gallinasPerdidas, causaPrincipal, vacunacionAlDia }) {
  return (
    <section className="ec-glass-card ec-reportes__card">
      <header className="ec-reportes__card-head">
        <Icon
          src="/assets/icons/icon_health.svg"
          className="ec-reportes__card-icon"
        />
        <h2 className="ec-reportes__card-title">Salud y Bajas</h2>
      </header>

      <div className="ec-reportes__salud">
        <div className="ec-reportes__salud-fila">
          <span className="ec-reportes__salud-label">Gallinas Perdidas</span>
          <span className="ec-reportes__salud-numero">{gallinasPerdidas}</span>
        </div>
        <p className="ec-reportes__salud-causa">
          Causa principal: {causaPrincipal || 'Sin registro'}
        </p>
      </div>

      <div className="ec-reportes__vacunas">
        <Icon
          src="/assets/icons/icon_shield.svg"
          className="ec-reportes__vacunas-icon"
        />
        <span>
          {vacunacionAlDia
            ? 'Vacunas y vitaminas al día'
            : 'Vacunas sin registro en el período'}
        </span>
      </div>
    </section>
  );
}

SaludBajasCard.propTypes = {
  gallinasPerdidas: PropTypes.number.isRequired,
  causaPrincipal: PropTypes.string,
  vacunacionAlDia: PropTypes.bool.isRequired,
};

SaludBajasCard.defaultProps = {
  causaPrincipal: null,
};

export default SaludBajasCard;
