/**
 * app.js — Main application router and initialization.
 */

function navigateTo(viewName) {
    document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
    const view = document.getElementById(`${viewName}-view`);
    if (view) {
        view.classList.add('active');
    }

    if (viewName === 'dashboard') {
        const username = api.getUsername();
        document.getElementById('nav-username').textContent = username;
        document.getElementById('welcome-username').textContent = username;
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
