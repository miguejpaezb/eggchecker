import PropTypes from 'prop-types';
import { useEffect, useRef, useState } from 'react';

import Icon from './Icon';

/**
 * Menú desplegable de acciones de una camada, anclado al botón more_vert.
 * Cierra con clic/tap fuera y con Escape.
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.camada - Camada sobre la que se actúa.
 * @param {Function} props.onEditar - Abrir la edición de la camada.
 * @param {Function} props.onMortalidad - Abrir el registro de mortalidad.
 * @param {Function} props.onAvanzarSemana - Sumar una semana de vida.
 * @param {Function} props.onSeguirActiva - Posponer la decisión una semana.
 * @param {Function} props.onDescartar - Descartar la camada.
 * @returns {JSX.Element} El menú de acciones.
 */
function CamadaAccionesMenu({
  camada,
  onEditar,
  onMortalidad,
  onAvanzarSemana,
  onSeguirActiva,
  onDescartar,
}) {
  const [abierto, setAbierto] = useState(false);
  const ref = useRef(null);
  const activa = camada.estado === 'activa';

  useEffect(() => {
    if (!abierto) {
      return undefined;
    }

    const cerrarSiFuera = (event) => {
      if (ref.current && !ref.current.contains(event.target)) {
        setAbierto(false);
      }
    };
    const cerrarConEscape = (event) => {
      if (event.key === 'Escape') {
        setAbierto(false);
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
  }, [abierto]);

  const ejecutar = (accion) => {
    setAbierto(false);
    accion();
  };

  return (
    <div className="ec-menu" ref={ref}>
      <button
        type="button"
        className="ec-camada-card__more"
        aria-haspopup="true"
        aria-expanded={abierto}
        aria-label="Acciones de la camada"
        title="Acciones de la camada"
        onClick={() => setAbierto((valor) => !valor)}
      >
        <Icon
          src="/assets/icons/more_vert-icon.svg"
          className="ec-camada-card__more-icon"
        />
      </button>

      {abierto && (
        <div className="ec-menu__lista" role="menu">
          {activa ? (
            <>
              <button
                type="button"
                role="menuitem"
                className="ec-menu__item"
                onClick={() => ejecutar(() => onEditar(camada))}
              >
                Editar camada
              </button>
              <button
                type="button"
                role="menuitem"
                className="ec-menu__item"
                onClick={() => ejecutar(() => onMortalidad(camada))}
              >
                Registrar mortalidad
              </button>
              <button
                type="button"
                role="menuitem"
                className="ec-menu__item"
                onClick={() => ejecutar(() => onAvanzarSemana(camada))}
              >
                Avanzar semana
              </button>
              {camada.requiere_decision && (
                <button
                  type="button"
                  role="menuitem"
                  className="ec-menu__item"
                  onClick={() => ejecutar(() => onSeguirActiva(camada))}
                >
                  Seguir activa
                </button>
              )}
              <button
                type="button"
                role="menuitem"
                className="ec-menu__item ec-menu__item--danger"
                onClick={() => ejecutar(() => onDescartar(camada))}
              >
                Descartar camada
              </button>
            </>
          ) : (
            <span className="ec-menu__item ec-menu__item--disabled">
              Camada retirada
            </span>
          )}
        </div>
      )}
    </div>
  );
}

CamadaAccionesMenu.propTypes = {
  camada: PropTypes.shape({
    estado: PropTypes.string.isRequired,
    requiere_decision: PropTypes.bool,
  }).isRequired,
  onEditar: PropTypes.func.isRequired,
  onMortalidad: PropTypes.func.isRequired,
  onAvanzarSemana: PropTypes.func.isRequired,
  onSeguirActiva: PropTypes.func.isRequired,
  onDescartar: PropTypes.func.isRequired,
};

export default CamadaAccionesMenu;
