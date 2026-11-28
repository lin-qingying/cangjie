# IntelliJ Platform 集成

[English](intellij-ide.md) | [文档索引](README.zh-CN.md) | [前端工件](../prepare/README.md)

IntelliJ 宿主是独立版本管理的 Gradle 构建，通常以 `intellij-ide/` 的目录名放在前端模块旁边。主仓库的 [settings](../settings.gradle.kts) 定义前端构建。本指南由前端仓库维护，使前端 checkout 和工作树均能读取集成合同及中英文入口。

下文的宿主文件路径指独立提供的宿主 checkout。其 `settings.gradle.kts`、`build.gradle.kts`、版本目录和 `build-logic/` 定义宿主实现与支持的平台版本。

## 职责归属

| 层 | 职责 |
| --- | --- |
| 前端模块 | 解析、CFIR、类型解析、诊断、`.cjo` 加载、Analysis API、引用与 code insight；见[模块目录](module-catalog.md)。 |
| 宿主领域模块 | `modules/domain/` 下的工具链、项目模型、包管理和遥测服务。 |
| 宿主 IDE 模块 | `modules/ide/` 下连接 UX、语言服务、工程、运行配置、宏、LSP 和调试器的平台适配。 |
| 宿主产品 | 插件描述符、运行时依赖、宿主模块组合、沙箱和插件发行包。 |

宿主根产品使用 `cangjie.intellij-product`，将宿主模块组合进插件，并集中声明前端运行时工件。语言和分析改动由前端模块承载；宿主适配层把这些公共合同接入平台服务。

## 接入前端源码

宿主 settings 把选定前端接入 composite build，并将前端工件替换为相应 `prepare` 项目，同时替换 `flatbuffers-gen` 和 `macro-process`。这些依赖的源码集成使用同一份前端 checkout。

| 配置 | 合同 |
| --- | --- |
| `cangjie.upstream.root` | 显式前端根目录，优先于环境配置。 |
| `CANGJIE_UPSTREAM_ROOT` | 未提供 Gradle 属性时，从宿主进程环境读取前端根目录。 |
| 相邻目录布局 | 两项均未提供时，宿主使用 `..`。 |

选定目录必须包含 `settings.gradle.kts`，缺失时宿主 settings 报错。隔离的前端工作树应显式传入其路径。[发布工件门面](../prepare/README.md)负责前端打包和发布坐标。

## 工具链与平台

宿主 Kotlin convention 使用 JDK 21；前端保留自身的 JDK 17 工具链。`baseIDE` 与 `platformVersion` 选择宿主平台；`ideRunVersion`、`ideToRunType` 和 `ideLocalPath` 控制开发 IDE；`sinceBuild` 和 `untilBuild` 定义插件兼容范围；`pluginVersion` 与 `versionSuffix` 组成产品版本。

使用所选分支宿主属性与 convention plugin 中的实际值。仓颉工程配置的运行时 SDK 和标准库根应对应该工程的真实工具链。

## 构建

在前端根目录使用 PowerShell 7。共享队列串行执行消费前端输出的 Gradle 进程：

```powershell
$frontendRoot = (Get-Location).Path
$intellijCheckout = '<IntelliJ-宿主-checkout>'
.\gradlew-queue.bat -p $intellijCheckout "-Pcangjie.upstream.root=$frontendRoot" test buildPlugin
```

`buildPlugin` 在宿主 `build/distributions/` 中生成发行包。产品资源位于 `product/idea-plugin/src/main/resources`。宿主根构建拥有运行时依赖声明和模块组合；模块的编译依赖声明本身不能定义插件分发内容。

## 验证

前端改动按[测试约定](../TESTING_CONVENTIONS.zh-CN.md)运行受影响模块测试。宿主测试覆盖平台适配；产品 convention 还定义集成和 UI 测试 source set，并配置 `testIdeUi`。针对改动的集成面选择相应宿主任务。

需要交互式开发 IDE 时，通过相同队列和 upstream-root 配置执行宿主 `runIde`。在沙箱中核验工程 SDK 根、源码与库模块、诊断、引用、宏以及受影响的编辑器操作。无界面的前端测试、插件打包和沙箱行为分别提供验证证据。

宏集成测试可以注入协议级 executor；实际宏执行还需要已配置的 SDK 宏服务。报告结果时分别记录协议测试与使用该服务的执行结果。

[编译阶段](cjfir-compiler-stages.zh-CN.md)和[架构图](project-architecture-diagram.zh-CN.md)描述宿主消费的前端边界。
