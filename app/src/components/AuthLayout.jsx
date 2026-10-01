import PropTypes from 'prop-types';

function AuthLayout({ children, register }) {
  return (
    <div className="ec-auth">
      <div className="ec-auth__pattern" aria-hidden="true" />
      <div
        className={`ec-auth__content${register ? ' ec-auth__content--register' : ''}`}
      >
        <div className="ec-auth__row">
          <div className="ec-auth__brand">
            <img
              src="/assets/img/logo-eggchecker.png"
              alt="isotipo EggChecker"
              className="ec-auth__logo"
            />
            <h1 className="ec-auth__title">EggChecker</h1>
            <p className="ec-auth__tagline">Cada huevo cuenta</p>
          </div>
          <div className="ec-auth__panel">{children}</div>
        </div>
      </div>
    </div>
  );
}

AuthLayout.propTypes = {
  children: PropTypes.node.isRequired,
  register: PropTypes.bool,
};

AuthLayout.defaultProps = {
  register: false,
};

export default AuthLayout;
