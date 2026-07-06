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

export default api;
