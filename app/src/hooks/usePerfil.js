import { useCallback, useState } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';

import { cerrarSesion } from '../services/authService';
import {
  actualizarNotificaciones,
  actualizarPerfil,
  cambiarContrasena,
} from '../services/perfilService';

/**
 * Gestiona la sección activa del perfil y las acciones de guardado.
 * @returns {Object} Perfil, sección activa y acciones del módulo.
 */
function usePerfil() {
  const navigate = useNavigate();
  const { perfil, recargarPerfil } = useOutletContext();
  const [seccion, setSeccion] = useState('editar');

  /**
   * Guarda los datos del perfil. Si el correo cambia, cierra la sesión
   * para obligar a un nuevo inicio de sesión con el correo nuevo.
   */
  const guardarPerfil = useCallback(
    async (datos) => {
      const cambioCorreo =
        datos.correo_electronico !== perfil?.correo_electronico;
      const actualizado = await actualizarPerfil(datos);
      if (cambioCorreo) {
        cerrarSesion();
        navigate('/login', { replace: true });
        return actualizado;
      }
      await recargarPerfil();
      return actualizado;
    },
    [perfil, recargarPerfil, navigate]
  );

  /** Guarda las preferencias de notificaciones y refresca el perfil. */
  const guardarNotificaciones = useCallback(
    async (datos) => {
      const actualizado = await actualizarNotificaciones(datos);
      await recargarPerfil();
      return actualizado;
    },
    [recargarPerfil]
  );

  /** Cambia la contraseña del usuario autenticado. */
  const guardarContrasena = useCallback(
    (datos) => cambiarContrasena(datos),
    []
  );

  return {
    perfil,
    seccion,
    cambiarSeccion: setSeccion,
    guardarPerfil,
    guardarNotificaciones,
    guardarContrasena,
  };
}

export default usePerfil;
