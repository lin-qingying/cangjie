# macro/ — 宏展开模块

前端管线阶段 5 `MACRO_EXPAND` 的实现。

对应官方 C++ 编译器的 `MacroExpansion::Execute()` 四步流程：`CollectMacros` → `EvaluateMacros` → `ProcessMacros` → `ReplaceAST`。

## 实现方案

本项目基于 JVM，无法 `dlopen` 仓颉 native 宏动态库，因此通过**外部进程 `LSPMacroServer`** 执行宏函数。三件套模块解耦：

```
macro-common  ─┬─→ macro-process  (生产实现)
               └─→ macro-stub      (测试 / IDE 桩)
```

## 子模块

| 模块 | 职责 |
|---|---|
| `macro-common` | 宏展开接口定义、数据模型、`protocol/` FlatBuffers 编解码 |
| `macro-process` | `ProcessMacroExecutor`：外部进程 LSPMacroServer 实现，匿名管道通信 |
| `macro-stub` | `StubMacroExecutor`：测试与 IDE 桩，不启动子进程 |

## 核心接口链

`MacroCollector`（收集宏调用）→ `MacroExecutor`（执行宏展开）→ `MacroReplacer`（PSI/AST 替换）→ `MacroExpander`（编排器）。

## 通信协议

- 序列化：FlatBuffers（schema 见 `:flatbuffers-gen`）
- 帧格式：length-prefixed（8 字节 `uint64_le` 长度前缀 + payload）
- 传输：匿名管道（与 LSPMacroServer 子进程通信）

## 关键包

- `org.cangnova.cangjie.macro` — 接口与数据模型
- `org.cangnova.cangjie.macro.protocol` — FlatBuffers 编解码

## 测试

```bash
./gradlew :macro:macro-common:test
./gradlew :macro:macro-process:test
./gradlew :macro:macro-stub:test
```

## 相关文档

- `../cjfir-compiler-stages.md` 第 5 阶段 — 宏展开设计
- `../intellij-ide/docs/macro-psi-replacement-design.md` — IDE 侧宏 PSI 替换设计
