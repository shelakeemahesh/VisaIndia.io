import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { DashboardPage } from '../pages/DashboardPage';
import * as tripsApi from '../api/trips';

vi.mock('../api/trips');

describe('DashboardPage — Traveler Portal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(tripsApi.getTripByReference).mockResolvedValue({
      trip: {
        id: 'trip-1',
        referenceId: 'VS-2026-6134',
        applicantEmail: 'kenji.sato@tokyo-travel.jp',
        applicantName: 'Kenji Sato',
        nationality: 'JPN',
        categoryName: 'e-Tourist Visa (30 Days)',
        status: 'APPROVED',
        totalAmount: 25.0,
        currency: 'USD',
        arrivalDate: '2026-11-18',
        portOfArrival: 'Delhi (DEL) - Indira Gandhi International',
        etaNumber: 'ETA-IN-2026-994821',
        qrToken: 'VS-QR-98174291847192',
        version: 1,
        createdAt: '2026-10-10T14:30:00Z',
        updatedAt: '2026-10-10T14:30:00Z',
        submittedAt: '2026-10-10T14:30:00Z',
      },
      modules: [],
    });
  });

  it('renders applicant welcome banner and key metrics', async () => {
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <DashboardPage />
      </MemoryRouter>
    );

    expect(await screen.findByText(/Welcome, Kenji Sato/i)).toBeInTheDocument();
    expect(screen.getByText(/kenji.sato@tokyo-travel.jp/i)).toBeInTheDocument();
    expect(screen.getByText(/Active Applications/i)).toBeInTheDocument();
    expect(screen.getByText(/ETA Travel Passes/i)).toBeInTheDocument();
    expect(screen.getByText(/Total Fees Paid/i)).toBeInTheDocument();
  });

  it('switches tabs to ETA Passes & QR Wallet and Advisories', async () => {
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <DashboardPage />
      </MemoryRouter>
    );

    // Switch to Passes tab
    const passesTab = screen.getByRole('button', { name: /ETA Passes & QR Wallet/i });
    fireEvent.click(passesTab);

    expect(await screen.findByText(/Electronic Travel Authorization Pass/i)).toBeInTheDocument();
    expect(screen.getByText(/ETA-IN-2026-994821/i)).toBeInTheDocument();

    // Switch to Advisories tab
    const advisoriesTab = screen.getByRole('button', { name: /Immigration Advisories/i });
    fireEvent.click(advisoriesTab);

    expect(await screen.findByText(/Biometric Enrolment at Indian Ports/i)).toBeInTheDocument();
    expect(screen.getByText(/37 Designated Airports & Seaports/i)).toBeInTheDocument();
  });

  it('triggers payment re-sync and updates status', async () => {
    vi.mocked(tripsApi.resyncPayment).mockResolvedValue({
      status: 'CAPTURED',
      referenceId: 'VS-2026-6134',
      newTripStatus: 'APPROVED',
      message: 'Gateway state reconciled successfully',
    });

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <DashboardPage />
      </MemoryRouter>
    );

    const resyncBtn = screen.getByRole('button', { name: /Payment Re-sync/i });
    fireEvent.click(resyncBtn);

    await waitFor(() => {
      expect(tripsApi.resyncPayment).toHaveBeenCalledWith('VS-2026-6134');
    });

    expect(await screen.findByText(/Re-sync successful/i)).toBeInTheDocument();
  });
});
