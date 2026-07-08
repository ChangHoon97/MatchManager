// ========== CSRF ==========

function getCsrfToken() {
    const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : '';
}

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
    return { S:7, A:6, B:5, C:4, D:3, E:2, F:1 }[grade?.toUpperCase()] || 0;
}

function totalScore(player) {
    if (!player) return 0;
    return (gradeNum(player.grade) - 1) * 100 + (player.rating ?? 50);
}

// ========== 선수 관리 ==========

let players = [];
const MIN_PLAYERS = 4;

function addPlayer(name = '', grade = '', rating = 50, gender = '', age = 0) {
    players.push({ name, grade, rating, gender, age });
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

    const grades = ['S','A','B','C','D','E','F'];
    const gradeOptions = grades.map(g =>
        `<option value="${g}" ${p.grade === g ? 'selected' : ''}>${g}</option>`
    ).join('');

    const ages = [20, 30, 40, 45, 50, 55, 60, 65];
    const ageOptions = ages.map(a =>
        `<option value="${a}" ${p.age === a ? 'selected' : ''}>${a}</option>`
    ).join('');

    const genderCls = p.gender ? ` gender-select-${p.gender}` : '';

    row.innerHTML = `
        <div class="player-num">${index + 1}</div>
        <input type="text"
               id="player-name-${index}"
               name="player[${index}][name]"
               class="player-name-input"
               placeholder="성함을 입력해주세요"
               maxlength="10"
               value="${escapeHtml(p.name)}"
               oninput="updatePlayer(${index}, 'name', this.value)"
               onkeydown="handleEnter(event, ${index})">
        <div class="player-controls">
            <select id="player-age-${index}"
                    name="player[${index}][age]"
                    class="age-select"
                    onchange="updatePlayer(${index}, 'age', +this.value)">
                <option value="0" ${!p.age ? 'selected' : ''}>나이</option>
                ${ageOptions}
            </select>
            <select id="player-grade-${index}"
                    name="player[${index}][grade]"
                    class="grade-select"
                    onchange="updatePlayer(${index}, 'grade', this.value)">
                <option value="" ${!p.grade ? 'selected' : ''}>급수</option>
                ${gradeOptions}
            </select>
            <select id="player-gender-${index}"
                    name="player[${index}][gender]"
                    class="gender-select${genderCls}"
                    onchange="updatePlayer(${index}, 'gender', this.value); this.className='gender-select'+(this.value?' gender-select-'+this.value:'')">
                <option value="" ${!p.gender ? 'selected' : ''}>성별</option>
                <option value="남" ${p.gender === '남' ? 'selected' : ''}>남</option>
                <option value="여" ${p.gender === '여' ? 'selected' : ''}>여</option>
            </select>
            <input type="number"
                   id="player-rating-${index}"
                   name="player[${index}][rating]"
                   class="value-input"
                   min="0" max="100"
                   value="${p.rating}"
                   oninput="this.value = Math.min(100, Math.max(0, +this.value || 0)); updatePlayer(${index}, 'rating', +this.value)">
            <button class="btn-remove" onclick="removePlayer(${index})" title="삭제">✕</button>
        </div>
    `;
    list.appendChild(row);
}

function updatePlayer(index, field, value) {
    if (field === 'name') value = value.slice(0, 10);
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
    for (let i = 0; i < 4; i++) addPlayer();
}

// ========== 샘플 데이터 ==========

