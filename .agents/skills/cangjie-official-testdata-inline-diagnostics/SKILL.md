---
name: cangjie-official-testdata-inline-diagnostics
description: Compile Cangjie test data with the official cjc compiler, collect JSON diagnostics including diagnostic definition names and source positions, and use that evidence to update this repository's inline diagnostic test data. Use when Codex is asked to 用官方 cjc 编译测试数据, 获取诊断定义名和位置, 更新/迁移 Cangjie 测试数据, 对齐官方诊断位置, 把底部错误定义改成内联诊断, or map official diagnostics into this repository's inline diagnostic names. Preserve the Cangjie source and grammar exactly. Read this repository only for diagnostic definition names, default messages, and existing inline test-data format examples; do not read checker, resolver, analysis, CFIR, or other implementation code that explains how diagnostics are produced.
---

# Cangjie Official Testdata Inline Diagnostics

## Overview

Compile one Cangjie test data file at a time with the official `cjc` compiler, collect the official diagnostic definition names and exact source positions, then update this repository's test data expectations.
Preserve the original Cangjie program. Only transform expectation markers or footer-defined expectations.

## Required Inputs

Start from a concrete file or a concrete batch directory.
If the input is a directory, still process one file at a time.

For each file, identify:

- The original Cangjie source body.
- Existing inline markers or footer-defined expected errors, if present.
- The official `cjc` JSON diagnostics for the source.
- The exact source spans those official diagnostics refer to.

## Official Compiler Evidence

Use the official `cjc` compiler as the semantic authority for diagnostic existence, diagnostic definition name, and source position.

Compiler lookup order:

1. Use a user-provided `cjc` path when provided.
2. Otherwise use `Get-Command cjc` on Windows or `command -v cjc` on Unix.
3. Otherwise check `CANGJIE_HOME` / `CANGJIE_STDX_PATH` only to locate the official toolchain, not to infer diagnostics from source.
4. If no official `cjc` is available, stop and report that validation is blocked.

When probing a file, run the compiler with JSON diagnostics and a pre-created output directory. On Windows:

```powershell
New-Item -ItemType Directory -Force .\tmp\cjc-diagnostics | Out-Null
cjc --diagnostic-format json --output-dir .\tmp\cjc-diagnostics <test-file>
```

If `cjc` exits non-zero after reporting diagnostics, still treat the emitted JSON diagnostics as evidence. Preserve stdout, stderr, exit code, and command line in the working notes.

If existing inline markers or footer metadata make the file uncompilable by official `cjc`, create a temporary probe copy with only test expectations stripped. Do not change Cangjie source tokens, declarations, expressions, imports, or formatting in the probe copy.

For each official diagnostic, record:

- official diagnostic definition name or identifier exactly as emitted;
- message text;
- file path;
- start line and column;
- end line and column or diagnostic length/range when emitted;
- the exact source text covered by that range.

## Allowed Reads

Read only the following categories inside this repository:

1. Diagnostic definition names.
2. Diagnostic default message definitions.
3. Existing inline diagnostic test-data files, only to copy marker syntax and file-format conventions.

Typical diagnostic definition entrypoints in this repository are:

- `cfir/checkers/gen/.../CfirErrors.kt`
- `cfir/checkers/src/.../CfirErrorsDefaultMessages.kt`
- `cfir/checkers/src/.../CfirRegisteredDiagnosticFactoriesStorage.kt`
- `common/diagnostics/src/.../rendering/*`

Use file search when paths drift. Prefer filename search such as `rg --files | rg "CfirErrors|DefaultMessages|Diagnostic.*Renderer|RegisteredDiagnostic"`.

## Forbidden Reads

Do not read this repository's diagnostic implementation paths to infer semantics or trigger conditions.
This prohibition includes, but is not limited to:

- checkers
- resolvers
- collectors
- analysis implementation
- CFIR implementation logic
- test runners and test checkers
- any code whose purpose is to explain why a diagnostic is reported

Do not inspect this repository's implementation to decide whether an official error is valid.
Only use this repository as a vocabulary source for diagnostic names and messages.

