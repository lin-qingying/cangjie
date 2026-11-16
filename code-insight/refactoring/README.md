# code-insight/refactoring/ — 重命名与重构

独立的能力与独立入口（rename / safe delete）。这里不属于「编辑力学」：重构的正确性由符号同一性与引用范围决定，PSI 引用搜索只是当前实现手段。IDE 与 LSP 共用同一套语言级重构核心，宿主侧只做注册。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.cangjie.codeinsight.refactoring` | 能力入口：`RefactoringSupportProvider`、名字校验器 `CangJieNamesValidator`、headless registrar |
| `org.cangnova.cangjie.codeinsight.refactoring.rename` | 各声明种类的 rename processor（类 / 函数 / 属性 / 参数 / 类型参数）与无 UI 执行器 `CangJieHeadlessRenamer` |
| `org.cangnova.cangjie.codeinsight.refactoring.safeDelete` | 元素可删除性判定 `canDeleteElement` |

## 依赖

- `:common`
- `:psi`
- `:util`

不依赖 `:code-insight:api` 与 Analysis 族：rename 当前以 PSI 引用搜索实现。

## 命令

```bash
./gradlew :code-insight:refactoring:assemble
./gradlew :code-insight:refactoring:test
```

## 相关文档

- `../README.md` — Code Insight 总览
- `../../docs/module-catalog.md` — 模块清单
