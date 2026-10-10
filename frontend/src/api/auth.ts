/**
 * Authentication and User Account Services for VisaSetu / VisaIndia.io
 */

export interface AuthUser {
  id: string;
  email: string;
  fullName: string;
  nationality?: string;
  passportNumber?: string;
  phoneNumber?: string;
  role: string;
}

export interface UserSettingsData {
  emailNotificationsEnabled: boolean;
  arrivalRemindersEnabled: boolean;
  advisoryAlertsEnabled: boolean;
  autoResyncEnabled: boolean;
  languagePreference: string;
}

export interface AuthResponse {
  status: string;
  message?: string;
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
  settings?: UserSettingsData;
}

const TOKEN_KEY = 'visasetu_auth_token';
const USER_KEY = 'visasetu_auth_user';

export const getStoredToken = (): string | null => {
  return localStorage.getItem(TOKEN_KEY);
};

export const setStoredToken = (token: string): void => {
  localStorage.setItem(TOKEN_KEY, token);
};

export const getStoredUser = (): AuthUser | null => {
  const data = localStorage.getItem(USER_KEY);
  if (!data) return null;
  try {
    return JSON.parse(data);
  } catch {
    return null;
  }
};

export const setStoredUser = (user: AuthUser | null): void => {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  } else {
    localStorage.removeItem(USER_KEY);
  }
};

export const clearAuthSession = (): void => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
};

const getHeaders = () => {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };
  const token = getStoredToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
};

export async function apiSignup(data: {
  email: string;
  fullName: string;
  password: string;
  nationality?: string;
  passportNumber?: string;
  phoneNumber?: string;
}): Promise<AuthResponse> {
  const res = await fetch('/api/v1/auth/signup', {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: 'Registration failed.' }));
    throw new Error(err.error || 'Registration failed.');
  }
  const result: AuthResponse = await res.json();
  if (result.accessToken) {
    setStoredToken(result.accessToken);
    setStoredUser(result.user);
  }
  return result;
}

export async function apiLogin(data: {
  email: string;
  password?: string;
  otp?: string;
}): Promise<AuthResponse> {
  const res = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: 'Sign in failed.' }));
    throw new Error(err.error || 'Sign in failed.');
  }
  const result: AuthResponse = await res.json();
  if (result.accessToken) {
    setStoredToken(result.accessToken);
    setStoredUser(result.user);
  }
  return result;
}

export async function apiRequestOtp(email: string, purpose: string = 'LOGIN'): Promise<{
  status: string;
  message: string;
  email: string;
  code?: string;
}> {
  const res = await fetch('/api/v1/auth/request-otp', {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify({ email, purpose }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: 'Failed to request verification code.' }));
    throw new Error(err.error || 'Failed to request verification code.');
  }
  return res.json();
}

export async function apiVerifyOtp(email: string, otp: string): Promise<AuthResponse> {
  const res = await fetch('/api/v1/auth/verify-otp', {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify({ email, otp }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: 'Invalid verification code.' }));
    throw new Error(err.error || 'Invalid verification code.');
  }
  const result: AuthResponse = await res.json();
  if (result.accessToken) {
    setStoredToken(result.accessToken);
    setStoredUser(result.user);
  }
  return result;
}

export async function apiForgotPassword(email: string): Promise<{
  status: string;
  message: string;
  code?: string;
}> {
  const res = await fetch('/api/v1/auth/forgot-password', {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify({ email }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: 'Failed to dispatch password reset code.' }));
    throw new Error(err.error || 'Failed to dispatch password reset code.');
  }
  return res.json();
}

export async function apiResetPassword(data: {
  email: string;
  otp: string;
  newPassword: string;
}): Promise<{ status: string; message: string }> {
  const res = await fetch('/api/v1/auth/reset-password', {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: 'Failed to reset password.' }));
    throw new Error(err.error || 'Failed to reset password.');
  }
  return res.json();
}

export async function apiGetMe(): Promise<AuthUser | null> {
  const token = getStoredToken();
  if (!token) return null;
  const res = await fetch('/api/v1/auth/me', {
    headers: getHeaders(),
  });
  if (!res.ok) return null;
  const data = await res.json();
  return {
    id: data.id,
    email: data.email,
    fullName: data.fullName || data.email.split('@')[0],
    nationality: data.nationality,
    passportNumber: data.passportNumber,
    role: data.role,
  };
}

export async function apiGetSettings(): Promise<UserSettingsData> {
  const res = await fetch('/api/v1/users/settings', {
    headers: getHeaders(),
  });
  if (!res.ok) {
    return {
      emailNotificationsEnabled: true,
      arrivalRemindersEnabled: true,
      advisoryAlertsEnabled: true,
      autoResyncEnabled: true,
      languagePreference: 'en',
    };
  }
  return res.json();
}

export async function apiUpdateSettings(settings: Partial<UserSettingsData>): Promise<UserSettingsData> {
  const res = await fetch('/api/v1/users/settings', {
    method: 'PUT',
    headers: getHeaders(),
    body: JSON.stringify(settings),
  });
  if (!res.ok) {
    throw new Error('Failed to update settings');
  }
  const data = await res.json();
  return data.settings || settings;
}
