import PropTypes from 'prop-types';
import { useEffect, useMemo, useRef, useState } from 'react';

/**
 * Campo de texto con autocompletado desplegable de categorías.
 * Se comporta como un input, pero sugiere las categorías existentes como
 * un select; solo se puede elegir una categoría del catálogo.
 * @param {Object} props - Propiedades del componente.
 * @param {string} props.id - Identificador del input.
 * @param {string} props.value - Texto actual del campo.
 * @param {Function} props.onChange - Acción al cambiar el texto o elegir.
 * @param {Array<Object>} props.categorias - Categorías sugeridas.
 * @param {string} [props.placeholder] - Texto de ayuda.
 * @returns {JSX.Element} El autocompletado de categorías.
 */
function CategoriaAutocomplete({
  id,
  value,
  onChange,
  categorias,
  placeholder,
}) {
  const [abierto, setAbierto] = useState(false);
  const ref = useRef(null);

  const opciones = useMemo(() => {
    const texto = value.trim().toLowerCase();
    if (!texto) {
      return categorias;
    }
    return categorias.filter((categoria) =>
      categoria.nombre_categ.toLowerCase().includes(texto)
    );
  }, [categorias, value]);

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

  const seleccionar = (nombre) => {
    onChange(nombre);
    setAbierto(false);
  };

  return (
    <div className="ec-autocomplete" ref={ref}>
      <input
        id={id}
        className="ec-form__input"
        type="text"
        value={value}
        onChange={(event) => {
          onChange(event.target.value);
          setAbierto(true);
        }}
        onFocus={() => setAbierto(true)}
        placeholder={placeholder}
        autoComplete="off"
        required
      />
      {abierto && opciones.length > 0 && (
        <ul className="ec-autocomplete__lista">
          {opciones.map((categoria) => (
            <li key={categoria.id_categoria}>
              <button
                type="button"
                className="ec-autocomplete__item"
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => seleccionar(categoria.nombre_categ)}
              >
                {categoria.nombre_categ}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

CategoriaAutocomplete.propTypes = {
  id: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  categorias: PropTypes.arrayOf(
    PropTypes.shape({
      id_categoria: PropTypes.number.isRequired,
      nombre_categ: PropTypes.string.isRequired,
    })
  ),
  placeholder: PropTypes.string,
};

CategoriaAutocomplete.defaultProps = {
  categorias: [],
  placeholder: '',
};

export default CategoriaAutocomplete;
