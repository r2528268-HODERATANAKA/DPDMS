import React, { useEffect, useRef } from 'react';
import L from 'leaflet';

const STATUS_COLORS = {
  APPROVED: '#2e9e5b',
  PENDING: '#e6a817',
  REJECTED: '#d64545',
  CORRECTIONS_REQUESTED: '#3b82d6',
};

/**
 * Leaflet map of Rushinga district. Circle markers avoid the
 * default icon-image dependency, so the map works fully offline
 * except for the OSM tiles.
 */
export default function MapPanel({ incidents, height = 430 }) {
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const layerRef = useRef(null);

  useEffect(() => {
    if (mapRef.current || !containerRef.current) return;
    mapRef.current = L.map(containerRef.current).setView([-16.72, 32.34], 9);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 18,
    }).addTo(mapRef.current);
    layerRef.current = L.layerGroup().addTo(mapRef.current);
  }, []);

  useEffect(() => {
    const layer = layerRef.current;
    if (!layer) return;
    layer.clearLayers();
    (incidents || [])
      .filter((i) => i.latitude != null && i.longitude != null)
      .forEach((i) => {
        const marker = L.circleMarker([i.latitude, i.longitude], {
          radius: 8,
          color: '#ffffff',
          weight: 1.5,
          fillColor: STATUS_COLORS[i.status] || '#8b8b8b',
          fillOpacity: 0.92,
        });
        marker.bindPopup(
          `<b>${i.title}</b><br/>${i.hazardType} - ${i.ward}<br/>` +
          `Severity: ${i.severity}<br/>Status: ${i.status}<br/>` +
          `Occurred: ${i.occurredAt || '-'}`,
        );
        marker.addTo(layer);
      });
  }, [incidents]);

  return (
    <div className="map-wrap">
      <div ref={containerRef} style={{ height, borderRadius: 10 }} />
      <div className="map-legend">
        {Object.entries(STATUS_COLORS).map(([status, color]) => (
          <span key={status}>
            <i style={{ background: color }} /> {status.replace(/_/g, ' ').toLowerCase()}
          </span>
        ))}
      </div>
    </div>
  );
}
