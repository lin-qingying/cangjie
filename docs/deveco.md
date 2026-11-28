# DevEco Studio integration

[中文](deveco.zh-CN.md) | [Documentation](README.md) | [Frontend artifacts](../prepare/README.md)

The DevEco host is an independently versioned Gradle build, conventionally checked out as `deveco/` beside the frontend modules. It has its own settings and product. This frontend-owned guide keeps the shared integration contract and bilingual navigation available without a host checkout.

Host file paths below refer to that separate checkout. Its settings, properties and `product/build.gradle.kts` define the installed-host platform, dependency resolution and distribution.

## Ownership and layout

The frontend [module catalog](module-catalog.md) defines shared language, analysis, reference and editor services. The host's domain and IDE modules adapt those services to the product. `modules/deveco-bridge/` owns DevEco-specific platform integration; `product/` owns the descriptor, frontend runtime classpath, templates and packaged build support.

The current host settings use `includeBuild("../")` when source frontend substitution is enabled. Its product also derives frontend runtime paths from the host root's parent directory. This layout places the host beside the frontend modules; both paths must identify the frontend used for the build.

## Configuration

| Setting | Responsibility |
| --- | --- |
| `DEVECO_HOME` / `devEcoHome` | Installed DevEco platform; the environment setting takes precedence over the Gradle property. |
| `devecoSyncPlatformVersion` | Downloaded IntelliJ platform used by the host settings when no installed-host path was configured. |
| `pluginSinceBuild` | Product compatibility lower bound. |
| `devecoUseSourceFrontend` | Enables composite frontend dependency substitution; its declared default is false. |
| `cangjieIdeVersion` | Version used to locate individual frontend runtime JARs. |
| `pluginVersion` | Host distribution version. |

The host Java product and Kotlin module conventions use JDK 21. The frontend has its own JDK 17 toolchain. Select platform and compatibility properties from the host branch and the actual installed product. The properties file's machine-specific installation path must be replaced with the intended host path.

The downloaded platform provides the configured development baseline. Verification of DevEco-specific extension points uses the installed DevEco product. The host product enables the platform test framework in the downloaded-platform configuration; check the selected platform's test-framework availability before selecting host tests.

## Frontend runtime

`product/build.gradle.kts` declares `cangjieFrontendRuntimeProjectPaths` and computes each local JAR path using its module directory and `cangjieIdeVersion`. This list includes `flatbuffers-gen`, common infrastructure, CFIR, the raw builders, entrypoint, Analysis APIs, stubs, decompilers, references and editor services.

The product's `checkCangjieFrontendRuntime` task reports missing runtime JARs. Build the listed frontend projects with the shared frontend queue and match their artifact versions. Source dependency substitution and the product's explicit local runtime list are separate parts of this packaging contract.

From the frontend root, inspect the runtime preparation with PowerShell 7:

```powershell
$devEcoCheckout = '<DevEco-host-checkout>'
.\gradlew-queue.bat -p $devEcoCheckout :product:checkCangjieFrontendRuntime
```

The [artifact facade](../prepare/README.md) describes the frontend aggregates. Individual changed frontend modules can be built with their `:<module>:jar` tasks. Re-run the runtime check after preparing the complete list required by the selected product.

## Product packaging

With the installed-host path configured, build the product through the shared queue:

```powershell
$devEcoInstallation = '<DevEco-Studio-installation>'
.\gradlew-queue.bat -p $devEcoCheckout "-PdevEcoHome=$devEcoInstallation" :product:buildPlugin
```

An existing `DEVECO_HOME` environment value has precedence over that property. `buildPlugin` depends on the frontend runtime check and plugin runtime classpath check. Its archive base name is `cangjie-deveco`; the distribution is produced under the host product's `build/distributions/`.

`prepareSandbox` packages `product/templates/` into the plugin's `lib/templates/` and `product/hvigor/cangjie-build-support/` into `lib/hvigor/cangjie-build-support/`. Host template and build-environment services consume those installed resource locations. Prepare the build-support payload according to the host's own build instructions when changing it.

## Verification

Run affected frontend tests according to the [testing conventions](../TESTING_CONVENTIONS.md). In the host checkout, select module tests and platform tests supported by the configured test framework. Preserve runtime and packaging check failures as results requiring repair.

Use a sandbox for the selected platform and verify language services, SDK roots, library loading, diagnostics, macros and the changed editor or project operation. Install the generated distribution in the target DevEco product to validate its extension points, templates, project model and build-environment integration. Record frontend tests, host packaging, sandbox behavior and installed-product behavior separately.

The [compiler stages](cjfir-compiler-stages.md) and [architecture diagram](project-architecture-diagram.md) describe the shared frontend consumed by this product.
