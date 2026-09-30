import PropTypes from 'prop-types';

import { etiquetaEstadoPedido, formatearFechaPedido } from '../../utils/pedido';

/**
 * Tarjeta "Pedidos recientes": últimos pedidos con su estado.
 * @param {Object} props - Propiedades del componente.
 * @param {Array<Object>} props.pedidos - Pedidos ordenados del más reciente.
 * @returns {JSX.Element} La tarjeta de pedidos recientes.
 */
function PedidosRecientesCard({ pedidos }) {
  return (
    <section className="ec-glass-card ec-dashboard__card">
      <h3 className="ec-dashboard__card-titulo">Pedidos recientes</h3>

      {pedidos.length === 0 ? (
        <p className="ec-dashboard__vacio">Aún no hay pedidos registrados.</p>
      ) : (
        <div className="ec-dashboard__pedidos">
          {pedidos.map((pedido) => (
            <div key={pedido.id_pedido} className="ec-dashboard__pedido">
              <div className="ec-dashboard__pedido-info">
                <h4 className="ec-dashboard__pedido-cliente">
                  {pedido.cliente_nombre}
                </h4>
                <p className="ec-dashboard__pedido-detalle">
                  {pedido.descripcion} |{' '}
                  {formatearFechaPedido(pedido.fecha_pedido)}
                </p>
              </div>
              <span
                className={`ec-dashboard__estado ec-dashboard__estado--${pedido.estado_pedido}`}
              >
                {etiquetaEstadoPedido(pedido.estado_pedido)}
              </span>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

PedidosRecientesCard.propTypes = {
  pedidos: PropTypes.arrayOf(
    PropTypes.shape({
      id_pedido: PropTypes.number.isRequired,
      cliente_nombre: PropTypes.string.isRequired,
      descripcion: PropTypes.string.isRequired,
      fecha_pedido: PropTypes.string.isRequired,
      estado_pedido: PropTypes.string.isRequired,
    })
  ).isRequired,
};

export default PedidosRecientesCard;
