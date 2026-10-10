import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { StartApplicationPage } from '../pages/StartApplicationPage';
import * as tripsApi from '../api/trips';
import * as eligibilityApi from '../api/eligibility';

vi.mock('../api/trips');
vi.mock('../api/eligibility');

describe('StartApplicationPage — Official Preliminary Form', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(eligibilityApi.fetchNationalities).mockResolvedValue([
      { code: 'JPN', name: 'Japan', flagEmoji: '🇯🇵' },
      { code: 'USA', name: 'United States', flagEmoji: '🇺🇸' },
      { code: 'GBR', name: 'United Kingdom', flagEmoji: '🇬🇧' },
    ]);
  });

  it('renders official e-Visa Part A form with mandatory preliminary fields', async () => {
    render(
      <MemoryRouter initialEntries={['/start-application?nationality=JPN&purpose=TOURISM']}>
        <StartApplicationPage />
      </MemoryRouter>
    );

    expect(await screen.findByText(/Online e-Visa Application Form \(Part A\)/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Passport Type \*/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Nationality \/ Region \*/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Designated Port of Arrival in India \*/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Date of Birth \*/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Email ID \*/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Re-enter Email ID \*/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Expected Date of Arrival \*/i)).toBeInTheDocument();
    expect(screen.getByText(/Security Verification \(Captcha\) \*/i)).toBeInTheDocument();
    expect(screen.getByText(/Statutory Undertaking:/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Continue \/ Register Application/i })).toBeInTheDocument();
  });

  it('validates email matching', async () => {
    render(
      <MemoryRouter initialEntries={['/start-application']}>
        <StartApplicationPage />
      </MemoryRouter>
    );

    const emailInput = screen.getByLabelText(/^Email ID \*/i);
    const confirmEmailInput = screen.getByLabelText(/^Re-enter Email ID \*/i);

    fireEvent.change(emailInput, { target: { value: 'traveler@test.com' } });
    fireEvent.change(confirmEmailInput, { target: { value: 'mismatch@test.com' } });

    const submitBtn = screen.getByRole('button', { name: /Continue \/ Register Application/i });
    fireEvent.click(submitBtn);

    expect(await screen.findByText(/Email address and confirmation email do not match\./i)).toBeInTheDocument();
  });

  it('validates statutory undertaking checkbox before submission', async () => {
    render(
      <MemoryRouter initialEntries={['/start-application']}>
        <StartApplicationPage />
      </MemoryRouter>
    );

    const emailInput = screen.getByLabelText(/^Email ID \*/i);
    const confirmEmailInput = screen.getByLabelText(/^Re-enter Email ID \*/i);

    fireEvent.change(emailInput, { target: { value: 'valid@test.com' } });
    fireEvent.change(confirmEmailInput, { target: { value: 'valid@test.com' } });

    const submitBtn = screen.getByRole('button', { name: /Continue \/ Register Application/i });
    fireEvent.click(submitBtn);

    expect(await screen.findByText(/You must acknowledge the statutory passport and eligibility declaration\./i)).toBeInTheDocument();
  });

  it('submits preliminary form, registers draft, and shows Reference ID with fee payment', async () => {
    vi.mocked(tripsApi.createOrGetDraft).mockResolvedValue({
      draftId: 'draft-12345',
      referenceId: 'VS-2026-9876',
      applicantEmail: 'valid@test.com',
      currentStep: 1,
      sharedProfileJson: '{}',
      modulesJson: '{}',
      version: 1,
      createdAt: '2026-10-10T00:00:00Z',
      updatedAt: '2026-10-10T00:00:00Z',
    });

    vi.mocked(tripsApi.createPaymentOrder).mockResolvedValue({
      orderId: 'order_test_999',
      referenceId: 'VS-2026-9876',
      amount: 2500,
      currency: 'USD',
      status: 'CREATED',
      idempotencyKey: 'idemp-999',
      razorpayKeyId: 'rzp_test_key',
    });

    vi.mocked(tripsApi.resyncPayment).mockResolvedValue({
      referenceId: 'VS-2026-9876',
      status: 'CAPTURED',
      newTripStatus: 'APPROVED',
      message: 'Payment verified and confirmed',
    });

    render(
      <MemoryRouter initialEntries={['/start-application']}>
        <StartApplicationPage />
      </MemoryRouter>
    );

    const emailInput = screen.getByLabelText(/^Email ID \*/i);
    const confirmEmailInput = screen.getByLabelText(/^Re-enter Email ID \*/i);

    fireEvent.change(emailInput, { target: { value: 'valid@test.com' } });
    fireEvent.change(confirmEmailInput, { target: { value: 'valid@test.com' } });

    // Check statutory undertaking
    const checkbox = screen.getByRole('checkbox');
    fireEvent.click(checkbox);

    const submitBtn = screen.getByRole('button', { name: /Continue \/ Register Application/i });
    fireEvent.click(submitBtn);

    // Draft registered and screen transitions
    expect(await screen.findByText(/Application Registered & ID Assigned/i)).toBeInTheDocument();
    expect(screen.getByText('VS-2026-9876')).toBeInTheDocument();
    expect(screen.getByText(/Pay Government Fee \(\$25\.00 USD\)/i)).toBeInTheDocument();

    // Trigger payment
    const payBtn = screen.getByRole('button', { name: /Pay Government Fee/i });
    fireEvent.click(payBtn);

    await waitFor(() => {
      expect(tripsApi.createPaymentOrder).toHaveBeenCalledWith('VS-2026-9876');
      expect(tripsApi.resyncPayment).toHaveBeenCalledWith('VS-2026-9876');
    });

    // Check approved screen and ETA links
    expect(await screen.findByText(/Official ETA Granted/i)).toBeInTheDocument();
    expect(screen.getByText(/Download Official ETA \(PDF\)/i)).toBeInTheDocument();
    expect(screen.getByText(/View Biometric QR Pass/i)).toBeInTheDocument();
  });
});
