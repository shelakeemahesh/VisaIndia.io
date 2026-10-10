import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { PageTemplate } from '../components/PageTemplate';

export const HowItWorksPage: React.FC = () => {
  const [activeFaq, setActiveFaq] = useState<number | null>(null);

  const steps = [
    {
      number: '01',
      title: 'Check Eligibility',
      badge: 'Instant (0 sec)',
      description: 'Select your nationality and travel intent to view every open immigration pathway—including e-Visa sub-categories, Visa on Arrival, or visa-free treaties.',
    },
    {
      number: '02',
      title: 'Unified Application Form',
      badge: '10–12 Mins',
      description: 'Fill ONE clean profile. Shared traveler particulars automatically populate your e-Visa, pre-arrival e-Arrival Card, and e-Suvidha health declaration.',
    },
    {
      number: '03',
      title: 'Pay Government Fee',
      badge: 'Encrypted & Idempotent',
      description: 'Transparent statutory fees with signature verification. Automatic reconciliation prevents double billing even if your network drops mid-checkout.',
    },
    {
      number: '04',
      title: 'Security & Immigration Review',
      badge: '3–4 Business Days',
      description: 'Automated screening against immigration databases and consular verification. Real-time status tracker keeps you updated at every milestone.',
    },
    {
      number: '05',
      title: 'Receive ETA & QR Pass',
      badge: 'Digital Grant',
      description: 'Download your official Electronic Travel Authorization (ETA) PDF and biometric QR Pass directly to your phone and email.',
    },
    {
      number: '06',
      title: 'Airport Landing & Biometrics',
      badge: 'Dedicated Counters',
      description: 'Present your ETA and passport at designated e-Visa immigration counters across 37 international airports for quick biometric scanning and stamping.',
    },
  ];

  const faqs = [
    {
      q: 'Do I need to print the Electronic Travel Authorization (ETA)?',
      a: 'Yes. You must print a physical paper copy of your Electronic Travel Authorization (ETA) confirmation and carry it throughout your journey. Airlines will verify this printout prior to boarding flights to India.',
    },
    {
      q: 'How does the unified 3-in-1 form work without re-typing data?',
      a: 'Under standard procedures, travelers must navigate three separate portals: Indian Visa Online for the visa, the Bureau of Immigration for the e-Arrival Card, and the health portal for e-Suvidha. VisaSetu stores a unified traveler schema in MongoDB; your passport details, flight number, and address auto-fill all three modules simultaneously.',
    },
    {
      q: 'What happens if my flight is delayed or my port of arrival changes?',
      a: 'Once granted, an e-Visa is valid for arrival at ANY of the 37 designated international airports and 5 major seaports. You do not need to notify authorities or amend your ETA if your entry airport changes within the designated list.',
    },
    {
      q: 'Is my fee refunded if an application is not approved?',
      a: 'No. As per official Government of India policy, the e-Visa processing fee is non-refundable once the application has been submitted and reviewed, regardless of the decision outcome.',
    },
  ];

  return (
    <PageTemplate
      title="How VisaSetu Works: The Unified Travel Experience"
      intro="A streamlined, 6-stage journey from eligibility check to airport landing. Discover how unified profiling replaces three disjoint government portals."
      breadcrumbs={[{ label: 'How It Works' }]}
      sourceUrl="https://indianvisaonline.gov.in/evisa/tvoa.html"
      lastVerifiedDate="October 2026"
    >
      {/* SECTION 1: 6-Step Visual Timeline */}
      <section className="mb-14">
        <h2 className="text-xl font-bold text-slate-900 mb-6 flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-teal-600" />
          The 6-Step Traveler Journey
        </h2>

        {/* Timeline grid (Horizontal on desktop, vertical on mobile) */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {steps.map((step) => (
            <div
              key={step.number}
              className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm hover:shadow-md transition-shadow relative flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between mb-4">
                  <span className="text-2xl font-black text-teal-700/30">
                    {step.number}
                  </span>
                  <span className="text-[11px] font-semibold px-2.5 py-0.5 rounded-full bg-teal-50 text-teal-800 border border-teal-200">
                    {step.badge}
                  </span>
                </div>
                <h3 className="font-bold text-base text-slate-900 mb-2">
                  {step.title}
                </h3>
                <p className="text-xs text-slate-600 leading-relaxed">
                  {step.description}
                </p>
              </div>

              <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-400">
                <span>Stage {step.number} of 06</span>
                <span className="text-teal-600 font-semibold">Verified Flow ✓</span>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* SECTION 2: 3-in-1 Portal Unification Breakdown */}
      <section className="mb-14 bg-gradient-to-br from-slate-900 to-teal-950 text-white rounded-2xl p-8 lg:p-10 shadow-lg">
        <div className="max-w-3xl mb-8">
          <span className="inline-block px-3 py-1 rounded-full text-xs font-semibold bg-teal-500/20 text-teal-300 border border-teal-500/30 mb-3">
            Core Architectural Solution
          </span>
          <h2 className="text-2xl lg:text-3xl font-bold mb-3">
            One Profile Instead of Three Disjoint Portals
          </h2>
          <p className="text-sm text-slate-300 leading-relaxed">
            International travelers to India historically had to fill out 3 separate government systems, entering passport, address, and flight numbers 3 separate times. VisaSetu unites these requirements under a single intuitive schema.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white/10 backdrop-blur-md rounded-xl p-5 border border-white/10">
            <div className="text-xl font-bold text-teal-300 mb-2">
              1. e-Visa (MHA)
            </div>
            <p className="text-xs text-slate-300 mb-3">
              Standard electronic travel authorization covering primary identity, citizenship, and purpose clearance.
            </p>
            <span className="text-[10px] text-teal-400 font-semibold block uppercase">
              Auto-filled via Core Profile
            </span>
          </div>

          <div className="bg-white/10 backdrop-blur-md rounded-xl p-5 border border-white/10">
            <div className="text-xl font-bold text-teal-300 mb-2">
              2. e-Arrival Card (BOI)
            </div>
            <p className="text-xs text-slate-300 mb-3">
              Digital replacement for the paper disembarkation slip. Captures baggage and flight seat information.
            </p>
            <span className="text-[10px] text-teal-400 font-semibold block uppercase">
              Auto-filled from Flight Schedule
            </span>
          </div>

          <div className="bg-white/10 backdrop-blur-md rounded-xl p-5 border border-white/10">
            <div className="text-xl font-bold text-teal-300 mb-2">
              3. e-Suvidha (Health)
            </div>
            <p className="text-xs text-slate-300 mb-3">
              Mandatory health and vaccination self-declaration for international travelers from endemic zones.
            </p>
            <span className="text-[10px] text-teal-400 font-semibold block uppercase">
              Auto-filled via Medical Rules
            </span>
          </div>
        </div>
      </section>

      {/* SECTION 3: What the Traveler Receives */}
      <section className="mb-14">
        <h2 className="text-xl font-bold text-slate-900 mb-6 flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-teal-600" />
          What You Receive Upon Approval
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
            <div className="flex items-center gap-3 mb-3">
              <span className="text-2xl">📄</span>
              <h3 className="font-bold text-base text-slate-900">
                Official ETA Grant Document (PDF)
              </h3>
            </div>
            <p className="text-xs text-slate-600 leading-relaxed mb-4">
              A cryptographically signed Electronic Travel Authorization issued under the authority of the Ministry of Home Affairs, bearing your Application Reference ID, nationality, ETA validity window, and barcode.
            </p>
            <div className="p-3 bg-slate-50 rounded-lg text-xs text-slate-700 font-mono">
              STATUS: GRANTED | ETA_NO: VS-2026-4011 | VALID_UNTIL: 2031-10-25
            </div>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
            <div className="flex items-center gap-3 mb-3">
              <span className="text-2xl">📱</span>
              <h3 className="font-bold text-base text-slate-900">
                Fast-Track Mobile QR Boarding Pass
              </h3>
            </div>
            <p className="text-xs text-slate-600 leading-relaxed mb-4">
              Scannable boarding pass containing encrypted pre-arrival clearance tokens. Designed for immediate inspection at airline boarding gates and Indian airport biometric e-gates.
            </p>
            <div className="p-3 bg-slate-50 rounded-lg text-xs text-slate-700 font-mono">
              QR_VERIFY://visasetu.demo/verify?ref=VS-2026-4011
            </div>
          </div>
        </div>
      </section>

      {/* SECTION 4: Mini-FAQ Accordion */}
      <section className="mb-10 bg-white rounded-2xl border border-slate-200 p-6 md:p-8 shadow-sm">
        <h2 className="text-xl font-bold text-slate-900 mb-6 flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-teal-600" />
          Frequently Asked Questions About the Workflow
        </h2>

        <div className="space-y-4">
          {faqs.map((faq, idx) => (
            <div
              key={idx}
              className="border border-slate-200 rounded-xl overflow-hidden transition-colors"
            >
              <button
                type="button"
                onClick={() => setActiveFaq(activeFaq === idx ? null : idx)}
                className="w-full p-4 text-left font-bold text-sm text-slate-800 bg-slate-50 hover:bg-slate-100 flex items-center justify-between transition-colors cursor-pointer"
              >
                <span>{faq.q}</span>
                <span className="text-slate-400 text-lg">
                  {activeFaq === idx ? '−' : '+'}
                </span>
              </button>
              {activeFaq === idx && (
                <div className="p-4 bg-white text-xs text-slate-600 leading-relaxed border-t border-slate-100">
                  {faq.a}
                </div>
              )}
            </div>
          ))}
        </div>
      </section>

      {/* Bottom CTA */}
      <div className="text-center py-6">
        <Link
          to="/"
          className="inline-block px-8 py-3.5 bg-teal-700 hover:bg-teal-800 text-white rounded-xl font-bold text-sm shadow-sm transition-colors"
        >
          Check Your Country’s Route & Apply Now →
        </Link>
      </div>
    </PageTemplate>
  );
};
