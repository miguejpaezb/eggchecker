import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

import Icon from './Icon';

/**
 * Menú desplegable de acciones de un pedido, anclado al botón more_vert.
 * Las opciones dependen del estado del pedido.
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.pedido - Pedido sobre el que se actúa.
 * @param {Function} props.onMarcarEnviado - Pasar el pedido a "en camino".
 * @param {Function} props.onMarcarRecibido - Marcar el pedido como recibido.
 * @param {Function} props.onEditar - Abrir la edición del pedido.
 * @param {Function} props.onCancelar - Cancelar el pedido.
 * @param {Function} props.onEliminar - Eliminar el pedido.
 * @returns {JSX.Element} El menú de acciones.
 */
function PedidoAccionesMenu({
  pedido,
  onMarcarEnviado,
  onMarcarRecibido,
  onEditar,
  onCancelar,
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

  const esPendiente = pedido.estado_pedido === 'pendiente';
  const esEnviado = pedido.estado_pedido === 'enviado';
  const operable = esPendiente || esEnviado;

  return (
    <div className="ec-menu" ref={ref}>
      <button
        type="button"
        className="ec-pedido-card__more"
        aria-haspopup="true"
        aria-expanded={abierto}
        aria-label="Acciones del pedido"
        title="Acciones del pedido"
        onClick={() => setAbierto((valor) => !valor)}
      >
        <Icon
          src="/assets/icons/more_vert-icon.svg"
          className="ec-pedido-card__more-icon"
        />
      </button>

      {abierto && (
        <div className="ec-menu__lista" role="menu">
          {!operable && (
            <span className="ec-menu__item ec-menu__item--disabled">
              {pedido.estado_pedido === 'recibido'
                ? 'Pedido recibido'
                : 'Pedido cancelado'}
            </span>
          )}

          {esPendiente && (
            <button
              type="button"
              role="menuitem"
              className="ec-menu__item"
              onClick={() => ejecutar(() => onMarcarEnviado(pedido))}
            >
              Marcar en camino
            </button>
          )}

          {operable && (
            <button
              type="button"
              role="menuitem"
              className="ec-menu__item"
              onClick={() => ejecutar(() => onMarcarRecibido(pedido))}
            >
              Marcar recibido
            </button>
          )}

          {esPendiente && (
            <button
              type="button"
              role="menuitem"
              className="ec-menu__item"
              onClick={() => ejecutar(() => onEditar(pedido))}
            >
              Editar
            </button>
          )}

          {operable && (
            <button
              type="button"
              role="menuitem"
              className="ec-menu__item"
              onClick={() => ejecutar(() => onCancelar(pedido))}
            >
              Cancelar
            </button>
          )}

          {operable && (
            <button
              type="button"
              role="menuitem"
              className="ec-menu__item ec-menu__item--danger"
              onClick={() => ejecutar(() => onEliminar(pedido))}
            >
              Eliminar
            </button>
          )}
        </div>
      )}
    </div>
  );
}

PedidoAccionesMenu.propTypes = {
  pedido: PropTypes.shape({
    estado_pedido: PropTypes.string.isRequired,
  }).isRequired,
  onMarcarEnviado: PropTypes.func.isRequired,
  onMarcarRecibido: PropTypes.func.isRequired,
  onEditar: PropTypes.func.isRequired,
  onCancelar: PropTypes.func.isRequired,
  onEliminar: PropTypes.func.isRequired,
};

export default PedidoAccionesMenu;
