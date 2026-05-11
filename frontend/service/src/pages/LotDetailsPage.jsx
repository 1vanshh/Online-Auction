import React, { useEffect, useMemo, useRef, useState } from 'react';
import { authedRequest, formatDate, formatPrice, formatStatus, minimumBidForLot, requestJson, winnerDisplayName } from '../api.js';

async function loadLotBundle(lotId, signal) {
  const [lotResult, bidsResult, resultResult] = await Promise.all([
    requestJson(`/api/auction/lots/${lotId}`, { signal }),
    requestJson(`/api/bidding/bids/lots/${lotId}`, { signal }),
    requestJson(`/api/bidding/bids/results/${lotId}`, { signal })
  ]);

  if (!lotResult.res.ok) {
    throw new Error(lotResult.data?.message || 'Не удалось загрузить лот');
  }

  if (!lotResult.isJson || !lotResult.data || typeof lotResult.data !== 'object') {
    throw new Error('Не удалось получить JSON лота. Откройте приложение через http://localhost/ или проверьте proxy Vite.');
  }

  if (!bidsResult.res.ok) {
    throw new Error(bidsResult.data?.message || 'Не удалось загрузить ставки');
  }

  if (!bidsResult.isJson || !Array.isArray(bidsResult.data)) {
    throw new Error('Не удалось получить JSON ставок. Откройте приложение через http://localhost/ или проверьте proxy Vite.');
  }

  return {
    lot: lotResult.data,
    bids: bidsResult.data,
    result: resultResult.res.ok && resultResult.isJson ? resultResult.data : null
  };
}

