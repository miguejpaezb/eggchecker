import PropTypes from 'prop-types';

/**
 * Renderiza un ícono SVG coloreado con la técnica de máscara CSS.
 * El color se hereda del `color` del elemento contenedor.
 * @param {Object} props - Propiedades del componente.
 * @param {string} props.src - Ruta pública del SVG.
 * @param {string} [props.className] - Clases adicionales de tamaño/posición.
 * @returns {JSX.Element} El ícono enmascarado.
 */
function Icon({ src, className }) {
  const mascara = `url('${src}')`;
  return (
    <span
      className={`ec-icon ${className}`}
      style={{ WebkitMaskImage: mascara, maskImage: mascara }}
      aria-hidden="true"
    />
  );
}

Icon.propTypes = {
  src: PropTypes.string.isRequired,
  className: PropTypes.string,
};

Icon.defaultProps = {
  className: '',
};

export default Icon;
