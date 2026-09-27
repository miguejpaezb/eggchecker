import PropTypes from 'prop-types';

/**
 * Tarjeta resumen de un cliente del listado.
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.cliente - Cliente devuelto por la API.
 * @param {Function} props.onVerDetalles - Acción al pulsar "Ver Detalles".
 * @param {React.ReactNode} props.children - Menú de acciones del cliente.
 * @returns {JSX.Element} La tarjeta del cliente.
 */
function ClienteCard({ cliente, onVerDetalles, children }) {
  return (
    <article className="ec-cliente-card ec-glass-card">
      <div className="ec-cliente-card__head">
        <div className="ec-cliente-card__info">
          <h3 className="ec-cliente-card__name">{cliente.nombre_cliente}</h3>
          <p className="ec-cliente-card__direccion">
            {cliente.direccion || 'Sin dirección'}
          </p>
        </div>
        <div className="ec-cliente-card__aside">
          <span
            className={`ec-cliente-badge ec-cliente-badge--${
              cliente.activo ? 'activo' : 'suspendido'
            }`}
          >
            {cliente.activo ? 'Activo' : 'Suspendido'}
          </span>
        </div>
      </div>

      <div className="ec-cliente-card__footer">
        <button
          type="button"
          className="ec-cliente-card__detail"
          onClick={() => onVerDetalles(cliente)}
        >
          Ver Detalles
        </button>
        {children}
      </div>
    </article>
  );
}

ClienteCard.propTypes = {
  cliente: PropTypes.shape({
    nombre_cliente: PropTypes.string.isRequired,
    direccion: PropTypes.string,
    activo: PropTypes.bool.isRequired,
  }).isRequired,
  onVerDetalles: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
};

export default ClienteCard;
