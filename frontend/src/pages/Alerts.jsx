import React, { useCallback, useEffect, useState } from 'react';
import api, { HAZARDS, errorMessage } from '../api.js';
import { Modal } from './Incidents.jsx';

/** Alert log (Provincial Administrator only). */
export default function Alerts() {
  const [rows, setRows] = useState([]);
  const [hazard, setHazard] = useState('');
  const [channel, setChannel] = useState('');
  const [status, setStatus] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState(null);

  const load = useCallback(() => {
    setError('');
    const params = {};
    if (hazard) params.hazard = hazard;
    if (channel) params.channel = channel;
    if (status) params.status = status;
    api.get('/alerts', { params })
      .then((r) => setRows(r.data))
      .catch((err) => setError(errorMessage(err)));
  }, [hazard, channel, status]);

  useEffect(() => { load(); }, [load]);

  const scan = async () => {
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const { data } = await api.post('/alerts/scan');
      setNotice(`Scan complete: ${data.alerted ?? '?'} alerted, ${data.skipped ?? '?'} skipped.`);
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const send = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    setNotice('');
    try {
      await api.post('/alerts/send', {
        ...form,
        incidentId: form.incidentId === '' ? null : Number(form.incidentId),
      });
      setForm(null);
      setNotice('Alert sent through all channels.');
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <div className="page-head">
        <h2>Alert log</h2>
        <div className="row-actions">
          <button className="btn" disabled={busy} onClick={scan}>
            {busy ? 'Scanning...' : 'Scan approved'}
          </button>
          <button
            className="btn primary"
            onClick={() => setForm({
              hazard: 'fire', incidentId: '', ward: 'Ward 1',
              district: 'Mudzi', severity: 'HIGH', message: '',
            })}
          >
            + Send alert
          </button>
        </div>
      </div>
      <div className="page-head">
        <div className="filters">
          <select value={hazard} onChange={(e) => setHazard(e.target.value)}>
            <option value="">All hazards</option>
            {HAZARDS.map((h) => <option key={h}>{h}</option>)}
          </select>
          <select value={channel} onChange={(e) => setChannel(e.target.value)}>
            <option value="">All channels</option>
            <option value="EMAIL">EMAIL</option>
            <option value="WHATSAPP">WHATSAPP</option>
            <option value="TELEGRAM">TELEGRAM</option>
          </select>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All statuses</option>
            <option value="SENT">SENT</option>
            <option value="SKIPPED">SKIPPED</option>
            <option value="FAILED">FAILED</option>
          </select>
        </div>
      </div>

      {error && <div className="alert bad">{error}</div>}
      {notice && <div className="alert info">{notice}</div>}

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>Sent at</th><th>Hazard</th><th>Ward</th><th>Channel</th>
              <th>Recipient</th><th>Status</th><th>Detail</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((a) => (
              <tr key={a.id}>
                <td>{(a.sentAt || '').replace('T', ' ').slice(0, 19)}</td>
                <td className="cap">{a.hazardType}</td>
                <td>{a.ward}</td>
                <td>{a.channel}</td>
                <td>{a.recipient}</td>
                <td>
                  <span className={`badge ${a.status === 'SENT' ? 'ok' : a.status === 'SKIPPED' ? 'info' : 'bad'}`}>
                    {a.status}
                  </span>
                </td>
                <td className="wrap">{a.detail}</td>
              </tr>
            ))}
            {rows.length === 0 && (
              <tr><td colSpan="7" className="muted">
                No alerts yet. Approve an incident in the Approvals page to trigger the fan-out.
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {form && (
        <Modal title="Send alert" onClose={() => setForm(null)}>
          <form onSubmit={send} className="grid-2">
            <label>Hazard
              <select value={form.hazard} onChange={(e) => setForm({ ...form, hazard: e.target.value })}>
                {HAZARDS.map((h) => <option key={h}>{h}</option>)}
              </select>
            </label>
            <label>Incident ID (optional)
              <input type="number" value={form.incidentId}
                onChange={(e) => setForm({ ...form, incidentId: e.target.value })}
                placeholder="e.g. 1" />
            </label>
            <label>Ward<input required value={form.ward} onChange={(e) => setForm({ ...form, ward: e.target.value })} /></label>
            <label>District<input required value={form.district} onChange={(e) => setForm({ ...form, district: e.target.value })} /></label>
            <label>Severity
              <select value={form.severity} onChange={(e) => setForm({ ...form, severity: e.target.value })}>
                {['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((s) => <option key={s}>{s}</option>)}
              </select>
            </label>
            <label className="span2">Message
              <textarea required rows="3" value={form.message}
                onChange={(e) => setForm({ ...form, message: e.target.value })}
                placeholder="e.g. Flash flood warning for Ward 1 - move to higher ground" />
            </label>
            <div className="span2 modal-actions">
              <button type="button" className="btn" onClick={() => setForm(null)}>Cancel</button>
              <button className="btn primary" disabled={busy}>{busy ? 'Sending...' : 'Send via all channels'}</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
