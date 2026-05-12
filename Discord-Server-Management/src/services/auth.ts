export type SocialProvider = 'discord' | 'google' | 'github';

export type AuthUser = {
  id: number;
  email: string;
  username: string;
  age: number | null;
  avatarUrl: string | null;
  role: 'USER' | 'ADMIN';
  status: 'ACTIVE' | 'DISABLED' | 'BANNED';
  emailVerified: boolean;
  createdAt: string | null;
  updatedAt: string | null;
};

export type AuthResponse = {
  message: string;
  accessToken: string;
  user: AuthUser;
};

export type RegisterPayload = {
  email: string;
  username: string;
  password: string;
  age: number | null;
};

export type LoginPayload = {
  email: string;
  password: string;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export async function login(payload: LoginPayload) {
  return requestAuth('/api/auth/login', payload);
}

export async function register(payload: RegisterPayload) {
  return requestAuth('/api/auth/register', payload);
}

export async function getCurrentUser(accessToken: string) {
  const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(data?.message ?? 'Could not load current user');
  }

  return data as AuthUser;
}

export function loginWithProvider(provider: SocialProvider) {
  window.location.href = `${API_BASE_URL}/oauth2/authorization/${provider}`;
}

async function requestAuth(path: string, payload: LoginPayload | RegisterPayload) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(data?.message ?? 'Authentication request failed');
  }

  return data as AuthResponse;
}
