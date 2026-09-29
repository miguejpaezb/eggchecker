import CuentasCard from '../components/reportes/CuentasCard';
import FiltroPeriodo from '../components/reportes/FiltroPeriodo';
import ResumenRecogidaCard from '../components/reportes/ResumenRecogidaCard';
import SaludBajasCard from '../components/reportes/SaludBajasCard';
import UsoComidaCard from '../components/reportes/UsoComidaCard';
import useReportes from '../hooks/useReportes';

/**
 * Página de Reportes: rentabilidad, producción, salud y consumo del período.
 * @returns {JSX.Element} La vista del módulo de reportes.
 */
function Reportes() {
  const {
    preset,
    desde,
    hasta,
    camada,
    camadas,
    reporte,
    cargando,
    descargando,
    error,
    dias,
    cambiarPreset,
    cambiarDesde,
    cambiarHasta,
    cambiarCamada,
    descargar,
  } = useReportes();

  return (
    <div>
      <div className="ec-page-head">
        <div>
          <h1 className="ec-page-title">Reportes</h1>
          <p className="ec-page-subtitle">
            Rentabilidad, producción y salud de tu granja.
          </p>
        </div>
      </div>

      <FiltroPeriodo
        preset={preset}
        desde={desde}
        hasta={hasta}
        camada={camada}
        camadas={camadas}
        descargando={descargando}
        onPreset={cambiarPreset}
        onDesde={cambiarDesde}
        onHasta={cambiarHasta}
        onCamada={cambiarCamada}
        onDescargar={descargar}
      />

      {cargando && <p className="ec-camadas__estado">Cargando reporte…</p>}

      {!cargando && error && (
        <div className="ec-glass-card ec-camadas__estado ec-camadas__estado--error">
          {error}
        </div>
      )}

      {!cargando && !error && reporte && (
        <div className="ec-reportes__layout">
          <div className="ec-reportes__columna">
            <CuentasCard
              ventas={reporte.ventas.ingreso_total}
              gastos={reporte.gastos.total}
              ganancia={reporte.ganancia.valor}
            />
            <SaludBajasCard
              gallinasPerdidas={reporte.salud.gallinas_perdidas}
              causaPrincipal={reporte.salud.causa_principal}
              vacunacionAlDia={reporte.salud.vacunacion_al_dia}
            />
          </div>

          <div className="ec-reportes__columna">
            <ResumenRecogidaCard
              produccion={reporte.produccion}
              camada={reporte.camada}
            />
            <UsoComidaCard
              kgUsados={reporte.alimento.kg_usados}
              promedioDiario={reporte.alimento.promedio_diario}
              dias={dias}
            />
          </div>
        </div>
      )}
    </div>
  );
}

export default Reportes;
