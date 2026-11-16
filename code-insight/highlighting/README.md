# code-insight/highlighting/ — 高亮

词法着色、结构规则、语义着色入口、属性 key 与 CDoc → 文档渲染。属性 key 同时是 ide/lsp（semantic tokens）与 ide/macro 的消费面，因此本模块对外暴露的是「key + 收集器」，而不是编辑器动作。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.codeinsight.highlighting` | `SyntaxHighlighter`、结构高亮规则、属性 key / 颜色定义、语义高亮名映射 |
| `org.cangnova.cangjie.documentation` | 文档模型（CDoc → `DocumentationModel`）与 HTML / Markdown 渲染、Quick Doc 解析服务 |
| `org.cangnova.cangjie.lexer.cdoc.render` | CDoc → HTML / Markdown 渲染器 |

## 依赖

- `:common`
- `:psi`
- `:util`
- `:analysis:analysis-api`
- `:analysis:cj-references`

不依赖 `:code-insight:api`；语义着色入口消费 Analysis API 契约。

## 命令

```bash
./gradlew :code-insight:highlighting:assemble
./gradlew :code-insight:highlighting:test
```

## 相关文档

- `../README.md` — Code Insight 总览
- `../../docs/module-catalog.md` — 模块清单
