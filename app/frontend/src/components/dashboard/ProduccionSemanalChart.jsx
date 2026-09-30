import Chart from 'chart.js/auto';
import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

// Colores del sistema usados en el gráfico.
const AMARILLO = '#FDC33B';
const VERDE = '#7C8A3E';
const MARRON = '#905E27';
const REJILLA = '#e5e7eb';

// En móvil solo caben los últimos días; en escritorio se muestra la semana.
const CONSULTA_MOVIL = '(max-width: 767px)';
const DIAS_MOVIL = 3;

/**
 * Indica si el viewport corresponde a la versión móvil.
 * @returns {boolean} True cuando el ancho es de móvil.
 */
function esViewportMovil() {
  return (
    typeof window !== 'undefined' && window.matchMedia(CONSULTA_MOVIL).matches
  );
}

/**
 * Gráfico de barras con la producción de los últimos días.
 *
 * En móvil recorta la serie a los últimos tres días para que no se salga
 * de la pantalla; en escritorio muestra los siete días completos.
 * @param {Object} props - Propiedades del componente.
 * @param {Array<Object>} props.serie - Días con etiqueta y total de huevos.
 * @returns {JSX.Element} El lienzo con el gráfico de producción.
 */
function ProduccionSemanalChart({ serie }) {
  const canvasRef = useRef(null);
  const [movil, setMovil] = useState(esViewportMovil);

  useEffect(() => {
    const consulta = window.matchMedia(CONSULTA_MOVIL);
    const manejarCambio = (evento) => setMovil(evento.matches);
    consulta.addEventListener('change', manejarCambio);
    return () => consulta.removeEventListener('change', manejarCambio);
  }, []);

  useEffect(() => {
    if (!canvasRef.current) {
      return undefined;
    }
    const visibles = movil ? serie.slice(-DIAS_MOVIL) : serie;
    const ultimo = visibles.length - 1;
    const grafico = new Chart(canvasRef.current, {
      type: 'bar',
      data: {
        labels: visibles.map((dia) => dia.etiqueta),
        datasets: [
          {
            label: 'Huevos recolectados',
            data: visibles.map((dia) => dia.total_huevos),
            backgroundColor: visibles.map((dia, indice) =>
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
  }, [serie, movil]);

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
