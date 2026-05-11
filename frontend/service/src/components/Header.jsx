import React from 'react';

function Header({ user, onNavigate, onLogout }) {
  return (
    <header className="site-header">
      <button className="brand-mark" type="button" onClick={() => onNavigate('/')}>
        Онлайн-аукцион
      </button>

      {user ? (
        <div className="header-actions">
          <button className="nav-button" type="button" onClick={() => onNavigate('/lots/create')}>
            Создать лот
          </button>
          {user.role === 'ADMIN' && (
            <button className="nav-button" type="button" onClick={() => onNavigate('/admin')}>
              Админ-панель
            </button>
          )}
          <button className="nav-button" type="button" onClick={() => onNavigate('/account')}>
            Профиль
          </button>
          <div className="user-chip">
            <span className="user-chip__label">Вы вошли</span>
            <strong>{user.email}</strong>
          </div>
          <button className="nav-button nav-button-dark" type="button" onClick={onLogout}>
            Выйти
          </button>
        </div>
      ) : (
        <div className="header-actions">
          <button className="nav-button" type="button" onClick={() => onNavigate('/login')}>
            Войти
          </button>
          <button className="nav-button nav-button-dark" type="button" onClick={() => onNavigate('/register')}>
            Регистрация
          </button>
        </div>
      )}
    </header>
  );
}

export default Header;
