# CLAUDE.md — MatchManager (badminton-draw)

배드민턴 번개모임 대진표 생성기 프로젝트 작업 시 참조하는 문서.

---

## 프로젝트 개요

- **서비스명**: MatchManager
- **목적**: 배드민턴 참가자 입력 → 코트·게임 자동 배분 → 대진표 출력
- **운영 URL**: Railway (main 브랜치 자동 배포)
- **로컬 포트**: 8080

---

## 기술 스택

| 구분 | 내용 |
|------|------|
| 백엔드 | Spring Boot 3.3.5, Java 17 (소스 레벨), WAR 패키징 |
| 빌드 | Maven (IntelliJ 내장 Maven 사용) |
| 프론트 | JSP (index.jsp) + 바닐라 JS (app.js) + CSS (style.css) |
| Lombok | 1.18.34 — Java 25(JBR)와 비호환, **반드시 Java 21 JDK**로 빌드 |
| Excel | Apache POI 5.2.5 |
| 검증 | Spring Validation (jakarta.validation) |

---

## 빌드 / 실행

### ⚠️ Lombok 주의사항
Lombok 1.18.34는 IntelliJ JBR(Java 25)과 비호환. Maven CLI 빌드 시 반드시 Java 21 JDK를 지정해야 함.

```powershell
# Java 21 JDK 경로
$env:JAVA_HOME = "C:\Users\이창훈\.jdks\ms-21.0.11"

# IntelliJ Maven 경로
$mvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd"

# 테스트 실행 (14개 전부 통과해야 정상)
& $mvn test -f "C:\Users\이창훈\IdeaProjects\badminton-draw\pom.xml"

# 앱 실행
& $mvn spring-boot:run -f "C:\Users\이창훈\IdeaProjects\badminton-draw\pom.xml"
```

### 권장 방법
IntelliJ에서 Java 21 SDK 설정 후 실행 (Run > MatchManagerApplication).

### Lombok annotationProcessorPaths
pom.xml에 명시적으로 선언되어 있음 — 제거하지 말 것:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>1.18.34</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

## 브랜치 & 릴리스 규칙

| 브랜치 | 용도 |
|--------|------|
| `dev` | 평소 개발 작업 |
| `main` | 운영 (Railway 자동 배포) |

**main 머지 시 순서:**
1. `dev → main` fast-forward merge
2. `pom.xml` `<version>` 버전 bump (e.g., 1.0.0 → 1.1.0)
3. 커밋: `release: vX.Y.Z`
4. 태그: `git tag vX.Y.Z`
5. `git push --tags`

현재 버전: **1.0.0**

---

## 파일 구조

```
src/main/java/com/matchmanager/
├── MatchManagerApplication.java
├── controller/
│   ├── DrawController.java        # API 엔드포인트
│   └── GlobalExceptionHandler.java
├── dto/
│   ├── DrawRequestDto.java        # PlayerDto 포함 (요청 DTO)
│   └── ExcelUploadResult.java
├── model/
│   ├── Player.java
│   ├── Court.java
│   └── Game.java
└── service/
    ├── DrawService.java           # 핵심 배분 알고리즘
    └── ExcelService.java          # 템플릿 생성 / 파싱 / 대진표 Excel

src/main/webapp/
├── WEB-INF/views/index.jsp        # 단일 페이지
├── css/style.css
├── js/app.js
├── fonts/                         # CariCadou, YoonMinGuk
└── img/                           # icon.png, title.png

src/test/java/com/matchmanager/service/
└── DrawServiceTest.java           # 14개 단위 테스트
```

---

## API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/` | index.jsp 반환 |
| POST | `/api/draw` | 대진표 생성 (JSON → `List<Court>`) |
| GET | `/api/excel-template` | 선수 입력 양식 Excel 다운로드 |
| POST | `/api/upload-excel` | Excel 파일 파싱 → PlayerDto 목록 반환 |
| POST | `/api/draw/excel` | 대진표 Excel 다운로드 (`대진표_YYYYMMDD.xlsx`) |

---

## 도메인 규칙

### 급수(Grade)
- S(최강) > A > B > C > D > E > F
- `getGradeValue()`: S=7, A=6, B=5, C=4, D=3, E=2, F=1

### 점수(TotalScore)
```
TotalScore = (gradeValue - 1) × 100 + value
범위: F+0=0 ~ S+100=700 (실질적으로 A+100=600 상한 사용)
```

### 선수 속성
| 필드 | 타입 | 규칙 |
|------|------|------|
| name | String | 필수, 최대 20자 |
| grade | String | 필수, S~F |
| value | int | 0~100, 기본값 50 |
| gender | String | 필수, "남" 또는 "여" |
| age | int | 선택, 0=미입력 / 20·30·40·45·50·55·60·65 |

### 코트 배분 알고리즘
- **절대 룰**: 코트당 4명 이상 (2v2 복식)
- **자동 계산**: `estimatedCourts = ceil(playerCount / 8.0)`
- **Snake 분배** (`fillCourtsFromGrades`): 총점 내림차순 정렬 후 짝수 라운드 좌→우, 홀수 라운드 우→좌 배정 → 코트 간 급수 균등화

### 게임 생성
- 6명 이하 코트 → 2게임
- 7명 이상 코트 → 3게임 (또는 `gamesPerPlayer`로 역산)
- 게임마다 파트너 반복 최소화 + 양 팀 점수 차 최소화로 최적 분할 선택

---

## 프론트엔드 핵심

### CSS 변수 (style.css)
```css
--clr-teal:       #1E7272  /* 메인 컬러 */
--clr-teal-mid:   #2E8C8C
--clr-teal-pale:  #C0DADA
--clr-bg:         #E9EDE8
```

### 폰트
- `CariCadou` (KERISKEDU_Line.otf) — 제목용
- `YoonMinGuk` (YoonChildfundkoreaMinGuk.otf) — 본문 강조
- `Pretendard` — 기본 본문

### 뱃지 표시 순서 (결과 모달 내 선수)
이름 → 나이뱃지 → 성별뱃지 → 급수뱃지

### 주요 JS 전역 변수
- `players[]` — 현재 입력된 선수 목록
- `courtsData[]` — 마지막 생성된 대진표 데이터 (엑셀 다운로드에 사용)
- `courtCount` / `gamesPerPlayer` — 0이면 자동

---

## Excel 관련

### 선수 입력 템플릿 (`/api/excel-template`)
- 1행: 헤더 (선수명 / 등급(S~F) / 수치(0~100) / 성별(남/여) / 나이(선택))
- 2행~: 선수 데이터
- 드롭다운 유효성 검사 포함 (grade, gender, age)

### 대진표 Excel (`/api/draw/excel`)
- 파일명: `대진표_YYYYMMDD.xlsx`
- 0행 0열: 빈 셀 (테두리 있음)
- 0행 1열~: 코트별 헤더 4열 병합 ("1코트", "2코트" ...) — 연회색 배경, 검은 글씨
- 1행~: "1게임", "2게임" ... (연회색) + 선수 4셀 (배경 없음)
- 모든 테두리: `THIN solid` 동일 굵기

---

## 테스트

```
src/test/java/com/matchmanager/service/DrawServiceTest.java
총 14개 — 코트배분(7) / Snake균형(1) / 게임생성(4) / GradeValue(2)
```

Player 생성 시 5-arg 생성자 사용: `new Player(name, grade, value, gender, age)`
- 2-arg 편의 생성자도 있음: `new Player(name, grade)` → value=50, gender="", age=0
