import React, { useEffect, useState } from 'react';

const lotStatuses = [
  { value: '', label: 'All statuses' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'FINISHED', label: 'Finished' },
  { value: 'CANCELLED', label: 'Cancelled' }
];

const formatPrice = (value) => {
  if (value === null || value === undefined) {
    return 'Not specified';
  }

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 2
  }).format(Number(value));
};

const formatDate = (value) => {
  if (!value) {
    return 'Not scheduled';
  }

  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(value));
};

function HomePage({ user, onNavigate }) {
  const [categories, setCategories] = useState([]);
  const [lots, setLots] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
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

  const visibleLots = lots.filter((lot) => {
    const matchesStatus = !statusFilter || lot.status === statusFilter;
    const matchesCategory = !categoryFilter || String(lot.categoryId) === categoryFilter;
    return matchesStatus && matchesCategory;
  });

  return (
    <main className="page">
      <section className="auction-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Auction feed</p>
            <h2>Live catalog from auction-service.</h2>
          </div>
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
