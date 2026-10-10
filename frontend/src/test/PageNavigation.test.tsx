import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { VisaCategoriesPage } from '../pages/VisaCategoriesPage';
import { RequirementsPage } from '../pages/RequirementsPage';
import { HowItWorksPage } from '../pages/HowItWorksPage';
import { ExistingApplicationsPage } from '../pages/ExistingApplicationsPage';
import { HelpPage } from '../pages/HelpPage';
import { ConnectAiAgentPage } from '../pages/ConnectAiAgentPage';
import { HashRedirector } from '../components/HashRedirector';
import * as contentApi from '../api/content';

vi.mock('../api/content');

describe('Multi-Page Navigation & Feature Suites', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders VisaCategoriesPage with categories and other routes', async () => {
    vi.mocked(contentApi.fetchVisaCategories).mockResolvedValue([
      {
        id: '1',
        slug: 'e-tourist-30-days',
        name: 'e-Tourist Visa (30 Days)',
        subCategoryName: 'e-Tourist 30D',
        code: 'ETV_30D',
        purposeCode: 'TOURISM',
        description: 'Short leisure visit',
        whoIsItFor: 'Holidaymakers',
        validityText: '30 Days',
        validityDays: 30,
        entries: 'DOUBLE',
        maxStayText: '30 Days',
        maxStayDays: 30,
        feeBandSummary: '$25 USD',
        processingDaysMin: 3,
        processingDaysMax: 4,
        isExtendable: false,
        isConvertible: false,
        requiredDocuments: ['Passport photo', 'Bio page scan'],
        entryPointLimits: '37 Airports',
        officialSourceUrl: 'https://indianvisaonline.gov.in',
        lastVerifiedAt: '2026-10-10T00:00:00Z',
      },
    ]);

    render(
      <MemoryRouter initialEntries={['/visa-categories']}>
        <VisaCategoriesPage />
      </MemoryRouter>
    );

    expect(await screen.findByText('e-Tourist Visa (30 Days)')).toBeInTheDocument();
    expect(screen.getByText(/Other Legal Immigration & Travel Routes/i)).toBeInTheDocument();
    expect(screen.getByText('Visa on Arrival (VoA)')).toBeInTheDocument();
  });

  it('renders RequirementsPage with interactive photo validator and ports', async () => {
    vi.mocked(contentApi.fetchEntryPoints).mockResolvedValue([
      {
        id: 'p1',
        name: 'Indira Gandhi International Airport, Delhi',
        code: 'DEL',
        type: 'AIRPORT',
        state: 'Delhi',
        isEvisaEntry: true,
        isEvisaExit: true,
        isVoaAllowed: true,
        officialSourceUrl: 'https://boi.gov.in/',
      },
    ]);

    render(
      <MemoryRouter initialEntries={['/requirements']}>
        <RequirementsPage />
      </MemoryRouter>
    );

    expect(screen.getByText(/Interactive Passport Photo Validator/i)).toBeInTheDocument();
    expect(screen.getByText(/Personalized Document Checklist/i)).toBeInTheDocument();
    expect(await screen.findByText(/Indira Gandhi International Airport, Delhi/i)).toBeInTheDocument();
  });

  it('renders HowItWorksPage with 6-stage timeline and 3-in-1 portal breakdown', () => {
    render(
      <MemoryRouter initialEntries={['/how-it-works']}>
        <HowItWorksPage />
      </MemoryRouter>
    );

    expect(screen.getByText(/The 6-Step Traveler Journey/i)).toBeInTheDocument();
    expect(screen.getByText(/One Profile Instead of Three Disjoint Portals/i)).toBeInTheDocument();
    expect(screen.getByText(/Check Eligibility/i)).toBeInTheDocument();
  });

  it('renders ExistingApplicationsPage and handles demo lookup and payment resync', async () => {
    vi.mocked(contentApi.trackApplication).mockResolvedValue({
      referenceId: 'VS-2026-4011',
      applicantName: 'Sarah Jane Jenkins',
      maskedEmail: 'sa****s@example.com',
      nationality: 'USA',
      categoryName: 'e-Tourist Visa (5 Years)',
      status: 'APPROVED',
      orderId: 'order_123',
      paymentId: 'pay_123',
      amount: 80,
      arrivalDate: '2026-10-25',
      portOfArrival: 'Delhi (DEL)',
      qrData: 'ETA:VS-2026-4011',
      createdAt: '2026-10-01T00:00:00Z',
      updatedAt: '2026-10-02T00:00:00Z',
      timeline: [
        { step: 'DRAFT', label: 'Drafted', state: 'COMPLETED', description: 'Draft created', timestamp: null },
        { step: 'PAYMENT', label: 'Paid', state: 'COMPLETED', description: 'Settled', timestamp: null },
      ],
    });

    render(
      <MemoryRouter initialEntries={['/existing-applications']}>
        <ExistingApplicationsPage />
      </MemoryRouter>
    );

    expect(screen.getByText(/Demo Application Tester/i)).toBeInTheDocument();
    const approvedBtn = screen.getByRole('button', { name: /Approved \(ETA Granted\)/i });
    fireEvent.click(approvedBtn);

    const lookupBtn = screen.getByRole('button', { name: /Lookup Status/i });
    fireEvent.click(lookupBtn);

    expect(await screen.findByText('VS-2026-4011')).toBeInTheDocument();
    expect(screen.getByText(/Sarah Jane Jenkins/i)).toBeInTheDocument();
    expect(screen.getByText(/Official ETA Pass Ready for Download/i)).toBeInTheDocument();
  });

  it('renders ConnectAiAgentPage with Claude and Cursor configs and safe-mode banner', () => {
    render(
      <MemoryRouter initialEntries={['/connect-ai-agent']}>
        <ConnectAiAgentPage />
      </MemoryRouter>
    );

    expect(screen.getByText(/Autonomous Agent Safe-Mode Guardrail Enforced/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Claude Desktop/i })).toBeInTheDocument();
    expect(screen.getByText(/Live Agent Tool Simulator/i)).toBeInTheDocument();
  });

  it('renders HelpPage with 24x7 call center and problem report form', async () => {
    vi.mocked(contentApi.fetchFaqs).mockResolvedValue([
      {
        id: 'faq1',
        topic: 'PAYMENT',
        question: 'What if bank is debited but status pending?',
        answer: 'Use the payment resync engine.',
        orderNum: 1,
        officialSourceUrl: 'https://boi.gov.in',
        lastVerifiedAt: '2026-10-10T00:00:00Z',
      },
    ]);

    render(
      <MemoryRouter initialEntries={['/help']}>
        <HelpPage />
      </MemoryRouter>
    );

    expect(screen.getByText(/\+91-11-24300666/i)).toBeInTheDocument();
    expect(screen.getByText(/Submit an Inquiry or Report an Issue/i)).toBeInTheDocument();
    expect(await screen.findByText('What if bank is debited but status pending?')).toBeInTheDocument();
  });

  it('HashRedirector navigates legacy hash to new route', async () => {
    render(
      <MemoryRouter initialEntries={['/#requirements']}>
        <HashRedirector />
        <Routes>
          <Route path="/" element={<div>Home</div>} />
          <Route path="/requirements" element={<div>Requirements Routed</div>} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Requirements Routed')).toBeInTheDocument();
    });
  });
});
