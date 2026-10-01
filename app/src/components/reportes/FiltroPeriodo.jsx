import PropTypes from 'prop-types';

import { PRESETS } from '../../utils/reportes';
import Icon from '../Icon';

/**
 * Contenedor de filtros del reporte: camada, período y descarga de PDF.
 * @param {Object} props - Propiedades del componente.
 * @returns {JSX.Element} El contenedor de filtros.
 */
function FiltroPeriodo({
  preset,
  desde,
  hasta,
  camada,
  camadas,
  descargando,
  onPreset,
  onDesde,
  onHasta,
  onCamada,
  onDescargar,
}) {
  return (
    <div className="ec-glass-card ec-reportes__filtro">
      <div className="ec-reportes__filtro-campos">
        <label className="ec-reportes__campo" htmlFor="reportes-camada">
          <span className="ec-reportes__campo-label">Reporte</span>
          <select
            id="reportes-camada"
            className="ec-reportes__select"
            value={camada}
            onChange={(event) => onCamada(event.target.value)}
          >
            <option value="">Toda la granja</option>
            {camadas.map((item) => (
              <option key={item.id_camada} value={item.id_camada}>
                {item.nombre_camada}
              </option>
            ))}
          </select>
        </label>

        <label className="ec-reportes__campo" htmlFor="reportes-preset">
          <span className="ec-reportes__campo-label">Período</span>
          <select
            id="reportes-preset"
            className="ec-reportes__select"
            value={preset}
            onChange={(event) => onPreset(event.target.value)}
          >
            {PRESETS.map((item) => (
              <option key={item.valor} value={item.valor}>
                {item.label}
              </option>
            ))}
            {preset === 'personalizado' && (
              <option value="personalizado">Personalizado</option>
            )}
          </select>
        </label>

        <label className="ec-reportes__campo" htmlFor="reportes-desde">
          <span className="ec-reportes__campo-label">Desde</span>
          <input
            id="reportes-desde"
            className="ec-reportes__fecha"
            type="date"
            value={desde}
            max={hasta}
            onChange={(event) => onDesde(event.target.value)}
          />
        </label>

        <label className="ec-reportes__campo" htmlFor="reportes-hasta">
          <span className="ec-reportes__campo-label">Hasta</span>
          <input
            id="reportes-hasta"
            className="ec-reportes__fecha"
            type="date"
            value={hasta}
            min={desde}
            onChange={(event) => onHasta(event.target.value)}
          />
        </label>
      </div>

      <button
        type="button"
        className="ec-btn ec-btn--primary ec-reportes__pdf"
        onClick={onDescargar}
        disabled={descargando}
      >
        <Icon src="/assets/icons/reports-icon.svg" className="ec-btn__icon" />
        {descargando ? 'Generando…' : 'Descargar PDF'}
      </button>
    </div>
  );
}

FiltroPeriodo.propTypes = {
  preset: PropTypes.string.isRequired,
  desde: PropTypes.string.isRequired,
  hasta: PropTypes.string.isRequired,
  camada: PropTypes.string.isRequired,
  camadas: PropTypes.arrayOf(
    PropTypes.shape({
      id_camada: PropTypes.number.isRequired,
      nombre_camada: PropTypes.string.isRequired,
    })
  ).isRequired,
  descargando: PropTypes.bool.isRequired,
  onPreset: PropTypes.func.isRequired,
  onDesde: PropTypes.func.isRequired,
  onHasta: PropTypes.func.isRequired,
  onCamada: PropTypes.func.isRequired,
  onDescargar: PropTypes.func.isRequired,
};

export default FiltroPeriodo;
