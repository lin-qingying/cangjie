# Semantic Review Checklist

## Scope

Use this checklist when reviewing and directly repairing Cangjie compiler test data one file at a time for 100 percent grammar and semantic correctness.

## Artifact Classification

- Identify the exact file currently under review.
- Identify whether it is parser, diagnostics, CFIR/LLT, analysis, resolution, type inference, codegen, or IDE data.
- Identify whether expected output is inline, directive-based, generated golden text, or external comparison output.
- If the user gives a subtree, choose one file and finish it before moving to the next.
- Default to editing the current file when a proven semantic or grammar problem is found.

## Evidence Checks

- Diagnostic definitions: confirm each diagnostic name exists in this repository and record its message.
- Cangjie grammar and semantics: confirm the rule in official docs MCP and official compiler source.
- Official compiler implementation: read the C/C++ parser, checker, resolver, type checker, or diagnostic path when it owns the construct.
- Official `cjc`: compile focused snippets with `C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\bin\cjc.exe` when the behavior can be validated directly; create temporary `.cj` files only under the repository root `tmp` directory.
- Kotlin reference: use only for fixture format when necessary, never as semantic authority.
- Do not use this repository's implementation behavior, producer path, or test result as semantic evidence.
- Do not create `cjc` probe files in the SDK, system temp, testData, source directories, or any path outside repository-root `tmp`.

## Per-Expectation Audit

For every marker, directive, diagnostic, or golden-output fragment, record:

- What construct triggers it.
- Whether the construct is legal or illegal in Cangjie.
- Which grammar or semantic rule proves that legality.
- Which diagnostic name should appear.
- Which diagnostic message should appear.
- Exactly where the diagnostic should appear in the Cangjie source.
- Whether additional diagnostics should also appear.
- Whether any current expectation is stale, redundant, misplaced, or too broad.
- The exact edit needed to make the current file match real Cangjie grammar and semantics.

## Repair Rule

- Modify the current file directly when a diagnostic is missing, wrong, misplaced, redundant, or has the wrong message.
- Modify the current file directly when the Cangjie code itself is not a valid or focused test of the intended semantic rule.
- Use real Cangjie grammar and semantics as the edit authority.
- Keep changes file-scoped; finish the current file before moving to another file.
- Produce review-only findings only when the user explicitly asks not to edit.

## Coverage Audit

- Include a legal baseline case when useful.
- Include illegal boundary cases for the semantic rule under review.
- Include interactions with type inference, overload resolution, generics, visibility, modifiers, flow, initialization, and pattern binding when relevant.
- Keep unrelated language features out of the fixture unless they are needed to prove interaction semantics.

## Rejection Criteria

Reject the test data when any item is true:

- Expected data is based only on Kotlin behavior.
- Expected data is written before reading real Cangjie grammar and semantics.
- Expected data is copied from legacy output without mapping to this repository's diagnostics.
- Diagnostic name does not exist, has the wrong message, or belongs to a different semantic condition.
- Marker placement does not correspond to the actual source range.
- Legal Cangjie code is marked as illegal, or illegal Cangjie code is accepted.
- Boundary cases are missing from a fixture whose purpose is semantic coverage.
- The fixture uses placeholder, fallback, broad, or approximate expectations.
- Multiple files are approved as a batch without independent per-file verification.
- The conclusion depends on this repository's current implementation or test result.
- A proven problem is only reported even though editing was allowed.
- A temporary `cjc` `.cj` probe file is created outside repository-root `tmp`.

## Completion Criteria

The reviewed scope is complete only when:

- Every expectation has evidence.
- Real Cangjie grammar and semantics have been read and understood for the reviewed construct.
- Docs MCP, official compiler implementation, or `cjc` validation has been used where applicable.
- Every semantic rule under test has positive and negative coverage where applicable.
- Diagnostic names match this repository's diagnostic definitions.
- Diagnostic messages match the selected diagnostic definitions.
- Marker locations match the exact Cangjie source construct that violates the rule.
- Each file is reviewed independently before moving to the next file.
- Proven problems in the current file have been directly corrected.
- No repository test run is required or treated as semantic validation.
