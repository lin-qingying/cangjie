# code-insight/editor/ — 折叠与格式化

「编辑力学」：折叠区间收集与格式化模型。输入只有 token、CST 与代码风格设置，没有跨声明的语义，不依赖 Analysis；调用方（IDE / LSP）复用同一份实现。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.codeinsight.folding` | 宿主无关的折叠区间收集器 `CangJieFoldingRangeCollector` 与折叠种类 / 区域 |
| `org.cangnova.cangjie.formatter` | 格式化模型构建、spacing 规则、代码风格设置与包条目（子包 `indent` 缩进规则、`util` 尾随逗号工具） |

## 依赖

- `:common`
- `:psi`
- `:util`

不依赖 `:code-insight:api` 与 Analysis 族：折叠与格式化不需要语义解析。

## 命令

```bash
./gradlew :code-insight:editor:assemble
./gradlew :code-insight:editor:test
```

## 相关文档

- `../README.md` — Code Insight 总览
- `../../docs/module-catalog.md` — 模块清单
