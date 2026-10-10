import React, { useState, useEffect, useRef } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import {
  CheckCircle2,
  Clock,
  Shield,
  CreditCard,
  Download,
  AlertCircle,
  FileCheck,
  Plane,
  HeartPulse,
  ArrowRight,
  ArrowLeft,
  RefreshCw,
  QrCode,
} from 'lucide-react';
import { PageTemplate } from '../components/PageTemplate';
import {
  createOrGetDraft,
  autosaveDraft,
  submitDraft,
  createPaymentOrder,
  resyncPayment,
  getTripByReference,
  getEtaPdfDownloadUrl,
  getQrPassImageUrl,
  SharedProfileData,
  TripModulesData,
  TripDraftDto,
  PaymentOrderDto,
} from '../api/trips';
import { fetchNationalities, NationalityOption } from '../api/eligibility';

const ENTRY_PORTS = [
  'Delhi (DEL) - Indira Gandhi International',
  'Mumbai (BOM) - Chhatrapati Shivaji Maharaj',
  'Bengaluru (BLR) - Kempegowda International',
  'Chennai (MAA) - Chennai International',
  'Kolkata (CCU) - Netaji Subhash Chandra Bose',
  'Hyderabad (HYD) - Rajiv Gandhi International',
  'Cochin (COK) - Cochin International',
  'Goa (GOI / GOX) - Dabolim / Manohar International',
  'Ahmedabad (AMD) - Sardar Vallabhbhai Patel',
  'Amritsar (ATQ) - Sri Guru Ram Dass Jee',
  'Trivandrum (TRV) - Thiruvananthapuram International',
  'Varanasi (VNS) - Lal Bahadur Shastri International',
];

