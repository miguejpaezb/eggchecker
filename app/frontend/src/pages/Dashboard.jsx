import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

import { cerrarSesion, obtenerToken } from '../services/authService';

function Dashboard() {
  const navigate = useNavigate();

  useEffect(() => {
    if (!obtenerToken()) {
      navigate('/login', { replace: true });
    }
  }, [navigate]);

  const handleLogout = () => {
    cerrarSesion();
    navigate('/login', { replace: true });
  };

  return (
    <div className="ec-auth">
      <div className="ec-auth__pattern" aria-hidden="true" />
      <div className="ec-auth__content">
        <div className="ec-auth__panel">
          <div className="ec-auth__card">
            <div className="ec-auth__card-inner text-center">
              <h2 className="ec-auth__heading">Bienvenido a EggChecker</h2>
              <p className="mb-0">Tu sesión se inició correctamente.</p>
              <button
                type="button"
                className="ec-auth__submit"
                onClick={handleLogout}
              >
                Cerrar sesión
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
