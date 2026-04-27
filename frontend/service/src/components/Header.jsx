import React from 'react';

function Header({ user, onNavigate, onLogout }) {
  return (
    <header className="site-header">
      <button className="brand-mark" type="button" onClick={() => onNavigate('/')}>
        Online Auction
      </button>

      {user ? (
        <div className="header-actions">
          <button className="nav-button" type="button" onClick={() => onNavigate('/account')}>
            Account
          </button>
          <div className="user-chip">
            <span className="user-chip__label">Signed in</span>
            <strong>{user.email}</strong>
          </div>
          <button className="nav-button nav-button-dark" type="button" onClick={onLogout}>
            Logout
          </button>
        </div>
      ) : (
        <div className="header-actions">
          <button className="nav-button" type="button" onClick={() => onNavigate('/login')}>
            Login
          </button>
          <button className="nav-button nav-button-dark" type="button" onClick={() => onNavigate('/register')}>
            Register
          </button>
        </div>
      )}
    </header>
  );
}

export default Header;
