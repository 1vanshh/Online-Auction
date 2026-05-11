import React, { useEffect, useMemo, useState } from 'react';
import { authedRequest, formatDate, formatPrice, formatStatus, requestJson, roleLabels, userDisplayName, winnerDisplayName } from '../api.js';

const initialCategory = { id: '', name: '', description: '' };
const initialUser = { id: '', active: true, banned: false, role: 'USER' };

const lotSortOptions = [
  { value: 'idDesc', label: 'ID: новые выше' },
  { value: 'priceDesc', label: 'Цена: по убыванию' },
  { value: 'priceAsc', label: 'Цена: по возрастанию' },
  { value: 'statusAsc', label: 'Статус' },
  { value: 'categoryAsc', label: 'Категория' }
];

const userSortOptions = [
  { value: 'idDesc', label: 'ID: новые выше' },
  { value: 'nameAsc', label: 'Имя' },
  { value: 'emailAsc', label: 'Email' },
  { value: 'roleAsc', label: 'Роль' },
  { value: 'statusAsc', label: 'Статус' }
];

const lotPrice = (lot) => Number(lot.currentPrice ?? lot.startPrice ?? 0);

function AdminPage({ user, token, refreshToken, onNavigate, onAuthRefresh }) {
  const [categories, setCategories] = useState([]);
  const [lots, setLots] = useState([]);
  const [users, setUsers] = useState([]);
  const [categoryForm, setCategoryForm] = useState(initialCategory);
  const [lotId, setLotId] = useState('');
  const [winnerId, setWinnerId] = useState('');
  const [paymentDeadlineDays, setPaymentDeadlineDays] = useState('3');
  const [loadedLot, setLoadedLot] = useState(null);
  const [adminUser, setAdminUser] = useState(initialUser);
  const [lotSort, setLotSort] = useState('idDesc');
  const [userSort, setUserSort] = useState('idDesc');
  const [lotStatusFilter, setLotStatusFilter] = useState('');
  const [userFilter, setUserFilter] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const isAdmin = user?.role === 'ADMIN';

  const loadCategories = async () => {
    const { res, data } = await requestJson('/api/auction/categories');
    if (!res.ok) {
      throw new Error(data?.message || 'Не удалось загрузить категории');
    }
    setCategories(Array.isArray(data) ? data : []);
  };

  const loadLots = async () => {
    const { res, data } = await requestJson('/api/auction/lots');
    if (!res.ok) {
      throw new Error(data?.message || 'Не удалось загрузить лоты');
    }
    setLots(Array.isArray(data) ? data : []);
  };

  const loadUsers = async () => {
    const { res, data } = await authedRequest({
      url: '/api/users/admin',
      token,
      refreshToken,
      onAuthRefresh,
      options: { method: 'GET' }
    });

    if (!res.ok) {
      throw new Error(data?.message || 'Не удалось загрузить пользователей');
    }
    setUsers(Array.isArray(data) ? data : []);
  };

  const refreshAdminData = async () => {
    await Promise.all([loadCategories(), loadLots(), loadUsers()]);
  };

  useEffect(() => {
    if (!isAdmin) {
      return;
    }

    setLoading(true);
    refreshAdminData()
      .catch((requestError) => setError(requestError.message || 'Не удалось загрузить данные админ-панели'))
      .finally(() => setLoading(false));
  }, [isAdmin]);

  const visibleLots = useMemo(() => {
    const filtered = lots.filter((lot) => !lotStatusFilter || lot.status === lotStatusFilter);

    return [...filtered].sort((left, right) => {
      if (lotSort === 'priceDesc') return lotPrice(right) - lotPrice(left);
      if (lotSort === 'priceAsc') return lotPrice(left) - lotPrice(right);
      if (lotSort === 'statusAsc') return String(left.status || '').localeCompare(String(right.status || ''), 'ru');
      if (lotSort === 'categoryAsc') return String(left.categoryName || '').localeCompare(String(right.categoryName || ''), 'ru');
      return Number(right.id || 0) - Number(left.id || 0);
    });
  }, [lots, lotSort, lotStatusFilter]);

  const visibleUsers = useMemo(() => {
    const q = userFilter.trim().toLowerCase();
    const filtered = users.filter((item) => {
      if (!q) return true;
      return [item.id, item.email, item.firstName, item.lastName, item.role]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(q));
    });

    return [...filtered].sort((left, right) => {
      if (userSort === 'nameAsc') return userDisplayName(left).localeCompare(userDisplayName(right), 'ru');
      if (userSort === 'emailAsc') return String(left.email || '').localeCompare(String(right.email || ''), 'ru');
      if (userSort === 'roleAsc') return String(left.role || '').localeCompare(String(right.role || ''), 'ru');
      if (userSort === 'statusAsc') return Number(left.banned) - Number(right.banned) || Number(right.active) - Number(left.active);
      return Number(right.id || 0) - Number(left.id || 0);
    });
  }, [users, userFilter, userSort]);

  if (!user) {
    return (
      <main className="page">
        <section className="auth-layout">
          <div className="auth-copy">
            <p className="eyebrow">Админ-панель</p>
            <h1>Войдите как администратор.</h1>
            <p className="lede">Управление сайтом доступно только пользователям с ролью ADMIN.</p>
          </div>
          <div className="auth-card account-empty">
            <button className="submit-button" type="button" onClick={() => onNavigate('/login')}>
              Войти
            </button>
          </div>
        </section>
      </main>
    );
  }

  if (!isAdmin) {
    return (
      <main className="page">
        <section className="auction-section">
          <div className="banner banner-error">Для доступа нужна роль администратора.</div>
        </section>
      </main>
    );
  }

  const runAdminRequest = async (url, options, successText, afterSuccess) => {
    setLoading(true);
    setError('');
    setMessage('');

    try {
      const { res, data } = await authedRequest({ url, token, refreshToken, onAuthRefresh, options });

      if (!res.ok) {
        throw new Error(data?.message || 'Не удалось выполнить действие');
      }

      await afterSuccess?.(data);
      setMessage(successText);
      return data;
    } catch (requestError) {
      setError(requestError.message || 'Не удалось выполнить действие');
      return null;
    } finally {
      setLoading(false);
    }
  };

  const saveCategory = async (event) => {
    event.preventDefault();
    const payload = {
      name: categoryForm.name.trim(),
      description: categoryForm.description.trim() || null
    };

    if (!payload.name || payload.name.length < 2) {
      setError('Название категории должно содержать минимум 2 символа');
      return;
    }

    await runAdminRequest(
      categoryForm.id ? `/api/auction/categories/admin/${categoryForm.id}` : '/api/auction/categories/admin',
      {
        method: categoryForm.id ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      },
      categoryForm.id ? 'Категория обновлена' : 'Категория создана',
      async () => {
        setCategoryForm(initialCategory);
        await loadCategories();
      }
    );
  };

  const deleteCategory = async (id) => {
    await runAdminRequest(`/api/auction/categories/admin/${id}`, { method: 'DELETE' }, 'Категория удалена', async () => {
      await loadCategories();
    });
  };

  const selectLot = (lot) => {
    setLoadedLot(lot);
    setLotId(String(lot.id));
    setWinnerId(lot.winnerId ? String(lot.winnerId) : '');
  };

  const loadLot = async () => {
    if (!lotId) {
      setError('Укажите ID лота');
      return;
    }

    setLoading(true);
    setError('');
    setMessage('');

    try {
      const { res, data } = await requestJson(`/api/auction/lots/${lotId}`);
      if (!res.ok) {
        throw new Error(data?.message || 'Лот не найден');
      }
      setLoadedLot(data);
      setMessage('Лот загружен');
    } catch (requestError) {
      setError(requestError.message || 'Лот не найден');
    } finally {
      setLoading(false);
    }
  };

  const refreshSelectedLot = async (data) => {
    if (data?.id) {
      setLoadedLot(data);
      setLotId(String(data.id));
    }
    await loadLots();
  };

  const finishAuctionLot = async () => {
    await runAdminRequest(`/api/auction/admin/lots/${lotId}/finish`, { method: 'POST' }, 'Лот завершён', refreshSelectedLot);
  };

  const finishBidding = async () => {
    await runAdminRequest(
      `/api/bidding/admin/bids/lots/${lotId}/finish`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ paymentDeadlineDays: Number(paymentDeadlineDays || 3) })
      },
      'Торги завершены',
      async () => {
        const { res, data } = await requestJson(`/api/auction/lots/${lotId}`);
        if (res.ok) {
          setLoadedLot(data);
        }
        await loadLots();
      }
    );
  };

  const markPaid = async () => {
    await runAdminRequest(`/api/bidding/admin/bids/lots/${lotId}/paid`, { method: 'POST' }, 'Оплата отмечена');
  };

  const setWinner = async () => {
    await runAdminRequest(
      `/api/auction/admin/lots/${lotId}/winner`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ winnerId: Number(winnerId) })
      },
      'Победитель назначен',
      refreshSelectedLot
    );
  };

  const banUnpaidWinner = async () => {
    await runAdminRequest(
      `/api/auction/admin/lots/${lotId}/unpaid-ban`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ banDays: 7, reason: `Победитель не оплатил лот #${lotId}` })
      },
      'Победитель заблокирован на 7 дней',
      async () => loadUsers()
    );
  };

  const selectUser = (selectedUser) => {
    setAdminUser({
      id: selectedUser.id,
      active: Boolean(selectedUser.active),
      banned: Boolean(selectedUser.banned),
      role: selectedUser.role || 'USER'
    });
  };

  const loadUser = async () => {
    if (!adminUser.id) {
      setError('Укажите ID пользователя');
      return;
    }

    const data = await runAdminRequest(`/api/users/admin/${adminUser.id}`, { method: 'GET' }, 'Пользователь загружен');
    if (data) {
      selectUser(data);
    }
  };

  const updateUserByPayload = async (id, payload, successText) => {
    await runAdminRequest(
      `/api/users/admin/${id}`,
      {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      },
      successText,
      async (data) => {
        if (data) selectUser(data);
        await loadUsers();
      }
    );
  };

  const updateUser = async (event) => {
    event.preventDefault();
    await updateUserByPayload(
      adminUser.id,
      { active: adminUser.active, banned: adminUser.banned, role: adminUser.role },
      'Пользователь обновлён'
    );
  };

  const quickBanUser = async (selectedUser) => {
    await updateUserByPayload(selectedUser.id, { active: false, banned: true, role: selectedUser.role }, 'Пользователь заблокирован на 7 дней');
  };

  const quickUnbanUser = async (selectedUser) => {
    await updateUserByPayload(selectedUser.id, { active: true, banned: false, role: selectedUser.role }, 'Пользователь разблокирован');
  };

  return (
    <main className="page">
      <section className="auction-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Админ-панель</p>
            <h2>Управление сайтом</h2>
          </div>
          <button className="nav-button" type="button" onClick={refreshAdminData} disabled={loading}>
            Обновить данные
          </button>
        </div>

        {message && <div className="banner banner-success">{message}</div>}
        {error && <div className="banner banner-error">{error}</div>}

        <div className="admin-grid">
          <section className="auth-card">
            <p className="eyebrow">Категории</p>
            <h3 className="side-title">Создать или изменить</h3>
            <form onSubmit={saveCategory}>
              <div className="form-grid">
                <label className="field">
                  <span>ID для изменения</span>
                  <input value={categoryForm.id} onChange={(event) => setCategoryForm((current) => ({ ...current, id: event.target.value }))} />
                </label>
                <label className="field">
                  <span>Название</span>
                  <input value={categoryForm.name} onChange={(event) => setCategoryForm((current) => ({ ...current, name: event.target.value }))} />
                </label>
                <label className="field field-wide">
                  <span>Описание</span>
                  <input value={categoryForm.description} onChange={(event) => setCategoryForm((current) => ({ ...current, description: event.target.value }))} />
                </label>
              </div>
              <button className="submit-button" type="submit" disabled={loading}>
                Сохранить категорию
              </button>
            </form>

            <div className="data-list data-list-spaced">
              {categories.map((category) => (
                <div className="data-row compact-row" key={category.id}>
                  <span>#{category.id} {category.name}</span>
                  <small>{category.description || 'Без описания'}</small>
                  <button className="inline-link" type="button" onClick={() => setCategoryForm({ id: category.id, name: category.name, description: category.description || '' })}>
                    изменить
                  </button>
                  <button className="inline-link danger-link" type="button" onClick={() => deleteCategory(category.id)}>
                    удалить
                  </button>
                </div>
              ))}
            </div>
          </section>

          <section className="auth-card">
            <p className="eyebrow">Завершение торгов</p>
            <h3 className="side-title">Действия с выбранным лотом</h3>
            <div className="form-grid">
              <label className="field">
                <span>ID лота</span>
                <input type="number" value={lotId} onChange={(event) => setLotId(event.target.value)} />
              </label>
              <label className="field">
                <span>Дней на оплату</span>
                <input type="number" min="1" max="30" value={paymentDeadlineDays} onChange={(event) => setPaymentDeadlineDays(event.target.value)} />
              </label>
              <label className="field field-wide">
                <span>ID победителя</span>
                <input type="number" value={winnerId} onChange={(event) => setWinnerId(event.target.value)} />
              </label>
            </div>

            <div className="card-actions">
              <button className="nav-button" type="button" onClick={loadLot} disabled={loading || !lotId}>Загрузить</button>
              <button className="nav-button" type="button" onClick={finishAuctionLot} disabled={loading || !lotId}>Завершить лот</button>
              <button className="nav-button" type="button" onClick={finishBidding} disabled={loading || !lotId}>Завершить торги</button>
              <button className="nav-button" type="button" onClick={markPaid} disabled={loading || !lotId}>Оплата получена</button>
              <button className="nav-button" type="button" onClick={setWinner} disabled={loading || !lotId || !winnerId}>Назначить победителя</button>
              <button className="nav-button nav-button-dark" type="button" onClick={banUnpaidWinner} disabled={loading || !lotId}>Бан неоплатившего</button>
            </div>

            {loadedLot && (
              <div className="result-box">
                <span>{loadedLot.title}</span>
                <strong>{formatStatus(loadedLot.status)} / {formatPrice(loadedLot.currentPrice ?? loadedLot.startPrice)}</strong>
                <small>Категория: {loadedLot.categoryName || 'Без категории'}</small>
                <small>Победитель: {winnerDisplayName(loadedLot) || 'не назначен'}</small>
                <small>Окончание: {formatDate(loadedLot.endTime)}</small>
              </div>
            )}
          </section>

          <section className="auth-card admin-wide-card">
            <div className="section-heading section-heading-tight">
              <div>
                <p className="eyebrow">Все лоты</p>
                <h3 className="side-title">Быстрый выбор и сортировка</h3>
              </div>
            </div>
            <div className="auction-toolbar admin-toolbar">
              <label className="filter-control">
                <span>Статус</span>
                <select value={lotStatusFilter} onChange={(event) => setLotStatusFilter(event.target.value)}>
                  <option value="">Все</option>
                  <option value="DRAFT">Черновик</option>
                  <option value="ACTIVE">Активен</option>
                  <option value="FINISHED">Завершён</option>
                  <option value="CANCELLED">Отменён</option>
                </select>
              </label>
              <label className="filter-control">
                <span>Сортировка</span>
                <select value={lotSort} onChange={(event) => setLotSort(event.target.value)}>
                  {lotSortOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                </select>
              </label>
            </div>

            <div className="admin-table-wrap">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Лот</th>
                    <th>Статус</th>
                    <th>Категория</th>
                    <th>Цена</th>
                    <th>Победитель</th>
                    <th>Действия</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleLots.map((lot) => (
                    <tr key={lot.id}>
                      <td>#{lot.id}</td>
                      <td>
                        <div className="table-title-cell">
                          {lot.mainImageUrl && <span className="table-thumb" style={{ backgroundImage: `url(${lot.mainImageUrl})` }} />}
                          <span>{lot.title}</span>
                        </div>
                      </td>
                      <td>{formatStatus(lot.status)}</td>
                      <td>{lot.categoryName || 'Без категории'}</td>
                      <td>{formatPrice(lot.currentPrice ?? lot.startPrice)}</td>
                      <td>{winnerDisplayName(lot) || '—'}</td>
                      <td>
                        <div className="table-actions">
                          <button className="inline-link" type="button" onClick={() => selectLot(lot)}>выбрать</button>
                          <button className="inline-link" type="button" onClick={() => onNavigate(`/lots/${lot.id}`)}>открыть</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="auth-card admin-wide-card">
            <div className="section-heading section-heading-tight">
              <div>
                <p className="eyebrow">Пользователи</p>
                <h3 className="side-title">Список, поиск и быстрый бан</h3>
              </div>
            </div>

            <form onSubmit={updateUser} className="admin-user-form">
              <div className="form-grid">
                <label className="field">
                  <span>ID пользователя</span>
                  <input type="number" value={adminUser.id} onChange={(event) => setAdminUser((current) => ({ ...current, id: event.target.value }))} />
                </label>
                <label className="field">
                  <span>Роль</span>
                  <select value={adminUser.role} onChange={(event) => setAdminUser((current) => ({ ...current, role: event.target.value }))}>
                    <option value="USER">Пользователь</option>
                    <option value="ADMIN">Администратор</option>
                  </select>
                </label>
                <label className="checkbox-field">
                  <input type="checkbox" checked={adminUser.active} onChange={(event) => setAdminUser((current) => ({ ...current, active: event.target.checked, banned: event.target.checked ? false : current.banned }))} />
                  <span>Активен</span>
                </label>
                <label className="checkbox-field">
                  <input type="checkbox" checked={adminUser.banned} onChange={(event) => setAdminUser((current) => ({ ...current, banned: event.target.checked, active: event.target.checked ? false : current.active }))} />
                  <span>Заблокирован</span>
                </label>
              </div>
              <div className="card-actions">
                <button className="nav-button" type="button" onClick={loadUser} disabled={loading || !adminUser.id}>Загрузить по ID</button>
                <button className="submit-button submit-button-inline" type="submit" disabled={loading || !adminUser.id}>Сохранить</button>
              </div>
            </form>

            <div className="auction-toolbar admin-toolbar">
              <label className="filter-control">
                <span>Поиск</span>
                <input value={userFilter} onChange={(event) => setUserFilter(event.target.value)} placeholder="email, имя, ID" />
              </label>
              <label className="filter-control">
                <span>Сортировка</span>
                <select value={userSort} onChange={(event) => setUserSort(event.target.value)}>
                  {userSortOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                </select>
              </label>
            </div>

            <div className="admin-table-wrap">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Пользователь</th>
                    <th>Email</th>
                    <th>Роль</th>
                    <th>Статус</th>
                    <th>Создан</th>
                    <th>Действия</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleUsers.map((item) => (
                    <tr key={item.id}>
                      <td>#{item.id}</td>
                      <td>{userDisplayName(item)}</td>
                      <td>{item.email}</td>
                      <td>{roleLabels[item.role] || item.role}</td>
                      <td>{item.banned ? 'Заблокирован' : item.active ? 'Активен' : 'Выключен'}</td>
                      <td>{formatDate(item.createdAt)}</td>
                      <td>
                        <div className="table-actions">
                          <button className="inline-link" type="button" onClick={() => selectUser(item)}>выбрать</button>
                          {item.banned ? (
                            <button className="inline-link" type="button" onClick={() => quickUnbanUser(item)}>разбанить</button>
                          ) : (
                            <button className="inline-link danger-link" type="button" onClick={() => quickBanUser(item)}>бан на 7 дней</button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </section>
    </main>
  );
}

export default AdminPage;
