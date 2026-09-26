import React, { useState } from 'react';
import api, { HAZARDS, errorMessage } from '../api.js';
import { useAuth } from '../App.jsx';

const FORMATS = ['pdf', 'docx', 'xlsx', 'csv'];

export default function Reports() {
  const { user } = useAuth();
  // single-hazard supervisors may only report on their hazard
  const myHazards = user?.role === 'PROVINCIAL_SUPERVISOR' && user?.hazard !== '*'
    ? [user.hazard] : HAZARDS;
  const [hazard, setHazard] = useState(myHazards[0]);
  const [format, setFormat] = useState('pdf');
  const [status, setStatus] = useState('');
  const [busy, setBusy] = useState(false);

  const download = async () => {
    setBusy(true);
    setStatus('');
    try {
      // The team report-service exposes one endpoint: GET /api/reports/{hazard}?format=pdf|docx|xlsx|csv
      const response = await api.get(`/reports/${hazard}`, { params: { format }, responseType: 'blob' });

      const header = response.headers['content-disposition'] || '';
      const match = header.match(/filename="?([^"]+)"?/);
      const filename = match ? match[1] : `dpdms-${hazard}-report.${format}`;

      const url2 = URL.createObjectURL(new Blob([response.data]));
      const a = document.createElement('a');
      a.href = url2;
      a.download = filename;
      a.click();
      URL.revokeObjectURL(url2);
      setStatus(`Downloaded ${filename}`);
    } catch (err) {
      let msg = errorMessage(err);
      if (err.response?.data instanceof Blob) {
        try {
          const text = await err.response.data.text();
          msg = JSON.parse(text).message || msg;
        } catch { /* keep default */ }
      }
      setStatus(msg);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <h2>Reports</h2>
      <p className="muted">Exports all approved {hazard} incidents. Pick a format and download.</p>
      <div className="card narrow">
        <div className="grid-2">
          <label>Hazard
            <select value={hazard} onChange={(e) => setHazard(e.target.value)}>
              {myHazards.map((h) => <option key={h}>{h}</option>)}
            </select>
          </label>
          <label>Format
            <select value={format} onChange={(e) => setFormat(e.target.value)}>
              {FORMATS.map((f) => <option key={f}>{f}</option>)}
            </select>
          </label>
        </div>
        <div className="modal-actions">
          <button className="btn primary" disabled={busy} onClick={download}>
            {busy ? 'Generating...' : 'Generate & download'}
          </button>
        </div>
        {status && <div className="alert info">{status}</div>}
      </div>
    </div>
  );
}
