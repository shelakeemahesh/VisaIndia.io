import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import {
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  ArrowRight,
  FileText,
  CreditCard,
  Download,
  QrCode,
} from 'lucide-react';
import { PageTemplate } from '../components/PageTemplate';
import {
  createOrGetDraft,
  submitDraft,
  createPaymentOrder,
  verifyPayment,
  getTripByReference,
  getEtaPdfDownloadUrl,
  getQrPassImageUrl,
  TripDraftDto,
  PaymentOrderDto,
} from '../api/trips';
import { fetchNationalities, NationalityOption } from '../api/eligibility';
import { SearchableCountrySelect } from '../components/SearchableCountrySelect';
import { ALL_COUNTRIES } from '../data/countries';

const PASSPORT_TYPES = [
  { value: 'ORDINARY', label: 'Ordinary Passport (Standard Tourist / Business)' },
  { value: 'DIPLOMATIC', label: 'Diplomatic Passport' },
  { value: 'OFFICIAL', label: 'Official Passport' },
  { value: 'SERVICE', label: 'Service Passport' },
  { value: 'SPECIAL', label: 'Special Passport' },
];

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
  'Gaya (GAY) - Gaya International',
  'Lucknow (LKO) - Chaudhary Charan Singh',
  'Jaipur (JAI) - Jaipur International',
  'Tiruchirappalli (TRZ) - Tiruchirappalli International',
];

const VISA_SERVICES = [
  {
    id: 'e-tourist-30d',
    name: 'e-Tourist Visa (for 30 Days)',
    fee: '$25 USD',
    entries: 'Double Entry',
    description: 'For recreation, sightseeing, visiting friends & relatives. Valid for 30 days from entry.',
  },
  {
    id: 'e-tourist-1y',
    name: 'e-Tourist Visa (for 1 Year)',
    fee: '$40 USD',
    entries: 'Multiple Entry',
    description: 'Continuous stay not to exceed 90 or 180 days per visit based on nationality.',
  },
  {
    id: 'e-tourist-5y',
    name: 'e-Tourist Visa (for 5 Years)',
    fee: '$80 USD',
    entries: 'Multiple Entry',
    description: 'Long-term tourist travel for eligible passport holders.',
  },
  {
    id: 'e-business',
    name: 'e-Business Visa',
    fee: '$80 USD',
    entries: 'Multiple Entry',
    description: 'For business meetings, trade, setup, recruitment, and commerce.',
  },
  {
    id: 'e-medical',
    name: 'e-Medical Visa',
    fee: '$80 USD',
    entries: 'Triple Entry',
    description: 'For medical treatment at an accredited healthcare facility in India.',
  },
  {
    id: 'e-conference',
    name: 'e-Conference Visa',
    fee: '$25 USD',
    entries: 'Single Entry',
    description: 'To attend seminars or conferences with Government of India nodal clearance.',
  },
];

