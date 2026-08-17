# Cangjie

![Kotlin/JVM](https://img.shields.io/badge/Kotlin%2FJVM-7F52FF?logo=kotlin&logoColor=white)
![JDK 17](https://img.shields.io/badge/JDK-17-ED8B00?logo=openjdk&logoColor=white)
![Gradle Kotlin DSL](https://img.shields.io/badge/Gradle-Kotlin%20DSL-02303A?logo=gradle&logoColor=white)
![CFIR](https://img.shields.io/badge/IR-CFIR-455A64)
![Analysis API](https://img.shields.io/badge/API-Analysis%20API-1976D2)
![LSP / IDE](https://img.shields.io/badge/Tooling-LSP%20%2F%20IDE-5C6BC0)
![IntelliJ Platform](https://img.shields.io/badge/IntelliJ%20Platform-253.29346.379-000000?logo=intellijidea&logoColor=white)

本仓库是仓颉编程语言前端的 Kotlin/JVM 工程。它围绕 Kotlin K2 风格的前端架构组织代码，并以官方仓颉编译器前端语义为对齐目标。

## Overview

Cangjie 前端覆盖从源码到语义模型的主要链路：

- 词法、语法、PSI 与 LightTree 输入
- Raw CFIR 构建与 CFIR 数据模型
- 多阶段语义解析、诊断检查与诊断渲染
- `.cjo` 序列化与跨模块符号加载
- Analysis API、Low-Level API、stub、反编译与 light declarations
- LSP 与 IDE 共享编辑能力
- 宏展开协议与执行器
- CHIR、CodeGen、LLVM 互操作等后端相关模块

## Architecture

```text
source (.cj)
  -> PARSE
  -> MACRO_EXPAND
  -> CFIR_BUILD
  -> CFIR_RESOLVE
  -> SAVE_CJO

optional backend:
CFIR -> CHIR -> LLVM IR
```

完整阶段设计见 [`docs/cjfir-compiler-stages.md`](docs/cjfir-compiler-stages.md)，主构建包含的模块以 [`settings.gradle.kts`](settings.gradle.kts) 为准。

## Repository Layout

| Path | Purpose |
|---|---|
| `common/`, `util/`, `generators/` | 公共模型、工具与代码生成基础设施 |
| `compiler/` | 编译器配置、阶段框架、参数、前端入口、CHIR 与 CodeGen |
| `psi/` | 仓颉词法、语法与 PSI |
| `cfir/` | CFIR 数据模型、Raw CFIR、语义解析、诊断检查、序列化与分析测试 |
| `analysis/` | Analysis API、低层 API、standalone、stub、反编译与 light declarations |
| `code-insight/` | IDE 与 LSP 共享的编辑能力 |
| `lsp/` | Language Server 模块 |
| `macro/` | 宏展开接口、协议与执行器 |
| `llvm-interop/` | LLVM API 与 JNI 互操作 |
| `tests/` | 测试基础设施 |
| `prepare/` | Maven 发布工件门面 |
| `docs/` | 架构、设计、对照与计划文档 |
| `openspec/` | 规格与变更提案 |

主构建包含的模块以 [`settings.gradle.kts`](settings.gradle.kts) 为准。

## Related Repositories

- [IntelliJ Cangjie plugin](https://github.com/lin-qingying/intellij-cangjie)

## Build From Source

```powershell
.\gradlew.bat assemble
.\gradlew.bat test
```

Unix/macOS:

```bash
./gradlew assemble
./gradlew test
```

常见定向入口：

```powershell
.\gradlew.bat :compiler:frontend:build
.\gradlew.bat :cfir:cfir-tree:build
.\gradlew.bat :analysis:analysis-api-cfir:test
```

测试约定见 [`TESTING_CONVENTIONS.md`](TESTING_CONVENTIONS.md)。

## Packages

发布工件由 `prepare/` 门面模块聚合：

```powershell
.\gradlew.bat installPublicArtifacts
.\gradlew.bat publishPublicArtifacts
```

工件列表、坐标与 IDE 子项目联动方式见 [`prepare/README.md`](prepare/README.md)。

## Current State

- **泛型推断失败不再静默成功**：参数约束对推断输入（expectedType 非 proper）改为直接添加（矛盾保留，不事务回滚），配合 `ResultTypeResolver` 1a/1b + `ConstraintSystemCompleter.fixVariable` 拦截，`conflictingConstraintFamily` 等 12+ 处 fixture 统一上报 `UNABLE_TO_INFER_GENERIC_FUNC` 并锚定 callee（与官方 `sema_unable_to_infer_generic_func` 一致）；Psi 版 lambda 参数锚定与 LightTree 对齐。交集推断结果由特性开关 `AllowIntersectionTypesInInference`（默认全版本关闭）控制，关闭 = 官方对齐，开启 = Kotlin K2 兼容。修复记录见 `cfir/analysis-tests/REPAIR_LOG.md`。
- `cfir:resolve`：extend 语义检查器新增声明侧可见性视图（deserialized extend 跨包过滤），`:cfir:resolve:test` 34/34 全绿。
- 待改进：`cfir:resolve` 测试目录中 21 个引用已删除约束系统 API 的历史测试暂移至 `C:\Users\lin17\AppData\Local\Temp\opencode\zombie-tests-backup\`，需按 K2 风格 API 重写后恢复。
- 待改进：`llt/ErrMsgs/type_arg_infer*` 等约 20 个 LLT fixture 的 `UNABLE_TO_INFER_GENERIC_FUNC` 范围仍写为整个调用，按官方 cjc 应锚定 callee，需依官方跨度重写期望。

## Documentation

- [`CLAUDE.md`](CLAUDE.md) - 项目定位、模块结构与开发约束
- [`DEVELOPMENT_CONVENTIONS.md`](DEVELOPMENT_CONVENTIONS.md) - 项目级开发规范
- [`TESTING_CONVENTIONS.md`](TESTING_CONVENTIONS.md) - 测试组织与 test data 约定
- [`docs/README.md`](docs/README.md) - 文档索引
