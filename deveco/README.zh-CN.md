# Cangjie DevEco

[English](README.md) · [主仓库](../README.zh-CN.md)

面向 DevEco Studio 的独立构建仓颉增强插件。它打包 DevEco 专属集成，并使用主仓库提供的仓颉前端运行时。

## 项目布局

```text
deveco/
├── product/                 # 插件打包入口与 plugin.xml
├── modules/core/            # 与平台交互的通用能力
├── modules/deveco-bridge/   # DevEco 专属桥接层
├── modules/test-support/    # DevEco 测试支撑
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 前置条件与平台模式

- 使用 `runIde` 运行插件必须安装本地 DevEco Studio，并设置 `DEVECO_HOME` 或 Gradle 属性 `devEcoHome`。
- 未配置本地 DevEco 时，Gradle 使用 `devecoSyncPlatformVersion` 指定的 IntelliJ IDEA 基线完成同步。该回退只支持构建配置，不启用 `runIde` 或 searchable options 生成。
- `:product:buildPlugin` 在产出插件归档前会校验随 DevEco 打包的官方仓颉运行时资源和前端运行时 jar。

## 与主仓库联动

- 默认从 `../build/repo`、Maven Local 和已配置的远程仓库解析依赖。
- 需要针对主仓库源码联调时，传入 `-PdevecoUseSourceFrontend=true`。仅在此模式下，`settings.gradle.kts` 才会 `includeBuild("../")`，并将 `cangjie-frontend-*-for-ide` 坐标替换为源码模块。
- 未开启源码替换时，构建本项目之前需要先发布或安装所需前端工件。

## 构建与运行

```powershell
# 在 deveco/ 目录执行
.\gradlew.bat :product:buildPlugin

# 需要 DEVECO_HOME 或 -PdevEcoHome=<DevEco 安装目录>
.\gradlew.bat :product:runIde

# 使用主仓库源码模块构建
.\gradlew.bat :product:buildPlugin -PdevecoUseSourceFrontend=true
```

IDE 集成使用的前端工件见主仓库的[发布说明](../prepare/README.md)。