function LotDetailsPage({ lotId, user, token, refreshToken, onNavigate, onAuthRefresh }) {
  const [lot, setLot] = useState(null);
  const [bids, setBids] = useState([]);
  const [result, setResult] = useState(null);
  const [bidAmount, setBidAmount] = useState('');
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const loadVersion = useRef(0);

  const load = async (signal) => {
    const version = loadVersion.current + 1;
    loadVersion.current = version;
    setLoading(true);
    setError('');

    try {
      const bundle = await loadLotBundle(lotId, signal);

      if (signal?.aborted || version !== loadVersion.current) {
        return;
      }

      setLot(bundle.lot);
      setBids(bundle.bids);
      setResult(bundle.result);
      setBidAmount(String(minimumBidForLot(bundle.lot)));
    } catch (requestError) {
      if (!signal?.aborted && version === loadVersion.current) {
        setError(requestError.message || 'Не удалось загрузить лот');
      }
    } finally {
      if (!signal?.aborted && version === loadVersion.current) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal);
    return () => controller.abort();
  }, [lotId]);

  const minimumBid = useMemo(() => minimumBidForLot(lot), [lot]);
  const isSeller = user?.id && lot?.sellerId && String(user.id) === String(lot.sellerId);
  const isAdmin = user?.role === 'ADMIN';
  const canBid = Boolean(user && lot?.status === 'ACTIVE' && !isSeller);
  const canManageLot = Boolean(user && (isSeller || isAdmin));
  const winnerName = winnerDisplayName(lot, result);

  const runLotAction = async (url, options = {}, successText = 'Действие выполнено') => {
    setActionLoading(true);
    setActionError('');
    setSuccessMessage('');

    try {
      const { res, data } = await authedRequest({
        url,
        token,
        refreshToken,
        onAuthRefresh,
        options
      });

      if (!res.ok) {
        throw new Error(data?.message || 'Не удалось выполнить действие');
      }

      setSuccessMessage(successText);
      const bundle = await loadLotBundle(lotId);
      setLot(bundle.lot);
      setBids(bundle.bids);
      setResult(bundle.result);
      setBidAmount(String(minimumBidForLot(bundle.lot)));
      return data;
    } catch (requestError) {
      setActionError(requestError.message || 'Не удалось выполнить действие');
      return null;
    } finally {
      setActionLoading(false);
    }
  };

  const handleBidSubmit = async (event) => {
    event.preventDefault();

    if (!lot) {
      setActionError('Лот ещё загружается. Обновите страницу и попробуйте снова.');
      return;
    }

    const amount = Number(bidAmount);
    if (!Number.isFinite(amount) || amount < minimumBid) {
      setActionError(`Минимальная ставка: ${formatPrice(minimumBid)}`);
      return;
    }

    await runLotAction('/api/bidding/bids', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ lotId: Number(lotId), amount })
    }, 'Ставка принята');
  };

  if (loading) {
    return (
      <main className="page">
        <section className="auction-section">
          <div className="auction-state">Загружаем лот...</div>
        </section>
      </main>
    );
  }

  if (error) {
    return (
      <main className="page">
        <section className="auction-section">
          <div className="banner banner-error">{error}</div>
          <button className="nav-button" type="button" onClick={() => onNavigate('/')}>
            Вернуться в каталог
          </button>
        </section>
      </main>
    );
  }

  if (!lot) {
    return (
      <main className="page">
        <section className="auction-section">
          <div className="auction-state">Лот ещё загружается...</div>
          <div className="card-actions">
            <button className="nav-button" type="button" onClick={() => load()}>
              Загрузить снова
            </button>
            <button className="nav-button" type="button" onClick={() => onNavigate('/')}>
              Вернуться в каталог
            </button>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="page">
      <section className="auction-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Лот #{lot.id}</p>
            <h2>{lot.title}</h2>
          </div>
          <span className={`status-badge status-${String(lot.status || '').toLowerCase()}`}>{formatStatus(lot.status)}</span>
        </div>

        <div className="lot-details-layout">
          <div className="lot-details-main">
            <article className="lot-card lot-card-large details-card">
              {lot.mainImageUrl && (
                <div className="lot-hero-image" style={{ backgroundImage: `url(${lot.mainImageUrl})` }} />
              )}
              <p className="lot-description lot-description-large">{lot.description || 'Описание пока не добавлено.'}</p>

              {lot.status === 'FINISHED' && (
                <div className="winner-strip winner-strip-large">
                  <span>Победитель аукциона</span>
                  <strong>{winnerName || 'Не определён'}</strong>
                </div>
              )}

              <dl className="lot-meta">
                <div>
                  <dt>Категория</dt>
                  <dd>{lot.categoryName || 'Без категории'}</dd>
                </div>
                <div>
                  <dt>Текущая цена</dt>
                  <dd>{formatPrice(lot.currentPrice ?? lot.startPrice)}</dd>
                </div>
                <div>
                  <dt>Начальная цена</dt>
                  <dd>{formatPrice(lot.startPrice)}</dd>
                </div>
                <div>
                  <dt>Шаг ставки</dt>
                  <dd>{formatPrice(lot.bidStep)}</dd>
                </div>
                <div>
                  <dt>Начало</dt>
                  <dd>{formatDate(lot.startTime)}</dd>
                </div>
                <div>
                  <dt>Окончание</dt>
                  <dd>{formatDate(lot.endTime)}</dd>
                </div>
                <div>
                  <dt>Продавец</dt>
                  <dd>{lot.sellerName || `Пользователь #${lot.sellerId}`}</dd>
                </div>
                <div>
                  <dt>Победитель</dt>
                  <dd>{winnerName || 'Пока нет'}</dd>
                </div>
              </dl>
            </article>

            <section className="lot-card lot-card-large">
              <div className="section-heading section-heading-tight">
                <div>
                  <p className="eyebrow">Ставки</p>
                  <h2>История ставок</h2>
                </div>
              </div>

              {bids.length > 0 ? (
                <div className="data-list">
                  {bids.map((bid) => (
                    <div className="data-row" key={bid.id}>
                      <span>Участник #{bid.bidderId}</span>
                      <strong>{formatPrice(bid.amount)}</strong>
                      <small>{formatDate(bid.createdAt)}</small>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="auction-state">Ставок пока нет.</div>
              )}
            </section>
          </div>

          <aside className="lot-details-side">
            <div className="auth-card sticky-card bid-card">
              <p className="eyebrow">Участие</p>
              <h3 className="side-title">Сделать ставку</h3>

              {result && (
                <div className="result-box">
                  <span>Текущий победитель</span>
                  <strong>{winnerName || 'Пока нет победителя'}</strong>
                  <small>{result.finalPrice ? `Итоговая цена: ${formatPrice(result.finalPrice)}` : 'Результат ещё не финальный'}</small>
                  {result.paymentDeadline && <small>Оплатить до: {formatDate(result.paymentDeadline)}</small>}
                  <small>{result.paid ? 'Оплата получена' : 'Оплата ожидается'}</small>
                </div>
              )}

              {!user && (
                <div className="auction-state">
                  Смотреть лот можно без регистрации. Для ставки войдите в аккаунт.
                </div>
              )}

              {user && isSeller && (
                <div className="auction-state">Продавец не может ставить на свой лот.</div>
              )}

              {user && lot.status !== 'ACTIVE' && (
                <div className="auction-state">Ставки доступны только для активных лотов.</div>
              )}

              {canBid && (
                <form onSubmit={handleBidSubmit}>
                  <label className="field">
                    <span>Сумма ставки</span>
                    <input
                      type="number"
                      min={minimumBid}
                      step="0.01"
                      value={bidAmount}
                      onChange={(event) => setBidAmount(event.target.value)}
                      disabled={actionLoading}
                    />
                    <small className="hint-text">Минимум: {formatPrice(minimumBid)}</small>
                  </label>

                  <button className="submit-button" type="submit" disabled={actionLoading}>
                    {actionLoading ? 'Отправляем...' : 'Сделать ставку'}
                  </button>
                </form>
              )}

              {!user && (
                <button className="submit-button" type="button" onClick={() => onNavigate('/login')}>
                  Войти для ставки
                </button>
              )}

              {actionError && <div className="banner banner-error">{actionError}</div>}
              {successMessage && <div className="banner banner-success">{successMessage}</div>}
            </div>

            {canManageLot && (
              <div className="auth-card sticky-card manage-card">
                <p className="eyebrow">Управление</p>
                <h3 className="side-title">Статус лота</h3>
                <div className="card-actions card-actions-column">
                  {lot.status === 'DRAFT' && (
                    <button
                      className="nav-button nav-button-dark"
                      type="button"
                      disabled={actionLoading}
                      onClick={() => runLotAction(`/api/auction/lots/${lot.id}/activate`, { method: 'POST' }, 'Лот активирован')}
                    >
                      Активировать
                    </button>
                  )}
                  {lot.status !== 'FINISHED' && lot.status !== 'CANCELLED' && (
                    <button
                      className="nav-button"
                      type="button"
                      disabled={actionLoading}
                      onClick={() => runLotAction(`/api/auction/lots/${lot.id}/cancel`, { method: 'POST' }, 'Лот отменён')}
                    >
                      Отменить
                    </button>
                  )}
                  {isAdmin && lot.status === 'ACTIVE' && (
                    <button
                      className="nav-button"
                      type="button"
                      disabled={actionLoading}
                      onClick={() => runLotAction(`/api/auction/admin/lots/${lot.id}/finish`, { method: 'POST' }, 'Лот завершён')}
                    >
                      Завершить как админ
                    </button>
                  )}
                </div>
              </div>
            )}
          </aside>
        </div>
      </section>
    </main>
  );
}

export default LotDetailsPage;
