# Cangjie

[![主分支测试](https://github.com/lin-qingying/cangjie/actions/workflows/main-tests.yml/badge.svg?branch=main)](https://github.com/lin-qingying/cangjie/actions/workflows/main-tests.yml)
![Kotlin/JVM](https://img.shields.io/badge/Kotlin%2FJVM-7F52FF?logo=kotlin&logoColor=white)
![Gradle Kotlin DSL](https://img.shields.io/badge/Gradle-Kotlin%20DSL-02303A?logo=gradle&logoColor=white)
![CFIR](https://img.shields.io/badge/IR-CFIR-455A64)
![Analysis API](https://img.shields.io/badge/API-Analysis%20API-1976D2)
![LSP / IDE](https://img.shields.io/badge/Tooling-LSP%20%2F%20IDE-5C6BC0)

[English](README.md) · [文档索引](docs/README.zh-CN.md) · [编译阶段](docs/cjfir-compiler-stages.zh-CN.md) · [模块目录](docs/module-catalog.md) · [官方仓颉编译器](https://gitcode.com/Cangjie/cangjie_compiler)

> 仓颉语言前端与语言工具基础设施的 Kotlin/JVM 实现。

Cangjie 提供仓颉语言面向编译器和 IDE 的基础层：源码解析、语义分析、诊断、语言 API 和编辑器服务。其架构在适用处借鉴 Kotlin K2 的设计，而[官方仓颉编译器](https://gitcode.com/Cangjie/cangjie_compiler)始终是语言语义的对照基准。

## 关于本仓库

主构建是可复用的前端与工具基础设施，而不是官方 `cjc` 发行版的独立替代品。它提供编译器宿主、IDE、LSP 宿主和测试环境理解仓颉源码所需的组件。

独立构建的宿主集成位于 [`intellij-ide/`](intellij-ide/README.zh-CN.md) 与 [`deveco/`](deveco/README.zh-CN.md)；公共前端与测试框架工件由 [`prepare/`](prepare/README.md) 装配。

## 核心能力

| 区域 | 提供内容 |
| --- | --- |
| 源码前端 | 用于构建 Raw CFIR 的 Lexer、Parser、PSI 与 LightTree 输入 |
| 语义前端 | CFIR 模型、分阶段声明与函数体解析、诊断和 `.cjo` 集成 |
| 工具 API | 公共与低层 Analysis API、引用、stub、反编译、light declarations、code insight 与 LSP 服务 |
| 可扩展集成 | 由独立模块边界承载的宏执行、CHIR、JVM/LLVM CodeGen 和 LLVM 互操作 |
| IDE 集成 | 独立构建并消费前端与工具工件的 IntelliJ Platform 与 DevEco Studio 项目 |

项目把源码表示、语义解析、诊断、API 和宿主集成划分为独立 Gradle 模块。`settings.gradle.kts` 定义主构建；[模块目录](docs/module-catalog.md)记录每个已包含模块的职责及其归属文档。

## 架构

```text
仓颉源码 (.cj)
        │
        ▼
PSI / LightTree ──► Raw CFIR ──► 宏准备（需要时）
                                          │
                                          ▼
                            源提供者注册与语义解析
                                          │
                                          ▼
                                        诊断
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    ▼                     ▼                     ▼
             Analysis API / LSP        .cjo 集成            CHIR / 后端
```

宏准备不属于普通 `CfirResolvePhase` 序列。解析阶段以函数体解析结束；随后 `:cfir:checkers` 根据所需的解析信息运行诊断管线。经源码核实的完整管线见[编译阶段](docs/cjfir-compiler-stages.zh-CN.md)，模块归属见[架构图](docs/project-architecture-diagram.zh-CN.md)。

## 从源码构建

安装 Git 和 JDK 21 后，在仓库根目录使用随仓库提供的 Gradle Wrapper。主构建固定使用 JDK 21 Gradle toolchain 并注册 Foojay resolver；无需安装系统 Gradle。

```powershell
# Windows PowerShell
.\gradlew.bat assemble
.\gradlew.bat test
.\gradlew.bat check
```

```bash
# Linux 与 macOS
./gradlew assemble
./gradlew test
./gradlew check
```

首次构建会下载 Gradle 依赖与所需 toolchain。持续集成使用 JDK 21 运行主仓库测试工作流。

### 常用 Gradle 任务

| 任务 | 用途 |
| --- | --- |
| `assemble` | 构建主 Gradle 构建中的生产工件 |
| `test` | 在 JUnit Platform 上运行仓库测试套件 |
| `check` | 运行更完整的验证生命周期，包含文档校验 |
| `validateDocumentation` | 校验维护 Markdown 的链接、锚点、双语入口、绝对路径和模块目录 |
| `:compiler:frontend:build` | 构建前端协调模块及其依赖 |
| `:cfir:resolve:test` | 运行定向 CFIR resolve 测试 |
| `:analysis:analysis-api-cfir:test` | 运行定向 Analysis API CFIR 测试 |

[测试约定](TESTING_CONVENTIONS.zh-CN.md)定义 testData、生成测试和 Analysis API 测试要求。

## 使用已发布工件

`prepare` 模块发布以下公共前端与测试框架门面：

| 工件 | 用途 |
| --- | --- |
| `cangjie-frontend` | 在受控 JVM 或 IntelliJ 类路径中集成前端 |
| `cangjie-frontend-embeddable` | 在宿主类路径不可控时集成带 relocation 的前端 |
| `cangjie-frontend-test-infrastructure` | 复用编译器与前端测试基础设施 |
| `cangjie-frontend-analysis-test-framework` | 复用 Analysis API 测试基础设施 |

安装到 Maven Local 或发布到已配置的 Maven 目标：

```powershell
.\gradlew.bat installPublicArtifacts
.\gradlew.bat publishPublicArtifacts
```

[发布说明](prepare/README.md)列出了完整工件集合（包括 IDE 依赖装配），并说明 IntelliJ 与 DevEco 构建如何消费它们。

## 仓库布局

| 路径 | 职责 |
| --- | --- |
| `compiler/`、`psi/`、`cfir/` | 编译器配置、源码表示、CFIR 构建、解析、诊断、序列化与前端测试 |
| `analysis/`、`code-insight/`、`lsp/` | 由 IDE 与 LSP 消费者共享的 Analysis API 与语言服务 |
| `common/`、`util/`、`generators/`、`resolution.common/` | 公共模型、工具、生成器与类型推断基础设施 |
| `macro/`、`chir/`、`llvm-interop/`、`compiler/*-codegen` | 宏和可选后端集成 |
| `prepare/`、`tests/` | 发布工件装配与可复用测试基础设施 |
| `intellij-ide/`、`deveco/` | 独立构建的 IntelliJ Platform 与 DevEco Studio 集成 |
| `docs/` | 维护中的架构、模块和语言参考文档 |

## 文档

- [文档索引](docs/README.zh-CN.md)：架构、治理、模块与语言参考入口
- [编译阶段](docs/cjfir-compiler-stages.zh-CN.md)：前端流、解析阶段、宏边界与诊断边界
- [架构图](docs/project-architecture-diagram.zh-CN.md)：子系统归属与集成点
- [模块目录](docs/module-catalog.md)：主构建中的全部 Gradle 模块
- [开发约定](DEVELOPMENT_CONVENTIONS.zh-CN.md)：项目级实现与变更规则
- [测试约定](TESTING_CONVENTIONS.zh-CN.md)：测试组织与验收要求

## 参与贡献

修改一方模块前，请阅读开发约定、测试约定和模块归属文档。先运行改动模块的定向构建或测试，再运行与改动范围相称的更完整验证。改变模块边界、公共契约、架构或测试策略的变更必须同步维护对应的活跃文档。

涉及语言行为或诊断时，以官方仓颉资料和 `cjc` 验证预期结果；不要只从本仓库当前实现反推语言规则。
