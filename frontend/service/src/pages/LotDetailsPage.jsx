import React, { useEffect, useMemo, useState } from 'react';
import { authedRequest, formatDate, formatPrice, minimumBidForLot, requestJson } from '../api.js';

async function loadLotBundle(lotId, signal) {
  const [lotResult, bidsResult, resultResult] = await Promise.all([
    requestJson(`/api/auction/lots/${lotId}`, { signal }),
    requestJson(`/api/bidding/bids/lots/${lotId}`, { signal }),
    requestJson(`/api/bidding/bids/results/${lotId}`, { signal })
  ]);

  if (!lotResult.res.ok) {
    throw new Error(lotResult.data?.message || 'Failed to load lot');
  }

  if (!bidsResult.res.ok) {
    throw new Error(bidsResult.data?.message || 'Failed to load bids');
  }

  return {
    lot: lotResult.data,
    bids: Array.isArray(bidsResult.data) ? bidsResult.data : [],
    result: resultResult.res.ok ? resultResult.data : null
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

  const load = async (signal) => {
    setLoading(true);
    setError('');

    try {
      const bundle = await loadLotBundle(lotId, signal);
      setLot(bundle.lot);
      setBids(bundle.bids);
      setResult(bundle.result);
      setBidAmount(String(minimumBidForLot(bundle.lot)));
    } catch (requestError) {
      if (requestError.name !== 'AbortError') {
        setError(requestError.message || 'Failed to load lot');
      }
    } finally {
      setLoading(false);
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

  const runLotAction = async (url, options = {}, successText = 'Action completed') => {
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
        throw new Error(data?.message || 'Action failed');
      }

      setSuccessMessage(successText);
      const bundle = await loadLotBundle(lotId);
      setLot(bundle.lot);
      setBids(bundle.bids);
      setResult(bundle.result);
      return data;
    } catch (requestError) {
      setActionError(requestError.message || 'Action failed');
      return null;
    } finally {
      setActionLoading(false);
    }
  };

  const handleBidSubmit = async (event) => {
    event.preventDefault();

    const amount = Number(bidAmount);
    if (!Number.isFinite(amount) || amount < minimumBid) {
      setActionError(`Bid must be at least ${formatPrice(minimumBid)}`);
      return;
    }

    await runLotAction('/api/bidding/bids', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ lotId: Number(lotId), amount })
    }, 'Bid placed successfully');
  };

  if (loading) {
    return (
      <main className="page">
        <section className="auction-section">
          <div className="auction-state">Loading lot details...</div>
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
            Back to catalog
          </button>
        </section>
      </main>
    );
  }

  return (
    <main className="page">
      <section className="auction-section lot-details-layout">
        <div className="lot-details-main">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Lot #{lot.id}</p>
              <h2>{lot.title}</h2>
            </div>
            <span className={`status-badge status-${String(lot.status || '').toLowerCase()}`}>{lot.status}</span>
          </div>

          <article className="lot-card lot-card-large">
            <p className="lot-description">{lot.description || 'Description will be added later.'}</p>

            <dl className="lot-meta">
              <div>
                <dt>Category</dt>
                <dd>{lot.categoryName || 'Uncategorized'}</dd>
              </div>
              <div>
                <dt>Current price</dt>
                <dd>{formatPrice(lot.currentPrice ?? lot.startPrice)}</dd>
              </div>
              <div>
                <dt>Start price</dt>
                <dd>{formatPrice(lot.startPrice)}</dd>
              </div>
              <div>
                <dt>Bid step</dt>
                <dd>{formatPrice(lot.bidStep)}</dd>
              </div>
              <div>
                <dt>Start time</dt>
                <dd>{formatDate(lot.startTime)}</dd>
              </div>
              <div>
                <dt>End time</dt>
                <dd>{formatDate(lot.endTime)}</dd>
              </div>
            </dl>
          </article>

          <section className="lot-card lot-card-large">
            <div className="section-heading section-heading-tight">
              <div>
                <p className="eyebrow">Bids</p>
                <h2>Bid history</h2>
              </div>
            </div>

            {bids.length > 0 ? (
              <div className="data-list">
                {bids.map((bid) => (
                  <div className="data-row" key={bid.id}>
                    <span>Bidder #{bid.bidderId}</span>
                    <strong>{formatPrice(bid.amount)}</strong>
                    <small>{formatDate(bid.createdAt)}</small>
                  </div>
                ))}
              </div>
            ) : (
              <div className="auction-state">There are no bids yet.</div>
            )}
          </section>
        </div>

        <aside className="lot-details-side">
          <div className="auth-card sticky-card">
            <p className="eyebrow">Participation</p>
            <h3 className="side-title">Place a bid</h3>

            {result && (
              <div className="result-box">
                <span>Current winner</span>
                <strong>{result.winnerId ? `User #${result.winnerId}` : 'No winner yet'}</strong>
                <small>{result.finalPrice ? `Final price: ${formatPrice(result.finalPrice)}` : 'Result is not final'}</small>
                {result.paymentDeadline && <small>Payment deadline: {formatDate(result.paymentDeadline)}</small>}
                <small>{result.paid ? 'Payment received' : 'Payment is pending'}</small>
              </div>
            )}

            {!user && (
              <div className="auction-state">
                You can browse this lot without registration. Log in to place a bid.
              </div>
            )}

            {user && isSeller && (
              <div className="auction-state">Sellers cannot bid on their own lots.</div>
            )}

            {user && lot.status !== 'ACTIVE' && (
              <div className="auction-state">Bids are available only for active lots.</div>
            )}

            {canBid && (
              <form onSubmit={handleBidSubmit}>
                <label className="field">
                  <span>Bid amount</span>
                  <input
                    type="number"
                    min={minimumBid}
                    step="0.01"
                    value={bidAmount}
                    onChange={(event) => setBidAmount(event.target.value)}
                    disabled={actionLoading}
                  />
                  <small>Minimum bid: {formatPrice(minimumBid)}</small>
                </label>

                <button className="submit-button" type="submit" disabled={actionLoading}>
                  {actionLoading ? 'Sending...' : 'Place bid'}
                </button>
              </form>
            )}

            {!user && (
              <button className="submit-button" type="button" onClick={() => onNavigate('/login')}>
                Login to bid
              </button>
            )}

            {actionError && <div className="banner banner-error">{actionError}</div>}
            {successMessage && <div className="banner banner-success">{successMessage}</div>}
          </div>

          {canManageLot && (
            <div className="auth-card sticky-card manage-card">
              <p className="eyebrow">Lot controls</p>
              <h3 className="side-title">Manage status</h3>
              <div className="card-actions card-actions-column">
                {lot.status === 'DRAFT' && (
                  <button
                    className="nav-button nav-button-dark"
                    type="button"
                    disabled={actionLoading}
                    onClick={() => runLotAction(`/api/auction/lots/${lot.id}/activate`, { method: 'POST' }, 'Lot activated')}
                  >
                    Activate lot
                  </button>
                )}
                {lot.status !== 'FINISHED' && lot.status !== 'CANCELLED' && (
                  <button
                    className="nav-button"
                    type="button"
                    disabled={actionLoading}
                    onClick={() => runLotAction(`/api/auction/lots/${lot.id}/cancel`, { method: 'POST' }, 'Lot cancelled')}
                  >
                    Cancel lot
                  </button>
                )}
              </div>
            </div>
          )}
        </aside>
      </section>
    </main>
  );
}

export default LotDetailsPage;
