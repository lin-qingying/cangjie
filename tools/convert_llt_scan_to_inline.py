#!/usr/bin/env python3
"""
将 LLT 官方 SCAN 诊断期望转换为本项目内联诊断标记。

脚本只生成 unified diff，不直接写入测试数据文件。这样批量迁移仍可通过
apply_patch 落盘，避免系统命令直接改写 .cj 文件编码。
"""

from __future__ import annotations

import argparse
import difflib
import json
import pathlib
import re
import subprocess
from collections import Counter
from dataclasses import dataclass


ROOT = pathlib.Path(__file__).resolve().parents[1]
DEFAULT_CJC = pathlib.Path(r"C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\bin\cjc.exe")

# cjc 与本项目诊断不是严格一一同名关系。这里仅记录已经按项目诊断消息
# 核对过的 canonical 映射，避免从 golden 对照表反向推导出错误诊断名。
CANONICAL_CJC_TO_PROJECT = {
    "chir_dce_unused_expression": "UNUSED_EXPRESSION",
    "chir_arithmetic_operator_overflow": "CONST_EVAL_ARITHMETIC_OVERFLOW",
    "chir_shift_length_overflow": "CONST_EVAL_SHIFT_COUNT_OVERFLOW",
    "sema_accessibility": "ACCESSIBILITY_ERROR",
    "sema_ambiguous_func_ref": "AMBIGUOUS_USE",
    "sema_except_catch_type_error": "CATCH_TYPE_MUST_EXTEND_EXCEPTION",
    "sema_generic_type_without_type_argument": "GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT",
    "sema_global_var_used_before_initialization": "USED_BEFORE_INITIALIZATION",
    "sema_illegal_place_of_calling_this_or_super": "ILLEGAL_THIS_OR_SUPER_CALL",
    "sema_illegal_place_of_calling_this_primary_constructor": "ILLEGAL_THIS_OR_SUPER_CALL",
    "sema_immutable_type_illegal_property": "EXTEND_IMMUTABLE_MUT_PROPERTY",
    "sema_interface_member_must_be_implemented": "ABSTRACT_MEMBER_NOT_IMPLEMENTED",
    "sema_interface_member_must_be_implemented_in_struct": "ABSTRACT_MEMBER_NOT_IMPLEMENTED",
    "sema_interface_is_not_extendable": "EXTEND_INTERFACE_NOT_EXTENDABLE",
    "sema_invalid_binary_expr": "INVALID_BINARY_OPERATOR",
    "sema_invalid_cfunc_arg_type": "INVALID_CFUNC_PARAMETER_TYPE",
    "sema_func_no_override_or_redefine_modifier": "NOTHING_TO_OVERRIDE",
    "sema_mismatched_types": "TYPE_MISMATCH",
    "sema_missing_overridden_func": "NOTHING_TO_OVERRIDE",
    "sema_missing_redefined_func": "NOTHING_TO_OVERRIDE",
    "sema_multiple_class_upperbounds": "ONLY_ONE_CLASS_BOUND_ALLOWED",
    "sema_need_member_implementation": "ABSTRACT_MEMBER_NOT_IMPLEMENTED",
    "sema_no_match_constructor": "NO_CONSTRUCTOR",
    "sema_no_non_param_constructor_in_super_class": "EXPLICIT_SUPER_CALL_REQUIRED",
    "sema_non_inheritable_super_class": "CLASS_NOT_OPEN_FOR_INHERITANCE",
    "sema_not_found_from_generic_upper_bounds": "GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS",
    "sema_no_match_operator_function_call": "NO_MATCHING_OPERATOR_INVOKE",
    "sema_nonexhuastive_patterns": "NON_EXHAUSTIVE_MATCH",
    "sema_overload_conflicts": "CONFLICTING_OVERLOADS",
    "sema_redef_modify_static_func": "REDEF_INSTANCE_ERROR",
    "sema_shift_count_overflow": "CONST_EVAL_SHIFT_COUNT_OVERFLOW",
    "sema_static_and_non_static_member_cannot_have_same_name": "INHERIT_MEMBER_KIND_INCONSISTENT",
    "sema_lambdaExpr_must_have_type_annotation": "LAMBDA_MUST_HAVE_TYPE_ANNOTATION",
    "sema_multiple_named_argument": "ARGUMENT_PASSED_TWICE",
    "sema_unknown_named_argument": "NAMED_PARAMETER_NOT_FOUND",
    "sema_unordered_arguments": "MIXING_NAMED_AND_POSITIONAL_ARGUMENTS",
    "sema_undeclared_identifier": "UNRESOLVED_REFERENCE",
    "sema_undeclared_type_name": "UNRESOLVED_REFERENCE",
    "sema_weak_visibility": "CANNOT_WEAKEN_ACCESS_PRIVILEGE",
}


