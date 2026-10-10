import React from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight, ExternalLink, ShieldCheck, Printer } from 'lucide-react';

export interface BreadcrumbItem {
  label: string;
  href?: string;
  to?: string;
}

export interface PageTemplateProps {
  title: string;
  subtitle?: string;
  intro?: string;
  breadcrumbs: BreadcrumbItem[];
  sourceUrl?: string;
  sourceLabel?: string;
  lastVerified?: string;
  lastVerifiedDate?: string;
  actions?: React.ReactNode;
  children: React.ReactNode;
}

export const SourceVerifiedBadge: React.FC<{
  sourceUrl?: string;
  sourceLabel?: string;
  lastVerified?: string;
  className?: string;
}> = ({
  sourceUrl = 'https://indianvisaonline.gov.in/evisa/tvoa.html',
  sourceLabel = 'Bureau of Immigration / NIC Official Portal',
  lastVerified = 'Current & Verified',
  className = '',
}) => {
  return (
    <div
      className={`inline-flex flex-wrap items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-100/90 border border-slate-200 text-xs text-slate-600 ${className}`}
    >
      <div className="flex items-center space-x-1.5 text-emerald-700 font-semibold">
        <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
        <span>Official Source:</span>
      </div>
      <a
        href={sourceUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="font-medium text-brand-700 hover:text-brand-900 underline underline-offset-2 flex items-center space-x-1"
        title="View primary official government source"
      >
        <span>{sourceLabel}</span>
        <ExternalLink className="w-3 h-3 inline ml-0.5 opacity-70" />
      </a>
      <span className="text-slate-300">·</span>
      <span className="text-slate-500 text-[11px]">
        Last verified: <strong className="text-slate-700">{lastVerified}</strong>
      </span>
    </div>
  );
};

export const PageTemplate: React.FC<PageTemplateProps> = ({
  title,
  subtitle,
  intro,
  breadcrumbs,
  sourceUrl = 'https://indianvisaonline.gov.in/evisa/tvoa.html',
  sourceLabel = 'indianvisaonline.gov.in',
  lastVerified,
  lastVerifiedDate,
  actions,
  children,
}) => {
  const displaySubtitle = subtitle || intro;
  const displayVerified = lastVerified || lastVerifiedDate || 'Current & Verified';

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="min-h-screen bg-slate-50/50 pb-20">
      {/* Page Header Banner */}
      <section className="bg-white border-b border-slate-200/80 pt-8 pb-10 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto">
          {/* Breadcrumbs Navigation */}
          <nav aria-label="Breadcrumb" className="mb-6">
            <ol className="flex items-center space-x-2 text-xs text-slate-500">
              <li>
                <Link to="/" className="hover:text-brand-600 font-medium transition-colors">
                  Home
                </Link>
              </li>
              {breadcrumbs.map((crumb, index) => {
                const isLast = index === breadcrumbs.length - 1;
                const linkTarget = crumb.href || crumb.to;
                return (
                  <li key={index} className="flex items-center space-x-2">
                    <ChevronRight className="w-3.5 h-3.5 text-slate-400 flex-shrink-0" />
                    {linkTarget && !isLast ? (
                      <Link to={linkTarget} className="hover:text-brand-600 font-medium transition-colors">
                        {crumb.label}
                      </Link>
                    ) : (
                      <span className="text-slate-900 font-semibold truncate max-w-xs sm:max-w-md" aria-current="page">
                        {crumb.label}
                      </span>
                    )}
                  </li>
                );
              })}
            </ol>
          </nav>

          {/* Heading, Meta & Action Controls */}
          <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-6">
            <div className="max-w-3xl">
              <h1 className="font-serif text-3xl sm:text-4xl lg:text-5xl font-bold text-slate-900 tracking-tight leading-tight mb-3">
                {title}
              </h1>
              {displaySubtitle && (
                <p className="text-slate-600 text-sm sm:text-base leading-relaxed mb-4">
                  {displaySubtitle}
                </p>
              )}

              {/* Source & Last Verified Chip */}
              <SourceVerifiedBadge
                sourceUrl={sourceUrl}
                sourceLabel={sourceLabel}
                lastVerified={displayVerified}
              />
            </div>

            {/* Custom Header Actions & Print Button */}
            <div className="flex items-center space-x-3 self-start lg:self-end flex-shrink-0 print:hidden">
              <button
                type="button"
                onClick={handlePrint}
                className="inline-flex items-center space-x-1.5 px-3.5 py-2 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 text-xs font-semibold shadow-sm transition-colors"
                title="Print this page or save as PDF"
              >
                <Printer className="w-4 h-4 text-slate-500" />
                <span>Print / PDF</span>
              </button>
              {actions}
            </div>
          </div>
        </div>
      </section>

      {/* Main Page Content Body */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-10">
        {children}
      </main>
    </div>
  );
};
