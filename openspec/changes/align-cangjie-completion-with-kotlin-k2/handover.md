# 交接文档：`align-cangjie-completion-with-kotlin-k2`

> 记录时间：2026-10-03
> 目标：把仓颉补全对齐到 Kotlin K2 原生 IDE 补全（`openspec/changes/align-cangjie-completion-with-kotlin-k2/`）
> 当前进度：**40/82 已勾选**（2026-10-07 更新：新增 6.5、6.7、6.10、9.4、9.5、9.6、9.10；6.6 部分完成（②未做，①③④已落地）；本行以上进度描述保留为当时状态）

---

## 1. 两个工作树

| 角色 | 路径 | 分支 | 基线 |
|---|---|---|---|
| 宿主（intellij-ide 插件） | `D:\code\intellij\cangjie\intellij-ide\.claude\worktrees\musing-bun-5ae793` | `claude/musing-bun-5ae793` | — |
| 上游（PSI / CFIR / Analysis / code-insight） | `D:\code\intellij\cangjie\.claude\worktrees\completion-k2-musing-bun` | `claude/completion-k2-musing-bun` | `b10638242` |

**绝大部分实现在上游工作树**，宿主工作树只放 OpenSpec 文档与宿主接线。

### 宿主构建必须显式指向上游工作树

宿主 `settings.gradle.kts` 已把写死的 `includeBuild("../")` 换成可配置解析（顺序：Gradle 属性 → 环境变量 → `..`）：

```bash
# 在宿主工作树里跑任何宿主任务时
./gradlew ... -Pcangjie.upstream.root=D:/code/intellij/cangjie/.claude/worktrees/completion-k2-musing-bun
# 或
export CANGJIE_UPSTREAM_ROOT=D:/code/intellij/cangjie/.claude/worktrees/completion-k2-musing-bun
```

不加这个参数会解析到 `.claude/worktrees/` 目录，报「Cangjie upstream settings.gradle.kts not found」。

### 工作树改动规模

上游工作树 `git status --porcelain` 共 **828** 条，其中：

- **755 条是生成产物**（`analysis/analysis-api-cfir/tests-gen/**`、`analysis/analysis-api-cfir/gen/**`），由生成器批量改写，**不要手改**；
- **73 条是手写源码/测试数据**，是需要人读的部分。

宿主工作树改动：`settings.gradle.kts`、`gradle/libs.versions.toml`、`modules/ide/base/build.gradle.kts`、`modules/test-support/build.gradle.kts`、`product/idea-plugin/build.gradle.kts`、`modules/ide/base/src/main/resources/org.cangnova.cangjie.ide.base.xml`、`docs/architecture-host-plugin.md`，以及未跟踪的 `product/idea-plugin/src/test/kotlin/org/cangnova/cangjie/ide/completion/`。

---

## 2. 已完成并有执行证据的 23 项

| 组 | 任务 | 落点 |
|---|---|---|
| 1 | 1.1–1.5 | `consumer-matrix.md`、`source-baseline.json`、宿主 `settings.gradle.kts` 上游路径配置 |
| 2 | 2.1–2.3 | `analysis-api/.../projectStructure/danglingFiles.kt`、`analyze.kt`（`analyzeCopy`） |
| 3 | 3.1–3.3、3.5 | `CaScopeContext.kt`、`CaScopeProvider.kt`、`CaCfirScopeContext.kt`、`CaCfirCompositeScope.kt` |
| 4 | 4.3、4.4 | `CfirChainedSubstitutor.kt`、`CaCfirSubstitutedSignatureModel.kt`、`CaCfirSignatureModel.kt` |
| 5 | 5.1–5.3 | `CaCompletionExtensionCandidateChecker.kt`、`CaCfirCompletionExtensionCandidateChecker.kt`、`CaCfirVisibilityChecker.kt` |
| 6 | 6.1–6.4 | `CaCfirExpressionTypeProvider.kt` + `testData/components/expressionTypeProvider/expectedExpressionType/**` |
| 9 | 9.3 | `CfirTopLevelCompletionSection.kt` |

对应的执行记录在 `implementation-log.md`（394 行，按主题分节，含每节的命令与结果）。