@dataclass(frozen=True)
class Diagnostic:
    project_name: str
    start_line: int
    start_column: int
    end_line: int
    end_column: int


@dataclass(frozen=True)
class FooterDiagnostic:
    severity: str
    message: str


@dataclass(frozen=True)
class DiagnosticSpan:
    project_names: tuple[str, ...]
    start: int
    end: int


def load_project_diagnostic_names() -> set[str]:
    text = (ROOT / "cfir/checkers/gen/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrors.kt").read_text(
        encoding="utf-8",
    )
    return set(re.findall(r"val\s+([A-Z0-9_]+)\s*:", text))


def load_unique_project_mapper(project_names: set[str]) -> dict[str, str]:
    mapper_path = (
        ROOT
        / "cfir/analysis-tests/testFixtures/org/cangnova/cangjie/cfir/analysis/tests/golden/DiagnosticNameMapper.kt"
    )
    text = mapper_path.read_text(encoding="utf-8")
    project_by_cjc: dict[str, list[str]] = {}
    for match in re.finditer(r'"([A-Z0-9_]+)"\s+to\s+"([a-zA-Z0-9_]+)"', text):
        project_name = match.group(1)
        cjc_kind = match.group(2)
        if project_name in project_names:
            project_by_cjc.setdefault(cjc_kind, []).append(project_name)

    return {
        cjc_kind: names[0]
        for cjc_kind, names in project_by_cjc.items()
        if len(set(names)) == 1
    }


def heuristic_project_name(cjc_kind: str) -> str:
    stripped = re.sub(r"^(sema|parse|package|chir|lex)_", "", cjc_kind)
    stripped = re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", stripped)
    return stripped.upper()


def map_cjc_kind(
    cjc_kind: str,
    unique_mapper: dict[str, str],
    project_names: set[str],
    message: str = "",
) -> str | None:
    if cjc_kind == "sema_inherit_duplicate_interface":
        project_name = "EXTEND_DUPLICATE_INTERFACE" if "has been implemented by" in message else "SUPER_TYPES_DUPLICATE"
        if project_name not in project_names:
            raise RuntimeError(f"message-based diagnostic {project_name} is not declared in CfirErrors.kt")
        return project_name
    if cjc_kind == "sema_wrong_number_of_arguments":
        if message.startswith("extra argument given"):
            project_name = "TOO_MANY_ARGUMENTS"
        elif message.startswith("missing argument for"):
            project_name = "NO_VALUE_FOR_PARAMETER"
        else:
            return None
        if project_name not in project_names:
            raise RuntimeError(f"message-based diagnostic {project_name} is not declared in CfirErrors.kt")
        return project_name

    if cjc_kind in CANONICAL_CJC_TO_PROJECT:
        project_name = CANONICAL_CJC_TO_PROJECT[cjc_kind]
        if project_name not in project_names:
            raise RuntimeError(f"canonical diagnostic {project_name} is not declared in CfirErrors.kt")
        return project_name
    if cjc_kind in unique_mapper:
        return unique_mapper[cjc_kind]

    heuristic = heuristic_project_name(cjc_kind)
    if heuristic in project_names:
        return heuristic
    return None


def compile_with_cjc(cjc: pathlib.Path, source: pathlib.Path, text: str) -> list[dict]:
    args = [
        str(cjc),
        str(source),
        "--diagnostic-format",
        "json",
        "--error-count-limit",
        "all",
    ]
    if "%compile_lib_opt" in text:
        args.extend(["--output-type=staticlib", "-o", str(ROOT / "tmp/cjc-diag-out")])

    process = subprocess.run(args, text=True, capture_output=True, cwd=ROOT)
    output = (process.stdout or "") + (process.stderr or "")
    start = output.find("{")
    if start < 0:
        raise RuntimeError(f"cjc did not produce JSON output for {source}")
    return json.loads(output[start:]).get("Diags", [])


def remove_scan_block(text: str) -> str:
    scan = re.search(r"\r?\n/\*\s*SCAN\b.*?\*/\s*$", text, flags=re.DOTALL)
    if not scan:
        return text
    return text[: scan.start()] + "\n"


def count_scan_diagnostics(text: str) -> int:
    return len(parse_footer_diagnostics(text))


def parse_footer_diagnostics(text: str) -> list[FooterDiagnostic]:
    scan = re.search(r"\r?\n/\*\s*SCAN\b(?P<body>.*?)\*/\s*$", text, flags=re.DOTALL)
    if not scan:
        return []

    diagnostics: list[FooterDiagnostic] = []
    for match in re.finditer(r"(?m)^(error|warning):\s(?P<message>.+)$", scan.group("body")):
        diagnostics.append(FooterDiagnostic(match.group(1), match.group("message").strip()))
    return diagnostics


