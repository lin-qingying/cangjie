from __future__ import annotations

import argparse
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


SCAN_BLOCK_RE = re.compile(r"/\* SCAN.*?\*/\s*$", re.S | re.M)
COMMAND_LINE_RE = re.compile(r"^\s*//\s*(EXEC(?:-PIPE(?:-\d+)?)?|ERRCHECK|ASSERT|SCAN-IN|DEPENDENCE)\b.*$", re.M)
API_LEVEL_RE = re.compile(r"APILevel_level(?:=|\s+)(\d+)")
API_LEVEL_SYSCAP_RE = re.compile(r"APILevel_syscap(?:=|\s+)([^\s]+)")
IMPORT_PATH_RE = re.compile(r"--import-path(?:=|\s+)([^\s]+)")
MODULE_NAME_RE = re.compile(r"--module-name(?:=|\s+)(?:\"([^\"]+)\"|([^\s]+))")
DEFAULT_MESSAGE_REPLACEMENTS = [
    (re.compile(r"'[^']*'"), "''{0}''"),
    (re.compile(r"\b\d+\b"), "{0}"),
]


@dataclass
class ScanDiagnostic:
    severity: str
    message: str
    line: int | None
    column: int | None
    span: int | None
    location: str | None


@dataclass
class InlinePlacement:
    start: int
    end: int
    diagnostic_names: list[str]


def normalize_message(message: str) -> str:
    value = " ".join(message.strip().split())
    for regex, replacement in DEFAULT_MESSAGE_REPLACEMENTS:
        value = regex.sub(replacement, value)
    return value


class DiagnosticResolver:
    """
    只做“LLT 文案 -> 项目诊断名”映射。

    不读取当前 CFIR 运行结果，也不做失败兜底；命中不了就返回 None。
    """

    def __init__(self) -> None:
        self._exact = {
            "unable to infer declaration type, please add type annotation": "UNABLE_TO_INFER_DECL",
            "unable to infer return type, please add type annotation": "UNABLE_TO_INFER_RETURN_TYPE",
            "unable to infer generic argument of this function": "UNABLE_TO_INFER_GENERIC_FUNC",
            "'return' must be used inside a function body": "INVALID_RETURN",
            "the return type of subscript assignment must be 'Unit'": "INVALID_SUBSCRIPT_ASSIGN_RETURN",
            "type argument's number does not match type parameter's number": "GENERIC_ARGUMENT_NO_MATCH",
            "generic type should be used with type argument": "GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT",
            "cannot assign to immutable value": "CANNOT_ASSIGN_TO_IMMUTABLE",
            "class 'Object' of package 'std/core' is not found, cannot use '--no-prelude' option": "CORE_OBJECT_NOT_FOUND_WHEN_NO_PRELUDE",
        }
        self._patterns: list[tuple[re.Pattern[str], str | tuple[str, str]]] = [
            (re.compile(r"^invalid binary operator '.*' on type '.*' and '.*'$"), "INVALID_BINARY_OPERATOR"),
            (re.compile(r"^invalid unary operator '.*' on type '.*'$"), "INVALID_UNARY_EXPR"),
            (re.compile(r"^cannot reference '.*'\(level: \d+\) which higher than level of the current scope\(level: \d+\)$"), "APILEVEL_REF_HIGHER"),
            (re.compile(r"^inappropriate syscap '.*'$"), ("APILEVEL_SYSCAP_ERROR", "APILEVEL_SYSCAP_WARNING")),
            (re.compile(r"^import '.*' to use the '.*' expression$"), "USE_EXPR_WITHOUT_IMPORT"),
            (re.compile(r"^generic instantiation '.*' causes ambiguous function '.*'$"), "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"),
            (re.compile(r"^the upper bound '.*' of generic parameter '.*' must be class or interface$"), "UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE"),
            (re.compile(r"^user defined decl '.*' not support to inherit, implement or extend 'ThreadContext'$"), "INHERIT_THREAD_CONTEXT_INVALID"),
            (re.compile(r"^invalid argument of spawn expr, user-defined 'ThreadContext' types are prohibited now$"), "SPAWN_ARG_INVALID"),
            (re.compile(r"^extend member '.*' is not allowed to shadow members of '.*'$"), "EXTEND_MEMBER_CANNOT_SHADOW"),
            (re.compile(r"^(?:imported|enum|class|struct) type '.*' cannot extend imported interface$"), "TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE"),
            (re.compile(r"^'@Annotation' modifying non-'public' class is invisible at runtime$"), "ANNOTATION_NON_PUBLIC"),
            (re.compile(r"^'@.*' not applicable to .*$"), "ANNOTATION_NOT_APPLICABLE_JFFI"),
            (re.compile(r"^undeclared identifier '.*'$"), "UNRESOLVED_REFERENCE"),
            (re.compile(r"^redefinition of declaration '.*'$"), "REDECLARATION"),
            (re.compile(r"^function '.*' has overload conflicts$"), "CONFLICTING_OVERLOADS"),
            (re.compile(r"^the number '.*' exceeds the value range of type '.*'$"), "LITERAL_NUMERIC_OVERFLOW"),
            (re.compile(r"^arithmetic operation '.*' overflow$"), "CONST_EVAL_ARITHMETIC_OVERFLOW"),
            (re.compile(r"^(divide|modulo) by zero$"), "CONST_EVAL_DIVIDE_BY_ZERO"),
            (re.compile(r"^global/static variable '.*' is used before initialization during initializing '.*'$"), "USED_BEFORE_INITIALIZATION"),
        ]

    def resolve(self, diagnostic: ScanDiagnostic) -> str | None:
        message = " ".join(diagnostic.message.split())
        if message in self._exact:
            return self._exact[message]

        for pattern, result in self._patterns:
            if not pattern.match(message):
                continue
            if isinstance(result, tuple):
                return result[0] if diagnostic.severity == "error" else result[1]
            return result

        return None


