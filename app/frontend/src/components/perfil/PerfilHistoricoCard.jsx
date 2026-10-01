import PropTypes from 'prop-types';

import { formatearMesAnio } from '../../utils/perfil';
import { formatoNumero } from '../../utils/reportes';
import Icon from '../Icon';

/**
 * Tarjeta con el histórico de la cuenta: huevos, aves y antigüedad.
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.perfil - Perfil del usuario autenticado.
 * @returns {JSX.Element} La tarjeta de histórico.
 */
function PerfilHistoricoCard({ perfil }) {
  return (
    <section className="ec-glass-card ec-perfil__card">
      <h3 className="ec-perfil__card-titulo">
        <Icon
          src="/assets/icons/icon_trending_up.svg"
          className="ec-perfil__card-icon"
        />
        Histórico
      </h3>
      <dl className="ec-perfil__stats">
        <div className="ec-perfil__stat">
          <dt className="ec-perfil__stat-label">Total huevos producidos</dt>
          <dd className="ec-perfil__stat-valor">
            {formatoNumero(perfil?.total_huevos_producidos)}
          </dd>
        </div>
        <div className="ec-perfil__stat">
          <dt className="ec-perfil__stat-label">Total aves gestionadas</dt>
          <dd className="ec-perfil__stat-valor">
            {formatoNumero(perfil?.total_aves_gestionadas)}
          </dd>
        </div>
        <div className="ec-perfil__stat">
          <dt className="ec-perfil__stat-label">Miembro desde</dt>
          <dd className="ec-perfil__stat-valor">
            {formatearMesAnio(perfil?.fecha_registro)}
          </dd>
        </div>
      </dl>
    </section>
  );
}

PerfilHistoricoCard.propTypes = {
  perfil: PropTypes.shape({
    total_huevos_producidos: PropTypes.number,
    total_aves_gestionadas: PropTypes.number,
    fecha_registro: PropTypes.string,
  }),
};

PerfilHistoricoCard.defaultProps = {
  perfil: null,
};

export default PerfilHistoricoCard;