def filter_cjc_diagnostics_by_footer(cjc_diags: list[dict], footer_diagnostics: list[FooterDiagnostic]) -> tuple[list[dict], list[str]]:
    expected_counts = Counter(footer_diagnostics)
    filtered: list[dict] = []
    for cjc_diag in cjc_diags:
        severity = str(cjc_diag.get("Severity", "")).strip().lower()
        message = str(cjc_diag.get("Message", "")).strip()
        key = FooterDiagnostic(severity, message)
        if expected_counts[key] <= 0:
            continue
        expected_counts[key] -= 1
        filtered.append(cjc_diag)

    missing = [
        f"footer_diagnostic_not_found:{diagnostic.severity}:{diagnostic.message}"
        for diagnostic, count in expected_counts.items()
        for _ in range(count)
    ]
    return filtered, missing


def line_column_to_offset(lines: list[str], line: int, column: int) -> int:
    if line < 1:
        line = 1
    if line > len(lines):
        line = len(lines)

    offset = sum(len(lines[index]) for index in range(line - 1))
    line_text = lines[line - 1]
    column = max(1, min(column, len(line_text) + 1))
    return offset + column - 1


def expand_single_character_identifier_ranges(clean_text: str, spans: list[DiagnosticSpan]) -> list[DiagnosticSpan]:
    def is_identifier_char(char: str) -> bool:
        return char == "_" or char.isalnum()

    expanded: list[DiagnosticSpan] = []
    for span in spans:
        if span.end - span.start != 1:
            expanded.append(span)
            continue
        if span.start < 0 or span.start >= len(clean_text) or not is_identifier_char(clean_text[span.start]):
            expanded.append(span)
            continue

        start = span.start
        end = span.end
        while start > 0 and is_identifier_char(clean_text[start - 1]):
            start -= 1
        while end < len(clean_text) and is_identifier_char(clean_text[end]):
            end += 1
        expanded.append(DiagnosticSpan(span.project_names, start, end))
    return expanded


def group_diagnostics_by_range(clean_text: str, diagnostics: list[Diagnostic]) -> list[DiagnosticSpan]:
    lines = clean_text.splitlines(keepends=True)
    names_by_range: dict[tuple[int, int], list[str]] = {}
    for diagnostic in diagnostics:
        start = line_column_to_offset(lines, diagnostic.start_line, diagnostic.start_column)
        end = line_column_to_offset(lines, diagnostic.end_line, diagnostic.end_column)
        if end <= start:
            end = min(start + 1, len(clean_text))
        names_by_range.setdefault((start, end), []).append(diagnostic.project_name)

    spans: list[DiagnosticSpan] = []
    for (start, end), names in names_by_range.items():
        unique_names = tuple(dict.fromkeys(names))
        spans.append(DiagnosticSpan(unique_names, start, end))
    return expand_single_character_identifier_ranges(clean_text, spans)


def validate_non_crossing_ranges(spans: list[DiagnosticSpan]) -> list[str]:
    errors: list[str] = []
    ordered = sorted(spans, key=lambda span: (span.start, span.end))
    for index, left in enumerate(ordered):
        for right in ordered[index + 1 :]:
            if right.start >= left.end:
                break
            if left.start < right.start < left.end < right.end:
                left_names = ", ".join(left.project_names)
                right_names = ", ".join(right.project_names)
                errors.append(f"crossing_ranges:{left_names}@{left.start}-{left.end}:{right_names}@{right.start}-{right.end}")
    return errors


def insert_inline_markers(clean_text: str, diagnostics: list[Diagnostic]) -> tuple[str, list[str]]:
    spans = group_diagnostics_by_range(clean_text, diagnostics)
    range_errors = validate_non_crossing_ranges(spans)
    if range_errors:
        return "", range_errors

    openings_by_offset: dict[int, list[DiagnosticSpan]] = {}
    closings_by_offset: dict[int, list[DiagnosticSpan]] = {}
    for span in spans:
        openings_by_offset.setdefault(span.start, []).append(span)
        closings_by_offset.setdefault(span.end, []).append(span)

    result_parts: list[str] = []
    for offset in range(len(clean_text) + 1):
        for span in sorted(closings_by_offset.get(offset, []), key=lambda item: item.start, reverse=True):
            result_parts.append("<!>")
        for span in sorted(openings_by_offset.get(offset, []), key=lambda item: item.end, reverse=True):
            result_parts.append(f"<!{', '.join(span.project_names)}!>")
        if offset < len(clean_text):
            result_parts.append(clean_text[offset])
    return "".join(result_parts), []



