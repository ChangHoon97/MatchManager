// ========== 내 대진표 목록 ==========

function loadMyDraws() {
    fetch('/api/draws')
        .then(async res => {
            if (res.status === 401) {
                showMyDrawsMsg('로그인이 필요합니다.');
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
            <button type="button" class="btn-remove" onclick="deleteDraw(${d.id})" title="삭제">✕</button>
        </div>
    `).join('');
}

function viewDraw(id) {
    fetch(`/api/draws/${id}`)
        .then(async res => {
            const data = await res.json();
            if (!res.ok) throw new Error(data?.message || '대진표를 불러오지 못했습니다.');
            return data;
        })
        .then(detail => {
            document.getElementById('detailTitle').textContent = '🏸 ' + detail.title;
            courtsData = detail.content;
            renderCourtsInto(document.getElementById('detailModalBody'), courtsData, detail.totalPlayers);
            document.getElementById('detailModal').classList.remove('hidden');
        })
        .catch(err => alert(err.message));
}

function closeDetailModal() {
    document.getElementById('detailModal').classList.add('hidden');
}

function deleteDraw(id) {
    if (!confirm('이 대진표를 삭제할까요?')) return;

    fetch(`/api/draws/${id}`, {
        method: 'DELETE',
        headers: { 'X-XSRF-TOKEN': getCsrfToken() }
    })
    .then(res => {
        if (!res.ok && res.status !== 204) throw new Error('삭제에 실패했습니다.');
        loadMyDraws();
    })
    .catch(err => alert(err.message));
}

document.addEventListener('DOMContentLoaded', loadMyDraws);
