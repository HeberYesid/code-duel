/**
 * api.js — HTTP client wrapper with JWT interceptor.
 * All backend calls go through this module.
 */
const API_BASE = 'http://localhost:8080/api';

const api = {
    /**
     * Get the stored JWT token.
     */
    getToken() {
        return localStorage.getItem('token');
    },

    /**
     * Store auth data after login/register.
     */
    setAuth(data) {
        localStorage.setItem('token', data.token);
        localStorage.setItem('userId', data.userId);
        localStorage.setItem('username', data.username);
    },

    /**
     * Clear stored auth data.
     */
    clearAuth() {
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        localStorage.removeItem('username');
    },

    /**
     * Check if user is authenticated.
     */
    isAuthenticated() {
        return !!this.getToken();
    },

    /**
     * Get stored username.
     */
    getUsername() {
        return localStorage.getItem('username');
    },

    /**
     * Make an authenticated HTTP request.
     * @param {string} endpoint - API path (e.g., '/auth/login')
     * @param {object} options - fetch options
     * @returns {Promise<object>} parsed JSON response
     */
    async request(endpoint, options = {}) {
        const url = `${API_BASE}${endpoint}`;
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers,
        };

        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const response = await fetch(url, {
            ...options,
            headers,
        });

        const data = await response.json().catch(() => null);

        if (!response.ok) {
            const message = data?.message || `Error ${response.status}`;
            throw new Error(message);
        }

        return data;
    },

    /**
     * POST shorthand.
     */
    post(endpoint, body) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(body),
        });
    },

    /**
     * GET shorthand.
     */
    get(endpoint) {
        return this.request(endpoint, {
            method: 'GET',
        });
    },
};
