/**
 * practice.js — Practice mode: load challenge, submit code, display results.
 */

const practiceApp = {
    currentChallenge: null,

    /**
     * Navigate to the practice view for a given challenge.
     */
    async open(challengeId) {
        navigateTo('practice');

        // Show loading state
        document.getElementById('practice-challenge-info').innerHTML =
            '<div class="loader-container"><div class="pulse-dot"></div><span>Loading challenge...</span></div>';
        document.getElementById('practice-results').innerHTML = '';
        document.getElementById('practice-results').style.display = 'none';

        try {
            const challenge = await api.get(`/challenges/${challengeId}`);
            this.currentChallenge = challenge;
            this.renderChallengeInfo(challenge);
            await codeEditor.init('editor-container');
            codeEditor.setValue('# Write your Python solution here\n\n');
        } catch (error) {
            console.error('Failed to load challenge:', error);
            document.getElementById('practice-challenge-info').innerHTML =
                '<div class="error-state">Failed to load challenge details.</div>';
        }
    },

    /**
     * Render the challenge description panel.
     */
    renderChallengeInfo(challenge) {
        const info = document.getElementById('practice-challenge-info');
        info.innerHTML = `
            <div class="practice-challenge-header">
                <h2 class="practice-title">${this.escapeHtml(challenge.title)}</h2>
                <span class="difficulty-badge badge-${challenge.difficulty.toLowerCase()}">${challenge.difficulty}</span>
            </div>
            <div class="practice-description">${this.formatDescription(challenge.description)}</div>
            <div class="practice-meta">
                <span class="meta-item">🧪 ${challenge.testCaseCount} test cases</span>
                <span class="meta-item">🐍 Python</span>
            </div>
        `;
    },

    /**
     * Submit the current code for execution.
     */
    async submit() {
        if (!this.currentChallenge) return;

        const code = codeEditor.getValue();
        if (!code.trim()) {
            this.showMessage('Write some code before submitting!', 'error');
            return;
        }

        const submitBtn = document.getElementById('practice-submit-btn');
        const resultsPanel = document.getElementById('practice-results');

        // Loading state
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="btn-loader"></span> Running...';
        resultsPanel.style.display = 'block';
        resultsPanel.innerHTML = '<div class="loader-container"><div class="pulse-dot"></div><span>Executing code...</span></div>';

        try {
            const response = await api.post('/submissions/practice', {
                code: code,
                challengeId: this.currentChallenge.id,
            });

            this.renderResults(response);
        } catch (error) {
            console.error('Submission failed:', error);
            resultsPanel.innerHTML = `<div class="error-state">Submission failed: ${this.escapeHtml(error.message)}</div>`;
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '▶ Run Code';
        }
    },

    /**
     * Render execution results.
     */
    renderResults(response) {
        const panel = document.getElementById('practice-results');
        if (!response.testResults || response.testResults.length === 0) {
            panel.innerHTML = '<div class="error-state">No test results returned.</div>';
            return;
        }

        const statusClass = response.overallStatus === 'ACCEPTED' ? 'result-accepted' : 'result-failed';
        const statusIcon = response.overallStatus === 'ACCEPTED' ? '✅' : '❌';
        const statusLabel = response.overallStatus.replace(/_/g, ' ');

        let html = `
            <div class="results-header ${statusClass}">
                <span class="results-status">${statusIcon} ${statusLabel}</span>
                <span class="results-time">${response.executionTimeMs}ms total</span>
            </div>
            <div class="results-list">
        `;

        for (const tr of response.testResults) {
            const icon = this.statusIcon(tr.status);
            const badgeClass = this.statusBadgeClass(tr.status);

            html += `
                <div class="test-result-item ${badgeClass}">
                    <div class="test-result-header">
                        <span class="test-order">${icon} Test ${tr.testOrder}</span>
                        <span class="test-status-badge ${badgeClass}">${tr.status.replace(/_/g, ' ')}</span>
                        <span class="test-time">${tr.executionTimeMs}ms</span>
                    </div>
            `;

            // Show details for non-accepted tests
            if (tr.status !== 'ACCEPTED') {
                html += `<div class="test-result-details">`;
                if (tr.input) {
                    html += `<div class="detail-row"><span class="detail-label">Input:</span><pre class="detail-value">${this.escapeHtml(tr.input)}</pre></div>`;
                }
                if (tr.expected) {
                    html += `<div class="detail-row"><span class="detail-label">Expected:</span><pre class="detail-value">${this.escapeHtml(tr.expected)}</pre></div>`;
                }
                if (tr.actual !== undefined && tr.actual !== null) {
                    html += `<div class="detail-row"><span class="detail-label">Got:</span><pre class="detail-value">${this.escapeHtml(tr.actual)}</pre></div>`;
                }
                html += `</div>`;
            }

            // Show stderr if present
            if (tr.stderr) {
                html += `
                    <details class="stderr-details">
                        <summary class="stderr-summary">⚠ stderr output</summary>
                        <pre class="stderr-content">${this.escapeHtml(tr.stderr)}</pre>
                    </details>
                `;
            }

            html += `</div>`;
        }

        html += `</div>`;
        panel.innerHTML = html;
    },

    /**
     * Go back to the dashboard.
     */
    goBack() {
        codeEditor.dispose();
        this.currentChallenge = null;
        navigateTo('dashboard');
    },

    // --- Helpers ---

    statusIcon(status) {
        switch (status) {
            case 'ACCEPTED': return '✅';
            case 'WRONG_ANSWER': return '❌';
            case 'TIME_LIMIT_EXCEEDED': return '⏱️';
            case 'RUNTIME_ERROR': return '💥';
            default: return '❓';
        }
    },

    statusBadgeClass(status) {
        switch (status) {
            case 'ACCEPTED': return 'status-accepted';
            case 'WRONG_ANSWER': return 'status-wrong';
            case 'TIME_LIMIT_EXCEEDED': return 'status-tle';
            case 'RUNTIME_ERROR': return 'status-error';
            default: return '';
        }
    },

    showMessage(msg, type) {
        const panel = document.getElementById('practice-results');
        panel.style.display = 'block';
        panel.innerHTML = `<div class="${type === 'error' ? 'error-state' : 'loader-container'}">${this.escapeHtml(msg)}</div>`;
    },

    formatDescription(desc) {
        return this.escapeHtml(desc).replace(/\\n/g, '<br>').replace(/\n/g, '<br>');
    },

    escapeHtml(unsafe) {
        if (!unsafe) return '';
        return unsafe
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    },
};
