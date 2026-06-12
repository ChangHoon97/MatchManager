<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MatchManager — 배드민턴 대진표 생성기</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/img/icon.png">
    <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/img/icon.png">
    <link rel="preconnect" href="https://cdn.jsdelivr.net">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="container">
    <header>
        <div class="header-inner">
            <img src="${pageContext.request.contextPath}/img/title.png"
                 alt="MATCH MANAGER" class="header-title-img">
        </div>
        <p class="subtitle">참가자 성함과 급수를 입력하고 대진표를 생성하세요</p>
    </header>
    <main>
        <section class="info-box">
            <h3>📋 급수 안내</h3>
            <div class="grade-legend">
                <span class="grade-badge grade-s">S</span>
                <span class="grade-badge grade-a">A</span>
                <span class="grade-badge grade-b">B</span>
                <span class="grade-badge grade-c">C</span>
                <span class="grade-badge grade-d">D</span>
                <span class="grade-badge grade-e">E</span>
                <span class="grade-badge grade-f">F</span>
            </div>
            <p class="info-text">
                S~F 등급까지 존재합니다.<br/>
                같은 등급 내의 실력 차이는 등급 옆 수치로 입력해주세요.(0~100까지만 입력가능)<br/>
                대진표에서 드래그앤드롭으로 수동 조정 가능합니다
            </p>
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
                <button type="button" class="btn btn-add"    onclick="addPlayer()">+선수추가</button>
                <button type="button" class="btn btn-clear"  onclick="clearAll()">전체 초기화</button>
                <button type="button" class="btn btn-sample" onclick="loadSample()">샘플데이터 불러오기</button>
            </div>

            <div class="excel-section">
                <span class="excel-label">엑셀 일괄 입력</span>
                <a href="${pageContext.request.contextPath}/api/excel-template"
                   class="btn btn-excel-dl" download>📥 양식 다운로드</a>
                <label class="btn btn-excel-ul">
                    📂 엑셀 업로드
                    <input type="file" id="excelFileInput" accept=".xlsx,.xls"
                           style="display:none" onchange="uploadExcel(this)">
                </label>
            </div>
            <div id="excelMsg" class="excel-msg hidden"></div>
        </section>

        <div id="validationMsg" class="validation-msg hidden"></div>

        <div class="court-setting">
            <span class="court-label">코트 수</span>
            <div class="court-stepper">
                <button type="button" class="stepper-btn" onclick="changeCourtCount(-1)">−</button>
                <span id="courtCountDisplay" class="stepper-val">자동</span>
                <button type="button" class="stepper-btn" onclick="changeCourtCount(+1)">+</button>
            </div>
            <span class="court-label">인당 게임 수</span>
            <div class="court-stepper">
                <button type="button" class="stepper-btn" onclick="changeGameCount(-1)">−</button>
                <span id="gameCountDisplay" class="stepper-val">자동</span>
                <button type="button" class="stepper-btn" onclick="changeGameCount(+1)">+</button>
            </div>
        </div>

        <button type="button" class="btn btn-generate" onclick="generateDraw()">
            대진표 생성하기
        </button>
    </main>
</div>

<footer class="site-footer">
    <div class="footer-inner">
        <div class="footer-brand">MATCH MANAGER</div>
        <div class="footer-divider"></div>
        <div class="footer-info">
            <div class="footer-row">
                <span class="footer-item"><span class="footer-label">Developer</span> 이창훈</span>
                <span class="footer-item"><span class="footer-label">Contact</span> hyo040441@gmail.com</span>
            </div>
            <div class="footer-row">
                <span class="footer-item"><span class="footer-label">Designer</span> 장혜원</span>
                <span class="footer-item"><span class="footer-label">Contact</span> ao_ong@naver.com</span>
            </div>
        </div>
    </div>
</footer>

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
            <button class="btn btn-print"      onclick="window.print()">🖨️ 인쇄</button>
            <button class="btn btn-regenerate" onclick="regenerate()">🔄 다시 생성</button>
            <button class="btn btn-close"      onclick="closeModal()">닫기</button>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