### 顺带修掉的三个框架缺陷（均有回归覆盖）

1. `CfirClassDeclaredMemberScope` / `CfirExtendMemberScope` / `CfirClassSubstitutionScope` / `CfirClassUseSiteMemberScope` 缺 `processVariablesByName` 覆写 → 字段/属性不出现在成员作用域；
2. `CaCfirFieldSymbol.createPointer()` 抛 `error(...)` → 新增 `CaCfirMemberFieldSymbolPointer`；
3. extend 适用性检查经 PSI 反查推进阶段 → 改为直接对 CFIR 元素 `lazyResolveToPhase(TYPES)`。

---

## 3. 未完成清单（59 项）

```
2.4  2.5                    副本模式的测试准备逻辑、嵌套/取消/并行隔离
3.6                          局部/参数/类型参数/嵌套块/同名遮蔽/match 绑定的公开层测试
4.1  4.2  4.5                类型作用域保留接收者、从 compiler 抽内部复用入口、签名测试矩阵
5.4  5.5  5.6                短名可达性关联、导入/缩短计划补全、正反测试
6.5  6.6  6.7                按名称过滤与范围查询契约、IDE/standalone 适配、发现能力测试
7.1  7.2  7.3  7.4           组件工厂接线、**跑真实生成器**、共享基座测试、窄集回归记录
8.1  8.2  8.3  8.4  8.6  8.7 模块目录与文档、跨模块服务接口、参数包装、dummy 与位置修正、
                            可取消串行 runner 与分批 sink、生产资源注册
9.1  9.4  9.5  9.6          局部/参数/类型参数候选、命名实参、关键字与声明名/类型建议、enum
10.1 … 10.8                  lookup 数据与工厂、pointer 恢复与版本校验、权重与 sorter、
                            SMART 过滤、导入策略、插入处理、组织名/alias/分组 import、测试
11.1 … 11.6                  构造候选、override-implement 复用、super/声明建议、CDoc、
                            宏 Tokens 分流、未解析接收者名称恢复
12.1 … 12.7                  completion fat jar 与 module 制品、宿主依赖与 substitution、
                            生产 XML 接线、产品 contributor 测试、类/XML 唯一性、平台矩阵
13.1 … 13.4                  dumb/无 SDK/取消/失败分流、耗时与取消统计、并行执行器与准入
14.1 … 14.6                  测试 DSL 迁移、host light fixture、LSP 未启动验收、窄集+跨层回归、
                              真实 sandbox 验收、默认开关与文档
```

另：**9.2 已修复并通过验证，但复选框仍是未勾选**（见 §4）。

---

## 4. 本次会话新增、尚未回写勾选框的工作

### 4.1 任务 3.4 —— static/instance 成员作用域契约（**已验证可编译**）

新增公开契约：

- `analysis/analysis-api/.../components/CaMemberScopeQuery.kt`（新文件）
  - `CaTypeAccessKind`：`INSTANCE_THROUGH_RECEIVER` / `STATIC_THROUGH_TYPE_NAME` / `THIS_OR_SUPER` / `EXTEND_MEMBER`
  - `CaMemberScopeQuery`：`scope` / `kind` / `accessedThroughTypeName` / `requiresReceiver`
  - `CaMemberScopeQueryData`：带 `token: CaLifetimeToken`（查询对象持有会话绑定的 `CaScope`，必须携带同一令牌）
- `CaScopeProvider` 新增三个成员：`CaType.instanceMemberScope`、`CaType.staticMemberScope`、`CaType.memberScopeQueries(accessKind)`
- `CaCfirScopeProvider` 实现：静态通道用 `CfirClassStaticScope(base).withoutInstanceMemberDiagnostics()`，`hasDefinitelyNoStaticMembers` 时返回 `null`；`CaType.scope` 改为复用新抽出的 `useSiteMemberScopeOf`

**注意**：`CaScopeProvider.kt` 底部有「自动生成的 context 桥接，请勿手工修改」。新增成员对应的桥接**尚未由生成器产出**。目前编译通过是因为 `analyze { }` 的上下文接收者直接暴露接口成员，但 **7.2 要求跑真实生成器**——那一步还没做。

