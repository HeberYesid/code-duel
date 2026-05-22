/**
 * dashboard.js — Competitive dashboard widgets for Phase 6.
 */

const dashboardApp = {
    _initialized: false,
    _notificationsOpen: false,
    _statsContainer: null,
    _leaderboardContainer: null,
    _notificationsList: null,
    _notificationsSummary: null,
    _notificationsBadge: null,
    _panel: null,
    _backdrop: null,

    init() {
        if (this._initialized) return;

        this._statsContainer = document.getElementById('profile-stats');
        this._leaderboardContainer = document.getElementById('leaderboard-summary');
        this._notificationsList = document.getElementById('notifications-list');
        this._notificationsSummary = document.getElementById('notifications-summary');
        this._notificationsBadge = document.getElementById('notifications-badge');
        this._panel = document.getElementById('notifications-panel');
        this._backdrop = document.getElementById('notifications-backdrop');

        this._initialized = true;
    },

    async loadDashboard() {
        this.init();
        this._renderLoadingState();

        try {
            const [stats, leaderboard, notifications] = await Promise.all([
                api.get('/profile/me/stats'),
                api.get('/leaderboard?limit=10'),
                api.get('/notifications'),
            ]);

            this._renderStats(stats);
            this._renderLeaderboard(leaderboard);
            this._renderNotifications(notifications);
        } catch (error) {
            console.error('Failed to load dashboard widgets:', error);
            this._renderErrorState(error.message || 'Failed to load dashboard data.');
        }
    },

    async refreshNotifications() {
        try {
            const notifications = await api.get('/notifications');
            this._renderNotifications(notifications);
        } catch (error) {
            console.error('Failed to refresh notifications:', error);
        }
    },

    async toggleNotifications() {
        this.init();
        this._notificationsOpen = !this._notificationsOpen;
        this._panel.classList.toggle('open', this._notificationsOpen);
        this._backdrop.classList.toggle('active', this._notificationsOpen);
        this._panel.setAttribute('aria-hidden', String(!this._notificationsOpen));

        if (this._notificationsOpen) {
            await this.refreshNotifications();
        }
    },

    closeNotifications() {
        this.init();
        this._notificationsOpen = false;
        this._panel.classList.remove('open');
        this._backdrop.classList.remove('active');
        this._panel.setAttribute('aria-hidden', 'true');
    },

    async markAllAsRead() {
        try {
            await api.post('/notifications/mark-all-read', {});
            await this.refreshNotifications();
        } catch (error) {
            console.error('Failed to mark notifications as read:', error);
        }
    },

    _renderLoadingState() {
        if (this._statsContainer) {
            this._statsContainer.innerHTML = '<div class="loader-container"><div class="pulse-dot"></div><span>Loading stats...</span></div>';
        }
        if (this._leaderboardContainer) {
            this._leaderboardContainer.innerHTML = '<div class="loader-container"><div class="pulse-dot"></div><span>Loading leaderboard...</span></div>';
        }
    },

    _renderErrorState(message) {
        const content = `<div class="error-state">${this._escapeHtml(message)}</div>`;
        if (this._statsContainer) this._statsContainer.innerHTML = content;
        if (this._leaderboardContainer) this._leaderboardContainer.innerHTML = content;
    },

    _renderStats(stats) {
        if (!this._statsContainer) return;

        const items = [
            { label: 'ELO', value: stats.elo, accent: 'accent' },
            { label: 'Wins', value: stats.wins },
            { label: 'Losses', value: stats.losses },
            { label: 'Draws', value: stats.draws },
            { label: 'Duels Played', value: stats.duelsPlayed },
        ];

        this._statsContainer.innerHTML = items.map(item => `
            <div class="stat-tile ${item.accent ? 'stat-tile--accent' : ''}">
                <span class="stat-label">${this._escapeHtml(String(item.label))}</span>
                <strong class="stat-value">${this._escapeHtml(String(item.value))}</strong>
            </div>
        `).join('');
    },

    _renderLeaderboard(leaderboard) {
        if (!this._leaderboardContainer) return;

        const entries = (leaderboard.entries || []).map(entry => `
            <div class="leaderboard-row">
                <span class="leaderboard-rank">#${entry.rank}</span>
                <span class="leaderboard-user">${this._escapeHtml(entry.username)}</span>
                <span class="leaderboard-elo">${entry.elo}</span>
            </div>
        `).join('');

        const currentUser = leaderboard.currentUser ? `
            <div class="leaderboard-current-user">
                <span class="leaderboard-current-label">You</span>
                <span class="leaderboard-current-name">${this._escapeHtml(leaderboard.currentUser.username)}</span>
                <span class="leaderboard-current-rank">#${leaderboard.currentUser.rank}</span>
                <span class="leaderboard-current-elo">${leaderboard.currentUser.elo} ELO</span>
            </div>
        ` : '';

        this._leaderboardContainer.innerHTML = `
            <div class="leaderboard-list">${entries || '<div class="empty-state">No ranked players yet.</div>'}</div>
            ${currentUser}
        `;
    },

    _renderNotifications(payload) {
        const unreadCount = payload.unreadCount || 0;
        const notifications = payload.notifications || [];

        if (this._notificationsBadge) {
            this._notificationsBadge.textContent = unreadCount;
            this._notificationsBadge.style.display = unreadCount > 0 ? 'inline-flex' : 'none';
        }

        if (this._notificationsSummary) {
            this._notificationsSummary.textContent = unreadCount > 0
                ? `${unreadCount} unread notification${unreadCount === 1 ? '' : 's'}`
                : 'No unread notifications';
        }

        if (!this._notificationsList) return;

        if (notifications.length === 0) {
            this._notificationsList.innerHTML = '<div class="empty-state">No notifications yet.</div>';
            return;
        }

        this._notificationsList.innerHTML = notifications.map(notification => `
            <article class="notification-item ${notification.read ? 'notification-item--read' : 'notification-item--unread'}">
                <div class="notification-item-header">
                    <strong>${this._escapeHtml(notification.title)}</strong>
                    <span class="notification-status">${notification.read ? 'Read' : 'Unread'}</span>
                </div>
                <p class="notification-message">${this._escapeHtml(notification.message)}</p>
            </article>
        `).join('');
    },

    _escapeHtml(value) {
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    },
};
