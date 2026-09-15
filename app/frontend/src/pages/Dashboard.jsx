import { Link, useOutletContext } from 'react-router-dom';

import BarChart from '../components/BarChart';
import Icon from '../components/Icon';

// Datos de demostración mientras los módulos de negocio no existen.
const PRODUCCION_SEMANAL = {
  labels: ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'],
  values: [1150, 1200, 1180, 1100, 1250, 1210, 1245],
};

const KPIS = [
  {
    id: 'produccion',
    label: 'Producción Hoy',
    valor: '1,245',
    hint: '↑ +5.2% vs ayer',
    hintTono: 'up',
    tono: 'yellow',
    icono: '/assets/icons/egg-icon.svg',
  },
  {
    id: 'aves',
    label: 'Aves activas',
    valor: '12,450',
    hint: 'En total',
    hintTono: 'neutro',
    tono: 'brown',
    icono: '/assets/icons/feather-icon.svg',
  },
  {
    id: 'pendientes',
    label: 'Pendientes',
    valor: '15',
    hint: 'Pedidos por entregar',
    hintTono: 'green',
    tono: 'green',
    icono: '/assets/icons/shopping_cart-icon.svg',
  },
  {
    id: 'alertas',
    label: 'Alertas',
    valor: '2',
    hint: 'Atención requerida',
    hintTono: 'red',
    tono: 'red',
    icono: '/assets/icons/warning-icon.svg',
  },
];

const RESUMEN_SEMANAL = [
  { label: 'Total huevos (7d):', valor: '371' },
  { label: 'Mejor día:', valor: '68 - Viernes', tono: 'green' },
  { label: 'Valor producido:', valor: '$296.800' },
  { label: 'Tasa postura:', valor: '79%', tono: 'green' },
  { label: 'Mortalidad:', valor: '1 Ave', tono: 'red' },
];

const ALERTAS = [
  {
    id: 'alerta-1',
    titulo: 'Concentrado Ponedoras',
    desc: 'Quedan 2 Bultos (Mínimo: 3)',
  },
  {
    id: 'alerta-2',
    titulo: 'Concentrado Ponedoras',
    desc: 'Quedan 2 Bultos (Mínimo: 3)',
  },
];

const PEDIDOS = [
  {
    nombre: 'Supermercado El Vergel',
    meta: '120 AA + 60 A | 19 Mar',
    estado: 'Pendiente',
  },
  {
    nombre: 'Restaurante La Fogata',
    meta: '60 AA | 20 Mar',
    estado: 'Pendiente',
  },
  {
    nombre: 'Tienda Don Rodrigo',
    meta: '30 AA + 20 A | 18 Mar',
    estado: 'Entregado',
  },
];

function Dashboard() {
  const perfil = useOutletContext();
  const nombre = perfil?.nombre_completo ?? 'avicultor';

  return (
    <div>
      <div className="ec-page-head">
        <div>
          <h1 className="ec-page-title">Dashboard</h1>
          <p className="ec-page-subtitle">Bienvenido de nuevo, {nombre}.</p>
        </div>
        <Link to="/produccion" className="ec-btn-primary">
          <Icon
            src="/assets/icons/add-icon.svg"
            className="ec-btn-primary__icon"
          />
          Nueva Producción
        </Link>
      </div>

      <div className="ec-kpi-grid">
        {KPIS.map((kpi) => (
          <div
            key={kpi.id}
            className={`ec-glass-card ec-kpi ec-kpi--${kpi.tono}`}
          >
            <div className="ec-kpi__row">
              <div>
                <p className="ec-kpi__label">{kpi.label}</p>
                <h3 className="ec-kpi__value">{kpi.valor}</h3>
                <p className={`ec-kpi__hint ec-kpi__hint--${kpi.hintTono}`}>
                  {kpi.hint}
                </p>
              </div>
              <div className="ec-kpi__icon-wrap">
                <Icon src={kpi.icono} className="ec-kpi__icon" />
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="ec-grid">
        <div className="ec-glass-card ec-card-pad ec-grid__span2">
          <h3 className="ec-section-title">Producción semanal</h3>
          <div className="ec-chart-wrap">
            <BarChart
              labels={PRODUCCION_SEMANAL.labels}
              values={PRODUCCION_SEMANAL.values}
            />
          </div>
        </div>

        <div className="ec-glass-card ec-card-pad">
          <h3 className="ec-section-title">Resumen semanal</h3>
          <div className="ec-summary">
            {RESUMEN_SEMANAL.map((fila) => (
              <div className="ec-summary__row" key={fila.label}>
                <span className="ec-summary__label">{fila.label}</span>
                <span
                  className={`ec-summary__value${
                    fila.tono ? ` ec-summary__value--${fila.tono}` : ''
                  }`}
                >
                  {fila.valor}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="ec-grid">
        <div className="ec-glass-card ec-card-pad ec-grid__span2">
          <div className="ec-alerts-head">
            <Icon
              src="/assets/icons/warning-icon.svg"
              className="ec-alerts-head__icon"
            />
            <h3 className="ec-alerts-head__title">
              Alertas Importantes ({ALERTAS.length})
            </h3>
          </div>
          <div className="ec-alert-list">
            {ALERTAS.map((alerta) => (
              <div className="ec-alert-item" key={alerta.id}>
                <div className="ec-alert-item__left">
                  <Icon
                    src="/assets/icons/notifications-icon.svg"
                    className="ec-alert-item__icon"
                  />
                  <div>
                    <h4 className="ec-alert-item__title">{alerta.titulo}</h4>
                    <p className="ec-alert-item__desc">{alerta.desc}</p>
                  </div>
                </div>
                <a href="#detalle" className="ec-alert-item__link">
                  Detalles
                </a>
              </div>
            ))}
          </div>
        </div>

        <div className="ec-glass-card ec-card-pad">
          <h3 className="ec-section-title">Pedidos recientes</h3>
          <div className="ec-order-list">
            {PEDIDOS.map((pedido) => (
              <div className="ec-order-item" key={pedido.nombre}>
                <div>
                  <h4 className="ec-order-item__name">{pedido.nombre}</h4>
                  <p className="ec-order-item__meta">{pedido.meta}</p>
                </div>
                <span
                  className={`ec-badge${
                    pedido.estado === 'Entregado'
                      ? ' ec-badge--done'
                      : ' ec-badge--pending'
                  }`}
                >
                  {pedido.estado}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
