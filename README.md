# Cangjie

[![Main tests](https://github.com/lin-qingying/cangjie/actions/workflows/main-tests.yml/badge.svg?branch=main)](https://github.com/lin-qingying/cangjie/actions/workflows/main-tests.yml)
![Kotlin/JVM](https://img.shields.io/badge/Kotlin%2FJVM-7F52FF?logo=kotlin&logoColor=white)
![Gradle Kotlin DSL](https://img.shields.io/badge/Gradle-Kotlin%20DSL-02303A?logo=gradle&logoColor=white)
![CFIR](https://img.shields.io/badge/IR-CFIR-455A64)
![Analysis API](https://img.shields.io/badge/API-Analysis%20API-1976D2)
![LSP / IDE](https://img.shields.io/badge/Tooling-LSP%20%2F%20IDE-5C6BC0)

[中文](README.zh-CN.md) · [Documentation](docs/README.md) · [Compiler stages](docs/cjfir-compiler-stages.md) · [Module catalog](docs/module-catalog.md) · [Official Cangjie compiler](https://gitcode.com/Cangjie/cangjie_compiler)

> A Kotlin/JVM implementation of the Cangjie frontend and language-tooling infrastructure.

Cangjie provides the compiler-facing and IDE-facing layers for the Cangjie language: source parsing, semantic analysis, diagnostics, language APIs, and editor services. It uses Kotlin K2 design ideas where they fit the project while treating the [official Cangjie compiler](https://gitcode.com/Cangjie/cangjie_compiler) as the language-semantics reference.

## About this repository

The main build is a reusable frontend and tooling codebase, not a standalone replacement for the official `cjc` distribution. It supplies the components that a compiler host, an IDE, an LSP host, or a test environment needs to understand Cangjie source code.

Its independently built host integrations are kept in [`intellij-ide/`](intellij-ide/README.md) and [`deveco/`](deveco/README.md). Public frontend and test-framework artifacts are assembled by [`prepare/`](prepare/README.md).

## Key capabilities

| Area | What it provides |
| --- | --- |
| Source frontends | Lexer, parser, PSI, and LightTree inputs for Raw CFIR construction |
| Semantic frontend | The CFIR model, staged declaration and body resolution, diagnostics, and `.cjo` integration |
| Tooling APIs | Public and low-level Analysis APIs, references, stubs, decompilation, light declarations, code insight, and LSP services |
| Extensible integrations | Macro execution, CHIR, JVM/LLVM code generation, and LLVM interoperation behind separate module boundaries |
| IDE integrations | Independently built IntelliJ Platform and DevEco Studio projects consuming frontend and tooling artifacts |

The project keeps source representation, semantic resolution, diagnostics, APIs, and host integrations in separate Gradle modules. `settings.gradle.kts` defines the main build; the [module catalog](docs/module-catalog.md) maps every included module to its responsibility and owner documentation.

## Architecture

```text
Cangjie source (.cj)
        │
        ▼
PSI / LightTree ──► Raw CFIR ──► macro preparation (when needed)
                                            │
                                            ▼
                          source providers and semantic resolution
                                            │
                                            ▼
                                      diagnostics
                                            │
                    ┌───────────────────────┼───────────────────────┐
                    ▼                       ▼                       ▼
             Analysis API / LSP         .cjo integration       CHIR / backends
```

Macro preparation is outside the ordinary `CfirResolvePhase` sequence. The resolve phases end at body resolution; `:cfir:checkers` then runs the diagnostics pipeline with the resolve information it needs. See [compiler stages](docs/cjfir-compiler-stages.md) for the verified pipeline and [the architecture diagram](docs/project-architecture-diagram.md) for module ownership.

## Build from source

Install Git and JDK 21, then use the checked-in Gradle Wrapper from the repository root. The main build configures JDK 21 Gradle toolchains and registers the Foojay resolver; no system Gradle installation is required.

```powershell
# Windows PowerShell
.\gradlew.bat assemble
.\gradlew.bat test
.\gradlew.bat check
```

```bash
# Linux and macOS
./gradlew assemble
./gradlew test
./gradlew check
```

The first build downloads Gradle dependencies and any required toolchain. CI runs the main test workflow on JDK 21.

### Common Gradle tasks

| Task | Purpose |
| --- | --- |
| `assemble` | Build production artifacts in the main Gradle build |
| `test` | Run the repository test suites on the JUnit Platform |
| `check` | Run the broader verification lifecycle, including documentation validation |
| `validateDocumentation` | Check maintained Markdown links, anchors, bilingual entrypoints, absolute paths, and the module catalog |
| `:compiler:frontend:build` | Build the frontend coordination module and its dependencies |
| `:cfir:resolve:test` | Run focused CFIR resolution tests |
| `:analysis:analysis-api-cfir:test` | Run focused Analysis API CFIR tests |

The [testing conventions](TESTING_CONVENTIONS.md) define test-data, generated-test, and Analysis API requirements.

## Use published artifacts

The `prepare` modules publish the public frontend and test-framework facades:

| Artifact | Intended use |
| --- | --- |
| `cangjie-frontend` | Frontend integration in a controlled JVM or IntelliJ classpath |
| `cangjie-frontend-embeddable` | Shaded frontend integration in an uncontrolled host classpath |
| `cangjie-frontend-test-infrastructure` | Reusable compiler and frontend test infrastructure |
| `cangjie-frontend-analysis-test-framework` | Reusable Analysis API test infrastructure |

Install the artifacts in Maven Local or publish them to the configured Maven target:

```powershell
.\gradlew.bat installPublicArtifacts
.\gradlew.bat publishPublicArtifacts
```

The [publication guide](prepare/README.md) lists the complete artifact set, including IDE dependency assemblies, and explains how the IntelliJ and DevEco builds consume them.

## Repository layout

| Path | Purpose |
| --- | --- |
| `compiler/`, `psi/`, `cfir/` | Compiler configuration, source representation, CFIR construction, resolution, diagnostics, serialization, and frontend tests |
| `analysis/`, `code-insight/`, `lsp/` | Analysis APIs and language services shared by IDE and LSP consumers |
| `common/`, `util/`, `generators/`, `resolution.common/` | Shared model, utilities, generators, and type-inference infrastructure |
| `macro/`, `chir/`, `llvm-interop/`, `compiler/*-codegen` | Macro and optional backend integrations |
| `prepare/`, `tests/` | Published-artifact assembly and reusable test infrastructure |
| `intellij-ide/`, `deveco/` | Independently built IntelliJ Platform and DevEco Studio integrations |
| `docs/` | Maintained architecture, module, and language-reference documentation |

## Documentation

- [Documentation index](docs/README.md) — architecture, governance, module, and language-reference entry points
- [Compiler stages](docs/cjfir-compiler-stages.md) — frontend flow, resolve phases, macro boundary, and diagnostics boundary
- [Architecture diagram](docs/project-architecture-diagram.md) — subsystem ownership and integration points
- [Module catalog](docs/module-catalog.md) — all Gradle modules in the main build
- [Development conventions](DEVELOPMENT_CONVENTIONS.md) — repository-wide implementation and change rules
- [Testing conventions](TESTING_CONVENTIONS.md) — test organization and acceptance expectations

## Contributing

Before changing a first-party module, read the development and testing conventions together with that module's owner documentation. Run the focused build or test for the module you change, then run the broader verification appropriate to the change. Updates to module boundaries, public contracts, architecture, or testing strategy must update the corresponding maintained documentation.

For language behavior or diagnostics, verify the intended result against the official Cangjie materials and `cjc`; do not infer the language rule solely from this repository's current implementation.
