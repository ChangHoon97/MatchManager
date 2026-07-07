// ========== 공유 대진표 열람 ==========

let shareEventSource = null;

function getShareToken() {
    return document.getElementById('shareToken').value;
}

function loadShareView() {
    fetch(`/api/share/${getShareToken()}`)
        .then(async res => {
            const data = await res.json();
            if (!res.ok) throw new Error(data?.message || '존재하지 않거나 만료된 링크입니다.');
            return data;
        })
        .then(applyShareView)
        .catch(err => {
            document.getElementById('shareSubtitle').textContent = err.message;
        });
}

function applyShareView(view) {
    document.getElementById('shareSubtitle').textContent = view.title;

    if (view.unlocked) {
        document.getElementById('unlockSection').classList.add('hidden');
        courtsData = view.content;
        renderCourtsInto(document.getElementById('shareContentBody'), courtsData, view.totalPlayers);
        connectShareEvents();
    } else {
        disconnectShareEvents();
        document.getElementById('unlockSection').classList.remove('hidden');
    }
}

function refreshUnlockedShareView() {
    fetch(`/api/share/${getShareToken()}`)
        .then(async res => {
            const data = await res.json();
            if (!res.ok) throw new Error(data?.message || '대진표를 갱신하지 못했습니다.');
            return data;
        })
        .then(view => {
            if (!view.unlocked) {
                applyShareView(view);
                return;
            }
            courtsData = view.content;
            renderCourtsInto(document.getElementById('shareContentBody'), courtsData, view.totalPlayers);
        })
        .catch(() => disconnectShareEvents());
}

function connectShareEvents() {
    if (shareEventSource && shareEventSource.readyState !== EventSource.CLOSED) return;
    shareEventSource = null;

    shareEventSource = new EventSource(`/api/share/${getShareToken()}/events`);
    shareEventSource.addEventListener('scores-updated', refreshUnlockedShareView);
    shareEventSource.onerror = () => {};
}

function disconnectShareEvents() {
    if (!shareEventSource) return;
    shareEventSource.close();
    shareEventSource = null;
}

function doUnlock() {
    const password = document.getElementById('sharePasswordInput').value;

    fetch(`/api/share/${getShareToken()}/unlock`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': getCsrfToken() },
        body: JSON.stringify({ password })
    })
    .then(async res => {
        const data = await res.json();
        if (!res.ok) throw new Error(data?.message || '비밀번호가 올바르지 않습니다.');
        return data;
    })
    .then(applyShareView)
    .catch(err => showAuthMsg('unlockMsg', err.message));
}

document.addEventListener('DOMContentLoaded', loadShareView);
window.addEventListener('beforeunload', disconnectShareEvents);
