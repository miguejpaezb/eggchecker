import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import Switch from './Switch';

// Alertas configurables del avicultor (el envío real aún no existe).
const OPCIONES = [
  {
    clave: 'notif_produccion_baja',
    titulo: 'Alertas de producción baja',
    descripcion:
      'Te avisa si la recolección de huevos cae por debajo del promedio.',
  },
  {
    clave: 'notif_stock_bajo',
    titulo: 'Nivel bajo de alimento',
    descripcion: 'Aviso preventivo para reabastecer el inventario de cuido.',
  },
  {
    clave: 'notif_vacunacion',
    titulo: 'Recordatorio de Vacunación',
    descripcion: 'Recibe un recordatorio 3 días antes de cada plan sanitario.',
  },
  {
    clave: 'notif_resumen_semanal',
    titulo: 'Resumen Semanal',
    descripcion:
      'Un reporte general de la granja enviado a tu correo todos los domingos.',
  },
];

const PREFERENCIAS_VACIAS = {
  notif_produccion_baja: true,
  notif_stock_bajo: true,
  notif_vacunacion: false,
  notif_resumen_semanal: true,
};

/**
 * Extrae las preferencias de notificaciones del perfil.
 * @param {Object|null} perfil - Perfil del usuario.
 * @returns {Object} Preferencias listas para el formulario.
 */
function extraerPreferencias(perfil) {
  if (!perfil) {
    return { ...PREFERENCIAS_VACIAS };
  }
  return {
    notif_produccion_baja: Boolean(perfil.notif_produccion_baja),
    notif_stock_bajo: Boolean(perfil.notif_stock_bajo),
    notif_vacunacion: Boolean(perfil.notif_vacunacion),
    notif_resumen_semanal: Boolean(perfil.notif_resumen_semanal),
  };
}

/**
 * Sección de preferencias de alertas por correo.
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.perfil - Perfil del usuario.
 * @param {Function} props.onGuardar - Guarda las preferencias.
 * @returns {JSX.Element} El formulario de notificaciones.
 */
function NotificacionesSeccion({ perfil, onGuardar }) {
  const [prefs, setPrefs] = useState(() => extraerPreferencias(perfil));
  const [error, setError] = useState('');
  const [exito, setExito] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    setPrefs(extraerPreferencias(perfil));
  }, [perfil]);

  const alternar = (clave, valor) =>
    setPrefs((previas) => ({ ...previas, [clave]: valor }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setExito('');
    setEnviando(true);
    try {
      await onGuardar(prefs);
      setExito('Preferencias guardadas correctamente');
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <form className="ec-perfil__form" onSubmit={handleSubmit}>
      <section className="ec-perfil__bloque">
        <h2 className="ec-perfil__seccion-titulo">Preferencias de Alertas</h2>
        <p className="ec-perfil__seccion-desc">
          Activa o desactiva los avisos que quieres recibir.
        </p>

        {error && <p className="ec-form__error">{error}</p>}
        {exito && <p className="ec-perfil__exito">{exito}</p>}

        <div className="ec-perfil__alertas">
          {OPCIONES.map((opcion) => (
            <article key={opcion.clave} className="ec-perfil__alerta">
              <div className="ec-perfil__alerta-texto">
                <h3 className="ec-perfil__alerta-titulo">{opcion.titulo}</h3>
                <p className="ec-perfil__alerta-desc">{opcion.descripcion}</p>
              </div>
              <div className="ec-perfil__alerta-switch">
                <Switch
                  id={`switch-${opcion.clave}`}
                  label={opcion.titulo}
                  checked={prefs[opcion.clave]}
                  onChange={(valor) => alternar(opcion.clave, valor)}
                  disabled={enviando}
                />
              </div>
            </article>
          ))}
        </div>
      </section>

      <div className="ec-form__actions">
        <button
          type="button"
          className="ec-btn ec-btn--ghost"
          onClick={() => {
            setPrefs(extraerPreferencias(perfil));
            setError('');
            setExito('');
          }}
          disabled={enviando}
        >
          Cancelar
        </button>
        <button
          type="submit"
          className="ec-btn ec-btn--primary"
          disabled={enviando}
        >
          {enviando ? 'Guardando…' : 'Guardar Cambios'}
        </button>
      </div>
    </form>
  );
}

NotificacionesSeccion.propTypes = {
  perfil: PropTypes.shape({
    notif_produccion_baja: PropTypes.bool,
    notif_stock_bajo: PropTypes.bool,
    notif_vacunacion: PropTypes.bool,
    notif_resumen_semanal: PropTypes.bool,
  }),
  onGuardar: PropTypes.func.isRequired,
};

NotificacionesSeccion.defaultProps = {
  perfil: null,
};

export default NotificacionesSeccion;
