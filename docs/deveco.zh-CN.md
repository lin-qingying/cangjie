# DevEco Studio 集成

[English](deveco.md) | [文档索引](README.zh-CN.md) | [前端工件](../prepare/README.md)

DevEco 宿主是独立版本管理的 Gradle 构建，通常以 `deveco/` 的目录名放在前端模块旁边，拥有自己的 settings 与产品。本指南由前端仓库维护，使未附带宿主 checkout 的前端工作树也能读取共享集成合同及中英文导航。

下文宿主文件路径指独立的宿主 checkout。其 settings、属性和 `product/build.gradle.kts` 定义安装宿主的平台、依赖解析和发行包。

## 职责与目录

前端[模块目录](module-catalog.md)定义共享的语言、分析、引用和编辑器服务。宿主领域与 IDE 模块把这些服务接入产品；`modules/deveco-bridge/` 拥有 DevEco 专用平台集成；`product/` 拥有描述符、前端运行时 classpath、模板和随包构建支撑资源。

当前宿主 settings 在启用源码前端替换时使用 `includeBuild("../")`。产品还从宿主根目录的父目录计算前端运行时路径。因此宿主放在前端模块旁边；两条路径都必须指向本次构建使用的前端。

## 配置

| 配置 | 职责 |
| --- | --- |
| `DEVECO_HOME` / `devEcoHome` | 已安装的 DevEco 平台；环境配置优先于 Gradle 属性。 |
| `devecoSyncPlatformVersion` | 未配置安装宿主路径时，宿主 settings 使用的下载 IntelliJ 平台。 |
| `pluginSinceBuild` | 产品兼容范围的下界。 |
| `devecoUseSourceFrontend` | 启用 composite 前端依赖替换；声明的默认值为 false。 |
| `cangjieIdeVersion` | 查找各个前端运行时 JAR 使用的版本。 |
| `pluginVersion` | 宿主发行包版本。 |

宿主 Java 产品和 Kotlin module convention 使用 JDK 21；前端保留自身的 JDK 17 工具链。平台和兼容属性应对应宿主分支与实际安装产品；属性文件中机器专属的安装路径应改为目标宿主路径。

下载平台提供已配置的开发基线；DevEco 专用扩展点通过安装的 DevEco 产品验收。宿主产品在下载平台配置中启用平台测试框架，选择宿主测试前应核对所选平台是否具备该测试框架。

## 前端运行时

`product/build.gradle.kts` 声明 `cangjieFrontendRuntimeProjectPaths`，根据模块目录与 `cangjieIdeVersion` 计算各个本地 JAR 路径。列表涵盖 `flatbuffers-gen`、公共基础设施、CFIR、raw builders、entrypoint、Analysis API、stub、反编译、引用及编辑器服务。

产品的 `checkCangjieFrontendRuntime` 任务报告缺失的运行时 JAR。使用前端共享队列构建列出的项目，并匹配工件版本。源码依赖替换与产品显式的本地运行时清单分别构成打包合同的一部分。

在前端根目录使用 PowerShell 7 检查运行时准备情况：

```powershell
$devEcoCheckout = '<DevEco-宿主-checkout>'
.\gradlew-queue.bat -p $devEcoCheckout :product:checkCangjieFrontendRuntime
```

[发布工件门面](../prepare/README.md)描述前端聚合工件。变更的单个前端模块可通过其 `:<module>:jar` 任务构建。准备完整的产品运行时清单后，再次执行检查。

## 产品打包

配置已安装宿主路径后，通过共享队列构建产品：

```powershell
$devEcoInstallation = '<DevEco-Studio-安装目录>'
.\gradlew-queue.bat -p $devEcoCheckout "-PdevEcoHome=$devEcoInstallation" :product:buildPlugin
```

现有 `DEVECO_HOME` 环境值优先于上述属性。`buildPlugin` 依赖前端运行时检查和插件 runtime classpath 检查；archive base name 为 `cangjie-deveco`，发行包位于宿主产品的 `build/distributions/`。

`prepareSandbox` 将 `product/templates/` 放入插件 `lib/templates/`，将 `product/hvigor/cangjie-build-support/` 放入 `lib/hvigor/cangjie-build-support/`。宿主模板与构建环境服务读取这些安装后资源位置。变更构建支撑 payload 时，按宿主自身构建说明准备它。

## 验证

按[测试约定](../TESTING_CONVENTIONS.zh-CN.md)运行受影响的前端测试。在宿主 checkout 中，选择已配置测试框架支持的模块和平台测试；运行时及打包检查失败应保留为待修复结果。

在所选平台沙箱中核验语言服务、SDK 根、库加载、诊断、宏及变更的编辑器或工程操作。把生成发行包安装到目标 DevEco 产品，验收其扩展点、模板、工程模型和构建环境集成。前端测试、宿主打包、沙箱行为和安装产品行为分别记录。

[编译阶段](cjfir-compiler-stages.zh-CN.md)和[架构图](project-architecture-diagram.zh-CN.md)描述产品消费的共享前端。
