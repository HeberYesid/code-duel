/**
 * app.js — Main application router and initialization.
 */

/**
 * Previous view name — used to clean up resources when navigating away.
 * @type {string|null}
 */
let _currentView = null;

function navigateTo(viewName) {
    // Clean up previous view resources
    if (_currentView === 'arena' && viewName !== 'arena') {
        matchmakingApp.destroy();
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
}

// On page load, check auth state
document.addEventListener('DOMContentLoaded', () => {
    if (api.isAuthenticated()) {
        navigateTo('dashboard');
    } else {
        navigateTo('auth');
    }
});
