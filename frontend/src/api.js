import axios from 'axios';

export const HAZARDS = ['flood', 'drought', 'fire', 'zoonotic', 'mining'];

// The team backend exposes one route per hazard under its plural name.
export const PLURAL = {
  flood: 'floods', drought: 'droughts', fire: 'fires',
  zoonotic: 'zoonotics', mining: 'minings',
};

// Every hazard service uses the same Severity enum (LOW/MEDIUM/HIGH/CRITICAL).
const SEVERITY = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
export const SEVERITY_OPTIONS = {
  flood: SEVERITY, drought: SEVERITY, fire: SEVERITY,
  zoonotic: SEVERITY, mining: SEVERITY,
};

// Hazard-specific indicator fields, matching the team's entities exactly.
// type 'select' renders a dropdown of `options`.
export const HAZARD_FIELDS = {
  flood: [
    { key: 'peakWaterLevelMetres', label: 'Peak water level (m)', type: 'number', required: true },
    { key: 'riverBasin', label: 'River basin / catchment', type: 'text', required: true },
    { key: 'householdsDisplaced', label: 'Households displaced', type: 'number', required: true },
    { key: 'areaFloodedHectares', label: 'Area flooded (ha)', type: 'number', required: true },
    { key: 'inundationDurationDays', label: 'Inundation duration (days)', type: 'number', required: true },
  ],
  drought: [
    { key: 'rainfallDeficitMm', label: 'Rainfall deficit (mm)', type: 'number', required: true },
    { key: 'consecutiveDryDays', label: 'Consecutive dry days', type: 'number', required: true },
    { key: 'affectedHouseholds', label: 'Affected households', type: 'number', required: true },
    { key: 'livestockDeaths', label: 'Livestock deaths', type: 'number', required: true },
    { key: 'cropDamageHectares', label: 'Crop damage (ha)', type: 'number', required: true },
    { key: 'waterSourceCondition', label: 'Water source condition', type: 'select', options: ['NORMAL', 'STRESSED', 'CRITICAL', 'DRY'], required: true },
  ],
  fire: [
    { key: 'areaBurnedHectares', label: 'Burnt area (ha)', type: 'number', required: true },
    { key: 'suspectedCause', label: 'Suspected cause', type: 'text', required: true },
    { key: 'injuries', label: 'Injuries', type: 'number', required: true },
    { key: 'fatalities', label: 'Fatalities', type: 'number', required: true },
    { key: 'structuresDestroyed', label: 'Structures destroyed', type: 'number', required: true },
    { key: 'fireStatus', label: 'Fire status', type: 'select', options: ['ACTIVE', 'CONTAINED'], required: true },
  ],
  zoonotic: [
    { key: 'diseaseName', label: 'Disease name', type: 'text', required: true },
    { key: 'suspectedAnimalSpecies', label: 'Suspected animal species', type: 'text' },
    { key: 'humanCases', label: 'Human cases', type: 'number', required: true },
    { key: 'animalsAffected', label: 'Animals affected', type: 'number', required: true },
    { key: 'humanDeaths', label: 'Human deaths', type: 'number', required: true },
    { key: 'outbreakStatus', label: 'Outbreak status', type: 'select', options: ['SUSPECTED', 'CONFIRMED', 'UNDER_CONTROL'], required: true },
  ],
  mining: [
    { key: 'mineName', label: 'Mine name', type: 'text', required: true },
    { key: 'accidentType', label: 'Accident type', type: 'select', options: ['ROCKFALL', 'FLOODING', 'GAS_LEAK', 'EQUIPMENT_FAILURE', 'OTHER'], required: true },
    { key: 'casualties', label: 'Casualties', type: 'number', required: true },
    { key: 'rescued', label: 'Rescued', type: 'number', required: true },
    { key: 'mineOperationalStatus', label: 'Mine status', type: 'select', options: ['OPERATIONAL', 'SUSPENDED', 'CLOSED'], required: true },
  ],
};

