// ========== 내 대진표 목록 ==========

let detailDrawId = null;

function requireLoginThenLoad() {
    fetch('/api/auth/me')
        .then(res => {
            if (!res.ok) {
                redirectToHomeForLogin();
                return null;
            }
            return res.json();
        })
        .then(user => {
            if (!user) return;
            currentUser = user;
            renderAuthWidget();
            loadMyDraws();
        })
        .catch(() => redirectToHomeForLogin());
}

function loadMyDraws() {
    fetch('/api/draws')
        .then(async res => {
            if (res.status === 401) {
                redirectToHomeForLogin();
                return null;
            }
            const data = await res.json();
            if (!res.ok) throw new Error(data?.message || '목록을 불러오지 못했습니다.');
            return data;
        })
        .then(draws => {
            if (draws) renderDrawList(draws);
        })
        .catch(err => showMyDrawsMsg(err.message));
}

function showMyDrawsMsg(msg) {
    const el = document.getElementById('myDrawsMsg');
    el.textContent = msg;
    el.classList.remove('hidden');
}

function renderDrawList(draws) {
    const list = document.getElementById('myDrawsList');
    if (draws.length === 0) {
        list.innerHTML = '<p class="subtitle">저장된 대진표가 없습니다.</p>';
        return;
    }

    list.innerHTML = draws.map(d => `
        <div class="draw-card">
            <div class="draw-card-info" onclick="viewDraw(${d.id})">
                <h3>${escapeHtml(d.title)}</h3>
                <p class="draw-card-meta">
                    총 ${d.totalPlayers}명 · ${d.courtCount > 0 ? d.courtCount + '개 코트' : '코트 자동'}
                    ${d.hasShare ? '<span class="draw-share-badge">공유중</span>' : ''}
                </p>
            </div>
            <button type="button" class="btn btn-auth" onclick="openShareModal(${d.id})">공유</button>
            <button type="button" class="btn-remove" onclick="deleteDraw(${d.id})" title="삭제">✕</button>
        </div>
    `).join('');
}

function viewDraw(id) {
    fetch(`/api/draws/${id}`)
        .then(async res => {
            if (res.status === 401) {
                redirectToHomeForLogin();
                return null;
            }
            const data = await res.json();
            if (!res.ok) throw new Error(data?.message || '대진표를 불러오지 못했습니다.');
            return data;
        })
        .then(detail => {
            if (!detail) return;
            detailDrawId = id;
            document.getElementById('detailTitle').textContent = '🏸 ' + detail.title;
            courtsData = detail.content;
            renderCourtsInto(document.getElementById('detailModalBody'), courtsData, detail.totalPlayers, { scoreMode: 'edit' });
            document.getElementById('detailModal').classList.remove('hidden');
        })
        .catch(err => alert(err.message));
}

function closeDetailModal() {
    document.getElementById('detailModal').classList.add('hidden');
    detailDrawId = null;
}

function saveScores() {
    if (!detailDrawId || !courtsData) return;

    const updates = [];
    document.querySelectorAll('#detailModalBody .score-input').forEach(input => {
        const ci = +input.dataset.ci;
        const gi = +input.dataset.gi;
        const field = input.dataset.scoreField;
        const game = courtsData?.[ci]?.games?.[gi];
        if (!game || !game.matchId) return;

        const existing = updates.find(item => item.matchId === game.matchId);
        const update = existing || { matchId: game.matchId, team1Score: null, team2Score: null };
        update[field] = input.value === '' ? null : +input.value;
        game[field] = update[field];
        if (!existing) updates.push(update);
    });

    fetch(`/api/draws/${detailDrawId}/scores`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': getCsrfToken() },
        body: JSON.stringify(updates)
    })
    .then(async res => {
        if (res.status === 401) {
            redirectToHomeForLogin();
            return false;
        }
        if (!res.ok) {
            const data = await res.json().catch(() => ({}));
            throw new Error(data?.message || '점수 저장에 실패했습니다.');
        }
        return true;
    })
    .then(saved => {
        if (saved) alert('점수가 저장되었습니다.');
    })
    .catch(err => alert(err.message));
}

