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

export function getErrorMessage(err) {
  if (err.response) {
    const data = err.response.data;
    if (data && typeof data === 'object' && data.message) {
      return data.message;
    }
    if (typeof data === 'string' && data.length > 0) {
      return data;
    }
    return `Error (${err.response.status})`;
  }
  return err.message || 'Unknown error';
}

export default api;
