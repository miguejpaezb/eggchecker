import PropTypes from 'prop-types';
import { useEffect } from 'react';
import { createPortal } from 'react-dom';

/**
 * Ventana modal accesible reutilizable.
 * Cierra con Escape y con clic/tap en el fondo, y bloquea el scroll de fondo.
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si la modal debe mostrarse.
 * @param {string} props.titulo - Título visible de la modal.
 * @param {Function} props.onCerrar - Acción al cerrar.
 * @param {React.ReactNode} props.children - Contenido de la modal.
 * @returns {React.ReactPortal|null} La modal montada en el body.
 */
function Modal({ abierto, titulo, onCerrar, children }) {
  useEffect(() => {
    if (!abierto) {
      return undefined;
    }

    const cerrarConEscape = (event) => {
      if (event.key === 'Escape') {
        onCerrar();
      }
    };
    const overflowPrevio = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    document.addEventListener('keydown', cerrarConEscape);

    return () => {
      document.removeEventListener('keydown', cerrarConEscape);
      document.body.style.overflow = overflowPrevio;
    };
  }, [abierto, onCerrar]);

  if (!abierto) {
    return null;
  }

  return createPortal(
    <div className="ec-modal">
      <button
        type="button"
        className="ec-modal__backdrop"
        aria-label="Cerrar ventana"
        onClick={onCerrar}
      />
      <div
        className="ec-modal__dialog"
        role="dialog"
        aria-modal="true"
        aria-label={titulo}
      >
        <header className="ec-modal__head">
          <h2 className="ec-modal__title">{titulo}</h2>
          <button
            type="button"
            className="ec-modal__close"
            aria-label="Cerrar ventana"
            onClick={onCerrar}
          >
            &times;
          </button>
        </header>
        <div className="ec-modal__body">{children}</div>
      </div>
    </div>,
    document.body
  );
}

Modal.propTypes = {
  abierto: PropTypes.bool,
  titulo: PropTypes.string.isRequired,
  onCerrar: PropTypes.func.isRequired,
  children: PropTypes.node,
};

Modal.defaultProps = {
  abierto: false,
  children: null,
};

export default Modal;
