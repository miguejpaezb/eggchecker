import PropTypes from 'prop-types';

import Icon from '../Icon';

// Secciones del perfil cargadas en la columna derecha.
const SECCIONES = [
  {
    id: 'editar',
    label: 'Editar Perfil',
    icon: '/assets/icons/icon_usuario.svg',
  },
  {
    id: 'notificaciones',
    label: 'Notificaciones',
    icon: '/assets/icons/icon_notification.svg',
  },
  {
    id: 'seguridad',
    label: 'Seguridad',
    icon: '/assets/icons/icon_password.svg',
  },
  {
    id: 'plan',
    label: 'Plan y Suscripción',
    icon: '/assets/icons/icon_loyalty.svg',
  },
  {
    id: 'ayuda',
    label: 'Ayuda y Soporte',
    icon: '/assets/icons/icon_help.svg',
  },
];

/**
 * Submenú de configuración del perfil.
 * @param {Object} props - Propiedades del componente.
 * @param {string} props.activa - Id de la sección activa.
 * @param {Function} props.onSeleccionar - Acción al elegir una sección.
 * @returns {JSX.Element} El submenú de configuración.
 */
function PerfilMenu({ activa, onSeleccionar }) {
  return (
    <section className="ec-glass-card ec-perfil__card">
      <h3 className="ec-perfil__card-titulo">Configuración</h3>
      <ul className="ec-perfil__menu">
        {SECCIONES.map((seccion) => {
          const esActiva = seccion.id === activa;
          return (
            <li key={seccion.id}>
              <button
                type="button"
                className={`ec-perfil__menu-item${esActiva ? ' ec-perfil__menu-item--active' : ''
                  }`}
                onClick={() => onSeleccionar(seccion.id)}
                aria-current={esActiva ? 'true' : undefined}
              >
                <Icon src={seccion.icon} className="ec-perfil__menu-icon" />
                <span className="ec-perfil__menu-label">{seccion.label}</span>
                <Icon
                  src="/assets/icons/keyboard_arrow_right-icon.svg"
                  className={`ec-perfil__menu-arrow${esActiva ? ' ec-perfil__menu-arrow--active' : ''
                    }`}
                />
              </button>
            </li>
          );
        })}
      </ul>
    </section>
  );
}

PerfilMenu.propTypes = {
  activa: PropTypes.string.isRequired,
  onSeleccionar: PropTypes.func.isRequired,
};

export default PerfilMenu;
