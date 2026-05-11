import React, { useEffect, useState } from 'react';
import { authedRequest, formatDate, formatPrice, requestJson } from '../api.js';

const initialCategory = { id: '', name: '', description: '' };
const initialUser = { id: '', active: true, banned: false, role: 'USER' };

function AdminPage({ user, token, refreshToken, onNavigate, onAuthRefresh }) {
  const [categories, setCategories] = useState([]);
  const [categoryForm, setCategoryForm] = useState(initialCategory);
  const [lotId, setLotId] = useState('');
  const [winnerId, setWinnerId] = useState('');
  const [paymentDeadlineDays, setPaymentDeadlineDays] = useState('3');
  const [loadedLot, setLoadedLot] = useState(null);
  const [adminUser, setAdminUser] = useState(initialUser);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const isAdmin = user?.role === 'ADMIN';

  const loadCategories = async () => {
    const { res, data } = await requestJson('/api/auction/categories');
    if (!res.ok) {
      throw new Error(data?.message || 'Failed to load categories');
    }
    setCategories(Array.isArray(data) ? data : []);
  };

  useEffect(() => {
    if (!isAdmin) {
      return;
    }

    loadCategories().catch((requestError) => setError(requestError.message || 'Failed to load categories'));
  }, [isAdmin]);

  if (!user) {
    return (
      <main className="page">
        <section className="auth-layout">
          <div className="auth-copy">
            <p className="eyebrow">Admin</p>
            <h1>Sign in as admin.</h1>
            <p className="lede">Administrative controls are hidden from anonymous visitors.</p>
          </div>
          <div className="auth-card account-empty">
            <button className="submit-button" type="button" onClick={() => onNavigate('/login')}>
              Login
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
          <div className="banner banner-error">Admin role is required.</div>
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
        throw new Error(data?.message || 'Admin action failed');
      }

      await afterSuccess?.(data);
      setMessage(successText);
      return data;
    } catch (requestError) {
      setError(requestError.message || 'Admin action failed');
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
      setError('Category name must contain at least 2 characters');
      return;
    }

    await runAdminRequest(
      categoryForm.id ? `/api/auction/categories/admin/${categoryForm.id}` : '/api/auction/categories/admin',
      {
        method: categoryForm.id ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      },
      categoryForm.id ? 'Category updated' : 'Category created',
      async () => {
        setCategoryForm(initialCategory);
        await loadCategories();
      }
    );
  };

  const deleteCategory = async (id) => {
    await runAdminRequest(`/api/auction/categories/admin/${id}`, { method: 'DELETE' }, 'Category deleted', async () => {
      await loadCategories();
    });
  };

  const loadLot = async () => {
    if (!lotId) {
      setError('Enter lot id');
      return;
    }

    setLoading(true);
    setError('');
    setMessage('');

    try {
      const { res, data } = await requestJson(`/api/auction/lots/${lotId}`);
      if (!res.ok) {
        throw new Error(data?.message || 'Lot not found');
      }
      setLoadedLot(data);
      setMessage('Lot loaded');
    } catch (requestError) {
      setError(requestError.message || 'Lot not found');
    } finally {
      setLoading(false);
    }
  };

  const finishAuctionLot = async () => {
    await runAdminRequest(`/api/auction/admin/lots/${lotId}/finish`, { method: 'POST' }, 'Lot finished', setLoadedLot);
  };

  const finishBidding = async () => {
    await runAdminRequest(
      `/api/bidding/admin/bids/lots/${lotId}/finish`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ paymentDeadlineDays: Number(paymentDeadlineDays || 3) })
      },
      'Bidding finished'
    );
  };

  const markPaid = async () => {
    await runAdminRequest(`/api/bidding/admin/bids/lots/${lotId}/paid`, { method: 'POST' }, 'Lot marked as paid');
  };

  const setWinner = async () => {
    await runAdminRequest(
      `/api/auction/admin/lots/${lotId}/winner`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ winnerId: Number(winnerId) })
      },
      'Winner assigned',
      setLoadedLot
    );
  };

  const banUnpaidWinner = async () => {
    await runAdminRequest(
      `/api/auction/admin/lots/${lotId}/unpaid-ban`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ banDays: 7, reason: `Winner did not pay for lot #${lotId}` })
      },
      'Winner banned for 7 days'
    );
  };

  const loadUser = async () => {
    if (!adminUser.id) {
      setError('Enter user id');
      return;
    }

    const data = await runAdminRequest(`/api/users/admin/${adminUser.id}`, { method: 'GET' }, 'User loaded');
    if (data) {
      setAdminUser({
        id: data.id,
        active: Boolean(data.active),
        banned: Boolean(data.banned),
        role: data.role || 'USER'
      });
    }
  };

  const updateUser = async (event) => {
    event.preventDefault();
    await runAdminRequest(
      `/api/users/admin/${adminUser.id}`,
      {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ active: adminUser.active, banned: adminUser.banned, role: adminUser.role })
      },
      'User updated'
    );
  };

  return (
    <main className="page">
      <section className="auction-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Admin</p>
            <h2>Manage auction data.</h2>
          </div>
          <p className="section-copy">Use IDs for destructive actions so accidental changes are harder to make.</p>
        </div>

        {message && <div className="banner banner-success">{message}</div>}
        {error && <div className="banner banner-error">{error}</div>}

        <div className="admin-grid">
          <section className="auth-card">
            <p className="eyebrow">Categories</p>
            <h3 className="side-title">Create or update category</h3>
            <form onSubmit={saveCategory}>
              <div className="form-grid">
                <label className="field">
                  <span>ID for update</span>
                  <input value={categoryForm.id} onChange={(event) => setCategoryForm((current) => ({ ...current, id: event.target.value }))} />
                </label>
                <label className="field">
                  <span>Name</span>
                  <input value={categoryForm.name} onChange={(event) => setCategoryForm((current) => ({ ...current, name: event.target.value }))} />
                </label>
                <label className="field field-wide">
                  <span>Description</span>
                  <input value={categoryForm.description} onChange={(event) => setCategoryForm((current) => ({ ...current, description: event.target.value }))} />
                </label>
              </div>
              <button className="submit-button" type="submit" disabled={loading}>
                Save category
              </button>
            </form>

            <div className="data-list data-list-spaced">
              {categories.map((category) => (
                <div className="data-row" key={category.id}>
                  <span>#{category.id} {category.name}</span>
                  <small>{category.description || 'No description'}</small>
                  <button className="inline-link" type="button" onClick={() => setCategoryForm({ id: category.id, name: category.name, description: category.description || '' })}>
                    edit
                  </button>
                  <button className="inline-link danger-link" type="button" onClick={() => deleteCategory(category.id)}>
                    delete
                  </button>
                </div>
              ))}
            </div>
          </section>

          <section className="auth-card">
            <p className="eyebrow">Lots and bids</p>
            <h3 className="side-title">Resolve auctions</h3>
            <div className="form-grid">
              <label className="field">
                <span>Lot ID</span>
                <input type="number" value={lotId} onChange={(event) => setLotId(event.target.value)} />
              </label>
              <label className="field">
                <span>Payment deadline days</span>
                <input type="number" min="1" max="30" value={paymentDeadlineDays} onChange={(event) => setPaymentDeadlineDays(event.target.value)} />
              </label>
              <label className="field field-wide">
                <span>Winner user ID</span>
                <input type="number" value={winnerId} onChange={(event) => setWinnerId(event.target.value)} />
              </label>
            </div>

            <div className="card-actions">
              <button className="nav-button" type="button" onClick={loadLot} disabled={loading}>Load lot</button>
              <button className="nav-button" type="button" onClick={finishAuctionLot} disabled={loading || !lotId}>Finish lot</button>
              <button className="nav-button" type="button" onClick={finishBidding} disabled={loading || !lotId}>Finish bidding</button>
              <button className="nav-button" type="button" onClick={markPaid} disabled={loading || !lotId}>Mark paid</button>
              <button className="nav-button" type="button" onClick={setWinner} disabled={loading || !lotId || !winnerId}>Set winner</button>
              <button className="nav-button nav-button-dark" type="button" onClick={banUnpaidWinner} disabled={loading || !lotId}>Ban unpaid winner for 7 days</button>
            </div>

            {loadedLot && (
              <div className="result-box">
                <span>{loadedLot.title}</span>
                <strong>{loadedLot.status} / {formatPrice(loadedLot.currentPrice ?? loadedLot.startPrice)}</strong>
                <small>Category: {loadedLot.categoryName || 'Uncategorized'}</small>
                <small>Winner: {loadedLot.winnerId || 'not assigned'}</small>
                <small>Ends: {formatDate(loadedLot.endTime)}</small>
              </div>
            )}
          </section>

          <section className="auth-card">
            <p className="eyebrow">Users</p>
            <h3 className="side-title">Moderate account</h3>
            <form onSubmit={updateUser}>
              <div className="form-grid">
                <label className="field">
                  <span>User ID</span>
                  <input type="number" value={adminUser.id} onChange={(event) => setAdminUser((current) => ({ ...current, id: event.target.value }))} />
                </label>
                <label className="field">
                  <span>Role</span>
                  <select value={adminUser.role} onChange={(event) => setAdminUser((current) => ({ ...current, role: event.target.value }))}>
                    <option value="USER">USER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </label>
                <label className="checkbox-field">
                  <input type="checkbox" checked={adminUser.active} onChange={(event) => setAdminUser((current) => ({ ...current, active: event.target.checked, banned: event.target.checked ? false : current.banned }))} />
                  <span>Active</span>
                </label>
                <label className="checkbox-field">
                  <input type="checkbox" checked={adminUser.banned} onChange={(event) => setAdminUser((current) => ({ ...current, banned: event.target.checked, active: event.target.checked ? false : current.active }))} />
                  <span>Banned</span>
                </label>
              </div>
              <div className="card-actions">
                <button className="nav-button" type="button" onClick={loadUser} disabled={loading || !adminUser.id}>Load user</button>
                <button className="submit-button submit-button-inline" type="submit" disabled={loading || !adminUser.id}>Update user</button>
              </div>
            </form>
          </section>
        </div>
      </section>
    </main>
  );
}

export default AdminPage;
