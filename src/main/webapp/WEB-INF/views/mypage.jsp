<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>마이페이지 — MatchManager</title>
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
                <button type="button" class="nav-icon-btn" onclick="location.href='${pageContext.request.contextPath}/'" title="홈">⌂</button>
            </div>
            <div id="authWidget" class="auth-widget"></div>
        </div>
        <div class="header-inner">
            <a href="${pageContext.request.contextPath}/"><img src="${pageContext.request.contextPath}/img/title.png"
                 alt="MATCH MANAGER" class="header-title-img"></a>
        </div>
        <p class="subtitle">내 회원 정보 관리</p>
    </header>

    <main>
        <section class="profile-panel">
            <div class="profile-header">
                <h2>회원 정보 수정</h2>
                <p>아이디는 변경할 수 없습니다.</p>
            </div>

            <label class="profile-label" for="profileEmail">아이디</label>
            <input type="email" id="profileEmail" class="auth-input profile-readonly" readonly>

            <label class="profile-label" for="profileName">이름</label>
            <input type="text" id="profileName" class="auth-input" maxlength="20" placeholder="이름">

            <label class="profile-label" for="profileNickname">닉네임</label>
            <input type="text" id="profileNickname" class="auth-input" maxlength="20" placeholder="닉네임">

            <label class="profile-label" for="profileCelno">휴대폰번호</label>
            <input type="text" id="profileCelno" class="auth-input" maxlength="20" placeholder="휴대폰번호 (선택)">

            <div id="profileMsg" class="validation-msg hidden"></div>
            <div class="profile-actions">
                <button type="button" class="btn btn-save-draw" onclick="saveProfile()">저장</button>
            </div>
        </section>

        <section class="profile-panel password-panel">
            <div class="profile-header">
                <h2>비밀번호 변경</h2>
                <p>현재 비밀번호를 확인한 뒤 새 비밀번호로 변경합니다.</p>
            </div>

            <div id="passwordLocalFields">
                <label class="profile-label" for="currentPassword">현재 비밀번호</label>
                <input type="password" id="currentPassword" class="auth-input" placeholder="현재 비밀번호">

                <label class="profile-label" for="newPassword">새 비밀번호</label>
                <input type="password" id="newPassword" class="auth-input" placeholder="새 비밀번호 (8~30자, 영어/숫자/!@#$%^&* 포함)">

                <label class="profile-label" for="newPasswordConfirm">새 비밀번호 확인</label>
                <input type="password" id="newPasswordConfirm" class="auth-input" placeholder="새 비밀번호 확인">

                <div id="passwordMsg" class="validation-msg hidden"></div>
                <div class="profile-actions">
                    <button type="button" class="btn btn-save-draw" onclick="changePassword()">비밀번호 변경</button>
                </div>
            </div>

            <div id="passwordOAuthNotice" class="profile-notice hidden">
                Google 로그인 계정은 MatchManager에서 비밀번호를 변경할 수 없습니다.
            </div>
        </section>
    </main>
</div>

<script src="${pageContext.request.contextPath}/js/app.js"></script>
<script src="${pageContext.request.contextPath}/js/myPage.js"></script>
</body>
</html>
