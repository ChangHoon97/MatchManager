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
</head>
<body>
<div class="container">
    <header>
        <div class="header-inner">
            <a href="${pageContext.request.contextPath}/"><img src="${pageContext.request.contextPath}/img/title.png"
                 alt="MATCH MANAGER" class="header-title-img"></a>
            <div id="authWidget" class="auth-widget"></div>
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
        <div class="modal-footer">
            <button class="btn btn-close" onclick="closeDetailModal()">닫기</button>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/app.js"></script>
<script src="${pageContext.request.contextPath}/js/myDraws.js"></script>
</body>
</html>
