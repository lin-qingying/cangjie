# Conversion Checklist

## Per-file checklist

1. Read the target test data file.
2. Locate existing inline markers, directives, and footer-defined expected errors.
3. Preserve the Cangjie source body exactly.
4. Locate the official `cjc` compiler; if unavailable, stop and report the blocker.
5. Pre-create a temporary output directory for `cjc`.
6. Run `cjc --diagnostic-format json --output-dir <dir> <file-or-probe-copy>`.
7. Record stdout, stderr, exit code, command line, diagnostic definition names, messages, and source ranges.
8. Read only repository diagnostic definitions and default messages when a repository-name mapping is needed.
9. Read an inline test-data example only for marker syntax and formatting.
10. Map every official `cjc` diagnostic to a repository diagnostic name by meaning and message, unless the target test format intentionally uses official names.
11. Insert or update inline markers at the exact official source span.
12. Remove stale footer expectations only after each entry is accounted for.
13. Re-check that no Cangjie syntax or program content changed.
14. Report unmapped or ambiguous diagnostics instead of inventing names.

## Hard prohibitions

- Do not read checker or resolver implementation.
- Do not infer semantics from repository behavior.
- Do not change Cangjie grammar or source logic.
- Do not invent diagnostic names.
- Do not keep both footer expectations and inline markers unless the user explicitly asks for a transitional file.
- Do not replace official `cjc` evidence with this repository's current test output.