// Fields present on every team hazard entity (the "shared metadata contract").
const CORE = new Set([
  'id', 'ward', 'district', 'province', 'occurredAt', 'reporter', 'severity', 'status',
  'latitude', 'longitude', 'reviewedBy', 'reviewedAt', 'reviewNotes', 'createdAt',
]);

const cap = (s) => (s ? s[0].toUpperCase() + s.slice(1) : s);

function safeAuth() {
  try { return JSON.parse(localStorage.getItem('dpdms_auth')); } catch { return null; }
}

// Convert the UI form into the exact body a team hazard entity expects.
function toTeamIncident(form, hazard) {
  const auth = safeAuth();
  const body = { ...form };
  body.reporter = form.reporter || auth?.user?.fullName || auth?.user?.username || 'unknown';
  body.district = form.district || 'Mudzi';
  body.province = form.province || 'Mashonaland East';
  if (body.occurredAt && body.occurredAt.length === 10) body.occurredAt += 'T00:00:00';
  delete body.title; // no title field on the team entities
  return body;
}

// Convert a team hazard entity into the shape the pages render.
function toUiIncident(e, hazard) {
  const details = {};
  Object.keys(e).forEach((k) => { if (!CORE.has(k)) details[k] = e[k]; });
  return {
    ...e,
    hazardType: hazard,
    title: e.title || `${cap(hazard)} incident #${e.id}`,
    reportedBy: e.reporter,
    reviewComment: e.reviewNotes,
    occurredAt: (e.occurredAt || '').slice(0, 10),
    details,
    recommendedActions: [],
  };
}

const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use((config) => {
  const auth = safeAuth();
  if (auth?.token) config.headers.Authorization = `Bearer ${auth.token}`;

  // Translate the UI's REST-ish calls onto the team backend's contract.
  const m = config.url.match(
    /^\/(flood|drought|fire|zoonotic|mining)\/incidents(?:\/(\d+))?(?:\/(approve|reject|request-corrections))?$/,
  );
  if (m) {
    const [, hazard, id, decision] = m;
    const p = PLURAL[hazard];
    config.__hazard = hazard; // remembered so the response mapper knows the hazard

    if (decision) {
      const comment = config.data?.comment ?? '';
      config.method = 'patch';
      config.url = `/${p}/${id}/${decision}`;
      config.data = decision === 'reject' ? { reason: comment }
        : decision === 'request-corrections' ? { notes: comment }
          : {};
    } else if (id) {
      config.url = `/${p}/${id}`;
      config.data = toTeamIncident(config.data, hazard);
    } else if (config.method === 'get') {
      config.url = `/${p}/scoped`; // role-scoped (recorders: own ward; supervisors/admins: all)
    } else {
      config.url = `/${p}`;
      config.data = toTeamIncident(config.data, hazard);
    }
  }
  return config;
});

api.interceptors.response.use(
  (res) => {
    const hazard = res.config?.__hazard;
    if (hazard) {
      res.data = Array.isArray(res.data)
        ? res.data.map((e) => toUiIncident(e, hazard))
        : toUiIncident(res.data, hazard);
    } else if ((res.config?.url || '').includes('/auth/users')) {
      res.data = Array.isArray(res.data)
        ? res.data.map((u) => ({ ...u, enabled: u.active }))
        : { ...res.data, enabled: res.data?.active };
    } else if ((res.config?.url || '').includes('/alerts')) {
      res.data = Array.isArray(res.data)
        ? res.data.map((a) => ({ ...a, hazardType: a.hazard }))
        : res.data;
    }
    return res;
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('dpdms_auth');
      if (!window.location.hash.startsWith('#/login')) {
        window.location.hash = '#/login';
        window.location.reload();
      }
    }
    return Promise.reject(err);
  },
);

export function errorMessage(err) {
  const data = err.response?.data;
  if (typeof data === 'string') return data;
  return data?.message || err.message || 'Request failed';
}

export default api;
