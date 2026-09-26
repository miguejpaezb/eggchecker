import PropTypes from 'prop-types';

import { etiquetaFechaCorta } from '../utils/produccion';

/**
 * Columna derecha con los registros recientes y el promedio de 7 días.
 * @param {Object} props - Propiedades del componente.
 * @param {Array<Object>} props.registros - Producciones recientes.
 * @param {number} props.promedio - Promedio diario de los últimos 7 días.
 * @returns {JSX.Element} El panel de historial de producción.
 */
function HistorialProduccion({ registros, promedio }) {
  return (
    <div className="ec-produccion__aside">
      <div className="ec-glass-card ec-produccion__historial">
        <h3 className="ec-produccion__historial-titulo">Registros recientes</h3>
        {registros.length === 0 ? (
          <p className="ec-produccion__historial-vacio">
            Aún no hay registros para esta camada.
          </p>
        ) : (
          <div className="ec-produccion__historial-lista">
            {registros.map((registro) => (
              <div
                className="ec-produccion__historial-fila"
                key={registro.id_produccion}
              >
                <span className="ec-produccion__historial-fecha">
                  {etiquetaFechaCorta(registro.fecha_recoleccion)}
                </span>
                <span className="ec-produccion__historial-total">
                  {registro.total_huevos}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="ec-glass-card ec-produccion__promedio">
        <h3 className="ec-produccion__promedio-titulo">Promedio 7 días</h3>
        <div className="ec-produccion__promedio-valor">{promedio}</div>
        <p className="ec-produccion__promedio-unidad">Huevos / día</p>
      </div>
    </div>
  );
}

HistorialProduccion.propTypes = {
  registros: PropTypes.arrayOf(
    PropTypes.shape({
      id_produccion: PropTypes.number.isRequired,
      fecha_recoleccion: PropTypes.string.isRequired,
      total_huevos: PropTypes.number.isRequired,
    })
  ),
  promedio: PropTypes.number,
};

HistorialProduccion.defaultProps = {
  registros: [],
  promedio: 0,
};

export default HistorialProduccion;
