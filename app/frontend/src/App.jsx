import { Navigate, Route, Routes } from 'react-router-dom';

import AppLayout from './components/AppLayout';
import Auth from './pages/Auth';
import Dashboard from './pages/Dashboard';
import ModulePlaceholder from './pages/ModulePlaceholder';

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<Auth mode="login" />} />
      <Route path="/register" element={<Auth mode="register" />} />

      <Route element={<AppLayout />}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route
          path="/produccion"
          element={<ModulePlaceholder title="Producción" />}
        />
        <Route
          path="/camadas"
          element={<ModulePlaceholder title="Camadas" />}
        />
        <Route
          path="/inventario"
          element={<ModulePlaceholder title="Inventario" />}
        />
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
