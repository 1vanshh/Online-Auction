import React, { useEffect, useMemo, useState } from 'react';
import { formatDate, formatPrice } from '../api.js';

const lotStatuses = [
  { value: '', label: 'All statuses' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'FINISHED', label: 'Finished' },
  { value: 'CANCELLED', label: 'Cancelled' }
];

const sortOptions = [
  { value: 'fresh', label: 'Newest first' },
  { value: 'priceAsc', label: 'Price: low to high' },
  { value: 'priceDesc', label: 'Price: high to low' },
  { value: 'categoryAsc', label: 'Category: A to Z' }
];

const getLotPrice = (lot) => Number(lot.currentPrice ?? lot.startPrice ?? 0);

function HomePage({ user, onNavigate }) {
  const [categories, setCategories] = useState([]);
  const [lots, setLots] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [sortOrder, setSortOrder] = useState('fresh');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const controller = new AbortController();

    const loadAuctionData = async () => {
      setLoading(true);
      setError('');

      try {
        const [categoriesRes, lotsRes] = await Promise.all([
          fetch('/api/auction/categories', { signal: controller.signal }),
          fetch('/api/auction/lots', { signal: controller.signal })
        ]);

        const [categoriesData, lotsData] = await Promise.all([
          categoriesRes.json().catch(() => []),
          lotsRes.json().catch(() => [])
        ]);

        if (!categoriesRes.ok) {
          throw new Error(categoriesData?.message || 'Failed to load categories');
        }

        if (!lotsRes.ok) {
          throw new Error(lotsData?.message || 'Failed to load lots');
        }

        setCategories(Array.isArray(categoriesData) ? categoriesData : []);
        setLots(Array.isArray(lotsData) ? lotsData : []);
      } catch (requestError) {
        if (requestError.name !== 'AbortError') {
          setError(requestError.message || 'Failed to load auction data');
        }
      } finally {
        setLoading(false);
      }
    };

    loadAuctionData();

    return () => {
      controller.abort();
    };
  }, []);

  const visibleLots = useMemo(() => {
    const filtered = lots.filter((lot) => {
      const matchesStatus = !statusFilter || lot.status === statusFilter;
      const matchesCategory = !categoryFilter || String(lot.categoryId) === categoryFilter;
      return matchesStatus && matchesCategory;
    });

    return [...filtered].sort((left, right) => {
      if (sortOrder === 'priceAsc') {
        return getLotPrice(left) - getLotPrice(right);
      }

      if (sortOrder === 'priceDesc') {
        return getLotPrice(right) - getLotPrice(left);
      }

      if (sortOrder === 'categoryAsc') {
        return String(left.categoryName || '').localeCompare(String(right.categoryName || '')) ||
          String(left.title || '').localeCompare(String(right.title || ''));
      }

      return new Date(right.createdAt || right.startTime || 0) - new Date(left.createdAt || left.startTime || 0);
    });
  }, [categoryFilter, lots, sortOrder, statusFilter]);

  return (
    <main className="page">
      <section className="auction-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Auction feed</p>
            <h2>Live catalog from auction-service.</h2>
          </div>
          <p className="section-copy">
            Browse lots without an account. Sign in only when you want to create a lot or place a bid.
          </p>
        </div>

        <div className="auction-toolbar">
          <label className="filter-control">
            <span>Status</span>
            <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              {lotStatuses.map((status) => (
                <option key={status.value || 'all'} value={status.value}>
                  {status.label}
                </option>
              ))}
            </select>
          </label>

          <label className="filter-control">
            <span>Category</span>
            <select value={categoryFilter} onChange={(event) => setCategoryFilter(event.target.value)}>
              <option value="">All categories</option>
              {categories.map((category) => (
                <option key={category.id} value={String(category.id)}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>

          <label className="filter-control">
            <span>Sort</span>
            <select value={sortOrder} onChange={(event) => setSortOrder(event.target.value)}>
              {sortOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>

        {loading && <div className="auction-state">Loading lots and categories...</div>}
        {error && <div className="banner banner-error">{error}</div>}

        {!loading && !error && (
          <>
            <div className="category-strip">
              {categories.map((category) => (
                <button
                  key={category.id}
                  className={`category-pill${categoryFilter === String(category.id) ? ' category-pill-active' : ''}`}
                  type="button"
                  onClick={() =>
                    setCategoryFilter((current) => (current === String(category.id) ? '' : String(category.id)))
                  }
                >
                  <strong>{category.name}</strong>
                  <span>{category.description || 'No description yet'}</span>
                </button>
              ))}
            </div>

            {visibleLots.length > 0 ? (
              <div className="lot-grid">
                {visibleLots.map((lot) => (
                  <article className="lot-card" key={lot.id}>
                    <div className="lot-card__top">
                      <span className={`status-badge status-${String(lot.status || '').toLowerCase()}`}>
                        {lot.status}
                      </span>
                      <span className="lot-category">{lot.categoryName || 'Uncategorized'}</span>
                    </div>

                    <h3>{lot.title}</h3>
                    <p className="lot-description">{lot.description || 'Description will be added later.'}</p>

                    <dl className="lot-meta">
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
                        <dt>Ends at</dt>
                        <dd>{formatDate(lot.endTime)}</dd>
                      </div>
                    </dl>

                    <div className="card-actions">
                      <button className="nav-button nav-button-dark" type="button" onClick={() => onNavigate(`/lots/${lot.id}`)}>
                        Open lot
                      </button>
                      {!user && lot.status === 'ACTIVE' && (
                        <button className="nav-button" type="button" onClick={() => onNavigate('/login')}>
                          Login to bid
                        </button>
                      )}
                    </div>
                  </article>
                ))}
              </div>
            ) : (
              <div className="auction-state">No lots match the current filters.</div>
            )}
          </>
        )}
      </section>
    </main>
  );
}

export default HomePage;
