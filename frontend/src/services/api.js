import axios from 'axios';

const GATEWAY_URL = 'http://localhost:8765';

const api = axios.create({
  baseURL: GATEWAY_URL,
});

let credentials = null;

export function setCredentials(email, password) {
  credentials = { email, password };
}

export function clearCredentials() {
  credentials = null;
}

api.interceptors.request.use((config) => {
  if (credentials) {
    const token = btoa(`${credentials.email}:${credentials.password}`);
    config.headers.Authorization = `Basic ${token}`;
  }
  return config;
});

export function basicAuthHeader(email, password) {
  return { Authorization: `Basic ${btoa(`${email}:${password}`)}` };
}

function looksTechnical(text) {
  return text.length > 150 || /https?:\/\//.test(text) || /error_code|exception|stack ?trace/i.test(text);
}

// Only the backend's own {status, message, timestamp} shape (and short, clearly
// human-written plain-text bodies) are trusted for direct display. Anything else -
// raw exception text, an external API's error body, a 5xx from an upstream
// dependency like CoinGecko - is replaced with a generic message so technical
// details never reach the user.
export function getErrorMessage(err) {
  if (err.response) {
    const { status, data } = err.response;

    if (data && typeof data === 'object' && typeof data.message === 'string') {
      return data.message;
    }

    if (status >= 500) {
      return 'This service is temporarily unavailable. Please try again in a moment.';
    }

    if (typeof data === 'string' && data.length > 0 && !looksTechnical(data)) {
      return data;
    }

    return `Request failed (${status}). Please try again.`;
  }
  return 'Unable to reach the server. Please check your connection and try again.';
}

export default api;
