#!/usr/bin/env python3
"""
将 cfir/analysis-tests/testData/llt 中的旧式 cjc 期望迁移为内联诊断标记。

脚本只处理测试数据文本，不修改项目实现代码。默认 dry-run；加 --apply 后才写回 .cj。
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
LLT_ROOT = REPO_ROOT / "cfir" / "analysis-tests" / "testData" / "llt"
CFIR_ERRORS = REPO_ROOT / "cfir" / "checkers" / "gen" / "org" / "cangnova" / "cangjie" / "cfir" / "analysis" / "diagnostics" / "CfirErrors.kt"
DIAGNOSTIC_NAME_MAPPER = (
    REPO_ROOT
    / "cfir"
    / "analysis-tests"
    / "testFixtures"
    / "org"
    / "cangnova"
    / "cangjie"
    / "cfir"
    / "analysis"
    / "tests"
    / "golden"
    / "DiagnosticNameMapper.kt"
)
DEFAULT_CJC = Path(r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\bin\cjc.exe")
TEMP_ROOT = REPO_ROOT / "tmp" / "llt-inline-cjc"


OLD_EXPECTATION_MARKERS = ("/* SCAN", "// ASSERT:", "// EXPECTED:")
INLINE_OPEN_RE = re.compile(r"<!([^!<>]+)!>")
INLINE_CLOSE_RE = re.compile(r"<!>")
SCAN_BLOCK_RE = re.compile(r"/\*\s*SCAN\b.*?\*/", re.DOTALL)


@dataclass(frozen=True)
class Position:
    line: int
    column: int


@dataclass(frozen=True)
class Range:
    begin: Position
    end: Position


@dataclass(frozen=True)
class Diagnostic:
    cjc_kind: str
    severity: str
    message: str
    range: Range


@dataclass(frozen=True)
class PlannedMarker:
    path: Path
    diagnostic_name: str
    cjc_kind: str
    range: Range


@dataclass
class FileResult:
    path: Path
    changed: bool
    diagnostics: list[Diagnostic]
    planned: list[PlannedMarker]
    unmapped: list[Diagnostic]
    skipped_reason: str | None = None


def read_utf8(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def detect_newline(text: str) -> str:
    return "\r\n" if "\r\n" in text else "\n"


def write_utf8(path: Path, text: str, newline: str = "\n") -> None:
    normalized = text.replace("\r\n", "\n").replace("\r", "\n")
    path.write_text(normalized.replace("\n", newline), encoding="utf-8", newline="")


def has_old_expectation(text: str) -> bool:
    return any(marker in text for marker in OLD_EXPECTATION_MARKERS)


def strip_inline_markers(text: str) -> str:
    """清理既有 inline 标记，保证传给 cjc 的文本仍是合法仓颉源码。"""
    text = INLINE_OPEN_RE.sub("", text)
    text = INLINE_CLOSE_RE.sub("", text)
    return text


def load_cfir_names() -> set[str]:
    text = read_utf8(CFIR_ERRORS)
    return set(re.findall(r"\bval\s+([A-Z][A-Z0-9_]*)\s*:", text))


def load_mapper() -> dict[str, set[str]]:
    """读取项目已有 DiagnosticNameMapper，得到 cjc kind -> CFIR 诊断名集合。"""
    text = read_utf8(DIAGNOSTIC_NAME_MAPPER)
    pairs = re.findall(r'"([A-Z0-9_]+)"\s+to\s+"([a-zA-Z0-9_]+)"', text)
    result: dict[str, set[str]] = {}
    for project_name, cjc_kind in pairs:
        result.setdefault(cjc_kind, set()).add(project_name)
    return result


def normalize_json_output(output: str) -> dict | None:
    start = output.find("{")
    if start < 0:
        return {"Diags": []} if not output.strip() else None
    candidate = output[start:]
    try:
        return json.loads(candidate)
    except json.JSONDecodeError:
        return None


def to_position(raw: dict | None, fallback: Position) -> Position:
    if not raw:
        return fallback
    line = int(raw.get("Line") or fallback.line)
    column = int(raw.get("Column") or fallback.column)
    return Position(max(line, 1), max(column, 1))


def parse_cjc_diagnostics(output: str) -> list[Diagnostic] | None:
    data = normalize_json_output(output)
    if data is None:
        return None
    diagnostics: list[Diagnostic] = []
    for item in data.get("Diags", []):
        location = item.get("Location") or {}
        fallback = Position(
            line=max(int(location.get("Line") or 1), 1),
            column=max(int(location.get("Column") or 1), 1),
        )
        range_raw = ((item.get("MainHint") or {}).get("Range") or {})
        begin = to_position(range_raw.get("Begin"), fallback)
        end = to_position(range_raw.get("End"), begin)
        if end.line < begin.line or (end.line == begin.line and end.column <= begin.column):
            end = Position(begin.line, begin.column + 1)
        diagnostics.append(
            Diagnostic(
                cjc_kind=str(item.get("DiagKind") or "").strip(),
                severity=str(item.get("Severity") or "").strip().lower(),
                message=str(item.get("Message") or "").strip(),
                range=Range(begin=begin, end=end),
            )
        )
    return diagnostics


def ensure_temp_root() -> None:
    TEMP_ROOT.mkdir(parents=True, exist_ok=True)


def temp_path_for(source: Path) -> Path:
    relative = source.relative_to(REPO_ROOT)
    return TEMP_ROOT / relative


def compile_with_cjc(cjc: Path, source: Path, text: str, timeout: int) -> tuple[list[Diagnostic] | None, str]:
    ensure_temp_root()
    temp_path = temp_path_for(source)
    temp_path.parent.mkdir(parents=True, exist_ok=True)
    write_utf8(temp_path, strip_inline_markers(text))

    output_path = TEMP_ROOT / "out" / source.relative_to(LLT_ROOT).with_suffix("")
    output_path.parent.mkdir(parents=True, exist_ok=True)

    command = [
        str(cjc),
        "--diagnostic-format=json",
        "--error-count-limit=all",
        "-o",
        str(output_path),
        str(temp_path),
    ]
    completed = subprocess.run(
        command,
        cwd=str(REPO_ROOT),
        text=True,
        encoding="utf-8",
        errors="replace",
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        timeout=timeout,
        check=False,
    )
    parsed = parse_cjc_diagnostics(completed.stdout)
    return parsed, completed.stdout


def choose_project_name(
    diagnostic: Diagnostic,
    cjc_to_project: dict[str, set[str]],
    valid_project_names: set[str],
    use_cjc_kind_for_unmapped: bool,
) -> str | None:
    candidates = sorted(cjc_to_project.get(diagnostic.cjc_kind, set()) & valid_project_names)
    if len(candidates) == 1:
        return candidates[0]
    if len(candidates) > 1:
        # 多对一映射不能靠测试结果猜测；需要全量迁移时使用官方 cjc kind 保留真实诊断。
        return diagnostic.cjc_kind if use_cjc_kind_for_unmapped and diagnostic.cjc_kind else None
    if use_cjc_kind_for_unmapped and diagnostic.cjc_kind:
        return diagnostic.cjc_kind
    return None


def line_column_to_offset(lines: list[str], pos: Position) -> int:
    line_index = min(max(pos.line - 1, 0), len(lines) - 1)
    line = lines[line_index]
    column_index = min(max(pos.column - 1, 0), len(line))
    return sum(len(lines[i]) + 1 for i in range(line_index)) + column_index


def offset_to_line_column(text: str, offset: int) -> Position:
    line = 1
    column = 1
    for ch in text[:offset]:
        if ch == "\n":
            line += 1
            column = 1
        else:
            column += 1
    return Position(line, column)


def apply_markers(text: str, markers: list[PlannedMarker]) -> str:
    if not markers:
        return text

    normalized_text = text.replace("\r\n", "\n").replace("\r", "\n")
    lines = normalized_text.split("\n")
    grouped: dict[tuple[int, int, int, int], set[str]] = {}
    for marker in markers:
        key = (
            marker.range.begin.line,
            marker.range.begin.column,
            marker.range.end.line,
            marker.range.end.column,
        )
        grouped.setdefault(key, set()).add(marker.diagnostic_name)

    events: list[tuple[int, int, str]] = []
    for (begin_line, begin_column, end_line, end_column), names in grouped.items():
        begin = line_column_to_offset(lines, Position(begin_line, begin_column))
        end = line_column_to_offset(lines, Position(end_line, end_column))
        if end <= begin:
            end = min(begin + 1, len(normalized_text))
        payload = ", ".join(sorted(names))
        events.append((end, 0, "<!>"))
        events.append((begin, 1, f"<!{payload}!>"))

    # 从后往前插入；同一 offset 先插入关闭标记再插入打开标记，保持嵌套关系稳定。
    result = normalized_text
    for offset, order, payload in sorted(events, key=lambda item: (item[0], item[1]), reverse=True):
        result = result[:offset] + payload + result[offset:]
    return result


def strip_old_expectations(text: str) -> str:
    text = SCAN_BLOCK_RE.sub("", text)
    new_lines: list[str] = []
    for line in text.split("\n"):
        stripped = line.lstrip()
        if stripped.startswith("// ASSERT:") or stripped.startswith("// EXPECTED:"):
            continue
        for marker in ("// ASSERT:", "// EXPECTED:"):
            index = line.find(marker)
            if index >= 0:
                line = line[:index].rstrip()
        new_lines.append(line)
    cleaned = "\n".join(new_lines)
    cleaned = re.sub(r"\n{3,}", "\n\n", cleaned)
    return cleaned


def process_file(
    path: Path,
    cjc: Path,
    cjc_to_project: dict[str, set[str]],
    valid_project_names: set[str],
    timeout: int,
    include_warnings: bool,
    use_cjc_kind_for_unmapped: bool,
    apply: bool,
) -> FileResult:
    original_raw = read_utf8(path)
    newline = detect_newline(original_raw)
    original = original_raw.replace("\r\n", "\n").replace("\r", "\n")
    diagnostics, raw_output = compile_with_cjc(cjc, path, original, timeout)
    if diagnostics is None:
        return FileResult(path=path, changed=False, diagnostics=[], planned=[], unmapped=[], skipped_reason=raw_output[:300])

    filtered = [
        diagnostic
        for diagnostic in diagnostics
        if include_warnings or diagnostic.severity == "error"
    ]
    planned: list[PlannedMarker] = []
    unmapped: list[Diagnostic] = []
    for diagnostic in filtered:
        name = choose_project_name(diagnostic, cjc_to_project, valid_project_names, use_cjc_kind_for_unmapped)
        if name is None:
            unmapped.append(diagnostic)
            continue
        planned.append(
            PlannedMarker(
                path=path,
                diagnostic_name=name,
                cjc_kind=diagnostic.cjc_kind,
                range=diagnostic.range,
            )
        )

    if unmapped and not use_cjc_kind_for_unmapped:
        return FileResult(path=path, changed=False, diagnostics=filtered, planned=planned, unmapped=unmapped)

    marked = apply_markers(original, planned)
    cleaned = strip_old_expectations(marked).strip() + "\n"
    changed = cleaned != original
    if apply and changed:
        write_utf8(path, cleaned, newline=newline)
    return FileResult(path=path, changed=changed, diagnostics=filtered, planned=planned, unmapped=unmapped)


def discover_files(limit: int | None) -> list[Path]:
    files = []
    for path in sorted(LLT_ROOT.rglob("*.cj")):
        text = read_utf8(path)
        if has_old_expectation(text):
            files.append(path)
            if limit is not None and len(files) >= limit:
                break
    return files


def main() -> int:
    parser = argparse.ArgumentParser(description="Migrate LLT old cjc expectations to inline diagnostic markers.")
    parser.add_argument("--apply", action="store_true", help="write migrated .cj files")
    parser.add_argument("--limit", type=int, default=None, help="process at most N old-format files")
    parser.add_argument("--timeout", type=int, default=20, help="cjc timeout per file in seconds")
    parser.add_argument("--cjc", type=Path, default=DEFAULT_CJC, help="path to cjc.exe")
    parser.add_argument("--include-warnings", action="store_true", help="also inline warning diagnostics")
    parser.add_argument(
        "--use-cjc-kind-for-unmapped",
        action="store_true",
        help="use raw cjc DiagKind as inline tag when no project diagnostic mapping exists",
    )
    parser.add_argument("--clean-temp", action="store_true", help="remove tmp/llt-inline-cjc before processing")
    args = parser.parse_args()

    if args.clean_temp and TEMP_ROOT.exists():
        resolved = TEMP_ROOT.resolve()
        if REPO_ROOT.resolve() not in resolved.parents:
            raise RuntimeError(f"Refuse to remove temp path outside repo: {resolved}")
        shutil.rmtree(TEMP_ROOT)

    if not args.cjc.exists():
        print(f"cjc not found: {args.cjc}", file=sys.stderr)
        return 2

    valid_project_names = load_cfir_names()
    cjc_to_project = load_mapper()
    files = discover_files(args.limit)
    results: list[FileResult] = []
    for index, path in enumerate(files, start=1):
        rel = path.relative_to(REPO_ROOT)
        print(f"[{index}/{len(files)}] {rel}", flush=True)
        try:
            results.append(
                process_file(
                    path=path,
                    cjc=args.cjc,
                    cjc_to_project=cjc_to_project,
                    valid_project_names=valid_project_names,
                    timeout=args.timeout,
                    include_warnings=args.include_warnings,
                    use_cjc_kind_for_unmapped=args.use_cjc_kind_for_unmapped,
                    apply=args.apply,
                )
            )
        except subprocess.TimeoutExpired:
            results.append(FileResult(path=path, changed=False, diagnostics=[], planned=[], unmapped=[], skipped_reason="cjc timeout"))

    changed = [result for result in results if result.changed]
    skipped = [result for result in results if result.skipped_reason]
    unmapped = [result for result in results if result.unmapped]
    print()
    print(f"old-format files scanned: {len(files)}")
    print(f"files changed: {len(changed)}")
    print(f"files skipped by cjc/output: {len(skipped)}")
    print(f"files with unmapped diagnostics: {len(unmapped)}")

    for result in unmapped[:80]:
        rel = result.path.relative_to(REPO_ROOT)
        details = ", ".join(f"{d.cjc_kind}@{d.range.begin.line}:{d.range.begin.column}" for d in result.unmapped[:8])
        print(f"UNMAPPED {rel}: {details}")
    if len(unmapped) > 80:
        print(f"... {len(unmapped) - 80} more files with unmapped diagnostics")

    for result in skipped[:40]:
        rel = result.path.relative_to(REPO_ROOT)
        reason = (result.skipped_reason or "").replace("\n", " ")
        print(f"SKIPPED {rel}: {reason[:240]}")
    if len(skipped) > 40:
        print(f"... {len(skipped) - 40} more skipped files")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
