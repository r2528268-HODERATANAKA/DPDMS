import React, { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../api.js';
import { Modal } from './Incidents.jsx';

const ROLES = ['WARD_RECORDER', 'PROVINCIAL_SUPERVISOR', 'PROVINCIAL_ADMIN'];

const EMPTY = {
  username: '', password: '', fullName: '',
  role: 'WARD_RECORDER', ward: 'Ward 1', hazard: 'flood',
};

/** User management (Provincial Administrator only). */
export default function Users() {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');
  const [form, setForm] = useState(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    api.get('/auth/users')
      .then((r) => setRows(r.data))
      .catch((err) => setError(errorMessage(err)));
  }, []);

  useEffect(() => { load(); }, [load]);

  const create = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await api.post('/auth/users', form);
      setForm(null);
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const toggle = async (u) => {
    try {
      await api.put(`/auth/users/${u.id}/status`);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <div>
      <div className="page-head">
        <h2>Users</h2>
        <button className="btn primary" onClick={() => setForm({ ...EMPTY })}>+ New user</button>
      </div>

      {error && <div className="alert bad">{error}</div>}

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>Username</th><th>Full name</th><th>Role</th><th>Ward</th>
              <th>Hazard</th><th>Status</th><th></th>
            </tr>
          </thead>
          <tbody>
            {rows.map((u) => (
              <tr key={u.id}>
                <td><b>{u.username}</b></td>
                <td>{u.fullName}</td>
                <td>{u.role.replace(/_/g, ' ').toLowerCase()}</td>
                <td>{u.ward}</td>
                <td>{u.hazard}</td>
                <td>
                  <span className={`badge ${u.enabled ? 'ok' : 'bad'}`}>{u.enabled ? 'enabled' : 'disabled'}</span>
                </td>
                <td><button className="btn tiny" onClick={() => toggle(u)}>{u.enabled ? 'Disable' : 'Enable'}</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {form && (
        <Modal title="New user" onClose={() => setForm(null)}>
          <form onSubmit={create} className="grid-2">
            <label>Username<input required value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} /></label>
            <label>Password<input required type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></label>
            <label>Full name<input required value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} /></label>
            <label>Role
              <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
                {ROLES.map((r) => <option key={r}>{r}</option>)}
              </select>
            </label>
            {form.role === 'WARD_RECORDER' && (
              <>
                <label>Ward (exact, e.g. Ward 3)<input required value={form.ward} onChange={(e) => setForm({ ...form, ward: e.target.value })} /></label>
                <label>Hazard
                  <select value={form.hazard} onChange={(e) => setForm({ ...form, hazard: e.target.value })}>
                    {['flood', 'drought', 'fire', 'zoonotic', 'mining'].map((h) => <option key={h}>{h}</option>)}
                  </select>
                </label>
              </>
            )}
            <div className="span2 modal-actions">
              <button type="button" className="btn" onClick={() => setForm(null)}>Cancel</button>
              <button className="btn primary" disabled={busy}>{busy ? 'Saving...' : 'Create user'}</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
