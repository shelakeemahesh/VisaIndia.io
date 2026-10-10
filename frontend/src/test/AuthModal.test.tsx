import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AuthModal } from '../components/AuthModal';
import * as authApi from '../api/auth';

vi.mock('../api/auth');

describe('AuthModal Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders sign in modal with email and password inputs', () => {
    render(
      <AuthModal
        isOpen={true}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
        initialMode="signin"
      />
    );

    expect(screen.getByText(/Traveler Portal Sign In/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/kenji.sato@tokyo-travel.jp/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Enter account password/i)).toBeInTheDocument();
  });

  it('allows switching between password and email OTP login methods', () => {
    render(
      <AuthModal
        isOpen={true}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
        initialMode="signin"
      />
    );

    const otpToggle = screen.getByRole('button', { name: /Email OTP Code/i });
    fireEvent.click(otpToggle);

    expect(screen.getByPlaceholderText(/526584/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Send Code/i })).toBeInTheDocument();
  });

  it('submits sign in and fires onSuccess callback', async () => {
    const handleSuccess = vi.fn();
    const handleClose = vi.fn();

    vi.mocked(authApi.apiLogin).mockResolvedValue({
      status: 'SUCCESS',
      accessToken: 'test-token',
      tokenType: 'Bearer',
      expiresIn: 86400,
      user: {
        id: 'user-1',
        email: 'kenji.sato@tokyo-travel.jp',
        fullName: 'Kenji Sato',
        role: 'ROLE_TRAVELER',
      },
    });

    render(
      <AuthModal
        isOpen={true}
        onClose={handleClose}
        onSuccess={handleSuccess}
        initialMode="signin"
      />
    );

    fireEvent.change(screen.getByPlaceholderText(/kenji.sato@tokyo-travel.jp/i), {
      target: { value: 'kenji.sato@tokyo-travel.jp' },
    });
    fireEvent.change(screen.getByPlaceholderText(/Enter account password/i), {
      target: { value: 'Welcome@123' },
    });

    fireEvent.click(screen.getByRole('button', { name: /Sign In to Dashboard/i }));

    await waitFor(() => {
      expect(authApi.apiLogin).toHaveBeenCalledWith({
        email: 'kenji.sato@tokyo-travel.jp',
        password: 'Welcome@123',
      });
      expect(handleSuccess).toHaveBeenCalledWith(
        expect.objectContaining({ email: 'kenji.sato@tokyo-travel.jp' })
      );
      expect(handleClose).toHaveBeenCalled();
    });
  });

  it('switches to create account mode and renders registration fields', () => {
    render(
      <AuthModal
        isOpen={true}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
        initialMode="signup"
      />
    );

    expect(screen.getByText(/Create Traveler Account/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Kenji Sato/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/traveler@domain.com/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Register Traveler Account/i })).toBeInTheDocument();
  });
});