## Conversion Workflow

1. Read the target test data file.
2. Split the file into the Cangjie program body and existing expectations: inline markers, directives, or footer-defined expected errors.
3. Prepare an official-compiler probe that preserves the Cangjie program body exactly.
   Do not rewrite grammar, tokens, declarations, expressions, indentation, or ordering just to make inline markers easier.
4. Run official `cjc --diagnostic-format json` against the probe and extract diagnostic definition names, messages, and source ranges.
5. Read this repository's diagnostic definition names and default messages only if the test format needs repository diagnostic names.
   Match by diagnostic meaning and message surface, not by implementation path.
6. Read one or more existing inline diagnostic test-data files only to copy the repository's marker syntax and formatting style.
7. Map each official diagnostic to exactly one repository diagnostic name unless the repository test format intentionally uses official names.
8. Insert or update inline diagnostic markers at the exact source span reported by official `cjc`.
9. Remove stale footer-defined expectations only after every footer entry has a corresponding official diagnostic or is explicitly reported as unmapped.
10. Re-read the converted file and verify that the only semantic content change is the expectation format conversion.

Read [references/conversion-checklist.md](references/conversion-checklist.md) for the execution checklist.

## Mapping Rules

- Keep the official `cjc` diagnostic name in notes and reports even when the inline marker must use a repository diagnostic name.
- Use this repository's diagnostic name, not the official compiler's raw diagnostic identifier, when the repository already defines a corresponding diagnostic.
- Use this repository's default diagnostic message definitions as the message authority for naming alignment.
- If multiple repository diagnostics look similar, choose the one whose default message and diagnostic intent most closely match the official diagnostic.
- If no exact repository diagnostic exists, stop and report the unmapped diagnostic instead of inventing a name.
- Do not broaden, merge, or weaken diagnostics to force a mapping.
- Do not create placeholder names, compatibility aliases, or TODO diagnostics.

## Source Preservation Rules

- Do not modify Cangjie syntax.
- Do not repair, simplify, normalize, or reformat the program body.
- Do not add helper declarations, imports, or wrappers.
- Do not delete illegal code that is needed for the original error case.
- Only add inline markers and remove or rewrite expectation metadata.

## Format Rules

- Follow the repository's existing inline marker syntax exactly.
- Keep directives, file headers, and non-footer comments unless the footer migration requires a local repositioning.
- Keep marker placement tight to the exact token, expression, declaration, or type reference reported by official `cjc`.
- When several diagnostics target overlapping ranges, follow the repository's existing inline formatting style from nearby examples.

## Decision Rules

- If official `cjc` and an existing footer expectation disagree, treat official `cjc` as current evidence and report the mismatch before editing.
- If the official diagnostic range is ambiguous or the JSON schema is unclear, do not guess. Report the raw JSON field names and the ambiguity.
- If the repository contains the diagnostic name but no matching message meaning, stop and report the mismatch.
- If the file mixes parse errors and semantic errors, convert both only when official `cjc` identifies their source spans clearly.
- If a batch contains files with different footer syntaxes, derive the footer grammar file by file instead of assuming one global parser.

## Output Standard

When editing:

- Update the file directly with inline markers or corrected expectations.
- Report which files were changed.
- Report the official `cjc` command used.
- Report any official diagnostics that could not be mapped without invention.

When review-only is requested:

- Report the file.
- Report each official `cjc` diagnostic name, message, and source range.
- Report the candidate repository diagnostic name.
- Report any unmapped or ambiguous entries.

## Validation Standard

Treat the conversion as correct only when all of the following are true:

- The Cangjie program body is unchanged except for inline markers.
- Every official `cjc` diagnostic has either been mapped to an inline repository diagnostic or explicitly reported as unmapped.
- Every inserted marker uses this repository's diagnostic name vocabulary.
- Marker syntax matches this repository's existing inline diagnostic test-data format.
- The report includes the official `cjc` command and whether it exited successfully or with expected diagnostic failure.
