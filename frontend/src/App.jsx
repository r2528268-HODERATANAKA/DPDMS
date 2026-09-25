import React, { createContext, useContext, useState } from 'react';
import { HashRouter, Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Incidents from './pages/Incidents.jsx';
import Approvals from './pages/Approvals.jsx';
import Reports from './pages/Reports.jsx';
import Alerts from './pages/Alerts.jsx';
import Users from './pages/Users.jsx';

const AuthCtx = createContext(null);
export const useAuth = () => useContext(AuthCtx);

export default function App() {
  const [auth, setAuth] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('dpdms_auth'));
    } catch {
      return null;
    }
  });

  const signIn = (payload) => {
    localStorage.setItem('dpdms_auth', JSON.stringify(payload));
    setAuth(payload);
  };

  const signOut = () => {
    localStorage.removeItem('dpdms_auth');
    setAuth(null);
  };

  const user = auth?.user || null;
  const role = user?.role || '';

  const canReview = ['PROVINCIAL_SUPERVISOR', 'PROVINCIAL_ADMIN', 'SYSTEM_ADMIN'].includes(role);
  const canReport = role !== 'WARD_RECORDER' && role !== '';
  const canAlerts = ['PROVINCIAL_ADMIN', 'SYSTEM_ADMIN'].includes(role);
  const isAdmin = role === 'SYSTEM_ADMIN';

  return (
    <AuthCtx.Provider value={{ auth, user, signIn, signOut, canReview, canReport, canAlerts, isAdmin }}>
      <HashRouter>
        {!user ? (
          <Login />
        ) : (
          <Layout>
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/incidents" element={<Incidents />} />
              <Route path="/approvals" element={canReview ? <Approvals /> : <Navigate to="/" replace />} />
              <Route path="/reports" element={canReport ? <Reports /> : <Navigate to="/" replace />} />
              <Route path="/alerts" element={canAlerts ? <Alerts /> : <Navigate to="/" replace />} />
              <Route path="/users" element={isAdmin ? <Users /> : <Navigate to="/" replace />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </Layout>
        )}
      </HashRouter>
    </AuthCtx.Provider>
  );
}
