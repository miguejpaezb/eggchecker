import PropTypes from 'prop-types';

import { descripcionPlan, textoPlan } from '../../utils/perfil';

/**
 * Sección informativa del plan y la suscripción actual.
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.perfil - Perfil del usuario.
 * @returns {JSX.Element} La tarjeta de suscripción.
 */
function PlanSeccion({ perfil }) {
  const plan = perfil?.plan_suscripcion ?? 'gratuito';

  return (
    <section className="ec-perfil__bloque">
      <h2 className="ec-perfil__seccion-titulo">Mi Suscripción</h2>

      <div className="ec-plan-card">
        <header className="ec-plan-card__head">
          <div>
            <span className="ec-plan-card__etiqueta">Plan Actual</span>
            <h3 className="ec-plan-card__nombre">{textoPlan(plan)}</h3>
          </div>
          <span className="ec-plan-card__badge">Activo</span>
        </header>

        <p className="ec-plan-card__desc">{descripcionPlan(plan)}</p>

        <div className="ec-plan-card__foot">
          <div>
            <span className="ec-plan-card__fact-label">
              Próxima facturación
            </span>
            <p className="ec-plan-card__fact-fecha">No aplica</p>
          </div>
          <button type="button" className="ec-plan-card__btn">
            Mejorar Plan
          </button>
        </div>
      </div>
    </section>
  );
}

PlanSeccion.propTypes = {
  perfil: PropTypes.shape({
    plan_suscripcion: PropTypes.string,
  }),
};

PlanSeccion.defaultProps = {
  perfil: null,
};

export default PlanSeccion;
