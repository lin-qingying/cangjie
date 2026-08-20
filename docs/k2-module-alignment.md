# Cangjie 与 Kotlin K2 的模块对照

本项目借鉴 Kotlin K2 的分层和接口边界，但不是 Kotlin 编译器的逐文件移植。下表仅说明当前一方模块与 Kotlin K2 子系统之间的设计参照；仓颉语言语义以官方仓颉编译器和语言资料为准。

## 对照原则

- 对照以职责为单位，不把 Kotlin 的平台模块、K1 兼容层或 Java 互操作模块当作本仓库必须拥有的模块。
- 模块集合由 [`settings.gradle.kts`](../settings.gradle.kts) 决定；完整列表见[模块目录](module-catalog.md)。
- 同名或相似的子系统不代表 API、类型模型或语言语义可以直接互换。

## 当前子系统对照

| Cangjie 模块 | Kotlin K2 参照 | 当前关系 |
| --- | --- | --- |
| `:common`、`:util` | `core/compiler.common`、`compiler/util` | 共享名称、语言模型与通用工具的基础层 |
| `:psi` | `compiler/psi` | 词法、语法和 PSI 源码表示 |
| `:cfir:cfir-cones`、`:cfir:cfir-tree` | `compiler/fir/cones`、`compiler/fir/tree` | CFIR 类型和节点模型 |
| `:cfir:cfir-tree:tree-generator` | `compiler/fir/tree/tree-generator` | 生成式 IR 树定义 |
| `:cfir:raw-cfir:*` | `compiler/fir/raw-fir/*` | 从 PSI 或 LightTree 构建 Raw CFIR |
| `:cfir:providers`、`:cfir:semantics`、`:cfir:resolve` | `compiler/fir/providers`、`compiler/fir/semantics`、`compiler/fir/resolve` | 符号提供、语义工具和惰性分阶段解析 |
| `:cfir:checkers`、`:cfir:diagnostic-renderers`、`:common:diagnostics` | `compiler/fir/checkers`、`compiler/fir/diagnostic-renderers`、FIR 诊断基础设施 | 独立诊断收集与渲染边界 |
| `:cfir:entrypoint` | `compiler/fir/entrypoint` | Session 和前端流程装配 |
| `:analysis:*` | `analysis/*` | Analysis API、平台接口、CFIR 后端、低层 API、stubs 与 light declarations |
| `:tests:test-infrastructure` | `compiler/test-infrastructure` | 文件驱动测试、指令、服务和 fixture 支撑 |
| `:chir:*`、`:compiler:*codegen` | `compiler/fir/fir2ir`、`compiler/ir/*`、后端模块 | 后端 IR 转换和代码生成的独立集成边界 |

## 仓颉特有边界

宏协议和执行器位于 `:macro:*`，FlatBuffers 协议生成位于 `:flatbuffers-gen`，`.cjo` 集成位于 `:cfir:cfir-serialization`。这些模块不能由 Kotlin 的元数据、编译器插件或后端模块直接替代。

同样，Kotlin 的平台专属后端、K1 兼容实现和 Java 专属模块并不构成此仓库的模块清单。是否新增或拆分模块应通过设计与 `settings.gradle.kts` 变更明确表达，而不是从本对照表推断。

## 使用方式

当实现 CFIR、Analysis API 或测试基础设施时，可将 Kotlin K2 用作框架设计参考；当判断仓颉程序的语义、诊断或示例时，应以官方仓颉资料和 `cjc` 验证为准。

## 相关文档

- [当前模块组织](module-organization.md)
- [编译器子系统设计](compiler-module-design.md)
- [编译阶段](cjfir-compiler-stages.md)
- [官方仓颉编译器](https://gitcode.com/Cangjie/cangjie_compiler)
