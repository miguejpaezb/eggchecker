import { Link, useOutletContext } from 'react-router-dom';

import AlertasImportantesCard from '../components/dashboard/AlertasImportantesCard';
import KpiCard from '../components/dashboard/KpiCard';
import KpiCarrusel from '../components/dashboard/KpiCarrusel';
import PedidosRecientesCard from '../components/dashboard/PedidosRecientesCard';
import ProduccionSemanalChart from '../components/dashboard/ProduccionSemanalChart';
import ResumenSemanalCard from '../components/dashboard/ResumenSemanalCard';
import Icon from '../components/Icon';
import useDashboard from '../hooks/useDashboard';
import { textoVariacion } from '../utils/dashboard';
import { formatoNumero } from '../utils/reportes';

/**
 * Página principal: KPI, producción semanal, alertas y pedidos recientes.
 * @returns {JSX.Element} La vista del dashboard.
 */
function Dashboard() {
  const { datos, cargando, error, recargar } = useDashboard();
  const { perfil } = useOutletContext();
  const nombre = perfil?.nombre_completo ?? 'avicultor';

  return (
    <div>
      <div className="ec-page-head">
        <div>
          <h1 className="ec-page-title">Dashboard</h1>
          <p className="ec-page-subtitle">Bienvenido de nuevo, {nombre}.</p>
        </div>
        <Link to="/produccion" className="ec-dashboard__nueva">
          <Icon
            src="/assets/icons/add-icon.svg"
            className="ec-dashboard__nueva-icon"
          />
          Nueva Producción
        </Link>
      </div>

      {cargando && <p className="ec-camadas__estado">Cargando dashboard…</p>}

      {!cargando && error && (
        <div className="ec-glass-card ec-camadas__estado ec-camadas__estado--error">
          <p className="mb-2">{error}</p>
          <button
            type="button"
            className="ec-dashboard__reintentar"
            onClick={recargar}
          >
            Reintentar
          </button>
        </div>
      )}

      {!cargando && !error && datos && (
        <>
          <KpiCarrusel
            etiquetas={[
              'Producción Hoy',
              'Aves activas',
              'Pendientes',
              'Alertas',
            ]}
          >
            <KpiCard
              titulo="Producción Hoy"
              valor={formatoNumero(datos.produccion_hoy)}
              detalle={textoVariacion(datos.variacion_produccion)}
              icono="/assets/icons/egg-icon.svg"
              tono="yellow"
            />
            <KpiCard
              titulo="Aves activas"
              valor={formatoNumero(datos.aves_activas)}
              detalle="En total"
              icono="/assets/icons/feather-icon.svg"
              tono="brown"
            />
            <KpiCard
              titulo="Pendientes"
              valor={formatoNumero(datos.pedidos_pendientes)}
              detalle="Pedidos por entregar"
              icono="/assets/icons/icon_ventas.svg"
              tono="green"
            />
            <KpiCard
              titulo="Alertas"
              valor={formatoNumero(datos.alertas_count)}
              detalle="Atención requerida"
              icono="/assets/icons/warning-icon.svg"
              tono="red"
            />
          </KpiCarrusel>

          <div className="ec-dashboard__fila">
            <section className="ec-glass-card ec-dashboard__card ec-dashboard__card--ancha">
              <h3 className="ec-dashboard__card-titulo">Producción semanal</h3>
              <ProduccionSemanalChart serie={datos.semana.serie} />
            </section>
            <ResumenSemanalCard semana={datos.semana} />
          </div>

          <div className="ec-dashboard__fila">
            <AlertasImportantesCard alertas={datos.alertas} />
            <PedidosRecientesCard pedidos={datos.pedidos_recientes} />
          </div>
        </>
      )}
    </div>
  );
}

export default Dashboard;
