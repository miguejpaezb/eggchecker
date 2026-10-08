import PropTypes from 'prop-types';

import {
  estiloCalidad,
  etiquetaAnomalia,
  formatearFechaHora,
  porcentaje,
} from '../../utils/analisis';

const SEGMENTOS = [
  { clave: 'AA', texto: 'AA', clase: 'aa' },
  { clave: 'A', texto: 'A', clase: 'a' },
  { clave: 'B', texto: 'B', clase: 'b' },
  { clave: 'No_apto', texto: 'No apto', clase: 'no-apto' },
];

/**
 * Diagnóstico de un análisis (RF-34 y RF-35): calidad, clasificación,
 * anomalías, recomendaciones y retroalimentación. Se usa en el resultado
 * recién obtenido y en el detalle del historial.
 * @param {Object} props - Propiedades del componente.
 * @returns {JSX.Element} El contenido del diagnóstico.
 */
function DiagnosticoContenido({ analisis, onRetroalimentar }) {
  const calidad = estiloCalidad(analisis.calidad_general);
  const { distribucion } = analisis;
  const totalDistribucion = distribucion
    ? SEGMENTOS.reduce((suma, s) => suma + distribucion[s.clave], 0)
    : 0;

  return (
    <div className="ec-analisis__diagnostico">
      {analisis.modo_demo && (
        <p className="ec-analisis__aviso">
          Modo demostración: resultado de ejemplo, no analiza la foto.
        </p>
      )}

      <div className="ec-analisis__encabezado">
        <div>
          <span
            className={`ec-analisis__chip ec-analisis__chip--${calidad.clase}`}
          >
            Calidad {calidad.texto}
          </span>
          {analisis.apto_venta !== null && (
            <p
              className={`ec-analisis__apto ec-analisis__apto--${
                analisis.apto_venta ? 'si' : 'no'
              }`}
            >
              {analisis.apto_venta
                ? 'Apto para la venta'
                : 'No apto para la venta'}
            </p>
          )}
          {analisis.huevos_detectados !== null && (
            <p className="ec-analisis__dato">
              {analisis.huevos_detectados === 1
                ? '1 huevo detectado'
                : `${analisis.huevos_detectados} huevos detectados`}
            </p>
          )}
        </div>
        {analisis.puntaje_calidad !== null && (
          <div className="ec-analisis__puntaje">
            <span
              className={`ec-analisis__puntaje-valor ec-analisis__texto--${calidad.clase}`}
            >
              {analisis.puntaje_calidad}
            </span>
            <span className="ec-analisis__dato">de 100</span>
          </div>
        )}
      </div>

      <p className="ec-analisis__resumen">{analisis.resultado_diagnostico}</p>

      {totalDistribucion > 0 && (
        <>
          <h3 className="ec-analisis__seccion">Clasificación estimada</h3>
          <div
            className="ec-analisis__barra"
            role="img"
            aria-label={SEGMENTOS.map(
              (s) => `${s.texto}: ${distribucion[s.clave]}`
            ).join(', ')}
          >
            {SEGMENTOS.filter((s) => distribucion[s.clave] > 0).map((s) => (
              <span
                key={s.clave}
                className={`ec-analisis__segmento ec-analisis__segmento--${s.clase}`}
                style={{ flexGrow: distribucion[s.clave] }}
              />
            ))}
          </div>
          <ul className="ec-analisis__leyenda">
            {SEGMENTOS.map((s) => (
              <li key={s.clave}>
                <span
                  className={`ec-analisis__punto ec-analisis__segmento--${s.clase}`}
                />
                {s.texto}: {distribucion[s.clave]}
              </li>
            ))}
          </ul>
        </>
      )}

      {analisis.anomalias.length > 0 && (
        <>
          <h3 className="ec-analisis__seccion">Anomalías detectadas</h3>
          <ul className="ec-analisis__anomalias">
            {analisis.anomalias.map((anomalia) => (
              <li
                key={`${anomalia.tipo}-${anomalia.descripcion}`}
                className="ec-analisis__anomalia"
              >
                <div className="ec-analisis__anomalia-head">
                  <strong>{etiquetaAnomalia(anomalia.tipo)}</strong>
                  <span
                    className={`ec-analisis__chip ec-analisis__chip--${anomalia.gravedad}`}
                  >
                    {anomalia.gravedad}
                  </span>
                </div>
                <span className="ec-analisis__dato">
                  {anomalia.descripcion} ({anomalia.huevos_afectados}{' '}
                  {anomalia.huevos_afectados === 1 ? 'huevo' : 'huevos'} ·
                  confianza {porcentaje(anomalia.confianza)})
                </span>
              </li>
            ))}
          </ul>
        </>
      )}

      {analisis.recomendaciones.length > 0 && (
        <>
          <h3 className="ec-analisis__seccion">
            Recomendaciones para tu granja
          </h3>
          <ol className="ec-analisis__recomendaciones">
            {analisis.recomendaciones.map((texto) => (
              <li key={texto}>{texto}</li>
            ))}
          </ol>
        </>
      )}

      <div className="ec-analisis__feedback">
        <p className="ec-analisis__dato">
          {analisis.diagnostico_correcto === true &&
            'Marcaste este diagnóstico como correcto.'}
          {analisis.diagnostico_correcto === false &&
            'Marcaste este diagnóstico como incorrecto.'}
          {analisis.diagnostico_correcto === null &&
            '¿El diagnóstico coincide con lo que ves?'}
        </p>
        <div className="ec-analisis__acciones">
          <button
            type="button"
            className={`ec-btn ${
              analisis.diagnostico_correcto === true
                ? 'ec-btn--primary'
                : 'ec-btn--ghost'
            }`}
            aria-pressed={analisis.diagnostico_correcto === true}
            onClick={() => onRetroalimentar(analisis.id_analisis, true)}
          >
            Sí
          </button>
          <button
            type="button"
            className={`ec-btn ${
              analisis.diagnostico_correcto === false
                ? 'ec-btn--primary'
                : 'ec-btn--ghost'
            }`}
            aria-pressed={analisis.diagnostico_correcto === false}
            onClick={() => onRetroalimentar(analisis.id_analisis, false)}
          >
            No
          </button>
        </div>
      </div>

      <p className="ec-analisis__pie">
        {formatearFechaHora(analisis.fecha_analisis)}
        {analisis.nombre_camada ? ` · ${analisis.nombre_camada}` : ''}
        <br />
        Diagnóstico orientativo generado con IA
        {analisis.confianza_general !== null
          ? ` (confianza ${porcentaje(analisis.confianza_general)})`
          : ''}
        .
      </p>
    </div>
  );
}

