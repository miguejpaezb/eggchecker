import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import CategoriaAutocomplete from './CategoriaAutocomplete';
import InputDecimal from './InputDecimal';
import Modal from './Modal';

/**
 * Modal con el formulario para registrar un insumo nuevo.
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si la modal debe mostrarse.
 * @param {Array<Object>} props.categorias - Catálogo de categorías existente.
 * @param {Function} props.onCerrar - Acción al cerrar la modal.
 * @param {Function} props.onCrear - Crea el insumo; recibe los datos.
 * @returns {JSX.Element} La modal de creación de insumo.
 */
function NuevoInsumoModal({ abierto, categorias, onCerrar, onCrear }) {
  const [nombre, setNombre] = useState('');
  const [categoria, setCategoria] = useState('');
  const [unidad, setUnidad] = useState('');
  const [stock, setStock] = useState('');
  const [umbral, setUmbral] = useState('');
  const [avisoDecimal, setAvisoDecimal] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (abierto) {
      setNombre('');
      setCategoria('');
      setUnidad('');
      setStock('');
      setUmbral('');
      setAvisoDecimal('');
      setError('');
      setEnviando(false);
    }
  }, [abierto]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    const categoriaEncontrada = categorias.find(
      (item) =>
        item.nombre_categ.toLowerCase() === categoria.trim().toLowerCase()
    );
    if (!categoriaEncontrada) {
      setError('Selecciona una categoría existente');
      return;
    }

    const stockNumero = Number(stock);
    const umbralNumero = Number(umbral);
    if (!(stockNumero > 0)) {
      setError('El stock actual debe ser un número mayor que cero');
      return;
    }
    if (!(umbralNumero > 0)) {
      setError('El stock mínimo debe ser un número mayor que cero');
      return;
    }

    setEnviando(true);
    try {
      await onCrear({
        id_categoria: categoriaEncontrada.id_categoria,
        nombre_insumo: nombre.trim(),
        unidad_medida: unidad.trim(),
        stock_actual: stockNumero,
        umbral_minimo: umbralNumero,
      });
      onCerrar();
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal abierto={abierto} titulo="Añadir Insumo" onCerrar={onCerrar}>
      <form className="ec-form" onSubmit={handleSubmit}>
        {error && <p className="ec-form__error">{error}</p>}

        <label className="ec-form__field" htmlFor="insumo-nombre">
          <span className="ec-form__label">Nombre del insumo</span>
          <input
            id="insumo-nombre"
            className="ec-form__input"
            type="text"
            value={nombre}
            onChange={(event) => setNombre(event.target.value)}
            placeholder="Ej. Concentrado Ponedora 16%"
            maxLength={80}
            required
          />
        </label>

        <label className="ec-form__field" htmlFor="insumo-categoria">
          <span className="ec-form__label">Categoría</span>
          <CategoriaAutocomplete
            id="insumo-categoria"
            value={categoria}
            onChange={setCategoria}
            categorias={categorias}
            placeholder="Escribe para buscar…"
          />
          <span className="ec-form__hint">
            Solo categorías existentes; créalas en el botón Categoría.
          </span>
        </label>

        <label className="ec-form__field" htmlFor="insumo-unidad">
          <span className="ec-form__label">Unidad de medida</span>
          <input
            id="insumo-unidad"
            className="ec-form__input"
            type="text"
            value={unidad}
            onChange={(event) => setUnidad(event.target.value)}
            placeholder="kg, litro, unidad…"
            maxLength={20}
            required
          />
        </label>

        <label className="ec-form__field" htmlFor="insumo-stock">
          <span className="ec-form__label">Stock actual</span>
          <InputDecimal
            id="insumo-stock"
            value={stock}
            onChange={setStock}
            onInvalid={setAvisoDecimal}
            placeholder="Ej. 50"
          />
        </label>

        <label className="ec-form__field" htmlFor="insumo-umbral">
          <span className="ec-form__label">Stock mínimo</span>
          <InputDecimal
            id="insumo-umbral"
            value={umbral}
            onChange={setUmbral}
            onInvalid={setAvisoDecimal}
            placeholder="Ej. 10"
          />
        </label>

        {avisoDecimal && (
          <p className="ec-form__hint ec-form__hint--aviso">{avisoDecimal}</p>
        )}

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
            {enviando ? 'Guardando…' : 'Registrar insumo'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

NuevoInsumoModal.propTypes = {
  abierto: PropTypes.bool,
  categorias: PropTypes.arrayOf(
    PropTypes.shape({
      id_categoria: PropTypes.number.isRequired,
      nombre_categ: PropTypes.string.isRequired,
    })
  ),
  onCerrar: PropTypes.func.isRequired,
  onCrear: PropTypes.func.isRequired,
};

NuevoInsumoModal.defaultProps = {
  abierto: false,
  categorias: [],
};

export default NuevoInsumoModal;
