import PropTypes from 'prop-types';

/**
 * Gráfico de barras simple construido con CSS, sin dependencias externas.
 * @param {Object} props - Propiedades del gráfico.
 * @param {string[]} props.labels - Etiquetas del eje horizontal.
 * @param {number[]} props.values - Valores de cada barra.
 * @returns {JSX.Element} El gráfico renderizado.
 */
function BarChart({ labels, values }) {
  const maximo = Math.max(...values, 1);

  return (
    <div
      className="ec-chart"
      role="img"
      aria-label="Gráfico de producción semanal"
    >
      {values.map((valor, indice) => (
        <div className="ec-chart__col" key={labels[indice]}>
          <div
            className={`ec-chart__bar${
              indice === values.length - 1 ? ' ec-chart__bar--highlight' : ''
            }`}
            style={{ height: `${(valor / maximo) * 100}%` }}
            title={`${labels[indice]}: ${valor}`}
          />
          <span className="ec-chart__label">{labels[indice]}</span>
        </div>
      ))}
    </div>
  );
}

BarChart.propTypes = {
  labels: PropTypes.arrayOf(PropTypes.string).isRequired,
  values: PropTypes.arrayOf(PropTypes.number).isRequired,
};

export default BarChart;
