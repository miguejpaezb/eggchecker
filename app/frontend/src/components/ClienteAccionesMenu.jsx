import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

import Icon from './Icon';

/**
 * Menú desplegable de acciones de un cliente, anclado al botón more_vert.
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.cliente - Cliente sobre el que se actúa.
 * @param {Function} props.onRegistrarVenta - Ir al área de ventas con el cliente.
 * @param {Function} props.onEditar - Abrir la edición del cliente.
 * @param {Function} props.onSuspender - Suspender el cliente.
 * @param {Function} props.onActivar - Reactivar el cliente.
 * @param {Function} props.onEliminar - Eliminar el cliente.
 * @returns {JSX.Element} El menú de acciones.
 */
function ClienteAccionesMenu({
  cliente,
  onRegistrarVenta,
  onEditar,
  onSuspender,
  onActivar,
  onEliminar,
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
        className="ec-cliente-card__more"
        aria-haspopup="true"
        aria-expanded={abierto}
        aria-label="Acciones del cliente"
        title="Acciones del cliente"
        onClick={() => setAbierto((valor) => !valor)}
      >
        <Icon
          src="/assets/icons/more_vert-icon.svg"
          className="ec-cliente-card__more-icon"
        />
      </button>

      {abierto && (
        <div className="ec-menu__lista" role="menu">
          {cliente.activo && (
            <button
              type="button"
              role="menuitem"
              className="ec-menu__item"
              onClick={() => ejecutar(() => onRegistrarVenta(cliente))}
            >
              Registrar venta
            </button>
          )}
          <button
            type="button"
            role="menuitem"
            className="ec-menu__item"
            onClick={() => ejecutar(() => onEditar(cliente))}
          >
            Editar
          </button>
          {cliente.activo ? (
            <button
              type="button"
              role="menuitem"
              className="ec-menu__item"
              onClick={() => ejecutar(() => onSuspender(cliente))}
            >
              Suspender
            </button>
          ) : (
            <button
              type="button"
              role="menuitem"
              className="ec-menu__item"
              onClick={() => ejecutar(() => onActivar(cliente))}
            >
              Activar
            </button>
          )}
          <button
            type="button"
            role="menuitem"
            className="ec-menu__item ec-menu__item--danger"
            onClick={() => ejecutar(() => onEliminar(cliente))}
          >
            Eliminar
          </button>
        </div>
      )}
    </div>
  );
}

ClienteAccionesMenu.propTypes = {
  cliente: PropTypes.shape({
    activo: PropTypes.bool.isRequired,
  }).isRequired,
  onRegistrarVenta: PropTypes.func.isRequired,
  onEditar: PropTypes.func.isRequired,
  onSuspender: PropTypes.func.isRequired,
  onActivar: PropTypes.func.isRequired,
  onEliminar: PropTypes.func.isRequired,
};

export default ClienteAccionesMenu;
