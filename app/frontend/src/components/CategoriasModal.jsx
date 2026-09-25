import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import ConfirmDialog from './ConfirmDialog';
import Modal from './Modal';

/**
 * Modal para gestionar (crear, editar y eliminar) categorías de insumo.
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si la modal debe mostrarse.
 * @param {Array<Object>} props.categorias - Categorías del catálogo.
 * @param {Function} props.onCerrar - Acción al cerrar la modal.
 * @param {Function} props.onCrear - Crea una categoría.
 * @param {Function} props.onEditar - Edita una categoría.
 * @param {Function} props.onEliminar - Elimina una categoría.
 * @returns {JSX.Element} La modal de categorías.
 */
function CategoriasModal({
  abierto,
  categorias,
  onCerrar,
  onCrear,
  onEditar,
  onEliminar,
}) {
  const [nombre, setNombre] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [editando, setEditando] = useState(null);
  const [error, setError] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [aEliminar, setAEliminar] = useState(null);
  const [errorEliminar, setErrorEliminar] = useState('');
  const [eliminando, setEliminando] = useState(false);

  useEffect(() => {
    if (abierto) {
      setNombre('');
      setDescripcion('');
      setEditando(null);
      setError('');
      setGuardando(false);
      setAEliminar(null);
      setErrorEliminar('');
      setEliminando(false);
    }
  }, [abierto]);

  const limpiarFormulario = () => {
    setNombre('');
    setDescripcion('');
    setEditando(null);
    setError('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    const datos = {
      nombre_categ: nombre.trim(),
      descripcion: descripcion.trim() || null,
    };

    setGuardando(true);
    try {
      if (editando) {
        await onEditar(editando.id_categoria, datos);
      } else {
        await onCrear(datos);
      }
      limpiarFormulario();
    } catch (err) {
      setError(err.message);
    } finally {
      setGuardando(false);
    }
  };

  const iniciarEdicion = (categoria) => {
    setEditando(categoria);
    setNombre(categoria.nombre_categ);
    setDescripcion(categoria.descripcion ?? '');
    setError('');
  };

  const confirmarEliminar = async () => {
    setErrorEliminar('');
    setEliminando(true);
    try {
      await onEliminar(aEliminar.id_categoria);
      setAEliminar(null);
    } catch (err) {
      setErrorEliminar(err.message);
    } finally {
      setEliminando(false);
    }
  };

  return (
    <Modal abierto={abierto} titulo="Categorías" onCerrar={onCerrar}>
      <form className="ec-form ec-categorias__form" onSubmit={handleSubmit}>
        {error && <p className="ec-form__error">{error}</p>}
        <div className="ec-categorias__campos">
          <label className="ec-form__field" htmlFor="categoria-nombre">
            <span className="ec-form__label">
              {editando ? 'Editar categoría' : 'Nueva categoría'}
            </span>
            <input
              id="categoria-nombre"
              className="ec-form__input"
              type="text"
              value={nombre}
              onChange={(event) => setNombre(event.target.value)}
              placeholder="Ej. Vitaminas"
              maxLength={60}
              required
            />
          </label>
          <label className="ec-form__field" htmlFor="categoria-descripcion">
            <span className="ec-form__label">Descripción (opcional)</span>
            <input
              id="categoria-descripcion"
              className="ec-form__input"
              type="text"
              value={descripcion}
              onChange={(event) => setDescripcion(event.target.value)}
              maxLength={200}
            />
          </label>
        </div>
        <div className="ec-form__actions">
          {editando && (
            <button
              type="button"
              className="ec-btn ec-btn--ghost"
              onClick={limpiarFormulario}
              disabled={guardando}
            >
              Cancelar edición
            </button>
          )}
          <button
            type="submit"
            className="ec-btn ec-btn--primary"
            disabled={guardando}
          >
            {editando ? 'Guardar cambios' : 'Agregar categoría'}
          </button>
        </div>
      </form>

      <ul className="ec-categorias__lista">
        {categorias.map((categoria) => (
          <li key={categoria.id_categoria} className="ec-categorias__item">
            <div className="ec-categorias__info">
              <span className="ec-categorias__nombre">
                {categoria.nombre_categ}
              </span>
              {categoria.descripcion && (
                <span className="ec-categorias__descripcion">
                  {categoria.descripcion}
                </span>
              )}
            </div>
            <div className="ec-categorias__acciones">
              <button
                type="button"
                className="ec-categorias__btn"
                onClick={() => iniciarEdicion(categoria)}
              >
                Editar
              </button>
              <button
                type="button"
                className="ec-categorias__btn ec-categorias__btn--danger"
                onClick={() => {
                  setErrorEliminar('');
                  setAEliminar(categoria);
                }}
              >
                Eliminar
              </button>
            </div>
          </li>
        ))}
      </ul>

      <ConfirmDialog
        abierto={aEliminar !== null}
        titulo="Eliminar categoría"
        mensaje={
          aEliminar
            ? `¿Eliminar la categoría "${aEliminar.nombre_categ}"? Solo se puede si no tiene insumos asociados.`
            : ''
        }
        textoConfirmar="Eliminar"
        peligro
        cargando={eliminando}
        error={errorEliminar}
        onConfirmar={confirmarEliminar}
        onCerrar={() => setAEliminar(null)}
      />
    </Modal>
  );
}

CategoriasModal.propTypes = {
  abierto: PropTypes.bool,
  categorias: PropTypes.arrayOf(
    PropTypes.shape({
      id_categoria: PropTypes.number.isRequired,
      nombre_categ: PropTypes.string.isRequired,
      descripcion: PropTypes.string,
    })
  ),
  onCerrar: PropTypes.func.isRequired,
  onCrear: PropTypes.func.isRequired,
  onEditar: PropTypes.func.isRequired,
  onEliminar: PropTypes.func.isRequired,
};

CategoriasModal.defaultProps = {
  abierto: false,
  categorias: [],
};

export default CategoriasModal;
