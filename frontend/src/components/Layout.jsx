import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../App.jsx';

const ROLE_LABELS = {
  SYSTEM_ADMIN: 'System Administrator',
  PROVINCIAL_ADMIN: 'Provincial Administrator',
  PROVINCIAL_SUPERVISOR: 'Provincial Supervisor',
  WARD_RECORDER: 'Ward Recorder',
  NATIONAL_USER: 'National Observer',
};

export default function Layout({ children }) {
  const { user, signOut, canReview, canReport, canAlerts, isAdmin } = useAuth();

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="side-brand">
          <span className="logo small">DPDMS</span>
          <span>Rushinga</span>
        </div>
        <nav>
          <NavLink to="/" end>Dashboard</NavLink>
          <NavLink to="/incidents">Incidents</NavLink>
          {canReview && <NavLink to="/approvals">Approvals</NavLink>}
          {canReport && <NavLink to="/reports">Reports</NavLink>}
          {canAlerts && <NavLink to="/alerts">Alert Log</NavLink>}
          {isAdmin && <NavLink to="/users">Users</NavLink>}
        </nav>
        <div className="side-user">
          <div className="who">
            <b>{user?.fullName}</b>
            <span>{ROLE_LABELS[user?.role] || user?.role}</span>
            {user?.ward && user?.ward !== '*' && <span className="scope">{user.ward} - {user.hazard}</span>}
          </div>
          <button className="btn ghost" onClick={signOut}>Sign out</button>
        </div>
      </aside>
      <main className="content">{children}</main>
    </div>
  );
}
