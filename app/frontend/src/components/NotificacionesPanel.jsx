import PropTypes from 'prop-types';

import Icon from './Icon';

/**
 * Panel desplegable de notificaciones anclado a la campanita del topbar.
 * @param {Object} props - Propiedades del componente.
 * @param {Array<Object>} props.notificaciones - Notificaciones a listar.
 * @param {boolean} props.cargando - Si se están cargando.
 * @param {string} props.error - Mensaje de error a mostrar.
 * @param {Function} props.onMarcarLeida - Marca una notificación como leída.
 * @param {Function} props.onMarcarTodas - Marca todas como leídas.
 * @param {Function} props.onEliminar - Elimina una notificación.
 * @param {Function} props.onVer - Navega a la acción de la notificación.
 * @returns {JSX.Element} El panel de notificaciones.
 */
function NotificacionesPanel({
  notificaciones,
  cargando,
  error,
  onMarcarLeida,
  onMarcarTodas,
  onEliminar,
  onVer,
}) {
  const hayNoLeidas = notificaciones.some((aviso) => !aviso.leida);

  return (
    <div className="ec-notif-panel">
      <div className="ec-notif-panel__head">
        <h3 className="ec-notif-panel__title">Notificaciones</h3>
        <button
          type="button"
          className="ec-notif__marcar-todas"
          onClick={onMarcarTodas}
          disabled={!hayNoLeidas}
        >
          Marcar todas como leído
        </button>
      </div>

      {cargando && <p className="ec-modal__mensaje">Cargando…</p>}
      {!cargando && error && <p className="ec-form__error">{error}</p>}

      {!cargando && !error && notificaciones.length === 0 && (
        <div className="ec-notif__vacio">
          <Icon
            src="/assets/icons/notifications-icon.svg"
            className="ec-notif__vacio-icon"
          />
          <p className="ec-notif__vacio-texto">No tienes notificaciones</p>
        </div>
      )}

      {!cargando && !error && notificaciones.length > 0 && (
        <ul className="ec-notif__lista">
          {notificaciones.map((aviso) => (
            <li
              key={aviso.id_notificacion}
              className={`ec-notif-card${
                aviso.leida ? ' ec-notif-card--leida' : ''
              }`}
            >
              <div className="ec-notif-card__head">
                <h4 className="ec-notif-card__titulo">{aviso.titulo}</h4>
                <button
                  type="button"
                  className="ec-notif-card__cerrar"
                  aria-label="Eliminar notificación"
                  title="Eliminar notificación"
                  onClick={() => onEliminar(aviso.id_notificacion)}
                >
                  &times;
                </button>
              </div>
              <p className="ec-notif-card__mensaje">{aviso.mensaje}</p>
              <div className="ec-notif-card__acciones">
                <button
                  type="button"
                  className="ec-notif-card__btn"
                  onClick={() => onVer(aviso)}
                >
                  Ver
                </button>
                {aviso.leida ? (
                  <span className="ec-notif-card__leida">Leída</span>
                ) : (
                  <button
                    type="button"
                    className="ec-notif-card__btn ec-notif-card__btn--primary"
                    onClick={() => onMarcarLeida(aviso.id_notificacion)}
                  >
                    Marcar como leído
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

NotificacionesPanel.propTypes = {
  notificaciones: PropTypes.arrayOf(
    PropTypes.shape({
      id_notificacion: PropTypes.number.isRequired,
      id_camada: PropTypes.number,
      titulo: PropTypes.string.isRequired,
      mensaje: PropTypes.string.isRequired,
      leida: PropTypes.bool.isRequired,
    })
  ),
  cargando: PropTypes.bool,
  error: PropTypes.string,
  onMarcarLeida: PropTypes.func.isRequired,
  onMarcarTodas: PropTypes.func.isRequired,
  onEliminar: PropTypes.func.isRequired,
  onVer: PropTypes.func.isRequired,
};

NotificacionesPanel.defaultProps = {
  notificaciones: [],
  cargando: false,
  error: '',
};

export default NotificacionesPanel;