function loadSample() {
    players = [
        { name: '김철수', grade: 'A', rating: 80, gender: '남', age: 30 },
        { name: '이영희', grade: 'A', rating: 40, gender: '여', age: 40 },
        { name: '박민준', grade: 'B', rating: 90, gender: '남', age: 45 },
        { name: '최지은', grade: 'B', rating: 55, gender: '여', age: 50 },
        { name: '정우성', grade: 'B', rating: 20, gender: '남', age: 20 },
        { name: '한소희', grade: 'C', rating: 85, gender: '여', age: 30 },
        { name: '오세훈', grade: 'C', rating: 60, gender: '남', age: 55 },
        { name: '신지아', grade: 'C', rating: 30, gender: '여', age: 40 },
        { name: '배준호', grade: 'C', rating: 10, gender: '남', age: 60 },
        { name: '윤미래', grade: 'D', rating: 75, gender: '여', age: 45 },
        { name: '임현식', grade: 'D', rating: 45, gender: '남', age: 50 },
        { name: '장나라', grade: 'D', rating: 15, gender: '여', age: 65 },
        { name: '강동원', grade: 'D', rating: 5,  gender: '남', age: 40 },
        { name: '서지수', grade: 'E', rating: 70, gender: '여', age: 55 },
        { name: '노준혁', grade: 'E', rating: 35, gender: '남', age: 30 },
        { name: '문채원', grade: 'F', rating: 60, gender: '여', age: 60 },
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
        headers: { 'X-XSRF-TOKEN': getCsrfToken() },
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
                name: (p.name || '').slice(0, 10),
                grade: p.grade,
                rating: p.rating ?? 50,
                gender: p.gender || '',
                age: p.age || 0
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
    const noGrade = validPlayers.filter(p => !p.grade);
    if (noGrade.length > 0) {
        showValidation(`급수를 선택하지 않은 선수가 있습니다. (${noGrade.map(p => p.name || '이름없음').join(', ')})`);
        return;
    }
    const noGender = validPlayers.filter(p => !p.gender);
    if (noGender.length > 0) {
        showValidation(`성별을 선택하지 않은 선수가 있습니다. (${noGender.map(p => p.name || '이름없음').join(', ')})`);
        return;
    }
    const noAge = validPlayers.filter(p => !p.age || p.age === 0);
    if (noAge.length > 0) {
        showValidation(`나이를 선택하지 않은 선수가 있습니다. (${noAge.map(p => p.name || '이름없음').join(', ')})`);
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
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': getCsrfToken() },
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
    renderCourtsInto(document.getElementById('modalBody'), courtsData, totalPlayersCount);
}

function renderCourtsInto(container, courts, totalPlayers, options = {}) {
    let html = `<p class="subtitle" style="margin-bottom:16px">총 ${totalPlayers}명 · ${courts.length}개 코트</p>`;

    courts.forEach((court, ci) => {
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
                        ${p.age > 0 ? `<span class="age-badge">${p.age}</span>` : ''}
                        <span class="grade-badge grade-${escapeHtml(p.grade.toLowerCase())}">${escapeHtml(p.grade)}</span>
                        ${p.gender ? `<span class="gender-badge gender-${escapeHtml(p.gender)}">${escapeHtml(p.gender)}</span>` : ''}
                    </span>
                `).join('')}
            </div>
            <div class="games-grid">
                ${court.games.map((game, gi) => renderGameCard(game, ci, gi, options)).join('')}
            </div>
        </div>`;
    });

    container.innerHTML = html;
}

function renderGameCard(game, ci, gi, options = {}) {
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
                <div class="team-label">A팀 ${renderTeamScore(game, 'team1Score', ci, gi, options)}</div>
                ${renderSlot(game.teamA1, ci, gi, 'teamA1', 'team-a')}
                ${renderSlot(game.teamA2, ci, gi, 'teamA2', 'team-a')}
            </div>
            <div class="vs-badge">VS</div>
            <div class="team team-b">
                <div class="team-label">B팀 ${renderTeamScore(game, 'team2Score', ci, gi, options)}</div>
                ${renderSlot(game.teamB1, ci, gi, 'teamB1', 'team-b')}
                ${renderSlot(game.teamB2, ci, gi, 'teamB2', 'team-b')}
            </div>
        </div>
        ${renderWaiting(game, ci, gi)}
    </div>`;
}

function renderTeamScore(game, field, ci, gi, options = {}) {
    if (options.scoreMode === 'edit' && game.matchId) {
        const value = game[field] ?? '';
        const label = field === 'team1Score' ? 'A팀 점수' : 'B팀 점수';
        return `<input type="text"
                       class="score-input"
                       inputmode="numeric"
                       pattern="[0-9]*"
                       maxlength="2"
                       value="${value}"
                       aria-label="${label}"
                       data-ci="${ci}"
                       data-gi="${gi}"
                       data-score-field="${field}"
                       oninput="this.value = this.value.replace(/[^0-9]/g, '').slice(0, 2)">`;
    }

    if (game[field] !== null && game[field] !== undefined) {
        return `<span class="team-score">${game[field]}</span>`;
    }

    return '';
}

function renderSlot(player, ci, gi, role, teamClass) {
    if (player) {
        const genderBadge = player.gender
            ? `<span class="gender-badge gender-${escapeHtml(player.gender)}">${escapeHtml(player.gender)}</span>`
            : '';
        const ageBadge = player.age > 0
            ? `<span class="age-badge">${player.age}</span>`
            : '';
        return `<div class="team-player ${teamClass} dnd-item"
                     draggable="true"
                     data-ci="${ci}" data-gi="${gi}" data-role="${role}"
                     ondragstart="dndStart(event)"
                     ondragend="dndEnd(event)"
                     ondragover="dndOver(event)"
                     ondragleave="dndLeave(event)"
                     ondrop="dndDrop(event)">
            ${escapeHtml(player.name)}
            ${ageBadge}
            <span class="grade-badge grade-${escapeHtml(player.grade.toLowerCase())}">${escapeHtml(player.grade)}</span>
            ${genderBadge}
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

// ========== 엑셀 다운로드 ==========

function downloadExcel() {
    if (!courtsData || courtsData.length === 0) return;
    fetch('/api/draw/excel', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': getCsrfToken() },
        body: JSON.stringify(courtsData)
    })
    .then(res => {
        if (!res.ok) throw new Error('엑셀 생성 실패');
        return res.blob();
    })
    .then(blob => {
        const d = new Date();
        const yyyymmdd = d.getFullYear().toString()
            + String(d.getMonth() + 1).padStart(2, '0')
            + String(d.getDate()).padStart(2, '0');
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `대진표_${yyyymmdd}.xlsx`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    })
    .catch(() => alert('엑셀 다운로드 중 오류가 발생했습니다.'));
}

// ========== 대진표 저장 ==========

function openSaveModal() {
    if (!currentUser) {
        alert('저장하려면 먼저 로그인해주세요.');
        openLoginModal();
        return;
    }
    document.getElementById('saveTitle').value = '';
    hideAuthMsg('saveMsg');
    document.getElementById('saveModal').classList.remove('hidden');
}

function closeSaveModal() {
    document.getElementById('saveModal').classList.add('hidden');
}

function doSaveDraw() {
    const title = document.getElementById('saveTitle').value.trim();
    if (!title) {
        showAuthMsg('saveMsg', '제목을 입력해주세요.');
        return;
    }

    fetch('/api/draws', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': getCsrfToken() },
        body: JSON.stringify({
            title,
            content: courtsData,
            courtCount,
            gamesPerPlayer
        })
    })
    .then(async res => {
        const data = await res.json();
        if (!res.ok) throw new Error(data?.message || '저장에 실패했습니다.');
        return data;
    })
    .then(() => {
        closeSaveModal();
        alert('저장되었습니다. "내 대진표"에서 확인할 수 있습니다.');
    })
    .catch(err => showAuthMsg('saveMsg', err.message));
}

// ========== 인증 ==========

let currentUser = null;

function checkAuthState() {
    fetch('/api/auth/me')
        .then(res => res.ok ? res.json() : null)
        .then(user => {
            currentUser = user;
            renderAuthWidget();
        })
        .catch(() => {
            currentUser = null;
            renderAuthWidget();
        });
}

function renderAuthWidget() {
    const el = document.getElementById('authWidget');
    if (!el) return;
    if (currentUser) {
        el.innerHTML = `
            <span class="auth-nickname">${escapeHtml(currentUser.nickname)}님</span>
            <a href="${window.location.origin}/my-draws" class="btn btn-auth">내 대진표</a>
            <button type="button" class="btn btn-auth" onclick="doLogout()">로그아웃</button>
        `;
    } else {
        el.innerHTML = `
            <button type="button" class="btn btn-auth" onclick="openLoginModal()">로그인</button>
            <button type="button" class="btn btn-auth" onclick="openSignupModal()">회원가입</button>
        `;
    }
}

function openLoginModal() {
    hideAuthMsg('loginMsg');
    document.getElementById('loginModal').classList.remove('hidden');
}

function closeLoginModal() {
    document.getElementById('loginModal').classList.add('hidden');
}

function openSignupModal() {
    hideAuthMsg('signupMsg');
    document.getElementById('signupModal').classList.remove('hidden');
}

function closeSignupModal() {
    document.getElementById('signupModal').classList.add('hidden');
}

function switchToSignup() {
    closeLoginModal();
    openSignupModal();
}

function switchToLogin() {
    closeSignupModal();
    openLoginModal();
}

function showAuthMsg(elId, msg) {
    const el = document.getElementById(elId);
    el.textContent = msg;
    el.classList.remove('hidden');
}

function hideAuthMsg(elId) {
    const el = document.getElementById(elId);
    if (el) el.classList.add('hidden');
}

const SIGNUP_PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,30}$/;
const SIGNUP_PASSWORD_RULE_MESSAGE = '비밀번호는 8~30자이며 영어, 숫자, 특수문자(!@#$%^&*)를 모두 포함해야 합니다.';

