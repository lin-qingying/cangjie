# Macro semantic integration fixtures

These source pairs exercise ordinary post-expansion symbol and type behavior with the official Cangjie macro compiler. Each `macro-package` directory is a separate `macro package` source root; `main.cj` is the regular package that imports and invokes it.

## `topLevelGeneratedCallable`

`@GenerateTopLevel` replaces `macroCarrier` with a top-level function declaration. `useGeneratedTopLevelCallable()` must resolve `generatedTopLevel(Int64): Int64`, accept the `Int64` argument, and infer the call result as `Int64`.

IDE checks to add against this source pair:

- Completion at a `generatedTop...` prefix in `main.cj` offers `generatedTopLevel` after macro construction.
- Reference resolution on `generatedTopLevel(42)` targets the generated declaration associated with the macro expansion.
- The call expression type is `Int64`, and the parameter type is `Int64`.

## `classGeneratedMember`

`@AddGeneratedMember` mutates the class AST by adding `generatedMember(): Int64`. `useGeneratedClassMember()` constructs the class, resolves the generated member, and returns its call result as `Int64`.

IDE checks to add against this source pair:

- Completion after `value.generatedMem...` offers `generatedMember` on `GeneratedHost`.
- Reference resolution on `value.generatedMember()` targets the generated class member.
- The call expression type is `Int64`; the receiver is `GeneratedHost`.

## Harness and SDK

The repository's real macro execution harness is `AbstractCfirLightTreeMacroDiagnosticsTest` in `cfir/analysis-tests/testFixtures/org/cangnova/cangjie/cfir/analysis/tests/runners/AbstractCfirDiagnosticTestBase.kt`. `MacroConstructionEnvironmentConfigurator` materializes `// FILE:` fragments, compiles each `macro package` through the production `cjc -p <root> --compile-macro -o <dir>` orchestrator, and configures the matching `LSPMacroServer`.

These IDE semantic fixtures are intentionally kept outside `cfir/analysis-tests/testData/macro`, whose generated diagnostics suite assumes every `.cj` is registered. An IDE Analysis API integration test should load each directory as a multi-file source project and configure its macro package source root and SDK before asking for completion, reference targets, and expression types. The present Analysis API test harness does not yet install `MacroConstructionEnvironmentConfigurator` or perform macro construction, so adding these files there alone would not test macro semantics.