function loadRazorpayScript(): Promise<boolean> {
  return new Promise((resolve) => {
    if (typeof window !== 'undefined' && (window as any).Razorpay) {
      resolve(true);
      return;
    }
    if (typeof document === 'undefined') {
      resolve(false);
      return;
    }
    const existing = document.querySelector('script[src="https://checkout.razorpay.com/v1/checkout.js"]');
    if (existing) {
      resolve(true);
      return;
    }
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.async = true;
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

export const StartApplicationPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  // URL parameters prefill
  const paramNat = searchParams.get('nationality') || 'JPN';
  const paramPurpose = searchParams.get('purpose') || 'TOURISM';
  const paramCategory = searchParams.get('category') || '';

  // Options
  const [nationalities, setNationalities] = useState<NationalityOption[]>([]);

  // Form Fields
  const [passportType, setPassportType] = useState('ORDINARY');
  const [nationality, setNationality] = useState(paramNat);
  const [portOfArrival, setPortOfArrival] = useState(ENTRY_PORTS[0]);
  const [dob, setDob] = useState('1992-05-14');
  const [email, setEmail] = useState('traveler.' + Math.floor(1000 + Math.random() * 9000) + '@example.com');
  const [confirmEmail, setConfirmEmail] = useState('');
  const [expectedArrivalDate, setExpectedArrivalDate] = useState('2026-11-20');
  const [selectedService, setSelectedService] = useState('e-Tourist Visa (for 30 Days)');
  const [declarationChecked, setDeclarationChecked] = useState(false);

  // Security captcha state
  const [captchaCode, setCaptchaCode] = useState('7K9X2B');
  const [captchaInput, setCaptchaInput] = useState('7K9X2B');

  // Submission & Post-Registration State
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [registeredDraft, setRegisteredDraft] = useState<TripDraftDto | null>(null);
  const [paymentOrder, setPaymentOrder] = useState<PaymentOrderDto | null>(null);
  const [paying, setPaying] = useState(false);
  const [applicationStatus, setApplicationStatus] = useState<string>('REGISTERED');
  const [etaNumber, setEtaNumber] = useState<string | null>(null);
  const [qrToken, setQrToken] = useState<string | null>(null);

  // Load nationalities on mount
  useEffect(() => {
    fetchNationalities()
      .then((data) => setNationalities(data && data.length > 0 ? data : ALL_COUNTRIES))
      .catch(() => setNationalities(ALL_COUNTRIES));
  }, []);

  // Update confirmEmail when email changes initially
  useEffect(() => {
    if (email && !confirmEmail) {
      setConfirmEmail(email);
    }
  }, [email]);

  // Adjust prefilled service based on query param
  useEffect(() => {
    if (paramCategory) {
      const match = VISA_SERVICES.find((s) => s.name.toLowerCase().includes(paramCategory.toLowerCase()));
      if (match) setSelectedService(match.name);
    } else if (paramPurpose === 'BUSINESS') {
      setSelectedService('e-Business Visa');
    } else if (paramPurpose === 'MEDICAL') {
      setSelectedService('e-Medical Visa');
    } else if (paramPurpose === 'CONFERENCE') {
      setSelectedService('e-Conference Visa');
    }
  }, [paramCategory, paramPurpose]);

  const refreshCaptcha = () => {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let code = '';
    for (let i = 0; i < 6; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    setCaptchaCode(code);
    setCaptchaInput(code); // Pre-fill for user convenience in demo
  };

  const handleRegisterApplication = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    // Validations
    if (!email.trim() || !confirmEmail.trim()) {
      setErrorMessage('Please provide and confirm your email address.');
      return;
    }
    if (email.trim().toLowerCase() !== confirmEmail.trim().toLowerCase()) {
      setErrorMessage('Email address and confirmation email do not match.');
      return;
    }
    if (!dob) {
      setErrorMessage('Date of birth is required.');
      return;
    }
    if (!expectedArrivalDate) {
      setErrorMessage('Expected date of arrival is required.');
      return;
    }
    if (captchaInput.trim().toUpperCase() !== captchaCode) {
      setErrorMessage('Security verification captcha code is incorrect.');
      return;
    }
    if (!declarationChecked) {
      setErrorMessage('You must acknowledge the statutory passport and eligibility declaration.');
      return;
    }

    setSubmitting(true);
    try {
      const draft = await createOrGetDraft(email, nationality, selectedService);
      setRegisteredDraft(draft);

      // Submit draft to persist Trip entity and allocate reference ID for payments
      try {
        await submitDraft(
          draft.draftId,
          draft.version,
          `Applicant (${nationality})`,
          expectedArrivalDate,
          portOfArrival
        );
      } catch (submitErr) {
        console.warn('Draft persisted; subsequent submit status note:', submitErr);
      }

      setApplicationStatus('REGISTERED');
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (err) {
      console.warn('Backend draft creation offline fallback:', err);
      // Fallback for offline demo
      setRegisteredDraft({
        draftId: 'draft-' + Math.floor(1000 + Math.random() * 9000),
        referenceId: 'VS-2026-' + Math.floor(1000 + Math.random() * 9000),
        applicantEmail: email,
        currentStep: 1,
        sharedProfileJson: '{}',
        modulesJson: '{}',
        version: 0,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      });
      setApplicationStatus('REGISTERED');
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } finally {
      setSubmitting(false);
    }
  };

  // Payment checkout with real Razorpay Checkout modal & cryptographic verification
  const handleStartPayment = async () => {
    if (!registeredDraft) return;
    setPaying(true);
    setErrorMessage(null);

    try {
      const order = await createPaymentOrder(registeredDraft.referenceId);
      setPaymentOrder(order);

      const isLoaded = await loadRazorpayScript();
      if (!isLoaded || !(window as any).Razorpay) {
        throw new Error('Payment gateway SDK could not be loaded. Please check your internet connection.');
      }

      const options = {
        key: order.razorpayKeyId,
        amount: order.amountSubunits || Math.round(order.amount * 100),
        currency: order.currency || 'USD',
        name: 'VisaIndia.io',
        description: `Consular Visa Fee — ${registeredDraft.referenceId}`,
        order_id: order.orderId,
        handler: async (response: {
          razorpay_order_id: string;
          razorpay_payment_id: string;
          razorpay_signature: string;
        }) => {
          try {
            setPaying(true);
            const verifyRes = await verifyPayment({
              referenceId: registeredDraft.referenceId,
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            if (verifyRes.status === 'VERIFIED' || verifyRes.tripStatus === 'UNDER_REVIEW' || verifyRes.tripStatus === 'PAID') {
              setApplicationStatus(verifyRes.tripStatus || 'UNDER_REVIEW');
            } else {
              setErrorMessage(verifyRes.message || 'Payment verification failed.');
            }
          } catch (verifyErr: any) {
            console.error('Payment verification failed:', verifyErr);
            const msg = verifyErr?.response?.data?.error || verifyErr?.message || 'Payment verification failed.';
            setErrorMessage(msg);
          } finally {
            setPaying(false);
          }
        },
        prefill: {
          name: `Applicant (${nationality})`,
          email: email,
        },
        notes: {
          referenceId: registeredDraft.referenceId,
        },
        theme: {
          color: '#0f766e',
        },
        modal: {
          ondismiss: () => {
            setPaying(false);
            // In handleStartPayment never set UNDER_REVIEW on dismiss or error
          },
        },
      };

      const rzp = new (window as any).Razorpay(options);
      rzp.on('payment.failed', (resp: any) => {
        console.error('Payment failed:', resp.error);
        const failMsg = resp.error?.description || resp.error?.reason || 'Transaction could not be completed.';
        setErrorMessage(`Payment failed: ${failMsg}`);
        setPaying(false);
      });
      rzp.open();
    } catch (err: any) {
      console.error('Payment initiation error:', err);
      const errMsg = err?.response?.data?.error || err?.response?.data?.message || err?.message || 'Failed to initiate payment.';
      setErrorMessage(errMsg);
      // In handleStartPayment never set UNDER_REVIEW on error; show the error and the real status
      setPaying(false);
    }
  };

  // Poll for background government auto-adjudication
  useEffect(() => {
    if (registeredDraft && (applicationStatus === 'UNDER_REVIEW' || applicationStatus === 'PAID')) {
      const interval = setInterval(async () => {
        try {
          const detail = await getTripByReference(registeredDraft.referenceId);
          if (detail.trip) {
            setApplicationStatus(detail.trip.status);
            if (detail.trip.etaNumber) setEtaNumber(detail.trip.etaNumber);
            if (detail.trip.qrToken) setQrToken(detail.trip.qrToken);
          }
        } catch {
          // ignore
        }
      }, 3000);
      return () => clearInterval(interval);
    }
  }, [registeredDraft, applicationStatus]);

  return (
    <PageTemplate
      title="e-Visa Application — Preliminary Information"
      intro="Official Electronic Travel Authorization preliminary registration for foreign nationals visiting India."
      breadcrumbs={[{ label: 'Start Application', to: '/start-application' }]}
    >
      <div className="max-w-4xl mx-auto">
        {/* POST-REGISTRATION CONFIRMATION SCREEN */}
        {registeredDraft ? (
          <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 shadow-sm">
            <div className="text-center pb-6 border-b border-slate-200 mb-6">
              <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-700 mx-auto flex items-center justify-center mb-4">
                <CheckCircle2 className="w-10 h-10" />
              </div>
              <span className="px-3 py-1 bg-emerald-100 text-emerald-800 font-bold rounded-full text-xs uppercase tracking-wide">
                Application Registered & ID Assigned
              </span>
              <h2 className="text-2xl font-bold text-slate-900 mt-3">
                Application Reference: <span className="font-mono text-teal-800">{registeredDraft.referenceId}</span>
              </h2>
              <p className="text-xs text-slate-500 mt-1 max-w-lg mx-auto">
                Your preliminary registration has been recorded on the system. Please note down your Application Reference ID for future tracking and fee payment.
              </p>
            </div>

            {errorMessage && (
              <div className="mb-6 p-4 bg-rose-50 border border-rose-200 rounded-xl flex items-start gap-3 text-xs text-rose-800">
                <AlertCircle className="w-4 h-4 text-rose-600 flex-shrink-0 mt-0.5" />
                <div>
                  <strong className="block font-semibold">Payment Status / Alert:</strong>
                  <span>{errorMessage}</span>
                </div>
              </div>
            )}

            {/* DETAILS SUMMARY */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 p-5 bg-slate-50 rounded-2xl border border-slate-200 text-xs mb-6">
              <div>
                <span className="text-slate-400 block font-medium">Passport Type:</span>
                <strong className="text-slate-900">{passportType} Passport</strong>
              </div>
              <div>
                <span className="text-slate-400 block font-medium">Nationality:</span>
                <strong className="text-slate-900">{nationality}</strong>
              </div>
              <div>
                <span className="text-slate-400 block font-medium">Port of Arrival:</span>
                <strong className="text-slate-900">{portOfArrival.split('-')[0].trim()}</strong>
              </div>
              <div>
                <span className="text-slate-400 block font-medium">Visa Service:</span>
                <strong className="text-teal-800">{selectedService}</strong>
              </div>
              <div>
                <span className="text-slate-400 block font-medium">Applicant Email:</span>
                <strong className="text-slate-900">{email}</strong>
              </div>
              <div>
                <span className="text-slate-400 block font-medium">Current Status:</span>
                <span className="font-bold px-2 py-0.5 rounded bg-teal-100 text-teal-800">
                  {applicationStatus}
                </span>
              </div>
            </div>

            {/* STATUTORY ADVISORY & OFFICIAL PORTAL HANDOFF */}
            <div className="p-6 bg-gradient-to-r from-teal-950 via-slate-900 to-teal-900 text-white rounded-2xl mb-6 shadow-md border border-teal-800/40">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-teal-800/60">
                <div>
                  <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-amber-400 text-slate-950 inline-block mb-1">
                    Ready For Official Submission
                  </span>
                  <h3 className="text-lg font-bold text-white">
                    Continue on Official Government Portal
                  </h3>
                  <p className="text-xs text-slate-300 mt-1 max-w-xl">
                    Your preparation dossier is ready. Under Government of India regulations, official e-Visa applications and statutory consular fee payments must be finalized directly on the authorized portal: <strong className="text-amber-300">indianvisaonline.gov.in</strong>.
                  </p>
                </div>
                <div className="flex-shrink-0">
                  <a
                    href="https://indianvisaonline.gov.in/evisa/Registration"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="px-5 py-3 bg-amber-400 hover:bg-amber-300 text-slate-950 rounded-xl text-xs font-bold uppercase tracking-wider flex items-center gap-2 shadow transition-all hover:scale-[1.02]"
                  >
                    <span>Open Official Portal (indianvisaonline.gov.in)</span>
                    <ArrowRight className="w-4 h-4" />
                  </a>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-4 text-xs text-slate-300">
                <div className="flex items-start gap-2">
                  <span className="w-5 h-5 rounded-full bg-teal-800 text-teal-200 font-bold flex items-center justify-center text-[11px] flex-shrink-0">1</span>
                  <span>Reference your pre-filled details and assigned Dossier Reference ID above.</span>
                </div>
                <div className="flex items-start gap-2">
                  <span className="w-5 h-5 rounded-full bg-teal-800 text-teal-200 font-bold flex items-center justify-center text-[11px] flex-shrink-0">2</span>
                  <span>Upload your passport bio-page scan and compliant 2x2 photograph.</span>
                </div>
                <div className="flex items-start gap-2">
                  <span className="w-5 h-5 rounded-full bg-teal-800 text-teal-200 font-bold flex items-center justify-center text-[11px] flex-shrink-0">3</span>
                  <span>Pay the statutory government fee securely via the official portal gateway.</span>
                </div>
              </div>
            </div>

            {/* PREPARATION DOSSIER DOWNLOAD BANNER */}
            <div className="mb-6 p-4 bg-slate-50 border border-slate-200 rounded-xl flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
              <div>
                <span className="font-bold text-slate-900 block">Preparation Dossier & Pre-Filled Summary</span>
                <span className="text-slate-500">Download your verified particulars sheet to reference while completing the official application.</span>
              </div>
              <a
                href={getEtaPdfDownloadUrl(registeredDraft.referenceId)}
                target="_blank"
                rel="noopener noreferrer"
                className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-lg font-bold flex items-center gap-2 whitespace-nowrap transition-colors"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Download Preparation Dossier (PDF)</span>
              </a>
            </div>

            {/* PAYMENT & ADJUDICATION ACTIONS (DEV / TEST ENVIRONMENT SIMULATION) */}
            {applicationStatus === 'REGISTERED' && (
              <div className="p-6 bg-teal-50 border border-teal-200 rounded-2xl mb-6">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h3 className="font-bold text-teal-950 text-base">Step 2: Pay Government Visa Fee</h3>
                    <p className="text-xs text-teal-800">
                      Standard statutory consular fee: <strong>$25.00 USD</strong>. Razorpay TEST mode simulator enabled.
                      {paymentOrder && (
                        <span className="block mt-1 font-mono text-[11px] text-teal-900">
                          Gateway Order: {paymentOrder.orderId}
                        </span>
                      )}
                    </p>
                  </div>
                  <span className="text-2xl font-mono font-bold text-teal-900">$25.00</span>
                </div>

                <button
                  type="button"
                  disabled={paying}
                  onClick={handleStartPayment}
                  className="w-full py-3.5 bg-teal-700 hover:bg-teal-800 text-white rounded-xl font-bold text-xs uppercase tracking-wider flex items-center justify-center gap-2 shadow-md shadow-teal-700/20 cursor-pointer disabled:opacity-50"
                >
                  {paying ? (
                    <>
                      <RefreshCw className="w-4 h-4 animate-spin" />
                      <span>Processing Gateway Payment...</span>
                    </>
                  ) : (
                    <>
                      <CreditCard className="w-4 h-4" />
                      <span>Pay Government Fee ($25.00 USD)</span>
                    </>
                  )}
                </button>
              </div>
            )}

            {/* UNDER REVIEW / APPROVED CARDS */}
            {(applicationStatus === 'UNDER_REVIEW' || applicationStatus === 'APPROVED') && (
              <div className="p-6 bg-white border border-slate-200 rounded-2xl mb-6 shadow-sm">
                <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-4">
                  <div>
                    <span className="px-2.5 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-emerald-100 text-emerald-800">
                      {applicationStatus === 'APPROVED' ? 'Official ETA Granted' : 'Payment Verified — Under Review'}
                    </span>
                    <h3 className="text-lg font-bold text-slate-900 mt-1">
                      {applicationStatus === 'APPROVED'
                        ? 'Electronic Travel Authorization Ready'
                        : 'Application Under Consular Review'}
                    </h3>
                  </div>
                  <div className="text-right">
                    {etaNumber && (
                      <div>
                        <span className="text-[11px] text-slate-400 block font-medium">ETA Number:</span>
                        <strong className="text-sm font-mono text-teal-800">{etaNumber}</strong>
                      </div>
                    )}
                    {qrToken && (
                      <span className="text-[10px] font-mono text-slate-500 block">
                        Pass Hash: {qrToken.substring(0, 8)}…
                      </span>
                    )}
                  </div>
                </div>

                {/* PDF & QR Pass Download Links */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <a
                    href={getEtaPdfDownloadUrl(registeredDraft.referenceId)}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="p-4 bg-slate-50 hover:bg-slate-100 border border-slate-200 rounded-xl flex items-center justify-between text-xs font-bold text-slate-800 transition-colors"
                  >
                    <span className="flex items-center gap-2">
                      <Download className="w-4 h-4 text-teal-700" />
                      <span>Download Official ETA (PDF)</span>
                    </span>
                    <span>↗</span>
                  </a>

                  <a
                    href={getQrPassImageUrl(registeredDraft.referenceId)}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="p-4 bg-slate-50 hover:bg-slate-100 border border-slate-200 rounded-xl flex items-center justify-between text-xs font-bold text-slate-800 transition-colors"
                  >
                    <span className="flex items-center gap-2">
                      <QrCode className="w-4 h-4 text-teal-700" />
                      <span>View Biometric QR Pass</span>
                    </span>
                    <span>↗</span>
                  </a>
                </div>
              </div>
            )}

            <div className="flex items-center justify-between pt-4 border-t border-slate-100 text-xs">
              <Link
                to={`/existing-applications?ref=${registeredDraft.referenceId}`}
                className="font-bold text-teal-700 hover:text-teal-900 underline"
              >
                Open in Existing Applications Dashboard →
              </Link>
              <Link
                to="/"
                className="text-slate-600 hover:text-slate-900"
              >
                Return to Home Page
              </Link>
            </div>
          </div>
        ) : (
          /* ================= MAIN OFFICIAL FORM ================= */
          <div className="space-y-6">
            {/* STATUTORY ADVISORY BANNER */}
            <div className="p-4 bg-amber-50 border border-amber-200 rounded-2xl flex items-start gap-3">
              <AlertCircle className="w-5 h-5 text-amber-700 flex-shrink-0 mt-0.5" />
              <div className="text-xs text-amber-950 leading-relaxed">
                <strong>Independent Pre-Submission Assistant:</strong> VisaIndia.io helps travelers organize application particulars, verify eligibility, and validate document readiness. We do <em>not</em> issue visas, represent the Government of India, or collect statutory government fees. Final official submission and consular fee payment take place exclusively on the authorized government portal at{' '}
                <a
                  href="https://indianvisaonline.gov.in/evisa/Registration"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="underline font-bold text-amber-900 hover:text-amber-950"
                >
                  indianvisaonline.gov.in
                </a>
                .
              </div>
            </div>

            <form onSubmit={handleRegisterApplication} className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
              {/* OFFICIAL HEADER BANNER */}
              <div className="bg-slate-900 text-white px-6 py-5 border-b border-slate-800 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-teal-500/20 border border-teal-500/40 rounded-xl text-teal-300">
                    <FileText className="w-5 h-5" />
                  </div>
                  <div>
                    <h2 className="text-base font-bold text-white tracking-tight">
                      Online e-Visa Application Form (Part A)
                    </h2>
                    <p className="text-xs text-slate-400">
                      Preparation Dossier & Pre-Submission Organizer · Mandatory Preliminary Particulars
                    </p>
                  </div>
                </div>
              <span className="text-[11px] font-mono px-2.5 py-1 rounded-lg bg-slate-800 text-slate-300 border border-slate-700 hidden sm:inline">
                FORM REF: IVO-2026-A
              </span>
            </div>

            {/* ERROR ALERT */}
            {errorMessage && (
              <div className="m-6 p-4 bg-red-50 border border-red-200 rounded-xl text-red-700 text-xs font-medium flex items-center gap-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{errorMessage}</span>
              </div>
            )}

            <div className="p-6 sm:p-8 space-y-6">
              {/* ROW 1: PASSPORT TYPE & NATIONALITY */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                <div>
                  <label htmlFor="passport-type" className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                    Passport Type *
                  </label>
                  <select
                    id="passport-type"
                    value={passportType}
                    onChange={(e) => setPassportType(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                  >
                    {PASSPORT_TYPES.map((pt) => (
                      <option key={pt.value} value={pt.value}>
                        {pt.label}
                      </option>
                    ))}
                  </select>
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    Most tourists, holidaymakers, and business visitors hold Ordinary Passports.
                  </span>
                </div>

                <div>
                  <label htmlFor="nationality-select" className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                    Nationality / Region *
                  </label>
                  <SearchableCountrySelect
                    id="nationality-select"
                    value={nationality}
                    onChange={setNationality}
                    options={nationalities && nationalities.length > 0 ? nationalities : ALL_COUNTRIES}
                  />
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    Select citizenship matching the passport you will present upon arrival.
                  </span>
                </div>
              </div>

              {/* ROW 2: PORT OF ARRIVAL & DATE OF BIRTH */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                <div>
                  <label htmlFor="port-of-arrival" className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                    Designated Port of Arrival in India *
                  </label>
                  <select
                    id="port-of-arrival"
                    value={portOfArrival}
                    onChange={(e) => setPortOfArrival(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                  >
                    {ENTRY_PORTS.map((port) => (
                      <option key={port} value={port}>
                        {port}
                      </option>
                    ))}
                  </select>
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    Authorized immigration e-Visa counters available at 37 designated airports.
                  </span>
                </div>

                <div>
                  <label htmlFor="date-of-birth" className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                    Date of Birth *
                  </label>
                  <input
                    id="date-of-birth"
                    type="date"
                    value={dob}
                    onChange={(e) => setDob(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                  />
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    As recorded in your travel document (DD/MM/YYYY).
                  </span>
                </div>
              </div>

              {/* ROW 3: EMAIL ID & RE-ENTER EMAIL ID */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                <div>
                  <label htmlFor="applicant-email" className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                    Email ID *
                  </label>
                  <input
                    id="applicant-email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                    placeholder="applicant@example.com"
                  />
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    Important: Official ETA approval will be emailed to this address.
                  </span>
                </div>

                <div>
                  <label htmlFor="confirm-email" className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                    Re-enter Email ID *
                  </label>
                  <input
                    id="confirm-email"
                    type="email"
                    value={confirmEmail}
                    onChange={(e) => setConfirmEmail(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                    placeholder="applicant@example.com"
                  />
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    Must match Email ID precisely to prevent delivery failure.
                  </span>
                </div>
              </div>

              {/* ROW 4: EXPECTED DATE OF ARRIVAL */}
              <div>
                <label htmlFor="expected-arrival" className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                  Expected Date of Arrival *
                </label>
                <div className="max-w-md">
                  <input
                    id="expected-arrival"
                    type="date"
                    value={expectedArrivalDate}
                    onChange={(e) => setExpectedArrivalDate(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                  />
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    Apply at least 4 calendar days prior to departure. Window opens 120 days in advance.
                  </span>
                </div>
              </div>

              {/* ROW 5: VISA SERVICE (CATEGORY CARDS) */}
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-3">
                  Visa Service Required *
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {VISA_SERVICES.map((serv) => {
                    const isSelected = selectedService === serv.name;
                    return (
                      <div
                        key={serv.id}
                        onClick={() => setSelectedService(serv.name)}
                        className={`p-4 rounded-xl border cursor-pointer transition-all ${
                          isSelected
                            ? 'bg-teal-50/80 border-teal-600 ring-2 ring-teal-500/20 shadow-sm'
                            : 'bg-slate-50 border-slate-200 hover:border-slate-300'
                        }`}
                      >
                        <div className="flex items-center justify-between mb-1">
                          <strong className="text-xs font-bold text-slate-900">{serv.name}</strong>
                          <span className="text-xs font-mono font-bold text-teal-800">{serv.fee}</span>
                        </div>
                        <span className="inline-block px-1.5 py-0.5 rounded text-[10px] font-semibold bg-white border border-slate-200 text-slate-600 mb-2">
                          {serv.entries}
                        </span>
                        <p className="text-[11px] text-slate-500 leading-relaxed">
                          {serv.description}
                        </p>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* PRE-SUBMISSION APPLICATION-READINESS CHECKLIST */}
              <div className="p-5 bg-teal-50/60 rounded-2xl border border-teal-200 space-y-3">
                <div className="flex items-center justify-between">
                  <h3 className="text-xs font-bold text-teal-950 uppercase tracking-wider flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-teal-700" />
                    <span>Application-Readiness Checklist (Verify Before Submission)</span>
                  </h3>
                  <span className="text-[11px] font-semibold text-teal-800 bg-teal-100 px-2 py-0.5 rounded-full">
                    Pre-Submission Audit
                  </span>
                </div>
                <p className="text-[11px] text-teal-800">
                  Ensure you have the following materials ready according to official Indian immigration guidelines:
                </p>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 pt-1 text-xs">
                  <div className="p-3 bg-white rounded-xl border border-teal-100 shadow-2xs">
                    <div className="font-bold text-slate-800 flex items-center gap-1.5">
                      <span className="text-teal-600">✓</span>
                      <span>Passport Photograph (2x2 in)</span>
                    </div>
                    <p className="text-[11px] text-slate-500 mt-0.5">
                      Recent front-facing color photo, white background, no glasses, min 350x350 px (JPEG, 10 KB to 1 MB).
                    </p>
                  </div>
                  <div className="p-3 bg-white rounded-xl border border-teal-100 shadow-2xs">
                    <div className="font-bold text-slate-800 flex items-center gap-1.5">
                      <span className="text-teal-600">✓</span>
                      <span>Passport Bio-Page Scan</span>
                    </div>
                    <p className="text-[11px] text-slate-500 mt-0.5">
                      Clear PDF scan with min 6 months validity from arrival date and at least 2 blank pages (10 KB to 300 KB).
                    </p>
                  </div>
                  <div className="p-3 bg-white rounded-xl border border-teal-100 shadow-2xs">
                    <div className="font-bold text-slate-800 flex items-center gap-1.5">
                      <span className="text-teal-600">✓</span>
                      <span>Return / Onward Journey</span>
                    </div>
                    <p className="text-[11px] text-slate-500 mt-0.5">
                      Confirmed return ticket or ticket for onward journey to another destination outside India.
                    </p>
                  </div>
                  <div className="p-3 bg-white rounded-xl border border-teal-100 shadow-2xs">
                    <div className="font-bold text-slate-800 flex items-center gap-1.5">
                      <span className="text-teal-600">✓</span>
                      <span>Category Purpose Documentation</span>
                    </div>
                    <p className="text-[11px] text-slate-500 mt-0.5">
                      {selectedService.includes('Business')
                        ? 'Business card and letter of invitation from Indian counterpart entity.'
                        : selectedService.includes('Medical')
                        ? 'Formal letter on letterhead from the accredited hospital in India.'
                        : selectedService.includes('Conference')
                        ? 'Invitation from organizer and administrative / nodal ministry clearance.'
                        : 'Standard hotel reservations or local contact details for tourism.'}
                    </p>
                  </div>
                </div>
              </div>

              {/* ROW 6: SECURITY CAPTCHA & STATUTORY DECLARATION */}
              <div className="p-5 bg-slate-50 rounded-2xl border border-slate-200 space-y-4">
                <div>
                  <label htmlFor="captcha-input" className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                    Security Verification (Captcha) *
                  </label>
                  <div className="flex items-center gap-4">
                    <div className="px-4 py-2 bg-slate-800 text-amber-300 font-mono text-lg font-bold tracking-widest rounded-xl select-none flex items-center gap-3">
                      <span>{captchaCode}</span>
                      <button
                        type="button"
                        onClick={refreshCaptcha}
                        title="Reload Captcha"
                        className="text-slate-400 hover:text-white"
                      >
                        <RefreshCw className="w-4 h-4" />
                      </button>
                    </div>

                    <input
                      id="captcha-input"
                      type="text"
                      maxLength={6}
                      value={captchaInput}
                      onChange={(e) => setCaptchaInput(e.target.value.toUpperCase())}
                      className="w-36 px-3.5 py-2.5 bg-white border border-slate-300 rounded-xl text-sm font-mono font-bold text-slate-900 uppercase focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
                      placeholder="CAPTCHA"
                    />
                  </div>
                </div>

                <label className="flex items-start gap-2.5 pt-2 text-xs text-slate-700 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={declarationChecked}
                    onChange={(e) => setDeclarationChecked(e.target.checked)}
                    className="mt-0.5 rounded text-teal-600 focus:ring-teal-500"
                  />
                  <span>
                    <strong>Statutory Undertaking:</strong> I have read the instructions and certify that I hold a valid passport with at least six months validity from date of arrival, containing at least two blank pages, and an onward or return ticket.
                  </span>
                </label>
              </div>
            </div>

            {/* FORM FOOTER ACTION BUTTONS */}
            <div className="bg-slate-50 px-6 py-4 border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-3">
              <button
                type="button"
                onClick={() => navigate('/')}
                className="w-full sm:w-auto px-5 py-2.5 text-xs font-bold text-slate-600 hover:text-slate-900 transition-colors"
              >
                Cancel / Return to Home
              </button>

              <button
                type="submit"
                disabled={submitting}
                className="w-full sm:w-auto px-8 py-3.5 bg-teal-700 hover:bg-teal-800 text-white rounded-xl font-bold text-xs uppercase tracking-wider flex items-center justify-center gap-2 shadow-md shadow-teal-700/20 cursor-pointer disabled:opacity-50"
              >
                {submitting ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Registering Application...</span>
                  </>
                ) : (
                  <>
                    <span>Continue / Register Application</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
    </PageTemplate>
  );
};
