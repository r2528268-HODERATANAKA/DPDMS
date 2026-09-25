import React, { useState } from 'react';
import api, { errorMessage } from '../api.js';

const DEMO_ACCOUNTS = [
  ['admin', 'Admin@123', 'Provincial Administrator'],
  ['fire.supervisor', 'Super@123', 'Fire Supervisor'],
  ['flood.supervisor', 'Super@123', 'Flood Supervisor'],
  ['ward4.fire', 'Ward@123', 'Ward 4 Fire Recorder'],
  ['ward1.flood', 'Ward@123', 'Ward 1 Flood Recorder'],
];

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const { data } = await api.post('/auth/login', { username, password });
      localStorage.setItem('dpdms_auth', JSON.stringify({
        token: data.token,
        user: {
          username: data.username,
          fullName: data.fullName,
          role: data.role,
          ward: data.ward,
          hazard: data.hazard,
        },
      }));
      window.location.reload();
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  };

  return (
    <div className="login-wrap">
      <div className="login-card">
        <div className="login-brand">
          <div className="logo">DPDMS</div>
          <h1>Rushinga District<br />Disaster Preparedness &amp; Monitoring</h1>
        </div>
        <form onSubmit={submit}>
          <label>Username</label>
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus />
          <label>Password</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
          {error && <div className="form-error">{error}</div>}
          <button className="btn primary block" disabled={busy}>
            {busy ? 'Signing in...' : 'Sign in'}
          </button>
        </form>
        <div className="demo-accounts">
          <h4>Demo accounts</h4>
          {DEMO_ACCOUNTS.map(([u, p, label]) => (
            <button
              key={u}
              type="button"
              className="demo-chip"
              onClick={() => { setUsername(u); setPassword(p); }}
              title="Click to fill"
            >
              <b>{u}</b> / {p} <span>- {label}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
