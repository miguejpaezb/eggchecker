import PropTypes from 'prop-types';

import { formatearTelefono, formatearUltimaCompra } from '../utils/cliente';
import Modal from './Modal';

/**
 * Modal con los detalles completos de un cliente.
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si la modal debe mostrarse.
 * @param {Object|null} props.cliente - Cliente seleccionado en el listado.
 * @param {Function} props.onCerrar - Acción al cerrar la modal.
 * @returns {JSX.Element} La modal con el detalle del cliente.
 */
function ClienteDetalleModal({ abierto, cliente, onCerrar }) {
  const titulo = cliente ? cliente.nombre_cliente : 'Detalle del cliente';

  return (
    <Modal abierto={abierto} titulo={titulo} onCerrar={onCerrar}>
      {cliente && (
        <>
          <span
            className={`ec-cliente-badge ec-cliente-badge--${
              cliente.activo ? 'activo' : 'suspendido'
            }`}
          >
            {cliente.activo ? 'Activo' : 'Suspendido'}
          </span>

          <dl className="ec-detalle">
            <div className="ec-detalle__fila">
              <dt>Teléfono:</dt>
              <dd>
                {cliente.telefono
                  ? formatearTelefono(cliente.telefono)
                  : 'Sin teléfono'}
              </dd>
            </div>
            <div className="ec-detalle__fila">
              <dt>Dirección:</dt>
              <dd>{cliente.direccion || 'Sin dirección'}</dd>
            </div>
            <div className="ec-detalle__fila">
              <dt>Última compra:</dt>
              <dd>{formatearUltimaCompra(cliente.fecha_ultima_compra)}</dd>
            </div>
          </dl>
        </>
      )}
    </Modal>
  );
}

ClienteDetalleModal.propTypes = {
  abierto: PropTypes.bool,
  cliente: PropTypes.shape({
    nombre_cliente: PropTypes.string.isRequired,
    telefono: PropTypes.string,
    direccion: PropTypes.string,
    fecha_ultima_compra: PropTypes.string,
    activo: PropTypes.bool.isRequired,
  }),
  onCerrar: PropTypes.func.isRequired,
};

ClienteDetalleModal.defaultProps = {
  abierto: false,
  cliente: null,
};

export default ClienteDetalleModal;
