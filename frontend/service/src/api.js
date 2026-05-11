export const getErrorMessage = (data, fallback) => data?.message || fallback;

export async function requestJson(url, options = {}) {
  const response = await fetch(url, options);
  const data = await response.json().catch(() => null);
  return { res: response, data };
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
    return 'Not specified';
  }

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 2
  }).format(Number(value));
};

export const formatDate = (value) => {
  if (!value) {
    return 'Not scheduled';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }

  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(date);
};

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
