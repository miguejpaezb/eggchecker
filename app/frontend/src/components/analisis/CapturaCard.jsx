import PropTypes from 'prop-types';
import { useRef, useState } from 'react';

import Icon from '../Icon';

/**
 * Tarjeta para elegir la foto de los huevos y lanzar el análisis (RF-33).
 * En el celular, "Tomar foto" abre la cámara trasera; en el computador abre
 * el selector de archivos. También acepta arrastrar y soltar la imagen.
 * @param {Object} props - Propiedades del componente.
 * @returns {JSX.Element} La tarjeta de captura.
 */
function CapturaCard({
  estadoIA,
  camadas,
  camadaId,
  fotoUrl,
  preparando,
  analizando,
  error,
  onCamada,
  onFoto,
  onAnalizar,
  onCambiarFoto,
}) {
  const camaraRef = useRef(null);
  const archivoRef = useRef(null);
  const [arrastrando, setArrastrando] = useState(false);

  const restantes = Math.max(estadoIA.limite_diario - estadoIA.usados_hoy, 0);
  const sinCupo = restantes === 0;

  const alElegir = (event) => {
    const input = event.currentTarget;
    onFoto(input.files[0]);
    // Permite volver a elegir el mismo archivo.
    input.value = '';
  };

  const alSoltar = (event) => {
    event.preventDefault();
    setArrastrando(false);
    if (!sinCupo) {
      onFoto(event.dataTransfer.files[0]);
    }
  };

  return (
    <section className="ec-glass-card ec-analisis__card">
      <header className="ec-reportes__card-head">
        <Icon
          src="/assets/icons/icon_ia.svg"
          className="ec-reportes__card-icon"
        />
        <h2 className="ec-reportes__card-title">Nuevo análisis</h2>
      </header>

      <label
        className="ec-reportes__campo ec-analisis__campo"
        htmlFor="camada-analisis"
      >
        <span className="ec-reportes__campo-label">Camada</span>
        <select
          id="camada-analisis"
          className="ec-reportes__select"
          value={camadaId}
          onChange={(event) => onCamada(event.target.value)}
        >
          <option value="">Sin camada</option>
          {camadas.map((camada) => (
            <option key={camada.id_camada} value={camada.id_camada}>
              {camada.nombre_camada}
            </option>
          ))}
        </select>
      </label>

      <div
        className={`ec-analisis__zona${arrastrando ? ' ec-analisis__zona--activa' : ''}`}
        onDragOver={(event) => {
          event.preventDefault();
          setArrastrando(true);
        }}
        onDragLeave={() => setArrastrando(false)}
        onDrop={alSoltar}
      >
        {preparando && (
          <p className="ec-analisis__zona-texto">Preparando la foto…</p>
        )}
        {!preparando && fotoUrl && (
          <img
            src={fotoUrl}
            alt="Foto de los huevos a analizar"
            className="ec-analisis__foto"
          />
        )}
        {!preparando && !fotoUrl && (
          <div className="ec-analisis__consejos">
            <Icon
              src="/assets/icons/egg-icon.svg"
              className="ec-analisis__consejos-icon"
            />
            <strong>Cómo tomar una buena foto</strong>
            <span>
              Huevos en la bandeja, de cerca y con luz natural. Evita sombras y
              reflejos. Hasta 30 huevos por foto. También puedes arrastrar la
              imagen aquí.
            </span>
          </div>
        )}
      </div>

      <input
        ref={camaraRef}
        type="file"
        accept="image/*"
        capture="environment"
        className="d-none"
        onChange={alElegir}
        aria-label="Tomar foto con la cámara"
      />
      <input
        ref={archivoRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        className="d-none"
        onChange={alElegir}
        aria-label="Elegir foto del equipo"
      />

      {error && <p className="ec-form__error ec-analisis__error">{error}</p>}

      {fotoUrl ? (
        <div className="ec-analisis__acciones">
          <button
            type="button"
            className="ec-btn ec-btn--primary"
            onClick={onAnalizar}
            disabled={analizando || sinCupo}
          >
            {analizando
              ? 'Analizando… (puede tardar unos segundos)'
              : 'Analizar con IA'}
          </button>
          <button
            type="button"
            className="ec-btn ec-btn--ghost"
            onClick={onCambiarFoto}
            disabled={analizando}
          >
            Cambiar foto
          </button>
        </div>
      ) : (
        <div className="ec-analisis__acciones">
          <button
            type="button"
            className="ec-btn ec-btn--primary"
            onClick={() => camaraRef.current.click()}
            disabled={sinCupo || preparando}
          >
            Tomar foto
          </button>
          <button
            type="button"
            className="ec-btn ec-btn--secondary"
            onClick={() => archivoRef.current.click()}
            disabled={sinCupo || preparando}
          >
            Subir imagen
          </button>
        </div>
      )}

      <p className="ec-analisis__cupo">
        {sinCupo
          ? `Alcanzaste el límite de ${estadoIA.limite_diario} análisis de hoy.`
          : `Te quedan ${restantes} de ${estadoIA.limite_diario} análisis hoy.`}
      </p>
    </section>
  );
}

CapturaCard.propTypes = {
  estadoIA: PropTypes.shape({
    limite_diario: PropTypes.number.isRequired,
    usados_hoy: PropTypes.number.isRequired,
  }).isRequired,
  camadas: PropTypes.arrayOf(
    PropTypes.shape({
      id_camada: PropTypes.number.isRequired,
      nombre_camada: PropTypes.string.isRequired,
    })
  ).isRequired,
  camadaId: PropTypes.string.isRequired,
  fotoUrl: PropTypes.string,
  preparando: PropTypes.bool.isRequired,
  analizando: PropTypes.bool.isRequired,
  error: PropTypes.string,
  onCamada: PropTypes.func.isRequired,
  onFoto: PropTypes.func.isRequired,
  onAnalizar: PropTypes.func.isRequired,
  onCambiarFoto: PropTypes.func.isRequired,
};

CapturaCard.defaultProps = {
  fotoUrl: '',
  error: '',
};

export default CapturaCard;
