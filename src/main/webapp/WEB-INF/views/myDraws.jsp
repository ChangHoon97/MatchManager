<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>내 대진표 — MatchManager</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/img/icon.png">
    <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/img/icon.png">
    <link rel="preconnect" href="https://cdn.jsdelivr.net">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <script src="https://cdn.jsdelivr.net/npm/qrcodejs@1.0.0/qrcode.min.js"></script>
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
            <a href="${pageContext.request.contextPath}/"><img src="${pageContext.request.contextPath}/img/title.png"
                 alt="MATCH MANAGER" class="header-title-img"></a>
        </div>
        <p class="subtitle">내가 저장한 대진표</p>
    </header>
    <main>
        <div id="myDrawsMsg" class="validation-msg hidden"></div>
        <div id="myDrawsList" class="draw-list">
            <!-- 목록이 여기에 출력됨 -->
        </div>
    </main>
</div>

<!-- 상세 모달 -->
<div id="detailModal" class="modal hidden">
    <div class="modal-content">
        <div class="modal-header">
            <h2 id="detailTitle">🏸 대진표</h2>
            <button class="modal-close" onclick="closeDetailModal()">✕</button>
        </div>
        <div id="detailModalBody" class="modal-body">
            <!-- 결과가 여기에 출력됨 -->
        </div>
        <div class="modal-footer detail-modal-footer">
            <div class="modal-footer-left">
                <button class="btn btn-save-draw" onclick="saveScores()">점수 저장</button>
            </div>
            <div class="modal-footer-right">
                <button class="btn btn-excel-result" onclick="downloadExcel()">📊 엑셀 다운로드</button>
                <button class="btn btn-print" onclick="window.print()">🖨️ 인쇄</button>
                <button class="btn btn-close" onclick="closeDetailModal()">닫기</button>
            </div>
        </div>
    </div>
</div>

<!-- 공유 모달 -->
<div id="shareModal" class="modal hidden">
    <div class="modal-content modal-content-sm">
        <div class="modal-header">
            <h2>공유 링크 생성</h2>
            <button class="modal-close" onclick="closeShareModal()">✕</button>
        </div>
        <div class="modal-body">
            <p class="subtitle" style="margin-bottom:12px">링크를 아는 사람이 아래 비밀번호로 열람할 수 있습니다.</p>
            <input type="text" id="sharePassword" class="auth-input" placeholder="공유 비밀번호 (4자 이상)" maxlength="30">
            <div id="shareMsg" class="validation-msg hidden"></div>
            <div id="shareResult" class="hidden">
                <div class="share-url-row">
                    <input type="text" id="shareUrlResult" class="auth-input" readonly onclick="this.select()">
                    <button type="button" class="btn btn-auth btn-copy-link" onclick="copyShareUrl()">링크 복사</button>
                </div>
                <div id="qrcodeContainer" class="qrcode-container"></div>
                <button type="button" class="btn btn-google" onclick="downloadQrCode()">📥 QR 코드 다운로드</button>
            </div>
            <button type="button" class="btn btn-generate" onclick="doCreateShare()">공유 링크 만들기</button>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/app.js"></script>
<script src="${pageContext.request.contextPath}/js/myDraws.js"></script>
</body>
</html>
