import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { PageTemplate } from '../components/PageTemplate';
import { trackApplication, resyncPayment, TrackApplicationResponse } from '../api/content';
import { getEtaPdfDownloadUrl, getQrPassImageUrl } from '../api/trips';

export const ExistingApplicationsPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const [referenceId, setReferenceId] = useState(searchParams.get('ref') || '');
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [otpRequired, setOtpRequired] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<TrackApplicationResponse | null>(null);

  // Resync payment state
  const [resyncing, setResyncing] = useState(false);
  const [resyncFeedback, setResyncFeedback] = useState<string | null>(null);

  useEffect(() => {
    const ref = searchParams.get('ref');
    if (ref) {
      setReferenceId(ref);
    }
  }, [searchParams]);

  // Demo presets for easy 1-click evaluation
  const demoPresets = [
    { label: 'Approved (ETA Granted)', ref: 'VS-2026-4011', email: 'sarah.jenkins@example.com', badge: 'bg-emerald-100 text-emerald-800' },
    { label: 'Under Review', ref: 'VS-2026-5509', email: 'akiko.tanaka@example.com', badge: 'bg-blue-100 text-blue-800' },
    { label: 'Payment Pending', ref: 'VS-2026-6134', email: 'david.kim@example.com', badge: 'bg-amber-100 text-amber-800' },
    { label: 'Payment Failed', ref: 'VS-2026-2048', email: 'claudia.muller@example.com', badge: 'bg-rose-100 text-rose-800' },
    { label: 'Draft Progress', ref: 'VS-2026-8821', email: 'traveler.draft@example.com', badge: 'bg-slate-100 text-slate-800' },
    { label: 'Rejected', ref: 'VS-2026-3199', email: 'marcus.dupont@example.com', badge: 'bg-red-100 text-red-800' },
  ];

  const loadPreset = (presetRef: string, presetEmail: string) => {
    setReferenceId(presetRef);
    setEmail(presetEmail);
    setOtp('');
    setOtpRequired(false);
    setError(null);
    setResyncFeedback(null);
  };

  const handleTrackSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!referenceId.trim() || !email.trim()) {
      setError('Please provide both your Application Reference ID and email address.');
      return;
    }

    setLoading(true);
    setError(null);
    setResyncFeedback(null);

    try {
      const data = await trackApplication(referenceId.trim(), email.trim(), otp ? otp.trim() : undefined);
      if (data.status === 'OTP_REQUIRED') {
        setOtpRequired(true);
        setOtp('');
      } else {
        setResult(data);
        setOtpRequired(false);
      }
    } catch (err: any) {
      console.error('Tracking lookup error:', err);
      const msg = err.response?.data?.error || 'Unable to locate application with the provided credentials.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleForceResync = async () => {
    if (!result) return;
    setResyncing(true);
    setResyncFeedback(null);

    try {
      const res = await resyncPayment(result.referenceId);
      setResyncFeedback(res.message);
      // Re-fetch updated application details
      const updated = await trackApplication(result.referenceId, email, otp ? otp.trim() : undefined);
      if (updated.timeline) {
        setResult(updated);
      }
    } catch (err: any) {
      console.error('Payment reconciliation error:', err);
      setResyncFeedback('Reconciliation request failed. Please contact the 24x7 help desk.');
    } finally {
      setResyncing(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return <span className="px-3 py-1 bg-emerald-100 text-emerald-800 border border-emerald-300 font-bold rounded-full text-xs">APPROVED — ETA GRANTED</span>;
      case 'UNDER_REVIEW':
        return <span className="px-3 py-1 bg-blue-100 text-blue-800 border border-blue-300 font-bold rounded-full text-xs">UNDER IMMIGRATION REVIEW</span>;
      case 'PAYMENT_PENDING':
        return <span className="px-3 py-1 bg-amber-100 text-amber-800 border border-amber-300 font-bold rounded-full text-xs">PAYMENT PENDING / PROCESSING</span>;
      case 'PAYMENT_FAILED':
        return <span className="px-3 py-1 bg-rose-100 text-rose-800 border border-rose-300 font-bold rounded-full text-xs">PAYMENT FAILED</span>;
      case 'REJECTED':
        return <span className="px-3 py-1 bg-red-100 text-red-800 border border-red-300 font-bold rounded-full text-xs">APPLICATION REJECTED</span>;
      case 'SUBMITTED':
        return <span className="px-3 py-1 bg-purple-100 text-purple-800 border border-purple-300 font-bold rounded-full text-xs">SUBMITTED</span>;
      default:
        return <span className="px-3 py-1 bg-slate-100 text-slate-800 border border-slate-300 font-bold rounded-full text-xs">DRAFT IN PROGRESS</span>;
    }
  };

  return (
    <PageTemplate
      title="Existing Application Tracker & Progress Dashboard"
      intro="Resume an autosaved draft, track live consular decisions, download official ETA passes, or reconcile delayed payment gateway callbacks."
      breadcrumbs={[{ label: 'Existing Applications' }]}
      sourceUrl="https://indianvisaonline.gov.in/evisa/tvoa.html"
      lastVerifiedDate="October 2026"
    >
      {/* DEMO PRESETS BAR */}
      <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 mb-8">
        <span className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
          Demo Application Tester (1-Click Presets Across Every State):
        </span>
        <div className="flex flex-wrap gap-2">
          {demoPresets.map((p) => (
            <button
              key={p.ref}
              type="button"
              onClick={() => loadPreset(p.ref, p.email)}
              className="text-xs px-2.5 py-1.5 rounded-lg border border-slate-300 bg-white hover:bg-slate-100 font-medium transition-colors flex items-center gap-1.5 shadow-2xs cursor-pointer"
            >
              <span className={`w-2 h-2 rounded-full ${p.badge.split(' ')[0]}`} />
              <span>{p.label}</span>
              <span className="font-mono text-slate-400">({p.ref})</span>
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: Lookup Form */}
        <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm h-fit">
          <h2 className="text-lg font-bold text-slate-900 mb-1 flex items-center gap-2">
            <span>🔍</span>
            Find Application
          </h2>
          <p className="text-xs text-slate-500 mb-6">
            Enter your Reference ID (e.g., VS-2026-4011) and the applicant's email.
          </p>

          <form onSubmit={handleTrackSubmit} className="space-y-4">
            <div>
              <label htmlFor="ref-id-input" className="block text-xs font-semibold text-slate-600 mb-1">
                Application Reference ID
              </label>
              <input
                id="ref-id-input"
                type="text"
                placeholder="e.g. VS-2026-4011"
                value={referenceId}
                onChange={(e) => setReferenceId(e.target.value)}
                required
                className="w-full text-sm font-mono p-2.5 bg-slate-50 border border-slate-300 rounded-lg focus:ring-teal-500 focus:border-teal-500"
              />
            </div>

            <div>
              <label htmlFor="email-input" className="block text-xs font-semibold text-slate-600 mb-1">
                Applicant Email Address
              </label>
              <input
                id="email-input"
                type="email"
                placeholder="applicant@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="w-full text-sm p-2.5 bg-slate-50 border border-slate-300 rounded-lg focus:ring-teal-500 focus:border-teal-500"
              />
            </div>

            {otpRequired && (
              <div className="p-3 bg-teal-50 border border-teal-200 rounded-lg animate-fadeIn">
                <label htmlFor="otp-input" className="block text-xs font-bold text-teal-900 mb-1">
                  6-Digit Email Verification Code
                </label>
                <input
                  id="otp-input"
                  type="text"
                  maxLength={6}
                  placeholder="Enter 6-digit code"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  className="w-full text-center tracking-widest font-mono text-base font-bold p-2 bg-white border border-teal-300 rounded-md focus:ring-teal-500"
                />
                <p className="text-[11px] text-teal-700 mt-1">
                  Security verification code dispatched to your registered email address.
                </p>
              </div>
            )}

            {error && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 bg-teal-700 hover:bg-teal-800 text-white rounded-lg font-bold text-sm shadow-sm transition-colors cursor-pointer flex items-center justify-center gap-2"
            >
              {loading ? (
                <span>Checking Database...</span>
              ) : otpRequired ? (
                <span>Verify Code & View Status →</span>
              ) : (
                <span>Lookup Status →</span>
              )}
            </button>
          </form>
        </div>

        {/* Right Column: Live Status & Timeline */}
        <div className="lg:col-span-2">
          {result ? (
            <div className="space-y-6">
              {/* Application Header Card */}
              <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
                <div className="flex flex-wrap items-start justify-between gap-4 pb-4 border-b border-slate-100">
                  <div>
                    <span className="text-xs font-mono text-slate-400">
                      REFERENCE NO.
                    </span>
                    <h2 className="text-xl font-bold font-mono text-slate-900">
                      {result.referenceId}
                    </h2>
                  </div>
                  <div>{getStatusBadge(result.status)}</div>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-4 text-xs">
                  <div>
                    <span className="text-slate-400 block mb-0.5">Applicant</span>
                    <span className="font-bold text-slate-800">{result.applicantName}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block mb-0.5">Category</span>
                    <span className="font-bold text-slate-800">{result.categoryName}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block mb-0.5">Nationality</span>
                    <span className="font-bold text-slate-800">{result.nationality}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block mb-0.5">Arrival Port</span>
                    <span className="font-bold text-slate-800">{result.portOfArrival || 'Designated Port'}</span>
                  </div>
                </div>

                {/* Force Payment Re-sync Button for pending / failed states */}
                {(result.status === 'PAYMENT_PENDING' || result.status === 'PAYMENT_FAILED' || result.status === 'SUBMITTED') && (
                  <div className="mt-5 p-4 bg-amber-50 border border-amber-200 rounded-xl flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                      <p className="text-xs font-bold text-amber-900">
                        Payment debited from your card but not showing as paid?
                      </p>
                      <p className="text-[11px] text-amber-800">
                        Force an immediate cryptographic reconciliation against the Razorpay payment engine.
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={handleForceResync}
                      disabled={resyncing}
                      className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-lg text-xs font-bold whitespace-nowrap transition-colors shadow-2xs cursor-pointer"
                    >
                      {resyncing ? 'Reconciling...' : 'Re-sync Payment Now ⟳'}
                    </button>
                  </div>
                )}

                {resyncFeedback && (
                  <div className="mt-3 p-3 bg-emerald-50 border border-emerald-200 rounded-lg text-xs text-emerald-800 font-medium">
                    ✓ {resyncFeedback}
                  </div>
                )}
              </div>

              {/* Status Timeline */}
              <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
                <h3 className="text-base font-bold text-slate-900 mb-6">
                  Application Lifecycle Timeline
                </h3>

                <div className="relative pl-6 space-y-6 before:content-[''] before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200">
                  {result.timeline.map((step, idx) => {
                    const isCompleted = step.state === 'COMPLETED';
                    const isCurrent = step.state === 'CURRENT';
                    const isFailed = step.state === 'FAILED';

                    return (
                      <div key={idx} className="relative">
                        {/* Dot */}
                        <div
                          className={`absolute -left-[29px] top-1 w-4 h-4 rounded-full border-2 ${
                            isCompleted
                              ? 'bg-teal-600 border-teal-600 text-white'
                              : isCurrent
                              ? 'bg-white border-teal-600 ring-4 ring-teal-100'
                              : isFailed
                              ? 'bg-red-600 border-red-600'
                              : 'bg-white border-slate-300'
                          }`}
                        />
                        <div>
                          <div className="flex items-center justify-between">
                            <h4
                              className={`text-xs font-bold ${
                                isCompleted
                                  ? 'text-teal-900'
                                  : isCurrent
                                  ? 'text-teal-700'
                                  : isFailed
                                  ? 'text-red-700'
                                  : 'text-slate-400'
                              }`}
                            >
                              {step.label}
                            </h4>
                            {step.timestamp && (
                              <span className="text-[10px] text-slate-400">
                                {new Date(step.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-slate-500 mt-0.5">
                            {step.description}
                          </p>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* ETA Download / QR Pass when Approved */}
              {result.status === 'APPROVED' && (
                <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-6 shadow-sm">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                    <div>
                      <span className="text-xs font-bold text-emerald-800 uppercase tracking-wider block mb-1">
                        Electronic Travel Authorization Granted
                      </span>
                      <h3 className="text-lg font-bold text-emerald-950">
                        Official ETA Pass Ready for Download
                      </h3>
                      <p className="text-xs text-emerald-800 mt-1">
                        Print your ETA and present it alongside your original passport at airline boarding and Indian immigration.
                      </p>
                    </div>

                    <div className="flex gap-2">
                      <a
                        href={getEtaPdfDownloadUrl(result.referenceId)}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="px-4 py-2.5 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-lg shadow-sm transition-colors cursor-pointer inline-flex items-center gap-1.5"
                      >
                        <span>Download Official ETA (PDF)</span>
                        <span>↗</span>
                      </a>
                    </div>
                  </div>

                  {result.qrData && (
                    <div className="mt-5 p-4 bg-white rounded-xl border border-emerald-200 flex items-center gap-4">
                      <img
                        src={getQrPassImageUrl(result.referenceId)}
                        alt="Biometric e-Gate Token"
                        className="w-16 h-16 bg-white border border-slate-300 rounded p-0.5 object-contain"
                        onError={(e) => {
                          // Fallback to text box if server QR not generated yet
                          (e.target as HTMLElement).style.display = 'none';
                        }}
                      />
                      <div className="text-xs">
                        <span className="font-semibold text-slate-800 block">Biometric e-Gate Token:</span>
                        <code className="text-[11px] text-teal-800 font-mono break-all">{result.qrData}</code>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          ) : (
            <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-slate-400">
              <div className="text-4xl mb-3">📋</div>
              <h3 className="text-base font-bold text-slate-700 mb-1">
                No Application Selected
              </h3>
              <p className="text-xs max-w-sm mx-auto mb-4">
                Select one of the demo presets above or enter your Reference ID to inspect status, timeline, and ETA passes.
              </p>
              <Link
                to="/start-application"
                className="inline-block px-5 py-2.5 bg-teal-700 hover:bg-teal-800 text-white text-xs font-bold rounded-xl shadow-sm transition-colors cursor-pointer"
              >
                Start New 3-in-1 Application →
              </Link>
            </div>
          )}
        </div>
      </div>
    </PageTemplate>
  );
};
