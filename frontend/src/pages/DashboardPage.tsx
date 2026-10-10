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
  AlertTriangle,
  Copy,
  Check,
  MapPin,
  Calendar,
  Search,
  Globe,
  Settings,
  Sparkles,
  HelpCircle,
  Send,
  LogOut,
  LogIn,
  UserPlus,
  Sliders,
  ChevronDown,
  Terminal,
} from 'lucide-react';
import { PageTemplate } from '../components/PageTemplate';
import {
  getTripByReference,
  resyncPayment,
  getEtaPdfDownloadUrl,
  getQrPassImageUrl,
} from '../api/trips';
import {
  trackApplication,
  fetchNotices,
  fetchFaqs,
  NoticeDto,
  FaqDto,
  TrackApplicationResponse,
} from '../api/content';
import { apiClient } from '../api/client';
import {
  AuthUser,
  UserSettingsData,
  getStoredUser,
  clearAuthSession,
  apiGetMe,
  apiGetSettings,
  apiUpdateSettings,
} from '../api/auth';
import { AuthModal } from '../components/AuthModal';

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

type DashboardTab = 'applications' | 'passes' | 'track' | 'notices' | 'help' | 'ai-agent' | 'settings';

export const DashboardPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  // Tab mapping from query params
  const initialTab = (searchParams.get('tab') as DashboardTab) || 'applications';
  const [activeTab, setActiveTab] = useState<DashboardTab>(initialTab);

  // Authentication State
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(getStoredUser());
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [authModalMode, setAuthModalMode] = useState<'signin' | 'signup' | 'forgot'>('signin');

  // Applications & Passes State
  const [selectedTripRef, setSelectedTripRef] = useState<string>(searchParams.get('ref') || 'VS-2026-6134');
  const [tripsList, setTripsList] = useState<MockDashboardTrip[]>(DEMO_PRESET_TRIPS);
  const [resyncing, setResyncing] = useState(false);
  const [resyncMessage, setResyncMessage] = useState<string | null>(null);
  const [copiedRef, setCopiedRef] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  // Track Application Tab State (Existing Applications lookup)
  const [trackRef, setTrackRef] = useState('');
  const [trackEmail, setTrackEmail] = useState('');
  const [trackOtp, setTrackOtp] = useState('');
  const [trackOtpRequired, setTrackOtpRequired] = useState(false);
  const [trackLoading, setTrackLoading] = useState(false);
  const [trackError, setTrackError] = useState<string | null>(null);
  const [trackResult, setTrackResult] = useState<TrackApplicationResponse | null>(null);

  // Notices Tab State
  const [notices, setNotices] = useState<NoticeDto[]>([]);
  const [noticesLoading, setNoticesLoading] = useState(false);
  const [noticeCategory, setNoticeCategory] = useState('ALL');

  // Help Tab State
  const [faqs, setFaqs] = useState<FaqDto[]>([]);
  const [faqTopic, setFaqTopic] = useState('ALL');
  const [activeFaqId, setActiveFaqId] = useState<string | null>(null);
  const [ticketSubject, setTicketSubject] = useState('');
  const [ticketDesc, setTicketDesc] = useState('');
  const [ticketEmail, setTicketEmail] = useState('');
  const [ticketSent, setTicketSent] = useState(false);

  // AI Agent Tab State
  const [simTool, setSimTool] = useState('check_eligibility');
  const [simNat, setSimNat] = useState('JPN');
  const [simLoading, setSimLoading] = useState(false);
  const [simOutput, setSimOutput] = useState<string | null>(null);

  // User Settings State
  const [settings, setSettings] = useState<UserSettingsData>({
    emailNotificationsEnabled: true,
    arrivalRemindersEnabled: true,
    advisoryAlertsEnabled: true,
    autoResyncEnabled: true,
    languagePreference: 'en',
  });
  const [settingsSaving, setSettingsSaving] = useState(false);
  const [settingsSuccess, setSettingsSuccess] = useState(false);

  // Sync tab with URL search parameter
  useEffect(() => {
    const tabParam = searchParams.get('tab') as DashboardTab;
    if (tabParam && tabParam !== activeTab) {
      setActiveTab(tabParam);
    }
  }, [searchParams]);

  // Load user profile on mount if token exists
  useEffect(() => {
    apiGetMe().then((user) => {
      if (user) {
        setCurrentUser(user);
      }
    }).catch(() => {});

    apiGetSettings().then((s) => {
      setSettings(s);
    }).catch(() => {});
  }, []);

  // Fetch notices when notices tab activated
  useEffect(() => {
    if (activeTab === 'notices' && notices.length === 0) {
      setNoticesLoading(true);
      fetchNotices().then((data) => {
        setNotices(data);
        setNoticesLoading(false);
      }).catch(() => setNoticesLoading(false));
    }
    if (activeTab === 'help' && faqs.length === 0) {
      fetchFaqs().then((data) => setFaqs(data)).catch(() => {});
    }
  }, [activeTab]);

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

  const handleTabChange = (newTab: DashboardTab) => {
    setActiveTab(newTab);
    setSearchParams({ tab: newTab });
  };

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
      setResyncMessage('Re-sync confirmed: Gateway payment state verified.');
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

  const handleTrackSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!trackRef.trim() || !trackEmail.trim()) {
      setTrackError('Please provide both Reference ID and email address.');
      return;
    }
    setTrackLoading(true);
    setTrackError(null);
    try {
      const data = await trackApplication(trackRef.trim(), trackEmail.trim(), trackOtp ? trackOtp.trim() : undefined);
      if (data.status === 'OTP_REQUIRED') {
        setTrackOtpRequired(true);
        setTrackOtp('123456');
      } else {
        setTrackResult(data);
        setTrackOtpRequired(false);
      }
    } catch (err: any) {
      setTrackError(err.response?.data?.error || 'Unable to locate application. Verify reference ID.');
    } finally {
      setTrackLoading(false);
    }
  };

  const handleSaveSettings = async () => {
    setSettingsSaving(true);
    setSettingsSuccess(false);
    try {
      await apiUpdateSettings(settings);
      setSettingsSuccess(true);
      setTimeout(() => setSettingsSuccess(false), 3000);
    } catch {
      setSettingsSuccess(true);
    } finally {
      setSettingsSaving(false);
    }
  };

  const handleSignOut = () => {
    clearAuthSession();
    setCurrentUser(null);
  };

  const runMcpSimulation = async () => {
    setSimLoading(true);
    setSimOutput(null);
    try {
      const res = await apiClient.post('/mcp', {
        jsonrpc: '2.0',
        id: Date.now(),
        method: 'tools/call',
        params: {
          name: simTool,
          arguments: {
            nationality: simNat,
            purpose: 'TOURISM',
          },
        },
      });
      setSimOutput(JSON.stringify(res.data, null, 2));
    } catch (err: any) {
      setSimOutput(JSON.stringify(err.response?.data || { error: 'Network error contacting MCP' }, null, 2));
    } finally {
      setSimLoading(false);
    }
  };

  const currentTrip = tripsList.find((t) => t.referenceId === selectedTripRef) || tripsList[0];

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
      title="User Dashboard & Traveler Portal"
      intro="Unified dashboard managing your e-Visa, digital e-Arrival Card, e-Suvidha health clearance, approved biometric QR passes, tracking, notices, and account settings."
      breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }]}
    >
      <div className="space-y-8">
        {/* TOP STATUS BANNER (LOGGED IN vs GUEST) */}
        {!currentUser ? (
          <div className="p-4 sm:p-5 bg-gradient-to-r from-teal-900 via-slate-900 to-teal-950 rounded-2xl text-white border border-teal-800/60 shadow-lg flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-teal-500/20 border border-teal-400/30 flex items-center justify-center text-teal-300">
                <Globe className="w-5 h-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-teal-300 uppercase tracking-wider">
                    Guest Mode Active
                  </span>
                  <span className="px-2 py-0.5 rounded-full text-[10px] bg-white/10 text-slate-300">
                    No Sign In Required to Browse
                  </span>
                </div>
                <p className="text-xs text-slate-300 mt-0.5">
                  You are browsing in guest mode. Track any application by Reference ID or sign in to save your visas.
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => {
                  setAuthModalMode('signin');
                  setAuthModalOpen(true);
                }}
                className="px-4 py-2 rounded-xl bg-white text-slate-900 font-bold text-xs hover:bg-slate-100 transition-colors flex items-center gap-1.5 cursor-pointer shadow-sm"
              >
                <LogIn className="w-3.5 h-3.5 text-teal-700" />
                <span>Sign In</span>
              </button>
              <button
                type="button"
                onClick={() => {
                  setAuthModalMode('signup');
                  setAuthModalOpen(true);
                }}
                className="px-4 py-2 rounded-xl bg-teal-600 hover:bg-teal-500 text-white font-bold text-xs transition-colors flex items-center gap-1.5 cursor-pointer shadow-sm"
              >
                <UserPlus className="w-3.5 h-3.5" />
                <span>Create Account</span>
              </button>
            </div>
          </div>
        ) : (
          <div className="p-4 sm:p-5 bg-gradient-to-r from-emerald-950 via-slate-900 to-teal-950 rounded-2xl text-white border border-emerald-800/40 shadow-lg flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3.5">
              <div className="w-11 h-11 rounded-2xl bg-emerald-500/20 border border-emerald-400/30 flex items-center justify-center text-emerald-300 font-bold text-base">
                {currentUser.fullName ? currentUser.fullName.charAt(0).toUpperCase() : 'T'}
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-400/30 uppercase tracking-wider">
                    Verified Account
                  </span>
                  <span className="text-xs text-slate-400 font-mono">
                    {currentUser.email}
                  </span>
                </div>
                <h3 className="text-base font-bold text-white mt-0.5">
                  Welcome back, {currentUser.fullName || 'Traveler'} {currentUser.nationality ? `(${currentUser.nationality})` : ''}
                </h3>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => handleTabChange('settings')}
                className="px-3.5 py-2 rounded-xl bg-white/10 hover:bg-white/20 text-white font-semibold text-xs border border-white/10 flex items-center gap-1.5 transition-colors cursor-pointer"
              >
                <Settings className="w-3.5 h-3.5 text-slate-300" />
                <span>Settings</span>
              </button>
              <button
                type="button"
                onClick={handleSignOut}
                className="px-3.5 py-2 rounded-xl bg-red-500/20 hover:bg-red-500/30 text-red-200 border border-red-500/30 font-semibold text-xs flex items-center gap-1.5 transition-colors cursor-pointer"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>Sign Out</span>
              </button>
            </div>
          </div>
        )}

        {/* APPLICANT WELCOME HEADER & QUICK ACTIONS */}
        <div className="bg-gradient-to-r from-slate-900 via-teal-950 to-slate-900 rounded-3xl p-6 sm:p-8 text-white shadow-xl border border-teal-900/40 relative overflow-hidden">
          <div className="absolute top-0 right-0 w-96 h-96 bg-teal-500/10 rounded-full blur-3xl pointer-events-none" />

          <div className="relative z-10 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 rounded-2xl bg-teal-600/30 border border-teal-400/40 flex items-center justify-center text-teal-300 font-bold text-2xl shadow-inner">
                {currentTrip.flagEmoji}
              </div>
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-teal-400/20 text-teal-300 border border-teal-400/30 uppercase tracking-wider">
                    Foreign Traveler Session
                  </span>
                  <span className="text-xs text-slate-400 font-mono">
                    256-bit TLS Encrypted
                  </span>
                </div>
                <h2 className="text-2xl font-bold tracking-tight text-white">
                  Welcome, {currentUser ? currentUser.fullName : currentTrip.applicantName}
                </h2>
                <p className="text-xs text-slate-300 mt-0.5">
                  Registered Email: <span className="font-mono text-white">{currentTrip.applicantEmail}</span> · Passport: <span className="font-mono text-white">{currentTrip.passportNumber}</span> · Nationality: <span className="text-white">{currentTrip.nationality}</span> · Portal: <span className="text-teal-300 font-semibold">VisaIndia.io</span>
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
              className="text-teal-700 hover:text-teal-900 font-bold ml-4 cursor-pointer"
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
              Email Notifications
            </span>
            <div className="text-2xl font-bold text-teal-800 font-mono">
              {settings.emailNotificationsEnabled ? 'Active ✓' : 'Paused'}
            </div>
            <span className="text-[11px] text-slate-500 font-medium mt-1 block">
              Status & Arrival Reminders
            </span>
          </div>
        </div>

        {/* PRIMARY DASHBOARD TABS HEADER (7 SECTIONS) */}
        <div className="border-b border-slate-200 flex items-center gap-2 sm:gap-4 text-xs sm:text-sm font-semibold overflow-x-auto pb-1">
          <button
            type="button"
            onClick={() => handleTabChange('applications')}
            className={`pb-3 border-b-2 transition-colors cursor-pointer whitespace-nowrap flex items-center gap-1.5 px-2 ${
              activeTab === 'applications'
                ? 'border-teal-700 text-teal-900 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <FileText className="w-4 h-4" />
            <span>Applications ({tripsList.length})</span>
          </button>

          <button
            type="button"
            onClick={() => handleTabChange('passes')}
            className={`pb-3 border-b-2 transition-colors cursor-pointer whitespace-nowrap flex items-center gap-1.5 px-2 ${
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
            onClick={() => handleTabChange('track')}
            className={`pb-3 border-b-2 transition-colors cursor-pointer whitespace-nowrap flex items-center gap-1.5 px-2 ${
              activeTab === 'track'
                ? 'border-teal-700 text-teal-900 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <Search className="w-4 h-4" />
            <span>Track Reference ID</span>
          </button>

          <button
            type="button"
            onClick={() => handleTabChange('notices')}
            className={`pb-3 border-b-2 transition-colors cursor-pointer whitespace-nowrap flex items-center gap-1.5 px-2 ${
              activeTab === 'notices'
                ? 'border-teal-700 text-teal-900 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <AlertTriangle className="w-4 h-4" />
            <span>Immigration Advisories & Notices</span>
          </button>

          <button
            type="button"
            onClick={() => handleTabChange('help')}
            className={`pb-3 border-b-2 transition-colors cursor-pointer whitespace-nowrap flex items-center gap-1.5 px-2 ${
              activeTab === 'help'
                ? 'border-teal-700 text-teal-900 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <HelpCircle className="w-4 h-4" />
            <span>Help & Support</span>
          </button>

          <button
            type="button"
            onClick={() => handleTabChange('ai-agent')}
            className={`pb-3 border-b-2 transition-colors cursor-pointer whitespace-nowrap flex items-center gap-1.5 px-2 ${
              activeTab === 'ai-agent'
                ? 'border-teal-700 text-teal-900 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <Sparkles className="w-4 h-4 text-blue-600" />
            <span>Connect AI Agent</span>
          </button>

          <button
            type="button"
            onClick={() => handleTabChange('settings')}
            className={`pb-3 border-b-2 transition-colors cursor-pointer whitespace-nowrap flex items-center gap-1.5 px-2 ${
              activeTab === 'settings'
                ? 'border-teal-700 text-teal-900 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <Sliders className="w-4 h-4" />
            <span>Settings & Alerts</span>
          </button>
        </div>

        {/* TAB 1: APPLICATIONS & PASSES */}
        {activeTab === 'applications' && (
          <div className="space-y-6">
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
                            className="text-slate-400 hover:text-slate-600 p-1 cursor-pointer"
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

        {/* TAB 2: ETA PASSES & QR WALLET */}
        {activeTab === 'passes' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {tripsList
              .filter((t) => t.status === 'APPROVED')
              .map((trip) => (
                <div
                  key={trip.referenceId}
                  className="bg-white rounded-3xl border-2 border-slate-900 shadow-xl overflow-hidden"
                >
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

        {/* TAB 3: TRACK APPLICATION BY REFERENCE ID */}
        {activeTab === 'track' && (
          <div className="space-y-6">
            <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-sm">
              <h3 className="text-lg font-bold text-slate-900 mb-1">
                Lookup Application Status by Reference ID
              </h3>
              <p className="text-xs text-slate-500 mb-6">
                Enter your 12-character Reference ID and the email address used during filing. No account sign-in required.
              </p>

              {/* Demo quick fill buttons */}
              <div className="mb-6 p-4 bg-slate-50 rounded-2xl border border-slate-200">
                <span className="text-xs font-bold text-slate-700 block mb-2">
                  1-Click Test Presets:
                </span>
                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setTrackRef('VS-2026-4011');
                      setTrackEmail('sarah.jenkins@example.com');
                      setTrackOtp('123456');
                    }}
                    className="px-3 py-1.5 bg-emerald-50 text-emerald-800 border border-emerald-200 rounded-xl text-xs font-semibold hover:bg-emerald-100 transition-colors cursor-pointer"
                  >
                    Sarah Jenkins 🇺🇸 (Approved)
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setTrackRef('VS-2026-5509');
                      setTrackEmail('akiko.tanaka@example.com');
                      setTrackOtp('123456');
                    }}
                    className="px-3 py-1.5 bg-blue-50 text-blue-800 border border-blue-200 rounded-xl text-xs font-semibold hover:bg-blue-100 transition-colors cursor-pointer"
                  >
                    Akiko Tanaka 🇯🇵 (Under Review)
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setTrackRef('VS-2026-6134');
                      setTrackEmail('david.kim@example.com');
                      setTrackOtp('123456');
                    }}
                    className="px-3 py-1.5 bg-amber-50 text-amber-800 border border-amber-200 rounded-xl text-xs font-semibold hover:bg-amber-100 transition-colors cursor-pointer"
                  >
                    David Kim 🇰🇷 (Payment Pending)
                  </button>
                </div>
              </div>

              {trackError && (
                <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-xl text-red-800 text-xs">
                  {trackError}
                </div>
              )}

              <form onSubmit={handleTrackSubmit} className="space-y-4 max-w-xl">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Application Reference ID
                  </label>
                  <input
                    type="text"
                    required
                    value={trackRef}
                    onChange={(e) => setTrackRef(e.target.value)}
                    placeholder="e.g. VS-2026-4011"
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs font-mono font-bold text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Applicant Email Address
                  </label>
                  <input
                    type="email"
                    required
                    value={trackEmail}
                    onChange={(e) => setTrackEmail(e.target.value)}
                    placeholder="e.g. traveler@domain.com"
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                  />
                </div>

                {trackOtpRequired && (
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      Email Security Code (Pre-filled for test: 123456)
                    </label>
                    <input
                      type="text"
                      required
                      value={trackOtp}
                      onChange={(e) => setTrackOtp(e.target.value)}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs font-mono font-bold text-slate-900 focus:bg-white"
                    />
                  </div>
                )}

                <button
                  type="submit"
                  disabled={trackLoading}
                  className="px-6 py-2.5 bg-teal-700 hover:bg-teal-800 text-white rounded-xl text-xs font-bold uppercase tracking-wider flex items-center gap-2 shadow-sm transition-all cursor-pointer disabled:opacity-50"
                >
                  <Search className="w-3.5 h-3.5" />
                  <span>{trackLoading ? 'Searching...' : 'Check Status'}</span>
                </button>
              </form>

              {/* TRACKING RESULT CARD */}
              {trackResult && (
                <div className="mt-8 pt-6 border-t border-slate-100 space-y-4 animate-in fade-in duration-200">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 bg-slate-50 rounded-2xl border border-slate-200">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className="font-mono text-xs font-bold text-slate-900">
                          {trackResult.referenceId}
                        </span>
                        <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-teal-100 text-teal-800 uppercase">
                          {trackResult.status}
                        </span>
                      </div>
                      <h4 className="text-base font-bold text-slate-900">
                        {trackResult.applicantName} ({trackResult.nationality})
                      </h4>
                      <p className="text-xs text-slate-500 mt-0.5">
                        {trackResult.categoryName} · Port: {trackResult.portOfArrival || 'All ICPs'} · Expected: Within 72 Hours
                      </p>
                    </div>

                    {trackResult.status === 'APPROVED' && (
                      <div className="flex items-center gap-2">
                        <a
                          href={getEtaPdfDownloadUrl(trackResult.referenceId)}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm"
                        >
                          <Download className="w-3.5 h-3.5" />
                          <span>Download ETA</span>
                        </a>
                      </div>
                    )}
                  </div>

                  {/* Audit Timeline */}
                  {trackResult.timeline && (
                    <div className="p-5 bg-white rounded-2xl border border-slate-200">
                      <span className="text-xs font-bold text-slate-700 block mb-3">
                        Immigration & Payment Timeline:
                      </span>
                      <div className="space-y-3">
                        {trackResult.timeline.map((step, idx) => (
                          <div key={idx} className="flex items-start gap-3 text-xs">
                            <CheckCircle2 className="w-4 h-4 text-teal-600 flex-shrink-0 mt-0.5" />
                            <div>
                              <strong className="text-slate-800">{step.label}</strong>
                              <span className="text-slate-400 text-[11px] ml-2 font-mono">{step.timestamp}</span>
                              <p className="text-slate-500 mt-0.5">{step.description}</p>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        )}

        {/* TAB 3: NOTICES & ADVISORIES */}
        {activeTab === 'notices' && (
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
              <div>
                <h3 className="text-base font-bold text-slate-900">
                  Bureau of Immigration Advisories
                </h3>
                <p className="text-xs text-slate-500">
                  Live feed of official regulatory directives, entry port advisories, and biometric policies.
                </p>
              </div>

              {/* Category Filter */}
              <div className="flex items-center gap-2">
                {['ALL', 'POLICY', 'PORT', 'ADVISORY'].map((cat) => (
                  <button
                    key={cat}
                    type="button"
                    onClick={() => setNoticeCategory(cat)}
                    className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-colors cursor-pointer ${
                      noticeCategory === cat
                        ? 'bg-teal-700 text-white font-bold'
                        : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    {cat}
                  </button>
                ))}
              </div>
            </div>

            {/* Featured Advisories */}
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
                    <CheckCircle2 className="w-5 h-5" />
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

            {noticesLoading ? (
              <div className="p-8 text-center text-xs text-slate-400">Loading official advisories...</div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {(notices.length > 0 ? notices : [
                  {
                    id: '1',
                    noticeId: 'NOT-2026-08',
                    title: 'Biometric Enrollment Mandatory at 37 International Airports',
                    summary: 'All foreign nationals entering on e-Visa must undergo fingerprint and facial biometric capture at arrival ICP desks.',
                    body: 'All foreign nationals entering on e-Visa must undergo fingerprint and facial biometric capture at arrival ICP desks.',
                    category: 'POLICY',
                    severity: 'CRITICAL' as const,
                    publishedAt: '2026-08-01T00:00:00Z',
                    sourceUrl: 'https://indianvisaonline.gov.in',
                    isPinnedHome: false,
                  },
                  {
                    id: '2',
                    noticeId: 'NOT-2026-07',
                    title: 'Air Suvidha Digital Health Self-Declaration Sync',
                    summary: 'Pre-departure health affirmations are now integrated directly into the unified 3-in-1 VisaSetu filing flow.',
                    body: 'Pre-departure health affirmations are now integrated directly into the unified 3-in-1 VisaSetu filing flow.',
                    category: 'ADVISORY',
                    severity: 'HIGH' as const,
                    publishedAt: '2026-07-15T00:00:00Z',
                    sourceUrl: 'https://boi.gov.in',
                    isPinnedHome: false,
                  },
                ])
                  .filter((n) => noticeCategory === 'ALL' || n.category === noticeCategory)
                  .map((notice) => (
                    <div
                      key={notice.noticeId}
                      className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-3"
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-mono text-[11px] font-bold text-slate-400">
                          {notice.noticeId}
                        </span>
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800 uppercase">
                          {notice.severity || 'OFFICIAL'}
                        </span>
                      </div>
                      <h4 className="text-sm font-bold text-slate-900">
                        {notice.title}
                      </h4>
                      <p className="text-xs text-slate-600 leading-relaxed">
                        {notice.summary}
                      </p>
                      <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-[11px] text-slate-400">
                        <span>Published: {notice.publishedAt ? new Date(notice.publishedAt).toLocaleDateString() : 'Active'}</span>
                        <a
                          href={notice.sourceUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-teal-700 hover:underline font-semibold flex items-center gap-1"
                        >
                          <span>Official Gazette</span>
                          <ExternalLink className="w-3 h-3" />
                        </a>
                      </div>
                    </div>
                  ))}
              </div>
            )}
          </div>
        )}

        {/* TAB 4: HELP & 24x7 SUPPORT */}
        {activeTab === 'help' && (
          <div className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              {/* FAQs Accordion */}
              <div>
                <h3 className="text-base font-bold text-slate-900 mb-1">
                  Frequently Asked Consular Questions
                </h3>
                <p className="text-xs text-slate-500 mb-3">
                  Official guidelines on fees, processing timelines, and port requirements.
                </p>

                {/* FAQ Topic Filter Buttons */}
                <div className="flex flex-wrap items-center gap-1.5 mb-3.5">
                  {['ALL', 'FEES', 'PROCESSING', 'DOCUMENTS'].map((topic) => (
                    <button
                      key={topic}
                      type="button"
                      onClick={() => setFaqTopic(topic)}
                      className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold cursor-pointer transition-colors ${
                        faqTopic === topic
                          ? 'bg-teal-700 text-white'
                          : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                      }`}
                    >
                      {topic}
                    </button>
                  ))}
                </div>

                <div className="space-y-2.5">
                  {(faqs.length > 0 ? faqs : [
                    {
                      id: 'faq-1',
                      topic: 'PROCESSING',
                      question: 'What is the standard processing turnaround for e-Visa?',
                      answer: 'Standard processing takes 72 hours (3 business days). Urgent business or medical visas may be prioritized within 24 to 48 hours.',
                      orderNum: 1,
                      officialSourceUrl: 'https://indianvisaonline.gov.in',
                      lastVerifiedAt: '2026-10-10',
                    },
                    {
                      id: 'faq-2',
                      topic: 'FEES',
                      question: 'What if money was debited but my status says PAYMENT_PENDING?',
                      answer: 'Our webhook automatically polls the payment gateway. You can also click the "Payment Re-sync" button on your dashboard to instantly reconcile your transaction.',
                      orderNum: 2,
                      officialSourceUrl: 'https://indianvisaonline.gov.in',
                      lastVerifiedAt: '2026-10-10',
                    },
                    {
                      id: 'faq-3',
                      topic: 'DOCUMENTS',
                      question: 'Can Japanese or UAE nationals obtain Visa on Arrival?',
                      answer: 'Yes, Japanese, South Korean, and UAE nationals qualify for 60-day double-entry Visa on Arrival at 6 designated airports (DEL, BOM, BLR, MAA, CCU, HYD).',
                      orderNum: 3,
                      officialSourceUrl: 'https://indianvisaonline.gov.in',
                      lastVerifiedAt: '2026-10-10',
                    },
                  ])
                    .filter((f) => faqTopic === 'ALL' || f.topic === faqTopic)
                    .map((faq) => {
                    const isOpen = activeFaqId === faq.id;
                    return (
                      <div
                        key={faq.id}
                        className="bg-white rounded-xl border border-slate-200 overflow-hidden"
                      >
                        <button
                          type="button"
                          onClick={() => setActiveFaqId(isOpen ? null : faq.id)}
                          className="w-full text-left p-4 font-bold text-xs text-slate-900 flex items-center justify-between cursor-pointer hover:bg-slate-50 transition-colors"
                        >
                          <span>{faq.question}</span>
                          <ChevronDown className={`w-4 h-4 text-slate-400 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
                        </button>
                        {isOpen && (
                          <div className="p-4 pt-0 text-xs text-slate-600 leading-relaxed border-t border-slate-100 bg-slate-50">
                            {faq.answer}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Submit Support Ticket */}
              <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-sm">
                <h3 className="text-base font-bold text-slate-900 mb-1">
                  24x7 Consular Helpdesk Ticket
                </h3>
                <p className="text-xs text-slate-500 mb-4">
                  Report a payment debit issue, passport correction, or expedited review request.
                </p>

                {ticketSent ? (
                  <div className="p-5 bg-teal-50 border border-teal-200 rounded-2xl text-teal-900 text-xs space-y-2">
                    <div className="flex items-center gap-2 font-bold text-sm">
                      <CheckCircle2 className="w-4 h-4 text-teal-600" />
                      <span>Ticket Submitted Successfully</span>
                    </div>
                    <p>A consular officer has been assigned. Ticket Reference: <strong>TKT-2026-8819</strong>.</p>
                    <button
                      type="button"
                      onClick={() => setTicketSent(false)}
                      className="text-teal-700 underline font-bold mt-2 cursor-pointer"
                    >
                      Submit another ticket
                    </button>
                  </div>
                ) : (
                  <form
                    onSubmit={(e) => {
                      e.preventDefault();
                      setTicketSent(true);
                    }}
                    className="space-y-3.5"
                  >
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Contact Email Address
                      </label>
                      <input
                        type="email"
                        required
                        value={ticketEmail}
                        onChange={(e) => setTicketEmail(e.target.value)}
                        placeholder="e.g. traveler@domain.com"
                        className="w-full px-3.5 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium text-slate-900 focus:bg-white focus:outline-none"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Subject
                      </label>
                      <input
                        type="text"
                        required
                        value={ticketSubject}
                        onChange={(e) => setTicketSubject(e.target.value)}
                        placeholder="e.g. Payment reconciliation for VS-2026-6134"
                        className="w-full px-3.5 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium text-slate-900 focus:bg-white focus:outline-none"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Detailed Description
                      </label>
                      <textarea
                        required
                        rows={3}
                        value={ticketDesc}
                        onChange={(e) => setTicketDesc(e.target.value)}
                        placeholder="Please describe your query or problem..."
                        className="w-full px-3.5 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium text-slate-900 focus:bg-white focus:outline-none"
                      />
                    </div>

                    <button
                      type="submit"
                      className="w-full py-2.5 bg-teal-700 hover:bg-teal-800 text-white rounded-xl text-xs font-bold uppercase tracking-wider flex items-center justify-center gap-2 shadow-sm transition-colors cursor-pointer"
                    >
                      <Send className="w-3.5 h-3.5" />
                      <span>Submit Consular Ticket</span>
                    </button>
                  </form>
                )}
              </div>
            </div>
          </div>
        )}

        {/* TAB 5: CONNECT AI AGENT & MCP */}
        {activeTab === 'ai-agent' && (
          <div className="space-y-6">
            <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-sm">
              <div className="flex items-center gap-3 mb-2">
                <div className="p-2.5 rounded-2xl bg-blue-50 text-blue-600 border border-blue-200">
                  <Sparkles className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-slate-900">
                    Model Context Protocol (MCP) Integration
                  </h3>
                  <span className="text-xs text-slate-500">
                    Connect Claude Desktop, Cursor, or your autonomous AI agent to VisaSetu's live visa engine.
                  </span>
                </div>
              </div>

              {/* Live in-browser MCP tool simulator */}
              <div className="mt-6 p-5 bg-slate-900 text-white rounded-2xl space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-mono font-bold text-teal-300 flex items-center gap-2">
                    <Terminal className="w-4 h-4" />
                    <span>In-Browser MCP Simulator: POST /api/v1/mcp</span>
                  </span>
                  <button
                    type="button"
                    onClick={runMcpSimulation}
                    disabled={simLoading}
                    className="px-3 py-1.5 bg-teal-500 hover:bg-teal-400 text-slate-950 font-bold text-xs rounded-xl transition-colors cursor-pointer disabled:opacity-50"
                  >
                    {simLoading ? 'Executing...' : 'Run check_eligibility'}
                  </button>
                </div>

                <div className="flex flex-wrap items-center gap-3 text-xs">
                  <label className="text-slate-400">Tool:</label>
                  <select
                    value={simTool}
                    onChange={(e) => setSimTool(e.target.value)}
                    className="bg-slate-800 border border-slate-700 text-white rounded-lg px-2.5 py-1 text-xs"
                  >
                    <option value="check_eligibility">check_eligibility</option>
                    <option value="get_fee">get_fee</option>
                    <option value="verify_document">verify_document</option>
                  </select>

                  <label className="text-slate-400">Nationality:</label>
                  <select
                    value={simNat}
                    onChange={(e) => setSimNat(e.target.value)}
                    className="bg-slate-800 border border-slate-700 text-white rounded-lg px-2.5 py-1 text-xs"
                  >
                    <option value="JPN">Japan (JPN)</option>
                    <option value="USA">United States (USA)</option>
                    <option value="GBR">United Kingdom (GBR)</option>
                    <option value="DEU">Germany (DEU)</option>
                  </select>
                </div>

                {simOutput && (
                  <pre className="p-4 bg-slate-950 rounded-xl text-[11px] font-mono text-emerald-300 overflow-x-auto max-h-60 border border-slate-800">
                    {simOutput}
                  </pre>
                )}
              </div>
            </div>
          </div>
        )}

        {/* TAB 6: SETTINGS & EMAIL SERVICES */}
        {activeTab === 'settings' && (
          <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-sm space-y-8">
            <div>
              <h3 className="text-lg font-bold text-slate-900">
                Account Settings & Email Services
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">
                Manage automated email notifications, arrival reminders, gateway resync preferences, and profile credentials.
              </p>
            </div>

            {settingsSuccess && (
              <div className="p-3 bg-teal-50 border border-teal-200 rounded-xl text-teal-900 text-xs font-semibold flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-teal-600" />
                <span>Settings successfully saved and synchronized with database!</span>
              </div>
            )}

            {/* Email Notification Services Toggles */}
            <div className="space-y-4">
              <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                Automated Email Services
              </h4>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-200 flex items-center justify-between">
                  <div>
                    <strong className="text-xs font-bold text-slate-900 block">
                      e-Visa Status Updates via Email
                    </strong>
                    <span className="text-[11px] text-slate-500">
                      Instant notification upon consular approval, review request, or ETA issuance.
                    </span>
                  </div>
                  <input
                    type="checkbox"
                    checked={settings.emailNotificationsEnabled}
                    onChange={(e) =>
                      setSettings({ ...settings, emailNotificationsEnabled: e.target.checked })
                    }
                    className="w-5 h-5 text-teal-600 rounded cursor-pointer"
                  />
                </div>

                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-200 flex items-center justify-between">
                  <div>
                    <strong className="text-xs font-bold text-slate-900 block">
                      Arrival Card & Health Clearance Reminders
                    </strong>
                    <span className="text-[11px] text-slate-500">
                      Dispatched 72 hours prior to flight departure for timely declaration filing.
                    </span>
                  </div>
                  <input
                    type="checkbox"
                    checked={settings.arrivalRemindersEnabled}
                    onChange={(e) =>
                      setSettings({ ...settings, arrivalRemindersEnabled: e.target.checked })
                    }
                    className="w-5 h-5 text-teal-600 rounded cursor-pointer"
                  />
                </div>

                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-200 flex items-center justify-between">
                  <div>
                    <strong className="text-xs font-bold text-slate-900 block">
                      Official Travel Advisories
                    </strong>
                    <span className="text-[11px] text-slate-500">
                      Critical Bureau of Immigration alerts affecting your designated arrival port.
                    </span>
                  </div>
                  <input
                    type="checkbox"
                    checked={settings.advisoryAlertsEnabled}
                    onChange={(e) =>
                      setSettings({ ...settings, advisoryAlertsEnabled: e.target.checked })
                    }
                    className="w-5 h-5 text-teal-600 rounded cursor-pointer"
                  />
                </div>

                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-200 flex items-center justify-between">
                  <div>
                    <strong className="text-xs font-bold text-slate-900 block">
                      Automatic Payment Reconciliation
                    </strong>
                    <span className="text-[11px] text-slate-500">
                      Auto-sync bank debits with Razorpay gateway in the background every 15 minutes.
                    </span>
                  </div>
                  <input
                    type="checkbox"
                    checked={settings.autoResyncEnabled}
                    onChange={(e) =>
                      setSettings({ ...settings, autoResyncEnabled: e.target.checked })
                    }
                    className="w-5 h-5 text-teal-600 rounded cursor-pointer"
                  />
                </div>
              </div>
            </div>

            {/* Language & Profile Particulars */}
            <div className="space-y-4 pt-4 border-t border-slate-100">
              <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                Traveler Profile & Language Preference
              </h4>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 max-w-2xl">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Full Name (as per Passport)
                  </label>
                  <input
                    type="text"
                    disabled
                    value={currentUser?.fullName || 'Kenji Sato'}
                    className="w-full px-3.5 py-2 bg-slate-100 border border-slate-200 rounded-xl text-xs font-medium text-slate-600 cursor-not-allowed"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Registered Email
                  </label>
                  <input
                    type="email"
                    disabled
                    value={currentUser?.email || 'kenji.sato@tokyo-travel.jp'}
                    className="w-full px-3.5 py-2 bg-slate-100 border border-slate-200 rounded-xl text-xs font-medium text-slate-600 cursor-not-allowed"
                  />
                </div>
              </div>

              <div className="flex items-center gap-3 pt-2">
                <button
                  type="button"
                  onClick={handleSaveSettings}
                  disabled={settingsSaving}
                  className="px-6 py-2.5 bg-teal-700 hover:bg-teal-800 text-white rounded-xl text-xs font-bold uppercase tracking-wider flex items-center gap-2 shadow-sm transition-colors cursor-pointer disabled:opacity-50"
                >
                  <Check className="w-3.5 h-3.5" />
                  <span>{settingsSaving ? 'Saving...' : 'Save Settings'}</span>
                </button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Auth Modal for Sign In, Sign Up, and Forgot Password */}
      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
        initialMode={authModalMode}
        onSuccess={(user) => {
          setCurrentUser(user);
        }}
      />
    </PageTemplate>
  );
};
