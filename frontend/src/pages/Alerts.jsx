import React, { useCallback, useEffect, useState } from 'react';
import api, { HAZARDS, errorMessage } from '../api.js';

/** Alert log (Provincial Administrator + System Administrator only). */
export default function Alerts() {
  const [rows, setRows] = useState([]);
  const [hazard, setHazard] = useState('');
  const [channel, setChannel] = useState('');
  const [status, setStatus] = useState('');
  const [error, setError] = useState('');

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

  return (
    <div>
      <div className="page-head">
        <h2>Alert log</h2>
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
    </div>
  );
}
