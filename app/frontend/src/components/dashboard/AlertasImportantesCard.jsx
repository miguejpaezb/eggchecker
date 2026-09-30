import PropTypes from 'prop-types';
import { useNavigate } from 'react-router-dom';

import { formatoNumero } from '../../utils/reportes';
import Icon from '../Icon';

/**
 * Tarjeta "Alertas importantes": insumos bajo su umbral mínimo.
 * @param {Object} props - Propiedades del componente.
 * @param {Array<Object>} props.alertas - Insumos en alerta.
 * @returns {JSX.Element} La tarjeta de alertas.
 */
function AlertasImportantesCard({ alertas }) {
  const navigate = useNavigate();

  const verDetalle = (idInsumo) =>
    navigate('/inventario', {
      state: { insumoId: idInsumo, abrirStock: true },
    });

  return (
    <section className="ec-glass-card ec-dashboard__card ec-dashboard__card--ancha">
      <header className="ec-dashboard__card-head">
        <Icon
          src="/assets/icons/warning-icon.svg"
          className="ec-dashboard__card-icon ec-dashboard__card-icon--rojo"
        />
        <h3 className="ec-dashboard__card-titulo ec-dashboard__card-titulo--rojo">
          Alertas Importantes ({alertas.length})
        </h3>
      </header>

      {alertas.length === 0 ? (
        <p className="ec-dashboard__vacio">
          No hay insumos bajo el umbral mínimo.
        </p>
      ) : (
        <div className="ec-dashboard__alertas">
          {alertas.map((alerta) => (
            <div key={alerta.id_insumo} className="ec-dashboard__alerta">
              <div className="ec-dashboard__alerta-info">
                <Icon
                  src="/assets/icons/notification_important-icon.svg"
                  className="ec-dashboard__alerta-icon"
                />
                <div>
                  <h4 className="ec-dashboard__alerta-nombre">
                    {alerta.nombre_insumo}
                  </h4>
                  <p className="ec-dashboard__alerta-texto">
                    Quedan {formatoNumero(alerta.stock_actual, 2)} (Mínimo:{' '}
                    {formatoNumero(alerta.umbral_minimo, 2)}) ·{' '}
                    {alerta.categoria}
                  </p>
                </div>
              </div>
              <button
                type="button"
                className="ec-dashboard__alerta-detalle"
                onClick={() => verDetalle(alerta.id_insumo)}
              >
                Detalles
              </button>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

AlertasImportantesCard.propTypes = {
  alertas: PropTypes.arrayOf(
    PropTypes.shape({
      id_insumo: PropTypes.number.isRequired,
      nombre_insumo: PropTypes.string.isRequired,
      categoria: PropTypes.string.isRequired,
      stock_actual: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
        .isRequired,
      umbral_minimo: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
        .isRequired,
    })
  ).isRequired,
};

export default AlertasImportantesCard;