export const analisisPropType = PropTypes.shape({
  id_analisis: PropTypes.number.isRequired,
  nombre_camada: PropTypes.string,
  fecha_analisis: PropTypes.string.isRequired,
  tiene_imagen: PropTypes.bool.isRequired,
  resultado_diagnostico: PropTypes.string.isRequired,
  recomendaciones: PropTypes.arrayOf(PropTypes.string).isRequired,
  calidad_general: PropTypes.string,
  puntaje_calidad: PropTypes.number,
  apto_venta: PropTypes.bool,
  huevos_detectados: PropTypes.number,
  anomalias: PropTypes.arrayOf(
    PropTypes.shape({
      tipo: PropTypes.string.isRequired,
      descripcion: PropTypes.string.isRequired,
      gravedad: PropTypes.string.isRequired,
      huevos_afectados: PropTypes.number.isRequired,
      confianza: PropTypes.number.isRequired,
    })
  ).isRequired,
  distribucion: PropTypes.objectOf(PropTypes.number),
  confianza_general: PropTypes.number,
  modo_demo: PropTypes.bool.isRequired,
  diagnostico_correcto: PropTypes.bool,
});

DiagnosticoContenido.propTypes = {
  analisis: analisisPropType.isRequired,
  onRetroalimentar: PropTypes.func.isRequired,
};

export default DiagnosticoContenido;
