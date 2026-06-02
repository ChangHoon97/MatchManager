// ========== 유틸: HTML 이스케이프 ==========

function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// ========== 선수 관리 ==========

let players = [];

function addPlayer(name = '', grade = 'C') {
    const index = players.length;
    players.push({ name, grade });
    renderPlayerRow(index);
    updateCount();
}

const MIN_PLAYERS = 4;

function removePlayer(index) {
    if (players.length <= MIN_PLAYERS) {
        showValidation(`최소 ${MIN_PLAYERS}명은 유지해야 합니다.`);
        return;
    }
    players.splice(index, 1);
    renderAll();
    hideValidation();
}

function renderAll() {
    const list = document.getElementById('playerList');
    list.innerHTML = '';
    players.forEach((_, i) => renderPlayerRow(i));
    updateCount();
}

function renderPlayerRow(index) {
    const list = document.getElementById('playerList');
    const p = players[index];

    const row = document.createElement('div');
    row.className = 'player-row';
    row.id = `row-${index}`;

    row.innerHTML = `
        <div class="player-num">${index + 1}</div>
        <input type="text"
               class="player-name-input"
               placeholder="이름 입력"
               value="${escapeHtml(p.name)}"
               oninput="updatePlayer(${index}, 'name', this.value)"
               onkeydown="handleEnter(event, ${index})">
        <select class="grade-select"
                onchange="updatePlayer(${index}, 'grade', this.value)">
            ${['A','B','C','D','E','F'].map(g =>
                `<option value="${g}" ${p.grade === g ? 'selected' : ''}>${g}</option>`
            ).join('')}
        </select>
        <button class="btn-remove" onclick="removePlayer(${index})" title="삭제">✕</button>
    `;

    list.appendChild(row);
}

function updatePlayer(index, field, value) {
    players[index][field] = value;
}

function updateCount() {
    document.getElementById('playerCount').textContent = `${players.length}명`;
}

function handleEnter(event, index) {
    if (event.key === 'Enter') {
        if (index === players.length - 1) {
            addPlayer();
            setTimeout(() => {
                const inputs = document.querySelectorAll('.player-name-input');
                if (inputs[index + 1]) inputs[index + 1].focus();
            }, 50);
        }
    }
}

function clearAll() {
    if (players.length > 0 && !confirm('전체 초기화할까요?')) return;
    players = [];
    renderAll();
}

// ========== 샘플 데이터 ==========

function loadSample() {
    const samplePlayers = [
        { name: '김철수', grade: 'A' }, { name: '이영희', grade: 'A' },
        { name: '박민준', grade: 'B' }, { name: '최지은', grade: 'B' },
        { name: '정우성', grade: 'B' }, { name: '한소희', grade: 'C' },
        { name: '오세훈', grade: 'C' }, { name: '신지아', grade: 'C' },
        { name: '배준호', grade: 'C' }, { name: '윤미래', grade: 'D' },
        { name: '임현식', grade: 'D' }, { name: '장나라', grade: 'D' },
        { name: '강동원', grade: 'D' }, { name: '서지수', grade: 'E' },
        { name: '노준혁', grade: 'E' }, { name: '문채원', grade: 'F' },
    ];
    players = samplePlayers;
    renderAll();
}

// ========== 대진표 생성 ==========

function generateDraw() {
    // 유효성 검사
    const validPlayers = players.filter(p => p.name.trim() !== '');
    if (validPlayers.length < 4) {
        showValidation('최소 4명 이상 입력해주세요.');
        return;
    }
    if (validPlayers.length < 6) {
        showValidation('코트당 최소 6명 필요합니다. 선수를 더 추가해주세요.');
        return;
    }

    hideValidation();

    // AJAX 요청
    fetch('/api/draw', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ players: validPlayers })
    })
    .then(async res => {
        const data = await res.json();
        if (!res.ok) {
            throw new Error(data && data.message ? data.message : '서버 오류');
        }
        return data;
    })
    .then(courts => renderResult(courts, validPlayers.length))
    .catch(err => {
        console.error(err);
        showValidation(err.message || '대진표 생성 중 오류가 발생했습니다. 다시 시도해주세요.');
    });
}

// ========== 결과 렌더링 ==========

function renderResult(courts, totalPlayers) {
    const modal = document.getElementById('resultModal');
    const body  = document.getElementById('modalBody');

    let html = `<p class="subtitle" style="margin-bottom:16px">총 ${totalPlayers}명 · ${courts.length}개 코트</p>`;

    courts.forEach(court => {
        html += `
        <div class="court-section">
            <div class="court-header">
                <h2>🏟️ ${court.courtNumber}번 코트</h2>
                <span class="player-count">${court.players.length}명</span>
            </div>
            <div class="court-players">
                ${court.players.map(p => `
                    <span class="player-chip">
                        ${escapeHtml(p.name)}
                        <span class="grade-badge grade-${escapeHtml(p.grade.toLowerCase())}">${escapeHtml(p.grade)}</span>
                    </span>
                `).join('')}
            </div>
            <div class="games-grid">
                ${court.games.map(game => renderGameCard(game)).join('')}
            </div>
        </div>`;
    });

    body.innerHTML = html;
    modal.classList.remove('hidden');
}

function renderPlayer(p) {
    const grade = escapeHtml(p.grade);
    const gradeLower = escapeHtml(p.grade.toLowerCase());
    return `${escapeHtml(p.name)} <span class="grade-badge grade-${gradeLower}">${grade}</span>`;
}

function renderGameCard(game) {
    const waitingHtml = game.waiting1 ? `
        <div class="waiting-area">
            <span class="waiting-label">대기</span>
            <span class="waiting-player">${escapeHtml(game.waiting1.name)} (${escapeHtml(game.waiting1.grade)})</span>
            ${game.waiting2 ? `<span class="waiting-player">${escapeHtml(game.waiting2.name)} (${escapeHtml(game.waiting2.grade)})</span>` : ''}
        </div>` : '';

    return `
    <div class="game-card">
        <div class="game-title">Game ${game.gameNumber}</div>
        <div class="match-area">
            <div class="team team-a">
                <div class="team-label">A팀</div>
                <div class="team-player">${renderPlayer(game.teamA1)}</div>
                <div class="team-player">${renderPlayer(game.teamA2)}</div>
            </div>
            <div class="vs-badge">VS</div>
            <div class="team team-b">
                <div class="team-label">B팀</div>
                <div class="team-player">${renderPlayer(game.teamB1)}</div>
                <div class="team-player">${renderPlayer(game.teamB2)}</div>
            </div>
        </div>
        ${waitingHtml}
    </div>`;
}

// ========== 유틸 ==========

function showValidation(msg) {
    const el = document.getElementById('validationMsg');
    el.textContent = msg;
    el.classList.remove('hidden');
}

function hideValidation() {
    document.getElementById('validationMsg').classList.add('hidden');
}

function closeModal() {
    document.getElementById('resultModal').classList.add('hidden');
}

function regenerate() {
    closeModal();
    setTimeout(generateDraw, 100);
}

// ========== 초기화: 기본 4줄 ==========
document.addEventListener('DOMContentLoaded', () => {
    for (let i = 0; i < 4; i++) addPlayer();
});
