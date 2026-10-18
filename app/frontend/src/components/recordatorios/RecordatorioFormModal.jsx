import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';

import { listarCategorias } from '../../services/categoriaService';
import { listarInsumos } from '../../services/insumoService';
import {
  CATEGORIAS_SANITARIAS,
  DIAS_SEMANA,
  MAXIMO_FECHAS,
  MODO_CADA_DIAS,
  MODO_DIAS_SEMANA,
  MODO_FECHAS,
  MODOS,
  formatoCantidad,
  hoyIso,
  textoCantidad,
} from '../../utils/recordatorios';
import Modal from '../Modal';

let secuenciaFechas = 0;

/** Crea una fila de fecha con un id estable para la lista. */
function nuevaFilaFecha(fecha) {
  secuenciaFechas += 1;
  return { id: secuenciaFechas, fecha: fecha || hoyIso() };
}

/**
 * Modal con el formulario para crear o editar un recordatorio sanitario.
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} props.abierto - Si el modal debe mostrarse.
 * @param {Object} props.camada - Camada desde cuyo menú se abre.
 * @param {Object|null} props.recordatorio - Regla a editar; null para crear.
 * @param {Function} props.onCerrar - Acción al cerrar.
 * @param {Function} props.onGuardar - Guarda la regla; recibe los datos.
 * @returns {JSX.Element} El formulario del recordatorio.
 */