function doLogin() {
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value;

    fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': getCsrfToken() },
        body: JSON.stringify({ email, password })
    })
    .then(async res => {
        const data = await res.json();
        if (!res.ok) throw new Error(data?.message || '로그인에 실패했습니다.');
        return data;
    })
    .then(user => {
        currentUser = user;
        renderAuthWidget();
        closeLoginModal();
    })
    .catch(err => showAuthMsg('loginMsg', err.message));
}

function doSignup() {
    const email = document.getElementById('signupEmail').value.trim();
    const password = document.getElementById('signupPassword').value;
    const passwordConfirm = document.getElementById('signupPasswordConfirm').value;
    const nickname = document.getElementById('signupNickname').value.trim();
    const celno = document.getElementById('signupCelno').value.trim();

    if (!SIGNUP_PASSWORD_PATTERN.test(password)) {
        showAuthMsg('signupMsg', SIGNUP_PASSWORD_RULE_MESSAGE);
        return;
    }

    if (password !== passwordConfirm) {
        showAuthMsg('signupMsg', '비밀번호가 일치하지 않습니다.');
        return;
    }

    fetch('/api/auth/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': getCsrfToken() },
        body: JSON.stringify({ email, password, nickname, celno })
    })
    .then(async res => {
        const data = await res.json();
        if (!res.ok) throw new Error(data?.message || '회원가입에 실패했습니다.');
        return data;
    })
    .then(() => {
        closeSignupModal();
        openLoginModal();
    })
    .catch(err => showAuthMsg('signupMsg', err.message));
}

function doLogout() {
    fetch('/api/auth/logout', {
        method: 'POST',
        headers: { 'X-XSRF-TOKEN': getCsrfToken() }
    })
    .finally(() => {
        currentUser = null;
        renderAuthWidget();
        if (window.location.pathname !== '/') {
            window.location.href = '/';
        }
    });
}

// ========== 초기화 ==========
document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('playerList')) {
        for (let i = 0; i < 4; i++) addPlayer();
    }
    checkAuthState();
});
