import PropTypes from 'prop-types';
import { NavLink } from 'react-router-dom';

import Icon from './Icon';

const MENU = [
  {
    to: '/dashboard',
    label: 'Dashboard',
    icon: '/assets/icons/icon_dashboard.svg',
  },
  {
    to: '/produccion',
    label: 'Producción',
    icon: '/assets/icons/icon_produccion.svg',
  },
  { to: '/camadas', label: 'Camadas', icon: '/assets/icons/icon_camadas.svg' },
  {
    to: '/inventario',
    label: 'Inventario',
    icon: '/assets/icons/icon_inventario.svg',
  },
  {
    to: '/clientes',
    label: 'Clientes',
    icon: '/assets/icons/icon_clientes.svg',
  },
  {
    to: '/ventas',
    label: 'Ventas',
    icon: '/assets/icons/icon_ventas.svg',
  },
  { to: '/analisis', label: 'Análisis IA', icon: '/assets/icons/icon_ia.svg' },
  {
    to: '/reportes',
    label: 'Reportes',
    icon: '/assets/icons/reports-icon.svg',
  },
  { to: '/perfil', label: 'Perfil', icon: '/assets/icons/icon_usuario.svg' },
];

function Sidebar({ perfil, abierto, onCerrar, onLogout }) {
  const nombre = perfil?.nombre_completo ?? 'Invitado';
  const plan = perfil?.plan_suscripcion ?? 'gratuito';
  const planTexto = `Plan ${plan.charAt(0).toUpperCase()}${plan.slice(1)}`;

  return (
    <aside
      className={`ec-sidebar${abierto ? ' ec-sidebar--abierto' : ''}`}
      aria-label="Navegación principal"
    >
      <nav className="ec-sidebar__nav">
        <ul className="ec-sidebar__list">
          {MENU.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                className={({ isActive }) =>
                  `ec-sidebar__link${
                    isActive ? ' ec-sidebar__link--active' : ''
                  }`
                }
                onClick={onCerrar}
              >
                <Icon src={item.icon} className="ec-sidebar__icon" />
                <span className="ec-sidebar__label">{item.label}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
      <div className="ec-sidebar__footer">
        <div className="ec-sidebar__user">
          <div className="ec-sidebar__avatar">
            <Icon
              src="/assets/icons/icon_usuario.svg"
              className="ec-sidebar__avatar-icon"
            />
          </div>
          <div className="ec-sidebar__info">
            <p className="ec-sidebar__name">{nombre}</p>
            <p className="ec-sidebar__plan">{planTexto}</p>
          </div>
          <button
            type="button"
            className="ec-sidebar__logout"
            onClick={onLogout}
            aria-label="Cerrar sesión"
            title="Cerrar sesión"
          >
            <Icon
              src="/assets/icons/icon_logout.svg"
              className="ec-sidebar__logout-icon"
            />
          </button>
        </div>
      </div>
    </aside>
  );
}

Sidebar.propTypes = {
  perfil: PropTypes.shape({
    nombre_completo: PropTypes.string,
    plan_suscripcion: PropTypes.string,
  }),
  abierto: PropTypes.bool,
  onCerrar: PropTypes.func.isRequired,
  onLogout: PropTypes.func.isRequired,
};

Sidebar.defaultProps = {
  perfil: null,
  abierto: false,
};

export default Sidebar;