### 4.2 任务 9.2 —— extend 成员归类（**已修复并通过验证**）

**根因**（此前一直没定位到的第二处原因）：use-site 成员作用域**本身已经注入了 extend 成员**。原实现先枚举成员通道 → extend 成员被标成 `MEMBER` → 之后 extend 通道的同名候选被 `distinctByIdentity` 去重丢掉 → 表现为「extend 成员出现了但类别标错」，`memberCandidatesIncludeExtend` 断言 `EXTEND_MEMBER` 失败。

**修法**（`CfirMemberCompletionSection.kt`）：先收集「适用且可见」的 extend 成员建立 `去重键 → 符号` 映射，再枚举成员通道；命中该映射的成员标记为 `EXTEND_MEMBER`，最后补齐成员通道未覆盖的 extend 成员。为此在 `CaCfirSnapshotFactory.kt` 抽出 `CaSession.callableIdentityKey(symbol)`，与 `callableSnapshot` 内部写入身份的键**必须同源**。

另外把使用点形态判定从语法猜测改为 Analysis 判定：仓颉 PSI 里 `Type.member` 与 `receiver.member` 都是 `receiver.selector`，语法上无法区分，靠首字母大写启发式会在小写命名的类型或大写命名的变量上给出错误候选集合。现在用 `CjReferenceExpression.resolveToSymbols()`——结果恰好是唯一 `CaClassLikeSymbol` 且没有任何 `CaVariableSymbol` 绑定时才判为类型限定。

**验证**：

```
./gradlew :code-insight:completion:impl-cfir:test --tests "*CfirMemberCompletionSectionTest"
→ TEST-...CfirMemberCompletionSectionTest.xml: tests=4 failures=0 errors=0
   ok memberCandidates / memberCandidatesRespectPrefix
   ok memberCandidatesIncludeExtend   ← 本次由红转绿
   ok memberCandidatesAbsentAtExpression
```

### 4.3 第 10 组契约骨架（**只有契约，无实现、无测试**）

`code-insight/completion/contracts/.../api/lookup/` 下新增 4 个文件：

| 文件 | 内容 |
|---|---|
| `CaCompletionIdentity.kt` | `CaCompletionIdentityKind`（SYMBOL/TYPE/PACKAGE/KEYWORD/NAMED_ARGUMENT/ENUM_ENTRY/DOCUMENTATION/DECLARATION_SUGGESTION）、`CaCompletionIdentity`（handle / name / qualifiedName / **deduplicationKey**） |
| `CaCompletionSortData.kt` | `CaCompletionOrigin`（11 种来源）、`CaExpectedTypeMatch`（MATCH/UNKNOWN/MISMATCH，**未知与不匹配必须分开**）、`CaCompletionSortData` + `CaImportCost` 常量 + `toWeight()` 折算 |
| `CaCompletionImportPlan.kt` | `CaCompletionImportTarget`（**import 里写什么**与**表达式里写什么**分开）、`CaCompletionImportPlan`（NoImport / AddImport / QualifyThenShorten）、`CaImportConflictKind`、`CaImportResolution` |
| `CaCompletionInsertion.kt` | `CaCaretPolicy`、`CaInsertionTrigger`（ENTER/TAB/TYPING）、`CaCompletionInsertion`（含 `documentStamp`） |

`contracts/.../api/CaCompletionFiltering.kt`（新文件）：`CaCompletionExpectedTypeKind`、`CaCompletionExpectedType`（`None` 是**显式结论**而非「算不出来」）、`CaCompletionFilter`（契约里写死三条：BASIC 不做期望类型过滤 / SMART 期望类型未知时必须返回与 BASIC 相同集合 / **不得依据 `invocationCount` 放宽可见性**）、`CaCompletionExpectedTypeProvider`。

`CaCompletionSnapshot` 扩展为携带 `typeText` / `identity` / `sort` / `importPlan` / `insertion` / `presentableText`，`insertParenthesis` 与 `namedArgumentPrefix` 改为从 `insertion` 派生。

