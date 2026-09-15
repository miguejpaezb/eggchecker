import { useEffect, useState } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';

import {
  cerrarSesion,
  obtenerPerfil,
  obtenerToken,
} from '../services/authService';
import Sidebar from './Sidebar';
import Topbar from './Topbar';

function AppLayout() {
  const navigate = useNavigate();
  const [perfil, setPerfil] = useState(null);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    if (!obtenerToken()) {
      navigate('/login', { replace: true });
      return undefined;
    }

    let activo = true;
    obtenerPerfil()
      .then((datos) => {
        if (activo) {
          setPerfil(datos);
        }
      })
      .catch(() => {
        cerrarSesion();
        navigate('/login', { replace: true });
      })
      .finally(() => {
        if (activo) {
          setCargando(false);
        }
      });

    return () => {
      activo = false;
    };
  }, [navigate]);

  if (cargando) {
    return <div className="ec-app-loading">Cargando…</div>;
  }

  return (
    <div className="ec-app">
      <Topbar />
      <div className="ec-app__body">
        <Sidebar perfil={perfil} />
        <main className="ec-main">
          <Outlet context={perfil} />
        </main>
      </div>
    </div>
  );
}

export default AppLayout;
