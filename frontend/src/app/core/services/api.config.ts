export const API_BASE_URL = resolveApiBaseUrl();
export const WS_BASE_URL = resolveWsBaseUrl();

function resolveApiBaseUrl(): string {
  if (typeof window === 'undefined') {
    return 'http://localhost:8080/api';
  }
  // Local `ng serve` → talk to backend directly.
  if (window.location.port === '4200' && !window.location.pathname.startsWith('/api')) {
    return 'http://localhost:8080/api';
  }
  // Docker / nginx same-origin proxy.
  return `${window.location.origin}/api`;
}

function resolveWsBaseUrl(): string {
  if (typeof window === 'undefined') {
    return 'ws://localhost:8080/ws';
  }
  if (window.location.port === '4200') {
    return 'ws://localhost:8080/ws';
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws`;
}
