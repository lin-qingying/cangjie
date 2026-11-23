# compiler/cli/cli-base/ — CLI 参数解析基础层

命令行参数解析基础层：按共享参数 schema（`cangjieCompilerArguments`）解析文本参数，产出解析后的参数对象、自由源码路径与解析诊断（`CangJieArgumentParseResult`）。对齐 Kotlin K2 `compiler/cli/cli-base`。

本模块不依赖前端管线；上层 `:compiler:cli` 依赖本模块（依赖方向不可逆）。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.cli.common.arguments` | `parseCommandLineArguments` 文本参数解析入口、`CangJieArgumentParseResult` 解析结果与 `CangJieArgumentParseIssue` 单条解析诊断 |

## 依赖

- `:compiler:arguments`（api）

## 命令

```bash
./gradlew :compiler:cli:cli-base:assemble
./gradlew :compiler:cli:cli-base:test
```

## 相关文档

- `../README.md` — `:compiler:cli` 上层 CLI 执行层
- `../../README.md` — 父目录总览
