import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { PageTemplate } from '../components/PageTemplate';
import { fetchNotices, NoticeDto } from '../api/content';

export const NoticesPage: React.FC = () => {
  const [notices, setNotices] = useState<NoticeDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  useEffect(() => {
    fetchNotices()
      .then((data) => {
        setNotices(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error('Error fetching notices:', err);
        setError('Failed to load official travel advisories feed.');
        setLoading(false);
      });
  }, []);

  const filteredNotices = notices.filter((n) => {
    if (categoryFilter !== 'ALL' && n.category !== categoryFilter) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      return (
        n.title.toLowerCase().includes(q) ||
        n.summary.toLowerCase().includes(q) ||
        n.noticeId.toLowerCase().includes(q)
      );
    }
    return true;
  });

  const getSeverityBadge = (sev: string) => {
    switch (sev) {
      case 'CRITICAL':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-red-100 text-red-800 border border-red-300">CRITICAL ADVISORY</span>;
      case 'HIGH':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-300">HIGH PRIORITY</span>;
      default:
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-700 border border-slate-300">OFFICIAL NOTICE</span>;
    }
  };

  const handleExportRss = () => {
    const rssXml = `<?xml version="1.0" encoding="UTF-8" ?>
<rss version="2.0">
<channel>
  <title>VisaSetu Official Travel Advisories & Notices</title>
  <link>https://visasetu.demo/notices</link>
  <description>Official alerts and regulatory changes from Bureau of Immigration</description>
  ${notices.map((n) => `
  <item>
    <title>${n.title}</title>
    <link>https://visasetu.demo/notices/${n.noticeId}</link>
    <description>${n.summary}</description>
    <pubDate>${new Date(n.publishedAt).toUTCString()}</pubDate>
  </item>`).join('')}
</channel>
</rss>`;

    const blob = new Blob([rssXml], { type: 'application/rss+xml' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'visasetu-notices-feed.xml';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <PageTemplate
      title="Official Public Notices & Consular Advisories"
      intro="Verified circulars, port alterations, health protocols, and policy directives published by the Ministry of External Affairs, Ministry of Health, and Bureau of Immigration."
      breadcrumbs={[{ label: 'Notices' }]}
      sourceUrl="https://boi.gov.in/"
      lastVerifiedDate="October 2026"
    >
      {/* Filtering & Export Bar */}
      <div className="bg-white rounded-xl border border-slate-200 p-5 mb-8 shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex flex-wrap items-center gap-3">
            <input
              type="text"
              placeholder="Search advisories & notices..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="text-xs px-3 py-2 bg-slate-50 border border-slate-300 rounded-lg focus:ring-teal-500 focus:border-teal-500 min-w-[240px]"
            />

            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              className="text-xs px-3 py-2 bg-slate-50 border border-slate-300 rounded-lg font-medium"
            >
              <option value="ALL">All Categories</option>
              <option value="POLICY">Policy Directives</option>
              <option value="ENTRY_POINTS">Entry Points & VoA</option>
              <option value="FEES">Fee Schedules</option>
              <option value="SYSTEM">System & Fraud Alerts</option>
            </select>
          </div>

          <button
            type="button"
            onClick={handleExportRss}
            className="px-3.5 py-2 bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-semibold rounded-lg flex items-center gap-2 self-start md:self-auto transition-colors cursor-pointer"
          >
            <span>📡</span> Export RSS Feed (XML)
          </button>
        </div>
      </div>

      {loading && (
        <div className="py-16 text-center">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-teal-600 border-t-transparent mb-3" />
          <p className="text-slate-600 text-xs font-medium">Fetching active notices...</p>
        </div>
      )}

      {error && (
        <div className="p-4 mb-6 bg-red-50 border border-red-200 rounded-lg text-red-700 text-xs">
          {error}
        </div>
      )}

      {!loading && (
        <div className="space-y-4 mb-12">
          {filteredNotices.map((n) => (
            <div
              key={n.noticeId}
              className={`bg-white rounded-xl border p-6 transition-all hover:shadow-sm ${
                n.isPinnedHome ? 'border-teal-400 ring-1 ring-teal-200 bg-teal-50/20' : 'border-slate-200'
              }`}
            >
              <div className="flex flex-wrap items-center justify-between gap-2 mb-2">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-xs font-bold text-slate-400">
                    {n.noticeId}
                  </span>
                  {getSeverityBadge(n.severity)}
                  {n.isPinnedHome && (
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-teal-100 text-teal-800">
                      PINNED ADVISORY
                    </span>
                  )}
                </div>
                <span className="text-xs text-slate-400">
                  {new Date(n.publishedAt).toLocaleDateString('en-US', {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric',
                  })}
                </span>
              </div>

              <h2 className="text-base font-bold text-slate-900 mb-2">
                <Link to={`/notices/${n.noticeId}`} className="hover:text-teal-700 transition-colors">
                  {n.title}
                </Link>
              </h2>

              <p className="text-xs text-slate-600 leading-relaxed mb-4">
                {n.summary}
              </p>

              <div className="flex items-center justify-between pt-3 border-t border-slate-100 text-xs">
                <span className="text-slate-400">
                  Category: <strong className="text-slate-700 uppercase">{n.category}</strong>
                </span>

                <Link
                  to={`/notices/${n.noticeId}`}
                  className="font-semibold text-teal-700 hover:text-teal-900 flex items-center gap-1"
                >
                  Read Complete Advisory & Gazette Reference →
                </Link>
              </div>
            </div>
          ))}

          {filteredNotices.length === 0 && (
            <div className="bg-white rounded-xl border border-slate-200 p-12 text-center text-slate-400">
              No notices match your filter parameters.
            </div>
          )}
        </div>
      )}
    </PageTemplate>
  );
};
