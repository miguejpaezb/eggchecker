import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import { DIAS_SEMANA } from '../../utils/recordatorios';
import Switch from './Switch';

// Frecuencias del recordatorio de reportes.
const FRECUENCIAS = [
  { valor: 'semanal', label: 'Semanal' },
  { valor: 'quincenal', label: 'Quincenal' },
  { valor: 'mensual', label: 'Mensual' },
];

const PREFERENCIAS_VACIAS = {
  notif_produccion_baja: true,
  notif_camada_limite: true,
  notif_stock_bajo: true,
  notif_vacunacion: false,
  notif_resumen_semanal: true,
  notif_produccion_hora: '05:00',
  notif_resumen_frecuencia: 'semanal',
  notif_resumen_dia: 0,
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
    notif_camada_limite: Boolean(perfil.notif_camada_limite ?? true),
    notif_stock_bajo: Boolean(perfil.notif_stock_bajo),
    notif_vacunacion: Boolean(perfil.notif_vacunacion),
    notif_resumen_semanal: Boolean(perfil.notif_resumen_semanal),
    notif_produccion_hora: (perfil.notif_produccion_hora || '05:00:00').slice(
      0,
      5
    ),
    notif_resumen_frecuencia: perfil.notif_resumen_frecuencia || 'semanal',
    notif_resumen_dia: Number(perfil.notif_resumen_dia ?? 0),
  };
}

/**
 * Fila de una alerta con su interruptor.
 * @param {Object} props - Propiedades de la fila.
 * @returns {JSX.Element} La fila de la alerta.
 */
function FilaAlerta({
  id,
  titulo,
  descripcion,
  activo,
  deshabilitado,
  onCambiar,
}) {
  return (
    <article className="ec-perfil__alerta">
      <div className="ec-perfil__alerta-texto">
        <h3 className="ec-perfil__alerta-titulo">{titulo}</h3>
        <p className="ec-perfil__alerta-desc">{descripcion}</p>
      </div>
      <div className="ec-perfil__alerta-switch">
        <Switch
          id={id}
          label={titulo}
          checked={activo}
          onChange={onCambiar}
          disabled={deshabilitado}
        />
      </div>
    </article>
  );
}

/**
 * Sección de preferencias de alertas y ajustes del resumen.
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
      await onGuardar({
        ...prefs,
        notif_resumen_dia: Number(prefs.notif_resumen_dia),
      });
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
          <FilaAlerta
            id="switch-notif-produccion"
            titulo="Producción baja"
            descripcion="Avisa si la recolección de ayer quedó por debajo del 85% de las aves activas."
            activo={prefs.notif_produccion_baja}
            deshabilitado={enviando}
            onCambiar={(valor) => alternar('notif_produccion_baja', valor)}
          />

          <div className="ec-perfil__opcion-extra">
            <label htmlFor="perfil-hora-analisis">
              Hora del análisis
              <input
                id="perfil-hora-analisis"
                className="ec-perfil__hora"
                type="time"
                value={prefs.notif_produccion_hora}
                disabled={!prefs.notif_produccion_baja || enviando}
                onChange={(event) =>
                  alternar('notif_produccion_hora', event.target.value)
                }
              />
            </label>
          </div>

          <FilaAlerta
            id="switch-notif-camada"
            titulo="Camada al límite"
            descripcion="Avisa cuando una camada supera las 72 semanas de vida productiva."
            activo={prefs.notif_camada_limite}
            deshabilitado={enviando}
            onCambiar={(valor) => alternar('notif_camada_limite', valor)}
          />

          <FilaAlerta
            id="switch-notif-stock"
            titulo="Nivel bajo de alimento"
            descripcion="Aviso preventivo para reabastecer el inventario de cuido."
            activo={prefs.notif_stock_bajo}
            deshabilitado={enviando}
            onCambiar={(valor) => alternar('notif_stock_bajo', valor)}
          />

          <FilaAlerta
            id="switch-notif-vacunacion"
            titulo="Recordatorios sanitarios"
            descripcion="Vacunas y vitaminas: avisa el día antes y el mismo día de cada aplicación."
            activo={prefs.notif_vacunacion}
            deshabilitado={enviando}
            onCambiar={(valor) => alternar('notif_vacunacion', valor)}
          />

          <FilaAlerta
            id="switch-notif-resumen"
            titulo="Resumen de reportes"
            descripcion="Un recordatorio para revisar el reporte del período."
            activo={prefs.notif_resumen_semanal}
            deshabilitado={enviando}
            onCambiar={(valor) => alternar('notif_resumen_semanal', valor)}
          />

          {prefs.notif_resumen_semanal && (
            <div className="ec-perfil__resumen-opciones">
              <label
                className="ec-perfil__select-campo"
                htmlFor="perfil-frecuencia"
              >
                Frecuencia
                <select
                  id="perfil-frecuencia"
                  className="ec-perfil__select"
                  value={prefs.notif_resumen_frecuencia}
                  disabled={enviando}
                  onChange={(event) =>
                    alternar('notif_resumen_frecuencia', event.target.value)
                  }
                >
                  {FRECUENCIAS.map((opcion) => (
                    <option key={opcion.valor} value={opcion.valor}>
                      {opcion.label}
                    </option>
                  ))}
                </select>
              </label>

              <label className="ec-perfil__select-campo" htmlFor="perfil-dia">
                Día
                <select
                  id="perfil-dia"
                  className="ec-perfil__select"
                  value={prefs.notif_resumen_dia}
                  disabled={enviando}
                  onChange={(event) =>
                    alternar('notif_resumen_dia', Number(event.target.value))
                  }
                >
                  {DIAS_SEMANA.map((dia) => (
                    <option key={dia.valor} value={dia.valor}>
                      {dia.largo}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          )}
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

FilaAlerta.propTypes = {
  id: PropTypes.string.isRequired,
  titulo: PropTypes.string.isRequired,
  descripcion: PropTypes.string.isRequired,
  activo: PropTypes.bool.isRequired,
  deshabilitado: PropTypes.bool,
  onCambiar: PropTypes.func.isRequired,
};

FilaAlerta.defaultProps = {
  deshabilitado: false,
};

NotificacionesSeccion.propTypes = {
  perfil: PropTypes.shape({
    notif_produccion_baja: PropTypes.bool,
    notif_camada_limite: PropTypes.bool,
    notif_stock_bajo: PropTypes.bool,
    notif_vacunacion: PropTypes.bool,
    notif_resumen_semanal: PropTypes.bool,
    notif_produccion_hora: PropTypes.string,
    notif_resumen_frecuencia: PropTypes.string,
    notif_resumen_dia: PropTypes.number,
  }),
  onGuardar: PropTypes.func.isRequired,
};

NotificacionesSeccion.defaultProps = {
  perfil: null,
};

export default NotificacionesSeccion;
