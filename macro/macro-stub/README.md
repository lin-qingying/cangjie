# macro/macro-stub/ — 宏执行桩

`MacroExecutor` 的**测试 / IDE 桩实现**：不启动子进程，按预设规则直接返回展开结果。

## 关键包

`org.cangnova.cangjie.macro.stub` — `StubMacroExecutor` 主体。

## 使用场景

- **单元测试**：避免依赖真实 `LSPMacroServer` 子进程
- **IDE 早期阶段**：在尚未对接进程实现前提供占位行为
- **CI**：在没有仓颉 native 工具链的环境下跑测试

## 依赖

- `:macro:macro-common`
- `:util`、`:common`

## 命令

```bash
./gradlew :macro:macro-stub:assemble
./gradlew :macro:macro-stub:test
```

## 相关文档

- `../README.md` — Macro 总览
- `../macro-common/README.md` — 协议契约
- `../macro-process/README.md` — 生产实现
