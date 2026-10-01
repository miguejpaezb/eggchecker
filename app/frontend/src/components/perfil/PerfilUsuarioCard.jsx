import PropTypes from 'prop-types';

import { textoPlan } from '../../utils/perfil';
import Icon from '../Icon';

/**
 * Tarjeta con el avatar y los datos básicos del usuario.
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.perfil - Perfil del usuario autenticado.
 * @returns {JSX.Element} La tarjeta de usuario.
 */
function PerfilUsuarioCard({ perfil }) {
  return (
    <section className="ec-glass-card ec-perfil__card ec-perfil__usuario">
      <div className="ec-perfil__avatar">
        <Icon
          src="/assets/icons/user-icon.svg"
          className="ec-perfil__avatar-icon"
        />
      </div>
      <h2 className="ec-perfil__nombre">
        {perfil?.nombre_completo ?? 'Usuario'}
      </h2>
      <p className="ec-perfil__correo">{perfil?.correo_electronico ?? '—'}</p>
      <span className="ec-perfil__plan">
        {textoPlan(perfil?.plan_suscripcion)}
      </span>
    </section>
  );
}

PerfilUsuarioCard.propTypes = {
  perfil: PropTypes.shape({
    nombre_completo: PropTypes.string,
    correo_electronico: PropTypes.string,
    plan_suscripcion: PropTypes.string,
  }),
};

PerfilUsuarioCard.defaultProps = {
  perfil: null,
};

export default PerfilUsuarioCard;
