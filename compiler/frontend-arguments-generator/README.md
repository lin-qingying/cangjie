# compiler/frontend-arguments-generator/ — 前端参数类生成器

代码生成器：以 `:compiler:arguments` 的参数 DSL 为输入，生成前端运行时使用的参数类（当前为 `CommonToolArguments` / `CommonCompilerArguments`，包 `org.cangnova.cangjie.frontend.arguments`）。对齐 Kotlin K2 `compiler/cli/cli-arguments-generator`。

`:compiler:frontend` 构建通过 `generateFrontendArguments` 任务（`generated-sources` 约定）调用本模块，生成源码写入 `:compiler:frontend` 的 `gen/` 目录，勿手工修改生成文件。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.frontend.arguments.generator` | 生成入口（`Main`）与生成逻辑（`generateArgumentsClass`、层级到类的映射表 `ArgumentsInfo`） |

## 依赖

- `:compiler:arguments`
- `:generators`（compileOnly）

## 命令

```bash
./gradlew :compiler:frontend-arguments-generator:assemble
./gradlew :compiler:frontend-arguments-generator:test
```

## 相关文档

- `../README.md` — 父目录总览
- `../arguments/README.md` — 本模块消费的参数 DSL 定义
