/**
 * app.js — Main application router and initialization.
 */

/**
 * Previous view name — used to clean up resources when navigating away.
 * @type {string|null}
 */
let _currentView = null;

function navigateTo(viewName, data) {
    // Clean up previous view resources
    if (_currentView === 'arena' && viewName !== 'arena' && viewName !== 'duel') {
        // Only destroy matchmaking if NOT going to duel (WS stays connected)
        matchmakingApp.destroy();
    }
    if (_currentView === 'duel' && viewName !== 'duel') {
        duelApp.destroy();
        // Disconnect WebSocket when leaving duel (unless going back to arena)
        if (viewName !== 'arena') {
            ws.disconnect();
        }
    }

    _currentView = viewName;

    document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
    const view = document.getElementById(`${viewName}-view`);
    if (view) {
        view.classList.add('active');
    }

    if (viewName === 'dashboard') {
        const username = api.getUsername();
        document.getElementById('nav-username').textContent = username;
        document.getElementById('welcome-username').textContent = username;
        
        // Load challenges when dashboard is opened
        challengesApp.init();
        challengesApp.loadChallenges();
    }

    if (viewName === 'arena') {
        matchmakingApp.init();
    }

    if (viewName === 'duel' && data) {
        duelApp.init(data);
    }
}

// On page load, check auth state
document.addEventListener('DOMContentLoaded', () => {
    if (api.isAuthenticated()) {
        navigateTo('dashboard');
    } else {
        navigateTo('auth');
    }
});
