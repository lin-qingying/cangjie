# 宿主侧待提交改动（2026-10-11）

本会话对 `intellij-ide` 工作树（`D:/code/intellij/cangjie/intellij-ide/.claude/worktrees/musing-bun-5ae793`，分支 `claude/musing-bun-5ae793`）的改动**已改好、已实跑，但未提交**——本会话的 git 被工作树守卫限制在自己的工作树内（`git -C` 实测被拒），提交需在宿主会话执行。

建议拆成两个提交：**生产码**与**测试/夹具**。

## 提交 1：宿主生产码（符号索引，任务 6.6②）

```bash
cd "D:/code/intellij/cangjie/intellij-ide/.claude/worktrees/musing-bun-5ae793" && git add modules/ide/base/src/main/kotlin/org/cangnova/cangjie/ide/base/index modules/ide/base/src/main/resources/META-INF/cangjie-decompiled.xml && git commit -F- <<'EOF'
feat(index): 宿主符号索引（任务 6.6②）——键 / 写侧 / 读侧与注册

按上游 `design-6.6-persistent-symbol-index.md` 落地 IDE 侧持久符号索引：

- `CangJieStubIndexKeys`：四个与 `CaSymbolIndexEntryKind` 一一对应的键（只收顶层，键值只有短名）+ `VERSION`
  （键语义变更必须升版本 → 触发平台索引重建）；
- `CangJieStubIndexExtensions`：四个 `StringStubIndexExtension`（平台持久索引，随 IDE 索引增量更新）；
- `CangJieStubIndexService`：接前端已预留的 `StubIndexService` 空钩子（每个元素类型的 `indexStub` 都已在调用它），
  只写顶层声明；枚举构造器按「所属枚举顶层」判定（其父桩是枚举体，需向上找类型声明桩）；
- `CangJieSymbolIndexQueryService`：`CaSymbolIndexQueryService` 实现（`PERSISTENT`），复用契约的
  `CaSymbolIndexNameFilter.matches` 口径、dumb 期间返回空且**不做任何扫描兜底**、
  枚举构造器条目填 `ownerClassId`（分析层靠它定位所属枚举），并记录 `queryCount` / `scannedKeyCount` / `queryNanos`；
- `cangjie-decompiled.xml`：`stubIndex` ×4 + `applicationService`（`StubIndexService`）+ `projectService`（`CaSymbolIndexQueryService`）。

验证：`product/idea-plugin` 的 `CangJieSymbolIndexQueryTest` 8 例全绿（见提交 2）。
EOF
```

## 提交 2：宿主测试与夹具