`impl-shared/.../CaCompletionSnapshotData.kt`（新文件）：`CaCompletionSnapshotData` + `CaCompletionSnapshotBuilder` + `caCompletionSnapshot(...)` + `caNamedArgumentSnapshot(...)`（仓颉命名实参前缀固定 `"$name: "`，**不接受调用方传分隔符**，从签名上杜绝写成 Kotlin 的 `name = `）。旧的 `impl-cfir` 内部类 `CaCompletionSnapshotImpl` 已删除。

四个区段（keyword / localScope / topLevel / member）已迁移到新工厂；`CaCfirSnapshotFactory.kt` 统一渲染签名尾文本（形参带 `!` 表示命名实参、`vararg` 前缀、`= ...` 表示默认值；枚举构造器渲染 payload）。

**⚠️ 这一批没有任何测试。** 10.1–10.7 的正反验收场景全部待补。

---

## 5. 已确认但未修的框架缺陷（属 LL 层，勿在插件侧绕过）

1. ~~**同名重载被判为同一符号**~~ —— **已修**（提交 `f6b306951`，2026-10-04）。`CaCfirCallableSymbolCacheKey` / `CaCfirExtendMemberCallableSymbolCacheKey` 增带 `CfirCallableSignature`，恢复端按签名筛选，形状对齐 `KaFirCallableIdPlusSignature`。对照实验证明新用例能抓住该缺陷（签名置空即精确变红），而既有 `symbolEquivalence` 在缺陷存在时照样通过——这解释了它此前为何没被拦住。详见 `implementation-log.md`「缺陷 D-2」节。
2. **dangling 副本的 PREFER_SELF 非局部解析返回 `null`** —— 必须修在 LL 的副本失效链路上（`CfirCacheWithInvalidation.getNotNullValueForNotNullContext` 抛 Failed requirement）。试过两条绕法（改 `AbstractDanglingFileCollectDiagnosticsTest` 的 `originalFile` 条件、在 Analysis 层绕过模块缓存），都会让 21 个生成用例失败，**已全部回退**。
3. ~~**块内局部声明未进入解析塔快照**~~ —— **已修**（提交 `3b1f2fdcf`，2026-10-05）。根因**不是**本条原先记录的 `addLocalVariable` 空 `localScopes` 早退（那处确实丢，但丢的是函数参数、随后被 `preloadValueParameters` 补回），而是 `ContextCollector` 把绑定变量入作用域放在 `visitPatternVariable` 的 `withLocalVariableBodyCompat`（即 `withTowerDataCleanup`）内部、退出时被回滚。存储已移到各自清理之外。解锁任务 3.7、3.10、9.1。副作用记录仍然成立：`scopeContext(position)` 额外拼接文件级查找层，函数体内才能看到顶层声明。
4. ~~**类字段/属性不出现在成员作用域**~~ —— **已修**（2026-10-07 回写）。根因不是「四层作用域没覆写 `processVariablesByName`」——那四处确实补齐了但症状仍在；真正的包装层是 **`CfirClassUseSiteMemberScope`**（`CaType.scope` 实际落到它），它同样只覆写了 callable、属性与名称集合。补上后字段候选出现，并连带暴露第二个缺口：`CaCfirFieldSymbol.createPointer` 直接抛 `Field symbol cannot create a stable pointer`，成员作用域一旦真的暴露字段就无法渲染，已按 `CaCfirMemberFunctionSymbolPointer` 的形状补齐字段指针。**回归已锁定**：共享夹具 `analysis/analysis-api/testData/components/scopeProvider/memberScope/memberScopeQueries.cj` 补入 `public var field` 与 `public let state` 及对应可用名/按名查询指令，golden 已重生成。本条原先写作「根因未最终定位」，那是过时表述。详见 `implementation-log.md`「字段成员缺失缺陷」节。

---

## 6. 验证纪律（这条务必先读）

**一次运行里混用多个 `--tests` 过滤时，`BUILD SUCCESSFUL` 不能作为通过证据。**

本变更早期曾因此报错一次结论：命令行同时带了两个 `--tests`，其中一个过滤的用例**根本没执行**，Gradle 因另一个过滤命中而整体成功，我却报告「4/4 PASS」。后来单独运行才发现该用例仍是红的，并已撤回勾选、在 `implementation-log.md` 写更正块。

