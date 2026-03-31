import React from 'react';

function HomePage({ user, onNavigate }) {
  return (
    <main className="page">
      <section className="hero-panel">
        <p className="eyebrow">Minimal bidding platform</p>
        <h1>{user ? `Welcome back, ${user.firstName || user.email}` : 'Bid on what matters.'}</h1>
        <p className="lede">
          Главная страница пока содержит только шапку и базовый hero-блок. Дальше сюда можно
          добавить список аукционов, поиск и карточки лотов.
        </p>

        {!user && (
          <div className="hero-actions">
            <button className="nav-button nav-button-dark" type="button" onClick={() => onNavigate('/register')}>
              Create account
            </button>
            <button className="nav-button" type="button" onClick={() => onNavigate('/login')}>
              Login
            </button>
          </div>
        )}
      </section>
    </main>
  );
}

export default HomePage;
