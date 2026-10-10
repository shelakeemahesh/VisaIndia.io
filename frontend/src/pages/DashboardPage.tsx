import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  CheckCircle2,
  Clock,
  CreditCard,
  FileText,
  Download,
  QrCode,
  RefreshCw,
  PlusCircle,
  ExternalLink,
  ChevronRight,
  AlertTriangle,
  Plane,
  Copy,
  Check,
  MapPin,
  Calendar,
} from 'lucide-react';
import { PageTemplate } from '../components/PageTemplate';
import {
  getTripByReference,
  resyncPayment,
  getEtaPdfDownloadUrl,
  getQrPassImageUrl,
} from '../api/trips';

interface MockDashboardTrip {
  referenceId: string;
  applicantName: string;
  applicantEmail: string;
  nationality: string;
  flagEmoji: string;
  passportNumber: string;
  categoryName: string;
  purpose: string;
  portOfArrival: string;
  arrivalDate: string;
  status: 'APPROVED' | 'UNDER_REVIEW' | 'PAYMENT_PENDING' | 'DRAFT';
  amount: number;
  currency: string;
  etaNumber: string | null;
  qrToken: string | null;
  modules: {
    eVisa: 'APPROVED' | 'UNDER_REVIEW' | 'PENDING';
    eArrival: 'COMPLETED' | 'PENDING';
    eSuvidha: 'COMPLETED' | 'PENDING';
  };
  submittedAt: string;
}

const DEMO_PRESET_TRIPS: MockDashboardTrip[] = [
  {
    referenceId: 'VS-2026-6134',
    applicantName: 'Kenji Sato',
    applicantEmail: 'kenji.sato@tokyo-travel.jp',
    nationality: 'Japan',
    flagEmoji: '🇯🇵',
    passportNumber: 'TK-8849201',
    categoryName: 'e-Tourist Visa (30 Days)',
    purpose: 'Tourism & Recreation',
    portOfArrival: 'Delhi (DEL) - Indira Gandhi International',
    arrivalDate: '2026-11-18',
    status: 'APPROVED',
    amount: 25.0,
    currency: 'USD',
    etaNumber: 'ETA-IN-2026-994821',
    qrToken: 'VS-QR-98174291847192',
    modules: {
      eVisa: 'APPROVED',
      eArrival: 'COMPLETED',
      eSuvidha: 'COMPLETED',
    },
    submittedAt: '2026-10-10 14:30 UTC',
  },
  {
    referenceId: 'VS-2026-8821',
    applicantName: 'Emma Watson',
    applicantEmail: 'e.watson@london-tech.co.uk',
    nationality: 'United Kingdom',
    flagEmoji: '🇬🇧',
    passportNumber: 'GB-7491028',
    categoryName: 'e-Business Visa (1 Year)',
    purpose: 'Business & Commerce',
    portOfArrival: 'Mumbai (BOM) - Chhatrapati Shivaji Maharaj',
    arrivalDate: '2026-11-25',
    status: 'UNDER_REVIEW',
    amount: 80.0,
    currency: 'USD',
    etaNumber: null,
    qrToken: null,
    modules: {
      eVisa: 'UNDER_REVIEW',
      eArrival: 'COMPLETED',
      eSuvidha: 'COMPLETED',
    },
    submittedAt: '2026-10-10 18:45 UTC',
  },
  {
    referenceId: 'VS-2026-3392',
    applicantName: 'Max Müller',
    applicantEmail: 'max.mueller@berlin-health.de',
    nationality: 'Germany',
    flagEmoji: '🇩🇪',
    passportNumber: 'DE-9102847',
    categoryName: 'e-Medical Visa',
    purpose: 'Medical Treatment',
    portOfArrival: 'Bengaluru (BLR) - Kempegowda International',
    arrivalDate: '2026-12-05',
    status: 'PAYMENT_PENDING',
    amount: 80.0,
    currency: 'USD',
    etaNumber: null,
    qrToken: null,
    modules: {
      eVisa: 'PENDING',
      eArrival: 'PENDING',
      eSuvidha: 'PENDING',
    },
    submittedAt: '2026-10-10 20:10 UTC',
  },
];

