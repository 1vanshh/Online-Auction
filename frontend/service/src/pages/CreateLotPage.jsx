import React, { useEffect, useState } from 'react';
import { toLocalDateTimePayload, validateLotImageFile } from '../api.js';

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

const validateForm = (form, imageFile) => {
  const errors = {};

  if (!form.title.trim()) {
    errors.title = 'Укажите название';
  } else if (form.title.trim().length < 3) {
    errors.title = 'Минимум 3 символа';
  } else if (form.title.trim().length > 255) {
    errors.title = 'Максимум 255 символов';
  }

  if (!form.categoryId) {
    errors.categoryId = 'Выберите категорию';
  }

  if (!form.startPrice) {
    errors.startPrice = 'Укажите начальную цену';
  } else if (Number(form.startPrice) <= 0) {
    errors.startPrice = 'Значение должно быть больше 0';
  }

  if (!form.bidStep) {
    errors.bidStep = 'Укажите шаг ставки';
  } else if (Number(form.bidStep) <= 0) {
    errors.bidStep = 'Значение должно быть больше 0';
  }

  if (!form.endTime) {
    errors.endTime = 'Укажите дату и время окончания';
  } else {
    const endTime = new Date(form.endTime);
    if (Number.isNaN(endTime.getTime()) || endTime <= new Date()) {
      errors.endTime = 'Выберите будущую дату';
    }
  }

  const imageError = validateLotImageFile(imageFile);
  if (imageError) {
    errors.imageFile = imageError;
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

async function uploadLotImageRequest(imageFile, accessToken) {
  const body = new FormData();
  body.append('image', imageFile);

  const res = await fetch('/api/auction/lots/images', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`
    },
    body
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
  const [imageFile, setImageFile] = useState(null);
  const [imagePreviewUrl, setImagePreviewUrl] = useState('');
  const [categories, setCategories] = useState([]);
  const [fieldErrors, setFieldErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [categoriesLoading, setCategoriesLoading] = useState(true);

  useEffect(() => {
    if (!imageFile) {
      setImagePreviewUrl('');
      return undefined;
    }

    const nextPreviewUrl = URL.createObjectURL(imageFile);
    setImagePreviewUrl(nextPreviewUrl);

    return () => {
      URL.revokeObjectURL(nextPreviewUrl);
    };
  }, [imageFile]);

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
          throw new Error(data?.message || 'Не удалось загрузить категории');
        }

        setCategories(Array.isArray(data) ? data : []);
      } catch (error) {
        if (error.name !== 'AbortError') {
          setServerError(error.message || 'Не удалось загрузить категории');
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
            <p className="eyebrow">Создание лота</p>
            <h1>Размещение доступно после входа.</h1>
            <p className="lede">Лот привязывается к вашему аккаунту, поэтому создать его может только авторизованный пользователь.</p>
          </div>

          <div className="auth-card account-empty">
            <p className="account-empty__text">Войдите, чтобы создать и управлять своими лотами.</p>
            <button className="submit-button" type="button" onClick={() => onNavigate('/login')}>
              Войти
            </button>
          </div>
        </section>
      </main>
    );
  }

  const clearFieldError = (fieldName) => {
    setFieldErrors((current) => {
      if (!current[fieldName]) {
        return current;
      }

      const next = { ...current };
      delete next[fieldName];
      return next;
    });
  };

  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((current) => ({
      ...current,
      [name]: value
    }));

    clearFieldError(name);
    setServerError('');
    setSuccessMessage('');
  };

  const handleImageChange = (event) => {
    const file = event.target.files?.[0] || null;
    const imageError = validateLotImageFile(file);

    setImageFile(file);
    setFieldErrors((current) => {
      const next = { ...current };
      if (imageError) {
        next.imageFile = imageError;
      } else {
        delete next.imageFile;
      }
      return next;
    });
    setServerError('');
    setSuccessMessage('');
  };

  const refreshTokenIfPossible = async () => {
    if (!refreshToken) {
      return null;
    }

    const refreshResult = await refreshAuthSession(refreshToken);
    if (!refreshResult.res.ok || !refreshResult.data?.accessToken) {
      return null;
    }

    onAuthRefresh(refreshResult.data);
    return refreshResult.data.accessToken;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const validationErrors = validateForm(form, imageFile);
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors);
      return;
    }

    setLoading(true);
    setFieldErrors({});
    setServerError('');
    setSuccessMessage(imageFile ? 'Загружаем фото...' : 'Создаём лот...');

    try {
      let activeToken = token;
      let mainImageUrl = null;

      if (imageFile) {
        let { res, data } = await uploadLotImageRequest(imageFile, activeToken);

        if ((res.status === 401 || res.status === 403) && refreshToken) {
          const refreshedToken = await refreshTokenIfPossible();
          if (refreshedToken) {
            activeToken = refreshedToken;
            ({ res, data } = await uploadLotImageRequest(imageFile, activeToken));
          }
        }

        if (!res.ok) {
          if (res.status === 401 || res.status === 403) {
            throw new Error('Сессия истекла. Войдите снова.');
          }

          throw new Error(data?.message || 'Не удалось загрузить фото');
        }

        mainImageUrl = data?.imageUrl || null;
      }

      const payload = {
        title: form.title.trim(),
        description: form.description.trim() || null,
        categoryId: Number(form.categoryId),
        startPrice: Number(form.startPrice),
        bidStep: Number(form.bidStep),
        endTime: toLocalDateTimePayload(form.endTime),
        mainImageUrl
      };

      setSuccessMessage('Создаём лот...');
      let { res, data } = await createLotRequest(payload, activeToken);

      if ((res.status === 401 || res.status === 403) && refreshToken) {
        const refreshedToken = await refreshTokenIfPossible();
        if (refreshedToken) {
          activeToken = refreshedToken;
          ({ res, data } = await createLotRequest(payload, activeToken));
        }
      }

      if (!res.ok) {
        if (data?.fieldErrors) {
          setFieldErrors(data.fieldErrors);
        }

        if (res.status === 401 || res.status === 403) {
          throw new Error('Сессия истекла. Войдите снова.');
        }

        throw new Error(data?.message || 'Не удалось создать лот');
      }

      setSuccessMessage('Лот создан');
      onNavigate(`/lots/${data.id}`);
    } catch (error) {
      setServerError(error.message || 'Не удалось создать лот');
      setSuccessMessage('');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="page">
      <section className="auth-layout create-lot-layout">
        <div className="auth-copy">
          <p className="eyebrow">Новый лот</p>
          <h1>Опубликуйте предмет для торгов.</h1>
          <p className="lede">
            Укажите категорию, стартовую цену, шаг ставки, время завершения и выберите фото с компьютера. Файл сохранится в хранилище сервиса, а ссылка на него — в БД.
          </p>
        </div>

        <form className="auth-card" onSubmit={handleSubmit}>
          <div className="form-grid">
            <label className="field field-wide">
              <span>Название</span>
              <input
                name="title"
                type="text"
                placeholder="Винтажные часы"
                value={form.title}
                onChange={handleChange}
                disabled={loading || categoriesLoading}
              />
              {fieldErrors.title && <small>{fieldErrors.title}</small>}
            </label>

            <label className="field field-wide">
              <span>Описание</span>
              <textarea
                name="description"
                placeholder="Состояние, комплектация, особенности"
                value={form.description}
                onChange={handleChange}
                disabled={loading || categoriesLoading}
              />
              {fieldErrors.description && <small>{fieldErrors.description}</small>}
            </label>

            <label className="field field-wide">
              <span>Фото лота</span>
              <input
                className="file-input"
                name="imageFile"
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                onChange={handleImageChange}
                disabled={loading || categoriesLoading}
              />
              {imagePreviewUrl && (
                <div className="image-preview" style={{ backgroundImage: `url(${imagePreviewUrl})` }}>
                  <span>Выбранное фото</span>
                </div>
              )}
              {fieldErrors.imageFile ? (
                <small>{fieldErrors.imageFile}</small>
              ) : (
                <small className="hint-text">JPG, PNG, WEBP или GIF, до 5 МБ. Можно оставить без фото.</small>
              )}
            </label>

            <label className="field">
              <span>Категория</span>
              <select
                name="categoryId"
                value={form.categoryId}
                onChange={handleChange}
                disabled={loading || categoriesLoading}
              >
                <option value="">Выберите категорию</option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
              {fieldErrors.categoryId && <small>{fieldErrors.categoryId}</small>}
            </label>

            <label className="field">
              <span>Окончание</span>
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
              <span>Начальная цена</span>
              <input
                name="startPrice"
                type="number"
                min="0.01"
                step="0.01"
                placeholder="1000"
                value={form.startPrice}
                onChange={handleChange}
                disabled={loading || categoriesLoading}
              />
              {fieldErrors.startPrice && <small>{fieldErrors.startPrice}</small>}
            </label>

            <label className="field">
              <span>Шаг ставки</span>
              <input
                name="bidStep"
                type="number"
                min="0.01"
                step="0.01"
                placeholder="100"
                value={form.bidStep}
                onChange={handleChange}
                disabled={loading || categoriesLoading}
              />
              {fieldErrors.bidStep && <small>{fieldErrors.bidStep}</small>}
            </label>
          </div>

          {serverError && <div className="banner banner-error">{serverError}</div>}
          {successMessage && <div className="banner banner-success">{successMessage}</div>}

          <button className="submit-button" type="submit" disabled={loading || categoriesLoading}>
            {loading ? 'Сохраняем...' : 'Создать лот'}
          </button>
        </form>
      </section>
    </main>
  );
}

export default CreateLotPage;
