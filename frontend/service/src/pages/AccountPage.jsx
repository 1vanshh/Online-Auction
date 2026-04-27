import React, { useEffect, useState } from 'react';

const profileFields = [
  { key: 'email', label: 'Email' },
  { key: 'role', label: 'Role' },
  { key: 'active', label: 'Active' },
  { key: 'banned', label: 'Banned' }
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

  if (form.firstName.trim().length > 100) {
    errors.firstName = 'Maximum 100 characters';
  }

  if (form.lastName.trim().length > 100) {
    errors.lastName = 'Maximum 100 characters';
  }

  if (form.phone.trim()) {
    if (form.phone.trim().length > 30) {
      errors.phone = 'Maximum 30 characters';
    } else if (!/^\+?[0-9]{7,15}$/.test(form.phone.trim())) {
      errors.phone = 'Format: +79991234567';
    }
  }

  if (form.birthDate) {
    const birthDate = new Date(form.birthDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (Number.isNaN(birthDate.getTime()) || birthDate >= today) {
      errors.birthDate = 'Birth date must be in the past';
    }
  }

  if (form.country.trim().length > 100) {
    errors.country = 'Maximum 100 characters';
  }

  if (form.city.trim().length > 100) {
    errors.city = 'Maximum 100 characters';
  }

  if (form.addressLine.trim().length > 255) {
    errors.addressLine = 'Maximum 255 characters';
  }

  if (form.postalCode.trim().length > 20) {
    errors.postalCode = 'Maximum 20 characters';
  }

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

async function refreshAuthSession(refreshToken) {
  const res = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
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

  useEffect(() => {
    setForm(createFormState(user));
    setFieldErrors({});
    setServerError('');
    setSuccessMessage('');
  }, [user]);

  if (!user) {
    return (
      <main className="page">
        <section className="auth-layout">
          <div className="auth-copy">
            <p className="eyebrow">Account</p>
            <h1>Your profile is available after sign in.</h1>
            <p className="lede">
              Log in to view personal data, role and future account settings linked to your auction profile.
            </p>
          </div>

          <div className="auth-card account-empty">
            <p className="account-empty__text">You are currently not signed in.</p>
            <button className="submit-button" type="button" onClick={() => onNavigate('/login')}>
              Login
            </button>
          </div>
        </section>
      </main>
    );
  }

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
        if (data?.fieldErrors) {
          setFieldErrors(data.fieldErrors);
        }

        if (res.status === 401 || res.status === 403) {
          throw new Error('Session expired. Please log in again.');
        }

        throw new Error(data?.message || 'Failed to update profile');
      }

      onUserUpdate(data);
      setSuccessMessage('Profile updated successfully');
    } catch (error) {
      setServerError(error.message || 'Failed to update profile');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="page">
      <section className="auth-layout">
        <div className="auth-copy">
          <p className="eyebrow">Account</p>
          <h1>{user.firstName ? `${user.firstName}, your profile.` : 'Your auction account.'}</h1>
          <p className="lede">
            This page uses the current authenticated user data already returned by the backend auth response.
          </p>
        </div>

        <div className="auth-card">
          <div className="account-summary">
            <div className="account-grid">
              {profileFields.map(({ key, label }) => (
                <div className="account-field" key={key}>
                  <span>{label}</span>
                  <strong>{user[key] !== null && user[key] !== undefined && user[key] !== '' ? String(user[key]) : 'Not specified'}</strong>
                </div>
              ))}
            </div>
          </div>

          <form className="account-form" onSubmit={handleSubmit}>
            <div className="form-grid">
              <label className="field">
                <span>First name</span>
                <input name="firstName" type="text" value={form.firstName} onChange={handleChange} disabled={loading} />
                {fieldErrors.firstName && <small>{fieldErrors.firstName}</small>}
              </label>

              <label className="field">
                <span>Last name</span>
                <input name="lastName" type="text" value={form.lastName} onChange={handleChange} disabled={loading} />
                {fieldErrors.lastName && <small>{fieldErrors.lastName}</small>}
              </label>

              <label className="field">
                <span>Phone</span>
                <input name="phone" type="tel" placeholder="+79991234567" value={form.phone} onChange={handleChange} disabled={loading} />
                {fieldErrors.phone && <small>{fieldErrors.phone}</small>}
              </label>

              <label className="field">
                <span>Birth date</span>
                <input name="birthDate" type="date" value={form.birthDate} onChange={handleChange} disabled={loading} />
                {fieldErrors.birthDate && <small>{fieldErrors.birthDate}</small>}
              </label>

              <label className="field field-wide">
                <span>Country</span>
                <input name="country" type="text" value={form.country} onChange={handleChange} disabled={loading} />
                {fieldErrors.country && <small>{fieldErrors.country}</small>}
              </label>

              <label className="field">
                <span>City</span>
                <input name="city" type="text" value={form.city} onChange={handleChange} disabled={loading} />
                {fieldErrors.city && <small>{fieldErrors.city}</small>}
              </label>

              <label className="field">
                <span>Postal code</span>
                <input name="postalCode" type="text" value={form.postalCode} onChange={handleChange} disabled={loading} />
                {fieldErrors.postalCode && <small>{fieldErrors.postalCode}</small>}
              </label>

              <label className="field field-wide">
                <span>Address</span>
                <input name="addressLine" type="text" value={form.addressLine} onChange={handleChange} disabled={loading} />
                {fieldErrors.addressLine && <small>{fieldErrors.addressLine}</small>}
              </label>
            </div>

            {serverError && <div className="banner banner-error">{serverError}</div>}
            {successMessage && <div className="banner banner-success">{successMessage}</div>}

            <button className="submit-button" type="submit" disabled={loading}>
              {loading ? 'Saving...' : 'Save changes'}
            </button>
          </form>
        </div>
      </section>
    </main>
  );
}

export default AccountPage;
