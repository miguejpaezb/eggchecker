import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import { listarCategorias } from '../services/categoriaService';
import { listarInsumos } from '../services/insumoService';
import Modal from './Modal';

/**
 * Modal para registrar el alimento consumido por una camada.
 * Solo muestra insumos de categoría alimento con stock disponible y
 * limita la cantidad al stock actual en tiempo real.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si la modal debe mostrarse.
 * @param {Object|null} props.camada - Camada que se va a alimentar.
 * @param {Function} props.onCerrar - Acción al cerrar la modal.
 * @param {Function} props.onRegistrar - Registra el alimento; recibe (id, datos).
 * @returns {JSX.Element} La modal de alimentación.
 */
function AlimentarModal({ abierto, camada, onCerrar, onRegistrar }) {
  const [alimentos, setAlimentos] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [idInsumo, setIdInsumo] = useState('');
  const [cantidad, setCantidad] = useState('');
  const [confirmando, setConfirmando] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!abierto) {
      return undefined;
    }

    let activo = true;
    setAlimentos([]);
    setIdInsumo('');
    setCantidad('');
    setConfirmando(false);
    setEnviando(false);
    setError('');
    setCargando(true);

    Promise.all([listarInsumos(), listarCategorias()])
      .then(([insumos, categorias]) => {
        if (!activo) {
          return;
        }
        const categoria = categorias.find(
          (item) => item.nombre_categ === 'alimento'
        );
        setAlimentos(
          insumos.filter(
            (insumo) =>
              insumo.id_categoria === categoria?.id_categoria &&
              insumo.activo &&
              !insumo.descontinuado &&
              Number(insumo.stock_actual) > 0
          )
        );
      })
      .catch((err) => {
        if (activo) {
          setError(err.message);
        }
      })
      .finally(() => {
        if (activo) {
          setCargando(false);
        }
      });

    return () => {
      activo = false;
    };
  }, [abierto]);

  const insumoElegido = alimentos.find(
    (insumo) => String(insumo.id_insumo) === String(idInsumo)
  );
  const stock = insumoElegido ? Number(insumoElegido.stock_actual) : 0;
  const unidad = insumoElegido?.unidad_medida || '';

  const cambiarCantidad = (valor) => {
    // No se permite escribir por encima del stock disponible.
    if (valor !== '' && Number(valor) > stock) {
      setCantidad(String(stock));
      setError(
        `La cantidad no puede superar el stock disponible (${stock} ${unidad}).`
      );
      return;
    }
    setCantidad(valor);
    setError('');
  };

  const handleCerrar = () => {
    setConfirmando(false);
    onCerrar();
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    setError('');
    if (!insumoElegido) {
      setError('Elige el alimento.');
      return;
    }
    const numero = Number(cantidad);
    if (!Number.isFinite(numero) || numero <= 0) {
      setError('La cantidad debe ser mayor que cero.');
      return;
    }
    if (numero > stock) {
      setError('La cantidad supera el stock disponible.');
      return;
    }
    setConfirmando(true);
  };

  const handleConfirmar = async () => {
    setError('');
    setEnviando(true);
    try {
      await onRegistrar(camada.id_camada, {
        id_insumo: insumoElegido.id_insumo,
        cantidad: Number(cantidad),
      });
      setConfirmando(false);
      onCerrar();
    } catch (err) {
      setError(err.message);
      setConfirmando(false);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal abierto={abierto} titulo="Alimentar camada" onCerrar={handleCerrar}>
      {confirmando && camada && insumoElegido ? (
        <>
          <p className="ec-confirm__mensaje">
            ¿Alimentar <strong>{camada.nombre_camada}</strong> con{' '}
            <strong>
              {cantidad} {unidad}
            </strong>{' '}
            de <strong>{insumoElegido.nombre_insumo}</strong>? Se descontará del
            inventario.
          </p>
          {error && <p className="ec-form__error">{error}</p>}
          <div className="ec-form__actions">
            <button
              type="button"
              className="ec-btn ec-btn--ghost"
              onClick={() => setConfirmando(false)}
              disabled={enviando}
            >
              Volver
            </button>
            <button
              type="button"
              className="ec-btn ec-btn--primary"
              onClick={handleConfirmar}
              disabled={enviando}
            >
              {enviando ? 'Procesando…' : 'Confirmar'}
            </button>
          </div>
        </>
      ) : (
        <form className="ec-form" onSubmit={handleSubmit}>
          {error && <p className="ec-form__error">{error}</p>}
          {cargando && <p className="ec-form__hint">Cargando alimentos…</p>}

          <label className="ec-form__field" htmlFor="alimentar-insumo">
            <span className="ec-form__label">Alimento</span>
            <select
              id="alimentar-insumo"
              className="ec-form__input"
              value={idInsumo}
              onChange={(event) => {
                setIdInsumo(event.target.value);
                setCantidad('');
                setError('');
              }}
              disabled={cargando || alimentos.length === 0}
              required
            >
              <option value="">Elegir alimento</option>
              {alimentos.map((insumo) => (
                <option key={insumo.id_insumo} value={insumo.id_insumo}>
                  {insumo.nombre_insumo} · {insumo.unidad_medida} · stock{' '}
                  {insumo.stock_actual}
                </option>
              ))}
            </select>
            {!cargando && alimentos.length === 0 && (
              <span className="ec-form__hint ec-form__hint--aviso">
                No tienes insumos de categoría alimento con stock disponible.
              </span>
            )}
            {insumoElegido && (
              <span className="ec-form__hint ec-alimentar__stock">
                Stock disponible: {stock} {unidad}
              </span>
            )}
          </label>

          <label className="ec-form__field" htmlFor="alimentar-cantidad">
            <span className="ec-form__label">
              Cantidad
              {insumoElegido ? ` (${unidad})` : ''}
            </span>
            <input
              id="alimentar-cantidad"
              className="ec-form__input"
              type="number"
              min="0.01"
              step="0.01"
              max={stock || undefined}
              value={cantidad}
              onChange={(event) => cambiarCantidad(event.target.value)}
              placeholder="Ej. 12.5"
              disabled={!insumoElegido}
              required
            />
          </label>

          <div className="ec-form__actions">
            <button
              type="button"
              className="ec-btn ec-btn--ghost"
              onClick={handleCerrar}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="ec-btn ec-btn--primary"
              disabled={!insumoElegido || cargando}
            >
              Continuar
            </button>
          </div>
        </form>
      )}
    </Modal>
  );
}

AlimentarModal.propTypes = {
  abierto: PropTypes.bool,
  camada: PropTypes.shape({
    id_camada: PropTypes.number.isRequired,
    nombre_camada: PropTypes.string.isRequired,
  }),
  onCerrar: PropTypes.func.isRequired,
  onRegistrar: PropTypes.func.isRequired,
};

AlimentarModal.defaultProps = {
  abierto: false,
  camada: null,
};

export default AlimentarModal;
