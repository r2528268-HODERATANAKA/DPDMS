import React, { useCallback, useEffect, useState } from 'react';
import api, { HAZARDS, HAZARD_FIELDS, SEVERITY_OPTIONS, errorMessage } from '../api.js';
import { useAuth } from '../App.jsx';

const STATUS_STYLES = {
  APPROVED: 'ok', PENDING: 'warn', REJECTED: 'bad', CORRECTIONS_REQUESTED: 'info',
};
const EDITABLE = ['PENDING', 'CORRECTIONS_REQUESTED'];
const EMPTY_FORM = {
  ward: 'Ward 1', district: 'Mudzi', province: 'Mashonaland East',
  severity: '',
  latitude: -16.75, longitude: 32.35, occurredAt: new Date().toISOString().slice(0, 10),
};

export default function Incidents() {
  const { user, canReview } = useAuth();
  // ward recorders work in exactly one hazard - hide the other tabs
  const myHazards = user?.role === 'WARD_RECORDER' && user?.hazard !== '*'
    ? [user.hazard] : HAZARDS;
  const [hazard, setHazard] = useState(myHazards[0]);
  const [list, setList] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [error, setError] = useState('');
  const [form, setForm] = useState(null);        // null | object
  const [editId, setEditId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [review, setReview] = useState(null);    // {id, decision}
  const [comment, setComment] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    setError('');
    api.get(`/${hazard}/incidents`)
      .then((r) => setList(r.data))
      .catch((err) => setError(errorMessage(err)));
  }, [hazard]);

  useEffect(() => { load(); }, [load]);

  const isRecorder = user?.role === 'WARD_RECORDER';

  const openCreate = () => {
    setEditId(null);
    const base = { ...EMPTY_FORM, severity: SEVERITY_OPTIONS[hazard][0] };
    (HAZARD_FIELDS[hazard] || []).forEach((f) => { if (f.options) base[f.key] = f.options[0]; });
    setForm(base);
  };

  const openEdit = (incident) => {
    setEditId(incident.id);
    const base = {
      ward: incident.ward,
      district: incident.district,
      province: incident.province,
      severity: incident.severity,
      latitude: incident.latitude,
      longitude: incident.longitude,
      occurredAt: incident.occurredAt,
    };
    (HAZARD_FIELDS[hazard] || []).forEach((f) => {
      base[f.key] = incident.details?.[f.key] ?? (f.options ? f.options[0] : '');
    });
    setForm(base);
  };

  const save = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      if (editId == null) {
        await api.post(`/${hazard}/incidents`, form);
      } else {
        await api.put(`/${hazard}/incidents/${editId}`, form);
      }
      setForm(null);
      setEditId(null);
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const submitReview = async () => {
    setBusy(true);
    setError('');
    try {
      await api.post(`/${hazard}/incidents/${review.id}/${review.decision}`, { comment });
      setReview(null);
      setComment('');
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const remove = async (incident) => {
    if (!window.confirm(`Delete ${incident.title}? This cannot be undone.`)) return;
    setError('');
    try {
      await api.delete(`/${hazard}/incidents/${incident.id}`);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const filtered = statusFilter ? list.filter((i) => i.status === statusFilter) : list;

  return (
    <div>
      <div className="page-head">
        <h2>Incidents</h2>
        {isRecorder && (
          <button className="btn primary" onClick={openCreate}>+ New {hazard} incident</button>
        )}
      </div>

      <div className="tabs">
        {myHazards.map((h) => (
          <button key={h} className={`tab ${h === hazard ? 'active' : ''}`} onClick={() => setHazard(h)}>
            {h}
          </button>
        ))}
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="ml-auto">
          <option value="">All statuses</option>
          <option value="PENDING">PENDING</option>
          <option value="APPROVED">APPROVED</option>
          <option value="REJECTED">REJECTED</option>
          <option value="CORRECTIONS_REQUESTED">CORRECTIONS REQUESTED</option>
        </select>
      </div>

      {error && <div className="alert bad">{error}</div>}

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>Title</th><th>Ward</th><th>Severity</th><th>Status</th>
              <th>Occurred</th><th>Reported by</th><th></th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((i) => (
              <tr key={i.id}>
                <td>{i.title}</td>
                <td>{i.ward}</td>
                <td>{i.severity}</td>
                <td><span className={`badge ${STATUS_STYLES[i.status] || ''}`}>{i.status.replace(/_/g, ' ')}</span></td>
                <td>{i.occurredAt}</td>
                <td>{i.reportedBy}</td>
                <td className="row-actions">
                  <button className="btn tiny" onClick={() => setDetail(i)}>View</button>
                  {isRecorder && EDITABLE.includes(i.status) && (
                    <button className="btn tiny" onClick={() => openEdit(i)}>Edit</button>
                  )}
                  {isRecorder && EDITABLE.includes(i.status) && (
                    <button className="btn tiny bad" onClick={() => remove(i)}>Delete</button>
                  )}
                  {canReview && i.status === 'PENDING' && (
                    <>
                      <button className="btn tiny ok" onClick={() => { setReview({ id: i.id, decision: 'approve' }); setComment(''); }}>Approve</button>
                      <button className="btn tiny bad" onClick={() => { setReview({ id: i.id, decision: 'reject' }); setComment(''); }}>Reject</button>
                      <button className="btn tiny info" onClick={() => { setReview({ id: i.id, decision: 'request-corrections' }); setComment(''); }}>Corrections</button>
                    </>
                  )}
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr><td colSpan="7" className="muted">No incidents found for this scope.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {form && (
        <Modal title={editId == null ? `New ${hazard} incident` : `Edit ${hazard} incident`} onClose={() => setForm(null)}>
          <form onSubmit={save} className="grid-2">
            <label>Ward<input required value={form.ward} onChange={(e) => setForm({ ...form, ward: e.target.value })} /></label>
            <label>District<input required value={form.district} onChange={(e) => setForm({ ...form, district: e.target.value })} /></label>
            <label>Province<input required value={form.province} onChange={(e) => setForm({ ...form, province: e.target.value })} /></label>
            <label>Severity
              <select value={form.severity} onChange={(e) => setForm({ ...form, severity: e.target.value })}>
                {SEVERITY_OPTIONS[hazard].map((s) => <option key={s}>{s}</option>)}
              </select>
            </label>
            <label>Occurred on<input required type="date" value={form.occurredAt} onChange={(e) => setForm({ ...form, occurredAt: e.target.value })} /></label>
            <label>Latitude<input required type="number" step="any" value={form.latitude} onChange={(e) => setForm({ ...form, latitude: parseFloat(e.target.value) })} /></label>
            <label>Longitude<input required type="number" step="any" value={form.longitude} onChange={(e) => setForm({ ...form, longitude: parseFloat(e.target.value) })} /></label>
            {(HAZARD_FIELDS[hazard] || []).map((f) => (
              <label key={f.key}>{f.label}
                {f.options ? (
                  <select
                    required={f.required}
                    value={form[f.key] ?? f.options[0]}
                    onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                  >
                    {f.options.map((o) => <option key={o}>{o}</option>)}
                  </select>
                ) : (
                  <input
                    type={f.type === 'number' ? 'number' : 'text'}
                    step="any"
                    required={f.required}
                    value={form[f.key] ?? ''}
                    onChange={(e) => setForm({ ...form, [f.key]: f.type === 'number' && e.target.value !== '' ? parseFloat(e.target.value) : e.target.value })}
                  />
                )}
              </label>
            ))}
            <div className="span2 modal-actions">
              <button type="button" className="btn" onClick={() => setForm(null)}>Cancel</button>
              <button className="btn primary" disabled={busy}>{busy ? 'Saving...' : 'Save (enters PENDING)'}</button>
            </div>
          </form>
        </Modal>
      )}

      {detail && (
        <Modal title={detail.title} onClose={() => setDetail(null)}>
          <div className="detail-grid">
            <span>Ward</span><b>{detail.ward}</b>
            <span>Severity</span><b>{detail.severity}</b>
            <span>Status</span><b>{detail.status}</b>
            <span>Occurred</span><b>{detail.occurredAt}</b>
            <span>Reported by</span><b>{detail.reportedBy}</b>
            <span>Reviewed by</span><b>{detail.reviewedBy || '-'}</b>
            <span>Review comment</span><b>{detail.reviewComment || '-'}</b>
            <span>GPS</span><b>{detail.latitude}, {detail.longitude}</b>
            {Object.entries(detail.details || {}).map(([k, v]) => (
              <React.Fragment key={k}><span>{k.replace(/([a-z])([A-Z])/g, '$1 $2')}</span><b>{v == null ? '-' : String(v)}</b></React.Fragment>
            ))}
          </div>
          <h4 className="mt">Recommended actions</h4>
          <ol className="actions-list">
            {(detail.recommendedActions || []).map((a, idx) => <li key={idx}>{a}</li>)}
          </ol>
          {detail.reviewComment && <p className="muted">Review comment: {detail.reviewComment}</p>}
        </Modal>
      )}

      {review && (
        <Modal title={review.decision.replace(/-/g, ' ')} onClose={() => setReview(null)}>
          <p className="muted">Add a comment for the record audit trail (optional for approval).</p>
          <textarea rows="4" value={comment} onChange={(e) => setComment(e.target.value)}
            placeholder="e.g. Verified against ward assessment photos" />
          <div className="modal-actions">
            <button className="btn" onClick={() => setReview(null)}>Cancel</button>
            <button className="btn primary" disabled={busy} onClick={submitReview}>Confirm</button>
          </div>
        </Modal>
      )}
    </div>
  );
}

export function Modal({ title, children, onClose }) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <h3>{title}</h3>
          <button className="btn tiny" onClick={onClose}>x</button>
        </div>
        {children}
      </div>
    </div>
  );
}
