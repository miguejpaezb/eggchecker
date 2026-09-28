import PropTypes from 'prop-types';
import { useEffect, useMemo, useState } from 'react';

import { formatoMoneda } from '../utils/pedido';
import ClienteAutocomplete from './ClienteAutocomplete';
import InputDecimal from './InputDecimal';
import Modal from './Modal';

/**
 * Devuelve la cantidad ya reservada por un pedido para un tipo de huevo.
 * @param {Object|null} pedido - Pedido en edición o null.
 * @param {number} idTipo - Identificador del tipo de huevo.
 * @returns {number} Cantidad reservada por el pedido.
 */
function cantidadReservada(pedido, idTipo) {
  const detalle = (pedido?.detalles ?? []).find(
    (fila) => fila.id_tipo === idTipo
  );
  return detalle ? detalle.cantidad : 0;
}

/**
 * Modal para registrar o editar un pedido.
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si la modal debe mostrarse.
 * @param {Array<Object>} props.clientes - Clientes activos para elegir.
 * @param {Array<Object>} props.stockPorTipo - Stock disponible por tipo.
 * @param {Object|null} props.pedido - Pedido a editar, o null para crear.
 * @param {Object|null} props.clienteInicial - Cliente preseleccionado.
 * @param {Function} props.onCerrar - Acción al cerrar la modal.
 * @param {Function} props.onGuardar - Recibe (datos, idPedido|null).
 * @returns {JSX.Element} La modal de pedido.
 */
