import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { PageTemplate } from '../components/PageTemplate';
import { fetchNoticeById, NoticeDto } from '../api/content';

export const NoticeDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [notice, setNotice] = useState<NoticeDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    fetchNoticeById(id)
      .then((data) => {
        setNotice(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error('Failed to load notice:', err);
        setError('Notice could not be retrieved.');
        setLoading(false);
      });
  }, [id]);

  if (loading) {
    return (
      <PageTemplate
        title="Loading Advisory..."
        breadcrumbs={[{ label: 'Notices', to: '/notices' }, { label: 'Loading...' }]}
      >
        <div className="py-20 text-center">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-teal-600 border-t-transparent mb-3" />
          <p className="text-slate-600 text-xs">Retrieving official gazette text...</p>
        </div>
      </PageTemplate>
    );
  }

  if (error || !notice) {
    return (
      <PageTemplate
        title="Notice Not Found"
        breadcrumbs={[{ label: 'Notices', to: '/notices' }, { label: 'Not Found' }]}
      >
        <div className="p-8 text-center bg-white rounded-xl border border-slate-200">
          <h2 className="text-lg font-bold text-slate-800 mb-2">Notice ID Invalid</h2>
          <p className="text-slate-600 text-xs mb-6">
            The requested circular could not be found in the official advisory repository.
          </p>
          <Link
            to="/notices"
            className="px-5 py-2.5 bg-teal-700 text-white rounded-lg text-xs font-semibold hover:bg-teal-800"
          >
            ← View All Active Notices
          </Link>
        </div>
      </PageTemplate>
    );
  }

  return (
    <PageTemplate
      title={notice.title}
      intro={notice.summary}
      breadcrumbs={[{ label: 'Notices', to: '/notices' }, { label: notice.noticeId }]}
      sourceUrl={notice.sourceUrl}
      lastVerifiedDate={new Date(notice.publishedAt).toLocaleDateString()}
    >
      <div className="max-w-3xl mx-auto bg-white rounded-2xl border border-slate-200 p-8 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3 pb-4 border-b border-slate-100 mb-6">
          <div className="flex items-center gap-2">
            <span className="font-mono text-xs font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
              {notice.noticeId}
            </span>
            <span className="text-xs font-bold px-2 py-0.5 rounded bg-teal-100 text-teal-800 uppercase">
              {notice.category}
            </span>
          </div>

          <span className="text-xs text-slate-400">
            Published: {new Date(notice.publishedAt).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}
          </span>
        </div>

        <div className="prose prose-slate max-w-none text-sm text-slate-700 leading-relaxed space-y-4">
          <p className="font-medium text-slate-900">
            {notice.summary}
          </p>
          <hr className="my-4 border-slate-100" />
          <div className="whitespace-pre-line text-xs leading-relaxed text-slate-700">
            {notice.body}
          </div>
        </div>

        <div className="mt-8 pt-6 border-t border-slate-200 flex flex-wrap items-center justify-between gap-4">
          <a
            href={notice.sourceUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-xs font-bold text-teal-700 hover:text-teal-900 flex items-center gap-1"
          >
            Verify at Official Portal (BOI / MHA) ↗
          </a>

          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => window.print()}
              className="px-3.5 py-2 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-lg text-xs font-medium cursor-pointer"
            >
              Print Advisory
            </button>
            <Link
              to="/notices"
              className="px-3.5 py-2 bg-teal-700 hover:bg-teal-800 text-white rounded-lg text-xs font-medium"
            >
              All Notices
            </Link>
          </div>
        </div>
      </div>
    </PageTemplate>
  );
};
