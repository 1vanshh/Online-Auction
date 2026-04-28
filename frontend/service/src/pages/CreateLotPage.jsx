import React, { useEffect, useState } from 'react';

const initialForm = {
  title: '',
  description: '',
  categoryId: '',
  startPrice: '',
  bidStep: '',
  endTime: ''
};

const toLocalDateTimeValue = () => {
  const nextHour = new Date(Date.now() + 60 * 60 * 1000);
  nextHour.setSeconds(0, 0);
  const offset = nextHour.getTimezoneOffset();
  const localDate = new Date(nextHour.getTime() - offset * 60 * 1000);
  return localDate.toISOString().slice(0, 16);
};

const validateForm = (form) => {
  const errors = {};

  if (!form.title.trim()) {
    errors.title = 'Title is required';
  } else if (form.title.trim().length < 3) {
    errors.title = 'Minimum 3 characters';
  } else if (form.title.trim().length > 255) {
    errors.title = 'Maximum 255 characters';
  }

  if (!form.categoryId) {
    errors.categoryId = 'Choose a category';
  }

  if (!form.startPrice) {
    errors.startPrice = 'Start price is required';
  } else if (Number(form.startPrice) <= 0) {
    errors.startPrice = 'Value must be greater than 0';
  }

  if (!form.bidStep) {
    errors.bidStep = 'Bid step is required';
  } else if (Number(form.bidStep) <= 0) {
    errors.bidStep = 'Value must be greater than 0';
  }

  if (!form.endTime) {
    errors.endTime = 'End time is required';
  } else {
    const endTime = new Date(form.endTime);
    if (Number.isNaN(endTime.getTime()) || endTime <= new Date()) {
      errors.endTime = 'Choose a future date and time';
    }
  }

  return errors;
};