def parse_scan_block(text: str) -> list[ScanDiagnostic]:
    match = SCAN_BLOCK_RE.search(text)
    if match is None:
        return []

    block = match.group(0)
    entries: list[ScanDiagnostic] = []
    lines = block.splitlines()
    index = 0
    while index < len(lines):
        line = lines[index].strip()
        if not (line.startswith("error:") or line.startswith("warning:")):
            index += 1
            continue

        severity, message = line.split(":", 1)
        severity = severity.strip()
        message = message.strip()
        location = None
        line_no = None
        column = None
        span = None

        cursor = index + 1
        while cursor < len(lines):
            raw = lines[cursor].rstrip("\n")
            stripped = raw.strip()
            if stripped.startswith(("error:", "warning:", "note:")):
                break
            if stripped.startswith("==>"):
                location_match = re.match(r"==>\s+([^:]+):(\d+):(\d+):", stripped)
                if location_match:
                    location = location_match.group(1)
                    line_no = int(location_match.group(2))
                    column = int(location_match.group(3))
            elif "|" in raw and "^" in raw:
                caret_match = re.search(r"(\^+)", raw)
                if caret_match:
                    span = len(caret_match.group(1))
            cursor += 1

        entries.append(
            ScanDiagnostic(
                severity=severity,
                message=message,
                line=line_no,
                column=column,
                span=span,
                location=location,
            )
        )
        index = cursor
    return entries


def line_starts(text: str) -> list[int]:
    starts = [0]
    for index, char in enumerate(text):
        if char == "\n":
            starts.append(index + 1)
    return starts


def offset_from_line_column(text: str, line: int, column: int) -> int:
    starts = line_starts(text)
    if line < 1 or line > len(starts):
        raise ValueError(f"line out of range: {line}")
    return starts[line - 1] + max(column - 1, 0)


def first_code_offset(text: str) -> int:
    for match in re.finditer(r"\S", text):
        if not text[max(0, match.start() - 2): match.start() + 1].startswith("//"):
            return match.start()
    return 0


def build_inline_placements(file_path: Path, text: str, diagnostics: list[ScanDiagnostic], resolver: DiagnosticResolver) -> tuple[list[InlinePlacement], list[str]]:
    unresolved: list[str] = []
    grouped: dict[tuple[int, int], list[str]] = {}
    for diagnostic in diagnostics:
        name = resolver.resolve(diagnostic)
        if name is None:
            unresolved.append(f"{file_path}: unmapped message: {diagnostic.severity}: {diagnostic.message}")
            continue

        if diagnostic.location and Path(diagnostic.location).name != file_path.name:
            unresolved.append(f"{file_path}: cross-file location {diagnostic.location} for {name}")
            continue

        if diagnostic.line is None or diagnostic.column is None:
            start = first_code_offset(text)
            end = start
        else:
            try:
                start = offset_from_line_column(text, diagnostic.line, diagnostic.column)
            except ValueError as exc:
                unresolved.append(f"{file_path}: {exc} for {name}")
                continue
            span = diagnostic.span or 0
            end = start + span

        grouped.setdefault((start, end), []).append(name)

    placements = [
        InlinePlacement(start=start, end=end, diagnostic_names=sorted(set(names)))
        for (start, end), names in grouped.items()
    ]
    placements.sort(key=lambda item: (item.start, item.end))

    for previous, current in zip(placements, placements[1:]):
        is_nested = current.start >= previous.start and current.end <= previous.end
        if current.start < previous.end and not is_nested:
            unresolved.append(f"{file_path}: overlapping diagnostics at {previous.start}-{previous.end} and {current.start}-{current.end}")

    return placements, unresolved


