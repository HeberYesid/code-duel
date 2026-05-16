/**
 * duel.js — Live duel arena: editor, submissions, progress, and end-of-match.
 *
 * Lifecycle:
 *   1. init(duelData) — called from matchmaking countdown → sets up STOMP subs, timer, editor
 *   2. submitCode()   — sends code via STOMP, shows loading
 *   3. _onSubmissionResult(data) — renders private result below editor
 *   4. _onProgressUpdate(data)   — updates HUD progress bars
 *   5. _onDuelFinished(data)     — shows end-of-match modal
 *   6. destroy()      — cleans up subscriptions, timer, editor
 *
 * STOMP subscriptions:
 *   /user/queue/duel/result           → private submission result
 *   /topic/duel/{duelId}/progress     → public progress (both players)
 *   /topic/duel/{duelId}/finished     → duel ended
 */
const duelApp = {
    _duelId: null,
    _myUsername: null,
    _opponentUsername: null,
    _totalTests: 0,
    _myBestPassed: 0,
    _opponentBestPassed: 0,
    _timerInterval: null,
    _endTime: null,
    _submitting: false,
    _attemptCount: 0,

    /** @type {object|null} STOMP subscriptions */
    _resultSub: null,
    _progressSub: null,
    _finishedSub: null,

    /** @type {number|null} timeout for opponent activity dot fade */
    _activityTimeout: null,

    /**
     * Initialize the duel view with match data.
     * @param {object} data — DuelStartMessage from server
     */
    async init(data) {
        this._duelId = data.duelId;
        this._myUsername = api.getUsername();
        this._opponentUsername = data.opponentUsername;
        this._totalTests = data.testCaseCount;
        this._myBestPassed = 0;
        this._opponentBestPassed = 0;
        this._attemptCount = 0;
        this._submitting = false;

        // Render challenge info
        this._renderChallengeInfo(data);

        // Init HUD
        document.getElementById('hud-opponent-name').textContent = this._escapeHtml(data.opponentUsername);
        this._updateHudScores();

        // Init Monaco editor
        await codeEditor.init('duel-editor-container', { disableCopyPaste: true });
        codeEditor.setValue('# Write your Python solution here\n\n');

        // Clear results
        document.getElementById('duel-results').innerHTML = '';

        // Hide end modal
        document.getElementById('duel-end-overlay').style.display = 'none';

        // Start countdown timer
        const timeoutMs = (data.timeoutMinutes || 20) * 60 * 1000;
        this._endTime = Date.now() + timeoutMs;
        this._updateTimer();
        this._timerInterval = setInterval(() => this._updateTimer(), 1000);

        // Set up STOMP subscriptions
        this._setupSubscriptions();
    },

    /**
     * Set up STOMP subscriptions for duel events.
     */
    _setupSubscriptions() {
        // Private: my submission results
        this._resultSub = ws.subscribe('/user/queue/duel/result', (data) => {
            this._onSubmissionResult(data);
        });

        // Public: progress of both players
        this._progressSub = ws.subscribe(`/topic/duel/${this._duelId}/progress`, (data) => {
            this._onProgressUpdate(data);
        });

        // Public: duel finished
        this._finishedSub = ws.subscribe(`/topic/duel/${this._duelId}/finished`, (data) => {
            this._onDuelFinished(data);
        });
    },

    /**
     * Submit code via STOMP.
     */
    submitCode() {
        if (this._submitting) return;

        const code = codeEditor.getValue();
        if (!code.trim()) return;

        this._submitting = true;
        this._attemptCount++;

        const btn = document.getElementById('duel-submit-btn');
        btn.disabled = true;
        btn.innerHTML = '<span class="btn-loader"></span> Running...';

        ws.send('/app/duel/submit', {
            duelId: this._duelId,
            code: code
        });

        // Re-enable after a short delay (actual result comes via STOMP)
        setTimeout(() => {
            btn.disabled = false;
            btn.innerHTML = '▶ Submit Code';
            this._submitting = false;
        }, 2000);
    },

    /**
     * Handle private submission result.
     */
    _onSubmissionResult(data) {
        const btn = document.getElementById('duel-submit-btn');
        btn.disabled = false;
        btn.innerHTML = '▶ Submit Code';
        this._submitting = false;

        const panel = document.getElementById('duel-results');

        const statusIcon = this._statusIcon(data.overallStatus);
        const statusClass = data.overallStatus === 'ACCEPTED' ? 'result-accepted' : 'result-failed';

        // Prepend new result (most recent at top)
        const resultDiv = document.createElement('div');
        resultDiv.className = `duel-result-item ${statusClass}`;
        resultDiv.innerHTML = `
            <div class="test-result-header">
                <span class="test-order">${statusIcon} Attempt #${this._attemptCount}</span>
                <span class="test-status-badge status-${data.overallStatus === 'ACCEPTED' ? 'accepted' : 'wrong'}">
                    ${data.testsPassedCount}/${data.totalTests} tests passed
                </span>
                <span class="test-time">${data.executionTimeMs}ms</span>
            </div>
        `;

        // Show individual test results for non-accepted submissions
        if (data.testResults && data.overallStatus !== 'ACCEPTED') {
            let detailsHtml = '<div class="test-result-details">';
            for (const tr of data.testResults) {
                if (tr.status !== 'ACCEPTED') {
                    detailsHtml += `
                        <div class="detail-row">
                            <span class="detail-label">${this._statusIcon(tr.status)} Test ${tr.testOrder}:</span>
                            <span class="test-status-badge status-${tr.status === 'ACCEPTED' ? 'accepted' : 'wrong'}">
                                ${tr.status.replace(/_/g, ' ')}
                            </span>
                        </div>`;
                    if (tr.stderr) {
                        detailsHtml += `<pre class="stderr-content">${this._escapeHtml(tr.stderr)}</pre>`;
                    }
                }
            }
            detailsHtml += '</div>';
            resultDiv.innerHTML += detailsHtml;
        }

        panel.prepend(resultDiv);

        // Update own progress in HUD
        if (data.testsPassedCount > this._myBestPassed) {
            this._myBestPassed = data.testsPassedCount;
        }
        this._updateHudScores();
    },

    /**
     * Handle public progress update.
     */
    _onProgressUpdate(data) {
        if (data.playerUsername === this._myUsername) {
            // My progress (could come from server recalculation)
            if (data.testsPassedCount > this._myBestPassed) {
                this._myBestPassed = data.testsPassedCount;
            }
        } else {
            // Opponent progress
            this._opponentBestPassed = data.testsPassedCount;

            // Show activity dot
            const dot = document.getElementById('opponent-activity');
            if (dot) {
                dot.classList.add('active');
                clearTimeout(this._activityTimeout);
                this._activityTimeout = setTimeout(() => {
                    dot.classList.remove('active');
                }, 3000);
            }
        }
        this._updateHudScores();
    },

    /**
     * Handle duel finished event.
     */
    _onDuelFinished(data) {
        // Stop timer
        this._stopTimer();

        // Determine result for current user
        const myUsername = this._myUsername;
        let resultType, title, icon;

        if (!data.winnerUsername) {
            resultType = 'draw';
            title = 'Draw!';
            icon = '🤝';
        } else if (data.winnerUsername === myUsername) {
            resultType = 'victory';
            title = 'Victory!';
            icon = '🏆';
        } else {
            resultType = 'defeat';
            title = 'Defeat';
            icon = '💀';
        }

        // Reason text
        const reasons = {
            SOLVED: 'All tests solved!',
            TIMEOUT: 'Time ran out',
            FORFEIT: 'Opponent disconnected'
        };
        let reasonText = reasons[data.finishReason] || data.finishReason;
        if (data.finishReason === 'FORFEIT' && data.winnerUsername !== myUsername) {
            reasonText = 'You disconnected';
        }

        // Show modal
        const overlay = document.getElementById('duel-end-overlay');
        const modal = document.getElementById('duel-end-modal');
        document.getElementById('end-modal-icon').textContent = icon;

        const titleEl = document.getElementById('end-modal-title');
        titleEl.textContent = title;
        titleEl.className = `end-modal-title result-${resultType}`;

        document.getElementById('end-modal-reason').textContent = reasonText;

        document.getElementById('end-modal-scores').innerHTML = `
            <div class="score-row">
                <span class="score-player">${this._escapeHtml(data.player1Username)}</span>
                <span class="score-value">${data.player1TestsPassed}/${this._totalTests}</span>
            </div>
            <div class="score-row">
                <span class="score-player">${this._escapeHtml(data.player2Username)}</span>
                <span class="score-value">${data.player2TestsPassed}/${this._totalTests}</span>
            </div>
        `;

        overlay.style.display = 'flex';
        modal.classList.add('modal-enter');
    },

    /**
     * Navigate back to dashboard after duel ends.
     */
    goToDashboard() {
        this.destroy();
        navigateTo('dashboard');
    },

    /**
     * Clean up all resources.
     */
    destroy() {
        this._stopTimer();

        // Unsubscribe STOMP
        if (this._resultSub) { this._resultSub.unsubscribe(); this._resultSub = null; }
        if (this._progressSub) { this._progressSub.unsubscribe(); this._progressSub = null; }
        if (this._finishedSub) { this._finishedSub.unsubscribe(); this._finishedSub = null; }

        // Dispose editor
        codeEditor.dispose();

        // Clear activity timeout
        clearTimeout(this._activityTimeout);

        // Reset state
        this._duelId = null;
        this._myUsername = null;
        this._opponentUsername = null;
        this._attemptCount = 0;
        this._submitting = false;
    },

    // ── Internal Helpers ──

    _renderChallengeInfo(data) {
        const info = document.getElementById('duel-challenge-info');
        info.innerHTML = `
            <div class="practice-challenge-header">
                <h2 class="practice-title">${this._escapeHtml(data.challengeTitle)}</h2>
                <span class="difficulty-badge badge-${data.difficulty.toLowerCase()}">${data.difficulty}</span>
            </div>
            <div class="practice-description">${this._formatDescription(data.challengeDescription)}</div>
            <div class="practice-meta">
                <span class="meta-item">🧪 ${data.testCaseCount} test cases</span>
                <span class="meta-item">🐍 Python</span>
                <span class="meta-item">⏱️ ${data.timeoutMinutes} min</span>
            </div>
        `;
    },

    _updateHudScores() {
        const total = this._totalTests || 1;

        document.getElementById('hud-self-score').textContent = `${this._myBestPassed}/${this._totalTests}`;
        document.getElementById('hud-self-bar').style.width = `${(this._myBestPassed / total) * 100}%`;

        document.getElementById('hud-opponent-score').textContent = `${this._opponentBestPassed}/${this._totalTests}`;
        document.getElementById('hud-opponent-bar').style.width = `${(this._opponentBestPassed / total) * 100}%`;
    },

    _updateTimer() {
        const now = Date.now();
        const remaining = Math.max(0, this._endTime - now);
        const minutes = Math.floor(remaining / 60000);
        const seconds = Math.floor((remaining % 60000) / 1000);

        const timerEl = document.getElementById('duel-timer');
        if (timerEl) {
            timerEl.textContent = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

            // Urgent styling when < 2 minutes
            if (remaining < 120000) {
                timerEl.classList.add('timer-urgent');
            } else {
                timerEl.classList.remove('timer-urgent');
            }
        }

        if (remaining <= 0) {
            this._stopTimer();
        }
    },

    _stopTimer() {
        if (this._timerInterval) {
            clearInterval(this._timerInterval);
            this._timerInterval = null;
        }
    },

    _statusIcon(status) {
        switch (status) {
            case 'ACCEPTED': return '✅';
            case 'WRONG_ANSWER': return '❌';
            case 'TIME_LIMIT_EXCEEDED': return '⏱️';
            case 'RUNTIME_ERROR': return '💥';
            default: return '❓';
        }
    },

    _formatDescription(desc) {
        return this._escapeHtml(desc).replace(/\\n/g, '<br>').replace(/\n/g, '<br>');
    },

    _escapeHtml(unsafe) {
        if (!unsafe) return '';
        return unsafe
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    },
};
