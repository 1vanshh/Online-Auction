import React, { useState } from 'react';

const initialForm = {
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  phone: ''
};

const fieldLabels = {
  firstName: 'Имя',
  lastName: 'Фамилия',
  email: 'Email',
  password: 'Пароль',
  phone: 'Телефон'
};

const validateForm = (form) => {
  const nextErrors = {};

  if (!form.firstName.trim()) {
    nextErrors.firstName = 'Укажите имя';
  } else if (form.firstName.trim().length < 2) {
    nextErrors.firstName = 'Минимум 2 символа';
  } else if (form.firstName.trim().length > 100) {
    nextErrors.firstName = 'Максимум 100 символов';
  }

  if (!form.lastName.trim()) {
    nextErrors.lastName = 'Укажите фамилию';
  } else if (form.lastName.trim().length < 2) {
    nextErrors.lastName = 'Минимум 2 символа';
  } else if (form.lastName.trim().length > 100) {
    nextErrors.lastName = 'Максимум 100 символов';
  }

  if (!form.email.trim()) {
    nextErrors.email = 'Укажите email';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    nextErrors.email = 'Некорректный email';
  } else if (form.email.trim().length > 255) {
    nextErrors.email = 'Максимум 255 символов';
  }

  if (!form.password) {
    nextErrors.password = 'Укажите пароль';
  } else if (form.password.length < 6) {
    nextErrors.password = 'Минимум 6 символов';
  } else if (form.password.length > 255) {
    nextErrors.password = 'Максимум 255 символов';
  }

  if (form.phone.trim()) {
    if (form.phone.trim().length > 30) {
      nextErrors.phone = 'Максимум 30 символов';
    } else if (!/^\+?[0-9]{7,15}$/.test(form.phone.trim())) {
      nextErrors.phone = 'Формат: +79991234567';
    }
  }

  return nextErrors;
};

function RegisterPage({ onNavigate, onSuccess }) {
  const [form, setForm] = useState(initialForm);
  const [fieldErrors, setFieldErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [response, setResponse] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((current) => ({
      ...current,
      [name]: value
    }));

    setFieldErrors((current) => {
      if (!current[name]) {
        return current;
      }

      const next = { ...current };
      delete next[name];
      return next;
    });

    setServerError('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const trimmedForm = {
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      email: form.email.trim(),
      password: form.password,
      phone: form.phone.trim()
    };

    const validationErrors = validateForm(trimmedForm);
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors);
      setResponse(null);
      return;
    }

    setLoading(true);
    setFieldErrors({});
    setServerError('');
    setResponse(null);

    try {
      const payload = {
        ...trimmedForm,
        phone: trimmedForm.phone || null
      };

      const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });

      const data = await res.json().catch(() => null);

      if (!res.ok) {
        if (data?.fieldErrors) {
          setFieldErrors(data.fieldErrors);
        }

        throw new Error(data?.message || 'Не удалось выполнить регистрацию');
      }

      setResponse(data);
      setForm(initialForm);
      onSuccess(data);
    } catch (error) {
      setServerError(error.message || 'Не удалось выполнить регистрацию');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="page">
      <section className="auth-layout">
        <div className="auth-copy">
          <p className="eyebrow">Регистрация</p>
          <h1>Создайте аккаунт для участия в торгах.</h1>
          <p className="lede">
            После регистрации вы сможете участвовать в торгах, создавать лоты и отслеживать свои ставки.
          </p>
        </div>

        <form className="auth-card" onSubmit={handleSubmit}>
          <div className="form-grid">
            <label className="field">
              <span>{fieldLabels.firstName}</span>
              <input
                name="firstName"
                type="text"
                placeholder="Иван"
                value={form.firstName}
                onChange={handleChange}
                disabled={loading}
              />
              {fieldErrors.firstName && <small>{fieldErrors.firstName}</small>}
            </label>

            <label className="field">
              <span>{fieldLabels.lastName}</span>
              <input
                name="lastName"
                type="text"
                placeholder="Иванов"
                value={form.lastName}
                onChange={handleChange}
                disabled={loading}
              />
              {fieldErrors.lastName && <small>{fieldErrors.lastName}</small>}
            </label>

            <label className="field field-wide">
              <span>{fieldLabels.email}</span>
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

            <label className="field field-wide">
              <span>{fieldLabels.password}</span>
              <input
                name="password"
                type="password"
                placeholder="Минимум 6 символов"
                value={form.password}
                onChange={handleChange}
                disabled={loading}
              />
              {fieldErrors.password && <small>{fieldErrors.password}</small>}
            </label>

            <label className="field field-wide">
              <span>{fieldLabels.phone}</span>
              <input
                name="phone"
                type="tel"
                placeholder="+79991234567"
                value={form.phone}
                onChange={handleChange}
                disabled={loading}
              />
              {fieldErrors.phone && <small>{fieldErrors.phone}</small>}
            </label>
          </div>

          {serverError && <div className="banner banner-error">{serverError}</div>}

          {response && (
            <div className="banner banner-success">
              <strong>Регистрация завершена.</strong>
              <span>
                {response.user?.email || response.user?.firstName || 'Пользователь создан'}
              </span>
            </div>
          )}

          <button className="submit-button" type="submit" disabled={loading}>
            {loading ? 'Создаём аккаунт...' : 'Зарегистрироваться'}
          </button>

          <p className="auth-footnote">
            Уже есть аккаунт?{' '}
            <button className="inline-link" type="button" onClick={() => onNavigate('/login')}>
              Войти
            </button>
          </p>
        </form>
      </section>
    </main>
  );
}

export default RegisterPage;