function NuevoPedidoModal({
  abierto,
  clientes,
  stockPorTipo,
  pedido,
  clienteInicial,
  onCerrar,
  onGuardar,
}) {
  const [clienteSeleccionado, setClienteSeleccionado] = useState(null);
  const [cantidades, setCantidades] = useState({});
  const [precios, setPrecios] = useState({});
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  const esEdicion = pedido !== null;
  const textoGuardar = esEdicion ? 'Guardar cambios' : 'Generar venta';

  useEffect(() => {
    if (!abierto) {
      return;
    }
    const cantidadesIniciales = {};
    const preciosIniciales = {};
    (stockPorTipo ?? []).forEach((tipo) => {
      const detalle = (pedido?.detalles ?? []).find(
        (fila) => fila.id_tipo === tipo.id_tipo
      );
      cantidadesIniciales[tipo.id_tipo] = detalle
        ? String(detalle.cantidad)
        : '';
      preciosIniciales[tipo.id_tipo] = detalle
        ? String(detalle.precio_unitario)
        : String(tipo.valor_unidad);
    });
    setCantidades(cantidadesIniciales);
    setPrecios(preciosIniciales);

    if (pedido) {
      setClienteSeleccionado(
        clientes.find((item) => item.id_cliente === pedido.id_cliente) ?? {
          id_cliente: pedido.id_cliente,
          nombre_cliente: pedido.cliente_nombre,
        }
      );
    } else {
      setClienteSeleccionado(clienteInicial ?? null);
    }
    setError('');
    setEnviando(false);
  }, [abierto, pedido, clienteInicial, stockPorTipo, clientes]);

  const disponible = useMemo(() => {
    const mapa = {};
    (stockPorTipo ?? []).forEach((tipo) => {
      mapa[tipo.id_tipo] =
        tipo.cantidad_actual + cantidadReservada(pedido, tipo.id_tipo);
    });
    return mapa;
  }, [stockPorTipo, pedido]);

  const total = useMemo(
    () =>
      (stockPorTipo ?? []).reduce((suma, tipo) => {
        const cantidad = Number(cantidades[tipo.id_tipo]) || 0;
        const precio = Number(precios[tipo.id_tipo]) || 0;
        return suma + cantidad * precio;
      }, 0),
    [stockPorTipo, cantidades, precios]
  );

  const cambiarCantidad = (idTipo, valor) => {
    setCantidades((actual) => ({ ...actual, [idTipo]: valor }));
  };

  const cambiarPrecio = (idTipo, valor) => {
    setPrecios((actual) => ({ ...actual, [idTipo]: valor }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    if (!clienteSeleccionado) {
      setError('Selecciona un cliente');
      return;
    }

    const detalles = [];
    let errorValidacion = '';
    (stockPorTipo ?? []).some((tipo) => {
      const cantidad = Number(cantidades[tipo.id_tipo]) || 0;
      if (cantidad === 0) {
        return false;
      }
      if (!Number.isInteger(cantidad) || cantidad < 0) {
        errorValidacion = 'Las cantidades deben ser números enteros positivos';
        return true;
      }
      const precio = Number(precios[tipo.id_tipo]);
      if (!(precio > 0)) {
        errorValidacion = `El precio de ${tipo.nombre_tipo} debe ser mayor que cero`;
        return true;
      }
      if (cantidad > disponible[tipo.id_tipo]) {
        errorValidacion = `La cantidad de ${tipo.nombre_tipo} supera el stock disponible (${disponible[tipo.id_tipo]})`;
        return true;
      }
      detalles.push({
        id_tipo: tipo.id_tipo,
        cantidad,
        precio_unitario: precio,
      });
      return false;
    });

    if (errorValidacion) {
      setError(errorValidacion);
      return;
    }
    if (detalles.length === 0) {
      setError('Ingresa al menos una cantidad de huevos');
      return;
    }

    setEnviando(true);
    try {
      await onGuardar(
        { id_cliente: clienteSeleccionado.id_cliente, detalles },
        pedido ? pedido.id_pedido : null
      );
      onCerrar();
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal
      abierto={abierto}
      titulo={esEdicion ? 'Editar Pedido' : 'Nuevo Pedido'}
      onCerrar={onCerrar}
    >
      <form className="ec-form" onSubmit={handleSubmit} noValidate>
        <label className="ec-form__field" htmlFor="pedido-cliente">
          <span className="ec-form__label">Cliente</span>
          <ClienteAutocomplete
            id="pedido-cliente"
            clientes={clientes}
            clienteSeleccionado={clienteSeleccionado}
            onSeleccionar={setClienteSeleccionado}
            placeholder="Escribe para buscar…"
          />
        </label>

        <div className="ec-pedido__lineas">
          {(stockPorTipo ?? []).map((tipo) => {
            const cantidad = Number(cantidades[tipo.id_tipo]) || 0;
            const precio = Number(precios[tipo.id_tipo]) || 0;
            return (
              <div className="ec-pedido__linea" key={tipo.id_tipo}>
                <div className="ec-pedido__linea-head">
                  <span className="ec-pedido__tipo">{tipo.nombre_tipo}</span>
                  <span className="ec-pedido__disponible">
                    Disponible: {disponible[tipo.id_tipo]}
                  </span>
                </div>
                <div className="ec-pedido__linea-campos">
                  <label
                    className="ec-form__field"
                    htmlFor={`pedido-cantidad-${tipo.id_tipo}`}
                  >
                    <span className="ec-form__label">Cantidad</span>
                    <input
                      id={`pedido-cantidad-${tipo.id_tipo}`}
                      className="ec-form__input"
                      type="number"
                      inputMode="numeric"
                      min="0"
                      step="1"
                      value={cantidades[tipo.id_tipo] ?? ''}
                      onChange={(event) =>
                        cambiarCantidad(tipo.id_tipo, event.target.value)
                      }
                      placeholder="0"
                    />
                  </label>
                  <label
                    className="ec-form__field"
                    htmlFor={`pedido-precio-${tipo.id_tipo}`}
                  >
                    <span className="ec-form__label">Valor unidad</span>
                    <InputDecimal
                      id={`pedido-precio-${tipo.id_tipo}`}
                      value={precios[tipo.id_tipo] ?? String(tipo.valor_unidad)}
                      onChange={(valor) => cambiarPrecio(tipo.id_tipo, valor)}
                      placeholder="0"
                    />
                  </label>
                </div>
                <p className="ec-pedido__subtotal">
                  Subtotal: {formatoMoneda(cantidad * precio)}
                </p>
              </div>
            );
          })}
        </div>

        <div className="ec-pedido__total">
          <span>Total del pedido</span>
          <strong>{formatoMoneda(total)}</strong>
        </div>

        {error && <p className="ec-form__error">{error}</p>}

        <div className="ec-form__actions">
          <button
            type="button"
            className="ec-btn ec-btn--ghost"
            onClick={onCerrar}
            disabled={enviando}
          >
            Cancelar
          </button>
          <button
            type="submit"
            className="ec-btn ec-btn--primary"
            disabled={enviando}
          >
            {enviando ? 'Guardando…' : textoGuardar}
          </button>
        </div>
      </form>
    </Modal>
  );
}

NuevoPedidoModal.propTypes = {
  abierto: PropTypes.bool,
  clientes: PropTypes.arrayOf(
    PropTypes.shape({
      id_cliente: PropTypes.number.isRequired,
      nombre_cliente: PropTypes.string.isRequired,
      direccion: PropTypes.string,
    })
  ),
  stockPorTipo: PropTypes.arrayOf(
    PropTypes.shape({
      id_tipo: PropTypes.number.isRequired,
      nombre_tipo: PropTypes.string.isRequired,
      cantidad_actual: PropTypes.number.isRequired,
      valor_unidad: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
        .isRequired,
    })
  ),
  pedido: PropTypes.shape({
    id_pedido: PropTypes.number.isRequired,
    id_cliente: PropTypes.number.isRequired,
    cliente_nombre: PropTypes.string.isRequired,
    detalles: PropTypes.arrayOf(
      PropTypes.shape({
        id_tipo: PropTypes.number.isRequired,
        cantidad: PropTypes.number.isRequired,
        precio_unitario: PropTypes.oneOfType([
          PropTypes.string,
          PropTypes.number,
        ]).isRequired,
      })
    ),
  }),
  clienteInicial: PropTypes.shape({
    id_cliente: PropTypes.number.isRequired,
    nombre_cliente: PropTypes.string.isRequired,
  }),
  onCerrar: PropTypes.func.isRequired,
  onGuardar: PropTypes.func.isRequired,
};

NuevoPedidoModal.defaultProps = {
  abierto: false,
  clientes: [],
  stockPorTipo: [],
  pedido: null,
  clienteInicial: null,
};

export default NuevoPedidoModal;
