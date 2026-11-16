# code-insight/hints/ — 内联提示

被动显示类能力：收集器只读分析快照、不改代码，因此与 code action / 补全分离，拥有独立开关与独立收集入口。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.codeinsight.hints` | 宿主无关的收集入口 `CangJieInlayHintCollector` 与各类收集器：类型、参数名、lambda 返回、调用链、注解参数、区间开闭性 |

## 依赖

- `:analysis:analysis-api`
- `:common`
- `:psi`

不依赖 `:code-insight:api`。

## 命令

```bash
./gradlew :code-insight:hints:assemble
./gradlew :code-insight:hints:test
```

## 相关文档

- `../README.md` — Code Insight 总览
- `../../docs/module-catalog.md` — 模块清单
