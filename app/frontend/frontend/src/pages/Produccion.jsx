import CamadaAutocomplete from '../components/CamadaAutocomplete';
import HistorialProduccion from '../components/HistorialProduccion';
import TipoHuevoCard from '../components/TipoHuevoCard';
import useProduccion from '../hooks/useProduccion';
import { TIPOS_HUEVO } from '../utils/produccion';

/**
 * Página de Producción Diaria: registro de la recolección por camada.
 * @returns {JSX.Element} La vista del módulo de producción.
 */
function Produccion() {
  const {
    camadas,
    camadaSeleccionada,
    seleccionarCamada,
    fecha,
    cambiarFecha,
    hoy,
    cantidades,
    incrementar,
    decrementar,
    total,
    limite,
    esHoy,
    editable,
    puedeGuardar,
    registrosRecientes,
    promedio7Dias,
    cargando,
    cargandoDia,
    guardando,
    error,
    mensaje,
    guardar,
  } = useProduccion();

  const limiteAlcanzado = total >= limite;

  return (
    <div>
      <div className="ec-page-head ec-produccion__head">
        <div>
          <h1 className="ec-page-title">Producción Diaria</h1>
          <p className="ec-page-subtitle">Gestión de recolección de huevos.</p>
        </div>

        <div className="ec-glass-card ec-produccion__selector">
          <label
            className="ec-produccion__selector-bloque"
            htmlFor="produccion-camada"
          >
            <span className="ec-produccion__selector-label">Camada</span>
            <CamadaAutocomplete
              id="produccion-camada"
              camadas={camadas}
              camadaSeleccionada={camadaSeleccionada}
              onSeleccionar={seleccionarCamada}
              placeholder="Selecciona una camada"
              deshabilitado={cargando}
            />
          </label>

          <label
            className="ec-produccion__selector-fecha"
            htmlFor="produccion-fecha"
          >
            <span className="ec-produccion__selector-label">
              Fecha de recolección
            </span>
            <input
              id="produccion-fecha"
              className="ec-produccion__fecha-input"
              type="date"
              value={fecha}
              max={hoy}
              onChange={(event) => cambiarFecha(event.target.value)}
            />
          </label>
        </div>
      </div>

      {cargando && <p className="ec-camadas__estado">Cargando producción…</p>}

      {!cargando && camadas.length === 0 && (
        <div className="ec-glass-card ec-camadas__estado">
          No tienes camadas en etapa de producción (28 semanas o más).
        </div>
      )}

      {!cargando && camadas.length > 0 && (
        <div className="ec-produccion__layout">
          <div className="ec-produccion__form">
            {!esHoy && (
              <p className="ec-produccion__aviso">
                Estás viendo una fecha anterior: los datos son de solo lectura.
                Selecciona la fecha de hoy para registrar.
              </p>
            )}

            {TIPOS_HUEVO.map((tipo) => (
              <TipoHuevoCard
                key={tipo.clave}
                tipo={tipo}
                valor={cantidades[tipo.clave]}
                onIncrementar={incrementar}
                onDecrementar={decrementar}
                deshabilitado={!editable}
                limiteAlcanzado={limiteAlcanzado}
              />
            ))}

            {editable && limiteAlcanzado && (
              <p className="ec-produccion__aviso">
                Alcanzaste la cantidad de aves actuales de la camada; no puedes
                registrar más huevos.
              </p>
            )}

            {error && (
              <div className="ec-glass-card ec-camadas__estado ec-camadas__estado--error">
                {error}
              </div>
            )}

            {mensaje && (
              <div className="ec-glass-card ec-produccion__exito">
                {mensaje}
              </div>
            )}

            <div className="ec-produccion__footer">
              <div className="ec-produccion__total">
                <h3 className="ec-produccion__total-titulo">
                  Total registrado
                </h3>
                <span className="ec-produccion__total-valor">
                  {cargandoDia ? '…' : total}
                </span>
              </div>
              <button
                type="button"
                className="ec-produccion__guardar"
                onClick={guardar}
                disabled={!puedeGuardar}
              >
                {guardando ? 'Guardando…' : 'Guardar'}
              </button>
            </div>
          </div>

          <HistorialProduccion
            registros={registrosRecientes}
            promedio={promedio7Dias}
          />
        </div>
      )}
    </div>
  );
}

export default Produccion;
