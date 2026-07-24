/**
 * config.js — Environment-aware configuration.
 */
(function () {
    const hostname = window.location.hostname;
    const isLocal = hostname === 'localhost' || hostname === '127.0.0.1';

    window.CODE_DUEL_CONFIG = {
        apiBase: isLocal ? 'http://localhost:8080/api' : '/api',
        wsBase: isLocal
            ? 'ws://localhost:8080/ws'
            : (window.location.protocol === 'https:' ? 'wss://' : 'ws://')
                + window.location.host + '/ws',
    };
})();

