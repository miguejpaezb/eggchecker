import PropTypes from 'prop-types';

import { formatoNumero } from '../../utils/reportes';
import Icon from '../Icon';

// Mínimo de días para mostrar la nota de promedio diario de comida.
const DIAS_MINIMOS_PROMEDIO = 4;

/**
 * Tarjeta "Uso de comida": kg usados y su promedio diario.
 * @param {Object} props - Propiedades del componente.
 * @returns {JSX.Element} La tarjeta de uso de comida.
 */
function UsoComidaCard({ kgUsados, promedioDiario, dias }) {
  const mostrarPromedio = dias >= DIAS_MINIMOS_PROMEDIO;

  return (
    <section className="ec-glass-card ec-reportes__card">
      <header className="ec-reportes__card-head">
        <Icon
          src="/assets/icons/icon_food.svg"
          className="ec-reportes__card-icon"
        />
        <h2 className="ec-reportes__card-title">Uso de Comida</h2>
      </header>

      <p className="ec-reportes__comida-texto">
        Se gastaron <strong>{formatoNumero(kgUsados, 2)}</strong> kg de
        concentrado
      </p>

      {mostrarPromedio && (
        <div className="ec-reportes__nota">
          <Icon
            src="/assets/icons/info-icon.svg"
            className="ec-reportes__nota-icon"
          />
          <span>
            Promedio de {formatoNumero(promedioDiario, 2)} kg por día en el
            período.
          </span>
        </div>
      )}
    </section>
  );
}

UsoComidaCard.propTypes = {
  kgUsados: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
    .isRequired,
  promedioDiario: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
    .isRequired,
  dias: PropTypes.number.isRequired,
};

export default UsoComidaCard;
