import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

import Icon from './Icon';

function Topbar({ perfil, menuAbierto, onAbrirMenu, onLogout }) {
  const [perfilAbierto, setPerfilAbierto] = useState(false);
  const perfilRef = useRef(null);

  useEffect(() => {
    if (!perfilAbierto) {
      return undefined;
    }

    const cerrarSiFuera = (event) => {
      if (perfilRef.current && !perfilRef.current.contains(event.target)) {
        setPerfilAbierto(false);
      }
    };
    const cerrarConEscape = (event) => {
      if (event.key === 'Escape') {
        setPerfilAbierto(false);
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
  }, [perfilAbierto]);

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
        <button
          type="button"
          className="ec-topbar__icon-btn"
          aria-label="Notificaciones"
        >
          <img
            src="/assets/icons/notifications-icon.svg"
            alt=""
            aria-hidden="true"
            className="ec-topbar__icon"
          />
        </button>
        <div className="ec-topbar__profile" ref={perfilRef}>
          <button
            type="button"
            className="ec-topbar__avatar-btn"
            aria-label="Perfil de usuario"
            aria-haspopup="true"
            aria-expanded={perfilAbierto}
            onClick={() => setPerfilAbierto((prev) => !prev)}
          >
            <span className="ec-topbar__avatar">
              <Icon
                src="/assets/icons/user-icon.svg"
                className="ec-topbar__avatar-icon"
              />
            </span>
          </button>
          {perfilAbierto && (
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