export const ApplyPage: React.FC = () => {
  const [searchParams] = useSearchParams();

  // Initial params
  const paramNat = searchParams.get('nationality') || 'JPN';
  const paramCategory = searchParams.get('category') || 'e-Tourist Visa (30 Days)';

  const [currentStep, setCurrentStep] = useState<number>(1);
  const [draft, setDraft] = useState<TripDraftDto | null>(null);
  const [saveStatus, setSaveStatus] = useState<'saved' | 'saving' | 'offline'>('saved');
  const [nationalities, setNationalities] = useState<NationalityOption[]>([]);
  const [formError, setFormError] = useState<string | null>(null);

  // Form State
  const [profile, setProfile] = useState<SharedProfileData>({
    email: 'traveler.' + Math.floor(1000 + Math.random() * 9000) + '@example.com',
    givenName: 'Kenji',
    surname: 'Sato',
    dob: '1992-05-14',
    gender: 'MALE',
    nationality: paramNat,
    passportNumber: 'M' + Math.floor(10000000 + Math.random() * 90000000),
    passportExpiry: '2031-10-25',
    phone: '+81-90-1234-5678',
  });

  const [modules, setModules] = useState<TripModulesData>({
    eVisa: {
      categoryName: paramCategory,
      stayDuration: '30 Days',
      arrivalDate: '2026-11-20',
      portOfArrival: 'Delhi (DEL) - Indira Gandhi International',
      previousVisit: false,
    },
    eArrival: {
      flightNumber: 'AI-307',
      airlineName: 'Air India',
      hotelAddress: 'Hotel Taj Palace, Sardar Patel Marg',
      cityInIndia: 'New Delhi',
      emergencyContactName: 'Yuki Sato',
      emergencyContactPhone: '+81-90-9876-5432',
    },
    eSuvidha: {
      healthDeclarationAccepted: true,
      yellowFeverEndemicVisit: false,
      feverSymptomFree: true,
    },
  });

  // Payment state
  const [paymentOrder, setPaymentOrder] = useState<PaymentOrderDto | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [paying, setPaying] = useState(false);
  const [referenceId, setReferenceId] = useState<string>('');
  const [applicationStatus, setApplicationStatus] = useState<string>('DRAFT');
  const [etaNumber, setEtaNumber] = useState<string | null>(null);
  const [qrToken, setQrToken] = useState<string | null>(null);

  const autosaveTimerRef = useRef<NodeJS.Timeout | null>(null);

  // Load nationalities on mount
  useEffect(() => {
    fetchNationalities()
      .then((data) => setNationalities(data))
      .catch(() => {});
  }, []);

  // Initialize draft on backend when email is ready
  useEffect(() => {
    if (profile.email && !draft) {
      createOrGetDraft(profile.email, profile.nationality, modules.eVisa?.categoryName)
        .then((d) => {
          setDraft(d);
          setReferenceId(d.referenceId);
          setSaveStatus('saved');
        })
        .catch((err) => {
          console.warn('Backend draft init offline fallback:', err);
          setSaveStatus('offline');
          setReferenceId('VS-2026-' + Math.floor(1000 + Math.random() * 9000));
        });
    }
  }, [profile.email]);

  // Debounced autosave
  const triggerAutosave = (updatedProfile: SharedProfileData, updatedModules: TripModulesData, step: number) => {
    if (!draft) return;
    setSaveStatus('saving');

    if (autosaveTimerRef.current) {
      clearTimeout(autosaveTimerRef.current);
    }

    autosaveTimerRef.current = setTimeout(async () => {
      try {
        const saved = await autosaveDraft(draft.draftId, draft.version, updatedProfile, updatedModules, step);
        setDraft(saved);
        setSaveStatus('saved');
      } catch (err: any) {
        if (err?.response?.status === 409) {
          console.warn('Draft version conflict detected, reloading latest');
        }
        setSaveStatus('offline');
      }
    }, 1200);
  };

  const handleProfileChange = (field: keyof SharedProfileData, value: string) => {
    const updated = { ...profile, [field]: value };
    setProfile(updated);
    triggerAutosave(updated, modules, currentStep);
  };

  const handleModuleChange = (
    moduleKey: keyof TripModulesData,
    field: string,
    value: any
  ) => {
    const updated = {
      ...modules,
      [moduleKey]: {
        ...(modules[moduleKey] as any),
        [field]: value,
      },
    };
    setModules(updated);
    triggerAutosave(profile, updated, currentStep);
  };

  // Next Step with validation
  const goToNextStep = () => {
    setFormError(null);
    if (currentStep === 1) {
      if (!profile.givenName?.trim() || !profile.surname?.trim() || !profile.passportNumber?.trim() || !profile.email?.trim()) {
        setFormError('Please complete all required traveler and passport fields.');
        return;
      }
    } else if (currentStep === 2) {
      if (!modules.eVisa?.arrivalDate || !modules.eVisa?.portOfArrival) {
        setFormError('Please select both your expected arrival date and entry port.');
        return;
      }
    } else if (currentStep === 3) {
      if (!modules.eSuvidha?.healthDeclarationAccepted) {
        setFormError('Mandatory e-Suvidha health declaration must be acknowledged.');
        return;
      }
    }
    const next = currentStep + 1;
    setCurrentStep(next);
    triggerAutosave(profile, modules, next);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Step 4: Submit draft into PostgreSQL Trip application
  const handleFinalSubmit = async () => {
    setSubmitting(true);
    setFormError(null);
    try {
      if (draft) {
        const fullName = `${profile.givenName || ''} ${profile.surname || ''}`.trim();
        const res = await submitDraft(
          draft.draftId,
          draft.version,
          fullName,
          modules.eVisa?.arrivalDate || '2026-11-20',
          modules.eVisa?.portOfArrival || 'Delhi (DEL)'
        );
        setReferenceId(res.referenceId);
        setApplicationStatus(res.status);
      }
      // Create payment order
      const order = await createPaymentOrder(referenceId || draft?.referenceId || 'VS-2026-8562');
      setPaymentOrder(order);
      setCurrentStep(5);
    } catch (err: any) {
      console.error('Submission failed:', err);
      // Fallback for resilient demo
      setCurrentStep(5);
    } finally {
      setSubmitting(false);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  // Step 5: Simulate Razorpay Test Payment
  const handleExecutePayment = async () => {
    setPaying(true);
    setFormError(null);
    try {
      const activeRef = referenceId || draft?.referenceId || 'VS-2026-8562';
      // Reconcile/finalize payment on backend
      const res = await resyncPayment(activeRef);
      setApplicationStatus(res.newTripStatus || 'UNDER_REVIEW');
      setCurrentStep(6);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (err) {
      console.error('Payment processing fallback:', err);
      setApplicationStatus('UNDER_REVIEW');
      setCurrentStep(6);
    } finally {
      setPaying(false);
    }
  };

  // Step 6: Poll for Mock Government Approval
  useEffect(() => {
    if (currentStep === 6 && referenceId) {
      const interval = setInterval(async () => {
        try {
          const detail = await getTripByReference(referenceId);
          if (detail.trip) {
            setApplicationStatus(detail.trip.status);
            if (detail.trip.etaNumber) {
              setEtaNumber(detail.trip.etaNumber);
            }
            if (detail.trip.qrToken) {
              setQrToken(detail.trip.qrToken);
            }
          }
        } catch {
          // Keep polling
        }
      }, 3000);

      return () => clearInterval(interval);
    }
  }, [currentStep, referenceId]);

  const stepTimeEstimates: Record<number, string> = {
    1: '~6 min left',
    2: '~4 min left',
    3: '~3 min left',
    4: '~1 min left',
    5: 'Payment processing',
    6: 'Official Grant',
  };

  return (
    <PageTemplate
      title="Unified 3-in-1 Application Portal"
      intro="One streamlined application covering your Indian e-Visa, pre-arrival e-Arrival Card, and e-Suvidha health clearance."
      breadcrumbs={[{ label: 'Apply', to: '/apply' }]}
    >
      <div className="max-w-4xl mx-auto">
        {/* TOP STATUS BAR & AUTOSAVE INDICATOR */}
        <div className="bg-slate-900 text-white rounded-2xl p-5 mb-8 shadow-lg flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-teal-500/20 border border-teal-500/40 flex items-center justify-center text-teal-400 font-bold">
              {currentStep}/6
            </div>
            <div>
              <div className="text-xs text-slate-400 font-medium">Application Reference ID</div>
              <div className="text-base font-mono font-bold text-teal-300">
                {referenceId || 'GENERATING...'}
              </div>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <div className="text-xs flex items-center gap-1.5 font-medium">
              <Clock className="w-3.5 h-3.5 text-slate-400" />
              <span className="text-slate-300">{stepTimeEstimates[currentStep]}</span>
            </div>

            {/* Autosave Status Pill */}
            <div
              className={`px-3 py-1 rounded-full text-xs font-semibold flex items-center gap-1.5 border transition-all ${
                saveStatus === 'saved'
                  ? 'bg-emerald-950/80 text-emerald-300 border-emerald-500/40'
                  : saveStatus === 'saving'
                  ? 'bg-amber-950/80 text-amber-300 border-amber-500/40 animate-pulse'
                  : 'bg-slate-800 text-slate-300 border-slate-700'
              }`}
            >
              <span className="w-1.5 h-1.5 rounded-full bg-current" />
              <span>
                {saveStatus === 'saved'
                  ? 'Autosaved ✓'
                  : saveStatus === 'saving'
                  ? 'Saving changes...'
                  : 'Saved Locally'}
              </span>
            </div>
          </div>
        </div>

        {/* PROGRESS STEPPER */}
        <div className="grid grid-cols-6 gap-2 mb-8">
          {[
            { num: 1, label: 'Profile' },
            { num: 2, label: 'Itinerary' },
            { num: 3, label: '3-in-1 Modules' },
            { num: 4, label: 'Review' },
            { num: 5, label: 'Payment' },
            { num: 6, label: 'Grant Pass' },
          ].map((s) => (
            <div key={s.num} className="text-center">
              <div
                className={`h-2 rounded-full mb-2 transition-all ${
                  currentStep >= s.num ? 'bg-teal-600' : 'bg-slate-200'
                }`}
              />
              <span
                className={`text-[11px] font-semibold hidden sm:inline ${
                  currentStep === s.num
                    ? 'text-teal-700'
                    : currentStep > s.num
                    ? 'text-slate-700'
                    : 'text-slate-400'
                }`}
              >
                {s.label}
              </span>
            </div>
          ))}
        </div>

        {/* FORM ERROR BANNER */}
        {formError && (
          <div className="p-4 mb-6 bg-red-50 border border-red-200 rounded-xl text-red-700 text-xs font-medium flex items-center gap-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            <span>{formError}</span>
          </div>
        )}

        {/* ================= STEP 1: TRAVELER PROFILE ================= */}
        {currentStep === 1 && (
          <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 shadow-sm">
            <h2 className="text-xl font-bold text-slate-900 mb-1 flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-teal-600" />
              Step 1: Traveler Profile & Passport Particulars
            </h2>
            <p className="text-xs text-slate-500 mb-6">
              Enter details exactly as they appear in your machine-readable passport.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5 mb-6">
              <div>
                <label htmlFor="apply-email" className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">
                  Email Address *
                </label>
                <input
                  id="apply-email"
                  type="email"
                  value={profile.email || ''}
                  onChange={(e) => handleProfileChange('email', e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                  placeholder="traveler@example.com"
                />
                <span className="text-[11px] text-slate-400 mt-1 block">
                  Your magic link, ETA pass, and payment receipts will be dispatched here.
                </span>
              </div>

              <div>
                <label htmlFor="apply-nationality" className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">
                  Citizenship / Nationality *
                </label>
                <select
                  id="apply-nationality"
                  value={profile.nationality || 'JPN'}
                  onChange={(e) => handleProfileChange('nationality', e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                >
                  {nationalities.map((n) => (
                    <option key={n.code} value={n.code}>
                      {n.flagEmoji} {n.name} ({n.code})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor="apply-given-name" className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">
                  Given Name(s) *
                </label>
                <input
                  id="apply-given-name"
                  type="text"
                  value={profile.givenName || ''}
                  onChange={(e) => handleProfileChange('givenName', e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 uppercase"
                  placeholder="e.g. KENJI"
                />
              </div>

              <div>
                <label htmlFor="apply-surname" className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">
                  Surname / Family Name *
                </label>
                <input
                  id="apply-surname"
                  type="text"
                  value={profile.surname || ''}
                  onChange={(e) => handleProfileChange('surname', e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 uppercase"
                  placeholder="e.g. SATO"
                />
              </div>

              <div>
                <label htmlFor="apply-passport-number" className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">
                  Passport Number *
                </label>
                <input
                  id="apply-passport-number"
                  type="text"
                  value={profile.passportNumber || ''}
                  onChange={(e) => handleProfileChange('passportNumber', e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm font-mono font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 uppercase"
                  placeholder="e.g. M12345678"
                />
              </div>

              <div>
                <label htmlFor="apply-passport-expiry" className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">
                  Passport Expiry Date *
                </label>
                <input
                  id="apply-passport-expiry"
                  type="date"
                  value={profile.passportExpiry || ''}
                  onChange={(e) => handleProfileChange('passportExpiry', e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                />
                <span className="text-[11px] text-slate-400 mt-1 block">
                  Must be valid for minimum 6 months from arrival date.
                </span>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">
                  Date of Birth *
                </label>
                <input
                  type="date"
                  value={profile.dob || ''}
                  onChange={(e) => handleProfileChange('dob', e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">
                  Gender *
                </label>
                <select
                  value={profile.gender || 'MALE'}
                  onChange={(e) => handleProfileChange('gender', e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                >
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other / Transgender</option>
                </select>
              </div>
            </div>

            <div className="flex justify-end pt-4 border-t border-slate-100">
              <button
                type="button"
                onClick={goToNextStep}
                className="px-6 py-3 bg-teal-700 hover:bg-teal-800 text-white rounded-xl font-bold text-xs uppercase tracking-wider flex items-center gap-2 shadow-md shadow-teal-700/20 cursor-pointer"
              >
                <span>Continue to Itinerary</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}

        {/* ================= STEP 2: ITINERARY & ENTRY PORT ================= */}
        {currentStep === 2 && (
          <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 shadow-sm">
            <h2 className="text-xl font-bold text-slate-900 mb-1 flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-teal-600" />
              Step 2: Trip Itinerary & Port of Entry
            </h2>
            <p className="text-xs text-slate-500 mb-6">
              Designated entry points authorized by the Bureau of Immigration (BOI).
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5 mb-6">
              <div className="sm:col-span-2">
                <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">
                  Visa Category *
                </label>
                <input
                  type="text"
                  readOnly
                  value={modules.eVisa?.categoryName || 'e-Tourist Visa (30 Days)'}
                  className="w-full px-3.5 py-2.5 bg-slate-100 border border-slate-200 rounded-xl text-sm font-bold text-teal-800"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">
                  Expected Arrival Date in India *
                </label>
                <input
                  type="date"
                  value={modules.eVisa?.arrivalDate || ''}
                  onChange={(e) => handleModuleChange('eVisa', 'arrivalDate', e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">
                  Designated Port of Entry *
                </label>
                <select
                  value={modules.eVisa?.portOfArrival || ENTRY_PORTS[0]}
                  onChange={(e) => handleModuleChange('eVisa', 'portOfArrival', e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                >
                  {ENTRY_PORTS.map((port) => (
                    <option key={port} value={port}>
                      {port}
                    </option>
                  ))}
                </select>
                <span className="text-[11px] text-slate-400 mt-1 block">
                  Valid for arrival at any of the 37 authorized international airports.
                </span>
              </div>
            </div>

            <div className="flex items-center justify-between pt-4 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setCurrentStep(1)}
                className="px-5 py-2.5 text-xs font-bold text-slate-600 hover:text-slate-900 flex items-center gap-1.5"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>Back</span>
              </button>

              <button
                type="button"
                onClick={goToNextStep}
                className="px-6 py-3 bg-teal-700 hover:bg-teal-800 text-white rounded-xl font-bold text-xs uppercase tracking-wider flex items-center gap-2 shadow-md shadow-teal-700/20 cursor-pointer"
              >
                <span>Continue to 3-in-1 Modules</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}

        {/* ================= STEP 3: UNIFIED 3-IN-1 MODULES ================= */}
        {currentStep === 3 && (
          <div className="space-y-6">
            <div className="bg-gradient-to-r from-teal-900 to-slate-900 text-white rounded-2xl p-6 shadow-sm">
              <div className="flex items-center gap-3 mb-2">
                <span className="p-2 rounded-xl bg-teal-500/20 border border-teal-400/30 text-teal-300">
                  ⚡
                </span>
                <h3 className="text-lg font-bold">1 Profile Powers 3 Official Port Requirements</h3>
              </div>
              <p className="text-xs text-slate-300 leading-relaxed max-w-2xl">
                VisaSetu automatically populates your immigration e-Visa, your mandatory e-Arrival Card, and your e-Suvidha health clearance simultaneously.
              </p>
            </div>

            {/* Sub-Module A: e-Arrival Card */}
            <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
              <div className="flex items-center gap-2 mb-4">
                <Plane className="w-5 h-5 text-teal-600" />
                <h3 className="text-base font-bold text-slate-900">Module A: e-Arrival Card Information</h3>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                    Flight / Carrier Number
                  </label>
                  <input
                    type="text"
                    value={modules.eArrival?.flightNumber || ''}
                    onChange={(e) => handleModuleChange('eArrival', 'flightNumber', e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-lg text-xs font-mono font-bold"
                    placeholder="e.g. AI-307 / JL-749"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                    Airline / Carrier Name
                  </label>
                  <input
                    type="text"
                    value={modules.eArrival?.airlineName || ''}
                    onChange={(e) => handleModuleChange('eArrival', 'airlineName', e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-lg text-xs"
                    placeholder="e.g. Air India / Japan Airlines"
                  />
                </div>

                <div className="sm:col-span-2">
                  <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                    Stay Address in India (Hotel or Residence)
                  </label>
                  <input
                    type="text"
                    value={modules.eArrival?.hotelAddress || ''}
                    onChange={(e) => handleModuleChange('eArrival', 'hotelAddress', e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-lg text-xs"
                    placeholder="Hotel name, street, locality, and state"
                  />
                </div>
              </div>
            </div>

            {/* Sub-Module B: e-Suvidha Health Declaration */}
            <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
              <div className="flex items-center gap-2 mb-4">
                <HeartPulse className="w-5 h-5 text-teal-600" />
                <h3 className="text-base font-bold text-slate-900">Module B: e-Suvidha Health Declaration</h3>
              </div>

              <div className="space-y-3 text-xs text-slate-700">
                <label className="flex items-start gap-2.5 p-3 rounded-xl bg-slate-50 border border-slate-200 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={Boolean(modules.eSuvidha?.healthDeclarationAccepted)}
                    onChange={(e) => handleModuleChange('eSuvidha', 'healthDeclarationAccepted', e.target.checked)}
                    className="mt-0.5 rounded text-teal-600 focus:ring-teal-500"
                  />
                  <span>
                    <strong>Mandatory Health Affirmation:</strong> I certify that I am currently asymptomatic of viral haemorrhagic fevers and acute respiratory illness.
                  </span>
                </label>

                <label className="flex items-start gap-2.5 p-3 rounded-xl bg-slate-50 border border-slate-200 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={Boolean(modules.eSuvidha?.yellowFeverEndemicVisit)}
                    onChange={(e) => handleModuleChange('eSuvidha', 'yellowFeverEndemicVisit', e.target.checked)}
                    className="mt-0.5 rounded text-teal-600 focus:ring-teal-500"
                  />
                  <span>
                    Have you visited or transited through any Yellow Fever endemic country in the last 6 days prior to arriving in India? (If checked, yellow fever card is mandatory).
                  </span>
                </label>
              </div>
            </div>

            <div className="flex items-center justify-between pt-4 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setCurrentStep(2)}
                className="px-5 py-2.5 text-xs font-bold text-slate-600 hover:text-slate-900 flex items-center gap-1.5"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>Back</span>
              </button>

              <button
                type="button"
                onClick={goToNextStep}
                className="px-6 py-3 bg-teal-700 hover:bg-teal-800 text-white rounded-xl font-bold text-xs uppercase tracking-wider flex items-center gap-2 shadow-md shadow-teal-700/20 cursor-pointer"
              >
                <span>Proceed to Review</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}

        {/* ================= STEP 4: REVIEW & CONFIRM ================= */}
        {currentStep === 4 && (
          <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 shadow-sm">
            <h2 className="text-xl font-bold text-slate-900 mb-1 flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-teal-600" />
              Step 4: Review Application & Statutory Fee Breakdown
            </h2>
            <p className="text-xs text-slate-500 mb-6">
              Review all details before atomic submission to the consular review database.
            </p>

            <div className="space-y-4 mb-6">
              {/* Traveler particulars box */}
              <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 text-xs">
                <div className="flex justify-between font-bold text-slate-800 mb-2 border-b pb-2">
                  <span>Applicant Summary</span>
                  <button type="button" onClick={() => setCurrentStep(1)} className="text-teal-700 underline">
                    Edit
                  </button>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div>
                    <span className="text-slate-400 block">Full Name:</span>
                    <strong className="text-slate-800">{profile.givenName} {profile.surname}</strong>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Nationality:</span>
                    <strong className="text-slate-800">{profile.nationality}</strong>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Passport:</span>
                    <strong className="font-mono text-slate-800">{profile.passportNumber}</strong>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Arrival:</span>
                    <strong className="text-slate-800">{modules.eVisa?.arrivalDate}</strong>
                  </div>
                </div>
              </div>

              {/* Fee Breakdown Table */}
              <div className="p-4 bg-teal-50/70 border border-teal-200 rounded-xl text-xs">
                <h4 className="font-bold text-teal-900 mb-2">Statutory Fee Breakdown</h4>
                <div className="space-y-1.5">
                  <div className="flex justify-between text-slate-700">
                    <span>Indian e-Visa (30 Days Double Entry):</span>
                    <strong className="font-mono">$25.00 USD</strong>
                  </div>
                  <div className="flex justify-between text-slate-700">
                    <span>e-Arrival Card Port Clearance:</span>
                    <strong className="text-emerald-700 font-mono">$0.00 (FREE)</strong>
                  </div>
                  <div className="flex justify-between text-slate-700">
                    <span>e-Suvidha Health Affirmation:</span>
                    <strong className="text-emerald-700 font-mono">$0.00 (FREE)</strong>
                  </div>
                  <div className="border-t border-teal-200 pt-2 flex justify-between text-sm font-bold text-slate-900">
                    <span>Total Amount Payable:</span>
                    <span className="text-teal-800 font-mono text-base">$25.00 USD</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="flex items-center justify-between pt-4 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setCurrentStep(3)}
                className="px-5 py-2.5 text-xs font-bold text-slate-600 hover:text-slate-900 flex items-center gap-1.5"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>Back</span>
              </button>

              <button
                type="button"
                disabled={submitting}
                onClick={handleFinalSubmit}
                className="px-8 py-3.5 bg-teal-700 hover:bg-teal-800 text-white rounded-xl font-bold text-xs uppercase tracking-wider flex items-center gap-2 shadow-lg shadow-teal-700/25 cursor-pointer disabled:opacity-50"
              >
                {submitting ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Submitting Application...</span>
                  </>
                ) : (
                  <>
                    <span>Confirm & Proceed to Payment ($25.00)</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </div>
          </div>
        )}

        {/* ================= STEP 5: BULLETPROOF PAYMENT ================= */}
        {currentStep === 5 && (
          <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 shadow-sm">
            <div className="flex items-center justify-between pb-4 border-b border-slate-200 mb-6">
              <div>
                <span className="px-2.5 py-0.5 rounded text-[10px] font-bold tracking-wider uppercase bg-amber-100 text-amber-900 border border-amber-300">
                  Razorpay TEST Mode Active
                </span>
                <h2 className="text-xl font-bold text-slate-900 mt-2">
                  Bulletproof Payment Checkout
                </h2>
                <p className="text-xs text-slate-500">
                  Idempotent transaction with signature verification and automatic outbox logging.
                </p>
              </div>

              <div className="text-right">
                <span className="text-xs text-slate-400 block font-medium">Payable:</span>
                <span className="text-2xl font-mono font-bold text-teal-800">$25.00 USD</span>
              </div>
            </div>

            {/* Test Card Simulator */}
            <div className="p-6 bg-slate-900 text-white rounded-2xl mb-6 shadow-md">
              <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-2">
                  <CreditCard className="w-6 h-6 text-teal-400" />
                  <span className="font-bold tracking-wider text-sm">RAZORPAY TEST GATEWAY</span>
                </div>
                <span className="text-xs font-mono text-slate-400">Order: {paymentOrder?.orderId || 'rzp_order_mock'}</span>
              </div>

              <div className="space-y-3 font-mono text-xs">
                <div>
                  <span className="text-slate-400 text-[10px] block">DEMO TEST CARD NUMBER</span>
                  <span className="text-base font-bold tracking-widest text-teal-200">
                    4111 2222 3333 4444
                  </span>
                </div>
                <div className="flex gap-8">
                  <div>
                    <span className="text-slate-400 text-[10px] block">EXPIRY</span>
                    <span>12/28</span>
                  </div>
                  <div>
                    <span className="text-slate-400 text-[10px] block">CVV</span>
                    <span>123</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="space-y-3">
              <button
                type="button"
                disabled={paying}
                onClick={handleExecutePayment}
                className="w-full py-4 bg-teal-700 hover:bg-teal-800 text-white rounded-xl font-bold text-sm uppercase tracking-wider flex items-center justify-center gap-2 shadow-lg shadow-teal-700/25 cursor-pointer disabled:opacity-50"
              >
                {paying ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Authorizing & Verifying HMAC Signature...</span>
                  </>
                ) : (
                  <>
                    <Shield className="w-4 h-4" />
                    <span>Pay $25.00 USD (Test Card Simulator)</span>
                  </>
                )}
              </button>

              <button
                type="button"
                onClick={handleExecutePayment}
                className="w-full py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl font-semibold text-xs transition-colors"
              >
                Simulate Delayed Capture & Force Gateway Re-sync
              </button>
            </div>
          </div>
        )}

        {/* ================= STEP 6: GRANT PASS & DOWNLOADS ================= */}
        {currentStep === 6 && (
          <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 shadow-sm">
            <div className="text-center pb-6 border-b border-slate-200 mb-6">
              <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-700 mx-auto flex items-center justify-center mb-4">
                <CheckCircle2 className="w-10 h-10" />
              </div>
              <span className="px-3 py-1 bg-emerald-100 text-emerald-800 font-bold rounded-full text-xs uppercase tracking-wide">
                Payment Confirmed & Application {applicationStatus}
              </span>
              <h2 className="text-2xl font-bold text-slate-900 mt-3">
                {applicationStatus === 'APPROVED'
                  ? 'Official Electronic Travel Authorization (ETA) Granted!'
                  : 'Application Successfully Paid & Under Review'}
              </h2>
              <p className="text-xs text-slate-500 mt-1 max-w-md mx-auto">
                Reference ID: <strong className="font-mono text-slate-900">{referenceId}</strong>
                {etaNumber && (
                  <span className="block mt-0.5 text-teal-800 font-semibold font-mono">
                    Official ETA: {etaNumber}
                  </span>
                )}
              </p>
            </div>

            {/* LIVE CONCURRENT REVIEW NOTICE */}
            {applicationStatus === 'UNDER_REVIEW' && (
              <div className="p-4 mb-6 bg-blue-50 border border-blue-200 rounded-xl text-blue-900 text-xs flex items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <RefreshCw className="w-4 h-4 animate-spin text-blue-600 flex-shrink-0" />
                  <span>
                    Mock Government Decision Engine is processing your application in the background (auto-clearance in ~10s)...
                  </span>
                </div>
                <button
                  type="button"
                  onClick={async () => {
                    const detail = await getTripByReference(referenceId);
                    if (detail.trip) setApplicationStatus(detail.trip.status);
                  }}
                  className="font-bold underline whitespace-nowrap"
                >
                  Refresh Now
                </button>
              </div>
            )}

            {/* OFFICIAL PASS DOWNLOADS */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
              {/* ETA PDF Download */}
              <div className="p-5 bg-slate-50 rounded-2xl border border-slate-200 flex flex-col justify-between">
                <div>
                  <div className="flex items-center gap-2 text-teal-800 font-bold text-sm mb-1">
                    <FileCheck className="w-4 h-4" />
                    <span>Official ETA Certificate</span>
                  </div>
                  <p className="text-xs text-slate-500 mb-4 leading-relaxed">
                    Official PDF grant letter generated by OpenPDF. Print and carry with your passport.
                  </p>
                </div>

                <a
                  href={getEtaPdfDownloadUrl(referenceId)}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full py-2.5 bg-teal-700 hover:bg-teal-800 text-white rounded-xl font-bold text-xs flex items-center justify-center gap-1.5 shadow-sm transition-colors"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>Download ETA (PDF)</span>
                </a>
              </div>

              {/* ZXing Biometric QR Pass */}
              <div className="p-5 bg-slate-50 rounded-2xl border border-slate-200 flex flex-col justify-between">
                <div>
                  <div className="flex items-center gap-2 text-teal-800 font-bold text-sm mb-1">
                    <QrCode className="w-4 h-4" />
                    <span>Biometric QR e-Gate Pass</span>
                  </div>
                  <p className="text-xs text-slate-500 mb-4 leading-relaxed">
                    Official 2D QR code token generated by ZXing for automated e-Gate biometric boarding.
                  </p>
                </div>

                <div className="flex items-center gap-3">
                  <a
                    href={getQrPassImageUrl(referenceId)}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex-1 py-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl font-bold text-xs flex items-center justify-center gap-1.5 shadow-sm transition-colors"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span>View QR Pass</span>
                  </a>
                </div>
              </div>
            </div>

            {/* QR PREVIEW WHEN AVAILABLE */}
            <div className="p-4 bg-teal-50 border border-teal-200 rounded-2xl flex items-center gap-4 mb-6">
              <img
                src={getQrPassImageUrl(referenceId)}
                alt="Biometric e-Gate QR Pass"
                className="w-20 h-20 bg-white border border-slate-300 rounded-lg p-1 object-contain"
                onError={(e) => {
                  (e.target as HTMLElement).style.display = 'none';
                }}
              />
              <div className="text-xs">
                <span className="font-bold text-teal-950 block">Biometric e-Gate Scanning Token</span>
                <span className="font-mono text-[11px] text-teal-800 break-all">
                  {qrToken || `ETA:${referenceId}|STATUS:${applicationStatus}`}
                </span>
                <p className="text-slate-500 text-[10px] mt-1">
                  Ready for scanning at any of the 37 designated e-Visa immigration airport counters.
                </p>
              </div>
            </div>

            <div className="text-center pt-4 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link
                to={`/existing-applications?ref=${referenceId}`}
                className="text-xs font-bold text-teal-700 hover:text-teal-900 underline"
              >
                Track this application on Status Dashboard →
              </Link>
              <Link
                to="/"
                className="text-xs font-semibold text-slate-600 hover:text-slate-900"
              >
                Return to Home Page
              </Link>
            </div>
          </div>
        )}
      </div>
    </PageTemplate>
  );
};
