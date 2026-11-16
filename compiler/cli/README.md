# compiler/cli/ — 编译器 CLI

仓颉编译器命令行层：`CangJieCLICompiler` 公共 CLI 执行层（共享 schema 解析文本参数 → 解析前统一上报诊断 → 调用编译管线）与 `CangJieFrontendCLICompiler`（把解析后的参数接入 `:compiler:frontend` 前端管线）。对齐 Kotlin K2 `compiler/cli`。

当前 `CangJieCLICompiler` 仍为 abstract，本模块没有可执行 main，尚不能作为独立编译器命令运行。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.cli` | `CangJieFrontendCLICompiler` 前端 CLI 基类（自由参数装配为源码输入后调用前端管线） |
| `org.cangnova.cangjie.cli.common` | `CangJieCLICompiler` CLI 公共执行层、`CangJieCLIExecutionResult` 执行结果模型 |

## 依赖

- `:compiler:cli:cli-base`（api）
- `:compiler:frontend`（api）
- `:compiler:config`
- `:common:diagnostics`
- `:dependencies:intellij-core`（runtimeOnly）

依赖方向：`cli` 依赖 `cli-base`；`cli-base` 只负责参数解析、不依赖前端管线。

## 命令

```bash
./gradlew :compiler:cli:assemble
./gradlew :compiler:cli:test
```

## 相关文档

- `../README.md` — 父目录总览
- `cli-base/README.md` — 参数解析基础层
