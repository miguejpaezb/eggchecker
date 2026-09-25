import PropTypes from 'prop-types';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import {
  eliminarNotificacion,
  listarNotificaciones,
  marcarLeida,
  marcarTodasLeidas,
} from '../services/notificacionService';
import Icon from './Icon';
import NotificacionesPanel from './NotificacionesPanel';

const INTERVALO_NOTIFICACIONES = 60000;

function Topbar({ perfil, menuAbierto, onAbrirMenu, onLogout }) {
  const navigate = useNavigate();
  const [menuActivo, setMenuActivo] = useState(null);
  const [notificaciones, setNotificaciones] = useState([]);
  const [cargandoNotif, setCargandoNotif] = useState(false);
  const [errorNotif, setErrorNotif] = useState('');
  const perfilRef = useRef(null);
  const notifRef = useRef(null);

  const cargarNotificaciones = useCallback(async () => {
    try {
      setNotificaciones(await listarNotificaciones());
    } catch {
      setNotificaciones([]);
    }
  }, []);

  useEffect(() => {
    cargarNotificaciones();
    const intervalo = setInterval(
      cargarNotificaciones,
      INTERVALO_NOTIFICACIONES
    );
    return () => clearInterval(intervalo);
  }, [cargarNotificaciones]);

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

  const alternarPerfil = () =>
    setMenuActivo((prev) => (prev === 'perfil' ? null : 'perfil'));

  const manejarNotificaciones = async () => {
    const abrir = menuActivo !== 'notificaciones';
    setMenuActivo(abrir ? 'notificaciones' : null);
    if (!abrir) {
      return;
    }
    setErrorNotif('');
    setCargandoNotif(true);
    try {
      await cargarNotificaciones();
    } finally {
      setCargandoNotif(false);
    }
  };

  const manejarMarcarLeida = async (idNotificacion) => {
    setErrorNotif('');
    try {
      await marcarLeida(idNotificacion);
      await cargarNotificaciones();
    } catch (err) {
      setErrorNotif(err.message);
    }
  };

  const manejarMarcarTodas = async () => {
    setErrorNotif('');
    try {
      await marcarTodasLeidas();
      await cargarNotificaciones();
    } catch (err) {
      setErrorNotif(err.message);
    }
  };

  const manejarEliminar = async (idNotificacion) => {
    setErrorNotif('');
    try {
      await eliminarNotificacion(idNotificacion);
      await cargarNotificaciones();
    } catch (err) {
      setErrorNotif(err.message);
    }
  };

  const manejarVer = (aviso) => {
    setMenuActivo(null);
    navigate('/camadas', { state: { camadaId: aviso.id_camada } });
  };

  const nombre = perfil?.nombre_completo ?? 'Invitado';
  const plan = perfil?.plan_suscripcion ?? 'gratuito';
  const planTexto = `Plan ${plan.charAt(0).toUpperCase()}${plan.slice(1)}`;
  const hayNoLeidas = notificaciones.some((aviso) => !aviso.leida);

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
            onClick={manejarNotificaciones}
          >
            <img
              src="/assets/icons/notifications-icon.svg"
              alt=""
              aria-hidden="true"
              className="ec-topbar__icon"
            />
            {hayNoLeidas && <span className="ec-topbar__dot" />}
          </button>
          {menuActivo === 'notificaciones' && (
            <NotificacionesPanel
              notificaciones={notificaciones}
              cargando={cargandoNotif}
              error={errorNotif}
              onMarcarLeida={manejarMarcarLeida}
              onMarcarTodas={manejarMarcarTodas}
              onEliminar={manejarEliminar}
              onVer={manejarVer}
            />
          )}
        </div>
        <div className="ec-topbar__profile" ref={perfilRef}>
          <button
            type="button"
            className="ec-topbar__avatar-btn"
            aria-label="Perfil de usuario"
            aria-haspopup="true"
            aria-expanded={menuActivo === 'perfil'}
            onClick={alternarPerfil}
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
                  src="/assets/icons/icon_logout.svg"
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