def convert_file(
    path: pathlib.Path,
    cjc: pathlib.Path,
    unique_mapper: dict[str, str],
    project_names: set[str],
) -> tuple[str, list[str]]:
    original = path.read_text(encoding="utf-8")
    clean = remove_scan_block(original)
    footer_diagnostics = parse_footer_diagnostics(original)
    scan_diagnostic_count = len(footer_diagnostics)
    if "/* SCAN" in original and not footer_diagnostics:
        return "", ["scan_has_no_diagnostic_entries"]
    try:
        cjc_diags = compile_with_cjc(cjc, path, original)
    except Exception as exception:
        return "", [f"cjc_json_failed:{exception}"]
    cjc_diags, missing_footer_diagnostics = filter_cjc_diagnostics_by_footer(cjc_diags, footer_diagnostics)
    if missing_footer_diagnostics:
        return "", missing_footer_diagnostics

    diagnostics: list[Diagnostic] = []
    unmapped: list[str] = []
    for cjc_diag in cjc_diags:
        kind = str(cjc_diag.get("DiagKind", "")).strip()
        message = str(cjc_diag.get("Message", "")).strip()
        project_name = map_cjc_kind(kind, unique_mapper, project_names, message)
        if project_name is None:
            unmapped.append(kind)
            continue

        location = cjc_diag.get("Location") or {}
        main_hint = cjc_diag.get("MainHint") or {}
        range_info = main_hint.get("Range") or {}
        begin = range_info.get("Begin") or {}
        end = range_info.get("End") or {}
        start_line = int(begin.get("Line") or location.get("Line") or 1)
        start_column = int(begin.get("Column") or location.get("Column") or 1)
        end_line = int(end.get("Line") or start_line)
        end_column = int(end.get("Column") or start_column + 1)
        diagnostics.append(Diagnostic(project_name, start_line, start_column, end_line, end_column))

    if unmapped:
        return "", unmapped
    if len(diagnostics) < scan_diagnostic_count:
        return "", [f"footer_diagnostic_count_mismatch:cjc_mapped={len(diagnostics)},scan={scan_diagnostic_count}"]

    converted, range_errors = insert_inline_markers(clean, diagnostics)
    if range_errors:
        return "", range_errors
    diff = "".join(
        difflib.unified_diff(
            original.splitlines(keepends=True),
            converted.splitlines(keepends=True),
            fromfile=str(path).replace("\\", "/"),
            tofile=str(path).replace("\\", "/"),
        ),
    )
    return diff, []


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("paths", nargs="*", help="要转换的 .cj 文件；为空时扫描 llt 下所有 SCAN 文件")
    parser.add_argument("--cjc", type=pathlib.Path, default=DEFAULT_CJC)
    parser.add_argument("--list-full", action="store_true", help="只列出当前映射可完整转换的文件")
    parser.add_argument("--summary", action="store_true", help="只输出未映射诊断 kind 统计，不输出 diff")
    args = parser.parse_args()

    project_names = load_project_diagnostic_names()
    unique_mapper = load_unique_project_mapper(project_names)
    if args.paths:
        files = [pathlib.Path(path) for path in args.paths]
    else:
        output = subprocess.check_output(
            ["rg", "-l", r"/\* SCAN", "cfir/analysis-tests/testData/llt", "-g", "*.cj"],
            text=True,
            cwd=ROOT,
        )
        files = [pathlib.Path(line) for line in output.splitlines() if line.strip()]

    all_diffs: list[str] = []
    full_files: list[str] = []
    unmapped_by_file: dict[str, list[str]] = {}
    for path in files:
        diff, unmapped = convert_file(path, args.cjc, unique_mapper, project_names)
        if unmapped:
            unmapped_by_file[str(path)] = sorted(set(unmapped))
        elif args.list_full:
            print(str(path))
        elif args.summary:
            full_files.append(str(path))
        else:
            all_diffs.append(diff)

    if args.list_full:
        return 0
    if args.summary:
        print(f"# Full convertible files: {len(full_files)}")
        kind_counts = Counter(kind for kinds in unmapped_by_file.values() for kind in kinds)
        first_file_by_kind: dict[str, str] = {}
        for file, kinds in unmapped_by_file.items():
            for kind in kinds:
                first_file_by_kind.setdefault(kind, file)
        for kind, count in kind_counts.most_common():
            print(f"{count}\t{kind}\t{first_file_by_kind[kind]}")
        return 0

    print("".join(all_diffs), end="")
    if unmapped_by_file:
        print("\n# Unmapped diagnostics:", flush=True)
        for file, kinds in sorted(unmapped_by_file.items()):
            print(f"# {file}: {', '.join(kinds)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
