import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

import Icon from './Icon';

/**
 * Menú desplegable de acciones de un insumo, anclado al botón more_vert.
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.insumo - Insumo sobre el que se actúa.
 * @param {Function} props.onEditar - Abrir la edición del insumo.
 * @param {Function} props.onAnadirStock - Abrir el alta de stock.
 * @param {Function} props.onSuspender - Suspender el insumo.
 * @param {Function} props.onActivar - Reactivar el insumo.
 * @param {Function} props.onDescontinuar - Descontinuar el insumo.
 * @returns {JSX.Element} El menú de acciones.
 */
function InsumoAccionesMenu({
  insumo,
  onEditar,
  onAnadirStock,
  onSuspender,
  onActivar,
  onDescontinuar,
}) {
  const [abierto, setAbierto] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    if (!abierto) {
      return undefined;
    }

    const cerrarSiFuera = (event) => {
      if (ref.current && !ref.current.contains(event.target)) {
        setAbierto(false);
      }
    };
    const cerrarConEscape = (event) => {
      if (event.key === 'Escape') {
        setAbierto(false);
      }
    };

    document.addEventListener('mousedown', cerrarSiFuera);
    document.addEventListener('touchstart', cerrarSiFuera);
    document.addEventListener('keydown', cerrarConEscape);
    return () => {
      document.removeEventListener('mousedown', cerrarSiFuera);
      document.removeEventListener('touchstart', cerrarSiFuera);
      document.removeEventListener('keydown', cerrarConEscape);
    };
  }, [abierto]);

  const ejecutar = (accion) => {
    setAbierto(false);
    accion();
  };

  return (
    <div className="ec-menu" ref={ref}>
      <button
        type="button"
        className="ec-insumo-card__more"
        aria-haspopup="true"
        aria-expanded={abierto}
        aria-label="Acciones del insumo"
        title="Acciones del insumo"
        onClick={() => setAbierto((valor) => !valor)}
      >
        <Icon
          src="/assets/icons/more_vert-icon.svg"
          className="ec-insumo-card__more-icon"
        />
      </button>

      {abierto && (
        <div className="ec-menu__lista" role="menu">
          {insumo.activo ? (
            <>
              <button
                type="button"
                role="menuitem"
                className="ec-menu__item"
                onClick={() => ejecutar(() => onEditar(insumo))}
              >
                Editar
              </button>
              <button
                type="button"
                role="menuitem"
                className="ec-menu__item"
                onClick={() => ejecutar(() => onAnadirStock(insumo))}
              >
                Añadir stock
              </button>
              <button
                type="button"
                role="menuitem"
                className="ec-menu__item"
                onClick={() => ejecutar(() => onSuspender(insumo))}
              >
                Suspender
              </button>
              <button
                type="button"
                role="menuitem"
                className="ec-menu__item ec-menu__item--danger"
                onClick={() => ejecutar(() => onDescontinuar(insumo))}
              >
                Descontinuar
              </button>
            </>
          ) : (
            <button
              type="button"
              role="menuitem"
              className="ec-menu__item"
              onClick={() => ejecutar(() => onActivar(insumo))}
            >
              Activar
            </button>
          )}
        </div>
      )}
    </div>
  );
}

InsumoAccionesMenu.propTypes = {
  insumo: PropTypes.shape({
    activo: PropTypes.bool.isRequired,
  }).isRequired,
  onEditar: PropTypes.func.isRequired,
  onAnadirStock: PropTypes.func.isRequired,
  onSuspender: PropTypes.func.isRequired,
  onActivar: PropTypes.func.isRequired,
  onDescontinuar: PropTypes.func.isRequired,
};

export default InsumoAccionesMenu;
