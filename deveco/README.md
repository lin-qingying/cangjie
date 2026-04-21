# Cangjie DevEco

`deveco` 目录现在按独立产品构建组织，用于承载 DevEco Studio 侧的仓颉增强插件。

## 模块布局

```text
deveco/
├── product/                 # 插件打包入口与 plugin.xml
├── modules/core/            # 与平台交互的通用增强能力
├── modules/deveco-bridge/   # DevEco 专属桥接层
├── modules/test-support/    # DevEco 侧测试支撑
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 同步模式

- 默认不要求本机安装 DevEco Studio。
- 当未配置 `DEVECO_HOME` 或 `devEcoHome` 时，构建脚本会使用 `devecoSyncPlatformVersion` 指定的 IntelliJ IDEA 平台基线完成 Gradle 同步。
- 当配置了本地 DevEco 安装目录后，可直接执行 `runIde` 和 `buildPlugin`。

## 与主仓库联动

- `settings.gradle.kts` 通过 `includeBuild("../")` 接入主仓库。
- `org.cangnova.cangjie:cangjie-frontend-*-for-ide` 相关坐标会优先替换为主仓库源码模块，避免先发布再联调。

## 常用命令

```powershell
./gradlew.bat help
./gradlew.bat :product:buildPlugin
./gradlew.bat :product:runIde
```
