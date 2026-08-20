# 编译器子系统设计

本文记录当前代码和构建中的编译器边界。模块存在性与名称由 [`settings.gradle.kts`](../settings.gradle.kts) 决定；本页不定义未来模块，也不以历史完成度作为架构事实。

## 编译器驱动

`:compiler` 是驱动层聚合命名空间。它的子模块承担不同的运行期职责：

| 模块 | 边界 |
| --- | --- |
| `:compiler:config` | 编译配置、内容根和环境模型 |
| `:compiler:phaser` | 编译阶段抽象与执行框架 |
| `:compiler:arguments` | 命令行参数模型 |
| `:compiler:frontend-arguments-generator` | 参数描述生成 |
| `:compiler:frontend` | 前端流程协调 |
| `:compiler:plugin` | 编译器插件集成边界 |
| `:compiler:codegen`、`:compiler:jvm-codegen` | 可选后端集成；其输入边界是 CHIR |

`compiler` 模块不替代 PSI、CFIR 或 Analysis API 的实现。那些能力分别属于 `:psi`、`:cfir:*` 和 `:analysis:*`。

## 语法、CFIR 与语义

源码可经 PSI 或 LightTree 进入 Raw CFIR：

```text
:psi
  → :cfir:raw-cfir:psi2cfir / :cfir:raw-cfir:light-tree2cfir
  → :cfir:entrypoint
  → :cfir:resolve
```

CFIR 数据与服务分布在 `:cfir:cfir-common`、`:cfir:cfir-cones`、`:cfir:cfir-tree`、`:cfir:semantics` 和 `:cfir:providers`。`:resolution.common` 提供跨解析流程使用的类型推断和约束基础设施。

普通 `CfirResolvePhase` 依次覆盖 `RAW_CFIR`、导入、父类型、类型、状态、扩展、隐式类型和函数体解析，终点为 `BODY_RESOLVE`。宏构造和诊断不属于该枚举：宏在普通 resolve 前准备输入，`:cfir:checkers` 在所需解析信息可用后收集诊断，`:cfir:diagnostic-renderers` 负责渲染。

## 消费边界

| 消费方向 | 入口模块 | 约束 |
| --- | --- | --- |
| 分析与 IDE | `:analysis:*`、`:code-insight:*`、`:lsp` | 通过 Analysis API 与平台接口使用语义能力，不把 CFIR 实现细节暴露给调用方 |
| 序列化 | `:cfir:cfir-serialization` | `.cjo` 序列化、反序列化和跨模块符号加载 |
| 宏 | `:macro:*` | 协议和执行器独立于普通 resolve 状态 |
| 后端 | `:chir:cfir2chir`、`:chir:chir-tree`、`:compiler:*codegen`、`:llvm-interop:*` | CFIR 到 CHIR 转换与下游代码生成是独立集成边界 |
| 发布 | `:prepare:*` | 只聚合可消费工件，不承载新的编译器逻辑 |

## 验证入口

```powershell
.\gradlew.bat :compiler:frontend:build
.\gradlew.bat :cfir:resolve:test
.\gradlew.bat :cfir:checkers:test
.\gradlew.bat :analysis:analysis-api-cfir:test
```

主构建的完整校验由 `check` 聚合；文档结构由 `validateDocumentation` 校验。

## 相关文档

- [编译阶段](cjfir-compiler-stages.md)
- [工程架构图](project-architecture-diagram.md)
- [当前模块组织](module-organization.md)
- [Kotlin K2 对照](k2-module-alignment.md)
