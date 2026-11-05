#!/usr/bin/env python3
"""TIREON 문서 및 GitHub 템플릿 검사 스크립트

검사 항목:
  - Markdown 상대 링크의 대상 파일 존재 여부
  - 코드 블록 밖의 후행 공백
  - Issue 템플릿 YAML frontmatter와 .github 하위 YAML 파일의 구문 유효성
"""

from __future__ import annotations

import re
import sys
from pathlib import Path
from urllib.parse import unquote

import yaml

ROOT = Path(__file__).resolve().parents[2]
EXCLUDED_DIRS = {".git", ".gradle", ".idea", ".kotlin", "build", "out", "node_modules"}
MARKDOWN_LINK = re.compile(r"!?\[[^\]]*\]\(([^)]+)\)")
TRAILING_WHITESPACE = re.compile(r"[ \t]+$")
SCHEME_PREFIXES = ("http://", "https://", "mailto:", "tel:", "data:")
REQUIRED_TEMPLATE_KEYS = ("name", "about")

errors: list[str] = []
checked = {"markdown": 0, "yaml": 0}


def report(path: Path, line: int | None, message: str) -> None:
    relative = path.relative_to(ROOT)
    location = f"{relative}:{line}" if line else str(relative)
    errors.append(f"{location}: {message}")


def is_excluded(path: Path) -> bool:
    return bool(EXCLUDED_DIRS & set(path.relative_to(ROOT).parts))


def iter_files(suffixes: set[str]):
    for path in sorted(ROOT.rglob("*")):
        if path.is_file() and path.suffix in suffixes and not is_excluded(path):
            yield path


def check_trailing_whitespace(path: Path, text: str) -> None:
    in_code_fence = False
    for number, line in enumerate(text.splitlines(), 1):
        stripped = line.lstrip()
        if stripped.startswith(("```", "~~~")):
            in_code_fence = not in_code_fence
            continue
        if in_code_fence:
            continue
        match = TRAILING_WHITESPACE.search(line)
        if match is None:
            continue
        # Markdown의 의도적인 줄바꿈(공백 2개)은 허용한다.
        if match.group(0) == "  ":
            continue
        report(path, number, "후행 공백이 있습니다.")


def resolve_link_target(raw_target: str) -> str:
    target = raw_target.strip()
    if target.startswith("<") and ">" in target:
        target = target[1:target.index(">")]
    else:
        parts = target.split()
        target = parts[0] if parts else ""
    return target


def check_links(path: Path, text: str) -> None:
    for number, line in enumerate(text.splitlines(), 1):
        for match in MARKDOWN_LINK.finditer(line):
            target = resolve_link_target(match.group(1))
            if not target or target.startswith(SCHEME_PREFIXES) or target.startswith("#"):
                continue
            target = unquote(target.split("#", 1)[0].split("?", 1)[0])
            if not target:
                continue
            base = ROOT if target.startswith("/") else path.parent
            resolved = (base / target.lstrip("/")).resolve()
            if not resolved.exists():
                report(path, number, f"링크 대상이 존재하지 않습니다: {target}")


def load_yaml(path: Path, content: str):
    try:
        return yaml.safe_load(content)
    except yaml.YAMLError as error:
        report(path, None, f"YAML 구문 오류: {error}")
        return None


def check_issue_template(path: Path, text: str) -> None:
    if not text.startswith("---\n"):
        report(path, 1, "Issue 템플릿에 YAML frontmatter가 없습니다.")
        return
    end = text.find("\n---", 4)
    if end == -1:
        report(path, 1, "YAML frontmatter가 닫히지 않았습니다.")
        return
    data = load_yaml(path, text[4:end])
    if data is None:
        return
    if not isinstance(data, dict):
        report(path, 1, "frontmatter가 key/value 형식이 아닙니다.")
        return
    for key in REQUIRED_TEMPLATE_KEYS:
        if not data.get(key):
            report(path, 1, f"frontmatter에 '{key}' 항목이 필요합니다.")


def check_markdown(path: Path) -> None:
    checked["markdown"] += 1
    text = path.read_text(encoding="utf-8")
    check_trailing_whitespace(path, text)
    check_links(path, text)
    if path.parent == ROOT / ".github" / "ISSUE_TEMPLATE":
        check_issue_template(path, text)


def check_yaml_file(path: Path) -> None:
    checked["yaml"] += 1
    text = path.read_text(encoding="utf-8")
    check_trailing_whitespace(path, text)
    load_yaml(path, text)


def main() -> int:
    for path in iter_files({".md"}):
        check_markdown(path)
    for path in iter_files({".yml", ".yaml"}):
        check_yaml_file(path)

    if errors:
        for error in errors:
            print(f"::error::{error}")
        print(f"\n검사 실패: {len(errors)}건")
        return 1

    print(
        f"검사 통과: Markdown {checked['markdown']}개, "
        f"YAML {checked['yaml']}개"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