function RecordatorioFormModal({
  abierto,
  camada,
  recordatorio,
  onCerrar,
  onGuardar,
}) {
  const [insumos, setInsumos] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [alcance, setAlcance] = useState('camada');
  const [categoria, setCategoria] = useState('medicamento');
  const [idInsumo, setIdInsumo] = useState('');
  const [cantidad, setCantidad] = useState('5');
  const [modo, setModo] = useState(MODO_CADA_DIAS);
  const [intervalo, setIntervalo] = useState('15');
  const [dias, setDias] = useState([]);
  const [fechas, setFechas] = useState([]);
  const [fechaInicio, setFechaInicio] = useState(hoyIso());
  const [fechaFin, setFechaFin] = useState('');
  const [activo, setActivo] = useState(true);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!abierto) {
      return undefined;
    }

    let activoCatalogos = true;
    Promise.all([listarInsumos(), listarCategorias()])
      .then(([listaInsumos, listaCategorias]) => {
        if (activoCatalogos) {
          setInsumos(listaInsumos);
          setCategorias(listaCategorias);
        }
      })
      .catch((err) => {
        if (activoCatalogos) {
          setError(err.message);
        }
      });

    return () => {
      activoCatalogos = false;
    };
  }, [abierto]);

  useEffect(() => {
    if (!abierto) {
      return;
    }

    setError('');
    setEnviando(false);

    if (!recordatorio) {
      setAlcance('camada');
      setCategoria('medicamento');
      setIdInsumo('');
      setCantidad('5');
      setModo(MODO_CADA_DIAS);
      setIntervalo('15');
      setDias([]);
      setFechas([]);
      setFechaInicio(hoyIso());
      setFechaFin('');
      setActivo(true);
      return;
    }

    setAlcance(recordatorio.id_camada ? 'camada' : 'global');
    setIdInsumo(String(recordatorio.id_insumo));
    setCantidad(formatoCantidad(recordatorio.cantidad));
    setModo(recordatorio.modo);
    setIntervalo(String(recordatorio.intervalo_dias || 15));
    setDias(recordatorio.dias_semana || []);
    setFechas(
      (recordatorio.fechas || []).map((fecha) => nuevaFilaFecha(fecha))
    );
    setFechaInicio(recordatorio.fecha_inicio);
    setFechaFin(recordatorio.fecha_fin || '');
    setActivo(recordatorio.activo);
  }, [abierto, recordatorio]);

  useEffect(() => {
    if (!recordatorio || insumos.length === 0 || categorias.length === 0) {
      return;
    }
    const insumo = insumos.find(
      (item) => item.id_insumo === recordatorio.id_insumo
    );
    const encontrada = categorias.find(
      (item) => item.id_categoria === insumo?.id_categoria
    );
    if (encontrada) {
      setCategoria(encontrada.nombre_categ);
    }
  }, [recordatorio, insumos, categorias]);

  const categoriaElegida = categorias.find(
    (item) => item.nombre_categ === categoria
  );
  const insumosFiltrados = insumos.filter(
    (insumo) =>
      insumo.id_categoria === categoriaElegida?.id_categoria &&
      insumo.activo &&
      !insumo.descontinuado
  );
  const insumoElegido = insumos.find(
    (item) => String(item.id_insumo) === String(idInsumo)
  );

  const alternarDia = (dia) => {
    setDias((previos) =>
      previos.includes(dia)
        ? previos.filter((item) => item !== dia)
        : [...previos, dia].sort((a, b) => a - b)
    );
    setError('');
  };

  const cambiarFecha = (id, valor) => {
    setFechas((previas) =>
      previas.map((item) => (item.id === id ? { ...item, fecha: valor } : item))
    );
    setError('');
  };

  const agregarFecha = () => {
    if (fechas.length >= MAXIMO_FECHAS) {
      return;
    }
    setFechas((previas) => [...previas, nuevaFilaFecha()]);
  };

  const quitarFecha = (id) => {
    setFechas((previas) => previas.filter((item) => item.id !== id));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    const numeroCantidad = Number(cantidad);
    const numeroIntervalo = Number(intervalo);

    if (!insumoElegido) {
      setError('Elige el medicamento o la vitamina.');
      return;
    }
    if (!Number.isFinite(numeroCantidad) || numeroCantidad <= 0) {
      setError('La dosis debe ser mayor que cero.');
      return;
    }
    if (
      modo === MODO_CADA_DIAS &&
      (!Number.isInteger(numeroIntervalo) ||
        numeroIntervalo < 1 ||
        numeroIntervalo > 365)
    ) {
      setError('El intervalo debe estar entre 1 y 365 días.');
      return;
    }
    if (modo === MODO_DIAS_SEMANA && dias.length === 0) {
      setError('Elige al menos un día de la semana.');
      return;
    }
    if (modo === MODO_FECHAS && fechas.length === 0) {
      setError('Agrega al menos una fecha.');
      return;
    }
    if (modo !== MODO_FECHAS && fechaFin && fechaFin < fechaInicio) {
      setError('La fecha de fin no puede ser anterior al inicio.');
      return;
    }

    const datos = {
      id_camada: alcance === 'global' ? null : (camada?.id_camada ?? null),
      id_insumo: insumoElegido.id_insumo,
      modo,
      intervalo_dias: modo === MODO_CADA_DIAS ? numeroIntervalo : null,
      dias_semana: modo === MODO_DIAS_SEMANA ? dias : null,
      fechas:
        modo === MODO_FECHAS ? fechas.map((item) => item.fecha).sort() : null,
      fecha_inicio: modo === MODO_FECHAS ? null : fechaInicio,
      fecha_fin: modo === MODO_FECHAS || !fechaFin ? null : fechaFin,
      cantidad: numeroCantidad,
      activo,
    };

    setEnviando(true);
    try {
      await onGuardar(datos);
      onCerrar();
    } catch (err) {
      setError(err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal
      abierto={abierto}
      titulo={recordatorio ? 'Editar recordatorio' : 'Nuevo recordatorio'}
      onCerrar={onCerrar}
    >
      <form className="ec-form" onSubmit={handleSubmit}>
        {error && <p className="ec-form__error">{error}</p>}

        <div className="ec-rec-form__grid">
          <label className="ec-form__field" htmlFor="rec-alcance">
            <span className="ec-form__label">Alcance</span>
            <select
              id="rec-alcance"
              className="ec-form__input"
              value={alcance}
              onChange={(event) => setAlcance(event.target.value)}
            >
              <option value="camada">
                {camada?.nombre_camada || 'Esta camada'}
              </option>
              <option value="global">Todas las camadas</option>
            </select>
          </label>

          <label className="ec-form__field" htmlFor="rec-categoria">
            <span className="ec-form__label">Tipo de insumo</span>
            <select
              id="rec-categoria"
              className="ec-form__input"
              value={categoria}
              onChange={(event) => {
                setCategoria(event.target.value);
                setIdInsumo('');
                setError('');
              }}
            >
              {CATEGORIAS_SANITARIAS.map((opcion) => (
                <option key={opcion.valor} value={opcion.valor}>
                  {opcion.label}
                </option>
              ))}
            </select>
          </label>
        </div>

        <label className="ec-form__field" htmlFor="rec-insumo">
          <span className="ec-form__label">Insumo</span>
          <select
            id="rec-insumo"
            className="ec-form__input"
            value={idInsumo}
            onChange={(event) => {
              setIdInsumo(event.target.value);
              setError('');
            }}
          >
            <option value="">Elegir insumo</option>
            {insumosFiltrados.map((insumo) => (
              <option key={insumo.id_insumo} value={insumo.id_insumo}>
                {insumo.nombre_insumo} · {insumo.unidad_medida} · stock{' '}
                {formatoCantidad(insumo.stock_actual)}
              </option>
            ))}
          </select>
          {insumosFiltrados.length === 0 && (
            <span className="ec-form__hint ec-form__hint--aviso">
              No tienes insumos activos de esta categoría.
            </span>
          )}
          {insumoElegido && (
            <span className="ec-form__hint">
              Stock actual:{' '}
              {textoCantidad(
                insumoElegido.stock_actual,
                insumoElegido.unidad_medida
              )}
            </span>
          )}
        </label>

        <label className="ec-form__field" htmlFor="rec-cantidad">
          <span className="ec-form__label">
            Dosis por camada
            {insumoElegido ? ` (${insumoElegido.unidad_medida})` : ''}
          </span>
          <input
            id="rec-cantidad"
            className="ec-form__input"
            type="number"
            min="0.01"
            step="0.01"
            value={cantidad}
            onChange={(event) => {
              setCantidad(event.target.value);
              setError('');
            }}
          />
          <span className="ec-form__hint">
            Se descuenta del inventario al registrar la aplicación.
          </span>
        </label>

        <label className="ec-form__field" htmlFor="rec-modo">
          <span className="ec-form__label">Frecuencia</span>
          <select
            id="rec-modo"
            className="ec-form__input"
            value={modo}
            onChange={(event) => {
              setModo(event.target.value);
              setError('');
            }}
          >
            {MODOS.map((opcion) => (
              <option key={opcion.valor} value={opcion.valor}>
                {opcion.label}
              </option>
            ))}
          </select>
        </label>

        {modo === MODO_CADA_DIAS && (
          <label className="ec-form__field" htmlFor="rec-intervalo">
            <span className="ec-form__label">Cada cuántos días</span>
            <input
              id="rec-intervalo"
              className="ec-form__input"
              type="number"
              min="1"
              max="365"
              value={intervalo}
              onChange={(event) => {
                setIntervalo(event.target.value);
                setError('');
              }}
            />
          </label>
        )}

        {modo === MODO_DIAS_SEMANA && (
          <fieldset className="ec-rec-form__dias">
            <legend className="ec-form__label">Días de la semana</legend>
            <div className="ec-rec-form__chips">
              {DIAS_SEMANA.map((dia) => (
                <button
                  key={dia.valor}
                  type="button"
                  className={`ec-chip${dias.includes(dia.valor) ? ' ec-chip--activo' : ''}`}
                  aria-pressed={dias.includes(dia.valor)}
                  onClick={() => alternarDia(dia.valor)}
                >
                  {dia.label}
                </button>
              ))}
            </div>
          </fieldset>
        )}

        {modo === MODO_FECHAS && (
          <fieldset className="ec-rec-form__fechas">
            <legend className="ec-form__label">Fechas de aplicación</legend>
            {fechas.map((item, indice) => (
              <div key={item.id} className="ec-rec-form__fecha-fila">
                <input
                  className="ec-form__input"
                  type="date"
                  aria-label={`Fecha ${indice + 1}`}
                  min={hoyIso()}
                  value={item.fecha}
                  onChange={(event) =>
                    cambiarFecha(item.id, event.target.value)
                  }
                />
                <button
                  type="button"
                  className="ec-btn ec-btn--ghost"
                  onClick={() => quitarFecha(item.id)}
                >
                  Quitar
                </button>
              </div>
            ))}
            {fechas.length < MAXIMO_FECHAS && (
              <button
                type="button"
                className="ec-btn ec-btn--ghost"
                onClick={agregarFecha}
              >
                Agregar fecha
              </button>
            )}
          </fieldset>
        )}

        {modo !== MODO_FECHAS && (
          <div className="ec-rec-form__grid">
            <label className="ec-form__field" htmlFor="rec-inicio">
              <span className="ec-form__label">Fecha de inicio</span>
              <input
                id="rec-inicio"
                className="ec-form__input"
                type="date"
                min={hoyIso()}
                value={fechaInicio}
                onChange={(event) => setFechaInicio(event.target.value)}
              />
            </label>

            <label className="ec-form__field" htmlFor="rec-fin">
              <span className="ec-form__label">Fecha de fin (opcional)</span>
              <input
                id="rec-fin"
                className="ec-form__input"
                type="date"
                min={fechaInicio}
                value={fechaFin}
                onChange={(event) => setFechaFin(event.target.value)}
              />
            </label>
          </div>
        )}

        <label className="ec-rec-form__activo" htmlFor="rec-activo">
          <input
            id="rec-activo"
            type="checkbox"
            checked={activo}
            onChange={(event) => setActivo(event.target.checked)}
          />
          <span>Recordatorio activo</span>
        </label>

        <div className="ec-form__actions">
          <button
            type="button"
            className="ec-btn ec-btn--ghost"
            onClick={onCerrar}
            disabled={enviando}
          >
            Cancelar
          </button>
          <button
            type="submit"
            className="ec-btn ec-btn--primary"
            disabled={enviando}
          >
            {enviando ? 'Guardando…' : 'Guardar'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

RecordatorioFormModal.propTypes = {
  abierto: PropTypes.bool,
  camada: PropTypes.shape({
    id_camada: PropTypes.number.isRequired,
    nombre_camada: PropTypes.string,
  }),
  recordatorio: PropTypes.shape({
    id_recordatorio: PropTypes.number.isRequired,
    id_camada: PropTypes.number,
    nombre_camada: PropTypes.string,
    id_insumo: PropTypes.number.isRequired,
    nombre_insumo: PropTypes.string,
    unidad_medida: PropTypes.string,
    cantidad: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    modo: PropTypes.string.isRequired,
    intervalo_dias: PropTypes.number,
    dias_semana: PropTypes.arrayOf(PropTypes.number),
    fechas: PropTypes.arrayOf(PropTypes.string),
    fecha_inicio: PropTypes.string,
    fecha_fin: PropTypes.string,
    activo: PropTypes.bool,
  }),
  onCerrar: PropTypes.func.isRequired,
  onGuardar: PropTypes.func.isRequired,
};

RecordatorioFormModal.defaultProps = {
  abierto: false,
  camada: null,
  recordatorio: null,
};

export default RecordatorioFormModal;
