export const getErrorMessage = (data, fallback) => data?.message || fallback;

export const MAX_LOT_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;

export const ALLOWED_LOT_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
export const ALLOWED_LOT_IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'webp', 'gif'];

export const getFileExtension = (fileName = '') => {
  const parts = String(fileName).toLowerCase().split('.');
  return parts.length > 1 ? parts.at(-1) : '';
};

export const validateLotImageFile = (file) => {
  if (!file) {
    return '';
  }

  const type = String(file.type || '').toLowerCase();
  const extension = getFileExtension(file.name);

  if (!ALLOWED_LOT_IMAGE_TYPES.includes(type) || !ALLOWED_LOT_IMAGE_EXTENSIONS.includes(extension)) {
    return 'Допустимые форматы фото: JPG, PNG, WEBP, GIF';
  }

  if (Number(file.size || 0) > MAX_LOT_IMAGE_SIZE_BYTES) {
    return 'Фото должно быть не больше 5 МБ';
  }

  return '';
};

export async function requestJson(url, options = {}) {
  const response = await fetch(url, options);
  const contentType = response.headers.get('content-type') || '';

  if (!contentType.toLowerCase().includes('application/json')) {
    const text = await response.text().catch(() => '');
    const message = text && !text.trim().startsWith('<') ? text : null;
    return {
      res: response,
      data: message ? { message } : null,
      isJson: false
    };
  }

  const data = await response.json().catch(() => null);
  return { res: response, data, isJson: true };
}

export async function refreshAuthSession(refreshToken) {
  return requestJson('/api/auth/refresh', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ refreshToken })
  });
}

export async function authedRequest({ url, token, refreshToken, onAuthRefresh, options = {} }) {
  const call = (accessToken) => {
    const headers = {
      ...(options.headers || {})
    };

    if (accessToken) {
      headers.Authorization = `Bearer ${accessToken}`;
    }

    return requestJson(url, {
      ...options,
      headers
    });
  };

  let result = await call(token);

  if ((result.res.status === 401 || result.res.status === 403) && refreshToken) {
    const refreshResult = await refreshAuthSession(refreshToken);

    if (refreshResult.res.ok && refreshResult.data?.accessToken) {
      onAuthRefresh?.(refreshResult.data);
      result = await call(refreshResult.data.accessToken);
    }
  }

  return result;
}

export const formatPrice = (value) => {
  if (value === null || value === undefined || value === '') {
    return 'Не указано';
  }

  return new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    maximumFractionDigits: 2
  }).format(Number(value));
};

export const formatDate = (value) => {
  if (!value) {
    return 'Не назначено';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }

  return new Intl.DateTimeFormat('ru-RU', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(date);
};

export const statusLabels = {
  DRAFT: 'Черновик',
  ACTIVE: 'Активен',
  FINISHED: 'Завершён',
  CANCELLED: 'Отменён'
};

export const roleLabels = {
  USER: 'Пользователь',
  ADMIN: 'Администратор'
};

export const formatStatus = (status) => statusLabels[status] || status || 'Неизвестно';

export const userDisplayName = (user) => {
  if (!user) {
    return '';
  }

  const name = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
  return name || user.email || `Пользователь #${user.id}`;
};

export const winnerDisplayName = (lot, result) => (
  lot?.winnerName ||
  result?.winnerName ||
  (lot?.winnerId ? `Пользователь #${lot.winnerId}` : '') ||
  (result?.winnerId ? `Пользователь #${result.winnerId}` : '')
);

export const minimumBidForLot = (lot) => {
  const currentPrice = Number(lot?.currentPrice ?? lot?.startPrice ?? 0);
  const bidStep = Number(lot?.bidStep ?? 0);
  return Number((currentPrice + bidStep).toFixed(2));
};

export const toLocalDateTimePayload = (localDateTimeValue) => {
  if (!localDateTimeValue) {
    return localDateTimeValue;
  }

  return localDateTimeValue.length === 16 ? `${localDateTimeValue}:00` : localDateTimeValue;
};
