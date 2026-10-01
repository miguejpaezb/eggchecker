import PropTypes from 'prop-types';

function ModulePlaceholder({ title }) {
  return (
    <div>
      <div className="ec-page-head">
        <div>
          <h1 className="ec-page-title">{title}</h1>
          <p className="ec-page-subtitle">Módulo en construcción.</p>
        </div>
      </div>
      <div className="ec-glass-card ec-placeholder">
        <p className="mb-0">
          Este módulo todavía no está implementado. La estructura de navegación
          ya está lista para conectarlo.
        </p>
      </div>
    </div>
  );
}

ModulePlaceholder.propTypes = {
  title: PropTypes.string.isRequired,
};

export default ModulePlaceholder;
