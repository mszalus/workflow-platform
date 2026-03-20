import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@wfp/shared-ui';
import type { AuditEntry } from '@wfp/shared-ui';

export default function AuditLog() {
  const [filters, setFilters] = useState({ entityType: '', userId: '', page: 0 });

  const { data, isLoading } = useQuery({
    queryKey: ['audit-log', filters],
    queryFn: () => {
      const params = new URLSearchParams();
      if (filters.entityType) params.set('entityType', filters.entityType);
      if (filters.userId) params.set('userId', filters.userId);
      params.set('page', String(filters.page));
      params.set('size', '20');
      return apiClient.get(`/api/audit/audit?${params}`).then((r) => r.data);
    },
  });

  return (
    <div>
      <h1 style={{ marginBottom: '1rem' }}>Audit Log</h1>
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
        <select value={filters.entityType} onChange={(e) => setFilters({ ...filters, entityType: e.target.value, page: 0 })} style={{ padding: '0.4rem' }}>
          <option value="">All Types</option>
          <option value="PROCESS">Process</option>
          <option value="TASK">Task</option>
        </select>
        <input placeholder="User ID" value={filters.userId} onChange={(e) => setFilters({ ...filters, userId: e.target.value, page: 0 })} style={{ padding: '0.4rem' }} />
      </div>
      {isLoading ? <p>Loading...</p> : (
        <table style={{ width: '100%', background: '#fff', borderCollapse: 'collapse', borderRadius: 8 }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #eee' }}>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Timestamp</th>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Event</th>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Entity</th>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>Entity ID</th>
              <th style={{ textAlign: 'left', padding: '0.75rem' }}>User</th>
            </tr>
          </thead>
          <tbody>
            {(data?.content ?? []).map((e: AuditEntry) => (
              <tr key={e.id} style={{ borderBottom: '1px solid #eee' }}>
                <td style={{ padding: '0.75rem', fontSize: '0.85rem' }}>{new Date(e.timestamp).toLocaleString()}</td>
                <td style={{ padding: '0.75rem' }}>{e.eventType}</td>
                <td style={{ padding: '0.75rem' }}>{e.entityType}</td>
                <td style={{ padding: '0.75rem', fontFamily: 'monospace', fontSize: '0.85rem' }}>{e.entityId}</td>
                <td style={{ padding: '0.75rem' }}>{e.userId}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {data && (
        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem', justifyContent: 'center' }}>
          <button disabled={filters.page === 0} onClick={() => setFilters({ ...filters, page: filters.page - 1 })} style={{ padding: '0.3rem 0.8rem' }}>Prev</button>
          <span style={{ padding: '0.3rem' }}>Page {filters.page + 1} of {data.totalPages || 1}</span>
          <button disabled={data.last} onClick={() => setFilters({ ...filters, page: filters.page + 1 })} style={{ padding: '0.3rem 0.8rem' }}>Next</button>
        </div>
      )}
    </div>
  );
}
