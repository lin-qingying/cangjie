# 交接文档：`align-cangjie-completion-with-kotlin-k2`

> 记录时间：2026-10-03
> 目标：把仓颉补全对齐到 Kotlin K2 原生 IDE 补全（`openspec/changes/align-cangjie-completion-with-kotlin-k2/`）
> 当前进度：**33/82 已勾选**（2026-10-05 更新：本轮新增 1.6、8.8 上游侧、5.7、10.1、10.4、3.10、3.6、3.9、9.1；3.7/3.8 部分完成；本节以上进度描述保留为 2026-10-03 当时的状态）

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
4. **类字段/属性不出现在成员作用域** —— 上一轮补了四处 `processVariablesByName` 覆写后仍复现，根因未最终定位。

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