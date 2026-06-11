// ========== 유틸 ==========

function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function gradeNum(grade) {
    return { A:6, B:5, C:4, D:3, E:2, F:1 }[grade?.toUpperCase()] || 0;
}

function totalScore(player) {
    if (!player) return 0;
    return (gradeNum(player.grade) - 1) * 100 + (player.value ?? 50);
}

// ========== 선수 관리 ==========

let players = [];
const MIN_PLAYERS = 4;

function addPlayer(name = '', grade = 'C', value = 50) {
    players.push({ name, grade, value });
    renderPlayerRow(players.length - 1);
    updateCount();
}

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

    const grades = ['A','B','C','D','E','F'];
    const gradeOptions = grades.map(g =>
        `<option value="${g}" ${p.grade === g ? 'selected' : ''}>${g}</option>`
    ).join('');

    row.innerHTML = `
        <div class="player-num">${index + 1}</div>
        <input type="text"
               class="player-name-input"
               placeholder="성함을 입력해주세요"
               value="${escapeHtml(p.name)}"
               oninput="updatePlayer(${index}, 'name', this.value)"
               onkeydown="handleEnter(event, ${index})">
        <select class="grade-select"
                onchange="updatePlayer(${index}, 'grade', this.value)">
            ${gradeOptions}
        </select>
        <input type="number"
               class="value-input"
               min="0" max="100"
               value="${p.value}"
               oninput="updatePlayer(${index}, 'value', Math.min(100, Math.max(0, +this.value || 0)))">
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
    if (event.key === 'Enter' && index === players.length - 1) {
        addPlayer();
        setTimeout(() => {
            const inputs = document.querySelectorAll('.player-name-input');
            if (inputs[index + 1]) inputs[index + 1].focus();
        }, 50);
    }
}

function clearAll() {
    if (players.length > 0 && !confirm('전체 초기화할까요?')) return;
    players = [];
    renderAll();
}

// ========== 샘플 데이터 ==========

function loadSample() {
    players = [
        { name: '김철수', grade: 'A', value: 80 },
        { name: '이영희', grade: 'A', value: 40 },
        { name: '박민준', grade: 'B', value: 90 },
        { name: '최지은', grade: 'B', value: 55 },
        { name: '정우성', grade: 'B', value: 20 },
        { name: '한소희', grade: 'C', value: 85 },
        { name: '오세훈', grade: 'C', value: 60 },
        { name: '신지아', grade: 'C', value: 30 },
        { name: '배준호', grade: 'C', value: 10 },
        { name: '윤미래', grade: 'D', value: 75 },
        { name: '임현식', grade: 'D', value: 45 },
        { name: '장나라', grade: 'D', value: 15 },
        { name: '강동원', grade: 'D', value: 5  },
        { name: '서지수', grade: 'E', value: 70 },
        { name: '노준혁', grade: 'E', value: 35 },
        { name: '문채원', grade: 'F', value: 60 },
    ];
    renderAll();
}

// ========== 엑셀 업로드 ==========

function uploadExcel(input) {
    const file = input.files[0];
    if (!file) return;
    input.value = ''; // 동일 파일 재업로드 허용

    const formData = new FormData();
    formData.append('file', file);

    showExcelMsg('⏳ 파일을 분석하는 중...', 'loading');

    fetch('/api/upload-excel', {
        method: 'POST',
        body: formData
    })
    .then(async res => {
        const data = await res.json();
        if (!res.ok) {
            const msg = data?.errors?.[0] || '업로드 오류';
            throw new Error(msg);
        }
        return data;
    })
    .then(result => {
        if (result.players && result.players.length > 0) {
            players = result.players.map(p => ({
                name: p.name,
                grade: p.grade,
                value: p.value ?? 50
            }));
            renderAll();
            hideValidation();
        }

        if (result.errors && result.errors.length > 0) {
            showExcelMsg(
                `<strong>⚠️ ${result.errors.length}개 행에 오류가 있습니다.</strong>` +
                (result.players.length > 0
                    ? ` (유효한 ${result.players.length}명은 불러왔습니다)`
                    : '') +
                '<ul>' + result.errors.map(e => `<li>${escapeHtml(e)}</li>`).join('') + '</ul>',
                'error'
            );
        } else {
            showExcelMsg(`✅ ${result.players.length}명의 선수를 불러왔습니다.`, 'success');
            setTimeout(hideExcelMsg, 3000);
        }
    })
    .catch(err => {
        showExcelMsg('❌ ' + escapeHtml(err.message || '업로드 중 오류가 발생했습니다.'), 'error');
    });
}

function showExcelMsg(html, type) {
    const el = document.getElementById('excelMsg');
    el.innerHTML = html;
    el.className = `excel-msg excel-msg-${type}`;
}

function hideExcelMsg() {
    const el = document.getElementById('excelMsg');
    if (el) {
        el.className = 'excel-msg hidden';
        el.innerHTML = '';
    }
}

// ========== 코트 수 ==========

let courtCount = 0; // 0 = 자동

function changeCourtCount(delta) {
    courtCount = Math.max(0, courtCount + delta);
    document.getElementById('courtCountDisplay').textContent =
        courtCount === 0 ? '자동' : courtCount;
}

let gamesPerPlayer = 0; // 0 = 자동

function changeGameCount(delta) {
    gamesPerPlayer = Math.max(0, gamesPerPlayer + delta);
    document.getElementById('gameCountDisplay').textContent =
        gamesPerPlayer === 0 ? '자동' : gamesPerPlayer;
}

// ========== 대진표 생성 ==========

function generateDraw() {
    const validPlayers = players.filter(p => p.name.trim() !== '');
    if (validPlayers.length < 4) {
        showValidation('최소 4명 이상 입력해주세요.');
        return;
    }
    if (courtCount > 0 && Math.floor(validPlayers.length / courtCount) < 4) {
        const maxCourts = Math.floor(validPlayers.length / 4);
        showValidation(`코트당 최소 4명이 필요합니다. ${validPlayers.length}명으로는 최대 ${maxCourts}개 코트 설정 가능합니다.`);
        return;
    }
    hideValidation();

    fetch('/api/draw', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ players: validPlayers, courtCount, gamesPerPlayer })
    })
    .then(async res => {
        const data = await res.json();
        if (!res.ok) throw new Error(data?.message || '서버 오류');
        return data;
    })
    .then(courts => renderResult(courts, validPlayers.length))
    .catch(err => {
        console.error(err);
        showValidation(err.message || '대진표 생성 중 오류가 발생했습니다. 다시 시도해주세요.');
    });
}

// ========== 결과 렌더링 ==========

let courtsData = [];
let totalPlayersCount = 0;

function renderResult(courts, totalPlayers) {
    courtsData = courts;
    totalPlayersCount = totalPlayers;
    renderModalContent();
    document.getElementById('resultModal').classList.remove('hidden');
}

function renderModalContent() {
    const body = document.getElementById('modalBody');
    let html = `<p class="subtitle" style="margin-bottom:16px">총 ${totalPlayersCount}명 · ${courtsData.length}개 코트</p>`;

    courtsData.forEach((court, ci) => {
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
                ${court.games.map((game, gi) => renderGameCard(game, ci, gi)).join('')}
            </div>
        </div>`;
    });

    body.innerHTML = html;
}

