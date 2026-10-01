import PropTypes from 'prop-types';

import {
  etiquetaEstadoPedido,
  formatearFechaPedido,
  formatoMoneda,
} from '../utils/pedido';
import Modal from './Modal';

/**
 * Modal con los detalles completos de un pedido.
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si la modal debe mostrarse.
 * @param {Object|null} props.pedido - Pedido seleccionado en el listado.
 * @param {Function} props.onCerrar - Acción al cerrar la modal.
 * @returns {JSX.Element} La modal con el detalle del pedido.
 */
function PedidoDetalleModal({ abierto, pedido, onCerrar }) {
  const titulo = pedido ? `Pedido de ${pedido.cliente_nombre}` : 'Pedido';

  return (
    <Modal abierto={abierto} titulo={titulo} onCerrar={onCerrar}>
      {pedido && (
        <>
          <span
            className={`ec-pedido-badge ec-pedido-badge--${pedido.estado_pedido}`}
          >
            {etiquetaEstadoPedido(pedido.estado_pedido)}
          </span>

          <dl className="ec-detalle">
            <div className="ec-detalle__fila">
              <dt>Fecha:</dt>
              <dd>{formatearFechaPedido(pedido.fecha_pedido)}</dd>
            </div>
            <div className="ec-detalle__fila">
              <dt>Cliente:</dt>
              <dd>{pedido.cliente_nombre}</dd>
            </div>
            <div className="ec-detalle__fila">
              <dt>Dirección:</dt>
              <dd>{pedido.cliente_direccion || 'Sin dirección'}</dd>
            </div>
          </dl>

          <ul className="ec-pedido-detalle__lineas">
            {pedido.detalles.map((detalle) => (
              <li className="ec-pedido-detalle__linea" key={detalle.id_tipo}>
                <span className="ec-pedido-detalle__tipo">
                  {detalle.cantidad} und {detalle.nombre_tipo}
                </span>
                <span className="ec-pedido-detalle__valores">
                  {formatoMoneda(detalle.precio_unitario)} c/u —{' '}
                  <strong>{formatoMoneda(detalle.subtotal)}</strong>
                </span>
              </li>
            ))}
          </ul>

          <div className="ec-pedido__total">
            <span>Total del pedido</span>
            <strong>{formatoMoneda(pedido.valor_total)}</strong>
          </div>
        </>
      )}
    </Modal>
  );
}

PedidoDetalleModal.propTypes = {
  abierto: PropTypes.bool,
  pedido: PropTypes.shape({
    cliente_nombre: PropTypes.string.isRequired,
    cliente_direccion: PropTypes.string,
    estado_pedido: PropTypes.string.isRequired,
    fecha_pedido: PropTypes.string.isRequired,
    valor_total: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
      .isRequired,
    detalles: PropTypes.arrayOf(
      PropTypes.shape({
        id_tipo: PropTypes.number.isRequired,
        nombre_tipo: PropTypes.string.isRequired,
        cantidad: PropTypes.number.isRequired,
        precio_unitario: PropTypes.oneOfType([
          PropTypes.string,
          PropTypes.number,
        ]).isRequired,
        subtotal: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
          .isRequired,
      })
    ),
  }),
  onCerrar: PropTypes.func.isRequired,
};

PedidoDetalleModal.defaultProps = {
  abierto: false,
  pedido: null,
};

export default PedidoDetalleModal;
