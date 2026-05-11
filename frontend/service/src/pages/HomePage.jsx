import React, { useEffect, useMemo, useState } from 'react';
import { formatDate, formatPrice, formatStatus, winnerDisplayName } from '../api.js';

const lotStatuses = [
  { value: '', label: 'Все статусы' },
  { value: 'DRAFT', label: 'Черновик' },
  { value: 'ACTIVE', label: 'Активен' },
  { value: 'FINISHED', label: 'Завершён' },
  { value: 'CANCELLED', label: 'Отменён' }
];

const sortOptions = [
  { value: 'fresh', label: 'Сначала новые' },
  { value: 'priceAsc', label: 'Цена: по возрастанию' },
  { value: 'priceDesc', label: 'Цена: по убыванию' },
  { value: 'categoryAsc', label: 'Категория: А-Я' }
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
          throw new Error(categoriesData?.message || 'Не удалось загрузить категории');
        }

        if (!lotsRes.ok) {
          throw new Error(lotsData?.message || 'Не удалось загрузить лоты');
        }

        setCategories(Array.isArray(categoriesData) ? categoriesData : []);
        setLots(Array.isArray(lotsData) ? lotsData : []);
      } catch (requestError) {
        if (requestError.name !== 'AbortError') {
          setError(requestError.message || 'Не удалось загрузить данные аукциона');
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
        return String(left.categoryName || '').localeCompare(String(right.categoryName || ''), 'ru') ||
          String(left.title || '').localeCompare(String(right.title || ''), 'ru');
      }

      return new Date(right.createdAt || right.startTime || 0) - new Date(left.createdAt || left.startTime || 0);
    });
  }, [categoryFilter, lots, sortOrder, statusFilter]);

  return (
    <main className="page">
      <section className="auction-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Каталог</p>
            <h2>Актуальные лоты</h2>
          </div>
          <p className="section-copy">
            Смотреть лоты можно без регистрации. Войдите в аккаунт только когда захотите создать лот или сделать ставку.
          </p>
        </div>

        <div className="auction-toolbar">
          <label className="filter-control">
            <span>Статус</span>
            <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              {lotStatuses.map((status) => (
                <option key={status.value || 'all'} value={status.value}>
                  {status.label}
                </option>
              ))}
            </select>
          </label>

          <label className="filter-control">
            <span>Категория</span>
            <select value={categoryFilter} onChange={(event) => setCategoryFilter(event.target.value)}>
              <option value="">Все категории</option>
              {categories.map((category) => (
                <option key={category.id} value={String(category.id)}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>

          <label className="filter-control">
            <span>Сортировка</span>
            <select value={sortOrder} onChange={(event) => setSortOrder(event.target.value)}>
              {sortOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>

        {loading && <div className="auction-state">Загружаем лоты и категории...</div>}
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
                  <span>{category.description || 'Без описания'}</span>
                </button>
              ))}
            </div>

            {visibleLots.length > 0 ? (
              <div className="lot-grid">
                {visibleLots.map((lot) => {
                  const winnerName = winnerDisplayName(lot);

                  return (
                    <article className={`lot-card${lot.mainImageUrl ? ' lot-card-with-image' : ''}`} key={lot.id}>
                      {lot.mainImageUrl && (
                        <div className="lot-card__image" style={{ backgroundImage: `url(${lot.mainImageUrl})` }} />
                      )}
                      <div className="lot-card__content">
                        <div className="lot-card__top">
                          <span className={`status-badge status-${String(lot.status || '').toLowerCase()}`}>
                            {formatStatus(lot.status)}
                          </span>
                          <span className="lot-category">{lot.categoryName || 'Без категории'}</span>
                        </div>

                        <h3>{lot.title}</h3>
                        <p className="lot-description">{lot.description || 'Описание пока не добавлено.'}</p>

                        {lot.status === 'FINISHED' && (
                          <div className="winner-strip">
                            <span>Победитель</span>
                            <strong>{winnerName || 'Не определён'}</strong>
                          </div>
                        )}

                        <dl className="lot-meta">
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
                            <dt>Завершение</dt>
                            <dd>{formatDate(lot.endTime)}</dd>
                          </div>
                        </dl>

                        <div className="card-actions">
                          <button className="nav-button nav-button-dark" type="button" onClick={() => onNavigate(`/lots/${lot.id}`)}>
                            Открыть лот
                          </button>
                          {!user && lot.status === 'ACTIVE' && (
                            <button className="nav-button" type="button" onClick={() => onNavigate('/login')}>
                              Войти для ставки
                            </button>
                          )}
                        </div>
                      </div>
                    </article>
                  );
                })}
              </div>
            ) : (
              <div className="auction-state">По выбранным фильтрам лоты не найдены.</div>
            )}
          </>
        )}
      </section>
    </main>
  );
}

export default HomePage;
