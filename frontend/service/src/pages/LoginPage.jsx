import React, { useState } from 'react';

const initialForm = {
  email: '',
  password: ''
};

const validateForm = (form) => {
  const errors = {};

  if (!form.email.trim()) {
    errors.email = 'Укажите email';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    errors.email = 'Некорректный email';
  }

  if (!form.password) {
    errors.password = 'Укажите пароль';
  }

  return errors;
};

function LoginPage({ onNavigate, onSuccess }) {
  const [form, setForm] = useState(initialForm);
  const [fieldErrors, setFieldErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((current) => ({
      ...current,
      [name]: value
    }));

    setFieldErrors((current) => {
      if (!current[name]) return current;
      const next = { ...current };
      delete next[name];
      return next;
    });

    setServerError('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const payload = {
      email: form.email.trim(),
      password: form.password
    };

    const validationErrors = validateForm(payload);
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors);
      return;
    }

    setLoading(true);
    setFieldErrors({});
    setServerError('');

    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await res.json().catch(() => null);

      if (!res.ok) {
        if (data?.fieldErrors) setFieldErrors(data.fieldErrors);
        throw new Error(data?.message || 'Не удалось войти');
      }

      onSuccess(data);
    } catch (error) {
      setServerError(error.message || 'Не удалось войти');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="page">
      <section className="auth-layout">
        <div className="auth-copy">
          <p className="eyebrow">Вход</p>
          <h1>Войдите в аккаунт.</h1>
          <p className="lede">После входа вы сможете делать ставки, создавать лоты и управлять своим профилем.</p>
        </div>

        <form className="auth-card" onSubmit={handleSubmit}>
          <label className="field">
            <span>Email</span>
            <input
              name="email"
              type="email"
              placeholder="name@example.com"
              value={form.email}
              onChange={handleChange}
              disabled={loading}
            />
            {fieldErrors.email && <small>{fieldErrors.email}</small>}
          </label>

          <label className="field">
            <span>Пароль</span>
            <input
              name="password"
              type="password"
              placeholder="Ваш пароль"
              value={form.password}
              onChange={handleChange}
              disabled={loading}
            />
            {fieldErrors.password && <small>{fieldErrors.password}</small>}
          </label>

          {serverError && <div className="banner banner-error">{serverError}</div>}

          <button className="submit-button" type="submit" disabled={loading}>
            {loading ? 'Входим...' : 'Войти'}
          </button>

          <p className="auth-footnote">
            Нет аккаунта?{' '}
            <button className="inline-link" type="button" onClick={() => onNavigate('/register')}>
              Зарегистрироваться
            </button>
          </p>
        </form>
      </section>
    </main>
  );
}

export default LoginPage;
