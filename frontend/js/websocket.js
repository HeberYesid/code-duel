/**
 * websocket.js — STOMP over WebSocket connection manager.
 *
 * Encapsulates ALL WebSocket lifecycle:
 * - Connects only when entering the Arena section
 * - Disconnects when leaving the Arena
 * - Auto-reconnects on temporary network drops (5s delay)
 *
 * Uses @stomp/stompjs v7 (loaded via CDN as global StompJs).
 */
const ws = {
    /** @type {StompJs.Client|null} */
    _client: null,

    /** @type {boolean} */
    _connected: false,

    /** @type {Function|null} */
    _onDisconnectCallback: null,

    /** @type {Function|null} */
    _onConnectCallback: null,

    /**
     * Connect to the STOMP broker.
     * JWT is sent as a query parameter in the WebSocket URL.
     * @returns {Promise<void>} resolves when connected, rejects on auth failure
     */
    connect() {
        return new Promise((resolve, reject) => {
            const token = api.getToken();
            if (!token) {
                reject(new Error('No auth token available'));
                return;
            }

            // Prevent double connections
            if (this._client && this._connected) {
                resolve();
                return;
            }

            const wsBase = window.CODE_DUEL_CONFIG?.wsBase || 'ws://localhost:8080/ws';
            const brokerURL = `${wsBase}?token=${encodeURIComponent(token)}`;

            this._client = new StompJs.Client({
                brokerURL: brokerURL,
                reconnectDelay: 5000,
                heartbeatIncoming: 10000,
                heartbeatOutgoing: 10000,
                debug: (msg) => {
                    // Uncomment for STOMP protocol debugging:
                    // console.debug('[STOMP]', msg);
                },
            });

            this._client.onConnect = () => {
                console.log('[WS] Connected to STOMP broker');
                this._connected = true;
                if (this._onConnectCallback) {
                    this._onConnectCallback();
                }
                resolve();
            };

            this._client.onStompError = (frame) => {
                console.error('[WS] STOMP error:', frame.headers['message']);
                console.error('[WS] Details:', frame.body);
                this._connected = false;
                reject(new Error(frame.headers['message'] || 'STOMP connection error'));
            };

            this._client.onWebSocketClose = (event) => {
                console.warn('[WS] WebSocket closed:', event.reason || 'no reason');
                const wasConnected = this._connected;
                this._connected = false;
                if (wasConnected && this._onDisconnectCallback) {
                    this._onDisconnectCallback(event);
                }
            };

            this._client.activate();
        });
    },

    /**
     * Cleanly disconnect from the STOMP broker.
     */
    disconnect() {
        if (this._client) {
            this._client.deactivate();
            this._client = null;
            this._connected = false;
            console.log('[WS] Disconnected');
        }
    },

    /**
     * Subscribe to a STOMP destination.
     * @param {string} destination - e.g., '/user/queue/matchmaking'
     * @param {Function} callback - receives the parsed message body
     * @returns {object|null} subscription object (call .unsubscribe() to stop)
     */
    subscribe(destination, callback) {
        if (!this._client || !this._connected) {
            console.error('[WS] Cannot subscribe: not connected');
            return null;
        }

        return this._client.subscribe(destination, (message) => {
            try {
                const body = JSON.parse(message.body);
                callback(body);
            } catch (e) {
                console.error('[WS] Failed to parse message:', e);
                callback(message.body);
            }
        });
    },

    /**
     * Send a message to a STOMP destination.
     * @param {string} destination - e.g., '/app/matchmaking/join'
     * @param {object} body - will be JSON-stringified
     */
    send(destination, body = {}) {
        if (!this._client || !this._connected) {
            console.error('[WS] Cannot send: not connected');
            return;
        }

        this._client.publish({
            destination: destination,
            body: JSON.stringify(body),
        });
    },

    /**
     * Check if currently connected.
     * @returns {boolean}
     */
    isConnected() {
        return this._connected;
    },

    /**
     * Register a callback for unexpected disconnections.
     * @param {Function} callback
     */
    onDisconnect(callback) {
        this._onDisconnectCallback = callback;
    },

    /**
     * Register a callback for successful connections (including reconnects).
     * @param {Function} callback
     */
    onConnect(callback) {
        this._onConnectCallback = callback;
    },
};
