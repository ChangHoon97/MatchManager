<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>🏸 MatchManager — 배드민턴 대진표 생성기</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="container">
    <header>
        <h1>🏸 MatchManager</h1>
        <p class="subtitle">참가자 이름과 급수를 입력하고 대진표를 생성하세요</p>
    </header>

    <main>
        <section class="info-box">
            <h3>📋 급수 안내</h3>
            <div class="grade-legend">
                <span class="grade-badge grade-a">A</span>
                <span class="grade-badge grade-b">B</span>
                <span class="grade-badge grade-c">C</span>
                <span class="grade-badge grade-d">D</span>
                <span class="grade-badge grade-e">E</span>
                <span class="grade-badge grade-f">F</span>
            </div>
            <p class="info-text">A가 가장 강함 · 코트당 6~8명 · 복식(2v2) · 비슷한 실력끼리 배정</p>
        </section>

        <section class="input-section">
            <div class="input-header">
                <h2>참가자 입력</h2>
                <span id="playerCount" class="count-badge">0명</span>
            </div>

            <div id="playerList" class="player-list">
                <!-- 선수 입력 행이 여기에 동적으로 추가됨 -->
            </div>

            <div class="btn-group">
                <button type="button" class="btn btn-add" onclick="addPlayer()">+ 선수 추가</button>
                <button type="button" class="btn btn-sample" onclick="loadSample()">샘플 데이터 불러오기</button>
                <button type="button" class="btn btn-clear" onclick="clearAll()">전체 초기화</button>
            </div>
        </section>

        <div id="validationMsg" class="validation-msg hidden"></div>

        <button type="button" class="btn btn-generate" onclick="generateDraw()">
            🎯 대진표 생성하기
        </button>
    </main>
</div>

<!-- 결과 모달 -->
<div id="resultModal" class="modal hidden">
    <div class="modal-content">
        <div class="modal-header">
            <h2>🏸 대진표 결과</h2>
            <button class="modal-close" onclick="closeModal()">✕</button>
        </div>
        <div id="modalBody" class="modal-body">
            <!-- 결과가 여기에 출력됨 -->
        </div>
        <div class="modal-footer">
            <button class="btn btn-print" onclick="window.print()">🖨️ 인쇄</button>
            <button class="btn btn-regenerate" onclick="regenerate()">🔄 다시 생성</button>
            <button class="btn btn-close" onclick="closeModal()">닫기</button>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
