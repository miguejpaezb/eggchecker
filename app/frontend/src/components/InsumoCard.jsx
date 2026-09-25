import PropTypes from 'prop-types';

import {
  aNumero,
  formatearCantidad,
  nivelStock,
  porcentajeBarra,
} from '../utils/inventario';

/**
 * Tarjeta de un insumo del inventario con su barra de nivel de stock.
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.insumo - Insumo a mostrar.
 * @param {string} props.categoria - Nombre de la categoría del insumo.
 * @param {React.ReactNode} props.children - Menú de acciones del insumo.
 * @returns {JSX.Element} La tarjeta del insumo.
 */
function InsumoCard({ insumo, categoria, children }) {
  const nivel = nivelStock(insumo.stock_actual, insumo.umbral_minimo);
  const porcentaje = porcentajeBarra(insumo.stock_actual, insumo.umbral_minimo);
  const sinStock = aNumero(insumo.stock_actual) <= 0;

  return (
    <article
      className={`ec-insumo-card ec-glass-card${
        sinStock ? ' ec-insumo-card--sin-stock' : ''
      }`}
    >
      <div className="ec-insumo-card__head">
        <div>
          <h3 className="ec-insumo-card__name">{insumo.nombre_insumo}</h3>
          <p className="ec-insumo-card__meta">
            {categoria} - {insumo.unidad_medida}
          </p>
        </div>
        <div className="ec-insumo-card__aside">
          {!insumo.activo && (
            <span className="ec-insumo-badge">Suspendido</span>
          )}
          {children}
        </div>
      </div>

      <div className="ec-insumo-card__stock">
        <span>
          Stock: {formatearCantidad(insumo.stock_actual)} {insumo.unidad_medida}
        </span>
        <span>
          Mínimo: {formatearCantidad(insumo.umbral_minimo)}{' '}
          {insumo.unidad_medida}
        </span>
      </div>

      <div className="ec-insumo-bar">
        <div
          className={`ec-insumo-bar__fill ec-insumo-bar__fill--${nivel}`}
          style={{ width: `${porcentaje}%` }}
        />
      </div>
    </article>
  );
}

InsumoCard.propTypes = {
  insumo: PropTypes.shape({
    nombre_insumo: PropTypes.string.isRequired,
    unidad_medida: PropTypes.string.isRequired,
    stock_actual: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
      .isRequired,
    umbral_minimo: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
      .isRequired,
    activo: PropTypes.bool.isRequired,
  }).isRequired,
  categoria: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
};

export default InsumoCard;
