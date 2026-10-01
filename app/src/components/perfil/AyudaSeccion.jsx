import { useState } from 'react';

import Icon from '../Icon';

// Correo de soporte al que se dirige el formulario.
const CORREO_SOPORTE = 'ayuda@eggchecker.click';

// Preguntas frecuentes de ejemplo (PQR).
const PQR = [
  {
    id: 1,
    pregunta: '¿Cómo registro la producción diaria de una camada?',
    respuesta:
      'Ve al módulo Producción, selecciona la camada y las cantidades por ' +
      'tipo de huevo (AA, A, B y No apto). El sistema guarda el total del día.',
  },
  {
    id: 2,
    pregunta: 'El stock de un insumo aparece en negativo, ¿qué hago?',
    respuesta:
      'Revisa los movimientos registrados. El sistema no permite salidas ' +
      'mayores al stock disponible; corrige el movimiento y vuelve a intentar.',
  },
  {
    id: 3,
    pregunta: '¿Cómo cambio el correo de mi cuenta?',
    respuesta:
      'En Perfil → Editar Perfil cambia el correo e ingresa tu contraseña ' +
      'actual. Por seguridad, la sesión se cerrará y deberás iniciar de nuevo.',
  },
  {
    id: 4,
    pregunta: '¿Qué incluye el plan Premium?',
    respuesta:
      'Aves y clientes ilimitados, además del módulo de Inteligencia ' +
      'Artificial. Consulta la sección Plan y Suscripción.',
  },
];

/**
 * Sección de ayuda: envía un correo de soporte y muestra PQR frecuentes.
 * @returns {JSX.Element} El formulario y la lista de PQR.
 */
function AyudaSeccion() {
  const [asunto, setAsunto] = useState('');
  const [mensaje, setMensaje] = useState('');
  const [abierta, setAbierta] = useState(null);

  const handleSubmit = (event) => {
    event.preventDefault();
    const enlace =
      `mailto:${CORREO_SOPORTE}` +
      `?subject=${encodeURIComponent(asunto)}` +
      `&body=${encodeURIComponent(mensaje)}`;
    window.location.href = enlace;
  };

  const alternar = (id) => setAbierta((previa) => (previa === id ? null : id));

  return (
    <section className="ec-perfil__bloque">
      <h2 className="ec-perfil__seccion-titulo">Ayuda y Soporte</h2>

      <form className="ec-perfil__form" onSubmit={handleSubmit}>
        <label className="ec-form__field" htmlFor="ayuda-asunto">
          <span className="ec-form__label">Asunto</span>
          <input
            id="ayuda-asunto"
            className="ec-form__input"
            type="text"
            value={asunto}
            onChange={(event) => setAsunto(event.target.value)}
            maxLength={120}
            placeholder="Ej. Problema al registrar un pedido"
            required
          />
        </label>

        <label className="ec-form__field" htmlFor="ayuda-mensaje">
          <span className="ec-form__label">Mensaje</span>
          <textarea
            id="ayuda-mensaje"
            className="ec-form__input ec-perfil__textarea"
            value={mensaje}
            onChange={(event) => setMensaje(event.target.value)}
            rows={5}
            placeholder="Cuéntanos en qué podemos ayudarte…"
            required
          />
        </label>

        <div className="ec-form__actions">
          <button type="submit" className="ec-btn ec-btn--primary">
            Enviar Correo
          </button>
        </div>
      </form>

      <div className="ec-perfil__pqr">
        <h3 className="ec-perfil__subtitulo">Preguntas frecuentes (PQR)</h3>
        <ul className="ec-perfil__pqr-lista">
          {PQR.map((item) => (
            <li key={item.id} className="ec-perfil__pqr-item">
              <button
                type="button"
                className="ec-perfil__pqr-boton"
                onClick={() => alternar(item.id)}
                aria-expanded={abierta === item.id}
              >
                <span>{item.pregunta}</span>
                <Icon
                  src="/assets/icons/keyboard_arrow_right-icon.svg"
                  className={`ec-perfil__pqr-icon${
                    abierta === item.id ? ' ec-perfil__pqr-icon--open' : ''
                  }`}
                />
              </button>
              {abierta === item.id && (
                <p className="ec-perfil__pqr-respuesta">{item.respuesta}</p>
              )}
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}

export default AyudaSeccion;
