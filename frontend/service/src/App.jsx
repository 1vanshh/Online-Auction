import React, { useEffect, useState } from 'react';
import Header from './components/Header.jsx';
import HomePage from './pages/HomePage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';

const AUTH_STORAGE_KEY = 'auction-auth';

const getRouteFromHash = () => {
  const route = window.location.hash.replace('#', '');

  if (route === '/login' || route === '/register') {
    return route;
  }

  return '/';
};

const readStoredAuth = () => {
  try {
    const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

function App() {
  const [route, setRoute] = useState(getRouteFromHash);
  const [authData, setAuthData] = useState(readStoredAuth);

  useEffect(() => {
    const handleHashChange = () => {
      setRoute(getRouteFromHash());
    };

    window.addEventListener('hashchange', handleHashChange);

    return () => {
      window.removeEventListener('hashchange', handleHashChange);
    };
  }, []);

  useEffect(() => {
    if (authData) {
      window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authData));
      return;
    }

    window.localStorage.removeItem(AUTH_STORAGE_KEY);
  }, [authData]);

  const navigate = (nextRoute) => {
    window.location.hash = nextRoute;
  };

  const handleAuthSuccess = (payload) => {
    setAuthData(payload);
    navigate('/');
  };

  const handleLogout = () => {
    setAuthData(null);
    navigate('/');
  };

  let page = <HomePage user={authData?.user} onNavigate={navigate} />;

  if (route === '/login') {
    page = <LoginPage onNavigate={navigate} onSuccess={handleAuthSuccess} />;
  }

  if (route === '/register') {
    page = <RegisterPage onNavigate={navigate} onSuccess={handleAuthSuccess} />;
  }

  return (
    <div className="app-shell">
      <Header user={authData?.user} onNavigate={navigate} onLogout={handleLogout} />
      {page}
    </div>
  );
}

export default App;
