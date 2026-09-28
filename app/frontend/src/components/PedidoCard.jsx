import PropTypes from 'prop-types';

import {
  etiquetaEstadoPedido,
  formatearFechaPedido,
  formatoMoneda,
  resumenUnidades,
} from '../utils/pedido';

/**
 * Tarjeta resumen de un pedido del listado.
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.pedido - Pedido devuelto por la API.
 * @param {Function} props.onVerDetalles - Acción al pulsar "Ver Detalles".
 * @param {React.ReactNode} props.children - Menú de acciones del pedido.
 * @returns {JSX.Element} La tarjeta del pedido.
 */
function PedidoCard({ pedido, onVerDetalles, children }) {
  return (
    <article className="ec-pedido-card ec-glass-card">
      <div className="ec-pedido-card__head">
        <div className="ec-pedido-card__info">
          <h3 className="ec-pedido-card__name">{pedido.cliente_nombre}</h3>
          <p className="ec-pedido-card__direccion">
            {pedido.cliente_direccion || 'Sin dirección'}
          </p>
        </div>
        <span
          className={`ec-pedido-badge ec-pedido-badge--${pedido.estado_pedido}`}
        >
          {etiquetaEstadoPedido(pedido.estado_pedido)}
        </span>
      </div>

      <div className="ec-pedido-card__resumen">
        <span>{resumenUnidades(pedido)}</span>
        <span className="ec-pedido-card__total">
          {formatoMoneda(pedido.valor_total)}
        </span>
      </div>

      <div className="ec-pedido-card__footer">
        <span className="ec-pedido-card__fecha">
          {formatearFechaPedido(pedido.fecha_pedido)}
        </span>
        <div className="ec-pedido-card__acciones">
          <button
            type="button"
            className="ec-pedido-card__detail"
            onClick={() => onVerDetalles(pedido)}
          >
            Ver Detalles
          </button>
          {children}
        </div>
      </div>
    </article>
  );
}

PedidoCard.propTypes = {
  pedido: PropTypes.shape({
    cliente_nombre: PropTypes.string.isRequired,
    cliente_direccion: PropTypes.string,
    estado_pedido: PropTypes.string.isRequired,
    valor_total: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
      .isRequired,
    unidades_totales: PropTypes.number,
    detalles: PropTypes.arrayOf(
      PropTypes.shape({
        id_tipo: PropTypes.number,
        nombre_tipo: PropTypes.string,
      })
    ),
    fecha_pedido: PropTypes.string.isRequired,
  }).isRequired,
  onVerDetalles: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
};

export default PedidoCard;
