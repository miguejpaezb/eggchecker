import { useCallback, useEffect, useState } from 'react';
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
  const [menuAbierto, setMenuAbierto] = useState(false);

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

  const abrirMenu = useCallback(() => setMenuAbierto(true), []);
  const cerrarMenu = useCallback(() => setMenuAbierto(false), []);
  const handleLogout = useCallback(() => {
    cerrarSesion();
    navigate('/login', { replace: true });
  }, [navigate]);

  if (cargando) {
    return <div className="ec-app-loading">Cargando…</div>;
  }

  return (
    <div className="ec-app">
      <Topbar
        perfil={perfil}
        menuAbierto={menuAbierto}
        onAbrirMenu={abrirMenu}
        onLogout={handleLogout}
      />
      <div className="ec-app__body">
        <Sidebar
          perfil={perfil}
          abierto={menuAbierto}
          onCerrar={cerrarMenu}
          onLogout={handleLogout}
        />
        {menuAbierto && (
          <button
            type="button"
            className="ec-sidebar-backdrop"
            aria-label="Cerrar menú"
            onClick={cerrarMenu}
          />
        )}
        <main className="ec-main">
          <Outlet context={perfil} />
        </main>
      </div>
    </div>
  );
}

export default AppLayout;