**正确做法**：

1. 一次只跑一个测试类；
2. 跑完读 `build/test-results/test/TEST-*.xml`，核对根节点的 `tests=` / `failures=` / `errors=`；
3. 只有 XML 里的执行数与预期用例数一致，才能声称通过。

分析层套件另注意：PSI/LightTree 双套件需要 `--rerun`；golden 用 `-Dupdate.test.data=true` 重生成。

---

## 7. 环境与红线

- **Gradle 必须串行**。两个 Gradle 进程同时写同一工作树的 `build/` 会互相破坏产物。
- **严禁任何 Git 回滚命令**：`git reset`、`git checkout -- <file>`、`git restore` 一律禁止（用户可能正在手动编辑文件）。发现文件被改动时，在当前状态上继续工作，不要「恢复」。
- **不要写主检出**：`D:/code/intellij/cangjie` 有 7 个用户手动修改的文件，不得触碰。本变更全部工作在两个工作树内。
- **git stash 共享**：跨工作树共享同一 stash 栈。需要暂存时用 `git stash push -u -m "<唯一标签>"`，立刻用 `git stash list --format='%H %gs'` 记下 SHA，恢复用 `git stash apply <sha>`（不是 `pop`）。
- 工作树会话的 git 受守卫限制在本工作树内，**合并回 main 只能在共享检出里做**。需要合并时：用 `git merge-tree` + `git commit-tree` 预备好提交，交给用户在共享检出中 merge。
- **含中文的文件一律用 Write 工具写**，不要用 bash heredoc（会被按 GBK 写成乱码）。
- Python 脚本偶发 `FileNotFoundError`：会话 cwd 会漂移，用绝对路径或显式 `cd`。

---

## 8. 建议的下一步顺序

1. **回写勾选框**：3.4、9.2 已有执行证据，可在 `tasks.md` 勾选（会变成 25/82）。补记 `implementation-log.md`。
2. **跑其余三个区段的回归**（keyword / localScope / topLevel）。本次会话只单独跑了 member 类确认无编译错误与 4/4 通过，其余三类在 9.2 修复后**尚未复跑**——区段都改用了新工厂，可能受影响。
3. **第 10 组实现**（10.1–10.7 契约已就位，是当前最大的一块未做工作）：
   - 10.1 `CaLookupElementFactory`：把快照映射为平台 `LookupElement`，携带图标、typeText、tailText、插入处理器；用 `putUserData` 挂快照供恢复
   - 10.2 `CaCompletionRecoveryService` 实现：重新进入分析恢复指针 + 文档版本与区间校验，失效候选拒绝写文档
   - 10.3 权重 → 平台优先级映射（结构化数据已在 `sort`，**不要为每个维度注册 XML weigher**）
   - 10.4 `CaCompletionFilter` 实现 + 期望类型 provider（走 `CaCfirExpressionTypeProvider`）
   - 10.5/10.7 导入计划写事务：幂等、组织名/alias、冲突处理、单次撤销
   - 10.6 插入处理器：括号、已有实参、命名冒号、caret
4. **9.4 命名实参 / 9.6 enum**：`namedParameterList()` 与 `CaEnumConstructorSymbol.payloadTypes` 已在工厂里备好，缺的是候选生成与正反测试。
5. **7.2 生成器**：为 `CaScopeProvider` 新增成员跑真实 context 桥接生成器，任务名需先定位（不能把 diagnostics generator 当成所有 API 的生成器）。

---

## 9. 相关记忆

- `cangjie-completion-alignment-state` —— 本变更的工作树布局与验证记录位置
- `cangjie-analysis-verified-defects` —— 上述四个框架缺陷的实测证据与影响面
- `cangjie-ide-composite-build-verification` —— 复合构建读取主检出源码、描述符装配机制
- `cangjie-cjc-probe-setup` / `cangjie-cjc-syntax-limits` —— 用官方 cjc 验证 `.cj` 夹具的方法与语法边界

---

## 10. 2026-10-04 续作记录

前会话在修复 `CaImportPlanApplier` 编译错时被推理网关错误中断。本次从断点接续：

