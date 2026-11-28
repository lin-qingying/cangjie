# IntelliJ Platform integration

[中文](intellij-ide.zh-CN.md) | [Documentation](README.md) | [Frontend artifacts](../prepare/README.md)

The IntelliJ host is an independently versioned Gradle build, conventionally checked out as `intellij-ide/` beside the frontend modules. The main [settings](../settings.gradle.kts) define the frontend build. This guide is maintained in the frontend repository so its integration contract and bilingual entry points are available in a frontend checkout or worktree.

Host file paths in this guide refer to the independently supplied host checkout. Its `settings.gradle.kts`, `build.gradle.kts`, version catalog and `build-logic/` define the host implementation and supported platform versions.

## Ownership

| Layer | Responsibility |
| --- | --- |
| Frontend modules | Parsing, CFIR, type resolution, diagnostics, `.cjo` loading, Analysis APIs, references and code insight; see the [module catalog](module-catalog.md). |
| Host domain modules | Toolchain, project model, package manager and telemetry services under `modules/domain/`. |
| Host IDE modules | Platform adapters for UX, language services, projects, run configurations, macros, LSP and debuggers under `modules/ide/`. |
| Host product | Plugin descriptor, runtime dependencies, composed host modules, sandbox and plugin distribution. |

The root host product applies `cangjie.intellij-product`. It composes its host modules into the plugin and declares the frontend runtime artifacts centrally. Language and analysis changes belong to the frontend modules; host adapters connect those public contracts to platform services.

## Connecting frontend sources

The host settings include the selected frontend as a composite build and substitute its frontend artifacts with the corresponding `prepare` projects. They also substitute `flatbuffers-gen` and `macro-process`. Source integration uses the same frontend checkout for these dependencies.

| Configuration | Contract |
| --- | --- |
| `cangjie.upstream.root` | Explicit frontend root; takes precedence over the environment setting. |
| `CANGJIE_UPSTREAM_ROOT` | Frontend root supplied to the host process when no Gradle property was set. |
| Adjacent layout | With neither setting, the host uses `..`. |

The selected root must contain `settings.gradle.kts`; the host settings report an error if it does not. For an isolated frontend worktree, supply its path explicitly. The [artifact facade](../prepare/README.md) owns frontend packaging and published coordinates.

## Toolchain and platform

The host Kotlin convention selects JDK 21. The frontend retains its own JDK 17 toolchain. Host platform selection is configured by `baseIDE` and `platformVersion`; `ideRunVersion`, `ideToRunType` and `ideLocalPath` control the development IDE. `sinceBuild` and `untilBuild` define the plugin compatibility range. `pluginVersion` and `versionSuffix` determine the product version.

Use the values in the host checkout's properties and convention plugins for the selected branch. The runtime SDK and standard-library roots configured in a Cangjie project must describe that project's actual toolchain.

## Building

Run from the frontend root with PowerShell 7. The shared queue serializes Gradle processes that consume frontend outputs:

```powershell
$frontendRoot = (Get-Location).Path
$intellijCheckout = '<IntelliJ-host-checkout>'
.\gradlew-queue.bat -p $intellijCheckout "-Pcangjie.upstream.root=$frontendRoot" test buildPlugin
```

`buildPlugin` creates the host distribution under its `build/distributions/`. The product resources are in `product/idea-plugin/src/main/resources`. Runtime dependency declarations and composed modules are owned by the host root build; changing a module's compile dependencies alone does not define the distributed plugin contents.

## Verification

Frontend changes require the affected frontend tests described in the [testing conventions](../TESTING_CONVENTIONS.md). Host tests cover platform adapters; its product convention also defines integration and UI test source sets and configures `testIdeUi`. Select those host tasks for the changed integration surface.

For an interactive development IDE, invoke the host `runIde` task through the same queue and upstream-root setting. Verify project SDK roots, source and library modules, diagnostics, references, macros and the affected editor operation in that sandbox. Headless frontend tests, plugin packaging and sandbox behavior provide separate evidence.

Macro integration tests can inject the protocol-level executor. Actual macro execution additionally requires the configured SDK macro service. Keep protocol tests and execution with that service distinct when reporting results.

The [compiler stages](cjfir-compiler-stages.md) and [architecture diagram](project-architecture-diagram.md) describe the frontend boundaries consumed by this host.
