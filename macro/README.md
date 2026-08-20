# macro/ — 宏展开模块

前端准备阶段的宏构造实现。它在普通 source provider 最终注册之前生成并记录扩展后的 Raw CFIR 输入，不属于 `CfirResolvePhase`。

对应官方 C++ 编译器的 `MacroExpansion::Execute()` 四步流程：`CollectMacros` → `EvaluateMacros` → `ProcessMacros` → `ReplaceAST`。

## 实现方案

生产实现通过**外部进程 `LSPMacroServer`** 执行宏函数。三个模块按接口和运行时实现解耦：

```
macro-common  ─┬─→ macro-process  (生产实现)
               └─→ macro-stub      (测试 / IDE 桩)
```

## 子模块

| 模块 | 职责 |
|---|---|
| `macro-common` | 宏展开接口定义、数据模型、`protocol/` FlatBuffers 编解码 |
| `macro-process` | `ProcessMacroExecutor` 抽象外部进程协议；`LspMacroServerMacroExecutor` 实现 LSPMacroServer 匿名管道通信 |
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

- `../docs/cjfir-compiler-stages.md` — 前端准备、ordinary resolve 与诊断的边界
- `../docs/module-catalog.md` — 宏模块及其 Gradle 路径
