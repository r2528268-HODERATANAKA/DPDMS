import React, { useCallback, useEffect, useState } from 'react';
import api, { HAZARDS, errorMessage } from '../api.js';
import { Modal } from './Incidents.jsx';

/**
 * Review queue across all hazards for provincial roles:
 * everything PENDING or CORRECTIONS_REQUESTED.
 */
export default function Approvals() {
  const [rows, setRows] = useState([]);
  const [hazardFilter, setHazardFilter] = useState('');
  const [error, setError] = useState('');
  const [review, setReview] = useState(null);
  const [comment, setComment] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    setError('');
    // allSettled: single-hazard supervisors get 403 on other hazards - skip them
    Promise.allSettled(HAZARDS.map((h) => api.get(`/${h}/incidents`)))
      .then((results) => {
        const all = results
          .filter((r) => r.status === 'fulfilled')
          .flatMap((r) => r.value.data);
        setRows(all.filter((i) => i.status === 'PENDING' || i.status === 'CORRECTIONS_REQUESTED'));
      })
      .catch((err) => setError(errorMessage(err)));
  }, []);

  useEffect(() => { load(); }, [load]);

  const submitReview = async () => {
    setBusy(true);
    setError('');
    try {
      await api.post(`/${review.hazardType}/incidents/${review.id}/${review.decision}`, { comment });
      setReview(null);
      setComment('');
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const visible = hazardFilter ? rows.filter((r) => r.hazardType === hazardFilter) : rows;

  return (
    <div>
      <div className="page-head">
        <h2>Approval queue</h2>
        <select value={hazardFilter} onChange={(e) => setHazardFilter(e.target.value)}>
          <option value="">All hazards</option>
          {HAZARDS.map((h) => <option key={h}>{h}</option>)}
        </select>
      </div>

      {error && <div className="alert bad">{error}</div>}

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>Hazard</th><th>Title</th><th>Ward</th><th>Severity</th>
              <th>Status</th><th>Reported by</th><th></th>
            </tr>
          </thead>
          <tbody>
            {visible.map((i) => (
              <tr key={`${i.hazardType}-${i.id}`}>
                <td className="cap">{i.hazardType}</td>
                <td>{i.title}</td>
                <td>{i.ward}</td>
                <td>{i.severity}</td>
                <td>
                  <span className={`badge ${i.status === 'PENDING' ? 'warn' : 'info'}`}>
                    {i.status.replace(/_/g, ' ')}
                  </span>
                </td>
                <td>{i.reportedBy}</td>
                <td className="row-actions">
                  <button className="btn tiny ok" onClick={() => { setReview({ ...i, decision: 'approve' }); setComment(''); }}>Approve</button>
                  <button className="btn tiny bad" onClick={() => { setReview({ ...i, decision: 'reject' }); setComment(''); }}>Reject</button>
                  <button className="btn tiny info" onClick={() => { setReview({ ...i, decision: 'request-corrections' }); setComment(''); }}>Request corrections</button>
                </td>
              </tr>
            ))}
            {visible.length === 0 && (
              <tr><td colSpan="7" className="muted">Queue is clear - nothing waiting for review.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {review && (
        <Modal title={`${review.decision.replace(/-/g, ' ')} - ${review.title}`} onClose={() => setReview(null)}>
          <p className="muted">
            Approving publishes an alert event to RabbitMQ; alert-service fans out
            Email + WhatsApp to the ward recorder and provincial officers.
          </p>
          <textarea rows="4" value={comment} onChange={(e) => setComment(e.target.value)}
            placeholder="Comment recorded in the audit trail" />
          <div className="modal-actions">
            <button className="btn" onClick={() => setReview(null)}>Cancel</button>
            <button className="btn primary" disabled={busy} onClick={submitReview}>Confirm {review.decision.replace(/-/g, ' ')}</button>
          </div>
        </Modal>
      )}
    </div>
  );
}
