# MatchManager — 배드민턴 대진표 생성기

배드민턴 번개모임에서 참가자 급수에 따라 자동으로 코트를 배분하고 균형 잡힌 대진표를 생성하는 웹 애플리케이션입니다.

---

## 주요 기능

- **참가자 입력** — 이름, 급수(S~F), 수치(0~100) 입력
- **엑셀 업로드** — 양식에 맞춘 `.xlsx` 파일로 참가자 일괄 등록
- **코트 자동 배분** — 급수 순서를 유지하며 코트별 인원을 균등하게 분배
- **대진표 생성** — 파트너 중복 최소화 + 팀 점수 균형을 동시에 고려
- **균형 표시** — 게임 카드에 팀 점수 차 기반 `균형` / `A팀 우세` / `B팀 우세` 배지 표시
- **드래그앤드롭** — 생성된 대진표에서 선수를 직접 끌어다 수동 조정
- **인쇄** — 대진표 모달에서 브라우저 인쇄 기능 제공 (등급 색상 유지)

---

## 급수 체계

| 등급 | 색상 | 점수 기준 |
|------|------|-----------|
| S    | 보라 | 600 + 수치 |
| A    | 청록 | 500 + 수치 |
| B    | 초록 | 400 + 수치 |
| C    | 연초록 | 300 + 수치 |
| D    | 황록 | 200 + 수치 |
| E    | 주황 | 100 + 수치 |
| F    | 빨강 | 0 + 수치 |

> `totalScore = (gradeNum - 1) × 100 + value`

---

## 기술 스택

| 구분 | 내용 |
|------|------|
| Backend | Spring Boot 3.3.5, Java 17 |
| View | JSP (Tomcat Embed Jasper), JSTL |
| Frontend | Vanilla JS, CSS (커스텀 폰트: KERISKEDU, YoonChildfundkorea) |
| 유효성 검증 | Jakarta Bean Validation |
| 엑셀 처리 | Apache POI 5.2.5 |
| 기타 | Lombok, Maven |

---

## 프로젝트 구조

```
src/main/
├── java/com/matchmanager/
│   ├── MatchManagerApplication.java       # 진입점
│   ├── controller/
│   │   ├── DrawController.java            # 대진표 생성 API
│   │   ├── ExcelController.java           # 엑셀 업로드/다운로드 API
│   │   ├── PageController.java            # 메인 페이지 라우팅
│   │   └── GlobalExceptionHandler.java    # 전역 예외 처리
│   ├── service/
│   │   ├── DrawService.java               # 코트 배분 + 게임 생성 핵심 로직
│   │   └── ExcelService.java              # 엑셀 파싱/생성
│   ├── model/
│   │   ├── Player.java                    # 선수 모델
│   │   ├── Court.java                     # 코트 모델
│   │   └── Game.java                      # 게임(대진) 모델
│   └── dto/
│       ├── DrawRequestDto.java            # 대진표 생성 요청 DTO
│       └── ExcelUploadResult.java         # 엑셀 업로드 응답 DTO
├── resources/
│   └── application.properties
└── webapp/
    ├── WEB-INF/views/index.jsp            # 메인 페이지
    ├── css/style.css
    ├── js/app.js
    ├── img/icon.png
    └── fonts/                             # 커스텀 폰트
```

---

## 실행 방법

### 사전 준비

- Java 17 이상
- Maven 3.x 이상 (또는 IntelliJ IDEA)

### 로컬 실행

```bash
# 프로젝트 클론
git clone <repository-url>
cd badminton-draw

# 빌드 및 실행
./mvnw spring-boot:run
```

브라우저에서 `http://localhost:8080` 접속

### IntelliJ IDEA

1. 프로젝트 열기
2. `MatchManagerApplication.java` 실행
3. `http://localhost:8080` 접속

---

## 코트 배분 알고리즘

**수동 코트 수 지정 시**
- `base = 전체인원 / 코트수`, `extra = 전체인원 % 코트수`
- 앞 `extra`개 코트에 `base+1`명, 나머지는 `base`명 배정
- 급수 순서(S→A→B→C→D→E→F)대로 carry 버퍼를 채워 각 코트 확정

**자동 모드**
- 코트당 목표 인원 ≈ 7명 기준으로 코트 수 추정
- 급수 경계를 최대한 유지하며 배분

---

## 크레딧

| 역할 | 이름 | 연락처 |
|------|------|--------|
| Developer | 이창훈 | hyo040441@gmail.com |
| Designer | 장혜원 | ao_ong@naver.com |
