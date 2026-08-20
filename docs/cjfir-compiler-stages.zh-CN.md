# 仓颉前端阶段

[English](cjfir-compiler-stages.md) | [架构图](project-architecture-diagram.zh-CN.md) | [模块目录](module-catalog.md)

本文描述当前前端边界，不是历史实现计划。若文档与实现不一致，应以源码、`settings.gradle.kts` 和测试套件为准。

## 前端流程

```text
源码文件
  → 解析为 PSI 或 LightTree
  → 构建 Raw CFIR
  → 存在宏时构建宏展开后的源码输入
  → 注册 source providers
  → 解析声明与函数体
  → 收集并渲染诊断
  → 提供 Analysis、序列化和后端集成点
```

解析与 Raw CFIR 构建由 `:psi`、`:cfir:raw-cfir:psi2cfir`、`:cfir:raw-cfir:light-tree2cfir` 提供；Session 与前端装配位于 `:cfir:entrypoint`；普通语义解析位于 `:cfir:resolve`；诊断由 `:cfir:checkers` 与 `:cfir:diagnostic-renderers` 提供。

## 宏边界

宏构造属于前端准备步骤，不属于普通 resolve phase。展开后的 raw 文件会在普通 source provider 最终注册之前被记录。`:compiler:frontend` 中的架构守卫测试强制 `CfirResolvePhase` 不得重新引入 `MACRO_EXPAND`。

## 普通 CFIR resolve phases

`CfirResolvePhase` 表示声明级、可惰性推进的语义状态：

```text
RAW_CFIR
  → IMPORTS
  → SUPER_TYPES
  → TYPES
  → STATUS
  → EXTENSIONS
  → IMPLICIT_TYPES
  → BODY_RESOLVE
```

- `RAW_CFIR` 是语法转换完成后的结构状态标记。
- `IMPORTS` 绑定 import 名称和包。
- `SUPER_TYPES`、`TYPES` 与 `STATUS` 建立声明头和继承契约。
- `EXTENSIONS` 解析仓颉扩展声明。
- `IMPLICIT_TYPES` 固化省略的声明级类型。
- `BODY_RESOLVE` 解析表达式、调用、重载和函数体级推断。

`:cfir:checkers` 会在所需 resolve 信息可用后运行诊断管线，刻意不作为 `CfirResolvePhase` 的枚举项。

## 产物与消费者

| 产物 | 主要消费者 |
| --- | --- |
| PSI / LightTree | Raw CFIR builder、编辑器服务、测试 |
| Resolved CFIR | Analysis API、code insight、序列化、可选后端 |
| Diagnostics | 编译器与 Analysis API 诊断消费者 |
| `.cjo` 集成 | 跨模块符号加载与反编译 |
| CHIR | JVM 与 LLVM 后端集成 |

子系统归属见[架构图](project-architecture-diagram.zh-CN.md)，所有 Gradle 模块见[模块目录](module-catalog.md)。
