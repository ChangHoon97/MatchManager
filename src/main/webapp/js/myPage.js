function loadProfile() {
    fetch('/api/auth/me')
        .then(async res => {
            const data = await res.json().catch(() => null);
            if (res.status === 401) {
                redirectToHomeForLogin();
                return null;
            }
            if (!res.ok) throw new Error(data?.message || '회원 정보를 불러오지 못했습니다.');
            return data;
        })
        .then(user => {
            if (!user) return;
            currentUser = user;
            renderAuthWidget();
            document.getElementById('profileEmail').value = user.email || '';
            document.getElementById('profileNickname').value = user.nickname || '';
            document.getElementById('profileCelno').value = user.celno || '';
            setPasswordSection(user);
        })
        .catch(err => showAuthMsg('profileMsg', err.message));
}

function saveProfile() {
    const nickname = document.getElementById('profileNickname').value.trim();
    const celno = document.getElementById('profileCelno').value.trim();

    fetch('/api/auth/me', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': getCsrfToken() },
        body: JSON.stringify({ nickname, celno })
    })
    .then(async res => {
        const data = await res.json().catch(() => null);
        if (res.status === 401) {
            redirectToHomeForLogin();
            return null;
        }
        if (!res.ok) throw new Error(data?.message || '회원 정보 저장에 실패했습니다.');
        return data;
    })
    .then(user => {
        if (!user) return;
        currentUser = user;
        renderAuthWidget();
        document.getElementById('profileNickname').value = user.nickname || '';
        document.getElementById('profileCelno').value = user.celno || '';
        showAuthMsg('profileMsg', '회원 정보가 저장되었습니다.');
    })
    .catch(err => showAuthMsg('profileMsg', err.message));
}

const PROFILE_PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,30}$/;
const PROFILE_PASSWORD_RULE_MESSAGE = '비밀번호는 8~30자이며 영어, 숫자, 특수문자(!@#$%^&*)를 모두 포함해야 합니다.';

function setPasswordSection(user) {
    const localFields = document.getElementById('passwordLocalFields');
    const oauthNotice = document.getElementById('passwordOAuthNotice');
    const isLocal = user.provider === 'LOCAL';

    localFields.classList.toggle('hidden', !isLocal);
    oauthNotice.classList.toggle('hidden', isLocal);
}

function changePassword() {
    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const newPasswordConfirm = document.getElementById('newPasswordConfirm').value;

    if (!PROFILE_PASSWORD_PATTERN.test(newPassword)) {
        showAuthMsg('passwordMsg', PROFILE_PASSWORD_RULE_MESSAGE);
        return;
    }
    if (newPassword !== newPasswordConfirm) {
        showAuthMsg('passwordMsg', '새 비밀번호가 일치하지 않습니다.');
        return;
    }

    fetch('/api/auth/me/password', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': getCsrfToken() },
        body: JSON.stringify({ currentPassword, newPassword })
    })
    .then(async res => {
        const data = await res.json().catch(() => null);
        if (res.status === 401) {
            redirectToHomeForLogin();
            return false;
        }
        if (!res.ok) throw new Error(data?.message || '비밀번호 변경에 실패했습니다.');
        return true;
    })
    .then(changed => {
        if (!changed) return;
        document.getElementById('currentPassword').value = '';
        document.getElementById('newPassword').value = '';
        document.getElementById('newPasswordConfirm').value = '';
        showAuthMsg('passwordMsg', '비밀번호가 변경되었습니다.');
    })
    .catch(err => showAuthMsg('passwordMsg', err.message));
}

function redirectToHomeForLogin() {
    alert('로그인이 필요합니다.');
    window.location.href = '/';
}

document.addEventListener('DOMContentLoaded', loadProfile);
