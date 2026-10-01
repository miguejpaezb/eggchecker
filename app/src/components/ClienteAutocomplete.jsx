import PropTypes from 'prop-types';
import { useEffect, useMemo, useRef, useState } from 'react';

/**
 * Campo de texto con autocompletado de clientes activos.
 * Se comporta como un input, pero solo permite elegir un cliente de la
 * lista sugerida.
 * @param {Object} props - Propiedades del componente.
 * @param {string} props.id - Identificador del input.
 * @param {Array<Object>} props.clientes - Clientes disponibles.
 * @param {Object|null} props.clienteSeleccionado - Cliente activo.
 * @param {Function} props.onSeleccionar - Acción al elegir un cliente.
 * @param {string} [props.placeholder] - Texto de ayuda.
 * @param {boolean} [props.deshabilitado] - Si el campo está bloqueado.
 * @returns {JSX.Element} El autocompletado de clientes.
 */
function ClienteAutocomplete({
  id,
  clientes,
  clienteSeleccionado,
  onSeleccionar,
  placeholder,
  deshabilitado,
}) {
  const [abierto, setAbierto] = useState(false);
  const [texto, setTexto] = useState('');
  const ref = useRef(null);

  useEffect(() => {
    setTexto(clienteSeleccionado ? clienteSeleccionado.nombre_cliente : '');
  }, [clienteSeleccionado]);

  const opciones = useMemo(() => {
    const busqueda = texto.trim().toLowerCase();
    const seleccionado = clienteSeleccionado
      ? clienteSeleccionado.nombre_cliente.toLowerCase()
      : '';
    if (!busqueda || busqueda === seleccionado) {
      return clientes;
    }
    return clientes.filter(
      (cliente) =>
        cliente.nombre_cliente.toLowerCase().includes(busqueda) ||
        (cliente.direccion ?? '').toLowerCase().includes(busqueda)
    );
  }, [clientes, texto, clienteSeleccionado]);

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

  const seleccionar = (cliente) => {
    setTexto(cliente.nombre_cliente);
    onSeleccionar(cliente);
    setAbierto(false);
  };

  return (
    <div className="ec-autocomplete" ref={ref}>
      <input
        id={id}
        className="ec-form__input"
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
          {opciones.map((cliente) => (
            <li key={cliente.id_cliente}>
              <button
                type="button"
                className="ec-autocomplete__item"
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => seleccionar(cliente)}
              >
                {cliente.nombre_cliente}
                {cliente.direccion ? ` — ${cliente.direccion}` : ''}
              </button>
            </li>
          ))}
        </ul>
      )}
      {abierto && opciones.length === 0 && (
        <div className="ec-autocomplete__vacio">
          No hay clientes activos que coincidan.
        </div>
      )}
    </div>
  );
}

ClienteAutocomplete.propTypes = {
  id: PropTypes.string.isRequired,
  clientes: PropTypes.arrayOf(
    PropTypes.shape({
      id_cliente: PropTypes.number.isRequired,
      nombre_cliente: PropTypes.string.isRequired,
      direccion: PropTypes.string,
    })
  ),
  clienteSeleccionado: PropTypes.shape({
    id_cliente: PropTypes.number.isRequired,
    nombre_cliente: PropTypes.string.isRequired,
  }),
  onSeleccionar: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  deshabilitado: PropTypes.bool,
};

ClienteAutocomplete.defaultProps = {
  clientes: [],
  clienteSeleccionado: null,
  placeholder: '',
  deshabilitado: false,
};

export default ClienteAutocomplete;
