# compiler/arguments/ — 命令行参数模型与 DSL

仓颉编译器命令行参数的统一表示：参数 DSL（`compilerArguments { }` 层级树、值类型、release 生命周期）与参数定义描述（`cangjieCompilerArguments`）。对齐 Kotlin K2 `compiler/arguments`。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.arguments.description` | 参数定义描述：`cangjieCompilerArguments` DSL 参数树、层级名称（`CompilerArgumentsLevelNames`） |
| `org.cangnova.cangjie.arguments.dsl` | 参数 DSL 基础模型：`CangJieCompilerArguments` 根模型、`CangJieCompilerArgument` 与层级模型（`dsl.base`）、参数值类型（`dsl.types`） |

## 依赖

- `:generators`（compileOnly）

## 命令

```bash
./gradlew :compiler:arguments:assemble
./gradlew :compiler:arguments:test
```

## 相关文档

- `../README.md` — 父目录总览
- `../frontend-arguments-generator/README.md` — 消费本模块 DSL 的参数类生成器
