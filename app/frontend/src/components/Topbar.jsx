function Topbar() {
  return (
    <header className="ec-topbar">
      <div className="ec-topbar__brand">
        <img
          src="/assets/img/logo-eggchecker.png"
          alt="isotipo EggChecker"
          className="ec-topbar__logo"
        />
        <h2 className="ec-topbar__title">EggChecker</h2>
      </div>
      <div className="ec-topbar__actions">
        <button
          type="button"
          className="ec-topbar__icon-btn"
          aria-label="Notificaciones"
        >
          <img
            src="/assets/icons/notifications-icon.svg"
            alt=""
            aria-hidden="true"
            className="ec-topbar__icon"
          />
        </button>
      </div>
    </header>
  );
}

export default Topbar;
