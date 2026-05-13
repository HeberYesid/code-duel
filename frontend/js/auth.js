/**
 * auth.js — Login and registration logic.
 */

const USERNAME_REGEX = /^[a-zA-Z0-9_]+$/;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PASSWORD_STRENGTH_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/;

function switchTab(tab) {
    const loginTab = document.getElementById('tab-login');
    const registerTab = document.getElementById('tab-register');
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const indicator = document.getElementById('tab-indicator');
    const message = document.getElementById('auth-message');
    message.style.display = 'none';

    if (tab === 'login') {
        loginTab.classList.add('active');
        registerTab.classList.remove('active');
        loginForm.classList.add('active');
        registerForm.classList.remove('active');
        indicator.classList.remove('right');
    } else {
        registerTab.classList.add('active');
        loginTab.classList.remove('active');
        registerForm.classList.add('active');
        loginForm.classList.remove('active');
        indicator.classList.add('right');
    }
}

function showAuthMessage(text, type = 'error') {
    const message = document.getElementById('auth-message');
    message.textContent = text;
    message.className = `auth-message ${type}`;
    message.style.display = 'block';
}

function setButtonLoading(btnId, loading) {
    const btn = document.getElementById(btnId);
    const text = btn.querySelector('.btn-text');
    const loader = btn.querySelector('.btn-loader');
    btn.disabled = loading;
    text.style.display = loading ? 'none' : 'inline';
    loader.style.display = loading ? 'inline-block' : 'none';
}

function validateLoginInput(username, password) {
    if (!username || !password) {
        return 'Please fill in all fields';
    }
    if (username.length < 3 || username.length > 50 || !USERNAME_REGEX.test(username)) {
        return 'Username must be 3-50 chars and only letters, numbers or underscores';
    }
    if (password.length < 6 || password.length > 100) {
        return 'Password must be between 6 and 100 characters';
    }
    return null;
}

function validateRegisterInput(username, email, password) {
    if (!username || !email || !password) {
        return 'Please fill in all fields';
    }
    if (username.length < 3 || username.length > 50 || !USERNAME_REGEX.test(username)) {
        return 'Username must be 3-50 chars and only letters, numbers or underscores';
    }
    if (email.length > 100 || !EMAIL_REGEX.test(email)) {
        return 'Please enter a valid email address';
    }
    if (password.length < 6 || password.length > 100) {
        return 'Password must be between 6 and 100 characters';
    }
    if (!PASSWORD_STRENGTH_REGEX.test(password)) {
        return 'Password must include uppercase, lowercase and a number';
    }
    return null;
}

async function handleLogin(event) {
    event.preventDefault();
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;
    const validationError = validateLoginInput(username, password);
    if (validationError) {
        showAuthMessage(validationError);
        return;
    }
    setButtonLoading('login-btn', true);
    try {
        const data = await api.post('/auth/login', { username, password });
        api.setAuth(data);
        showAuthMessage('Login successful!', 'success');
        setTimeout(() => navigateTo('dashboard'), 500);
    } catch (error) {
        showAuthMessage(error.message);
    } finally {
        setButtonLoading('login-btn', false);
    }
}

async function handleRegister(event) {
    event.preventDefault();
    const username = document.getElementById('register-username').value.trim();
    const email = document.getElementById('register-email').value.trim();
    const password = document.getElementById('register-password').value;
    const validationError = validateRegisterInput(username, email, password);
    if (validationError) {
        showAuthMessage(validationError);
        return;
    }
    setButtonLoading('register-btn', true);
    try {
        const data = await api.post('/auth/register', { username, email, password });
        api.setAuth(data);
        showAuthMessage('Account created successfully!', 'success');
        setTimeout(() => navigateTo('dashboard'), 500);
    } catch (error) {
        showAuthMessage(error.message);
    } finally {
        setButtonLoading('register-btn', false);
    }
}

function handleLogout() {
    api.clearAuth();
    navigateTo('auth');
}