```bash
cd "D:/code/intellij/cangjie/intellij-ide/.claude/worktrees/musing-bun-5ae793" && git add product/idea-plugin/src/test/kotlin/org/cangnova/cangjie/ide/index product/idea-plugin/src/test/kotlin/org/cangnova/cangjie/ide/completion modules/ide/base/src/test/kotlin/org/cangnova/cangjie/platform modules/test-support/src/testFixtures/org/cangnova/cangjie/test/CangJieStdlibLightProjectDescriptor.kt docs/host-upstream-responsibility-matrix.md docs/architecture-host-plugin.md && git commit -F- <<'EOF'
test(index, completion): 宿主符号索引与补全的真实入口验收

- `CangJieSymbolIndexQueryTest`（8 例）：四类顶层声明按前缀召回且种类正确、嵌套与 extend 声明**不入索引**、
  枚举构造器条目带 `ownerClassId`、前缀口径（≤3 字符 startsWith / 否则 contains，**区分大小写**）、
  scope 限定（空 scope 不召回）、增量可见（新增文件里的声明立刻可召回）、
  扫描量上界（只随**索引键集合**、不随文件数——「禁止全工作区扫描」的机械判据）、查询计数与耗时；
  用例必须在产品级夹具跑（服务注册的描述符链由产品描述符 include，base 模块自己的测试夹具没有这层装配）。
- `CangJieCompletionRegistrationTest`：真实触发 BASIC/SMART、接受候选写入文档、增量编辑后再补全、
  同行 after 语义、唯一候选自动插入、contributor 唯一注册、LSP 未启动下原生链路照常；
  另留两条**记录式探针**（撤销与库侧候选，均以对照实验证明为夹具限制，真机复验归上游 14.5）。
- `PlatformMatrixDeclarationTest`：声明与受测平台一致性守卫（`sinceBuild == platformVersion`、
  `platform/<版本>/` 存在、`untilBuild` 落在已测基线内、跑测试的平台就是声明基线）。
- `docs/host-upstream-responsibility-matrix.md` / `docs/architecture-host-plugin.md`：责任域补两行（补全管线与宿主符号索引的归属）、架构文档新增「Default completion path and kill switch」一节（默认路径、独立停用路径、默认值翻转被 14.1–14.5 门禁）。
- `CangJieStdlibLightProjectDescriptor`：夹具补 `std/std.collection.cjo`——`std.ast` 大量引用
  `std.collection.ArrayList`，缺它时反编译文本在类型位置渲染出 `<error>`，AST 与 compiled stub 树因此对不上
  （平台 `FileTrees.reconcilePsi` 抛「PSI and index do not match」）；该修复同时让
  `CangJieCompiledFilesHighlightingTest` 里同一条断言的**既有失败**转绿。

- `CangJieCompletionUndoOnFileTest`：把撤销的**夹具事实**钉死——物理文件 + 真编辑器下，
  对照组（普通命令编辑）与补全插入经 `ACTION_UNDO` / `UndoManager.undo(FileEditor)` 四变体**全不回退**，
  故「撤销不回退」不是补全缺陷证据（真机验收归 14.5）；用例断言的是「撤销触发后文本仍停在编辑后」，
  将来平台/夹具修好撤销时这两条会失败，届时翻成真验收。
已知既有失败（与本轮改动无关，撤回改动同样失败）：`CangJieCompiledFilesHighlightingTest` 的
`testStdlibAstCjoPlaceholderDocumentReloadsAfterToolchainRegistration` 与 `testLargeStdlibCjoCanOpenInEditor`。
EOF
```

## 提交 3：宿主构建配置（任务 14.5 的 Plugin Verifier 落点）

```bash
cd "D:/code/intellij/cangjie/intellij-ide/.claude/worktrees/musing-bun-5ae793" && git add build-logic/src/main/kotlin/cangjie.intellij-product.gradle.kts && git commit -F- <<'EOF'
build(verifier): 配置 JetBrains Plugin Verifier（任务 14.5 的可机械化验收）

在 `cangjie.intellij-product.gradle.kts` 里声明被测平台：
`pluginVerification { ides { ide(ideToRunType, ideRunVersion) } }`——与声明矩阵同源（IU / 253.29346.50，
即 sinceBuild=253 对应的那一个），未声明兼容的平台不进 verifier。

实测：`./gradlew :product:idea-plugin:verifyPlugin` 对 `intellij-cangjie-2.1.4.zip` 出报告
`product/idea-plugin/build/reports/pluginVerifier/IU-253.29346.50/report.html`，
verifier 1.410 判定 **Compatible**（14 项 scheduled-for-removal / 168 项 deprecated / 265 项 experimental /
104 项 internal / 2 项 non-extendable / 3 条 plugin structure warnings）。

三条结构告警是「描述符声明 + 依赖插件集里同样有」被判为重复依赖（`org.toml.lang`、`com.redhat.devtools.lsp4ij`、
`com.intellij.modules.json`），**不能照字面删**：lsp4ij 是运行时必需且不随 IDE 分发的外部插件。
处置记在上游 `verification-14.5-plugin-verifier.md`，本轮不改描述符。
EOF
```

## 复核命令（可选）

```bash
cd "D:/code/intellij/cangjie/.claude/worktrees/musing-bun-5ae793" && ./gradlew.bat -p . :product:idea-plugin:test --tests "*CangJieSymbolIndexQueryTest*" --tests "*CangJieCompletionRegistrationTest*" -Pcangjie.upstream.root=D:/code/intellij/cangjie/.claude/worktrees/completion-k2-musing-bun
```
