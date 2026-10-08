import PropTypes from 'prop-types';

import { estiloCalidad, formatearFechaHora } from '../../utils/analisis';
import { analisisPropType } from './DiagnosticoContenido';

/**
 * Historial de análisis con filtro por camada (RF-36).
 * @param {Object} props - Propiedades del componente.
 * @returns {JSX.Element} La sección de historial.
 */
function HistorialAnalisis({
  historial,
  camadas,
  filtro,
  cargando,
  error,
  onFiltro,
  onAbrir,
}) {
  return (
    <section className="ec-analisis__historial">
      <div className="ec-analisis__historial-head">
        <h2 className="ec-reportes__card-title">Historial</h2>
        <label className="ec-filtro" htmlFor="filtro-historial">
          <span className="ec-filtro__label">Ver</span>
          <select
            id="filtro-historial"
            className="ec-filtro__select"
            value={filtro}
            onChange={(event) => onFiltro(event.target.value)}
          >
            <option value="">Toda la granja</option>
            {camadas.map((camada) => (
              <option key={camada.id_camada} value={camada.id_camada}>
                {camada.nombre_camada}
              </option>
            ))}
          </select>
        </label>
      </div>

      {cargando && historial.length === 0 && (
        <p className="ec-camadas__estado">Cargando historial…</p>
      )}
      {error && (
        <div className="ec-glass-card ec-camadas__estado ec-camadas__estado--error">
          {error}
        </div>
      )}
      {!cargando && !error && historial.length === 0 && (
        <div className="ec-glass-card ec-camadas__estado">
          {filtro
            ? 'Esta camada aún no tiene análisis.'
            : 'Aún no tienes análisis. Toma tu primera foto.'}
        </div>
      )}

      {!error && historial.length > 0 && (
        <ul className="ec-analisis__lista">
          {historial.map((analisis) => {
            const calidad = estiloCalidad(analisis.calidad_general);
            return (
              <li key={analisis.id_analisis}>
                <button
                  type="button"
                  className="ec-glass-card ec-analisis__item"
                  onClick={() => onAbrir(analisis)}
                >
                  <span className="ec-analisis__item-head">
                    <span>
                      <strong>
                        {formatearFechaHora(analisis.fecha_analisis)}
                      </strong>
                      <span className="ec-analisis__dato">
                        {analisis.nombre_camada || 'Sin camada'}
                      </span>
                    </span>
                    {analisis.puntaje_calidad !== null && (
                      <span
                        className={`ec-analisis__item-puntaje ec-analisis__texto--${calidad.clase}`}
                      >
                        {analisis.puntaje_calidad}/100
                      </span>
                    )}
                    <span
                      className={`ec-analisis__chip ec-analisis__chip--${calidad.clase}`}
                    >
                      {calidad.texto}
                    </span>
                  </span>
                  <span className="ec-analisis__item-resumen">
                    {analisis.resultado_diagnostico}
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}

HistorialAnalisis.propTypes = {
  historial: PropTypes.arrayOf(analisisPropType).isRequired,
  camadas: PropTypes.arrayOf(
    PropTypes.shape({
      id_camada: PropTypes.number.isRequired,
      nombre_camada: PropTypes.string.isRequired,
    })
  ).isRequired,
  filtro: PropTypes.string.isRequired,
  cargando: PropTypes.bool.isRequired,
  error: PropTypes.string,
  onFiltro: PropTypes.func.isRequired,
  onAbrir: PropTypes.func.isRequired,
};

HistorialAnalisis.defaultProps = {
  error: '',
};

export default HistorialAnalisis;