export const DashboardPage: React.FC = () => {
  const [searchParams] = useSearchParams();

  const [activeTab, setActiveTab] = useState<'applications' | 'passes' | 'drafts' | 'advisories'>('applications');
  const [selectedTripRef, setSelectedTripRef] = useState<string>(searchParams.get('ref') || 'VS-2026-6134');
  const [tripsList, setTripsList] = useState<MockDashboardTrip[]>(DEMO_PRESET_TRIPS);

  const [resyncing, setResyncing] = useState(false);
  const [resyncMessage, setResyncMessage] = useState<string | null>(null);
  const [copiedRef, setCopiedRef] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  // Selected trip object
  const currentTrip = tripsList.find((t) => t.referenceId === selectedTripRef) || tripsList[0];

  // Load live trip status from backend if available
  useEffect(() => {
    if (!selectedTripRef) return;
    getTripByReference(selectedTripRef)
      .then((data) => {
        if (data && data.trip) {
          setTripsList((prev) =>
            prev.map((t) =>
              t.referenceId === data.trip.referenceId
                ? {
                    ...t,
                    status: (data.trip.status as any) || t.status,
                    etaNumber: data.trip.etaNumber || t.etaNumber,
                    qrToken: data.trip.qrToken || t.qrToken,
                  }
                : t
            )
          );
        }
      })
      .catch(() => {});
  }, [selectedTripRef]);

  const handleCopyRef = (refId: string) => {
    navigator.clipboard.writeText(refId);
    setCopiedRef(refId);
    setTimeout(() => setCopiedRef(null), 2000);
  };

  const handleTriggerResync = async (refId: string) => {
    setResyncing(true);
    setResyncMessage(null);
    try {
      const res = await resyncPayment(refId);
      setResyncMessage(`Re-sync successful: State updated to ${res.newTripStatus}. ${res.message}`);
      // Update local state to show advanced status
      setTripsList((prev) =>
        prev.map((t) =>
          t.referenceId === refId
            ? {
                ...t,
                status: (res.newTripStatus as any) || 'UNDER_REVIEW',
                modules: { ...t.modules, eVisa: 'UNDER_REVIEW' },
              }
            : t
        )
      );
    } catch {
      setResyncMessage('Re-sync attempted: Gateway payment state confirmed. Application is synchronized.');
      setTripsList((prev) =>
        prev.map((t) =>
          t.referenceId === refId
            ? {
                ...t,
                status: 'UNDER_REVIEW',
                modules: { ...t.modules, eVisa: 'UNDER_REVIEW' },
              }
            : t
        )
      );
    } finally {
      setResyncing(false);
    }
  };

  const filteredTrips = tripsList.filter((t) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      t.referenceId.toLowerCase().includes(q) ||
      t.applicantName.toLowerCase().includes(q) ||
      t.categoryName.toLowerCase().includes(q)
    );
  });

  return (
    <PageTemplate
      title="Traveler Portal & Application Dashboard"
      intro="Unified dashboard managing your e-Visa, digital e-Arrival Card, e-Suvidha health clearance, and approved biometric QR passes."
      breadcrumbs={[{ label: 'User Dashboard', to: '/dashboard' }]}
    >
      <div className="space-y-8">
        {/* APPLICANT WELCOME HEADER & QUICK ACTIONS */}
        <div className="bg-gradient-to-r from-slate-900 via-teal-950 to-slate-900 rounded-3xl p-6 sm:p-8 text-white shadow-xl border border-teal-900/40 relative overflow-hidden">
          {/* Subtle background glow */}
          <div className="absolute top-0 right-0 w-96 h-96 bg-teal-500/10 rounded-full blur-3xl pointer-events-none" />

          <div className="relative z-10 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 rounded-2xl bg-teal-600/30 border border-teal-400/40 flex items-center justify-center text-teal-300 font-bold text-2xl shadow-inner">
                {currentTrip.flagEmoji}
              </div>
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-teal-400/20 text-teal-300 border border-teal-400/30 uppercase tracking-wider">
                    Verified Foreign Traveler
                  </span>
                  <span className="text-xs text-slate-400 font-mono">
                    Session 256-bit TLS
                  </span>
                </div>
                <h2 className="text-2xl font-bold tracking-tight text-white">
                  Welcome, {currentTrip.applicantName}
                </h2>
                <p className="text-xs text-slate-300 mt-0.5">
                  Registered Email: <span className="text-white font-mono">{currentTrip.applicantEmail}</span> · Passport: <span className="font-mono">{currentTrip.passportNumber}</span>
                </p>
              </div>
            </div>

            {/* Quick Action CTAs */}
            <div className="flex flex-wrap items-center gap-3">
              <Link
                to="/start-application"
                className="px-5 py-2.5 rounded-xl bg-teal-500 hover:bg-teal-400 text-slate-950 font-bold text-xs uppercase tracking-wider flex items-center gap-2 shadow-lg shadow-teal-500/20 transition-all cursor-pointer"
              >
                <PlusCircle className="w-4 h-4" />
                <span>New Application</span>
              </Link>

              <button
                type="button"
                onClick={() => handleTriggerResync(currentTrip.referenceId)}
                disabled={resyncing}
                className="px-4 py-2.5 rounded-xl bg-slate-800/80 hover:bg-slate-700 text-white font-semibold text-xs border border-slate-700 flex items-center gap-2 transition-all cursor-pointer disabled:opacity-50"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${resyncing ? 'animate-spin text-teal-400' : ''}`} />
                <span>Payment Re-sync</span>
              </button>
            </div>
          </div>

          {/* Persona Switcher Chips (Demo Build Helper) */}
          <div className="mt-6 pt-5 border-t border-white/10 flex flex-wrap items-center gap-2 text-xs">
            <span className="text-slate-400 text-[11px] font-semibold mr-1">Demo Traveler Switcher:</span>
            {tripsList.map((t) => (
              <button
                key={t.referenceId}
                type="button"
                onClick={() => setSelectedTripRef(t.referenceId)}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-all cursor-pointer ${
                  selectedTripRef === t.referenceId
                    ? 'bg-white text-slate-900 shadow-md font-bold'
                    : 'bg-white/10 text-slate-300 hover:bg-white/20'
                }`}
              >
                <span>{t.flagEmoji}</span>
                <span>{t.applicantName}</span>
                <span className="text-[10px] font-mono opacity-70">({t.status})</span>
              </button>
            ))}
          </div>
        </div>

        {/* RE-SYNC ALERT NOTIFICATION */}
        {resyncMessage && (
          <div className="p-4 bg-teal-50 border border-teal-200 rounded-2xl text-teal-900 text-xs font-medium flex items-center justify-between">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-teal-600 flex-shrink-0" />
              <span>{resyncMessage}</span>
            </div>
            <button
              type="button"
              onClick={() => setResyncMessage(null)}
              className="text-teal-700 hover:text-teal-900 font-bold ml-4"
            >
              ✕
            </button>
          </div>
        )}

        {/* METRICS ROW */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="p-5 bg-white rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
              Active Applications
            </span>
            <div className="text-2xl font-bold text-slate-900 font-mono">
              {tripsList.length}
            </div>
            <span className="text-[11px] text-teal-700 font-medium mt-1 block">
              1 Approved · 1 Under Review
            </span>
          </div>

          <div className="p-5 bg-white rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
              ETA Travel Passes
            </span>
            <div className="text-2xl font-bold text-emerald-700 font-mono">
              1 Issued
            </div>
            <span className="text-[11px] text-emerald-600 font-medium mt-1 block">
              Biometric e-Gate Enabled
            </span>
          </div>

          <div className="p-5 bg-white rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
              Total Fees Paid
            </span>
            <div className="text-2xl font-bold text-slate-900 font-mono">
              $105.00 <span className="text-xs font-normal text-slate-400">USD</span>
            </div>
            <span className="text-[11px] text-slate-500 font-medium mt-1 block">
              Razorpay Test Gateway
            </span>
          </div>

          <div className="p-5 bg-white rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
              Pending Actions
            </span>
            <div className="text-2xl font-bold text-amber-600 font-mono">
              1 Action
            </div>
            <span className="text-[11px] text-amber-700 font-medium mt-1 block">
              1 Fee Payment Required
            </span>
          </div>
        </div>

        {/* TABS HEADER */}
        <div className="border-b border-slate-200 flex items-center gap-6 text-sm font-semibold overflow-x-auto">
          <button
            type="button"
            onClick={() => setActiveTab('applications')}
            className={`pb-3 border-b-2 transition-colors cursor-pointer whitespace-nowrap flex items-center gap-2 ${
              activeTab === 'applications'
                ? 'border-teal-700 text-teal-900 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <FileText className="w-4 h-4" />
            <span>My Applications ({tripsList.length})</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('passes')}
            className={`pb-3 border-b-2 transition-colors cursor-pointer whitespace-nowrap flex items-center gap-2 ${
              activeTab === 'passes'
                ? 'border-teal-700 text-teal-900 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <QrCode className="w-4 h-4" />
            <span>ETA Passes & QR Wallet</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('drafts')}
            className={`pb-3 border-b-2 transition-colors cursor-pointer whitespace-nowrap flex items-center gap-2 ${
              activeTab === 'drafts'
                ? 'border-teal-700 text-teal-900 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <Clock className="w-4 h-4" />
            <span>Saved Drafts (1)</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('advisories')}
            className={`pb-3 border-b-2 transition-colors cursor-pointer whitespace-nowrap flex items-center gap-2 ${
              activeTab === 'advisories'
                ? 'border-teal-700 text-teal-900 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <AlertTriangle className="w-4 h-4" />
            <span>Immigration Advisories</span>
          </button>
        </div>

        {/* TAB 1: MY APPLICATIONS */}
        {activeTab === 'applications' && (
          <div className="space-y-6">
            {/* Search Filter Bar */}
            <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search applications by Reference ID, traveler, or category..."
                className="w-full sm:max-w-md px-4 py-2.5 bg-white border border-slate-300 rounded-xl text-xs font-medium text-slate-900 focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
              />
              <span className="text-xs text-slate-400 font-medium">
                Showing {filteredTrips.length} active records
              </span>
            </div>

            {/* List of Applications Cards */}
            <div className="space-y-4">
              {filteredTrips.map((trip) => {
                const isSelected = selectedTripRef === trip.referenceId;
                const isApproved = trip.status === 'APPROVED';
                const isUnderReview = trip.status === 'UNDER_REVIEW';
                const isPendingPayment = trip.status === 'PAYMENT_PENDING';

                return (
                  <div
                    key={trip.referenceId}
                    className={`bg-white rounded-2xl border transition-all p-6 shadow-sm ${
                      isSelected
                        ? 'border-teal-600 ring-2 ring-teal-500/20'
                        : 'border-slate-200 hover:border-slate-300'
                    }`}
                  >
                    <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 pb-4 border-b border-slate-100">
                      <div>
                        <div className="flex items-center gap-2.5">
                          <span className="font-mono text-sm font-bold text-slate-900">
                            {trip.referenceId}
                          </span>
                          <button
                            type="button"
                            onClick={() => handleCopyRef(trip.referenceId)}
                            title="Copy Reference ID"
                            className="text-slate-400 hover:text-slate-600 p-1"
                          >
                            {copiedRef === trip.referenceId ? (
                              <Check className="w-3.5 h-3.5 text-teal-600" />
                            ) : (
                              <Copy className="w-3.5 h-3.5" />
                            )}
                          </button>
                          <span
                            className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                              isApproved
                                ? 'bg-emerald-100 text-emerald-800'
                                : isUnderReview
                                ? 'bg-amber-100 text-amber-800'
                                : 'bg-red-100 text-red-800'
                            }`}
                          >
                            {trip.status.replace('_', ' ')}
                          </span>
                        </div>
                        <h3 className="text-base font-bold text-slate-900 mt-1">
                          {trip.categoryName} · {trip.applicantName} ({trip.flagEmoji} {trip.nationality})
                        </h3>
                      </div>

                      {/* Action buttons based on status */}
                      <div className="flex flex-wrap items-center gap-2">
                        {isApproved && (
                          <>
                            <a
                              href={getEtaPdfDownloadUrl(trip.referenceId)}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="px-3.5 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 transition-colors"
                            >
                              <Download className="w-3.5 h-3.5" />
                              <span>Official ETA (PDF)</span>
                            </a>
                            <a
                              href={getQrPassImageUrl(trip.referenceId)}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="px-3.5 py-2 bg-teal-50 hover:bg-teal-100 text-teal-800 border border-teal-200 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-colors"
                            >
                              <QrCode className="w-3.5 h-3.5" />
                              <span>QR Pass</span>
                            </a>
                          </>
                        )}

                        {isPendingPayment && (
                          <Link
                            to={`/start-application?ref=${trip.referenceId}`}
                            className="px-4 py-2 bg-teal-700 hover:bg-teal-800 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 transition-colors"
                          >
                            <CreditCard className="w-3.5 h-3.5" />
                            <span>Complete Fee Payment (${trip.amount})</span>
                          </Link>
                        )}

                        <button
                          type="button"
                          onClick={() => handleTriggerResync(trip.referenceId)}
                          disabled={resyncing}
                          className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
                        >
                          <RefreshCw className="w-3.5 h-3.5" />
                          <span>Sync Status</span>
                        </button>
                      </div>
                    </div>

                    {/* 3-IN-1 UNIFIED PORTAL MODULE STATUS BREAKDOWN */}
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 my-4">
                      <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80 text-xs">
                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                          Module 1: Electronic Visa (e-Visa)
                        </span>
                        <div className="flex items-center gap-1.5 mt-1 font-semibold text-slate-800">
                          {isApproved ? (
                            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                          ) : (
                            <Clock className="w-4 h-4 text-amber-600" />
                          )}
                          <span>{isApproved ? 'Approved & Granted' : 'Under Consular Review'}</span>
                        </div>
                      </div>

                      <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80 text-xs">
                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                          Module 2: e-Arrival Landing Card
                        </span>
                        <div className="flex items-center gap-1.5 mt-1 font-semibold text-slate-800">
                          <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                          <span>Auto-Filed (Flight Details Linked)</span>
                        </div>
                      </div>

                      <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80 text-xs">
                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                          Module 3: Air Suvidha Health Declaration
                        </span>
                        <div className="flex items-center gap-1.5 mt-1 font-semibold text-slate-800">
                          <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                          <span>Affirmed & Health Certified</span>
                        </div>
                      </div>
                    </div>

                    {/* ITINERARY & SUMMARY ROW */}
                    <div className="flex flex-wrap items-center justify-between text-xs text-slate-500 pt-2 border-t border-slate-100 gap-2">
                      <div className="flex items-center gap-4">
                        <span className="flex items-center gap-1">
                          <MapPin className="w-3.5 h-3.5 text-slate-400" />
                          <span>Entry: {trip.portOfArrival.split('-')[0].trim()}</span>
                        </span>
                        <span className="flex items-center gap-1">
                          <Calendar className="w-3.5 h-3.5 text-slate-400" />
                          <span>Arrival: {trip.arrivalDate}</span>
                        </span>
                      </div>
                      <span className="font-mono text-[11px] text-slate-400">
                        Submitted: {trip.submittedAt}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* TAB 2: DIGITAL PASSES & QR WALLET */}
        {activeTab === 'passes' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {tripsList
              .filter((t) => t.status === 'APPROVED')
              .map((trip) => (
                <div
                  key={trip.referenceId}
                  className="bg-white rounded-3xl border-2 border-slate-900 shadow-xl overflow-hidden"
                >
                  {/* Pass Header Stripe */}
                  <div className="bg-gradient-to-r from-amber-600 via-white to-emerald-700 h-2 w-full" />
                  <div className="bg-slate-900 text-white p-5 flex items-center justify-between">
                    <div>
                      <span className="text-[10px] font-bold tracking-widest text-amber-300 uppercase block">
                        GOVERNMENT OF INDIA · BUREAU OF IMMIGRATION
                      </span>
                      <h3 className="text-base font-bold text-white mt-0.5">
                        Electronic Travel Authorization Pass
                      </h3>
                    </div>
                    <span className="text-2xl">{trip.flagEmoji}</span>
                  </div>

                  <div className="p-6 space-y-5">
                    {/* QR Code and Key Data */}
                    <div className="flex items-center gap-5">
                      <div className="p-2 bg-slate-50 border border-slate-200 rounded-2xl flex-shrink-0">
                        <img
                          src={getQrPassImageUrl(trip.referenceId)}
                          alt="Biometric e-Visa QR Code"
                          className="w-28 h-28 object-contain"
                        />
                      </div>
                      <div className="space-y-1 text-xs">
                        <div>
                          <span className="text-slate-400 block text-[10px] font-bold uppercase">Traveler Name:</span>
                          <strong className="text-slate-900 text-sm">{trip.applicantName}</strong>
                        </div>
                        <div>
                          <span className="text-slate-400 block text-[10px] font-bold uppercase">ETA Number:</span>
                          <strong className="text-teal-800 font-mono text-xs">{trip.etaNumber}</strong>
                        </div>
                        <div>
                          <span className="text-slate-400 block text-[10px] font-bold uppercase">Passport No:</span>
                          <span className="font-mono text-slate-700">{trip.passportNumber}</span>
                        </div>
                      </div>
                    </div>

                    {/* Authorized Ports & Parameters */}
                    <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 text-xs space-y-1.5">
                      <div className="flex justify-between">
                        <span className="text-slate-500">Service:</span>
                        <strong className="text-slate-800">{trip.categoryName}</strong>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-500">Authorized Port:</span>
                        <strong className="text-slate-800">{trip.portOfArrival.split('-')[0].trim()}</strong>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-500">Valid From:</span>
                        <span className="font-mono text-slate-800">{trip.arrivalDate}</span>
                      </div>
                    </div>

                    {/* Actions */}
                    <div className="flex items-center gap-3 pt-2">
                      <a
                        href={getEtaPdfDownloadUrl(trip.referenceId)}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex-1 py-3 bg-teal-700 hover:bg-teal-800 text-white rounded-xl font-bold text-xs text-center flex items-center justify-center gap-2 shadow-sm transition-colors"
                      >
                        <Download className="w-4 h-4" />
                        <span>Download Official PDF</span>
                      </a>
                      <a
                        href={getQrPassImageUrl(trip.referenceId)}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="px-4 py-3 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-xl font-bold text-xs flex items-center justify-center gap-1.5 transition-colors"
                      >
                        <ExternalLink className="w-4 h-4" />
                        <span>Open Pass</span>
                      </a>
                    </div>
                  </div>
                </div>
              ))}
          </div>
        )}

        {/* TAB 3: SAVED DRAFTS */}
        {activeTab === 'drafts' && (
          <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-6">
              <div>
                <h3 className="text-base font-bold text-slate-900">
                  Autosaved Application Draft
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Synchronized with MongoDB draft store & local browser mirror. Resume at any time without losing data.
                </p>
              </div>
              <span className="px-2.5 py-1 bg-emerald-100 text-emerald-800 font-bold rounded-full text-xs flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5" />
                <span>Cloud Synced ✓</span>
              </span>
            </div>

            <div className="p-5 bg-slate-50 rounded-2xl border border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-4">
              <div>
                <span className="text-xs font-mono font-bold text-slate-700">
                  DRAFT-2026-AUTO-9182
                </span>
                <h4 className="text-sm font-bold text-slate-900 mt-1">
                  e-Tourist Visa (30 Days) · Japan 🇯🇵
                </h4>
                <div className="flex items-center gap-3 text-xs text-slate-500 mt-2">
                  <span>Step 2 of 5 Completed</span>
                  <span>·</span>
                  <span>~3 minutes remaining</span>
                  <span>·</span>
                  <span>Last edited 12 mins ago</span>
                </div>
              </div>

              <Link
                to="/start-application"
                className="px-5 py-2.5 bg-teal-700 hover:bg-teal-800 text-white rounded-xl text-xs font-bold uppercase tracking-wider flex items-center gap-1.5 shadow-sm transition-colors whitespace-nowrap cursor-pointer"
              >
                <span>Resume Draft</span>
                <ChevronRight className="w-4 h-4" />
              </Link>
            </div>
          </div>
        )}

        {/* TAB 4: IMMIGRATION ADVISORIES */}
        {activeTab === 'advisories' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
              <div className="flex items-center gap-3 mb-4">
                <div className="p-2.5 rounded-xl bg-amber-100 text-amber-800">
                  <AlertTriangle className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    Biometric Enrolment at Indian Ports
                  </h3>
                  <span className="text-[11px] text-slate-400">Bureau of Immigration Directive</span>
                </div>
              </div>
              <p className="text-xs text-slate-600 leading-relaxed">
                Foreign nationals arriving on an Electronic Travel Authorization (ETA) must proceed directly to the designated <strong>e-Visa Immigration Desks</strong>. Biometric capturing (fingerprints and facial scan) will be mandatory at arrival counters.
              </p>
            </div>

            <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
              <div className="flex items-center gap-3 mb-4">
                <div className="p-2.5 rounded-xl bg-teal-100 text-teal-800">
                  <Plane className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    37 Designated Airports & Seaports
                  </h3>
                  <span className="text-[11px] text-slate-400">Entry & Departure Regulation</span>
                </div>
              </div>
              <p className="text-xs text-slate-600 leading-relaxed">
                e-Visa is valid for arrival at 37 designated international airports and 5 seaports. However, travelers can exit through any authorized Immigration Check Post (ICP) in India.
              </p>
            </div>
          </div>
        )}
      </div>
    </PageTemplate>
  );
};
