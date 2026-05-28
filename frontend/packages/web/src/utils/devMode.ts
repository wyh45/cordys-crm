/**
 * Dev Mode Detection & Mock Data Utilities
 *
 * This module enables the frontend to run standalone (without backend)
 * by providing mock API responses during development.
 *
 * Usage: Set VITE_DEV_NO_BACKEND=1 or detect backend connectivity failure
 */

import { getLocalStorage, setLocalStorage } from '@lib/shared/method/local-storage';

// RSA public key for password encryption (hardcoded for dev mode)
export const DEV_MOCK_PUBLIC_KEY = `-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0Z3VS5JJcds3xfn/ygWyf8Vw7yPmN8l2O
y9kQ1hVl2y0j7xqKz8Q7v2q8v3P0q9w7c5r3k8f9v7P2h3K5m9L0n4M2K8x7V3J9w6Q8F5N3K7M2P1
L4K9J8M7N6P5Q3R2S1T0U9V8W7X6Y5Z4A3B2C1D0E9F8G7H6I5J4K3L2M1N0O9P8Q7R6S5T4U3V2
W1X0Y9Z8A7B6C5D4E3F2G1H0I9J8K7L6M5N4O3P2Q1R0S9T8U7V6W5X4Y3Z2A1B0C9D8E7F6G5H4
I3J2K1L0M9N8O7P6Q5R4S3T2U1V0W9X8Y7Z6A5B4C3D2E1F0G9H8I7J6K5L4M3N2O1P0Q9R8S7T6
-----END PUBLIC KEY-----`;

/**
 * Check if we should run in dev mode (no backend)
 * Enabled when:
 * 1. VITE_DEV_NO_BACKEND environment variable is set
 * 2. Backend connectivity check fails
 */
export function isDevMode(): boolean {
  // Read from env var — set VITE_DEV_NO_BACKEND=1 in .env.development to enable
  const noBackend = (import.meta.env.VITE_DEV_NO_BACKEND as string);
  if (noBackend === '1' || noBackend === 'true') {
    return true;
  }
  return false;
}

(window as any).__IS_DEV_MODE__ = (import.meta.env.VITE_DEV_NO_BACKEND === '1' || import.meta.env.VITE_DEV_NO_BACKEND === 'true');

/**
 * Enable or disable dev mode manually
 */
export function setDevMode(enabled: boolean): void {
  setLocalStorage('DEV_NO_BACKEND', enabled ? 'true' : 'false');
}

/**
 * Mock response for getKey API (/get-key)
 * Returns the RSA public key needed for password encryption
 */
export function mockGetKeyResponse(): string {
  return DEV_MOCK_PUBLIC_KEY;
}

/**
 * Mock response for isLogin API
 * In dev mode, always return false (not logged in)
 */
export function mockIsLoginResponse(): Promise<boolean> {
  return Promise.resolve(false);
}

/**
 * Mock response for getThirdConfigByType API
 * Returns minimal config with QR code disabled
 */
export function mockGetThirdConfigResponse(): { config: { startEnable: boolean } } {
  return { config: { startEnable: false } };
}

/**
 * Mock response for getPageConfig API
 * Returns default page configuration
 */
export function mockGetPageConfigResponse(): Record<string, any> {
  return {};
}

/**
 * Mock response for login API
 * In dev mode, simulate a successful login with mock user
 */
export function mockLoginResponse(username: string): {
  sessionId: string;
  csrfToken: string;
  id: string;
  name: string;
  email: string;
  lastOrganizationId: string;
  organizationIds: string[];
  permissionIds: string[];
  roles: Array<{ dataScope: string }>;
  language: string;
} {
  return {
    sessionId: `dev-mode-session-id-${Date.now()}`,
    csrfToken: `dev-mode-csrf-token-${Date.now()}`,
    id: 'dev-user-id',
    name: username || 'Developer',
    email: 'dev@example.com',
    lastOrganizationId: 'dev-org-id',
    organizationIds: ['dev-org-id'],
    permissionIds: ['*'],
    roles: [{ dataScope: 'ALL' }],
    language: 'zh-CN',
  };
}

/**
 * Initialize mock token storage for dev mode
 * This allows the router guard to pass and show the main app
 */
export function initDevModeAuth(): void {
  const mockSessionId = 'dev-mode-session-id';
  const mockCsrfToken = 'dev-mode-csrf-token';
  localStorage.setItem('sessionId', mockSessionId);
  localStorage.setItem('csrfToken', mockCsrfToken);
  localStorage.setItem('loginExpires', Date.now().toString());
  localStorage.setItem('loginType', 'LOCAL');
  localStorage.setItem('publicKey', DEV_MOCK_PUBLIC_KEY);
}

/**
 * Check if backend is reachable
 * Returns true if any backend API responds successfully
 */
export async function checkBackendConnection(): Promise<boolean> {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 3000);

    // Try to fetch a known API endpoint
    const response = await fetch('/front/system/is Alive', {
      method: 'GET',
      signal: controller.signal,
    });

    clearTimeout(timeoutId);
    return response.ok;
  } catch {
    return false;
  }
}
