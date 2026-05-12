# macro/macro-process/ — 外部进程宏执行器

`MacroExecutor` 的**生产实现**：通过外部子进程 `LSPMacroServer` 执行仓颉 native 宏函数。

## 关键包

`org.cangnova.cangjie.macro.process` — `ProcessMacroExecutor` 主体、子进程生命周期管理、IO 流封装。

## 通信协议

- **传输**：匿名管道（与 `LSPMacroServer` 子进程通信）
- **帧格式**：length-prefixed（8 字节 `uint64_le` 长度前缀 + payload）
- **序列化**：FlatBuffers（schema 见 `:flatbuffers-gen`、契约见 `:macro:macro-common`）

## 为什么需要外部进程

本项目基于 JVM，无法 `dlopen` 仓颉 native 宏动态库。必须通过外部进程加载 native 库并执行宏函数。

## 依赖

- `:macro:macro-common`
- `:flatbuffers-gen`
- `:util`、`:common`

## 命令

```bash
./gradlew :macro:macro-process:assemble
./gradlew :macro:macro-process:test
```

## 相关文档

- `../README.md` — Macro 总览
- `../macro-common/README.md` — 协议契约
