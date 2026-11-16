# compiler/phaser/ — 编译器阶段管理框架

编译器阶段（phaser）框架：`CompilerPhase` / `NamedCompilerPhase` 阶段接口、`PhaseSet` 阶段集合、`PhaserState` 运行状态、`PhaseConfig` 阶段配置与阶段耗时剖析（`PhaserProfiler`）。对齐 Kotlin K2 phaser 层（`CompilerPhase` / `PhaseSet` / `PhaserState`）。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.phaser` | `CompilerPhase` / `NamedCompilerPhase` 阶段接口、`PhaseSet` 阶段集合、`PhaserState` 运行状态、`PhaseConfig` 阶段配置、`Action` 前后置动作、`LoggingContext` 日志上下文、`PhaserProfiler` 阶段剖析 |

## 依赖

- `:compiler:config`（api）

## 命令

```bash
./gradlew :compiler:phaser:assemble
./gradlew :compiler:phaser:test
```

## 相关文档

- `../README.md` — 父目录总览
