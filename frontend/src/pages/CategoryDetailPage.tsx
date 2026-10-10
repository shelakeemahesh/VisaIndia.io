import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { PageTemplate } from '../components/PageTemplate';
import { fetchVisaCategoryBySlug, VisaCategoryDetailDto } from '../api/content';

export const CategoryDetailPage: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const [category, setCategory] = useState<VisaCategoryDetailDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!slug) return;
    setLoading(true);
    fetchVisaCategoryBySlug(slug)
      .then((data) => {
        setCategory(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error('Failed to load category:', err);
        setError('Category details could not be found or failed to load.');
        setLoading(false);
      });
  }, [slug]);

  if (loading) {
    return (
      <PageTemplate
        title="Loading Visa Specification..."
        breadcrumbs={[{ label: 'Visa Categories', to: '/visa-categories' }, { label: 'Loading...' }]}
      >
        <div className="py-20 text-center">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-teal-600 border-t-transparent mb-3" />
          <p className="text-slate-600">Retrieving official category schedule...</p>
        </div>
      </PageTemplate>
    );
  }

  if (error || !category) {
    return (
      <PageTemplate
        title="Visa Category Not Found"
        breadcrumbs={[{ label: 'Visa Categories', to: '/visa-categories' }, { label: 'Not Found' }]}
      >
        <div className="p-8 text-center bg-white rounded-xl border border-slate-200">
          <h2 className="text-xl font-bold text-slate-800 mb-2">Category Not Available</h2>
          <p className="text-slate-600 text-sm mb-6">
            The requested e-Visa category slug does not match any current official sub-category schedule.
          </p>
          <Link
            to="/visa-categories"
            className="px-5 py-2.5 bg-teal-700 text-white rounded-lg text-sm font-semibold hover:bg-teal-800 transition-colors"
          >
            ← View All Active Categories
          </Link>
        </div>
      </PageTemplate>
    );
  }

  return (
    <PageTemplate
      title={category.name}
      intro={category.description}
      breadcrumbs={[
        { label: 'Visa Categories', to: '/visa-categories' },
        { label: category.name },
      ]}
      sourceUrl={category.officialSourceUrl}
      lastVerifiedDate={category.lastVerifiedAt ? new Date(category.lastVerifiedAt).toLocaleDateString() : 'October 2026'}
    >
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-12">
        {/* Left 2 Cols: Comprehensive Overview */}
        <div className="lg:col-span-2 space-y-6">
          {/* Target Audience */}
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
            <h2 className="text-lg font-bold text-slate-900 mb-3 flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-teal-600" />
              Eligible Travelers & Permitted Activities
            </h2>
            <p className="text-sm text-slate-700 leading-relaxed">
              {category.whoIsItFor}
            </p>
          </div>

          {/* Key Parameters Matrix */}
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
            <h2 className="text-lg font-bold text-slate-900 mb-4 flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-teal-600" />
              Statutory Immigration Provisions
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
              <div className="p-3.5 bg-slate-50 rounded-lg border border-slate-100">
                <span className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">
                  Validity Duration
                </span>
                <span className="font-bold text-slate-900">{category.validityText}</span>
              </div>

              <div className="p-3.5 bg-slate-50 rounded-lg border border-slate-100">
                <span className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">
                  Authorized Entries
                </span>
                <span className="font-bold text-slate-900">{category.entries} Entry</span>
              </div>

              <div className="p-3.5 bg-slate-50 rounded-lg border border-slate-100">
                <span className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">
                  Maximum Continuous Stay
                </span>
                <span className="font-bold text-slate-900">{category.maxStayText}</span>
              </div>

              <div className="p-3.5 bg-slate-50 rounded-lg border border-slate-100">
                <span className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">
                  Turnaround / Processing SLA
                </span>
                <span className="font-bold text-slate-900">
                  {category.processingDaysMin} to {category.processingDaysMax} Business Days
                </span>
              </div>

              <div className="p-3.5 bg-slate-50 rounded-lg border border-slate-100">
                <span className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">
                  Extendability
                </span>
                <span className="font-bold text-slate-900">
                  {category.isExtendable ? 'Permitted under FRRO medical oversight' : 'Non-Extendable'}
                </span>
              </div>

              <div className="p-3.5 bg-slate-50 rounded-lg border border-slate-100">
                <span className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">
                  Convertibility
                </span>
                <span className="font-bold text-slate-900">
                  {category.isConvertible ? 'Convertible' : 'Non-Convertible to any other category'}
                </span>
              </div>
            </div>
          </div>

          {/* Mandatory Documents Checklist */}
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
            <h2 className="text-lg font-bold text-slate-900 mb-3 flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-teal-600" />
              Document Checklist & Upload Specifications
            </h2>
            <ul className="space-y-3">
              {category.requiredDocuments.map((doc, idx) => (
                <li key={idx} className="flex items-start gap-3 p-3 rounded-lg bg-teal-50/50 border border-teal-100 text-sm text-slate-800">
                  <span className="text-teal-700 font-bold mt-0.5">✓</span>
                  <span>{doc}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Port Limits & Exit Rules */}
          {category.entryPointLimits && (
            <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
              <h2 className="text-lg font-bold text-slate-900 mb-3 flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-teal-600" />
                Designated Ports of Arrival & Departure
              </h2>
              <p className="text-sm text-slate-700 mb-3">
                {category.entryPointLimits}
              </p>
              <Link
                to="/requirements#entry-points"
                className="text-xs font-semibold text-teal-700 hover:text-teal-900 underline"
              >
                Browse all 37 designated airports and 5 seaports →
              </Link>
            </div>
          )}
        </div>

        {/* Right Col: Action & Fee Summary Card */}
        <div className="space-y-6">
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm sticky top-24">
            <span className="inline-block px-2.5 py-0.5 text-xs font-semibold rounded bg-teal-100 text-teal-800 uppercase tracking-wide mb-3">
              {category.code}
            </span>
            <h3 className="text-2xl font-bold text-slate-900 mb-2">
              {category.name}
            </h3>
            <p className="text-xs text-slate-500 mb-4">
              Official Indian e-Visa Sub-Category
            </p>

            <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 mb-6">
              <span className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">
                Official Fee Band
              </span>
              <div className="text-xl font-bold text-teal-800">
                {category.feeBandSummary}
              </div>
              <p className="text-[11px] text-slate-500 mt-1">
                Reciprocal rates apply based on citizenship. Non-refundable once processed.
              </p>
            </div>

            <div className="space-y-3">
              <button
                type="button"
                onClick={() => navigate(`/?purpose=${category.purposeCode}#checker`)}
                className="w-full py-3 bg-teal-700 hover:bg-teal-800 text-white rounded-lg font-semibold text-sm shadow-sm transition-colors text-center cursor-pointer"
              >
                Check My Country’s Eligibility
              </button>

              <Link
                to="/requirements"
                className="w-full block py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-lg font-semibold text-sm transition-colors text-center"
              >
                Verify Photo & Passport Specs
              </Link>
            </div>

            <div className="mt-6 pt-5 border-t border-slate-200">
              <a
                href={category.officialSourceUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="text-xs text-slate-600 hover:text-teal-700 flex items-center justify-between group"
              >
                <span>Official Government Gazette Page</span>
                <span className="text-slate-400 group-hover:text-teal-700">↗</span>
              </a>
            </div>
          </div>
        </div>
      </div>
    </PageTemplate>
  );
};
