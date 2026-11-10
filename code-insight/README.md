# Code insight

`code-insight/` 提供 IDE 与语言服务共用的编辑器侧能力，只消费 Analysis API 契约与 IntelliJ Platform API，不拥有解析，也不拥有 CFIR 语义解析。

## 布局原则

1. **一个用户可见能力 = 一个模块**。能力内部的契约 / 机制 / 流水线用包分层，不再拆成多个 Gradle 模块。
2. **对外扩展点单独成模块**（`api`）：第三方或 IDE 侧要实现、要调用的稳定表面。
3. **不设空聚合模块**；聚合由目录与发布制品表达。
4. **制品 ≠ 模块**：制品按发布需要组合模块，禁止 1:1 复制出打包工程。

## 模块

| 模块 | 用户可见能力 | 主要内容 |
| --- | --- | --- |
| `:code-insight:api` | —（扩展契约） | quick-fix 注册表与 factory、成员生成服务、补全服务与插入/导入计划的数据类型 |
| `:code-insight:editor` | 折叠、格式化 | 折叠区间收集、格式化模型与 spacing、代码风格设置 |
| `:code-insight:highlighting` | 高亮 | 词法/结构/语义着色、属性 key、CDoc 渲染 |
| `:code-insight:completion` | 补全 | contributor、section、lookup、排序、插入、恢复、统计 |
| `:code-insight:code-actions` | 快速修复、生成成员 | quick fixes、override/implement |
| `:code-insight:hints` | 内联提示 | 类型、参数名、lambda、调用链、注解参数 |
| `:code-insight:refactoring` | 重命名与重构 | rename 处理器、headless renamer、名字校验、安全删除 |

依赖关系（结果，不是判据）：

```
api           ← editor / highlighting / completion / code-actions / hints / refactoring
completion    ← code-actions（复用 CompletionImportPlan 的 PSI 应用器）
editor / highlighting / hints / refactoring 之间无依赖
```

## 发布制品

只发布一个 IDE 制品：

| 制品 | 打包模块 |
| --- | --- |
| `cangjie-frontend-code-insight-for-ide` | 上述 7 个模块 + `:psi`、`:common`、`:util`、`:analysis:analysis-api`、`:analysis:analysis-api-cfir` |

将来若需要单独发布某个子集，只需新增一个 `publishCangjieJarsForIde(...)` 列表，不调整模块边界。

## 构建与测试

```powershell
.\gradlew-queue.bat :code-insight:api:assemble
.\gradlew-queue.bat :code-insight:editor:test
.\gradlew-queue.bat :code-insight:highlighting:test
.\gradlew-queue.bat :code-insight:completion:test
.\gradlew-queue.bat :code-insight:code-actions:test
.\gradlew-queue.bat :code-insight:hints:test
.\gradlew-queue.bat :code-insight:refactoring:assemble
```

## 相关文档

- [Analysis 子系统](../analysis/README.md)
- [IntelliJ Platform 插件](../intellij-ide/README.md)
- [模块清单](../docs/module-catalog.md)
