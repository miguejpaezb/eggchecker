import { Navigate, Route, Routes } from 'react-router-dom';

import AppLayout from './components/AppLayout';
import AnalisisIA from './pages/AnalisisIA';
import Auth from './pages/Auth';
import Camadas from './pages/Camadas';
import Clientes from './pages/Clientes';
import Dashboard from './pages/Dashboard';
import Inventario from './pages/Inventario';
import Perfil from './pages/Perfil';
import Produccion from './pages/Produccion';
import Reportes from './pages/Reportes';
import Ventas from './pages/Ventas';

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<Auth mode="login" />} />
      <Route path="/register" element={<Auth mode="register" />} />
      <Route path="/recuperar" element={<Auth mode="recuperar" />} />

      <Route element={<AppLayout />}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/produccion" element={<Produccion />} />
        <Route path="/camadas" element={<Camadas />} />
        <Route path="/inventario" element={<Inventario />} />
        <Route path="/clientes" element={<Clientes />} />
        <Route path="/ventas" element={<Ventas />} />
        <Route path="/analisis" element={<AnalisisIA />} />
        <Route path="/reportes" element={<Reportes />} />
        <Route path="/perfil" element={<Perfil />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default App;
