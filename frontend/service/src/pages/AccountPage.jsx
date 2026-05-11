import React, { useEffect, useState } from 'react';
import { formatDate, formatPrice, formatStatus, roleLabels, userDisplayName } from '../api.js';

const profileFields = [
  { key: 'email', label: 'Email' },
  { key: 'role', label: 'Роль' },
  { key: 'active', label: 'Активен' },
  { key: 'banned', label: 'Заблокирован' }
];

const createFormState = (user) => ({
  firstName: user?.firstName || '',
  lastName: user?.lastName || '',
  phone: user?.phone || '',
  birthDate: user?.birthDate || '',
  country: user?.country || '',
  city: user?.city || '',
  addressLine: user?.addressLine || '',
  postalCode: user?.postalCode || ''
});

const validateForm = (form) => {
  const errors = {};

  if (form.firstName.trim().length > 100) errors.firstName = 'Максимум 100 символов';
  if (form.lastName.trim().length > 100) errors.lastName = 'Максимум 100 символов';

  if (form.phone.trim()) {
    if (form.phone.trim().length > 30) errors.phone = 'Максимум 30 символов';
    else if (!/^\+?[0-9]{7,15}$/.test(form.phone.trim())) errors.phone = 'Формат: +79991234567';
  }

  if (form.birthDate) {
    const birthDate = new Date(form.birthDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (Number.isNaN(birthDate.getTime()) || birthDate >= today) errors.birthDate = 'Дата рождения должна быть в прошлом';
  }

  if (form.country.trim().length > 100) errors.country = 'Максимум 100 символов';
  if (form.city.trim().length > 100) errors.city = 'Максимум 100 символов';
  if (form.addressLine.trim().length > 255) errors.addressLine = 'Максимум 255 символов';
  if (form.postalCode.trim().length > 20) errors.postalCode = 'Максимум 20 символов';

  return errors;
};

async function updateProfileRequest(payload, accessToken) {
  const res = await fetch('/api/users/me', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`
    },
    body: JSON.stringify(payload)
  });

  const data = await res.json().catch(() => null);
  return { res, data };
}

async function getProfileRequest(accessToken) {
  const res = await fetch('/api/users/me', {
    method: 'GET',
    headers: { Authorization: `Bearer ${accessToken}` }
  });

  const data = await res.json().catch(() => null);
  return { res, data };
}

async function getMyLotsRequest(accessToken) {
  const res = await fetch('/api/auction/lots/my', {
    method: 'GET',
    headers: { Authorization: `Bearer ${accessToken}` }
  });

  const data = await res.json().catch(() => null);
  return { res, data };
}

async function refreshAuthSession(refreshToken) {
  const res = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });

  const data = await res.json().catch(() => null);
  return { res, data };
}

function AccountPage({ token, refreshToken, user, onNavigate, onUserUpdate, onAuthRefresh }) {
  const [form, setForm] = useState(createFormState(user));
  const [fieldErrors, setFieldErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [profileLoading, setProfileLoading] = useState(false);
  const [myLots, setMyLots] = useState([]);
  const [lotsLoading, setLotsLoading] = useState(false);
  const [lotsError, setLotsError] = useState('');

  useEffect(() => {
    setForm(createFormState(user));
    setFieldErrors({});
    setServerError('');
    setSuccessMessage('');
  }, [user]);

  useEffect(() => {
    if (!token || !user) return;

    let cancelled = false;

    const loadProfile = async () => {
      setProfileLoading(true);
      setServerError('');

      try {
        let activeToken = token;
        let { res, data } = await getProfileRequest(activeToken);

        if ((res.status === 401 || res.status === 403) && refreshToken) {
          const refreshResult = await refreshAuthSession(refreshToken);
          if (refreshResult.res.ok && refreshResult.data?.accessToken) {
            activeToken = refreshResult.data.accessToken;
            if (!cancelled) onAuthRefresh(refreshResult.data);
            ({ res, data } = await getProfileRequest(activeToken));
          }
        }

        if (!res.ok) throw new Error(res.status === 401 || res.status === 403 ? 'Сессия истекла. Войдите снова.' : data?.message || 'Не удалось загрузить профиль');
        if (!cancelled) onUserUpdate(data);
      } catch (error) {
        if (!cancelled) setServerError(error.message || 'Не удалось загрузить профиль');
      } finally {
        if (!cancelled) setProfileLoading(false);
      }
    };

    loadProfile();
    return () => { cancelled = true; };
  }, [token, refreshToken, user?.id]);

  useEffect(() => {
    if (!token || !user) return;

    let cancelled = false;

    const loadMyLots = async () => {
      setLotsLoading(true);
      setLotsError('');

      try {
        let activeToken = token;
        let { res, data } = await getMyLotsRequest(activeToken);

        if ((res.status === 401 || res.status === 403) && refreshToken) {
          const refreshResult = await refreshAuthSession(refreshToken);
          if (refreshResult.res.ok && refreshResult.data?.accessToken) {
            activeToken = refreshResult.data.accessToken;
            if (!cancelled) onAuthRefresh(refreshResult.data);
            ({ res, data } = await getMyLotsRequest(activeToken));
          }
        }

        if (!res.ok) throw new Error(res.status === 401 || res.status === 403 ? 'Сессия истекла. Войдите снова.' : data?.message || 'Не удалось загрузить ваши лоты');
        if (!cancelled) setMyLots(Array.isArray(data) ? data : []);
      } catch (error) {
        if (!cancelled) setLotsError(error.message || 'Не удалось загрузить ваши лоты');
      } finally {
        if (!cancelled) setLotsLoading(false);
      }
    };

    loadMyLots();
    return () => { cancelled = true; };
  }, [token, refreshToken, user?.id]);

  if (!user) {
    return (
      <main className="page">
        <section className="auth-layout">
          <div className="auth-copy">
            <p className="eyebrow">Профиль</p>
            <h1>Профиль доступен после входа.</h1>
            <p className="lede">Войдите, чтобы увидеть личные данные, роль и свои лоты.</p>
          </div>

          <div className="auth-card account-empty">
            <p className="account-empty__text">Сейчас вы не авторизованы.</p>
            <button className="submit-button" type="button" onClick={() => onNavigate('/login')}>
              Войти
            </button>
          </div>
        </section>
      </main>
    );
  }

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setFieldErrors((current) => {
      if (!current[name]) return current;
      const next = { ...current };
      delete next[name];
      return next;
    });
    setServerError('');
    setSuccessMessage('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const payload = {
      firstName: form.firstName.trim() || null,
      lastName: form.lastName.trim() || null,
      phone: form.phone.trim() || null,
      birthDate: form.birthDate || null,
      country: form.country.trim() || null,
      city: form.city.trim() || null,
      addressLine: form.addressLine.trim() || null,
      postalCode: form.postalCode.trim() || null
    };

    const validationErrors = validateForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors);
      return;
    }

    setLoading(true);
    setFieldErrors({});
    setServerError('');
    setSuccessMessage('');

    try {
      let activeToken = token;
      let { res, data } = await updateProfileRequest(payload, activeToken);

      if ((res.status === 401 || res.status === 403) && refreshToken) {
        const refreshResult = await refreshAuthSession(refreshToken);
        if (refreshResult.res.ok && refreshResult.data?.accessToken) {
          activeToken = refreshResult.data.accessToken;
          onAuthRefresh(refreshResult.data);
          ({ res, data } = await updateProfileRequest(payload, activeToken));
        }
      }

      if (!res.ok) {
        if (data?.fieldErrors) setFieldErrors(data.fieldErrors);
        throw new Error(res.status === 401 || res.status === 403 ? 'Сессия истекла. Войдите снова.' : data?.message || 'Не удалось обновить профиль');
      }

      onUserUpdate(data);
      setSuccessMessage('Профиль обновлён');
    } catch (error) {
      setServerError(error.message || 'Не удалось обновить профиль');
    } finally {
      setLoading(false);
    }
  };

  const profileValue = (key) => {
    if (key === 'active') return user.active ? 'Да' : 'Нет';
    if (key === 'banned') return user.banned ? 'Да' : 'Нет';
    if (key === 'role') return roleLabels[user.role] || user.role;
    return user[key] || '—';
  };

  return (
    <main className="page">
      <section className="auction-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Профиль</p>
            <h2>{userDisplayName(user)}</h2>
          </div>
          {profileLoading && <p className="section-copy">Обновляем данные...</p>}
        </div>

        <div className="account-summary account-grid">
          {profileFields.map((field) => (
            <div className="account-field" key={field.key}>
              <span>{field.label}</span>
              <strong>{profileValue(field.key)}</strong>
            </div>
          ))}
        </div>

        <form className="auth-card account-form" onSubmit={handleSubmit}>
          <p className="eyebrow">Личные данные</p>
          <div className="form-grid">
            <label className="field">
              <span>Имя</span>
              <input name="firstName" value={form.firstName} onChange={handleChange} disabled={loading} />
              {fieldErrors.firstName && <small>{fieldErrors.firstName}</small>}
            </label>
            <label className="field">
              <span>Фамилия</span>
              <input name="lastName" value={form.lastName} onChange={handleChange} disabled={loading} />
              {fieldErrors.lastName && <small>{fieldErrors.lastName}</small>}
            </label>
            <label className="field">
              <span>Телефон</span>
              <input name="phone" value={form.phone} onChange={handleChange} disabled={loading} />
              {fieldErrors.phone && <small>{fieldErrors.phone}</small>}
            </label>
            <label className="field">
              <span>Дата рождения</span>
              <input name="birthDate" type="date" value={form.birthDate} onChange={handleChange} disabled={loading} />
              {fieldErrors.birthDate && <small>{fieldErrors.birthDate}</small>}
            </label>
            <label className="field">
              <span>Страна</span>
              <input name="country" value={form.country} onChange={handleChange} disabled={loading} />
              {fieldErrors.country && <small>{fieldErrors.country}</small>}
            </label>
            <label className="field">
              <span>Город</span>
              <input name="city" value={form.city} onChange={handleChange} disabled={loading} />
              {fieldErrors.city && <small>{fieldErrors.city}</small>}
            </label>
            <label className="field field-wide">
              <span>Адрес</span>
              <input name="addressLine" value={form.addressLine} onChange={handleChange} disabled={loading} />
              {fieldErrors.addressLine && <small>{fieldErrors.addressLine}</small>}
            </label>
            <label className="field">
              <span>Индекс</span>
              <input name="postalCode" value={form.postalCode} onChange={handleChange} disabled={loading} />
              {fieldErrors.postalCode && <small>{fieldErrors.postalCode}</small>}
            </label>
          </div>

          {serverError && <div className="banner banner-error">{serverError}</div>}
          {successMessage && <div className="banner banner-success">{successMessage}</div>}

          <button className="submit-button" type="submit" disabled={loading}>
            {loading ? 'Сохраняем...' : 'Сохранить профиль'}
          </button>
        </form>
      </section>

      <section className="account-lots-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Мои лоты</p>
            <h2>Созданные вами лоты</h2>
          </div>
        </div>

        {lotsLoading && <div className="auction-state">Загружаем ваши лоты...</div>}
        {lotsError && <div className="banner banner-error">{lotsError}</div>}

        {!lotsLoading && !lotsError && (
          myLots.length > 0 ? (
            <div className="lot-grid">
              {myLots.map((lot) => (
                <article className={`lot-card${lot.mainImageUrl ? ' lot-card-with-image' : ''}`} key={lot.id}>
                  {lot.mainImageUrl && <div className="lot-card__image" style={{ backgroundImage: `url(${lot.mainImageUrl})` }} />}
                  <div className="lot-card__content">
                    <div className="lot-card__top">
                      <span className={`status-badge status-${String(lot.status || '').toLowerCase()}`}>{formatStatus(lot.status)}</span>
                      <span className="lot-category">{lot.categoryName || 'Без категории'}</span>
                    </div>
                    <h3>{lot.title}</h3>
                    <p className="lot-description">{lot.description || 'Описание пока не добавлено.'}</p>
                    <dl className="lot-meta">
                      <div><dt>Текущая цена</dt><dd>{formatPrice(lot.currentPrice ?? lot.startPrice)}</dd></div>
                      <div><dt>Окончание</dt><dd>{formatDate(lot.endTime)}</dd></div>
                      <div><dt>Шаг ставки</dt><dd>{formatPrice(lot.bidStep)}</dd></div>
                      <div><dt>Победитель</dt><dd>{lot.winnerName || 'Пока нет'}</dd></div>
                    </dl>
                    <div className="card-actions">
                      <button className="nav-button nav-button-dark" type="button" onClick={() => onNavigate(`/lots/${lot.id}`)}>Открыть</button>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <div className="auction-state">Вы ещё не создали ни одного лота.</div>
          )
        )}
      </section>
    </main>
  );
}

export default AccountPage;
