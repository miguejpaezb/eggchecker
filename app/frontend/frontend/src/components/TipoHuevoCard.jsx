import PropTypes from 'prop-types';

/**
 * Tarjeta de un tipo de huevo con su contador y botones de ajuste.
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.tipo - Tipo de huevo (clave, nombre, título, etc.).
 * @param {number} props.valor - Cantidad registrada ese día.
 * @param {Function} props.onIncrementar - Acción al pulsar "+".
 * @param {Function} props.onDecrementar - Acción al pulsar "-".
 * @param {boolean} props.deshabilitado - Bloquea ambos botones.
 * @param {boolean} props.limiteAlcanzado - Bloquea solo el botón "+".
 * @returns {JSX.Element} La tarjeta del tipo de huevo.
 */
function TipoHuevoCard({
  tipo,
  valor,
  onIncrementar,
  onDecrementar,
  deshabilitado,
  limiteAlcanzado,
}) {
  return (
    <div className={`ec-tipo-card ec-tipo-card--${tipo.clase}`}>
      <div className="ec-tipo-card__info">
        <h3 className="ec-tipo-card__titulo">{tipo.titulo}</h3>
        {tipo.descripcion && (
          <p className="ec-tipo-card__descripcion">{tipo.descripcion}</p>
        )}
      </div>
      <div className="ec-tipo-card__contador">
        <button
          type="button"
          className="ec-tipo-card__btn"
          onClick={() => onDecrementar(tipo.clave)}
          disabled={deshabilitado || valor === 0}
          aria-label={`Quitar un ${tipo.nombre}`}
        >
          -
        </button>
        <span className="ec-tipo-card__valor">{valor}</span>
        <button
          type="button"
          className="ec-tipo-card__btn"
          onClick={() => onIncrementar(tipo.clave)}
          disabled={deshabilitado || limiteAlcanzado}
          aria-label={`Añadir un ${tipo.nombre}`}
        >
          +
        </button>
      </div>
    </div>
  );
}

TipoHuevoCard.propTypes = {
  tipo: PropTypes.shape({
    clave: PropTypes.string.isRequired,
    nombre: PropTypes.string.isRequired,
    titulo: PropTypes.string.isRequired,
    descripcion: PropTypes.string,
    clase: PropTypes.string.isRequired,
  }).isRequired,
  valor: PropTypes.number.isRequired,
  onIncrementar: PropTypes.func.isRequired,
  onDecrementar: PropTypes.func.isRequired,
  deshabilitado: PropTypes.bool,
  limiteAlcanzado: PropTypes.bool,
};

TipoHuevoCard.defaultProps = {
  deshabilitado: false,
  limiteAlcanzado: false,
};

export default TipoHuevoCard;