function renderGameCard(game, ci, gi) {
    const tA = totalScore(game.teamA1) + totalScore(game.teamA2);
    const tB = totalScore(game.teamB1) + totalScore(game.teamB2);
    const diff = Math.abs(tA - tB);
    const balanced = diff < 70;
    const label = balanced ? '균형' : (tA > tB ? 'A팀 우세' : 'B팀 우세');
    const cls   = balanced ? 'balance-ok' : 'balance-warn';

    return `
    <div class="game-card">
        <div class="game-title">
            Game ${game.gameNumber}
            <span class="balance-badge ${cls}" title="팀 점수차: ${diff}">
                ${label}
            </span>
        </div>
        <div class="match-area">
            <div class="team team-a">
                <div class="team-label">A팀</div>
                ${renderSlot(game.teamA1, ci, gi, 'teamA1', 'team-a')}
                ${renderSlot(game.teamA2, ci, gi, 'teamA2', 'team-a')}
            </div>
            <div class="vs-badge">VS</div>
            <div class="team team-b">
                <div class="team-label">B팀</div>
                ${renderSlot(game.teamB1, ci, gi, 'teamB1', 'team-b')}
                ${renderSlot(game.teamB2, ci, gi, 'teamB2', 'team-b')}
            </div>
        </div>
        ${renderWaiting(game, ci, gi)}
    </div>`;
}