async function loadCategories(signal) {
  const res = await fetch('/api/auction/categories', { signal });
  const data = await res.json().catch(() => []);
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

async function createLotRequest(payload, accessToken) {
  const res = await fetch('/api/auction/lots', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`
    },
    body: JSON.stringify(payload)
  });

  const data = await res.json().catch(() => null);
  return { res, data };
}

function CreateLotPage({ token, refreshToken, user, onNavigate, onAuthRefresh }) {
  const [form, setForm] = useState({ ...initialForm, endTime: toLocalDateTimeValue() });
  const [categories, setCategories] = useState([]);
  const [fieldErrors, setFieldErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [categoriesLoading, setCategoriesLoading] = useState(true);

  useEffect(() => {
    if (!user) {
      return;
    }

    const controller = new AbortController();

    const fetchCategories = async () => {
      setCategoriesLoading(true);
      setServerError('');

      try {
        const { res, data } = await loadCategories(controller.signal);
        if (!res.ok) {
          throw new Error(data?.message || 'Failed to load categories');
        }

        setCategories(Array.isArray(data) ? data : []);
      } catch (error) {
        if (error.name !== 'AbortError') {
          setServerError(error.message || 'Failed to load categories');
        }
      } finally {
        setCategoriesLoading(false);
      }
    };

    fetchCategories();

    return () => {
      controller.abort();
    };
  }, [user]);

  if (!user) {
    return (
      <main className="page">
        <section className="auth-layout">
          <div className="auth-copy">
            <p className="eyebrow">Create lot</p>
            <h1>Publish items after sign in.</h1>
            <p className="lede">
              Creating a lot is available only for authenticated users because the auction backend links each lot to a seller.
            </p>
          </div>

          <div className="auth-card account-empty">
            <p className="account-empty__text">Sign in to create and manage your auction lots.</p>
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

    const validationErrors = validateForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors);
      return;
    }

    const payload = {
      title: form.title.trim(),
      description: form.description.trim() || null,
      categoryId: Number(form.categoryId),
      startPrice: Number(form.startPrice),
      bidStep: Number(form.bidStep),
      endTime: new Date(form.endTime).toISOString()
    };

    setLoading(true);
    setFieldErrors({});
    setServerError('');
    setSuccessMessage('');

    try {
      let activeToken = token;
      let { res, data } = await createLotRequest(payload, activeToken);

      if ((res.status === 401 || res.status === 403) && refreshToken) {
        const refreshResult = await refreshAuthSession(refreshToken);
        if (refreshResult.res.ok && refreshResult.data?.accessToken) {
          activeToken = refreshResult.data.accessToken;
          onAuthRefresh(refreshResult.data);
          ({ res, data } = await createLotRequest(payload, activeToken));
        }
      }

      if (!res.ok) {
        if (data?.fieldErrors) {
          setFieldErrors(data.fieldErrors);
        }

        if (res.status === 401 || res.status === 403) {
          throw new Error('Session expired. Please log in again.');
        }

        throw new Error(data?.message || 'Failed to create lot');
      }

      setSuccessMessage('Lot created successfully');
      onNavigate('/');
    } catch (error) {
      setServerError(error.message || 'Failed to create lot');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="page">
      <section className="auth-layout create-lot-layout">
        <div className="auth-copy">
          <p className="eyebrow">Create lot</p>
          <h1>Publish a new auction item.</h1>
          <p className="lede">
            Fill in the core sale parameters: category, opening price, bid step and the auction end date.
          </p>
        </div>

        <form className="auth-card" onSubmit={handleSubmit}>
          <div className="form-grid">
            <label className="field field-wide">
              <span>Title</span>
              <input
                name="title"
                type="text"
                placeholder="Vintage mechanical watch"
                value={form.title}
                onChange={handleChange}
                disabled={loading || categoriesLoading}
              />
              {fieldErrors.title && <small>{fieldErrors.title}</small>}
            </label>

            <label className="field field-wide">
              <span>Description</span>
              <input
                name="description"
                type="text"
                placeholder="Short description of the item"
                value={form.description}
                onChange={handleChange}
                disabled={loading || categoriesLoading}
              />
              {fieldErrors.description && <small>{fieldErrors.description}</small>}
            </label>

            <label className="field">
              <span>Category</span>
              <select
                name="categoryId"
                value={form.categoryId}
                onChange={handleChange}
                disabled={loading || categoriesLoading}
              >
                <option value="">Select category</option>
                {categories.map((category) => (
                  <option key={category.id} value={String(category.id)}>
                    {category.name}
                  </option>
                ))}
              </select>
              {fieldErrors.categoryId && <small>{fieldErrors.categoryId}</small>}
            </label>

            <label className="field">
              <span>End time</span>
              <input
                name="endTime"
                type="datetime-local"
                value={form.endTime}
                onChange={handleChange}
                disabled={loading || categoriesLoading}
              />
              {fieldErrors.endTime && <small>{fieldErrors.endTime}</small>}
            </label>

            <label className="field">
              <span>Start price</span>
              <input
                name="startPrice"
                type="number"
                min="0.01"
                step="0.01"
                placeholder="100.00"
                value={form.startPrice}
                onChange={handleChange}
                disabled={loading || categoriesLoading}
              />
              {fieldErrors.startPrice && <small>{fieldErrors.startPrice}</small>}
            </label>

            <label className="field">
              <span>Bid step</span>
              <input
                name="bidStep"
                type="number"
                min="0.01"
                step="0.01"
                placeholder="5.00"
                value={form.bidStep}
                onChange={handleChange}
                disabled={loading || categoriesLoading}
              />
              {fieldErrors.bidStep && <small>{fieldErrors.bidStep}</small>}
            </label>
          </div>

          {categoriesLoading && <div className="banner">Loading categories...</div>}
          {serverError && <div className="banner banner-error">{serverError}</div>}
          {successMessage && <div className="banner banner-success">{successMessage}</div>}

          <button className="submit-button" type="submit" disabled={loading || categoriesLoading}>
            {loading ? 'Publishing...' : 'Create lot'}
          </button>
        </form>
      </section>
    </main>
  );
}

export default CreateLotPage;
