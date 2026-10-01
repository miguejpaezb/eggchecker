import AyudaSeccion from '../components/perfil/AyudaSeccion';
import EditarPerfilSeccion from '../components/perfil/EditarPerfilSeccion';
import NotificacionesSeccion from '../components/perfil/NotificacionesSeccion';
import PerfilHistoricoCard from '../components/perfil/PerfilHistoricoCard';
import PerfilMenu from '../components/perfil/PerfilMenu';
import PerfilUsuarioCard from '../components/perfil/PerfilUsuarioCard';
import PlanSeccion from '../components/perfil/PlanSeccion';
import SeguridadSeccion from '../components/perfil/SeguridadSeccion';
import usePerfil from '../hooks/usePerfil';

/**
 * Página de Perfil: datos del usuario, histórico y secciones de cuenta.
 * @returns {JSX.Element} La vista del módulo de perfil.
 */
function Perfil() {
  const {
    perfil,
    seccion,
    cambiarSeccion,
    guardarPerfil,
    guardarNotificaciones,
    guardarContrasena,
  } = usePerfil();

  const renderizarSeccion = () => {
    switch (seccion) {
      case 'notificaciones':
        return (
          <NotificacionesSeccion
            perfil={perfil}
            onGuardar={guardarNotificaciones}
          />
        );
      case 'seguridad':
        return <SeguridadSeccion onGuardar={guardarContrasena} />;
      case 'plan':
        return <PlanSeccion perfil={perfil} />;
      case 'ayuda':
        return <AyudaSeccion />;
      default:
        return (
          <EditarPerfilSeccion perfil={perfil} onGuardar={guardarPerfil} />
        );
    }
  };

  return (
    <div>
      <div className="ec-page-head">
        <div>
          <h1 className="ec-page-title">Perfil</h1>
          <p className="ec-page-subtitle">
            Gestiona tu cuenta, tu granja y tus preferencias.
          </p>
        </div>
      </div>

      <div className="ec-perfil__layout">
        <div className="ec-perfil__izq">
          <PerfilUsuarioCard perfil={perfil} />
          <PerfilHistoricoCard perfil={perfil} />
          <PerfilMenu activa={seccion} onSeleccionar={cambiarSeccion} />
        </div>
        <div className="ec-perfil__der">{renderizarSeccion()}</div>
      </div>
    </div>
  );
}

export default Perfil;
