// ========== 공유 대진표 열람 ==========

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
    } else {
        document.getElementById('unlockSection').classList.remove('hidden');
    }
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
