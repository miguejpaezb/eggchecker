import PropTypes from 'prop-types';

import ConfirmDialog from '../ConfirmDialog';
import Modal from '../Modal';
import DiagnosticoContenido, { analisisPropType } from './DiagnosticoContenido';

/**
 * Detalle de un análisis del historial, con su foto y opción de borrar.
 * @param {Object} props - Propiedades del componente.
 * @returns {JSX.Element} La modal de detalle y su confirmación de borrado.
 */
function AnalisisDetalleModal({
  analisis,
  imagen,
  cargandoImagen,
  confirmarEliminar,
  eliminando,
  errorEliminar,
  onCerrar,
  onRetroalimentar,
  onPedirEliminar,
  onCancelarEliminar,
  onEliminar,
}) {
  return (
    <>
      <Modal
        abierto={Boolean(analisis) && !confirmarEliminar}
        titulo="Detalle del análisis"
        onCerrar={onCerrar}
      >
        {analisis && (
          <>
            {analisis.tiene_imagen && (
              <div className="ec-analisis__zona ec-analisis__zona--detalle">
                {imagen && (
                  <img
                    src={imagen}
                    alt="Foto analizada"
                    className="ec-analisis__foto"
                  />
                )}
                {!imagen && (
                  <p className="ec-analisis__zona-texto">
                    {cargandoImagen
                      ? 'Cargando foto…'
                      : 'No se pudo cargar la foto'}
                  </p>
                )}
              </div>
            )}
            <DiagnosticoContenido
              analisis={analisis}
              onRetroalimentar={onRetroalimentar}
            />
            <div className="ec-form__actions">
              <button
                type="button"
                className="ec-btn ec-btn--ghost"
                onClick={onPedirEliminar}
              >
                Eliminar análisis
              </button>
            </div>
          </>
        )}
      </Modal>

      <ConfirmDialog
        abierto={Boolean(analisis) && confirmarEliminar}
        titulo="Eliminar análisis"
        mensaje="Se borrarán el diagnóstico y la foto. Esta acción no se puede deshacer."
        textoConfirmar="Eliminar"
        peligro
        cargando={eliminando}
        error={errorEliminar}
        onConfirmar={onEliminar}
        onCerrar={onCancelarEliminar}
      />
    </>
  );
}

AnalisisDetalleModal.propTypes = {
  analisis: analisisPropType,
  imagen: PropTypes.string,
  cargandoImagen: PropTypes.bool.isRequired,
  confirmarEliminar: PropTypes.bool.isRequired,
  eliminando: PropTypes.bool.isRequired,
  errorEliminar: PropTypes.string,
  onCerrar: PropTypes.func.isRequired,
  onRetroalimentar: PropTypes.func.isRequired,
  onPedirEliminar: PropTypes.func.isRequired,
  onCancelarEliminar: PropTypes.func.isRequired,
  onEliminar: PropTypes.func.isRequired,
};

AnalisisDetalleModal.defaultProps = {
  analisis: null,
  imagen: '',
  errorEliminar: '',
};

export default AnalisisDetalleModal;
