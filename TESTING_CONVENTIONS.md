# Testing conventions

[中文](TESTING_CONVENTIONS.zh-CN.md) | [Documentation](docs/README.md)

These conventions apply to all first-party Gradle modules. Select the smallest test layer that proves the changed contract, then expand verification only when the change crosses module or compiler-phase boundaries.

## Test classification

- Test-data, diagnostics, golden-output, and multi-service scenarios use the shared test infrastructure.
- Analysis API tests that create PSI, sessions, projects, builtins, decompilers, or application services must use the shared analysis test base.
- Pure model, formatter, cache, or binary-header tests may remain direct JUnit tests only when they do not construct compiler or IDE infrastructure.
- Compiler behaviour is tested through the nearest stage or module test, with end-to-end fixtures reserved for cross-stage contracts.

## Test data and diagnostics

- Keep test-data source files, directives, inline markers, and golden outputs together under the owning module.
- Diagnostic expectations must use the repository diagnostic name and the source range required by the language contract.
- For Cangjie semantic expectations, use the official compiler and official language sources as evidence before changing a fixture.
- Generated test classes are derived artefacts. Update their source test-data and generator inputs rather than hand-editing generated output.

## Running tests

Run commands from the repository root:

```powershell
.\\gradlew.bat :<module>:test
.\\gradlew.bat :<module>:test --tests "fully.qualified.TestClass"
.\\gradlew.bat :<module>:test --tests "fully.qualified.TestClass.method name"
.\\gradlew.bat check
```

Use the module README and Gradle task listing for module-specific entry points. The documentation task is available as `validateDocumentation` and is included in `check`.

## Acceptance

- Run the affected module test and the documentation check for every documentation change.
- Keep focused, family, and full-suite outcomes separate in reports.
- If broader verification is not run, state the exact command that was run and the remaining scope.
