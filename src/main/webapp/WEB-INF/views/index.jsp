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
        <div class="top-nav">
            <div class="nav-left">
                <button type="button" class="nav-icon-btn" onclick="location.href='${pageContext.request.contextPath}/'" title="홈">🏠</button>
            </div>
            <div id="authWidget" class="auth-widget"></div>
        </div>
        <div class="header-inner">
            <img src="${pageContext.request.contextPath}/img/title.png"
                 alt="MATCH MANAGER" class="header-title-img">
        </div>
        <p class="subtitle">참가자 성함과 급수를 입력하고 대진표를 생성하세요</p>
    </header>
    <main>
        <section class="info-box usage-guide">
            <h3>📋 사용법 안내</h3>
            <ol class="usage-steps">
                <li>
                    <strong>참가자 정보를 입력하세요.</strong>
                    선수 이름을 입력하고 나이, 급수, 성별, 수치를 선택합니다. 급수는 S~F 순서이며, 수치는 같은 급수 안에서의 실력 차이를 0~100으로 조정할 때 사용합니다.
                </li>
                <li>
                    <strong>코트 수와 인당 게임 수를 정하세요.</strong>
                    코트 수와 인당 게임 수는 1부터 선택할 수 있습니다. 참가자 수에 맞게 코트 수를 정하고, 한 사람이 몇 게임씩 참여할지 직접 설정하세요.
                </li>
                <li>
                    <strong>대진표 생성을 누르세요.</strong>
                    입력된 급수와 수치를 기준으로 코트별 실력 차이를 줄이고, 파트너 중복을 최소화해 복식 대진표를 만듭니다.
                </li>
                <li>
                    <strong>결과를 확인하고 필요하면 조정하세요.</strong>
                    생성된 대진표는 드래그앤드롭으로 선수를 옮겨 수동 조정할 수 있고, 엑셀 다운로드로 보관할 수 있습니다.
                </li>
                <li>
                    <strong>저장 및 링크 공유는 로그인 후 이용할 수 있어요.</strong>
                    대진표 생성 후 내 대진표 메뉴에 들어가 공유버튼을 통해 공유링크와 QR코드를 만들 수 있습니다.
                </li>
            </ol>
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
                <span class="excel-label-group">
                    <span class="excel-help" tabindex="0" aria-label="엑셀 업로드 방법">
                        <span class="excel-help-icon" aria-hidden="true">i</span>
                        <span class="excel-help-tooltip" role="tooltip">
                            엑셀 양식 다운로드 후 선수명, 등급, 수치, 성별, 나이를 입력하고 업로드하세요. 업로드가 완료되면 참가자 입력란에 자동으로 반영됩니다.
                        </span>
                    </span>
                    <span class="excel-label">엑셀 일괄 입력</span>
                </span>
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
                <span id="courtCountDisplay" class="stepper-val">1</span>
                <button type="button" class="stepper-btn" onclick="changeCourtCount(+1)">+</button>
            </div>
            <span class="court-label">인당 게임 수</span>
            <div class="court-stepper">
                <button type="button" class="stepper-btn" onclick="changeGameCount(-1)">−</button>
                <span id="gameCountDisplay" class="stepper-val">1</span>
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
            <button class="btn btn-save-draw"    onclick="openSaveModal()">💾 저장</button>
            <button class="btn btn-excel-result" onclick="downloadExcel()">📊 엑셀 다운로드</button>
            <button class="btn btn-print"        onclick="window.print()">🖨️ 인쇄</button>
            <button class="btn btn-regenerate"   onclick="regenerate()">🔄 다시 생성</button>
            <button class="btn btn-close"        onclick="closeModal()">닫기</button>
        </div>
    </div>
</div>

<!-- 저장 모달 -->
<div id="saveModal" class="modal hidden">
    <div class="modal-content modal-content-sm">
        <div class="modal-header">
            <h2>대진표 저장</h2>
            <button class="modal-close" onclick="closeSaveModal()">✕</button>
        </div>
        <div class="modal-body">
            <input type="text" id="saveTitle" class="auth-input" placeholder="제목 (예: 7/6 번개모임)" maxlength="200">
            <div id="saveMsg" class="validation-msg hidden"></div>
            <button type="button" class="btn btn-generate" onclick="doSaveDraw()">저장하기</button>
        </div>
    </div>
</div>

<!-- 로그인 모달 -->
<div id="loginModal" class="modal hidden">
    <div class="modal-content modal-content-sm">
        <div class="modal-header">
            <h2>로그인</h2>
            <button class="modal-close" onclick="closeLoginModal()">✕</button>
        </div>
        <div class="modal-body">
            <input type="email" id="loginEmail" class="auth-input" placeholder="이메일">
            <input type="password" id="loginPassword" class="auth-input" placeholder="비밀번호">
            <div id="loginMsg" class="validation-msg hidden"></div>
            <button type="button" class="btn btn-generate" onclick="doLogin()">로그인</button>
            <a href="${pageContext.request.contextPath}/oauth2/authorization/google" class="btn btn-google">Google로 로그인</a>
            <p class="auth-switch">계정이 없으신가요? <a href="#" onclick="switchToSignup(); return false;">회원가입</a></p>
        </div>
    </div>
</div>

<!-- 회원가입 모달 -->
<div id="signupModal" class="modal hidden">
    <div class="modal-content modal-content-sm">
        <div class="modal-header">
            <h2>회원가입</h2>
            <button class="modal-close" onclick="closeSignupModal()">✕</button>
        </div>
        <div class="modal-body">
            <input type="email" id="signupEmail" class="auth-input" placeholder="이메일">
            <input type="password" id="signupPassword" class="auth-input" placeholder="비밀번호 (8~30자, 영어/숫자/!@#$%^&* 포함)">
            <input type="password" id="signupPasswordConfirm" class="auth-input" placeholder="비밀번호 확인">
            <input type="text" id="signupName" class="auth-input" placeholder="이름" maxlength="20">
            <input type="text" id="signupNickname" class="auth-input" placeholder="닉네임" maxlength="20">
            <input type="text" id="signupCelno" class="auth-input" placeholder="휴대폰번호 (선택)">
            <div id="signupMsg" class="validation-msg hidden"></div>
            <button type="button" class="btn btn-generate" onclick="doSignup()">회원가입</button>
            <a href="${pageContext.request.contextPath}/oauth2/authorization/google" class="btn btn-google">Google로 회원가입</a>
            <p class="auth-switch">이미 계정이 있으신가요? <a href="#" onclick="switchToLogin(); return false;">로그인</a></p>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
