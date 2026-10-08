import AnalisisDetalleModal from '../components/analisis/AnalisisDetalleModal';
import CapturaCard from '../components/analisis/CapturaCard';
import DiagnosticoContenido from '../components/analisis/DiagnosticoContenido';
import HistorialAnalisis from '../components/analisis/HistorialAnalisis';
import Icon from '../components/Icon';
import useAnalisisIA from '../hooks/useAnalisisIA';

/**
 * Página del módulo Análisis IA (RF-33 a RF-36, CU-06): foto de los huevos,
 * diagnóstico con anomalías y recomendaciones, e historial por camada.
 * @returns {JSX.Element} La vista del módulo.
 */
function AnalisisIA() {
  const ia = useAnalisisIA();
  const { estadoIA } = ia;
  const camadasActivas = ia.camadas.filter((c) => c.estado === 'activa');

  return (
    <div>
      <div className="ec-page-head">
        <div>
          <h1 className="ec-page-title">Análisis IA</h1>
          <p className="ec-page-subtitle">
            Fotografía tus huevos y recibe un diagnóstico de calidad.
          </p>
        </div>
      </div>

      {ia.cargandoEstado && !estadoIA && (
        <p className="ec-camadas__estado">Cargando análisis IA…</p>
      )}

      {!ia.cargandoEstado && ia.errorEstado && !estadoIA && (
        <div className="ec-glass-card ec-camadas__estado ec-camadas__estado--error">
          <p>{ia.errorEstado}</p>
          <button
            type="button"
            className="ec-btn ec-btn--ghost"
            onClick={ia.cargarEstado}
          >
            Reintentar
          </button>
        </div>
      )}

      {estadoIA && !estadoIA.disponible && (
        <section className="ec-glass-card ec-analisis__premium">
          <Icon
            src="/assets/icons/icon_shield.svg"
            className="ec-analisis__premium-icon"
          />
          <h2 className="ec-reportes__card-title">Función Premium</h2>
          <p>{estadoIA.mensaje}</p>
          <p className="ec-analisis__dato">
            Con el plan Premium puedes fotografiar tus huevos y recibir un
            diagnóstico de calidad con anomalías detectadas, recomendaciones
            para tu granja e historial por camada. Consulta los planes en
            Perfil.
          </p>
        </section>
      )}

      {estadoIA && estadoIA.disponible && (
        <>
          {estadoIA.modo_demo && (
            <p className="ec-analisis__aviso">{estadoIA.mensaje}</p>
          )}

          <div className="ec-analisis__layout">
            {ia.resultado ? (
              <section className="ec-glass-card ec-analisis__card">
                <header className="ec-reportes__card-head">
                  <Icon
                    src="/assets/icons/icon_ia.svg"
                    className="ec-reportes__card-icon"
                  />
                  <h2 className="ec-reportes__card-title">
                    Resultado del análisis
                  </h2>
                </header>
                <DiagnosticoContenido
                  analisis={ia.resultado}
                  onRetroalimentar={ia.retroalimentar}
                />
                <button
                  type="button"
                  className="ec-btn ec-btn--secondary ec-analisis__nuevo"
                  onClick={ia.nuevoAnalisis}
                >
                  Hacer otro análisis
                </button>
              </section>
            ) : (
              <CapturaCard
                estadoIA={estadoIA}
                camadas={camadasActivas}
                camadaId={ia.camadaAnalisis}
                fotoUrl={ia.foto ? ia.foto.url : ''}
                preparando={ia.preparando}
                analizando={ia.analizando}
                error={ia.errorAnalisis}
                onCamada={ia.setCamadaAnalisis}
                onFoto={ia.elegirFoto}
                onAnalizar={ia.analizar}
                onCambiarFoto={ia.descartarFoto}
              />
            )}

            <HistorialAnalisis
              historial={ia.historial}
              camadas={ia.camadas}
              filtro={ia.filtro}
              cargando={ia.cargandoHistorial}
              error={ia.errorHistorial}
              onFiltro={ia.setFiltro}
              onAbrir={ia.abrirDetalle}
            />
          </div>
        </>
      )}

      <AnalisisDetalleModal
        analisis={ia.detalle}
        imagen={ia.imagenDetalle}
        cargandoImagen={ia.cargandoImagen}
        confirmarEliminar={ia.confirmarEliminar}
        eliminando={ia.eliminando}
        errorEliminar={ia.errorEliminar}
        onCerrar={ia.cerrarDetalle}
        onRetroalimentar={ia.retroalimentar}
        onPedirEliminar={ia.pedirEliminar}
        onCancelarEliminar={ia.cancelarEliminar}
        onEliminar={ia.eliminar}
      />
    </div>
  );
}

export default AnalisisIA;
