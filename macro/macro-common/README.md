# macro/macro-common/ — 宏展开公共接口与协议

`macro` 三件套中**仅含接口与数据模型**的部分，不依赖具体执行方式（进程 / 桩）。所有宏调用方与执行方共享本模块的协议契约。

## 关键包

| 包 | 职责 |
|---|---|
| `cangjie.macro` | `MacroCollector` / `MacroExecutor` / `MacroReplacer` / `MacroExpander` 接口，与宏调用 / 展开结果数据模型 |
| `cangjie.macro.protocol` | FlatBuffers 编解码与帧格式（length-prefixed：8 字节 uint64_le + payload） |

## 设计要点

- **接口优先**：本模块只定义契约，调用方通过接口注入 `MacroExecutor` 实现
- **进程无关**：协议与 FlatBuffers 编解码足以支持任意 `MacroExecutor` 后端
- **可观测**：宏展开产生的 token 记录到 `tokensEvalInMacro` 供调试

## 依赖

- `:util`、`:common`
- `:flatbuffers-gen`

## 实现后端

- [`../macro-process/`](../macro-process/README.md) — 生产实现，外部进程 LSPMacroServer
- [`../macro-stub/`](../macro-stub/README.md) — 测试 / IDE 桩

## 命令

```bash
./gradlew :macro:macro-common:assemble
./gradlew :macro:macro-common:test
```

## 相关文档

- `../README.md` — Macro 总览
- `../../cjfir-compiler-stages.md` 第 5 阶段
