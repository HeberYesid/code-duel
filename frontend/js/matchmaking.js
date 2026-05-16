/**
 * matchmaking.js — Arena UI logic and matchmaking flow.
 *
 * States:
 *   idle      → Difficulty selector visible, waiting for user to pick
 *   searching → Timer counting up, cancel button visible, waiting for match
 *   matched   → Opponent found, 3-second countdown before arena transition
 *
 * STOMP subscriptions:
 *   /user/queue/matchmaking → match found notification
 *   /user/queue/errors      → error messages from server
 */
const matchmakingApp = {
    /** @type {'idle'|'searching'|'matched'} */
    _state: 'idle',

    /** @type {string|null} Current difficulty being searched */
    _currentDifficulty: null,

    /** @type {number|null} setInterval ID for the search timer */
    _timerInterval: null,

    /** @type {number} Elapsed seconds while searching */
    _elapsedSeconds: 0,

    /** @type {number|null} setInterval ID for the match countdown */
    _countdownInterval: null,

    /** @type {object|null} STOMP subscription for match notifications */
    _matchSub: null,

    /** @type {object|null} STOMP subscription for error messages */
    _errorSub: null,

    /**
     * Initialize the Arena view.
     * Connects WebSocket and sets up subscriptions.
     */
    async init() {
        this._setState('idle');

        try {
            await ws.connect();
            this._setupSubscriptions();
        } catch (err) {
            console.error('[Matchmaking] WebSocket connection failed:', err);
            this._showError('Connection failed. Please try again.');
        }

        // Handle reconnects — re-subscribe
        ws.onConnect(() => {
            console.log('[Matchmaking] Reconnected — restoring subscriptions');
            this._setupSubscriptions();
        });

        ws.onDisconnect(() => {
            console.warn('[Matchmaking] Disconnected — attempting reconnect...');
        });
    },

    /**
     * Set up STOMP subscriptions for match results and errors.
     */
    _setupSubscriptions() {
        // Clean previous subscriptions if any
        if (this._matchSub) this._matchSub.unsubscribe();
        if (this._errorSub) this._errorSub.unsubscribe();

        this._matchSub = ws.subscribe('/user/queue/matchmaking', (data) => {
            this._onMatchFound(data);
        });

        this._errorSub = ws.subscribe('/user/queue/errors', (data) => {
            if (data && data.message) {
                console.warn('[Matchmaking] Server error:', data.message);
                this._showError(data.message);
                this._setState('idle');
            }
        });
    },

    /**
     * User selected a difficulty — start searching for a match.
     * @param {string} difficulty — 'EASY', 'MEDIUM', or 'HARD'
     */
    searchMatch(difficulty) {
        if (!ws.isConnected()) {
            this._showError('Not connected. Please wait...');
            return;
        }

        this._currentDifficulty = difficulty;
        this._setState('searching');

        // Update UI
        const badge = document.getElementById('searching-difficulty');
        if (badge) {
            badge.textContent = difficulty;
            badge.className = `searching-badge badge-${difficulty.toLowerCase()}`;
        }

        // Start the timer
        this._elapsedSeconds = 0;
        this._updateTimerDisplay();
        this._timerInterval = setInterval(() => {
            this._elapsedSeconds++;
            this._updateTimerDisplay();
        }, 1000);

        // Send join request via STOMP
        ws.send('/app/matchmaking/join', { difficulty: difficulty });
    },

    /**
     * User cancelled the search — leave queue and return to idle.
     */
    cancelSearch() {
        this._stopTimer();

        // Tell the server to remove us from the queue
        if (ws.isConnected()) {
            ws.send('/app/matchmaking/leave');
        }

        this._setState('idle');
    },

    /**
     * Handler for when a match is found.
     * Shows the opponent and starts a 3-second countdown.
     * @param {{ matchId: string, opponentUsername: string, difficulty: string }} data
     */
    _onMatchFound(data) {
        this._stopTimer();
        this._setState('matched');

        console.log('[Matchmaking] Match found!', data);

        // Display opponent info
        const myUsername = api.getUsername();
        const vsPlayers = document.getElementById('match-players');
        if (vsPlayers) {
            vsPlayers.innerHTML = `
                <span class="player-name player-self">${this._escapeHtml(myUsername)}</span>
                <span class="vs-label">VS</span>
                <span class="player-name player-opponent">${this._escapeHtml(data.opponentUsername)}</span>
            `;
        }

        // Display matched difficulty
        const diffLabel = document.getElementById('match-difficulty');
        if (diffLabel) {
            diffLabel.textContent = data.difficulty;
            diffLabel.className = `match-diff-badge badge-${data.difficulty.toLowerCase()}`;
        }

        // Start countdown: 3 → 2 → 1 → redirect
        let countdown = 3;
        const countdownEl = document.getElementById('match-countdown-number');
        if (countdownEl) {
            countdownEl.textContent = countdown;
        }

        this._countdownInterval = setInterval(() => {
            countdown--;
            if (countdownEl) {
                countdownEl.textContent = countdown;
                countdownEl.classList.add('countdown-pulse');
                setTimeout(() => countdownEl.classList.remove('countdown-pulse'), 300);
            }

            if (countdown <= 0) {
                clearInterval(this._countdownInterval);
                this._countdownInterval = null;
                // TODO Phase 5: navigateTo('duel', data.matchId);
                console.log('[Matchmaking] Countdown complete. Ready for Phase 5 redirect.', data.matchId);
            }
        }, 1000);
    },

    /**
     * Clean up everything when leaving the Arena view.
     */
    destroy() {
        this._stopTimer();
        this._stopCountdown();

        // Cancel search if active
        if (this._state === 'searching' && ws.isConnected()) {
            ws.send('/app/matchmaking/leave');
        }

        // Clean subscriptions
        if (this._matchSub) {
            this._matchSub.unsubscribe();
            this._matchSub = null;
        }
        if (this._errorSub) {
            this._errorSub.unsubscribe();
            this._errorSub = null;
        }

        // Disconnect WebSocket
        ws.disconnect();

        this._state = 'idle';
        this._currentDifficulty = null;
    },

    // ---- Internal Helpers ----

    /**
     * Sets the view state by toggling CSS classes on the arena content container.
     * @param {'idle'|'searching'|'matched'} state
     */
    _setState(state) {
        this._state = state;
        const container = document.getElementById('arena-content');
        if (!container) return;

        container.classList.remove('arena--idle', 'arena--searching', 'arena--matched');
        container.classList.add(`arena--${state}`);

        // Clear error on state change
        const errorEl = document.getElementById('arena-error');
        if (errorEl) errorEl.style.display = 'none';
    },

    _updateTimerDisplay() {
        const timerEl = document.getElementById('search-timer');
        if (!timerEl) return;

        const minutes = Math.floor(this._elapsedSeconds / 60).toString().padStart(2, '0');
        const seconds = (this._elapsedSeconds % 60).toString().padStart(2, '0');
        timerEl.textContent = `${minutes}:${seconds}`;
    },

    _stopTimer() {
        if (this._timerInterval) {
            clearInterval(this._timerInterval);
            this._timerInterval = null;
        }
    },

    _stopCountdown() {
        if (this._countdownInterval) {
            clearInterval(this._countdownInterval);
            this._countdownInterval = null;
        }
    },

    _showError(message) {
        const errorEl = document.getElementById('arena-error');
        if (errorEl) {
            errorEl.textContent = message;
            errorEl.style.display = 'block';
        }
    },

    /**
     * Escape HTML to prevent XSS when injecting usernames.
     */
    _escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    },
};
