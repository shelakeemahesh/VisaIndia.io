import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { PageTemplate } from '../components/PageTemplate';
import { fetchVisaCategories, VisaCategoryDetailDto } from '../api/content';

export const VisaCategoriesPage: React.FC = () => {
  const navigate = useNavigate();
  const [categories, setCategories] = useState<VisaCategoryDetailDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [selectedPurpose, setSelectedPurpose] = useState<string>('ALL');
  const [selectedDuration, setSelectedDuration] = useState<string>('ALL');
  const [selectedNationality, setSelectedNationality] = useState<string>('JPN');

  // Comparison drawer state
  const [compareSlugs, setCompareSlugs] = useState<string[]>([]);
  const [showComparisonModal, setShowComparisonModal] = useState(false);

  useEffect(() => {
    fetchVisaCategories()
      .then((data) => {
        setCategories(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error('Error fetching categories:', err);
        setError('Failed to load live visa categories from official data feed.');
        setLoading(false);
      });
  }, []);

  const toggleCompare = (slug: string) => {
    if (compareSlugs.includes(slug)) {
      setCompareSlugs(compareSlugs.filter((s) => s !== slug));
    } else {
      if (compareSlugs.length >= 3) {
        alert('You can compare up to 3 visa categories simultaneously.');
        return;
      }
      setCompareSlugs([...compareSlugs, slug]);
    }
  };

  const filteredCategories = categories.filter((cat) => {
    if (selectedPurpose !== 'ALL' && cat.purposeCode !== selectedPurpose) return false;
    if (selectedDuration === 'SHORT' && cat.validityDays > 60) return false;
    if (selectedDuration === 'LONG' && cat.validityDays <= 60) return false;
    return true;
  });

  const comparedItems = categories.filter((c) => compareSlugs.includes(c.slug));

  // Nationality route lookup for "Other Routes"
  const getRouteAvailability = (route: string, nat: string) => {
    if (route === 'voa') {
      return ['JPN', 'KOR', 'ARE'].includes(nat)
        ? { status: 'Available', color: 'bg-emerald-100 text-emerald-800 border-emerald-300', text: 'Eligible at 6 designated international airports (DEL, BOM, BLR, MAA, CCU, HYD).' }
        : { status: 'Not available', color: 'bg-slate-100 text-slate-700 border-slate-300', text: 'Visa on Arrival is exclusively reserved for citizens of Japan, South Korea, and UAE.' };
    }
    if (route === 'visa-free') {
      return ['NPL', 'BTN'].includes(nat)
        ? { status: 'Available', color: 'bg-emerald-100 text-emerald-800 border-emerald-300', text: 'Complete freedom of movement under bilateral treaties. No visa required.' }
        : { status: 'Not available', color: 'bg-slate-100 text-slate-700 border-slate-300', text: 'Visa-free entry is limited to citizens of Nepal and Bhutan.' };
    }
    if (route === 'regular') {
      return { status: 'Available', color: 'bg-blue-100 text-blue-800 border-blue-300', text: 'Available to all passport holders via Indian Embassy or Consulate sticker processing.' };
    }
    if (route === 'oci') {
      return { status: 'Special Provision', color: 'bg-purple-100 text-purple-800 border-purple-300', text: 'Overseas Citizenship of India cardholders enjoy lifelong visa-free entry with multi-purpose rights.' };
    }
    return { status: 'Not listed in official sources', color: 'bg-amber-100 text-amber-800 border-amber-300', text: 'Consult your nearest Indian diplomatic mission.' };
  };

  return (
    <PageTemplate
      title="Official Visa Categories & Sub-Classifications"
      intro="Explore all verified electronic and consular visa sub-categories recognized by the Ministry of Home Affairs and Bureau of Immigration. Filter by purpose, duration, or compare parameters side by side."
      breadcrumbs={[{ label: 'Visa Categories' }]}
      sourceUrl="https://indianvisaonline.gov.in/evisa/tvoa.html"
      lastVerifiedDate="October 2026"
    >
      {/* Filtering & Comparison Bar */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5 mb-8">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div className="flex flex-wrap items-center gap-3">
            <div>
              <label htmlFor="purpose-filter" className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">
                Purpose
              </label>
              <select
                id="purpose-filter"
                value={selectedPurpose}
                onChange={(e) => setSelectedPurpose(e.target.value)}
                className="bg-slate-50 border border-slate-300 text-slate-850 text-sm rounded-lg focus:ring-teal-500 focus:border-teal-500 block p-2"
              >
                <option value="ALL">All Purposes</option>
                <option value="TOURISM">Tourism & Leisure</option>
                <option value="BUSINESS">Business & Trade</option>
                <option value="MEDICAL">Medical Treatment & Ayush</option>
                <option value="CONFERENCE">Conference & Seminars</option>
              </select>
            </div>

            <div>
              <label htmlFor="duration-filter" className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">
                Validity
              </label>
              <select
                id="duration-filter"
                value={selectedDuration}
                onChange={(e) => setSelectedDuration(e.target.value)}
                className="bg-slate-50 border border-slate-300 text-slate-850 text-sm rounded-lg focus:ring-teal-500 focus:border-teal-500 block p-2"
              >
                <option value="ALL">All Durations</option>
                <option value="SHORT">Short-Stay (≤ 60 Days)</option>
                <option value="LONG">Long-Term (1 to 5 Years)</option>
              </select>
            </div>

            <div>
              <label htmlFor="nationality-select" className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">
                Your Nationality
              </label>
              <select
                id="nationality-select"
                value={selectedNationality}
                onChange={(e) => setSelectedNationality(e.target.value)}
                className="bg-slate-50 border border-slate-300 text-slate-850 text-sm rounded-lg focus:ring-teal-500 focus:border-teal-500 block p-2 font-medium"
              >
                <option value="JPN">🇯🇵 Japan (JPN)</option>
                <option value="USA">🇺🇸 United States (USA)</option>
                <option value="GBR">🇬🇧 United Kingdom (GBR)</option>
                <option value="DEU">🇩🇪 Germany (DEU)</option>
                <option value="FRA">🇫🇷 France (FRA)</option>
                <option value="KOR">🇰🇷 South Korea (KOR)</option>
                <option value="ARE">🇦🇪 United Arab Emirates (ARE)</option>
                <option value="NPL">🇳🇵 Nepal (NPL)</option>
                <option value="AUS">🇦🇺 Australia (AUS)</option>
                <option value="CAN">🇨🇦 Canada (CAN)</option>
              </select>
            </div>
          </div>

          {/* Comparison CTA */}
          <div className="flex items-center gap-3">
            <span className="text-xs text-slate-500 font-medium">
              Compare: <strong className="text-teal-700">{compareSlugs.length}/3</strong> selected
            </span>
            <button
              type="button"
              disabled={compareSlugs.length < 2}
              onClick={() => setShowComparisonModal(true)}
              className={`px-4 py-2 text-sm font-semibold rounded-lg transition-colors ${
                compareSlugs.length >= 2
                  ? 'bg-teal-700 text-white hover:bg-teal-800 shadow-sm cursor-pointer'
                  : 'bg-slate-100 text-slate-400 cursor-not-allowed border border-slate-200'
              }`}
            >
              Compare Side-by-Side
            </button>
            {compareSlugs.length > 0 && (
              <button
                type="button"
                onClick={() => setCompareSlugs([])}
                className="text-xs text-slate-500 hover:text-red-600 underline"
              >
                Clear
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Loading & Error States */}
      {loading && (
        <div className="py-16 text-center">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-teal-600 border-t-transparent mb-3" />
          <p className="text-slate-600 font-medium">Loading verified official visa categories...</p>
        </div>
      )}

      {error && (
        <div className="p-4 mb-6 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
          {error}
        </div>
      )}

      {/* Cards Grid */}
      {!loading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-12">
          {filteredCategories.map((cat) => {
            const isSelected = compareSlugs.includes(cat.slug);
            return (
              <div
                key={cat.slug}
                className={`bg-white rounded-xl border transition-all flex flex-col justify-between overflow-hidden ${
                  isSelected ? 'border-teal-500 ring-2 ring-teal-200 shadow-md' : 'border-slate-200 hover:border-slate-300 hover:shadow-sm'
                }`}
              >
                <div className="p-5">
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <span className="inline-block px-2.5 py-0.5 text-xs font-semibold rounded bg-teal-50 text-teal-800 border border-teal-200 uppercase tracking-wide">
                      {cat.purposeCode}
                    </span>
                    <button
                      type="button"
                      onClick={() => toggleCompare(cat.slug)}
                      className={`text-xs px-2 py-1 rounded border font-medium transition-colors ${
                        isSelected
                          ? 'bg-teal-700 text-white border-teal-700'
                          : 'bg-slate-50 text-slate-600 border-slate-300 hover:bg-slate-100'
                      }`}
                    >
                      {isSelected ? '✓ Selected' : '+ Compare'}
                    </button>
                  </div>

                  <h2 className="text-lg font-bold text-slate-900 mb-2 leading-snug">
                    <Link to={`/visa-categories/${cat.slug}`} className="hover:text-teal-700 transition-colors">
                      {cat.name}
                    </Link>
                  </h2>

                  <p className="text-xs text-slate-600 mb-4 line-clamp-2">
                    {cat.description}
                  </p>

                  <div className="space-y-2 py-3 border-y border-slate-100 text-xs">
                    <div className="flex justify-between">
                      <span className="text-slate-500">Validity:</span>
                      <span className="font-semibold text-slate-800">{cat.validityText}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">Entries:</span>
                      <span className="font-semibold text-slate-800">{cat.entries} Entry</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">Max Stay:</span>
                      <span className="font-semibold text-slate-800 text-right">{cat.maxStayText}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">Fee Band:</span>
                      <span className="font-semibold text-teal-800">{cat.feeBandSummary}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">Processing Time:</span>
                      <span className="font-semibold text-slate-800">
                        {cat.processingDaysMin}–{cat.processingDaysMax} Business Days
                      </span>
                    </div>
                  </div>

                  <div className="mt-3">
                    <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider mb-1">
                      Key Documents:
                    </p>
                    <ul className="text-xs text-slate-600 list-disc list-inside space-y-1">
                      {cat.requiredDocuments.slice(0, 2).map((doc, idx) => (
                        <li key={idx} className="truncate" title={doc}>
                          {doc}
                        </li>
                      ))}
                      {cat.requiredDocuments.length > 2 && (
                        <li className="text-slate-500 font-medium">
                          +{cat.requiredDocuments.length - 2} more requirements
                        </li>
                      )}
                    </ul>
                  </div>
                </div>

                <div className="bg-slate-50 p-3.5 border-t border-slate-100 flex items-center justify-between gap-1.5 flex-wrap">
                  <button
                    type="button"
                    onClick={() => navigate(`/?nationality=${selectedNationality}&purpose=${cat.purposeCode}#checker`)}
                    className="text-xs font-semibold text-slate-600 hover:text-teal-700 py-1.5 px-2.5 rounded hover:bg-slate-100 transition-colors"
                  >
                    Eligibility
                  </button>
                  <div className="flex items-center gap-1.5">
                    <Link
                      to={`/visa-categories/${cat.slug}`}
                      className="text-xs font-semibold text-teal-800 bg-white hover:bg-teal-50 py-1.5 px-2.5 rounded border border-teal-200 transition-colors"
                    >
                      Specs
                    </Link>
                    <button
                      type="button"
                      onClick={() => navigate(`/apply?purpose=${cat.purposeCode}&category=${encodeURIComponent(cat.name)}&nationality=${selectedNationality}`)}
                      className="text-xs font-bold bg-teal-700 text-white hover:bg-teal-800 py-1.5 px-3 rounded shadow-sm transition-colors cursor-pointer"
                    >
                      Apply →
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Comparison Modal / Side-by-Side View */}
      {showComparisonModal && comparedItems.length > 0 && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-4xl w-full p-6 shadow-2xl border border-slate-200 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-4 border-b border-slate-200 mb-6">
              <div>
                <h2 className="text-xl font-bold text-slate-900">
                  Side-by-Side Category Comparison
                </h2>
                <p className="text-xs text-slate-500">
                  Review specifications and limitations side by side
                </p>
              </div>
              <button
                type="button"
                onClick={() => setShowComparisonModal(false)}
                className="text-slate-400 hover:text-slate-700 text-2xl font-bold p-1 leading-none"
              >
                &times;
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {comparedItems.map((item) => (
                <div key={item.slug} className="bg-slate-50 rounded-xl p-5 border border-slate-200">
                  <h3 className="font-bold text-base text-slate-900 mb-2">
                    {item.name}
                  </h3>
                  <span className="inline-block px-2 py-0.5 text-xs font-semibold rounded bg-teal-100 text-teal-800 mb-4">
                    {item.purposeCode}
                  </span>

                  <dl className="space-y-3 text-xs">
                    <div>
                      <dt className="text-slate-500 font-medium">Validity</dt>
                      <dd className="font-semibold text-slate-900 mt-0.5">{item.validityText}</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500 font-medium">Entries</dt>
                      <dd className="font-semibold text-slate-900 mt-0.5">{item.entries}</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500 font-medium">Max Continuous Stay</dt>
                      <dd className="font-semibold text-slate-900 mt-0.5">{item.maxStayText}</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500 font-medium">Official Fee Range</dt>
                      <dd className="font-semibold text-teal-800 mt-0.5">{item.feeBandSummary}</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500 font-medium">Processing SLA</dt>
                      <dd className="font-semibold text-slate-900 mt-0.5">{item.processingDaysMin}–{item.processingDaysMax} Business Days</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500 font-medium">Extendable?</dt>
                      <dd className="font-semibold text-slate-900 mt-0.5">{item.isExtendable ? 'Yes (via FRRO on medical grounds)' : 'Strictly Non-Extendable'}</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500 font-medium">Convertible?</dt>
                      <dd className="font-semibold text-slate-900 mt-0.5">{item.isConvertible ? 'Yes' : 'Strictly Non-Convertible'}</dd>
                    </div>
                  </dl>

                  <div className="mt-5 pt-4 border-t border-slate-200">
                    <Link
                      to={`/visa-categories/${item.slug}`}
                      className="block text-center text-xs font-semibold bg-teal-700 text-white hover:bg-teal-800 py-2 rounded transition-colors"
                    >
                      View Full Category Spec →
                    </Link>
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-6 pt-4 border-t border-slate-200 text-right">
              <button
                type="button"
                onClick={() => setShowComparisonModal(false)}
                className="px-5 py-2 text-sm font-semibold bg-slate-200 hover:bg-slate-300 text-slate-800 rounded-lg transition-colors"
              >
                Close Comparison
              </button>
            </div>
          </div>
        </div>
      )}

      {/* SECTION: Other Immigration Routes */}
      <section className="mt-12 pt-8 border-t border-slate-200">
        <div className="mb-6">
          <h2 className="text-xl font-bold text-slate-900">
            Other Legal Immigration & Travel Routes
          </h2>
          <p className="text-sm text-slate-600 mt-1">
            Depending on your passport and heritage, electronic visas may not be your only or most convenient entry mechanism.
            Showing status for nationality: <strong className="text-teal-800 font-semibold">{selectedNationality}</strong>.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Visa on Arrival */}
          <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-bold text-slate-900 text-base">
                Visa on Arrival (VoA)
              </h3>
              {(() => {
                const r = getRouteAvailability('voa', selectedNationality);
                return (
                  <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full border ${r.color}`}>
                    {r.status}
                  </span>
                );
              })()}
            </div>
            <p className="text-xs text-slate-600 mb-3">
              Special bilateral facility permitting eligible travelers to land without prior paperwork and pay ₹2,000 / $25 USD at immigration counters.
            </p>
            <div className="p-3 bg-slate-50 rounded-lg text-xs text-slate-700 mb-3 border border-slate-100">
              {getRouteAvailability('voa', selectedNationality).text}
            </div>
            <a
              href="https://indianvisaonline.gov.in/visa/visa-on-arrival.html"
              target="_blank"
              rel="noopener noreferrer"
              className="text-xs font-semibold text-teal-700 hover:text-teal-900 flex items-center gap-1"
            >
              Official VoA Gazette Notification ↗
            </a>
          </div>

          {/* Visa-Free Arrangements */}
          <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-bold text-slate-900 text-base">
                Visa-Free Freedom of Movement
              </h3>
              {(() => {
                const r = getRouteAvailability('visa-free', selectedNationality);
                return (
                  <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full border ${r.color}`}>
                    {r.status}
                  </span>
                );
              })()}
            </div>
            <p className="text-xs text-slate-600 mb-3">
              Reciprocal open border treaties under the 1950 Indo-Nepal and Indo-Bhutan Friendships allowing indefinite stay and employment.
            </p>
            <div className="p-3 bg-slate-50 rounded-lg text-xs text-slate-700 mb-3 border border-slate-100">
              {getRouteAvailability('visa-free', selectedNationality).text}
            </div>
            <a
              href="https://www.mea.gov.in"
              target="_blank"
              rel="noopener noreferrer"
              className="text-xs font-semibold text-teal-700 hover:text-teal-900 flex items-center gap-1"
            >
              Ministry of External Affairs Treaty Archives ↗
            </a>
          </div>

          {/* Regular Embassy Visa */}
          <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-bold text-slate-900 text-base">
                Regular / Paper Sticker Visa
              </h3>
              {(() => {
                const r = getRouteAvailability('regular', selectedNationality);
                return (
                  <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full border ${r.color}`}>
                    {r.status}
                  </span>
                );
              })()}
            </div>
            <p className="text-xs text-slate-600 mb-3">
              Required for stays over 180 days, employment, higher studies, diplomatic delegations, or journalists.
            </p>
            <div className="p-3 bg-slate-50 rounded-lg text-xs text-slate-700 mb-3 border border-slate-100">
              {getRouteAvailability('regular', selectedNationality).text}
            </div>
            <a
              href="https://indianvisaonline.gov.in/visa/index.html"
              target="_blank"
              rel="noopener noreferrer"
              className="text-xs font-semibold text-teal-700 hover:text-teal-900 flex items-center gap-1"
            >
              Regular Visa Application Portal (IVFRT) ↗
            </a>
          </div>

          {/* Overseas Citizen of India (OCI) */}
          <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-bold text-slate-900 text-base">
                Overseas Citizen of India (OCI)
              </h3>
              {(() => {
                const r = getRouteAvailability('oci', selectedNationality);
                return (
                  <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full border ${r.color}`}>
                    {r.status}
                  </span>
                );
              })()}
            </div>
            <p className="text-xs text-slate-600 mb-3">
              Lifelong multipurpose visa granting parity with Non-Resident Indians (NRIs) in economic, financial, and educational fields.
            </p>
            <div className="p-3 bg-slate-50 rounded-lg text-xs text-slate-700 mb-3 border border-slate-100">
              {getRouteAvailability('oci', selectedNationality).text}
            </div>
            <a
              href="https://ociservices.gov.in/"
              target="_blank"
              rel="noopener noreferrer"
              className="text-xs font-semibold text-teal-700 hover:text-teal-900 flex items-center gap-1"
            >
              Official OCI Services Portal (MHA) ↗
            </a>
          </div>
        </div>
      </section>
    </PageTemplate>
  );
};
