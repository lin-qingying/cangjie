# compiler/config/ — 编译配置与环境模型

编译器配置层。承载 `CompilerConfiguration` 键值配置容器、配置键集合、Content Roots 源码输入模型，以及 headless 核心环境（application / project 两级环境与平台引导）。对齐 Kotlin K2 `compiler/config`。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.config` | `CompilerConfiguration` 配置容器与 `CompilerConfigurationKey`；通用 / CLI 配置键（`CommonConfigurationKeys` / `CLIConfigurationKeys`）；调用选项键（`CompilerInvocationConfiguration`）；Content Roots 输入模型（`ContentRoots`）；语言版本配置（`LanguageVersionSettingsConfigurator`） |
| `org.cangnova.cangjie` | 本模块中的 headless 核心环境入口（`CangJieCoreEnvironment`）与两级环境（`CangjieCoreApplicationEnvironment` / `CangjieCoreProjectEnvironment`）、headless 平台引导（`CangJieHeadlessPlatformBootstrap`）、headless PSI Symbol 服务（`CangJieHeadlessPsiSymbolServices`） |
| `org.cangnova.cangjie.extensions` | 项目级扩展点（`CompilerConfigurationExtension`、`ProjectExtensionDescriptor`） |
| `org.cangnova.cangjie.incremental.components` | 增量编译跟踪组件（`Position` / `ScopeKind` / `LookupTracker` / `EnumMatchTracker` / `ImportTracker` / `ICFileMappingTracker`） |
| `org.cangnova.cangjie.compiler.plugin` | 编译器插件扩展实例存储（`ExtensionStorage`）及对应配置键 |

## 依赖

- `:common:diagnostics`
- `:psi`
- `:dependencies:intellij-core`（compileOnly）

## 命令

```bash
./gradlew :compiler:config:assemble
./gradlew :compiler:config:test
```

## 相关文档

- `../README.md` — 父目录总览
