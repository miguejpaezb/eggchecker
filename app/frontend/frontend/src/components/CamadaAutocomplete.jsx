import PropTypes from 'prop-types';
import { useEffect, useMemo, useRef, useState } from 'react';

/**
 * Campo de texto con autocompletado de camadas en etapa de producción.
 * Se comporta como un input, pero solo permite elegir una camada de la
 * lista sugerida.
 * @param {Object} props - Propiedades del componente.
 * @param {string} props.id - Identificador del input.
 * @param {Array<Object>} props.camadas - Camadas disponibles.
 * @param {Object|null} props.camadaSeleccionada - Camada activa.
 * @param {Function} props.onSeleccionar - Acción al elegir una camada.
 * @param {string} [props.placeholder] - Texto de ayuda.
 * @param {boolean} [props.deshabilitado] - Si el campo está bloqueado.
 * @returns {JSX.Element} El autocompletado de camadas.
 */
function CamadaAutocomplete({
  id,
  camadas,
  camadaSeleccionada,
  onSeleccionar,
  placeholder,
  deshabilitado,
}) {
  const [abierto, setAbierto] = useState(false);
  const [texto, setTexto] = useState('');
  const ref = useRef(null);

  useEffect(() => {
    setTexto(camadaSeleccionada ? camadaSeleccionada.nombre_camada : '');
  }, [camadaSeleccionada]);

  const opciones = useMemo(() => {
    const busqueda = texto.trim().toLowerCase();
    const seleccionada = camadaSeleccionada
      ? camadaSeleccionada.nombre_camada.toLowerCase()
      : '';
    if (!busqueda || busqueda === seleccionada) {
      return camadas;
    }
    return camadas.filter((camada) =>
      camada.nombre_camada.toLowerCase().includes(busqueda)
    );
  }, [camadas, texto, camadaSeleccionada]);

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

  const seleccionar = (camada) => {
    setTexto(camada.nombre_camada);
    onSeleccionar(camada);
    setAbierto(false);
  };

  return (
    <div className="ec-autocomplete" ref={ref}>
      <input
        id={id}
        className="ec-produccion__camada-input"
        type="text"
        value={texto}
        onChange={(event) => {
          setTexto(event.target.value);
          setAbierto(true);
        }}
        onFocus={() => setAbierto(true)}
        placeholder={placeholder}
        autoComplete="off"
        disabled={deshabilitado}
      />
      {abierto && opciones.length > 0 && (
        <ul className="ec-autocomplete__lista">
          {opciones.map((camada) => (
            <li key={camada.id_camada}>
              <button
                type="button"
                className="ec-autocomplete__item"
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => seleccionar(camada)}
              >
                {camada.nombre_camada}
              </button>
            </li>
          ))}
        </ul>
      )}
      {abierto && opciones.length === 0 && (
        <div className="ec-autocomplete__vacio">
          No hay camadas en etapa de producción.
        </div>
      )}
    </div>
  );
}

CamadaAutocomplete.propTypes = {
  id: PropTypes.string.isRequired,
  camadas: PropTypes.arrayOf(
    PropTypes.shape({
      id_camada: PropTypes.number.isRequired,
      nombre_camada: PropTypes.string.isRequired,
    })
  ),
  camadaSeleccionada: PropTypes.shape({
    id_camada: PropTypes.number.isRequired,
    nombre_camada: PropTypes.string.isRequired,
  }),
  onSeleccionar: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  deshabilitado: PropTypes.bool,
};

CamadaAutocomplete.defaultProps = {
  camadas: [],
  camadaSeleccionada: null,
  placeholder: '',
  deshabilitado: false,
};

export default CamadaAutocomplete;
