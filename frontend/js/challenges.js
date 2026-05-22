/**
 * challenges.js — Logic for fetching and rendering challenges
 */

const challengesApp = {
    container: null,
    _initialized: false,
    
    init() {
        if (this._initialized) return;

        this.container = document.getElementById('challenges-grid');
        this.filterSelect = document.getElementById('difficulty-filter');
        
        if (this.filterSelect) {
            this.filterSelect.addEventListener('change', (e) => {
                this.loadChallenges(e.target.value);
            });
        }

        this._initialized = true;
    },

    async loadChallenges(difficulty = '') {
        if (!this.container) return;
        
        this.container.innerHTML = '<div class="loader-container"><div class="pulse-dot"></div><span>Loading challenges...</span></div>';
        
        try {
            const url = difficulty ? `/challenges?difficulty=${difficulty}` : '/challenges';
            const challenges = await api.request(url);
            
            this.renderChallenges(challenges);
        } catch (error) {
            console.error('Failed to load challenges:', error);
            this.container.innerHTML = `<div class="error-state">Failed to load challenges. Please try again.</div>`;
        }
    },

    renderChallenges(challenges) {
        if (!challenges || challenges.length === 0) {
            this.container.innerHTML = `<div class="empty-state">No challenges found for the selected difficulty.</div>`;
            return;
        }

        this.container.innerHTML = challenges.map(c => `
            <div class="challenge-card">
                <div class="challenge-header">
                    <h3 class="challenge-title">${this.escapeHtml(c.title)}</h3>
                    <span class="difficulty-badge badge-${c.difficulty.toLowerCase()}">${c.difficulty}</span>
                </div>
                <p class="challenge-desc">${this.escapeHtml(c.description.substring(0, 120))}${c.description.length > 120 ? '...' : ''}</p>
                <div class="challenge-footer">
                    <button class="btn-primary btn-sm" onclick="practiceApp.open('${c.id}')">Solve Challenge</button>
                </div>
            </div>
        `).join('');
    },

    escapeHtml(unsafe) {
        return unsafe
             .replace(/&/g, "&amp;")
             .replace(/</g, "&lt;")
             .replace(/>/g, "&gt;")
             .replace(/"/g, "&quot;")
             .replace(/'/g, "&#039;");
    }
};
