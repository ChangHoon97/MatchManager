<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>공유된 대진표 — MatchManager</title>
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
        </div>
        <div class="header-inner">
            <a href="${pageContext.request.contextPath}/"><img src="${pageContext.request.contextPath}/img/title.png"
                 alt="MATCH MANAGER" class="header-title-img"></a>
        </div>
        <p id="shareSubtitle" class="subtitle">불러오는 중...</p>
    </header>
    <main>
        <div id="unlockSection" class="hidden">
            <input type="password" id="sharePasswordInput" class="auth-input" placeholder="비밀번호를 입력해주세요" maxlength="30">
            <div id="unlockMsg" class="validation-msg hidden"></div>
            <button type="button" class="btn btn-generate" onclick="doUnlock()">열람하기</button>
        </div>
        <div id="shareContentBody">
            <!-- 언락 후 대진표가 여기에 출력됨 -->
        </div>
    </main>
</div>

<input type="hidden" id="shareToken" value="${shareToken}">
<script src="${pageContext.request.contextPath}/js/app.js"></script>
<script src="${pageContext.request.contextPath}/js/share.js"></script>
</body>
</html>
