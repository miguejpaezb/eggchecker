import 'bootstrap/dist/css/bootstrap.min.css';
import './index.css';
import './styles/auth.css';
import './styles/app.css';
import './styles/camadas.css';
import './styles/notificaciones.css';
import './styles/inventario.css';
import './styles/produccion.css';

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';

import App from './App';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>
);
