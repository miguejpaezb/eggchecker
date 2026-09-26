import { Navigate, Route, Routes } from 'react-router-dom';

import AppLayout from './components/AppLayout';
import Auth from './pages/Auth';
import Camadas from './pages/Camadas';
import Inventario from './pages/Inventario';
import ModulePlaceholder from './pages/ModulePlaceholder';
import Produccion from './pages/Produccion';

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<Auth mode="login" />} />
      <Route path="/register" element={<Auth mode="register" />} />
      <Route path="/recuperar" element={<Auth mode="recuperar" />} />

      <Route element={<AppLayout />}>
        <Route
          path="/dashboard"
          element={<ModulePlaceholder title="Dashboard" />}
        />
        <Route path="/produccion" element={<Produccion />} />
        <Route path="/camadas" element={<Camadas />} />
        <Route path="/inventario" element={<Inventario />} />
        <Route
          path="/clientes"
          element={<ModulePlaceholder title="Clientes" />}
        />
        <Route path="/ventas" element={<ModulePlaceholder title="Ventas" />} />
        <Route
          path="/analisis"
          element={<ModulePlaceholder title="Análisis IA" />}
        />
        <Route
          path="/reportes"
          element={<ModulePlaceholder title="Reportes" />}
        />
        <Route path="/perfil" element={<ModulePlaceholder title="Perfil" />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default App;
