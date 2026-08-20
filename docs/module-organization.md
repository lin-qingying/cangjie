# 当前模块组织

本页描述主构建的当前组织方式，不是拆分计划。模块集合以 [`settings.gradle.kts`](../settings.gradle.kts) 为准；完整、可自动核验的 101 个模块清单见[模块目录](module-catalog.md)。

## 组织原则

- 模块按稳定职责划分：公共模型、语法输入、前端语义、工具 API、编辑器服务、后端集成和发布工件各自独立。
- 公共能力经接口暴露。高层消费者不以跨模块方式依赖实现细节。
- `intellij-ide/` 与 `deveco/` 是独立 Gradle 构建，不属于主 `settings.gradle.kts` 的模块集合。
- `external/` 是参考源码和上游镜像，不是主构建的一方模块。

## 子系统

| 子系统 | 主要模块 | 职责 |
| --- | --- | --- |
| 基础设施 | `:common`、`:common:diagnostics`、`:util`、`:generators`、`:flatbuffers-gen`、`:dependencies:intellij-core` | 共享领域模型、诊断基础、工具、生成和 IntelliJ 依赖边界 |
| 编译器与语法 | `:compiler:*`、`:psi` | 配置、阶段框架、参数、前端协调、插件接缝、Lexer、Parser、PSI |
| CFIR | `:cfir:*` | Raw CFIR 构建、IR 与类型模型、providers、普通 resolve、诊断、序列化和前端测试 |
| 类型推断 | `:resolution.common` | 跨前端解析使用的约束与类型推断基础设施 |
| 宏 | `:macro:*` | 宏协议、外部执行器和测试 / IDE 桩 |
| Analysis | `:analysis:*` | 公共与低层 Analysis API、CFIR 实现、stubs、反编译、light declarations 和测试支撑 |
| 编辑器与 LSP | `:code-insight:*`、`:lsp` | 编辑器能力和语言服务器框架 |
| 后端集成 | `:chir:*`、`:compiler:codegen`、`:compiler:jvm-codegen`、`:llvm-interop:*` | CHIR、CFIR 到 CHIR 转换、JVM/LLVM 后端和 LLVM 互操作 |
| 发布 | `:prepare:*` | 面向嵌入式前端和 IDE 依赖的 Maven 工件聚合 |
| 测试 | `:tests`、`:tests:test-infrastructure`、`:cfir:analysis-tests` | 通用测试框架与 CFIR 端到端测试 |

## 前端边界

```text
source
  → :psi
  → :cfir:raw-cfir:psi2cfir or :cfir:raw-cfir:light-tree2cfir
  → :cfir:entrypoint
  → :cfir:resolve
  → :cfir:checkers and :cfir:diagnostic-renderers
  → Analysis API, editor services, .cjo integration, or backend integrations
```

宏构造在普通 resolve 之前准备扩展后的源码输入；它不属于 `CfirResolvePhase`。普通 resolve 的状态以 `BODY_RESOLVE` 结束，诊断由独立的 checker 管线产生。详见[编译阶段](cjfir-compiler-stages.md)。

## 聚合模块与叶子模块

`analysis/`、`cfir/`、`chir/`、`code-insight/`、`compiler/`、`llvm-interop/` 和 `tests/` 的聚合 README 是对应叶子模块的导航入口。发布模块 `:prepare:*` 由[发布工件说明](../prepare/README.md)集中描述，避免在每个工件目录重复维护同一份说明。

## 相关文档

- [模块目录](module-catalog.md)
- [编译器子系统设计](compiler-module-design.md)
- [工程架构图](project-architecture-diagram.md)
- [IntelliJ Platform 插件](../intellij-ide/README.md)
- [DevEco 集成](../deveco/README.md)
