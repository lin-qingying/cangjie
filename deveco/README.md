# Cangjie DevEco

[中文文档](README.zh-CN.md) · [Main repository](../README.md)

An independently built Cangjie enhancement plugin for DevEco Studio. It packages DevEco-specific integration together with the Cangjie frontend runtime consumed from the main repository.

## Project layout

```text
deveco/
├── product/                 # plugin packaging and plugin.xml
├── modules/core/            # shared platform-facing features
├── modules/deveco-bridge/   # DevEco-specific bridge
├── modules/test-support/    # DevEco test support
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Prerequisites and platform mode

- A local DevEco Studio installation is required to run the plugin with `runIde`. Set `DEVECO_HOME` or the `devEcoHome` Gradle property.
- Without a DevEco installation, Gradle synchronizes against the IntelliJ IDEA baseline from `devecoSyncPlatformVersion`. This fallback supports build configuration, but does not enable `runIde` or searchable-options generation.
- `:product:buildPlugin` verifies the bundled official Cangjie runtime resources and the frontend runtime jars before producing a plugin archive.

## Main-repository integration

- By default, dependencies are resolved from `../build/repo`, Maven Local, and configured remote repositories.
- To develop the plugin against the main repository's source modules, add `-PdevecoUseSourceFrontend=true`. Only then does `settings.gradle.kts` include `../` and substitute the `cangjie-frontend-*-for-ide` coordinates with source projects.
- When source substitution is disabled, publish or install the required frontend artifacts before building this project.

## Build and run

```powershell
# Run in deveco/
.\gradlew.bat :product:buildPlugin

# Requires DEVECO_HOME or -PdevEcoHome=<DevEco installation>
.\gradlew.bat :product:runIde

# Build against main-repository source modules
.\gradlew.bat :product:buildPlugin -PdevecoUseSourceFrontend=true
```

The main repository's [publication guide](../prepare/README.md) describes the frontend artifacts used by IDE integrations.
