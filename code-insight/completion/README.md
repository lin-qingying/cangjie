# code-insight/completion/ — 补全

仓颉原生补全：契约实现、补全位置 / 插入 / 导入机制、Analysis 支撑的候选流水线。三者是一个功能的三个阶段，用包分层，不再拆成多个 Gradle 模块。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.ide.completion.impl.cfir` | Analysis（CFIR）支撑的补全服务与 contributor（子包 `sections` 候选区段、`filter` 期望类型过滤、`recovery` 候选复查恢复、`resolve` 声明解析） |
| `org.cangnova.cangjie.ide.completion.impl.cfir.sections` | 候选区段：顶层、局部、成员、关键字、枚举条目、命名实参、CDoc、声明建议，以及快照工厂 |
| `org.cangnova.cangjie.ide.completion.impl.shared` | 宿主无关的共享机制：contributor 基类、（并行）runner、位置上下文、dummy 标识、`LookupElement` 工厂与图标、OTel 统计 sink |
| `org.cangnova.cangjie.ide.completion.impl.shared.insert` | 插入处理器、选择上下文与插入前校验（文档版本 / 区间 / 候选身份） |
| `org.cangnova.cangjie.ide.completion.impl.shared.imports` | `ImportPlanApplier`：导入计划的写入应用器，必须在写命令内调用；供 `:code-insight:code-actions` 的 import quick fix 复用 |

## 依赖

- `:code-insight:api`
- `:analysis:analysis-api`
- `:psi`
- `:common`

## 命令

```bash
./gradlew :code-insight:completion:assemble
./gradlew :code-insight:completion:test
```

## 相关文档

- `../README.md` — Code Insight 总览
- `../../docs/module-catalog.md` — 模块清单