function renderSlot(player, ci, gi, role, teamClass) {
    if (player) {
        return `<div class="team-player ${teamClass} dnd-item"
                     draggable="true"
                     data-ci="${ci}" data-gi="${gi}" data-role="${role}"
                     ondragstart="dndStart(event)"
                     ondragend="dndEnd(event)"
                     ondragover="dndOver(event)"
                     ondragleave="dndLeave(event)"
                     ondrop="dndDrop(event)">
            ${escapeHtml(player.name)}
            <span class="grade-badge grade-${escapeHtml(player.grade.toLowerCase())}">${escapeHtml(player.grade)}</span>
        </div>`;
    }
    return `<div class="team-player ${teamClass} dnd-empty"
                 data-ci="${ci}" data-gi="${gi}" data-role="${role}"
                 ondragover="dndOver(event)"
                 ondragleave="dndLeave(event)"
                 ondrop="dndDrop(event)">–</div>`;
}

function renderWaiting(game, ci, gi) {
    if (!game.waiting1 && !game.waiting2) return '';

    const w1 = renderSlot(game.waiting1, ci, gi, 'waiting1', 'waiting-slot');
    const w2 = renderSlot(game.waiting2, ci, gi, 'waiting2', 'waiting-slot');

    return `<div class="waiting-area">
        <span class="waiting-label">대기</span>
        ${w1}
        ${game.waiting1 ? w2 : ''}
    </div>`;
}

// ========== 드래그앤드롭 ==========

let dndSrc = null;

function dndStart(event) {
    const el = event.currentTarget;
    dndSrc = { ci: +el.dataset.ci, gi: +el.dataset.gi, role: el.dataset.role };
    el.classList.add('dnd-dragging');
    event.dataTransfer.effectAllowed = 'move';
}

function dndEnd(event) {
    event.currentTarget.classList.remove('dnd-dragging');
}

function dndOver(event) {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
    event.currentTarget.classList.add('dnd-over');
}

function dndLeave(event) {
    event.currentTarget.classList.remove('dnd-over');
}

function dndDrop(event) {
    event.preventDefault();
    event.currentTarget.classList.remove('dnd-over');
    if (!dndSrc) return;

    const dstEl = event.currentTarget;
    const dst = { ci: +dstEl.dataset.ci, gi: +dstEl.dataset.gi, role: dstEl.dataset.role };

    if (dndSrc.ci === dst.ci && dndSrc.gi === dst.gi && dndSrc.role === dst.role) {
        dndSrc = null;
        return;
    }

    const srcGame = courtsData[dndSrc.ci].games[dndSrc.gi];
    const dstGame = courtsData[dst.ci].games[dst.gi];

    const srcPlayer = srcGame[dndSrc.role] ?? null;
    const dstPlayer = dstGame[dst.role] ?? null;

    srcGame[dndSrc.role] = dstPlayer;
    dstGame[dst.role] = srcPlayer;

    rebuildCourtPlayers(courtsData[dndSrc.ci]);
    if (dst.ci !== dndSrc.ci) rebuildCourtPlayers(courtsData[dst.ci]);

    dndSrc = null;
    renderModalContent();
}

function rebuildCourtPlayers(court) {
    const seen = new Set();
    const result = [];
    const roles = ['teamA1', 'teamA2', 'teamB1', 'teamB2', 'waiting1', 'waiting2'];
    for (const game of court.games) {
        for (const role of roles) {
            const p = game[role];
            if (p && !seen.has(p.name)) {
                seen.add(p.name);
                result.push(p);
            }
        }
    }
    court.players = result;
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

// ========== 초기화 ==========
document.addEventListener('DOMContentLoaded', () => {
    for (let i = 0; i < 4; i++) addPlayer();
});