- 第 10 组实现接线完成并首批转绿：lookup 工厂接入 contributor、服务端「去重 → SMART 过滤 → 权重稳定排序」、documentStamp 全链路（原始文件戳，Long）、导入应用器修复（不可变 builder 返回值、插入点语义、文档缺失显式报错）。
- 9 个测试类 43 用例全绿（impl-shared 4 类 23 例 + impl-cfir 5 类 20 例）；命令与 XML 结果、4 个新夹具的 cjc 1.1.3 取证见 `implementation-log.md`「第 10 组续作」。
- `tasks.md` 回写 10.1、10.4（现 27/82）。10.2/10.3/10.5/10.6/10.7/10.8 部分完成，缺口与理由同节列出。
- 下一步优先级：10.2 recovery 实现（接口已就位）→ 9.4 命名实参 / 9.6 enum → 7.2 生成器定位 → 10.6/10.8 的端到端插入验证（host fixture）。
- 平台环境事实（排查耗时较长，已单独记忆）：本版本 `LookupElementBuilder` 不可变；轻量夹具不注册 `EditorFactory`；PSI 文档写入要求 EDT + 命令上下文。
---

## 0. 变更文档的存放位置（2026-10-07 补充）

本变更的文档**规范副本在主检出**：`D:\code\intellij\cangjie\openspec\changes\align-cangjie-completion-with-kotlin-k2\`。

实施工作树 `.claude/worktrees/completion-k2-musing-bun/` 里也有一份 `openspec/` 副本，
是 `openspec-cn` CLI（`status` / `instructions` 要求变更位于当前目录树内）而放的，**未纳入
该工作树的 git**。两份内容的唯一来源仍是主检出：在工作树副本上改完后必须
`cp` 回主检出，否则主检出（也就是 `/goal` 判定的目标）看到的是旧状态。

---

## 11. 2026-10-08 续作记录

承接 §10 之后的实现（第 11 组：语义分流与降级）。与本文档其余部分的关系：**§3 的未完成清单未变**。

### 11.1 已完成并验证：11.5②「`.cjo` 无源码 / 无索引」降级

- 根因：名称发现通道的第二条（自我描述为「`.cjo` 名字视图」）实际取自
  `symbolProvider.symbolNamesProvider`，它把**源码符号源**也组合进来——在 standalone/LSP 宿主下
  那个符号源的声明 provider 会遍历源码根目录下全部 `.cj` 文件。于是「按前缀发现名字」退化成
  「每键入一次遍历一遍项目源码」，索引形同虚设。
- 修法：`CfirSymbolProvider` 新增 `libraryDeclarationsNamesProvider`（只覆盖 `.cjo`/stub/合成 builtins）；
  库侧实现覆写它，组合方组合子视图，**源码符号源不覆写 → 贡献为空**。
- 过程中打出的第二个缺陷（比第一个更隐蔽）：默认值若取「无法枚举」（`CfirNullSymbolNamesProvider`），
  组合的并集 `flatMapToNullableSet` 会被它拖成 `null`，**别的符号源真实存在的名字一起消失**
  （实测：`.cjo` 的 46 个包被一个源码符号源清空）。默认值改为**空视图**后，漏覆写的表现退化为
  「该符号源缺席」，不再波及他人。
- 证据与执行记录：`implementation-log.md` §11.5②（含探针输出与三套件用例数）。

### 11.2 ⚠️ 这条改动给两个宿主加了硬前置（交接要点）

修好后**源码侧名字只能来自宿主名称索引**。宿主没注册 `CaSymbolIndexQueryService` 时，
「未导入的源码模块声明」这一类候选会整类缺席（这是 §13.1 要求的降级形态，不是可长期保留的状态）：

- **LSP**：本次已在 `AnalysisApiLspServiceRegistrar` 按该文件既有写法补注册
  （`CaStandaloneSymbolIndexQueryService`）。LSP 的补全目前仍走旧实现，接入本管线是 §15 的工作。
- **IDE 插件**（`intellij-ide/`，不在本工作区）：**未做**，属 §6.6② 范围。
  只读核对过 `CaIdeDeclarationProviderFactory`：它的 source 收集器同样是遍历 `allSourceFiles`，
  没有索引实现可依赖；选项是「先注册 standalone 实现（能跑但不快）」或「直接做 6.6② 的持久索引」。

### 11.3 仍未做（本次未动）

- 11.5① 宏展开后可映射代码的识别与可靠源映射：**缺分析层契约**（全仓无
  `macroExpand`/`expandedFrom`/`sourceMapping` 任何入口，展开结果不进可分析 PSI/CFIR），
  须先在分析层立「展开后可映射代码」的表示。
- 第 2/3/4/5/6/7/8/10/12/13/14/15 组的未勾选项同 §3，本次未推进。

### 11.4 文档同步纪律（重申 §0）

`tasks.md` / `implementation-log.md` 的规范副本在**主检出**；工作树副本因 `openspec-cn` CLI 而存在、
未纳入 git。**改完必须 `cp` 回主检出**，否则 `/goal` 看到的是旧状态。

### 11.5 第 15 组（LSP 接入）已开始：15.1 结论＝分支 B

- **15.1 三项结论已写入 `implementation-log.md`**（任务措辞要求「未记录即视为本组未开始」）：
  ① 无头容器**不可能**提供 `Editor`/`CompletionProcess`（`:lsp` 的 `intellijCore()` 制品里
  `com/intellij/openapi/editor` 类数为 0；平台 `core` 制品在该包下 42 个类里没有 `Editor`/`EditorFactory`；
  `CoreApplicationEnvironment` 常量池亦无 `EditorFactory`）⇒ **分支 B**：不注册 `completion.contributor`，直接驱动管线；
  ② 分析对象＝ **B-2 且已存在**（`createAnalyzableSnapshot` → `LspAnalysisPsiFileFactory.createFile` + 真实 use-site 模块）；
  ③ `kind` 恒 `BASIC`；`invocationCount` = `1 + 同一 (uri, version, offset) 的重复请求数`（信号：`TriggerForIncompleteCompletions`）。
- **接线已做、待编译验证**：`lsp/build.gradle.kts` 加三模块依赖；`AnalysisApiLspServiceRegistrar` 注册
  `CfirCompletionServiceProvider` 与 `CaSymbolIndexQueryService`。
- **15.4 前置① 不必再做**：`CompletionDummyIdentifierProvider.provideFor(file, offset)` 在 8.4 已落地（`c4c6c55af`）。
- **15.2/15.3 按分支 B 调整**（tasks.md 已注明）：15.2 只保留「加依赖」一半；15.3 的「上游范围例外」在分支 B 下**不存在**，
  `docs/architecture-host-plugin.md` 应据此改记（proposal.md 的影响面已同步更正）。

### 11.6 变更文档重跑（§12.11）

`verification.md` 已按 §12.11 重跑并回写：`openspec-cn validate` **通过**、4/4 产出物、
**30 需求 / 55 场景**、任务 **107 项（勾选 59 / 未勾选 48）**、相对链接 **89 条全部可定位**。
本轮修掉三处文档缺陷：**84 条相对链接层级错误**（两族深度分别在「另一个工作树内」「intellij-ide 仓内」写成）、
`tasks.md` 3.8 行一处损坏字符、`implementation-log.md` 一处行尾空白。**未重跑**：`design.md` 的逐条源码行号锚点、
`source-baseline.json` 的 SHA-256 漂移核对（理由记在 `verification.md`）。

### 11.7 本轮环境阻塞（交接须知）

另一会话的 Gradle 守护进程长时间占用共享缓存锁 `~/.gradle/caches/9.4.0/generated-gradle-jars`，
导致 **`:lsp` 侧改动（编译 + 套件）与 `impl-shared` 回归未能执行**（八次尝试皆因该锁失败；
`jcmd` 显示持有者 164 线程、确实在构建，不是死锁，故未干预）。**下一次能做主的工作**：
先补跑 `:lsp:compileKotlin`、`:lsp:test --tests '*CangjieSemanticFeatureIntegrationTest*'`、
`:code-insight:completion:impl-shared:test`，再继续 15.5/15.7。清单与状态见
`implementation-log.md`「15.x 分支 B 起步与待验证项」。
