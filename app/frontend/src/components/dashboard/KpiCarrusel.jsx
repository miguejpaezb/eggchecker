import PropTypes from 'prop-types';
import { useCallback, useRef, useState } from 'react';

/**
 * Carrusel horizontal de tarjetas KPI para móvil.
 *
 * En pantallas pequeñas las tarjetas se deslizan de izquierda a derecha y
 * unos puntos indican la posición actual; en escritorio se muestra la
 * cuadrícula y los puntos se ocultan por CSS.
 * @param {Object} props - Propiedades del componente.
 * @param {React.ReactNode} props.children - Tarjetas KPI a mostrar.
 * @param {Array<string>} props.etiquetas - Nombre de cada tarjeta (puntos).
 * @returns {JSX.Element} El carrusel de indicadores.
 */
function KpiCarrusel({ children, etiquetas }) {
  const contenedorRef = useRef(null);
  const [activo, setActivo] = useState(0);

  const manejarScroll = useCallback(() => {
    const contenedor = contenedorRef.current;
    if (!contenedor) {
      return;
    }
    const centro =
      contenedor.getBoundingClientRect().left + contenedor.clientWidth / 2;
    let masCercano = 0;
    let menorDistancia = Infinity;
    Array.from(contenedor.children).forEach((hijo, indice) => {
      const rect = hijo.getBoundingClientRect();
      const distancia = Math.abs(rect.left + rect.width / 2 - centro);
      if (distancia < menorDistancia) {
        menorDistancia = distancia;
        masCercano = indice;
      }
    });
    setActivo(masCercano);
  }, []);

  const irA = useCallback((indice) => {
    const contenedor = contenedorRef.current;
    const hijo = contenedor?.children[indice];
    if (!contenedor || !hijo) {
      return;
    }
    const rectContenedor = contenedor.getBoundingClientRect();
    const rectHijo = hijo.getBoundingClientRect();
    const centrado = (contenedor.clientWidth - rectHijo.width) / 2;
    contenedor.scrollTo({
      left:
        contenedor.scrollLeft +
        (rectHijo.left - rectContenedor.left) -
        centrado,
      behavior: 'smooth',
    });
  }, []);

  return (
    <div className="ec-dashboard__kpis-bloque">
      <div
        className="ec-dashboard__kpis"
        ref={contenedorRef}
        onScroll={manejarScroll}
      >
        {children}
      </div>

      <div className="ec-dashboard__kpis-dots">
        {etiquetas.map((etiqueta, indice) => (
          <button
            key={etiqueta}
            type="button"
            className={`ec-dashboard__kpis-dot${
              indice === activo ? ' ec-dashboard__kpis-dot--activo' : ''
            }`}
            aria-label={`Ir al indicador ${etiqueta}`}
            aria-current={indice === activo ? 'true' : undefined}
            onClick={() => irA(indice)}
          />
        ))}
      </div>
    </div>
  );
}

KpiCarrusel.propTypes = {
  children: PropTypes.node.isRequired,
  etiquetas: PropTypes.arrayOf(PropTypes.string).isRequired,
};

export default KpiCarrusel;
