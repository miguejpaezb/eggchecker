import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

import Icon from './Icon';

function Topbar({ perfil, menuAbierto, onAbrirMenu, onLogout }) {
  const [menuActivo, setMenuActivo] = useState(null);
  const perfilRef = useRef(null);
  const notifRef = useRef(null);

  useEffect(() => {
    if (!menuActivo) {
      return undefined;
    }

    const cerrarSiFuera = (event) => {
      const enPerfil = perfilRef.current?.contains(event.target);
      const enNotif = notifRef.current?.contains(event.target);
      if (!enPerfil && !enNotif) {
        setMenuActivo(null);
      }
    };
    const cerrarConEscape = (event) => {
      if (event.key === 'Escape') {
        setMenuActivo(null);
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
  }, [menuActivo]);

  const alternar = (nombre) =>
    setMenuActivo((prev) => (prev === nombre ? null : nombre));

  const nombre = perfil?.nombre_completo ?? 'Invitado';
  const plan = perfil?.plan_suscripcion ?? 'gratuito';
  const planTexto = `Plan ${plan.charAt(0).toUpperCase()}${plan.slice(1)}`;

  return (
    <header className="ec-topbar">
      <div className="ec-topbar__left">
        <button
          type="button"
          className="ec-topbar__menu-btn"
          aria-label="Abrir menú"
          aria-expanded={menuAbierto}
          onClick={onAbrirMenu}
        >
          <svg
            className="ec-topbar__menu-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            aria-hidden="true"
          >
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>
        <div className="ec-topbar__brand">
          <img
            src="/assets/img/logo-eggchecker.png"
            alt="isotipo EggChecker"
            className="ec-topbar__logo"
          />
          <h2 className="ec-topbar__title">EggChecker</h2>
        </div>
      </div>
      <div className="ec-topbar__actions">
        <div className="ec-topbar__notif" ref={notifRef}>
          <button
            type="button"
            className="ec-topbar__icon-btn"
            aria-label="Notificaciones"
            aria-haspopup="true"
            aria-expanded={menuActivo === 'notificaciones'}
            onClick={() => alternar('notificaciones')}
          >
            <img
              src="/assets/icons/notifications-icon.svg"
              alt=""
              aria-hidden="true"
              className="ec-topbar__icon"
            />
          </button>
          {menuActivo === 'notificaciones' && (
            <div className="ec-notif-menu">
              <h3 className="ec-notif-menu__title">Notificaciones</h3>
              <div className="ec-notif-menu__empty">
                <Icon
                  src="/assets/icons/notifications-icon.svg"
                  className="ec-notif-menu__empty-icon"
                />
                <p className="ec-notif-menu__empty-text">
                  No tienes notificaciones
                </p>
              </div>
            </div>
          )}
        </div>
        <div className="ec-topbar__profile" ref={perfilRef}>
          <button
            type="button"
            className="ec-topbar__avatar-btn"
            aria-label="Perfil de usuario"
            aria-haspopup="true"
            aria-expanded={menuActivo === 'perfil'}
            onClick={() => alternar('perfil')}
          >
            <span className="ec-topbar__avatar">
              <Icon
                src="/assets/icons/user-icon.svg"
                className="ec-topbar__avatar-icon"
              />
            </span>
          </button>
          {menuActivo === 'perfil' && (
            <div className="ec-profile-menu">
              <div className="ec-profile-menu__avatar">
                <Icon
                  src="/assets/icons/user-icon.svg"
                  className="ec-profile-menu__avatar-icon"
                />
              </div>
              <p className="ec-profile-menu__name">{nombre}</p>
              <p className="ec-profile-menu__plan">{planTexto}</p>
              <button
                type="button"
                className="ec-profile-menu__logout"
                onClick={onLogout}
              >
                <Icon
                  src="/assets/icons/logout-icon.svg"
                  className="ec-profile-menu__logout-icon"
                />
                Cerrar Sesión
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

Topbar.propTypes = {
  perfil: PropTypes.shape({
    nombre_completo: PropTypes.string,
    plan_suscripcion: PropTypes.string,
  }),
  menuAbierto: PropTypes.bool,
  onAbrirMenu: PropTypes.func.isRequired,
  onLogout: PropTypes.func.isRequired,
};

Topbar.defaultProps = {
  perfil: null,
  menuAbierto: false,
};

export default Topbar;