def render_inline_text(text: str, placements: Iterable[InlinePlacement]) -> str:
    rendered = text
    for placement in sorted(placements, key=lambda item: (item.start, item.end), reverse=True):
        opening = f"<!{', '.join(placement.diagnostic_names)}!>"
        closing = "<!>"
        rendered = rendered[:placement.end] + closing + rendered[placement.end:]
        rendered = rendered[:placement.start] + opening + rendered[placement.start:]
    return rendered


def collect_directives(text: str) -> tuple[list[str], list[str]]:
    directives: list[str] = []
    warnings: list[str] = []

    if re.search(r"^\s*//\s*NO_PRELUDE\s*$", text, re.M):
        directives.append("// NO_PRELUDE")

    api_levels = list(dict.fromkeys(API_LEVEL_RE.findall(text)))
    if len(api_levels) > 1:
        warnings.append(f"multiple API_LEVEL values: {api_levels}")
    elif api_levels:
        directives.append(f"// API_LEVEL: {api_levels[0]}")

    syscap_values = list(dict.fromkeys(API_LEVEL_SYSCAP_RE.findall(text)))
    if len(syscap_values) > 1:
        warnings.append(f"multiple API_LEVEL_SYSCAP values: {syscap_values}")
    elif syscap_values:
        directives.append(f"// API_LEVEL_SYSCAP: {syscap_values[0]}")

    import_paths = list(dict.fromkeys(IMPORT_PATH_RE.findall(text)))
    directives.extend(f"// IMPORT_PATH: {value}" for value in import_paths)

    module_names = {
        value for a, b in MODULE_NAME_RE.findall(text)
        for value in [a or b]
        if value
    }
    if module_names:
        warnings.append(f"module-name requires manual migration: {sorted(module_names)}")

    return directives, warnings


def clean_legacy_harness(text: str) -> str:
    cleaned = SCAN_BLOCK_RE.sub("", text)
    cleaned = COMMAND_LINE_RE.sub("", cleaned)
    cleaned = re.sub(r"\n{3,}", "\n\n", cleaned)
    return cleaned.strip() + "\n"


def migrate_file(file_path: Path, resolver: DiagnosticResolver, apply: bool) -> list[str]:
    original = file_path.read_text(encoding="utf-8")
    diagnostics = parse_scan_block(original)
    if not diagnostics:
        return []

    placements, unresolved = build_inline_placements(file_path, original, diagnostics, resolver)
    if unresolved:
        return unresolved

    directives, directive_warnings = collect_directives(original)
    if directive_warnings:
        return [f"{file_path}: {warning}" for warning in directive_warnings]

    cleaned = clean_legacy_harness(original)
    migrated = render_inline_text(cleaned, placements)

    prefix = "\n".join(dict.fromkeys(directives))
    if prefix:
        migrated = f"{prefix}\n\n{migrated.lstrip()}"

    if apply and migrated != original:
        file_path.write_text(migrated, encoding="utf-8", newline="\n")
    return []


def candidate_files(root: Path, include_dirs: list[str]) -> Iterable[Path]:
    if not include_dirs:
        yield from root.rglob("*.cj")
        return
    for relative in include_dirs:
        base = root / relative
        if base.is_file() and base.suffix == ".cj":
            yield base
        elif base.is_dir():
            yield from base.rglob("*.cj")


def main() -> int:
    parser = argparse.ArgumentParser(description="Migrate LLT source expectations into inline diagnostic testdata.")
    parser.add_argument("--root", type=Path, default=Path("cfir/analysis-tests/testData/llt"))
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--include", nargs="*", default=[])
    parser.add_argument("--report", type=Path, default=Path("build/llt-migration-unresolved.txt"))
    args = parser.parse_args()

    resolver = DiagnosticResolver()
    root = args.root.resolve()
    unresolved: list[str] = []
    migrated_count = 0

    for file_path in sorted(set(candidate_files(root, args.include))):
        original_has_scan = bool(parse_scan_block(file_path.read_text(encoding="utf-8")))
        issues = migrate_file(file_path, resolver, apply=args.apply)
        if issues:
            unresolved.extend(issues)
            continue
        if original_has_scan:
            migrated_count += 1

    args.report.parent.mkdir(parents=True, exist_ok=True)
    report_lines = [
        f"root={root}",
        f"apply={args.apply}",
        f"migrated_candidates={migrated_count}",
        f"unresolved={len(unresolved)}",
        "",
        *sorted(dict.fromkeys(unresolved)),
    ]
    args.report.write_text("\n".join(report_lines) + "\n", encoding="utf-8", newline="\n")
    print(f"report: {args.report.resolve()}")
    print(f"unresolved: {len(unresolved)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
