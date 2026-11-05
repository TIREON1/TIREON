# TIREON

[![CI](https://github.com/TIREON1/TIREON/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/TIREON1/TIREON/actions/workflows/ci.yml)

초보 운전자가 더 안전한 운전 습관을 만들 수 있도록 돕는 AI 운전 코치 앱입니다.
현대자동차그룹 Pleos Connect 환경에서 동작하도록 개발합니다.

## 주요 기능

> 현재 기획 및 초기 개발 단계입니다.

- Pleos Connect 차량 데이터 연동
- 운전 데이터 기반 운전 습관 분석
- AI 기반 운전 코칭 제공

## 기술 스택

| 항목             | 환경                         |
| ---------------- | ---------------------------- |
| Platform         | Pleos Connect                |
| OS               | Android Automotive OS (AAOS) |
| Language         | Kotlin                       |
| IDE              | Android Studio               |
| Build System     | Gradle                       |
| SDK              | Pleos Connect SDK            |
| Test Environment | Pleos Connect Emulator       |

## 요구 사항

- Android Studio
- JDK 21 (`.java-version` 참고)
- Pleos Connect Emulator
- Pleos Connect SDK (프로젝트 의존성 설정 필요)

CI는 JDK 21에서 실행되므로, Gradle 프로젝트는 Gradle 8.5 이상과 AGP 8.4 이상을 사용해야 합니다.
팀원이 다른 JDK를 사용하더라도 Java/Kotlin 타깃은 프로젝트 설정을 기준으로 맞춥니다.

## 빌드 및 실행

Gradle 프로젝트 구성 후 아래 명령을 사용할 수 있습니다.

```bash
./gradlew build        # 빌드, 단위 테스트, Android Lint
./gradlew installDebug # 에뮬레이터 또는 연결된 기기에 설치
```

## 프로젝트 구조

```
.github/
├── ISSUE_TEMPLATE/   # Bug Report, Feature Request, Task, SDK Investigation
├── scripts/          # 문서 및 템플릿 검사 스크립트
├── workflows/        # CI 워크플로
└── PULL_REQUEST_TEMPLATE.md
CONTRIBUTING.md       # 브랜치, 커밋, PR, 코딩 규칙
AGENTS.md             # AI 개발 에이전트 지침
```

## CI

| Job                 | 실행 조건            | 내용                                   |
| ------------------- | -------------------- | -------------------------------------- |
| Docs & templates    | 모든 PR              | 문서 링크, 후행 공백, YAML 검사        |
| Android build/test  | `gradlew` 존재 시    | `./gradlew build` (테스트, Lint 포함)  |

## 협업

- 개발 규칙과 협업 절차는 [CONTRIBUTING.md](CONTRIBUTING.md)를 따릅니다.
- 작업 단위는 GitHub Issue로 관리하며, 목적에 맞는 템플릿을 사용합니다.
- `main` 브랜치에 직접 Push하지 않고 Pull Request를 통해 병합합니다.

## 보안

API Key, Token, 서명 키 등 민감한 정보는 저장소에 커밋하지 않습니다.
자세한 내용은 [CONTRIBUTING.md](CONTRIBUTING.md)의 보안 규칙을 참고하세요.
