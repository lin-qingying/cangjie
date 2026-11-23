# compiler/plugin/ — 编译器插件加载占位

管线阶段 1（`LOAD_PLUGINS`）的占位模块，是编译器插件加载的集成边界。当前为零源码模块（仅 `build.gradle.kts`，纯 `java` 插件）；插件扩展实例存储机制位于 `:compiler:config` 的 `org.cangnova.cangjie.compiler.plugin`（`ExtensionStorage`）。

## 依赖

无项目依赖（仅 JUnit 测试依赖）。

## 命令

```bash
./gradlew :compiler:plugin:assemble
./gradlew :compiler:plugin:test
```

## 相关文档

- `../README.md` — 父目录总览