function deleteDraw(id) {
    if (!confirm('이 대진표를 삭제할까요?')) return;

    fetch(`/api/draws/${id}`, {
        method: 'DELETE',
        headers: { 'X-XSRF-TOKEN': getCsrfToken() }
    })
    .then(res => {
        if (res.status === 401) {
            redirectToHomeForLogin();
            return false;
        }
        if (!res.ok && res.status !== 204) throw new Error('삭제에 실패했습니다.');
        return true;
    })
    .then(deleted => {
        if (deleted) loadMyDraws();
    })
    .catch(err => alert(err.message));
}

// ========== 공유 링크 ==========

let shareDrawId = null;

function openShareModal(id) {
    shareDrawId = id;
    document.getElementById('sharePassword').value = '';
    setShareResultVisible(false);
    document.getElementById('qrcodeContainer').innerHTML = '';
    hideAuthMsg('shareMsg');
    document.getElementById('shareModal').classList.remove('hidden');

    fetch(`/api/draws/${id}/share`)
        .then(async res => {
            if (res.status === 401) {
                redirectToHomeForLogin();
                return null;
            }
            return res.ok ? res.json() : null;
        })
        .then(info => {
            if (!info || !info.password || !info.shareUrl) return;
            document.getElementById('sharePassword').value = info.password;
            const fullUrl = window.location.origin + info.shareUrl;
            document.getElementById('shareUrlResult').value = fullUrl;
            setShareResultVisible(true);
            renderShareQrCode(fullUrl);
        });
}

function closeShareModal() {
    document.getElementById('shareModal').classList.add('hidden');
}

function doCreateShare() {
    const password = document.getElementById('sharePassword').value;

    fetch(`/api/draws/${shareDrawId}/share`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': getCsrfToken() },
        body: JSON.stringify({ password })
    })
    .then(async res => {
        if (res.status === 401) {
            redirectToHomeForLogin();
            return null;
        }
        const data = await res.json();
        if (!res.ok) throw new Error(data?.message || '공유 링크 생성에 실패했습니다.');
        return data;
    })
    .then(data => {
        if (!data) return;
        const fullUrl = window.location.origin + data.shareUrl;
        document.getElementById('shareUrlResult').value = fullUrl;
        setShareResultVisible(true);
        renderShareQrCode(fullUrl);
        loadMyDraws();
        showToast('공유 링크가 생성되었습니다.');
    })
    .catch(err => showAuthMsg('shareMsg', err.message));
}

function setShareResultVisible(visible) {
    const result = document.getElementById('shareResult');
    if (!result) return;
    result.classList.toggle('hidden', !visible);
    result.style.display = visible ? 'block' : 'none';
}

function copyShareUrl() {
    const input = document.getElementById('shareUrlResult');
    const url = input.value;
    if (!url) return;

    const done = () => showToast('공유 링크를 복사했습니다.');

    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(url).then(done).catch(() => {
            input.select();
            document.execCommand('copy');
            done();
        });
        return;
    }

    input.select();
    document.execCommand('copy');
    done();
}

function renderShareQrCode(url) {
    const container = document.getElementById('qrcodeContainer');
    container.innerHTML = '';
    new QRCode(container, { text: url, width: 180, height: 180 });
}

function downloadQrCode() {
    const canvas = document.querySelector('#qrcodeContainer canvas');
    if (!canvas) {
        showToast('다운로드할 QR 코드가 없습니다.');
        return;
    }
    const a = document.createElement('a');
    a.href = canvas.toDataURL('image/png');
    a.download = 'share-qrcode.png';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    showToast('QR 코드 다운로드를 시작했습니다.');
}

function showToast(message) {
    let toast = document.getElementById('appToast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'appToast';
        toast.className = 'app-toast';
        document.body.appendChild(toast);
    }

    toast.textContent = message;
    toast.classList.add('app-toast-visible');
    clearTimeout(showToast.timer);
    showToast.timer = setTimeout(() => {
        toast.classList.remove('app-toast-visible');
    }, 1800);
}

function redirectToHomeForLogin() {
    alert('로그인이 필요합니다.');
    window.location.href = '/';
}

document.addEventListener('DOMContentLoaded', requireLoginThenLoad);
