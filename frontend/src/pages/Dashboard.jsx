import React, { useEffect, useState } from 'react';
import api, { HAZARDS } from '../api.js';
import MapPanel from '../components/MapPanel.jsx';

const STATUS_STYLES = {
  APPROVED: 'ok',
  PENDING: 'warn',
  REJECTED: 'bad',
  CORRECTIONS_REQUESTED: 'info',
};

export default function Dashboard() {
  const [all, setAll] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // allSettled: scoped users (ward recorders, single-hazard supervisors)
    // get 403 on hazards outside their scope - those are simply skipped
    Promise.allSettled(HAZARDS.map((h) => api.get(`/${h}/incidents`)))
      .then((results) => setAll(
        results.filter((r) => r.status === 'fulfilled').flatMap((r) => r.value.data),
      ))
      .finally(() => setLoading(false));
  }, []);

  const approved = all.filter((i) => i.status === 'APPROVED');
  const counts = {
    total: all.length,
    approved: approved.length,
    pending: all.filter((i) => i.status === 'PENDING').length,
    corrections: all.filter((i) => i.status === 'CORRECTIONS_REQUESTED').length,
  };
  const recent = [...all]
    .sort((a, b) => (b.occurredAt || '').localeCompare(a.occurredAt || ''))
    .slice(0, 8);

  return (
    <div>
      <h2>Dashboard - Rushinga District</h2>
      {error && <div className="alert bad">{error}</div>}
      {loading ? (
        <div className="loading">Loading incidents...</div>
      ) : (
        <>
          <div className="stat-grid">
            <div className="stat"><b>{counts.total}</b><span>incidents total</span></div>
            <div className="stat ok"><b>{counts.approved}</b><span>approved</span></div>
            <div className="stat warn"><b>{counts.pending}</b><span>pending review</span></div>
            <div className="stat info"><b>{counts.corrections}</b><span>corrections requested</span></div>
          </div>

          <div className="card">
            <h3>Approved incidents map</h3>
            <p className="muted">Only approved records appear on the public dashboard map (FR-DSH).</p>
            <MapPanel incidents={approved} />
          </div>

          <div className="card">
            <h3>Most recent incidents (all statuses, per your scope)</h3>
            <table className="table">
              <thead>
                <tr>
                  <th>Hazard</th><th>Title</th><th>Ward</th><th>Severity</th><th>Status</th><th>Occurred</th>
                </tr>
              </thead>
              <tbody>
                {recent.map((i) => (
                  <tr key={`${i.hazardType}-${i.id}`}>
                    <td className="cap">{i.hazardType}</td>
                    <td>{i.title}</td>
                    <td>{i.ward}</td>
                    <td>{i.severity}</td>
                    <td><span className={`badge ${STATUS_STYLES[i.status] || ''}`}>{i.status.replace(/_/g, ' ')}</span></td>
                    <td>{i.occurredAt}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
