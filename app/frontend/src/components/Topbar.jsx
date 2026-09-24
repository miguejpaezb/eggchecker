import PropTypes from 'prop-types';
import { useCallback, useEffect, useRef, useState } from 'react';

import {
  descartarCamada,
  listarAlertas,
  seguirActiva,
} from '../services/camadaService';
import ConfirmDialog from './ConfirmDialog';
import Icon from './Icon';

const INTERVALO_ALERTAS = 60000;

function Topbar({ perfil, menuAbierto, onAbrirMenu, onLogout }) {
  const [menuActivo, setMenuActivo] = useState(null);
  const [alertas, setAlertas] = useState([]);
  const [errorAlerta, setErrorAlerta] = useState('');
  const [alertaADescartar, setAlertaADescartar] = useState(null);
  const [descartando, setDescartando] = useState(false);
  const [errorDescartar, setErrorDescartar] = useState('');
  const perfilRef = useRef(null);
  const notifRef = useRef(null);

  const cargarAlertas = useCallback(async () => {
    try {
      setAlertas(await listarAlertas());
    } catch {
      setAlertas([]);
    }
  }, []);

  useEffect(() => {
    cargarAlertas();
    const intervalo = setInterval(cargarAlertas, INTERVALO_ALERTAS);
    return () => clearInterval(intervalo);
  }, [cargarAlertas]);

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

  const manejarNotificaciones = () => {
    const abrir = menuActivo !== 'notificaciones';
    setMenuActivo(abrir ? 'notificaciones' : null);
    if (abrir) {
      setErrorAlerta('');
      cargarAlertas();
    }
  };

  const manejarSeguirActiva = async (camada) => {
    setErrorAlerta('');
    try {
      await seguirActiva(camada.id_camada);
      await cargarAlertas();
    } catch (err) {
      setErrorAlerta(err.message);
    }
  };

  const confirmarDescartar = async () => {
    setErrorDescartar('');
    setDescartando(true);
    try {
      await descartarCamada(alertaADescartar.id_camada);
      setAlertaADescartar(null);
      await cargarAlertas();
    } catch (err) {
      setErrorDescartar(err.message);
    } finally {
      setDescartando(false);
    }
  };

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
            onClick={manejarNotificaciones}
          >
            <img
              src="/assets/icons/notifications-icon.svg"
              alt=""
              aria-hidden="true"
              className="ec-topbar__icon"
            />
            {alertas.length > 0 && (
              <span className="ec-topbar__badge">{alertas.length}</span>
            )}
          </button>
          {menuActivo === 'notificaciones' && (
            <div className="ec-notif-menu">
              <h3 className="ec-notif-menu__title">Notificaciones</h3>
              {errorAlerta && <p className="ec-form__error">{errorAlerta}</p>}
              {alertas.length === 0 ? (
                <div className="ec-notif-menu__empty">
                  <Icon
                    src="/assets/icons/notifications-icon.svg"
                    className="ec-notif-menu__empty-icon"
                  />
                  <p className="ec-notif-menu__empty-text">
                    No tienes notificaciones
                  </p>
                </div>
              ) : (
                <ul className="ec-notif-menu__lista">
                  {alertas.map((camada) => (
                    <li key={camada.id_camada} className="ec-notif-item">
                      <p className="ec-notif-item__texto">
                        <strong>{camada.nombre_camada}</strong> cumplió 72
                        semanas. ¿Descartar o seguir activa?
                      </p>
                      <div className="ec-notif-item__acciones">
                        <button
                          type="button"
                          className="ec-notif-item__btn"
                          onClick={() => manejarSeguirActiva(camada)}
                        >
                          Seguir activa
                        </button>
                        <button
                          type="button"
                          className="ec-notif-item__btn ec-notif-item__btn--danger"
                          onClick={() => {
                            setErrorDescartar('');
                            setAlertaADescartar(camada);
                          }}
                        >
                          Descartar
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
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
                  src="/assets/icons/icon_logout.svg"
                  className="ec-profile-menu__logout-icon"
                />
                Cerrar Sesión
              </button>
            </div>
          )}
        </div>
      </div>

      <ConfirmDialog
        abierto={alertaADescartar !== null}
        titulo="Descartar camada"
        mensaje={
          alertaADescartar
            ? `¿Descartar "${alertaADescartar.nombre_camada}"? Esta acción no se puede revertir.`
            : ''
        }
        textoConfirmar="Descartar"
        peligro
        cargando={descartando}
        error={errorDescartar}
        onConfirmar={confirmarDescartar}
        onCerrar={() => setAlertaADescartar(null)}
      />
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
