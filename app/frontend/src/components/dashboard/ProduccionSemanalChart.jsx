import Chart from 'chart.js/auto';
import PropTypes from 'prop-types';
import { useEffect, useRef } from 'react';

// Colores del sistema usados en el gráfico.
const AMARILLO = '#FDC33B';
const VERDE = '#7C8A3E';
const MARRON = '#905E27';
const REJILLA = '#e5e7eb';

/**
 * Gráfico de barras con la producción de los últimos siete días.
 * @param {Object} props - Propiedades del componente.
 * @param {Array<Object>} props.serie - Días con etiqueta y total de huevos.
 * @returns {JSX.Element} El lienzo con el gráfico de producción.
 */
function ProduccionSemanalChart({ serie }) {
  const canvasRef = useRef(null);

  useEffect(() => {
    if (!canvasRef.current) {
      return undefined;
    }
    const ultimo = serie.length - 1;
    const grafico = new Chart(canvasRef.current, {
      type: 'bar',
      data: {
        labels: serie.map((dia) => dia.etiqueta),
        datasets: [
          {
            label: 'Huevos recolectados',
            data: serie.map((dia) => dia.total_huevos),
            backgroundColor: serie.map((dia, indice) =>
              indice === ultimo ? VERDE : AMARILLO
            ),
            borderRadius: 4,
            borderSkipped: false,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: { backgroundColor: MARRON },
        },
        scales: {
          y: {
            beginAtZero: true,
            grid: { color: REJILLA },
            border: { display: false },
          },
          x: {
            grid: { display: false },
            border: { display: false },
          },
        },
      },
    });
    return () => grafico.destroy();
  }, [serie]);

  return (
    <div className="ec-dashboard__chart">
      <canvas ref={canvasRef} />
    </div>
  );
}

ProduccionSemanalChart.propTypes = {
  serie: PropTypes.arrayOf(
    PropTypes.shape({
      etiqueta: PropTypes.string.isRequired,
      total_huevos: PropTypes.number.isRequired,
    })
  ).isRequired,
};

export default ProduccionSemanalChart;
