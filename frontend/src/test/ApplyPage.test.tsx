import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ApplyPage } from '../pages/ApplyPage';
import * as tripsApi from '../api/trips';
import * as eligibilityApi from '../api/eligibility';

vi.mock('../api/trips');
vi.mock('../api/eligibility');

describe('ApplyPage - Unified 3-in-1 Application Wizard', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(eligibilityApi.fetchNationalities).mockResolvedValue([
      { code: 'JPN', name: 'Japan', flagEmoji: '🇯🇵' },
      { code: 'USA', name: 'United States', flagEmoji: '🇺🇸' },
    ]);

    vi.mocked(tripsApi.createOrGetDraft).mockResolvedValue({
      draftId: 'draft-123',
      referenceId: 'VS-2026-8562',
      applicantEmail: 'traveler@example.com',
      currentStep: 1,
      sharedProfileJson: '{}',
      modulesJson: '{}',
      version: 0,
      createdAt: '2026-10-10T00:00:00Z',
      updatedAt: '2026-10-10T00:00:00Z',
    });

    vi.mocked(tripsApi.autosaveDraft).mockResolvedValue({
      draftId: 'draft-123',
      referenceId: 'VS-2026-8562',
      applicantEmail: 'traveler@example.com',
      currentStep: 2,
      sharedProfileJson: '{}',
      modulesJson: '{}',
      version: 1,
      createdAt: '2026-10-10T00:00:00Z',
      updatedAt: '2026-10-10T00:00:00Z',
    });

    vi.mocked(tripsApi.submitDraft).mockResolvedValue({
      tripId: 'trip-999',
      referenceId: 'VS-2026-8562',
      status: 'SUBMITTED',
      amount: 25,
      currency: 'USD',
      applicantName: 'Kenji Sato',
      modules: [],
    });

    vi.mocked(tripsApi.createPaymentOrder).mockResolvedValue({
      orderId: 'order_rzp_mock_123',
      amount: 25,
      currency: 'USD',
      status: 'CREATED',
      idempotencyKey: 'idem-123',
      razorpayKeyId: 'rzp_test',
      referenceId: 'VS-2026-8562',
    });

    vi.mocked(tripsApi.resyncPayment).mockResolvedValue({
      status: 'RECONCILED',
      referenceId: 'VS-2026-8562',
      newTripStatus: 'APPROVED',
      message: 'Payment verified and confirmed',
    });

    vi.mocked(tripsApi.getEtaPdfDownloadUrl).mockReturnValue('http://localhost:8080/api/v1/documents/VS-2026-8562/eta-pdf');
    vi.mocked(tripsApi.getQrPassImageUrl).mockReturnValue('http://localhost:8080/api/v1/documents/VS-2026-8562/qr-pass');
  });

  it('renders Step 1 with traveler profile and autosave indicator', async () => {
    render(
      <MemoryRouter initialEntries={['/apply?nationality=JPN']}>
        <ApplyPage />
      </MemoryRouter>
    );

    expect(screen.getByText(/Step 1: Traveler Profile & Passport Particulars/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Given Name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Passport Number/i)).toBeInTheDocument();
    expect(await screen.findByText(/Autosaved ✓/i)).toBeInTheDocument();
  });

  it('progresses through wizard steps and renders fee review', async () => {
    render(
      <MemoryRouter initialEntries={['/apply?nationality=JPN']}>
        <ApplyPage />
      </MemoryRouter>
    );

    // Step 1 -> Step 2
    const toStep2Btn = screen.getByRole('button', { name: /Continue to Itinerary/i });
    fireEvent.click(toStep2Btn);

    expect(await screen.findByText(/Step 2: Trip Itinerary & Port of Entry/i)).toBeInTheDocument();

    // Step 2 -> Step 3
    const toStep3Btn = screen.getByRole('button', { name: /Continue to 3-in-1 Modules/i });
    fireEvent.click(toStep3Btn);

    expect(await screen.findByText(/1 Profile Powers 3 Official Port Requirements/i)).toBeInTheDocument();
    expect(screen.getByText(/Module A: e-Arrival Card Information/i)).toBeInTheDocument();
    expect(screen.getByText(/Module B: e-Suvidha Health Declaration/i)).toBeInTheDocument();

    // Step 3 -> Step 4
    const toStep4Btn = screen.getByRole('button', { name: /Proceed to Review/i });
    fireEvent.click(toStep4Btn);

    expect(await screen.findByText(/Step 4: Review Application & Statutory Fee Breakdown/i)).toBeInTheDocument();
    expect(screen.getByText(/Total Amount Payable:/i)).toBeInTheDocument();
    expect(screen.getAllByText(/\$25.00 USD/i)[0]).toBeInTheDocument();
  });

  it('handles submission, payment simulation, and provides ETA PDF download', async () => {
    render(
      <MemoryRouter initialEntries={['/apply?nationality=JPN']}>
        <ApplyPage />
      </MemoryRouter>
    );

    // Navigate to Step 4
    fireEvent.click(screen.getByRole('button', { name: /Continue to Itinerary/i }));
    fireEvent.click(await screen.findByRole('button', { name: /Continue to 3-in-1 Modules/i }));
    fireEvent.click(await screen.findByRole('button', { name: /Proceed to Review/i }));

    // Submit at Step 4 -> advances to Step 5
    const submitBtn = await screen.findByRole('button', { name: /Confirm & Proceed to Payment/i });
    fireEvent.click(submitBtn);

    expect(await screen.findByText(/Bulletproof Payment Checkout/i)).toBeInTheDocument();
    expect(screen.getByText(/Razorpay TEST Mode Active/i)).toBeInTheDocument();

    // Execute Payment at Step 5 -> advances to Step 6
    const payBtn = screen.getByRole('button', { name: /Pay \$25.00 USD/i });
    fireEvent.click(payBtn);

    expect(await screen.findByText(/Payment Confirmed/i)).toBeInTheDocument();
    expect(screen.getByText(/Download ETA \(PDF\)/i)).toBeInTheDocument();
    expect(screen.getByText(/View QR Pass/i)).toBeInTheDocument();
  });
});
