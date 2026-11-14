export class ApiError extends Error {
  status: number;
  fieldErrors: Record<string, string>;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

function getCookie(name: string): string | undefined {
  const prefix = `${name}=`;
  const match = document.cookie.split('; ').find((c) => c.startsWith(prefix));
  return match ? decodeURIComponent(match.slice(prefix.length)) : undefined;
}

// All requests to the Spring Boot backend go through here.
export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase();
  const headers = new Headers(options.headers);
  if (!headers.has('Content-Type') && options.body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }
  if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
    let xsrf = getCookie('XSRF-TOKEN');
    if (!xsrf) {
      await fetch('/api/health', { credentials: 'same-origin' });
      xsrf = getCookie('XSRF-TOKEN');
    }
    if (xsrf) {
      headers.set('X-XSRF-TOKEN', xsrf);
    }
  }
  const res = await fetch(`/api${path}`, {
    credentials: 'same-origin',
    ...options,
    headers,
  });
  if (!res.ok) {
    let message = `API error ${res.status}`;
    let fieldErrors: Record<string, string> = {};
    try {
      const body = await res.json();
      if (body?.message) message = body.message;
      if (body?.fieldErrors) fieldErrors = body.fieldErrors;
    } catch {
      // keep fallback message when response body is not JSON
    }
    throw new ApiError(res.status, message, fieldErrors);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}
