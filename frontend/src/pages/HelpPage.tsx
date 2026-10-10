import React, { useState, useEffect } from 'react';
import { PageTemplate } from '../components/PageTemplate';
import { fetchFaqs, submitHelpTicket, FaqDto, HelpTicketResponse } from '../api/content';

export const HelpPage: React.FC = () => {
  const [faqs, setFaqs] = useState<FaqDto[]>([]);
  const [loadingFaqs, setLoadingFaqs] = useState(true);
  const [selectedTopic, setSelectedTopic] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [activeAccordion, setActiveAccordion] = useState<string | null>(null);

  // Problem report form state
  const [ticketCategory, setTicketCategory] = useState('PAYMENT');
  const [ticketSubject, setTicketSubject] = useState('');
  const [ticketDesc, setTicketDesc] = useState('');
  const [ticketEmail, setTicketEmail] = useState('');
  const [ticketRef, setTicketRef] = useState('');
  const [submittingTicket, setSubmittingTicket] = useState(false);
  const [ticketSuccess, setTicketSuccess] = useState<HelpTicketResponse | null>(null);
  const [ticketError, setTicketError] = useState<string | null>(null);

  useEffect(() => {
    fetchFaqs()
      .then((data) => {
        setFaqs(data);
        setLoadingFaqs(false);
      })
      .catch((err) => {
        console.error('Error fetching FAQs:', err);
        setLoadingFaqs(false);
      });
  }, []);

  const handleTicketSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!ticketSubject.trim() || !ticketDesc.trim() || !ticketEmail.trim()) {
      setTicketError('Please provide subject, description, and your contact email.');
      return;
    }

    setSubmittingTicket(true);
    setTicketError(null);

    try {
      const res = await submitHelpTicket({
        category: ticketCategory,
        subject: ticketSubject.trim(),
        description: ticketDesc.trim(),
        email: ticketEmail.trim(),
        referenceId: ticketRef.trim() || undefined,
      });
      setTicketSuccess(res);
      setTicketSubject('');
      setTicketDesc('');
      setTicketRef('');
    } catch (err: any) {
      console.error('Error filing help ticket:', err);
      setTicketError('Failed to dispatch support ticket. Please verify details.');
    } finally {
      setSubmittingTicket(false);
    }
  };

  const filteredFaqs = faqs.filter((faq) => {
    if (selectedTopic !== 'ALL' && faq.topic !== selectedTopic) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      return faq.question.toLowerCase().includes(q) || faq.answer.toLowerCase().includes(q);
    }
    return true;
  });

  return (
    <PageTemplate
      title="Consular Assistance & 24x7 Support Desk"
      intro="Frequently asked questions, official helpline directories, payment troubleshooting guides, and incident ticket submission."
      breadcrumbs={[{ label: 'Help' }]}
      sourceUrl="https://indianvisaonline.gov.in/evisa/tvoa.html"
      lastVerifiedDate="October 2026"
    >
      {/* 24x7 HELPDESK BANNER */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
        <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
          <div className="w-10 h-10 rounded-lg bg-teal-50 text-teal-700 flex items-center justify-center text-xl mb-3">
            📞
          </div>
          <h3 className="font-bold text-slate-900 text-sm mb-1">
            Official 24x7 Call Center
          </h3>
          <p className="text-xs text-slate-500 mb-2">
            National Bureau of Immigration helpline
          </p>
          <a href="tel:+911124300666" className="text-sm font-bold text-teal-800 hover:underline">
            +91-11-24300666
          </a>
        </div>

        <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
          <div className="w-10 h-10 rounded-lg bg-teal-50 text-teal-700 flex items-center justify-center text-xl mb-3">
            ✉️
          </div>
          <h3 className="font-bold text-slate-900 text-sm mb-1">
            Official Consular Helpdesk
          </h3>
          <p className="text-xs text-slate-500 mb-2">
            Direct email query response
          </p>
          <a href="mailto:indiatvoa@gov.in" className="text-sm font-bold text-teal-800 hover:underline">
            indiatvoa@gov.in
          </a>
        </div>

        <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
          <div className="w-10 h-10 rounded-lg bg-teal-50 text-teal-700 flex items-center justify-center text-xl mb-3">
            🚨
          </div>
          <h3 className="font-bold text-slate-900 text-sm mb-1">
            FRRO Emergency Desk
          </h3>
          <p className="text-xs text-slate-500 mb-2">
            Foreigners Regional Registration Office
          </p>
          <span className="text-sm font-bold text-slate-800">
            support.immigration@nic.in
          </span>
        </div>
      </div>

      {/* SECTION 1: FAQ Explorer */}
      <section className="mb-14">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
          <div>
            <h2 className="text-xl font-bold text-slate-900 mb-1 flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-teal-600" />
              Comprehensive Knowledge Base (FAQs)
            </h2>
            <p className="text-xs text-slate-600">
              Instant answers across all 6 visa stages sourced from statutory regulations.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <input
              type="text"
              placeholder="Search answers..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="text-xs px-3 py-2 bg-slate-50 border border-slate-300 rounded-lg focus:ring-teal-500 focus:border-teal-500 min-w-[200px]"
            />

            <select
              value={selectedTopic}
              onChange={(e) => setSelectedTopic(e.target.value)}
              className="text-xs px-3 py-2 bg-slate-50 border border-slate-300 rounded-lg font-medium"
            >
              <option value="ALL">All 6 Topics</option>
              <option value="BEFORE_APPLYING">Before Applying</option>
              <option value="APPLICATION">Application Form</option>
              <option value="PAYMENT">Payment Troubles</option>
              <option value="AFTER_APPROVAL">After Approval</option>
              <option value="ARRIVAL">Airport Arrival</option>
              <option value="TECHNICAL">Technical Rules</option>
            </select>
          </div>
        </div>

        {loadingFaqs ? (
          <div className="py-12 text-center text-xs text-slate-500">
            Loading answers...
          </div>
        ) : (
          <div className="space-y-3">
            {filteredFaqs.map((faq) => {
              const isOpen = activeAccordion === faq.id;
              return (
                <div
                  key={faq.id || faq.question}
                  className="bg-white border border-slate-200 rounded-xl overflow-hidden transition-colors"
                >
                  <button
                    type="button"
                    onClick={() => setActiveAccordion(isOpen ? null : faq.id)}
                    className="w-full p-4 text-left font-bold text-sm text-slate-900 bg-white hover:bg-slate-50 flex items-center justify-between gap-4 transition-colors cursor-pointer"
                  >
                    <span>{faq.question}</span>
                    <span className="text-slate-400 text-lg flex-shrink-0">
                      {isOpen ? '−' : '+'}
                    </span>
                  </button>

                  {isOpen && (
                    <div className="p-4 bg-slate-50/50 text-xs text-slate-700 leading-relaxed border-t border-slate-100">
                      <p className="mb-3">{faq.answer}</p>
                      <div className="text-[11px] text-slate-400">
                        Topic: <span className="font-semibold text-slate-600">{faq.topic}</span>
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* SECTION 2: Payment Troubleshooting Card */}
      <section className="mb-14 bg-amber-50/70 border border-amber-200 rounded-2xl p-6 md:p-8">
        <h2 className="text-lg font-bold text-amber-950 mb-3 flex items-center gap-2">
          <span>💳</span> Payment Troubleshooting Guide
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 text-xs text-amber-900 leading-relaxed">
          <div>
            <h3 className="font-bold mb-1">Money Debited But Not Confirmed?</h3>
            <p>
              Banks may take up to 30 minutes to broadcast authorization tokens. Do not make an immediate duplicate payment. Use the <strong>Re-sync Payment</strong> button on the Existing Applications page.
            </p>
          </div>
          <div>
            <h3 className="font-bold mb-1">Max 3 Payment Attempts</h3>
            <p>
              To protect applicants against card theft, accounts lock after 3 unsuccessful attempts. You can regenerate a clean checkout session with a fresh idempotency key without losing saved profile data.
            </p>
          </div>
          <div>
            <h3 className="font-bold mb-1">Browser Crashed Mid-Transaction</h3>
            <p>
              VisaSetu stores transactions with transactional outbox logs. The signature-verified webhook reconciles your application state automatically when connectivity returns.
            </p>
          </div>
        </div>
      </section>

      {/* SECTION 3: Report a Problem Ticket Form */}
      <section className="bg-white rounded-2xl border border-slate-200 p-6 md:p-8 shadow-sm">
        <div className="max-w-2xl mb-6">
          <h2 className="text-xl font-bold text-slate-900 mb-1 flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-teal-600" />
            Submit an Inquiry or Report an Issue
          </h2>
          <p className="text-xs text-slate-600">
            Cannot find an answer? Dispatch an official ticket directly to our consular helpdesk team. Typical response time is 4 to 8 business hours.
          </p>
        </div>

        {ticketSuccess ? (
          <div className="p-6 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-900">
            <span className="text-xl block mb-2">🎉</span>
            <h3 className="text-base font-bold mb-1">Ticket Successfully Logged!</h3>
            <p className="text-xs mb-3">
              Your inquiry reference number is: <strong className="font-mono text-sm">{ticketSuccess.ticketId}</strong>.
            </p>
            <p className="text-xs text-emerald-800">
              {ticketSuccess.message}
            </p>
            <button
              type="button"
              onClick={() => setTicketSuccess(null)}
              className="mt-4 px-4 py-2 bg-emerald-700 text-white rounded-lg text-xs font-semibold cursor-pointer"
            >
              Submit Another Inquiry
            </button>
          </div>
        ) : (
          <form onSubmit={handleTicketSubmit} className="max-w-xl space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Issue Classification
                </label>
                <select
                  value={ticketCategory}
                  onChange={(e) => setTicketCategory(e.target.value)}
                  className="w-full text-xs p-2.5 bg-slate-50 border border-slate-300 rounded-lg"
                >
                  <option value="PAYMENT">Payment Debited / Transaction Failure</option>
                  <option value="DOCUMENT">Photo or Passport Upload Rejection</option>
                  <option value="CORRECTION">Correction to Submitted Application</option>
                  <option value="URGENT">Urgent Travel Emergency (&lt; 48h)</option>
                  <option value="GENERAL">General Policy Inquiry</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Application Reference ID (Optional)
                </label>
                <input
                  type="text"
                  placeholder="e.g. VS-2026-4011"
                  value={ticketRef}
                  onChange={(e) => setTicketRef(e.target.value)}
                  className="w-full text-xs p-2.5 font-mono bg-slate-50 border border-slate-300 rounded-lg"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">
                Your Contact Email Address
              </label>
              <input
                type="email"
                required
                placeholder="traveler@example.com"
                value={ticketEmail}
                onChange={(e) => setTicketEmail(e.target.value)}
                className="w-full text-xs p-2.5 bg-slate-50 border border-slate-300 rounded-lg"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">
                Subject
              </label>
              <input
                type="text"
                required
                placeholder="Brief summary of the issue..."
                value={ticketSubject}
                onChange={(e) => setTicketSubject(e.target.value)}
                className="w-full text-xs p-2.5 bg-slate-50 border border-slate-300 rounded-lg"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">
                Detailed Explanation
              </label>
              <textarea
                required
                rows={4}
                placeholder="Provide specific details such as date of travel, error codes encountered, or payment gateway reference numbers..."
                value={ticketDesc}
                onChange={(e) => setTicketDesc(e.target.value)}
                className="w-full text-xs p-2.5 bg-slate-50 border border-slate-300 rounded-lg"
              />
            </div>

            {ticketError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700">
                {ticketError}
              </div>
            )}

            <button
              type="submit"
              disabled={submittingTicket}
              className="px-6 py-2.5 bg-teal-700 hover:bg-teal-800 text-white rounded-lg text-xs font-bold shadow-sm transition-colors cursor-pointer"
            >
              {submittingTicket ? 'Dispatched to Desk...' : 'Submit Support Ticket →'}
            </button>
          </form>
        )}
      </section>
    </PageTemplate>
  );
};
