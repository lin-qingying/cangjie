# 仓颉代码补全对齐 Kotlin K2 —— 设计

> 变更：`align-cangjie-completion-with-kotlin-k2`
> 本地 Kotlin 参考：主仓库 `external/kotlin`、`external/kotlin-plugin`、`external/kotlin-lsp`
> 源码指纹与逐条核对位置见 `evidence.md`、`source-baseline.json`

---

## 1. 上下文

### 1.1 范围与证据规则

本设计对齐的是**本地 Kotlin 插件 K2 原生补全架构与适用于仓颉的语义能力**，不是恢复旧 K1 实现，也不是以启动 LSP 代替原生补全。

工作分两条线：

- **Analysis 模块缺失项**（§6）：公开语义契约、CFIR/低层分析适配及其测试。
- **插件侧缺失项**（§7）：主仓库 code-insight 中的补全实现，以及 `intellij-ide` 的平台接线、装配与验收。

「插件侧」描述职责层，不意味着所有代码都放进 `intellij-ide`。主仓库拥有 code-insight 实现，宿主不重新实现 PSI、解析器或 Analysis。

| 标记 | 含义 |
|---|---|
| 已有，可复用 | 已看到相关实现与调用，不代表所有语义已测试正确 |
| 契约缺失 | 本次核对的公开 API 缺少消费方需要的输入/输出 |
| 实现需补齐 | API 已有，但实际分派或数据流不能覆盖目标场景 |
| 待验证 | 需要实施阶段用例/运行结果确认，不能写成已支持或已失效 |
| 拟新增 | 本设计提出的模块、类型、方法或测试，不是现存符号 |

本设计编写阶段未运行编译、测试、打包或 IDE 沙箱。已有测试源码是验收入口，历史测试 XML 不作为当前通过证据。OpenSpec 结构校验也不证明代码正确。

### 1.2 已核实的关键事实

1. 本地 Kotlin 插件入口按 K1 系统属性选择描述符；K2 描述符加载独立 completion 模块。[K01–K03]
2. Kotlin 平台入口仍继承 IntelliJ `CompletionContributor`。语义管线已分为位置上下文、内部贡献者、section/runner、工厂、排序和插入，不是 K1 的 `BasicCompletionSession`。[K03–K08]
3. `Completions` 的贡献者是内部固定列表。`K2CompletionContributor` 是内部抽象类，不是给每种候选注册公共扩展点。[K05–K06]
4. 仓颉已有文件、包、成员查询，已有 LL 上下文收集、词法 scope 和游离文件实现。需要补齐公开投影及消费契约，不能称为「没有底座」。[A01–A04、A10]
5. 仓颉已有候选三态判定、可见性检查、导入/缩短规划，但三态判定没有执行 Kotlin 那种扩展候选适用性检查。[A05–A09、K09]
6. 仓颉 expectedType 接口存在；当前 CFIR 分派仅处理成功解析调用的实参和 return 表达式，不能据接口存在认定智能补全前提齐全。[A11、K10]
7. LSP 已有正式补全方法，包含 reference variants 和文件/工作区声明回退。它不等同于原生 IDE contributor、插入或产品装配。[L01–L02]
8. 当前宿主实际按 `plugin.xml` 引用模块描述符，base 描述符再 include 上游 code-insight XML。不是文档中旧的 `cangjie-all.xml` 布局。[H01–H02]

### 1.3 关键决策摘要

以下是本设计做出的、会显著影响实施形态的决策，每项在正文都有对应章节。

| 决策 | 结论 | 章节 |
|---|---|---|
| 分层 | 主仓库写共有逻辑；IDE 与 LSP 各自写自己的平台接入，上游无消费点专用代码 | §5 D3 |
| 扩展点装配 | 上游描述符声明「贡献什么」，消费点各自决定「用什么 loader 装载」；IDE 只能 `xi:include`，LSP 需 prime 平台扩展点 | §5 D4 |
| 模块落位 | `:code-insight:completion:{contracts, impl-shared, impl-cfir}`，单向依赖 | §4.1–4.2 |
| 包名 | 沿用 `org.cangnova.cangjie.ide.completion.*`（少数派），并记为显式决策；新模块默认跟随多数派 `codeinsight.*` | §4.7 |
| 代码片段 | 仓颉无脚本，`CaScopeKind` 用 `CodeFragmentMemberScope`，不照抄 Kotlin `ScriptMemberScope` | §6.2 |
| LSP | 纳入范围，与 IDE 共用候选生成实现 | §7.11 |
| 扩展点抽象 | 不为单个实现源引入自定义补全扩展点；出现第二个贡献源时再引入 | §5 D3 |
| 框架缺陷 | 已定位的缺陷必须先在 LL/Analysis 层修，禁止插件侧绕过 | §8 |

---

## 2. 目标 / 非目标

**目标：**

- 保留 Kotlin 的模块、包层次、职责和调用顺序。Analysis 公共类型使用 `Ca`，CFIR/解析层使用 `Cfir`，IDE 入口使用仓颉插件命名。
- 分别建模位置、接收者、签名替换、适用性、可见性、名称可达性和导入策略。
- 完成基础、智能、接收者恢复式链补全，以及仓颉适用的声明生成、文档、代码片段和语言特有场景。分阶段不意味着最终只交付关键字补全。
- 每项任务具备源码依据、模块落点、依赖和正反验收场景。新增接口必须有实际消费者。
- 保留已有 API 和其它消费者的兼容性，建立从公开契约到真实插件装配的测试闭环。
- **两份消费者契约同时成立**：IDE 原生补全（验收在 LSP 未启动条件下完成，见 `specs/ide-completion-support`）与 LSP `textDocument/completion`（见 `specs/lsp-completion-support`），共用同一份候选生成实现。

**非目标：**

- 不移植 Java/JVM 静态成员、SAM、companion、expect/actual 等仓颉不具备的语义。
- 不用全工作区 PSI 扫描替代符号索引或词法查询，不在插件中复制编译器的访问/类型规则。
- 不新增任意长度表达式链搜索。这里对齐的 Kotlin chain 是未解析或未导入接收者恢复，以及复用普通成员补全。[K13]
- 不在 `:lsp` 重新实现候选生成、可见性或排序逻辑——LSP 只做协议适配与扩展点装配。
- 不给每个内部贡献者新建公共扩展点；贡献者列表是模块内部固定列表，对齐 `Completions.contributors`。[K05–K06]
- 不在本设计编写阶段修改实现、构建制品、运行测试、提交或推送。
- 不宣称「Analysis 已全部齐全」或「只缺一个接口」。兼容性和性能须经过阶段准入。

---

## 3. 命名约定

### 3.1 类名前缀

前缀分两类，**不可混用**：

**（一）层前缀**——类型与其所在层强绑定，换层必须改名。

| 层 | 前缀 | 例 |
|---|---|---|
| Analysis 公共契约（`analysis-api`） | `Ca` | `CaScope`、`CaScopeProvider`、`CaScopeContext`、`CaSymbol`、`CaType` |
| CFIR 内部 | `Cfir` | `CfirScope`、`CfirLocalScope`、`CfirSession`、`CfirCompletionSection` |
| 低层分析 | `LL` | `LLCangJieSymbolProvider`、`LLCfirInternals` |
| PSI | `Cj` | `CjFile`、`CjReferenceExpression`、`CjCodeFragment` |
| 平台基础设施（非 PSI、非 CFIR） | `CangJie` | `CangJiePsiFacade`、`CangJieCompletionService` |
| 平台注册入口（出现在描述符里的类） | `CangJie` + 角色 | `CangJieCfirCompletionContributor`、`CangJieRefactoringSupportProvider` |
| LSP 协议适配（`:lsp`） | `Lsp` | `LspCompletionItemConverter`、`LspCompletionSessionStore` |

**（二）子系统前缀**——补全子系统内部、与层无关的契约类型，统一用 `Completion`，**不加层前缀**：

`CompletionFilter`、`CompletionIdentity`、`CompletionSnapshot`、`CompletionRunner`、`CompletionSection`、`CompletionResultSink`、`CompletionRequest`、`CompletionPositionContext`、`CompletionImportPlan`、`CompletionInsertion`、`CompletionSortData`、`CompletionRecovery`。

**迁移的真实状态（已核实：只做了一半）**：`code-insight/completion` 曾整体使用 `Ca` 前缀，现已对**文件名**完成重命名（`CaCompletionFiltering.kt → CompletionFiltering.kt`、`CaCfirSnapshotFactory.kt → CfirSnapshotFactory.kt`、`CaCangJieCompletionService.kt → CangJieCompletionService.kt` 等）。但**类声明未同步**，以下仍带 `Ca` 前缀，属 §3.1 规则的现存违反，**必须一并改掉**：

| 文件 | 仍声明为 |
|---|---|
| `impl-shared/test/…/context/CompletionPositionContextTest.kt` | `class CaCompletionPositionContextTest` |
| `impl-shared/test/…/imports/PsiImportPlanApplierTest.kt` | `class CaPsiImportPlanApplierTest` |
| `impl-shared/test/…/insert/CangjieInsertionTest.kt` | `class CaCangjieInsertionTest` |
| `impl-shared/test/…/insert/InsertionValidatorTest.kt` | `class CaInsertionValidatorTest` |
| `impl-shared/test/…/lookup/LookupElementFactoryTest.kt` | `class CaLookupElementFactoryTest` |

这与缺陷 **D-5** 同源（都是 `Ca` 前缀下线时的机械重命名漏改），**并入 D-5 一并修复**，不单列。

**新代码一律不得在 code-insight / `:lsp` 层使用 `Ca` 前缀。**

> 该迁移目前只存在于上游工作树的**暂存区**，尚未提交（`git log --diff-filter=R` 无记录）。§4.4 的清单对应暂存状态而非任何提交。**任务 1.6 必须先提交它**，否则一次 stash 或工作树重建就会静默还原文件名。

**两层前缀同时适用时**：类型若既是 CFIR 实现又只服务补全，使用层前缀（`CfirCompletionSection`）；若与层无关，使用子系统前缀（`CompletionSection`）。平台注册入口若同时是 CFIR 实现，使用 `CangJie` + 角色（`CangJieCfirCompletionContributor`）——**注册入口优先于层**。

**`:lsp` 的既有例外，不得批量改名**：`:lsp/src/` 中并存两种风格——7 个 `Lsp*`（`LspDocumentStore`、`LspTextEditsComputer` 等）与 **17 个 `Cangjie*`（小写 j，如 `CangjieLanguageServer`、`CangjieTextDocumentService`）**。新代码用 `Lsp*`；**既有 `Cangjie*` 保持不变**，不得因为与 §3.1 的 `CangJie`（大写 J）不一致而批量重命名——那是与本变更无关的独立清理。

**禁止**：同一概念跨层改名（`CaScope` 与 `CfirScope` 是两层各自的类型，可以；同一个类在两层各定义一次且语义相同，不可以）。

### 3.2 文件名

- **文件名 = 主声明名**。允许同族辅助类型共居（既有代码做法：`CompletionFiltering.kt` 内含 `CompletionExpectedTypeKind` 枚举 + `CompletionExpectedType` + `CompletionFilter`；`CompletionIdentity.kt` 内含 `CompletionIdentityKind` + `CompletionIdentity` + `CompletionIdentityData`）。判定「同族」的标准是：这些类型单独存在没有意义，且共同构成一个契约的组成部分。
- **禁止一个文件里出现两个语义无关的声明。**
- 纯函数集合用 `<area>Util.kt`（单数）或 `<area>Utils.kt`（复数），参照 PSI 侧 `psiUtil/` 既有惯例。
- 文件名不得携带实现层次（禁止 `...ImplK2.kt`、`...ImplCfirV2.kt`）——层次属于包。
- 描述符固定放在所属模块的 `resources/META-INF/code-insight/`。

**现存反例（清单）**：

| 文件 | 违反 | 处置 |
|---|---|---|
| `impl-cfir/…/ngJieCfirCompletionContributor.kt` | 文件名丢首字母 `C`；内声明的是 `class CangJieCfirCompletionContributor`，**只有文件名错** | **必修**（D-5） |
| `impl-shared/test/…` 的 5 个文件 | 文件名已改，**类名仍带 `Ca` 前缀**（清单见 §3.1） | **必修**（D-5） |
| `impl-cfir/…/sections/CfirSnapshotFactory.kt` | 不含任何类型声明，只有对 `CaSession` / `CaCallableSymbol` 的 `internal fun` 扩展 | 登记豁免：工具函数集合允许无同名类型 |
| `impl-shared/…/lookup/LookupIcons.kt` | 只声明 `internal fun iconOf`，无同名类型 | 登记豁免：同上 |
| `impl-cfir/test/…/sections/CompletionSectionTestSupport.kt` | 声明的是 `TestCompletionRequest`，与文件名不同 | 登记豁免：夹具文件按内容命名；§3.4 的 `...Support` 示例以实际类型为准 |

### 3.3 方法与属性形态

| 形态 | 规则 | 例 |
|---|---|---|
| 无参抽象成员 | 用 `val` 表达可观察属性，不用 `getXxx()` | `CaScope.declarations: Sequence<CaDeclarationSymbol>` |
| 有参数的查询 | 用 `fun`，参数名表达筛选维度 | `callables(nameFilter: (Name) -> Boolean)`、`callables(names: Collection<Name>)` |
| 符号派生的视图 | 扩展属性 `val X.foo: Y` | `val CaClassLikeSymbol.memberScope: CaScope` |
| 非扩展的按名查询 | `get*` 前缀 | `getFileScope(): CaScope`、`getPackageScope(fqName): CaScope?` |
| 布尔谓词 | `is*`（确定）/ `may*`（允许假阳性） | `isVisible`、`mayContainName`、`hasDefinitelyNoStaticMembers` |
| 工厂 | `create*` | `createCallableLookupElement` |
| 会话内访问 | `context(session: CaSession)` 参数样式；桥接由生成器产出并带「请勿手工修改」头 | 见 `CaScopeProvider.kt` 底部 |

**新增 `Ca*` 成员必须走生成器**：生成器定位完成前允许手工补 `context(session)` 桥接；完成后禁止手改生成文件。§8 缺陷 D-6 登记了该缺口。

### 3.4 契约类型后缀

| 后缀 | 含义 | 例 |
|---|---|---|
| `...Provider` | 服务/工厂提供者 | `CangJieDeclarationProviderFactory` |
| `...Registry` | 跨 session 全局注册表 | `CangJieCompletionServiceRegistry` |
| `...Section` | 一个候选区段 | `CfirMemberCompletionSection` |
| `...Checker` | 布尔判定，无副作用 | `CaCompletionExtensionCandidateChecker` |
| `...Filter` | 变换候选序列 | `CfirExpectedTypeCompletionFilter` |
| `...Data` | 不可变数据载体 | `CompletionSortData`、`CompletionInsertion` |
| `...Plan` | 待执行方案（分析期只算，写入留给执行期） | `CompletionImportPlan` |
| `...Context` | 位置/上下文对象 | `CompletionPositionContext` |
| `...Util` / `...Utils` | 无状态工具函数集合 | `LspCompletionSortingUtil` |
| `...Support` | 测试夹具 | `CompletionSectionTestSupport` |

**硬约束一（分析生命周期）**：`...Plan` / `...Data` / `...Context` 三类**不得捕获裸 `CaSession` / `CaSymbol` / `CaType` / 分析期 `Sequence`**。UI 侧只能持有不可变数据、稳定标识与必要的 smart pointer。这是既有 `CompletionSnapshot` 的约定，`contracts` 与 `impl-*` 的全部类型必须遵守，违反即阻塞项。

**硬约束二（跨请求持有的平台对象）**：平台对象（`LookupElement` 等）**不得**由 `...Plan` / `...Data` / `...Context` 直接持有。跨请求持有只允许经 `LspCompletionSessionStore`，且必须同时携带会话键与过期判定；过期一律返回明确错误，不得写入文档。见 §7.11。

### 3.5 常量、扩展函数与数据类

- 常量：`UPPER_SNAKE_CASE`，放 `companion object` 或文件级 `private const val`。
- 扩展函数：与被扩展类型同包；跨模块的公开扩展必须带 `@CaExperimentalApi` 或明确 opt-in。
- 密封层次 vs 枚举：**`CaScopeKind` 已经是既有的 `enum class`**（`analysis-api/.../api/components/CaScopeContext.kt`，由任务 3.1 交付，含 12 个条目），本次只**增枚举项** `CodeFragmentMemberScope`，**不改其形态**；是否改为 sealed interface 属独立决策，不在本设计范围。现存的 `CompletionIdentityKind` / `ExpectedTypeMatch` / `CompletionExpectedTypeKind` 同为无前缀 enum。
- 数据类：承载不可变数据的类型一律 `data class` + `val`，不得有可变字段。

### 3.6 测试命名

- 顶层测试类 `...Test`，文件名 = 类名。
- 共享夹具 `...TestSupport` / `...TestBase`。
- **测试方法名 = testData 文件名（无扩展名）**，沿用 `AbstractAnalysisApiExecutionTest` 的既有约定。
- 正例与反例必须成对；只断言「出现了某几个名字」不构成验收。

### 3.7 注释

中文 KDoc。公开契约用 `@property` 标注构造参数（参照 `cfir/providers/src/.../scopes/impl/CfirLocalScopeImpl.kt` 中 `CfirLocalScope` 的写法——注意该文件本身是 §3.2「文件名 = 主声明名」的既有反例，属上游历史遗留，不作为新代码范例）。**每个公开契约需注明对位 Kotlin 实体；无仓颉对位时必须写明原因**——这是仓库 Cfir/K2 对齐约定的要求。

---

## 4. 模块与文件位置

### 4.1 Gradle 模块

| Gradle 路径 | 目录 | 职责 | 与 Kotlin 对位 |
|---|---|---|---|
| `:code-insight:completion:contracts` | `code-insight/completion/contracts/` | 跨模块服务接口、lookup 数据契约、过滤契约、恢复契约。不含实现，不依赖 `analysis-api-cfir` | `completion/api` + 部分 `impl-shared` |
| `:code-insight:completion:impl-shared` | `code-insight/completion/impl-shared/` | 平台参数包装、位置上下文分类器、dummy、runner/sink、snapshot 工厂、lookup 元素工厂、插入与导入应用 | `completion/impl-shared` |
| `:code-insight:completion:impl-cfir` | `code-insight/completion/impl-cfir/` | CFIR 侧 section 实现、快照工厂 CFIR 特化、期望类型过滤、恢复服务、生产描述符 | `completion/impl-k2` |

三个模块目录**仅存在于实施工作树** `completion-k2-musing-bun`（主检出 `D:\code\intellij\cangjie\code-insight\` 下无 `completion` 目录）。已核实：该工作树 `settings.gradle.kts` 已注册全部五条（`:code-insight:completion:{contracts,impl-shared,impl-cfir}` 与两条 `prepare` 制品），`contracts` 8 个 `.kt`、`impl-shared` 16 个、`impl-cfir` 18 个，合计 42 个（已排除 `bin/`、`build/`）。宿主工作树 `musing-bun-5ae793` 也已完成 substitution 与 `xi:include`。

**主检出的差项**（把成果并回主检出时必须补齐，这是模块骨架任务的真正剩余工作）：
- `D:\code\intellij\cangjie\settings.gradle.kts` 无任何 completion 相关 `include`；
- `D:\code\intellij\cangjie\intellij-ide\settings.gradle.kts` 无 completion substitution，且仍是写死的 `includeBuild("../")`；
- `D:\code\intellij\cangjie\intellij-ide\modules\ide\base\src\main/resources/org.cangnova.cangjie.ide.base.xml` 无补全描述符的 `xi:include`。

**DevEco 侧差项（第三个出货宿主，装配机制不同，见 §5 D4）**：
- `deveco/settings.gradle.kts` 无 completion substitution（实测计数为 0；该文件已有 `includeBuild("../")` 与 code-insight substitution 块）；
- `deveco/product/build.gradle.kts` 的模块清单需加三个 completion 模块；
- `deveco/product/src/main/resources/META-INF/plugin.xml` **无法用 `xi:include`**（其头部记录 jar 形态下解析失败），须按同文件既有条目（533 / 753 行）**手工追加**一条 `completion.contributor language="CangJie"`。

> **DevEco 侧的过期注册是静默失效**，不像 IDE 侧缺失 include 会构建失败。因此 §7.10 的「唯一性」验收必须**同时覆盖两个生产描述符**，且任何 contributor 改名都要连带核对 DevEco（已写入 D-5 验收与 §9 风险表）。

### 4.2 依赖方向

```
analysis-api ──────┐
                   ├─▶ impl-shared ──▶ impl-cfir
psi / common / util┘        ▲                │
                            └──── contracts ◀┘

:lsp ──▶ contracts + impl-shared + impl-cfir     （协议适配，见 §7.11）
intellij-ide ──▶ 三者（经 prepare 制品 + 描述符 include）
```

`contracts` 不依赖 `impl-*`；`impl-shared` 不依赖 `impl-cfir`。`:lsp` 可以依赖三者（它是消费点），但**不得被任何上游模块依赖**。任何反向依赖即为阻塞项。

> 现状差异：`impl-cfir` 的 `main` 依赖目前声明的是 `contracts`、`impl-shared`、`:analysis:analysis-api`、`:psi`、`:common`，`analysis-api-cfir` 只出现在 `testImplementation`。**上图是目标依赖图**；`impl-cfir` 接入 `analysis-api-cfir` 是待办项。

### 4.3 制品与宿主接线

照 `code-insight/refactoring` 的既有四处形态，新增 completion 对应项：

1. `prepare/ide-plugin-dependencies/cangjie-frontend-code-insight-completion-for-ide/build.gradle.kts` → `publishCangjieJarsForIde(listOf(":code-insight:completion:contracts", ":code-insight:completion:impl-shared", ":code-insight:completion:impl-cfir", ":psi", ":common", ":util"))`
2. `prepare/ide-plugin-dependencies-module/cangjie-frontend-code-insight-completion-for-ide-module/build.gradle.kts` → module 形态，`api(project(...))` + `configurations.configureEach { exclude(group = "com.jetbrains.intellij.platform") ... }`
3. 主检出 `settings.gradle.kts` → 三条模块 `include` + 两条 `prepare` 的 `include`
4. `intellij-ide/settings.gradle.kts` → `substitute(module("org.cangnova.cangjie:cangjie-frontend-code-insight-completion-for-ide")).using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-code-insight-completion-for-ide-module"))`

**平台 artifact 归属待实测**（已逐模块核对）：`api` / `fixes` / `folding` / `highlighting` 只声明 `compileOnly(intellijCore())`；`refactoring` 另有 `analysis` / `indexing` / `refactoring` / `usage-view`；`formatting` 另有 `code-style` / `code-style-impl` / `util-jdom`；`override-implement` 另有 `core-ui`。`:lsp` 另外声明了 `analysis` / `indexing` / `usage-view` / `refactoring` / `code-style` / `code-style-impl`。**没有任何模块声明 `completion` artifact**（全仓 `build.gradle.kts` 检索 `intellij.platform:completion` 零命中）；`CompletionContributor` / `LookupElementBuilder` 很可能在 `intellijCore()` 内。以任务 1.4 的核验结论为准，**不得以「Gradle 能编译」推定归属正确**。

### 4.4 现有文件清单

根目录 `code-insight/completion/`。状态列含义：**已存在** = 实施工作树中已有；**待重命名** = 文件名违反 §3.2。

清单以**上游工作树的 `git ls-files`（当前暂存状态）**核对，共 **42** 个 `.kt`（8 + 16 + 18），已排除 `bin/`、`build/`。**注意基线归属**：变更基线 `b10638242` 早于补全模块（模块在后续提交进入），HEAD 上该目录为 71 个文件，而工作树索引为 42 个 `.kt`——**这三个数字都不等于本清单，清单对应的是暂存状态**。且 `Ca` 前缀下线的 27 个重命名**尚未提交**（见 §3.1），任何 `git stash` / 工作树重建都会静默还原这些文件名。**G0 的第一件事是把该迁移提交，避免 §4.4 失效。**

```
contracts/
  build.gradle.kts
  src/org/cangnova/cangjie/ide/completion/api/
    CangJieCompletionService.kt              已存在  （CangJieCompletionService / CompletionKind / CompletionRequest / CompletionSnapshot）
    CangJieCompletionServiceRegistry.kt      已存在
    CompletionFiltering.kt                   已存在  （CompletionExpectedTypeKind / CompletionExpectedType / CompletionFilter）
    lookup/
      CompletionIdentity.kt                  已存在  （CompletionIdentityKind / CompletionIdentity / CompletionIdentityData）
      CompletionImportPlan.kt                已存在
      CompletionInsertion.kt                 已存在
      CompletionRecovery.kt                  已存在
      CompletionSortData.kt                  已存在

impl-shared/
  build.gradle.kts
  src/org/cangnova/cangjie/ide/completion/impl/shared/
    AbstractCangJieCompletionContributor.kt  已存在
    CompletionRunner.kt                      已存在  （CompletionResultSink / CompletionSection / CompletionRunner）
    CompletionSnapshotData.kt                已存在  （CompletionSnapshotData / CompletionSnapshotBuilder）
    PlatformCompletionRequest.kt             已存在
    context/CompletionPositionContext.kt     已存在
    dummy/CompletionDummyIdentifierProvider.kt 已存在
    imports/ImportPlanApplier.kt             已存在
    insert/CompletionInsertHandlers.kt       已存在
    insert/InsertionValidator.kt             已存在
    lookup/LookupElementFactory.kt           已存在
    lookup/LookupIcons.kt                    已存在

impl-cfir/
  build.gradle.kts
  resources/META-INF/code-insight/cangjie-code-insight-completion.xml  已存在
  src/org/cangnova/cangjie/ide/completion/impl/cfir/
    CfirCompletionSection.kt                 已存在
    CfirCompletionService.kt                 已存在
    CfirCompletionServiceProvider.kt         已存在
    ngJieCfirCompletionContributor.kt        待重命名 → CangJieCfirCompletionContributor.kt（§8 D-5）
    filter/CfirExpectedTypeCompletionFilter.kt  已存在
    recovery/CfirCompletionRecoveryService.kt   已存在
    sections/CfirKeywordCompletionSection.kt    已存在
    sections/CfirLocalScopeCompletionSection.kt 已存在
    sections/CfirMemberCompletionSection.kt     已存在
    sections/CfirSnapshotFactory.kt             已存在
    sections/CfirTopLevelCompletionSection.kt   已存在

impl-shared/test/org/cangnova/cangjie/ide/completion/impl/shared/
  context/CompletionPositionContextTest.kt   已存在
  imports/PsiImportPlanApplierTest.kt       已存在
  insert/CangjieInsertionTest.kt            已存在
  insert/InsertionValidatorTest.kt          已存在
  lookup/LookupElementFactoryTest.kt        已存在

impl-cfir/test/org/cangnova/cangjie/ide/completion/impl/cfir/
  filter/CfirExpectedTypeCompletionFilterTest.kt        已存在
  recovery/CfirCompletionRecoveryServiceTest.kt        已存在
  sections/CfirKeywordCompletionSectionTest.kt         已存在
  sections/CfirLocalScopeCompletionSectionTest.kt      已存在
  sections/CfirMemberCompletionSectionTest.kt          已存在
  sections/CfirTopLevelCompletionSectionTest.kt        已存在
  sections/CompletionSectionTestSupport.kt             已存在
```

> `handover.md` §4.3 与 `implementation-log.md` 记录的 `CaCompletionIdentity.kt` 等文件名是**重命名前**的旧名；实际文件已无 `Ca` 前缀，见 §3.1 的迁移说明。

### 4.5 本次新增文件

| 章节 | 文件 | 动作 | 模块 |
|---|---|---|---|
| §6.2 | `analysis/low-level-api-cfir/src/.../compile/CodeFragmentScopeProvider.kt` | **改**：实现 `getExtraScopes` 桩（现为 `emptyList()`） | Analysis |
| §6.2 | `analysis/analysis-api/src/.../components/CaScopeContext.kt` | **改**：既有 `enum class CaScopeKind` **增枚举项** `CodeFragmentMemberScope`（不改形态） | Analysis |
| §6.2 | `analysis/analysis-api/src/.../components/CaScopeProvider.kt` | **改**：增 `CjCodeFragment.fragmentMemberScope` 与 `scopeContext` 入口 + 生成桥接 | Analysis |
| §6.2 | `analysis/analysis-api-cfir/src/.../scopes/CaCfirScopeContext.kt` | **改**：在既有片段/位置作用域投影中增片段分支（该文件已持有 `CaScopeWithKind` 与 `toScopeKind`） | Analysis |
| §6.2 | `code-insight/completion/impl-shared/src/.../context/CompletionPositionContext.kt` | **改**：增代码片段分支（**不新建文件**） | impl-shared |
| §8 D-1 | `cfir/resolve/src/.../body/CfirTowerDataContext.kt` | **改** | LL |
| §8 D-2 | `analysis/analysis-api-cfir/src/.../symbols/CaCfirPublicSymbolKeyMapping.kt` | **改** | Analysis |
| §7.11.1 | `lsp/src/org/cangnova/cangjie/lsp/analysis/completion/LspCompletionHeadlessRegistrar.kt` | **新增** | `:lsp` |
| §7.11.3 | `lsp/src/org/cangnova/cangjie/lsp/analysis/completion/LspCompletionItemConverter.kt` | **新增** | `:lsp` |
| §7.11.3 | `lsp/src/org/cangnova/cangjie/lsp/analysis/completion/LspCompletionItemKindProvider.kt` | **新增** | `:lsp` |
| §7.11.3 | `lsp/src/org/cangnova/cangjie/lsp/analysis/completion/LspCompletionSortingUtil.kt` | **新增** | `:lsp` |
| §7.11.4 | `lsp/src/org/cangnova/cangjie/lsp/analysis/completion/LspCompletionSessionStore.kt` | **新增** | `:lsp` |
| §7.11.7 | `lsp/test/org/cangnova/cangjie/lsp/analysis/completion/LspCompletionTest.kt` | **新增** | `:lsp` |

代码片段的**位置上下文分类**并入既有 `CompletionPositionContext.kt`（增一个分支），**不新建文件**——该文件已是所有位置上下文的唯一入口，新建平行文件会分裂分类逻辑。

### 4.6 需修改的既有文件

**跨三个仓库，必须分开看**——`:lsp` 属于**上游 monorepo**（`D:\code\intellij\cangjie`），不是宿主插件仓库；宿主 LSP 客户端（`modules/ide/lsp`）属于 `intellij-ide`。§7.11 的前置链因此横跨两个仓库。

**（一）上游工作树** `.claude/worktrees/completion-k2-musing-bun`

| 文件 | 改动 | 章节 |
|---|---|---|
| `code-insight/completion/impl-cfir/resources/…/cangjie-code-insight-completion.xml` | **已注册** `completion.contributor` → `CangJieCfirCompletionContributor`；本次只补 dummy service / char-filter（必要时 statistician） | §7.7 |
| `analysis/analysis-api-standalone/src/…/PluginStructureProvider.kt` | `allowedExtensionPointNames` 增补条目（**范围例外，见 §7.11.2**） | §7.11.2 |
| `settings.gradle.kts` / `docs/module-catalog.md` / `code-insight/README.md` | 模块注册与目录文档 | §4.3 |
| `prepare/ide-plugin-dependencies{,-module}/…-completion-for-ide{,-module}/build.gradle.kts` | 两种制品 | §4.3 |
| `analysis/low-level-api-cfir`、`cfir/resolve`、`analysis/analysis-api{,-cfir}` | 缺陷 D-1 / D-2 与代码片段桩与契约 | §6.2、§8 |

**（二）上游 monorepo `:lsp`**（`D:\code\intellij\cangjie\lsp`；**无对应工作树，其既有接线在主检出中**）

| 文件 | 改动 | 章节 |
|---|---|---|
| `lsp/build.gradle.kts` | 新增三个 completion 模块依赖 | §7.11.1 |
| `lsp/src/…/AnalysisApiLspServiceRegistrar.kt` | 追加 registrar 调用与上游补全描述符装载 | §7.11.2 |
| `lsp/src/…/AnalysisApiCangjieAnalysisFacade.kt` | 现有 `completion`（185–233 行）迁至 `legacyCompletion` 并加开关，新实现成为默认 | §7.11.5 |
| `lsp/src/…/capabilities/CangjieServerCapabilitiesFactory.kt` | **声明** `capabilities.workspace.applyEdit = true`（当前未声明，服务端无权发送 `workspace/applyEdit`） | §7.11.3 |
| `lsp/src/…/capabilities/CangjieLanguageServerDescriptor.kt` | `executeCommands` 加入 `applyCompletion`（当前为空列表，`executeCommandProvider` 不会被广告） | §7.11.3 |
| `lsp/src/…/server/CangjieWorkspaceService.kt` | `executeCommand` 由 `completedFuture(Any())` 空桩改为路由 `applyCompletion` | §7.11.3 |

**（三）宿主仓库 `intellij-ide`**（工作树 `musing-bun-5ae793`）

| 文件 | 改动 | 章节 |
|---|---|---|
| `modules/ide/base/src/main/resources/org.cangnova.cangjie.ide.base.xml` | 新增一行 `xi:include`（**工作树已完成，主检出待补**） | §7.10 |
| `modules/ide/lsp` | 注册 completion feature | §7.11.8 |
| `settings.gradle.kts` / `gradle/libs.versions.toml` | substitution 与 Version Catalog（**工作树已完成，主检出待补**） | §4.3 |
| `modules/ide/base/build.gradle.kts` / `modules/test-support/build.gradle.kts` / `product/idea-plugin/build.gradle.kts` | compileOnly/runtimeOnly/test 依赖与测试运行时资源 | §7.10 |
| `docs/architecture-host-plugin.md` | 责任域文档：登记 `allowedExtensionPointNames` 这条例外 | §7.11.2 |

### 4.7 包名决策

实际包名为 `org.cangnova.cangjie.ide.completion.{api, impl.shared, impl.cfir}`。`code-insight` 模块的**多数派**是 `org.cangnova.cangjie.codeinsight.<feature>`（`api`、`folding`、`refactoring`），`org.cangnova.cangjie.ide.*` 是少数派（`fixes`、`override-implement`）。

**决策**：本模块沿用 `org.cangnova.cangjie.ide.completion.*`，与少数派一致；**迁移成本高于收益，不改**。需在代码库文档中记此决策，避免后续误判为笔误。**新模块默认跟随多数派 `codeinsight.*`。**

### 4.8 持久化符号索引的位置

索引契约与实现分三层落位，与 §6.9 的查询契约对应：

| 层 | 落位 | 前置改动 | 持久化形态 |
|---|---|---|---|
| 契约 | `analysis/analysis-api-platform-interface` 的 `CaSymbolIndexQuery`（按名称/前缀过滤 + analysisScope 限定） | 新增 | — |
| IDE 实现 | 宿主侧的 `CaSymbolIndexQuery` 实现 | **按项目服务注册，不新增扩展点**——§5 D3 已否决为单个实现源引入自定义扩展点；平台 artifact 归属待任务 1.4 核验（见 §12.1） | **跨会话持久**，随 IDE 索引增量更新 |
| 无平台实现 | `analysis/analysis-api-standalone` | **两处前置**：① `analysis-api-standalone` 当前**不依赖** `:analysis:stubs`，需新增该模块边；② `CaStubSnapshotBuilder` 与 `CaStubSnapshot` 均为 `internal`，需放开为公开 API。二者都必须登记为 §4.6 的改动项 | **会话内**，按 `modificationCount` 重建；进程存活期间有效，**不得宣称持久** |
| 消费 | `code-insight/completion` 的**新增索引召回段** | `CfirCompletionService.sections` 现为固定的四个区段（keyword / localScope / topLevel / member），**没有索引召回区段**；须新增该区段（对位 Kotlin 的 `FROM_INDEX` 优先级档），并在 §7.4 的家族表中补一行 | — |

`.cjo` 侧统一走 `cfir/cfir-serialization` 已有的只读名字视图（`CfirDeserializedSymbolNamesProvider` / `CjoExportedTopLevelNamesResolver`），不另建索引。

> **消费者必须真实存在**：§2 要求「新增接口必须有实际消费者」。索引若无召回区段，则 §6.9 的交付物没有消费方——因此「新增索引召回段」是 §6.9 的**组成部分**，不是可选优化。

---

## 5. 决策

### D1 模块及接口落位

**选择**：复用现有 Analysis/CFIR 层，按 §4.1 新增三个模块。保持 Kotlin `api / impl-shared / impl-k2` 的结构，后端名称换为 `contracts / impl-shared / impl-cfir`。

使用主仓库 `projectDefault()` 的 `src/resources/test/testResources` 布局。`impl-shared` 允许依赖 PSI、必要的 IntelliJ API 和 Analysis 公共契约，**不把它误称为纯平台无关库**。`impl-cfir` 经 Analysis API 消费语义，**不能直接修改 CFIR resolver 状态**。[P01–P03]

宿主由 `modules/ide/base` 消费接口和描述符，由 `product/idea-plugin` 打包。不新增宿主本地 `analysis/` 或 `psi/` 模块。

**未选方案**：把全部补全塞入 quick-fix API；在宿主重建旧 analysis 模块；给每个内部 contributor 新建公共 EP。这些分别破坏职责、上游所有权或偏离参考设计。

### D2 语言差异

| Kotlin 参考点 | 仓颉处理 | 依据及验收 |
|---|---|---|
| extension callable | extend 目标、实例化、where；复用 compiler matcher | 官方 direct_extension；§6.5 |
| enum entry/构造器 | 无参值、有参构造器、模式区分；允许同名不同参数数目 | 官方 enum；§7.4 |
| `$name` 与 `${...}` | 使用仓颉 `${...}`；原始字符串不插值 | 官方 strings；§7.2 |
| 命名实参 `name =` | 仅命名参数可用，调用插入 `name:`，默认值按仓颉 | 官方 define_functions/call_functions |
| when/companion/expect-actual/SAM/JVM static | Kotlin 专属不实现；适用部分另映射 match、enum、静态成员 | 逐项登记，不按名称自动映射 |
| import/FQN | `org::pkg` 与表达式名称分离，alias 后使用别名 | 官方 import；§6.6/7.7 |
| JVM 库符号 | .cjo、仓颉 SDK/模块可见性，不用 class fixture 替代 | 官方 cjo_artifacts；§6.9 |
| 宏 | Tokens 输入与展开代码分流，可靠映射下才提供展开语义 | 官方 implementation_of_macros |
| script / code fragment | 仓颉无脚本，映射到 code fragment（表达式片段 / 类型片段） | §6.2 |
| expected-type 特例 | 不搬 Kotlin delegated supertype/表达式体等专属语法 | §6.8 公开 API 与编辑态双层用例 |

补充语义边界：仓颉支持尾随 lambda，参数名建议仍属 §7.8，使用 `=>` 而非 Kotlin `->`；排除的是 implicit `it`、componentN 等专属规则，不是整个候选家族。Kotlin `::foo` 形式不移植，但仓颉普通函数值候选必须保留。Kotlin XML 额外注册的 IDE command-completion（重构/导航等命令面板）不属于本设计的语言候选范围，不以「仓颉语义不适用」描述。

调用恢复优先复用 `CaErrorCallInfo.candidateCalls`：它包含本次解析保留的候选，不等同于 Kotlin 完整 `collectAllCandidates` 查询。只有各保留候选都能提供一致、已确定的参数约束时才返回 expected type；命名实参/组合候选若需要额外收集能力，再按 §6.5/§6.8 的窄接缝原则补充。

### D3 分层原则：主仓库写共有逻辑，消费点写自己的平台接入

**上游模块只写共有逻辑**——语言语义、作用域、候选收集/去重/排序，以及用平台词汇表达的注册声明（描述符 XML 是模块对平台的公开接口，与模块对外暴露一个 class 同性质）。**平台接入由各消费点自己写**：IDE 侧写 `xi:include` 与制品依赖，LSP 侧写 headless 扩展点注册与协议映射。**上游不得出现消费点专用代码。**

边界裁定：

- 描述符 XML **不是**消费点专有代码——它是模块对平台的贡献声明，描述「我贡献什么」；消费点只决定「用什么 loader 装载它」。上游描述符已注册 `completion.contributor` → `CangJieCfirCompletionContributor`，IDE 与 LSP 装载同一份文件。
- `PluginStructureProvider.allowedExtensionPointNames` 增补条目 **不是**消费点专有代码——该列表表达「此扩展在无头容器可安全装配」这一关于扩展的事实，已有 16 条同类条目、同时服务 standalone / LSP / 测试三方。
- LSP 侧的 headless 扩展点 prime、`CompletionItem` 字段映射、命令与 resolve **是**消费点专有代码，落在 `:lsp`。

**与 Kotlin 参考的有意偏离（必须记录，不得以「Kotlin 这么做」为论据）**：

Kotlin 参考**并不是**「同一份描述符、两个 loader」。已核实：`external/kotlin-lsp/features-impl/kotlin/resources/META-INF/language-server/features/kotlin/completion.xml` 是 LSP **自己**的一份描述符，重新声明了 `completion.contributor`（`order="first"`）、dummy service 与一个 weigher；而 `kotlinPlugins.kt` 的 `xmlModules` 映射中**不含** `intellij.kotlin.completion.impl.xml`。也就是说 **Kotlin 走的是下面被否决的方案 2**。

本方案**有意偏离**，理由：

1. Kotlin 的做法让 contributor 类名在插件仓库与 LSP 仓库两处重复，上游改类名/挪包需两处同步；
2. 该风险不是理论——本变更自己的缺陷 **D-5**（`CangJieCfirCompletionContributor` 文件名被误改为 `ngJie…`）正是这类前缀漂移的真实案例；
3. 偏离成本极低：LSP 侧只需多写一行描述符路径 + 一个 registrar。**收益的边界必须说清**：`intellij-ide` 与 `:lsp` 两端由此获得绑定单点维护；但 **DevEco 因平台限制无法 `xi:include`，必须手工展开（见 D4），那里的类名仍会出现在两处**。因此「单点维护」是**两个宿主成立、三个宿主不成立**，不能说成本变更名全链路只需改一处。

**已否决的替代方案**：

1. **在上游另建 Kotlin 版扩展点清单** —— 会与描述符 XML 形成两个真相源，且与 `allowedExtensionPointNames` 第三处重叠。扩展点声明只保留 XML 一处。
2. **每个消费点各自手写 `<completion.contributor implementationClass=...>`** —— 同一「扩展点→类」绑定在 N 处重复，上游改类名/挪包需全量同步。**Kotlin 参考采用的正是这一条**；本方案偏离，理由见上。
3. **上游声明自定义补全扩展点、让消费点实现** —— 当前只有一个实现源（引擎自己），属空间接层外加多一处类名重复。**若将来出现第二个贡献源（DevEco 插件贡献仓颉专有符号源、测试注入假符号集），再引入该扩展点并由上游发默认实现。**
4. **让 `:lsp` 依赖平台 `LookupElement` 做候选层抽象** —— 本次不采纳：`:lsp` 已在 classpath 上有 IntelliJ 平台（§7.11），直接共用同一实现比再造一层 transport-neutral 数据模型更省。

### D4 扩展点装配机制

**必须先分清两个出货宿主，装配机制不同**（已核实）：

| 宿主 | 构建根 | 描述符机制 | 缺注册时的后果 |
|---|---|---|---|
| `intellij-ide` | 独立 Gradle 构建，`includeBuild("../")` 接上游 | 模块描述符 + `xi:include` | **构建期响亮失败**（include 指向缺失文件） |
| `deveco` | 独立 Gradle 构建根（自有 `settings.gradle.kts` / `gradlew`） | **单一扁平 `plugin.xml`，手工展开全部注册** | **静默失效**（类名靠反射解析，构建通过但扩展不生效） |

DevEco 的机制不是本设计能裁决的：其 `plugin.xml` 头部已记录平台限制——**DevEco 6.1（IntelliJ 243）的 plugin descriptor `xi:include` 在 jar 形态下解析失败（Cannot resolve /META-INF/*.xml）**，因此把原先按责任域拆分的 `devEco-*.xml` 静态合并进该文件，并约定「后续新增扩展请直接追加到此处」。该文件已手工展开 `lang.namesValidator`、`lang.refactoringSupport`、`CangJieQuickFixService`、`CangJieReferenceMutateServiceImpl` 等上游注册，并已有两条 `completion.contributor`（`CjdbConsoleCompletionContributor`、`CjpmTomlCompletionContributor`）。

因此：

- **IDE 侧不存在「程序化注册扩展点」的路径。** `CoreApplicationEnvironment.registerExtensionPoint` 是 headless/测试设施；IntelliJ 早已移除 `Extensions.getRootArea()` / `ExtensionPointImpl`，因为它们破坏插件卸载、类加载器隔离与「哪个插件贡献了哪个扩展点」的可追踪性。真正的插件只能由平台读自己的描述符来注册实现，`xi:include` 是聚合多 jar 描述符的标准机制。**这不是取舍。**
- **DevEco 侧必须手工追加一条 `completion.contributor language="CangJie"`**，参照同文件既有条目。这是平台限制下的唯一可行方式，**不是设计选择**。
- **LSP 侧必须手写一处**：headless 容器不加载任何插件描述符，**连平台自己的也没有**，因此 `com.intellij.completion.contributor` 不存在，须由 `:lsp` 的 registrar prime。实现声明再由 `PluginStructureProvider.registerApplicationServices` 从同一份描述符装载。该装载路径已在使用中：`:lsp` 已装载 3 份 `META-INF/analysis-api/*` 描述符（`analysisPluginXmls`），并单独装载 1 份上游 code-insight 描述符 `META-INF/code-insight/cangjie-code-insight-refactoring.xml`（`AnalysisApiLspServiceRegistrar.kt` 中 `CangJieRefactoringHeadlessRegistrar` 调用之后）。补全描述符走同一条已验证路径。
- **两处风险性质不同，不得互相套用缓解措施**：IDE 侧重命名会构建失败（安全，可依赖构建发现）；DevEco 侧重命名会静默失去补全（不安全，构建照过）。**任何 contributor 类名变更必须同时核对 DevEco 的扁平 `plugin.xml`，并把这一条写进改名验收**——这也是 §5 D3「绑定单点维护」收益在 DevEco 上不成立的地方。

---

## 6. Analysis 模块缺失项

### 6.1 A1 位置作用域上下文与隐式接收者

- **现状**：`CaScopeProvider` 有文件、包、成员和类型查询，没有 Kotlin `scopeContext(position)` 对应的公开上下文。LL 的 `ContextCollector` 和 compiler scope/tower 基础已存在。[A01、A10、K11]
- **拟新增**：`CaScopeContext`、`CaScopeWithKind`、`CaScopeKind` 与隐式接收者视图；在现有 provider 增加位置及组合查询。
- **实现**：从 LL 目标位置上下文投影 scope 与接收者，保留查找次序、来源及 owner。区分变量、参数、函数和类型声明的可见时机，不机械套用「所有声明都在声明前不可见」。
- **禁止**：在 IDE 遍历 PSI 祖先手写词法规则；公开泄漏 `TowerDataContext` 或 CFIR scope。
- **验收**：嵌套块、参数、同名局部/成员/导入、lambda/成员函数/extend、静态与实例上下文。断言正反候选、身份、scope 层次和接收者类型。
- **依赖**：已有 LL 收集器与会话；副本端到端验收依赖 §6.7。**前置：缺陷 D-1 必须先修。**

### 6.2 A1b 代码片段作用域（code fragment）

- **现状**：仓颉 PSI 已有 `CjCodeFragment` / `CjExpressionCodeFragment` / `CjTypeCodeFragment` / `CjCodeFragmentBase`；`analysis/low-level-api-cfir` 已有 `CodeFragmentScopeProvider`，但 `getExtraScopes(codeFragment: CjCodeFragment): List<CfirLocalScope> = emptyList()` **是硬编码空列表的桩**。
- **对位（已核实）**：Kotlin 侧 `KaScopeKind.ScriptMemberScope` 存在，但它**仅由 `FirScriptDeclarationsScope` 产生**，即只服务 `.kts` 脚本；在 K2 补全里唯一用途是被归类为 `CallableKind.GLOBAL` 权重档。Kotlin 的**代码片段**（`KtExpressionCodeFragment` / `KtTypeCodeFragment` / `KtBlockCodeFragment`）不走独立 scope kind，而是经编译器设施被塑造成脚本形状。对应符号是 `KaScriptSymbol`（**不是** `KaScriptClassSymbol`——后者在三个本地参考中均不存在）。
- **仓颉取舍**：仓颉有代码片段但没有脚本文件。因此**不引入对标脚本的 scope kind 语义**，而是把片段建模为「片段自身的顶层声明构成一个成员作用域」——这是对片段这一真实语言特性的建模，不是对 Kotlin 脚本的对位翻译。命名 `CodeFragmentMemberScope` 而非 `ScriptMemberScope`。
- **拟新增**：
  - `CaScopeKind.CodeFragmentMemberScope`（KDoc 注明「对位 Kotlin `ScriptMemberScope`，仓颉无脚本语义故改名」）；
  - `CaScopeProvider` 增 `CjCodeFragment.fragmentMemberScope: CaScope`；
  - 位置上下文增代码片段分支，使片段内候选走同一 section 管线。
- **实现**：先把 `CodeFragmentScopeProvider.getExtraScopes` 的桩实现掉，再谈补全——**不得在补全侧绕过该桩自行收集片段顶层声明**。
- **验收**：表达式片段（IDE Evaluate Expression、调试器求值窗口）与类型片段各一组正反用例；断言片段内可见片段自身顶层声明 + 目标文件可见符号，且不泄漏宿主/IDE 专有符号。
- **依赖与归属**：作用域契约（`CaScopeKind`、`fragmentMemberScope`、快照桩）属 Analysis 层，落点见 §4.5；位置上下文分类落在 `impl-shared` 的既有 `CompletionPositionContext.kt`（增一个分支，不新建文件）。**前置：§8 缺陷 D-1 必须先修**，否则片段内的局部作用域同样缺失。
- **未决**：两类宿主（IDE 求值窗口 vs 调试器）是否走同一 `CodeFragmentScopeProvider` 路径需实测；不通则该场景单独降级并记录。

### 6.3 A2 文件声明、导入与组合 scope

- **现状**：`CaCfirFileScope` 实际枚举本文件 CFIR 声明。文件/包 scope 的构造器空集合不能直接定性为缺陷。CFIR `createFileLookupScopes` 已组合当前包、显式/星号/默认导入。[A02、A12]
- **选择**：保留文件声明视图，新增明确的 importing/position composite scope。修正文档里「文件 scope 自带所有 import/同包」的含混表述，不无条件扩大既有结果。
- **实现**：复用 lookup bindings、优先级与 alias。组合视图暴露查询名称、来源和 shadow 行为，不无差别扁平化所有 scope。
- **验收**：显式导入、星号、默认、同包、alias/组织名和冲突；文件声明与导入查询分别验收。
- **依赖**：§6.1 数据模型，无须重建 compiler 导入解析。

### 6.4 A3 类型作用域与替换后签名

- **现状**：公开 `CaType.scope` 将具体类型还原成 class-like，再调用 `unsubstitutedScope`，**不能保证实际类型实参保留**。compiler 接收者 scope 路径已有具体 receiver、上界、intersection 和 builtin extend 处理。[A03、A13]
- **选择**：新增 `CaTypeScope` 和保留原声明身份的签名查询，复用 type-to-scope 接缝和现有替换组件。先增加新入口，保留旧 `CaType.scope: CaScope?`；变更旧返回类型必须单独迁移全部调用者。
- **实现**：将适用的底层 scope 构造抽成可复用接口，不能从 resolver 复制到插件。保留继承实例化、override/provenance、typealias 展开和原名展示信息。
- **已有但受限的签名实现**：首次函数签名替换已有代码；替换后的签名再次做非空替换会抛错。变量签名渲染的底层类型强转也必须用 property 专项验证。本项包含组合替换与属性签名闭环，不能只新建类型接口。[A18]
- **验收**：同一泛型的不同实参、连续替换、泛型继承、属性、受约束 extend、primitive/元组/函数/Option/typealias；不仅比较名称，还比较参数与返回类型。
- **依赖**：§6.1/§6.3，不重新实现已有父 scope 遍历。

### 6.5 A4 扩展候选适用性检查

- **现状**：Kotlin 检查器绑定 original file、name expression、explicit receiver，结果含适用性和 substitutor。仓颉现有同名组件只返回可达性三态及 requiredImport，**二者不是能力超集关系**。[K09、A05–A07]
- **拟新增**：独立的适用性入口及 `CaCompletionExtensionCandidateChecker` / `CaExtensionApplicabilityResult` 契约。形状参考 Kotlin，仓颉偏差写入 KDoc。
- **实现**：复用 `CfirExtendProvider`、LL provider、`createExtendDeclarationSubstitution` 和真实候选规则。已确定 receiver 的 extend 匹配使用**带约束**版本，禁止误用只供约束推导的宽松版本。[A14–A15]
- **边界**：Kotlin extension function 与仓颉 extend 不同。完全实例化匹配、泛型/where、成员来源、跨包导出按仓颉处理。若须补充单候选解析接缝，只增加所需模式和适配，**不凭类名另造完整 resolver**。
- **兼容**：旧三态 API 保留并委托明确的可达性逻辑，引用缩短等调用者继续回归。
- **验收**：同名不同 receiver、满足/不满足约束、typealias、无源码依赖、可恢复的未完成调用，同时检查替换后签名。
- **依赖**：§6.1/§6.4/§6.7，独立于是否需要 import。

### 6.6 A5 可见性、遮蔽、可达性和导入规划

- **现状**：`CaCfirVisibilityChecker` 的 receiver 参数**只进入有效性断言**，后续运行自己的访问分支。compiler 调用访问经 `CfirAccessContext` 使用公共 accessibility checker，应收敛适配，不以分支齐全宣称语义完整。[A08–A09]
- **现状**：三态判定直接可达性只查文件 lookup scopes，**不能替代任意位置的局部遮蔽**。缩短目前只覆盖可独立短名化的 class-like 与顶层 callable，不是通用成员导入方案。[A06–A07]
- **实现**：将 Analysis 的文件、位置、receiver 和来源适配 compiler 可访问性接口；区分词法可见性、类型适用性、权限、导入后名称和冲突。
- **导入规划**：复用现有 plan/command，补目标身份、alias 及冲突复验信息。Analysis 只计算方案，写入属于 §7.7。**`requiredImport != null` 不代表加入 import 后一定能安全插入短名。**
- **文档**：本地 Kotlin 参考未定义 `KaImportOptimizer`，**不得根据仓颉 KDoc 虚构同名对位类型**。保留仓颉已有规划能力并说明本项目契约。
- **验收**：权限、局部 shadow、同名导入、alias/组织名、private extend、需要 receiver 的实例成员；插入前后目标一致。
- **依赖**：§6.1/§6.3/§6.5。**失败、未知与不可见不得全部压成 HIDDEN。**

### 6.7 A6 补全副本分析、模式与生命周期

- **现状**：已有 `CaDanglingFileModule`、模式、模块 provider、LL session 和副本处理。普通 copied file 默认 IGNORE_SELF 路径有实现；缺少作用域化模式覆盖/`analyzeCopy` 便捷契约及补全压力验证。[A04、A10]
- **选择**：在已有设施上增加包装与模式覆盖，**不再造临时项目或另一套引擎**。
- **实现**：模式覆盖须线程隔离、可嵌套、异常/取消后恢复。缓存键和模块身份不得污染原件或另一副本。保留 original/completion file 映射，明确局部与非局部声明解析策略。
- **验收**：原件/副本、两种模式、未提交文档、取消、嵌套、跨线程、修改失效和 pointer 恢复。已有原件/副本测试继续保留，**不把包装函数完成当作全部验收**。[T10]
- **依赖**：既有 LL/平台模块设施，是 §7.3 并行的前置闸门。**前置：缺陷 D-3 必须先修。**

### 6.8 A7 期望类型与不完整表达式支持

- **现状**：组件名为 `CaExpressionTypeProvider`，不是另建 ExpectedTypeProvider。当前 expectedType 仅处理成功调用的实参 mapping 和 return。[A11、K10]
- **实现**：补仓颉适用的 typed initializer、赋值、普通 Bool 条件、命名参数默认值及适用分支/块结果。调用尚未成功时只恢复可确定约束，**歧义时不能任取第一个重载**。
- **复用**：实际类型、类型关系、签名替换、data-flow、renderer 已有组件；逐项验证补全消费契约，不全部重写，也不以接口存在认定完整。
- **测试定位**：已有 propertyInitializer golden 的 bottommost expression 是 callee，**不能直接将 null 改为初始化 RHS 类型**。保留定位专项，新增明确 RHS/引用用例。[T03]
- **验收**：公开 API 测合法源码，插件恢复 fixture 测未完成实参/占位符。每个合法 expected-type 场景配 SMART 过滤测试，不要求 BASIC 排除所有类型不匹配项。
- **依赖**：§6.4/§6.7。基础关键字可先完成，完整 SMART 必须通过本项。

### 6.9 A8 名称发现、持久化符号索引和稳定候选身份

- **现状（已核实）**：`analysis/stubs` 提供三层聚合索引——`CaStubSnapshotBuilder.build(modificationCount)` 产出 `CaStubSnapshot`，装配阶段同时建立**文件维度、包级 classifier/callable、class 成员**三类索引；`CaStubIndexFacade`（`analysis-api/.../stubs/CaStubServices.kt`）暴露 `fileProvider`、`packageIndex`、`getClassMemberNames(classId)`。`psi` 侧有自定义 `StubIndexService` 与各元素类型的 `indexXxx(stub, sink)`，并带 `NO_INDEX` 兜底。
- **缺口（精确）**：反向的「`Name` → 声明集合」查询**已经存在**——`CaSymbolProvider.getTopLevelClassLikeSymbols(packageFqName, name)` 与 `getTopLevelCallableSymbols(packageFqName, name)`，由 `analysis-api-cfir` 实现并覆盖源码、模块依赖、stub 库与反序列化 `.cjo` 四类来源；`CaScope.callables(nameFilter)` / `classifiers(...)` / `CaScopeLike.mayContainName(name)` 也已提供按名过滤的符号查询。真正缺的是：**① 前缀 / 驼峰过滤**（现只有单包精确名）；**② 跨模块的工作区级召回并受 analysisScope 限定**；**③ 一个把这两者统一起来的入口**，使补全不必逐包试精确名。[A16]
- **持久化交付物（本次必须落地，不得降级为「以后优化」）**：
  1. **契约**：`analysis-api-platform-interface` 新增 `CaSymbolIndexQuery`，提供「按名称前缀/驼峰过滤 + analysisScope 限定 + 模块依赖可见性」的反向查询，返回符号身份而非索引键；
  2. **IDE 实现**：基于平台索引扩展，**跨会话持久**，随 IDE 索引增量更新；
  3. **无平台实现**：`analysis-api-standalone` 复用 `CaStubSnapshotBuilder` 的快照（按 `modificationCount` 重建），语义等价但**仅会话内有效**——这是无平台宿主的事实，不得宣称持久；
  4. **`.cjo` 侧**：统一走 `cfir/cfir-serialization` 已有的只读名字视图，不另建索引；
  5. 位置见 §4.8。
- **前缀过滤分级**：前缀 ≤3 字符且非重复触发时只做「起始匹配」，否则放宽到子串；空前缀时提示宿主在任意前缀变化时重启补全。避免短前缀下驼峰匹配召回过多候选。
- **身份**：保留声明、参数签名、实例化 receiver 和来源；**不得只按 label 去重**，局部符号不强求 callableId。UI 快照遵守 §7.5 生命周期。
- **验收**：源码/`.cjo`/SDK、未打开文件、未导入类型、同名函数和 enum 构造器；断言按前缀查询命中且**不漏报**；**记录索引查询次数与耗时并设预算，禁止每次键入扫描全工作区**；断言 IDE 实现与无平台实现在同一查询下返回同一符号集合（语义等价性）。
- **依赖**：§6.1/§6.4/§6.6 和现有 platform providers 与 stub 索引。

### 6.10 A9 公开契约、生成器与兼容测试

- 新 API 联动公共接口、实现基类、CFIR 会话组件、生成桥接和测试注册，**不只加接口、不手改生成文件**。[A17、T01–T04]
- 复用 `AbstractAnalysisApiComponentTest` 和共享 testData。CFIR 专有问题补实现回归，每项包含反例。
- 位置 scope context 与代码片段 scope 是新契约；文件/包/成员 scope、candidate decisions、expectedType、dangling 是已有能力的扩展。
- 保留受影响 LSP/refactoring 回归。**未产生新执行结果时，所有实现验收维持未勾选。**

---

## 7. 插件侧缺失项

### 7.1 B1 模块骨架与服务契约

按 §4.1 建模块，保留 `contributors/context/lookups/factories/handlers/weighers` 层次。为独立服务提供接口，内部贡献者采用**显式列表**，不要求公共动态 EP。[K05–K06]

入口与内部命名遵循 §3：`CangJieCfirCompletionContributor`（平台注册入口）、`CfirCompletionRunner`、`CfirCompletionSection`；`Ca` 保留给 Analysis 公共契约。**先检查生产资源与依赖图，空骨架不算功能完成。**

### 7.2 B2 平台入口、参数、dummy 与替换位置

- 单职责补全描述符注册 dummy service、必要 char filter/statistician，由宿主 base 描述符 include。**`completion.contributor` 已在上游描述符中注册给 `CangJieCfirCompletionContributor`，不得重复注册**（重复注册会使 contributor 每次补全执行两遍）。[K02、H02]
- 构造原件/副本、偏移、前缀、invocation count、BASIC/SMART、替换区间；在 `beforeCompletion`/参数准备阶段正确修正位置。
- 覆盖泛型、未完成调用/声明、成员访问、文档和 `${…}`；**不继承 Kotlin `$name`、KDoc、尾随 lambda 的专属规则**。
- 与现有 typed-handler 触发逻辑协调，避免重复弹窗。**autopopup 请求本身不等于语言补全已实现。**
- 依赖 §6.7；测自动/手动、前缀/caret、数字/注释/原始字符串排除及错误 PSI 恢复。

### 7.3 B3 位置上下文、贡献者、runner 和 sink

- 先识别表达式、类型、receiver、import/package、声明、命名参数、操作符、CDoc 和代码片段，再选贡献者。[K04–K06]
- section 执行优先级与展示权重分离。先实现串行优先队列、取消、批量结果和延迟阶段，**不吞掉异常后报告「无结果」**。
- 后续补并行 runner。§6.7 隔离、取消、串行等价验证通过后才能启用；每个会话独立 common data，稳定结果归并，**不跨线程共享裸 `CaSession`**。
- **两个消费者的线程模型都必须覆盖**：IDE 侧在 read action 内取快照、选择时恢复指针；LSP 侧在请求线程内完成，且 `MockApplication`/`MockProject` 上 `CaLifetime` 失效路径与 IDE 不同，需单独验证（见 §7.11）。
- 依赖 §6.1/§6.3/§6.7，测顺序、重入、取消、受限分析和失败观测。

### 7.4 B4 基础候选家族

| 家族 | Analysis 依赖 | 插件职责与验收 |
|---|---|---|
| 局部、参数、类型参数、隐式 receiver 成员 | §6.1/§6.6 | 前缀、词法次序、遮蔽、重复抑制 |
| 显式 receiver、继承、extend、super | §6.4/§6.5/§6.6 | 实例化签名；不混入无关全局声明 |
| 类型、顶层 callable、包/import | §6.3/§6.9 | 已导入/未导入分组、alias、来源 |
| 命名参数及实参组合 | §6.4/§6.8 | 仅支持命名调用的参数提供 `name:`，排除已填参数 |
| 关键字、声明名称与类型建议 | PSI context，必要时 §6.8/§6.9 | 合法位置和仓颉形式，不出现 Kotlin 专属关键字 |
| enum 构造器与模式 | §6.4/§6.6/§6.8 | 无参值/有参调用/模式区分，同名不同 arity 保留 |
| 代码片段内候选 | §6.2 | 片段顶层声明 + 目标文件可见符号 |
| **索引召回候选**（未导入 / 跨模块） | §6.9 | 由 `CaSymbolIndexQuery` 按前缀过滤 + analysisScope 限定召回；与作用域路径候选**共用同一去重器**，被遮蔽的索引候选按 `CaScopeKind` 决定存活 |

按参考贡献者拆分，**不用单个大类按字符串判断所有场景**。过滤必须有 absence 断言，不能只验证出现几个名字。

### 7.5 B5 Lookup 工厂与稳定快照

参照类型/函数/变量/包/命名参数工厂，把符号和替换后 signature 转成显示、图标、来源、排序数据与插入选项。[K07–K08]

`analyze` 内完成语义渲染。UI 只保留不可变数据、稳定标识与必要 smart pointer/`CaSymbolPointer`，**不保存裸会话、类型、符号或惰性分析 `Sequence`**（§3.4 硬约束）。选择时校验版本/位置；需语义则恢复 pointer 并重新分析，**恢复失败不修改文档**。

测试重载展示、泛型、.cjo、分析结束后渲染和选择。

### 7.6 B6 排序、期望类型与 SMART

区分分析期加权和平台 sorter 组合。参考代码在平台锚点组合权重，**不是每个 weigher 都注册 XML**。[K08]

权重覆盖 scope/receiver 来源、类型匹配、import 成本、类别、弃用、参数匹配及稳定同级顺序。SMART 与 BASIC 共用生成，但独立做期望类型过滤/构造。未知时明确降级并测试。

**不机械复制 Kotlin 重复调用放开 private 的策略；仓颉权限过滤不因 invocation count 自动绕过。** 依赖 §6.4/§6.6/§6.8 和 §7.4/§7.5，集合与排序分别验收。**前置：缺陷 D-2 必须先修**，否则排序与去重建立在错误身份上。

### 7.7 B7 插入、导入与引用缩短

- 定义 no-import、add-import、qualified-then-shorten 插入策略，**不与 Analysis 三态混淆**。[K12]
- 复用 import PSI 构造和声明插入。当前 `CangJieReferenceMutateService` 的 bind **只换短名**，不能当成完整导入/冲突缩短实现。[P04–P05]
- 安全分析阶段准备方案，写命令阶段校验文档版本/范围并应用；**失效则重新补全**。
- 覆盖 Enter/Tab/字符选择、已有括号/实参、caret、命名参数冒号、enum 无参/有参、组织名、alias、去重与冲突。
- 组织名前缀留在 import，表达式使用合法名字；不安全缩短时保留合法限定名。
- 验收 after 文本、caret、目标身份、import 和撤销，**不能仅检查 label**。

### 7.8 B8 高级候选、声明生成与接收者恢复

复用 override-implement 的生成与 renderer。跨模块消费 internal 实现前**先提取窄接口**，不直接越过可见性边界。[P06]

完成构造/实例化、适用实参组合、声明建议、super、match/enum、CDoc、代码片段及宏上下文。Kotlin 专属贡献者按 D2 登记不适用或替代，**不默默删除仓颉适用场景**。

chain 对齐**未解析接收者的精确名称候选恢复**，重建上下文并复用普通成员补全，协调 receiver import 与最终插入；单列预算和取消，**不扩展成任意深度表达式搜索**。[K13]

依赖 §7.4–§7.7 和 §6.4/§6.5/§6.8/§6.9。高级场景晚于基础闭环，但仍在最终任务范围。

### 7.9 B9 降级、统计与性能

**IDE 消费者**：dumb mode 只运行无需索引的贡献者，**不扫描全项目**。记录 setup/context/candidate/render/insert 的耗时、候选量、查询和取消，**不记录完整源码或文档内容**。

**LSP 消费者**（无头容器，术语与 IDE 不同，不得套用 IDE 概念）：

| 维度 | LSP 侧要求 |
|---|---|
| 索引不可用 | 无头宿主默认没有 SDK/索引构建期；此时只提供不依赖索引的候选，**不退化为全工作区声明遍历** |
| 取消 | 必须处理 `$/cancelRequest`；取消在请求线程内传播并释放资源，不得留下跨请求状态 |
| 会话过期 | §3.4 硬约束二的过期错误必须被计数上报，不得静默 |
| resolve 开销 | 文档在 `completionItem/resolve` 时才填充，该阶段耗时**单独统计**，不并入弹窗路径 |
| 受限分析 | 与 IDE 共用 Analysis 的受限分析入口与失败分类，不另立一套 |

**共同**：先测冷/热及大小项目基线，再制定可复测预算，并把预算写入**具名测试配置文件**（不得只写在文档里）。**计划不虚构毫秒指标**；阈值由实施时的新测量写入该配置。缓存遵循文档/模块失效，**不永久缓存某次空或异常结果**。

### 7.10 B10 制品、宿主依赖与生产接线

- 按 §4.3 新增 `cangjie-frontend-code-insight-completion-for-ide` 及对应 `-module`。fat jar **明确列出** contracts/impl-shared/impl-cfir 合并输入，不能假定传递项目自动被合并；避免重复打包已有 common/psi/analysis 类。[P07–P09]
- 联动主 settings、module-catalog、prepare；宿主 catalog、substitution、base compileOnly/test、product runtimeOnly/test、test-support 运行时资源。
- 上游单职责描述符 `cangjie-code-insight-completion.xml` 由现有 base 描述符 include；验证资源与唯一性，**不建 catch-all**。[H01–H04]
- **前置检查（已在工作树完成，主检出待补）**：宿主工作树 `musing-bun-5ae793` 的 `settings.gradle.kts` 已把 `includeBuild("../")` 换成可配置解析（Gradle 属性 → 环境变量 → `..`，含 fail-fast 校验），并已完成补全制品的 substitution。**主检出 `intellij-ide/settings.gradle.kts` 仍是写死的 `includeBuild("../")`**，并入时须同步。[H05]
- 多版本差异集中于 `platform/<version>`。**属性文件和占位目录不等于已兼容**；每个承诺平台独立验收。

### 7.11 B11 LSP 接入

LSP 通过平台补全管线接入，与 IDE 共用同一份候选生成实现；`:lsp` 只做协议适配与扩展点装配，**不在 `:lsp` 重新实现候选生成、可见性或排序**。

**前置事实**（已核实）：

| 事实 | 依据 |
|---|---|
| `:lsp` 已以 `implementation` 依赖 `intellijCore()` | `lsp/build.gradle.kts` |
| `:lsp` 已依赖 4 个上游 code-insight 模块（`formatting`/`folding`/`highlighting`/`refactoring`） | 同上 |
| `:lsp` 已用 `*HeadlessRegistrar` 在 `MockApplication`/`MockProject` 注册扩展点与加载上游描述符 | `AnalysisApiLspServiceRegistrar.kt`、`CangJieRefactoringHeadlessRegistrar.kt` |
| 上游描述符可被 `:lsp` 直接装载（已有先例） | `AnalysisApiLspServiceRegistrar` 已装载 3 份 `META-INF/analysis-api/*` + 1 份 `META-INF/code-insight/cangjie-code-insight-refactoring.xml` |
| 上游 `PluginStructureProvider.registerExtensionPointImplementations` **按 `allowedExtensionPointNames` 白名单**过滤扩展点实现 | `PluginStructureProvider.kt` |
| 插件侧 LSP 客户端已声明完整 `CompletionCapabilities`，但当前只注册了 diagnostic / codelens / hover | `intellij-ide/modules/ide/lsp` |

#### 7.11.0 前置最小实验（**必须先做，决定后续走哪条路**）

已记录的平台环境事实（`implementation-log.md`）：**轻量夹具不注册 `EditorFactory`；PSI 文档写入要求 EDT + 命令上下文。** 该事实来自**测试夹具**，LSP 生产容器是另一个容器，是否同样受限**未经验证**。

因此在 §7.11.1 之前必须先做一个最小实验，并按结果二选一：

| 分支 | 触发条件 | 做法 |
|---|---|---|
| **A：走平台补全进程** | LSP 生产容器能提供 `Editor` / `CompletionProcess` | 走标准 `CompletionContributor` 管线，IDE 与 LSP 用同一入口类 |
| **B：直接驱动管线** | 上述不可得 | **不注册 `completion.contributor`**，由 `LspCompletionHeadlessRegistrar` 构造 `PlatformCompletionRequest` 并直接调用 `CfirCompletionService`/section 管线，把结果转成 `CompletionItem` |

> 判定只取决于「能否提供 `Editor` / `CompletionProcess`」这一项。**扩展点 prime 不是未知项**——`CangJieRefactoringHeadlessRegistrar.registerExtensionPoints` 已用同一机制 prime 了 8 个重构扩展点，分支 A 沿用即可，不需重新判定。

**分支 B 的前置（缺一即静默返回空候选，必须先补齐）**：

1. **占位标识符修正的独立入口**。现有 `CompletionDummyIdentifierProvider.provide(parameters: CompletionParameters)` 是**唯一**产出 `CompletionDummyContext`（副本 + 语法合法占位 + 去前缀）的入口，其内部辅助（`replaceWithPlaceholder`、以 `DUMMY_MARKER` 为键的 `prefixOf`）均为 private。分支 B 没有 `CompletionParameters`，**必须新增一个接受 `(CjFile, offset)` 的无参入口**。否则会在裸占位 PSI 上分析并静默得到零候选——正是 `AbstractCangJieCompletionContributor.fillCompletionVariants` 明确警告的失败模式。
2. **`documentStamp` 的来源**。`PlatformCompletionRequest` 需要它，而现有唯一工厂从 `parameters.originalFile…modificationStamp` 取；LSP 侧 PSI 来自 `LspAnalysisVirtualFile`，不会给出有意义的戳。**必须接入 `LspTextDocument.version`**，否则 §3.4 硬约束二的过期判定与 §7.11.4 的过期错误都建立在常量 `0` 上。
3. **`CfirCompletionServiceProvider` 必须在 LSP 容器注册**。`cfirCompletionServiceOrNull(project)` 走 `project.getService(...)`，而上游补全描述符只声明了 `completion.contributor`，**没有 `<projectService>` 条目**。分支 B 下须由 registrar 手工注册（本仓库先例：`AnalysisApiLspServiceRegistrar` 为 `CangJieOpenTelemetryProvider`、refactoring registrar 为 `RefactoringListenerManager` 都手工补了）。否则取到 `null` → 零候选且不报错。
4. **转换对象要说清**。上游管线终点是 `CfirCompletionService.complete(): List<CompletionSnapshot>`，**不是 `LookupElement`**。分支 B 下 `LspCompletionItemConverter` 的输入是 `CompletionSnapshot`，**不经过 `LookupElementFactory`**，因此 §7.11.3 字段表中依赖 `presentation.*` 的三行须改读快照的 `presentableText` / `tailText` / `typeText`。
5. **分析跑在哪份 `CjFile` 上（分支 B 最关键的未决点）**。IDE 路径由平台提供 `parameters.originalFile` 及其补全副本；分支 B 没有 `CompletionParameters`，**拿不到平台生成的副本**，必须自己决定：
   - **(B-1)** 走 §6.7 的 `analyzeCopy` + `CaDanglingFileModule`（PREFER_SELF）自行构造补全副本——语义与 IDE 一致，但**受缺陷 D-3 阻塞**（副本模式下 PREFER_SELF 非局部解析当前返回 `null`，未修）；
   - **(B-2)** 直接对 LSP 侧 `CjFile` 分析——绕开副本，但**与 IDE 的分析语义不等价**，且未提交文档下的行为需另行验证。注意 `LspAnalysisVirtualFile` 是 `LspAnalysisPsiFileFactory` 内的 **private 类**，不能直接引用；B-2 须经该工厂的 `createFile` 路径取得 `CjFile`（即复用 `AnalysisApiLspServiceRegistrar` 已装配的 PSI 工厂链路）。

   **§7.11.0 的实验必须同时判定这一项**，并写明选了哪条。若选 B-1，则 D-3 从「阻塞 §6.7」扩展为「同时阻塞分支 B」；若选 B-2，必须补一条「两端分析语义等价性」的对照用例，否则「同源候选」这一 G5 准入无法成立。
6. **`invocationCount` 与 `completionKind` 的来源**。`CompletionRequest` 需要 `invocationCount`（IDE 侧由 PSI 上的 `INVOCATION_COUNT_KEY` 用户数据携带，**新解析的 LSP 文档不会带**）与 `kind`（由 `completionType` 推导，而 LSP 的 `CompletionParams.context.triggerKind` 与 `CompletionType.BASIC`/`SMART` 并非一一对应）。分支 B 必须定义二者的映射规则，并纳入 §7.11.0 实验结论。

**实验结论与最终分支必须写入 `implementation-log.md`**，未记录即视为 §7.11 未开始。

#### 7.11.1 无头扩展点装配（分支 A）

`lsp/build.gradle.kts` 新增三个 completion 模块依赖；新增 `LspCompletionHeadlessRegistrar`，prime `com.intellij.completion.contributor`（及 `com.intellij.weigher`，若分支 A 需要），写法照 `CangJieRefactoringHeadlessRegistrar` 的 `registerExtensionPointIfMissing`（`CoreApplicationEnvironment.registerExtensionPoint`）。

**验收**：测试中真实触发一次补全并断言走的是仓颉管线；**禁止手工 new contributor 塞进扩展点**。

#### 7.11.2 上游描述符装载与白名单条目（**范围例外，须登记**）

上游 `PluginStructureProvider.allowedExtensionPointNames` 增补条目，由 `AnalysisApiLspServiceRegistrar` 装载 `cangjie-code-insight-completion.xml`。

**这是本变更唯一一处「为消费点改动上游 Analysis 模块」的例外**，理由与边界：

- 该列表表达的是「此扩展在无头容器可安全装配」这一**关于扩展的事实**，已有 16 条同类条目、同时服务 standalone / LSP / 测试三方；
- **但它同时改变 standalone 与测试容器的行为**（这两个容器也会开始注册该扩展的实现）。因此必须：
  1. **枚举**要新增的确切条目名（不得写「等」）；
  2. 在 `intellij-ide/docs/architecture-host-plugin.md` 登记本例外；
  3. 补测试证明 standalone 与测试容器**未因此获得错误的补全行为**（它们不应触发 IDE 补全管线）。

#### 7.11.3 CompletionItem 字段映射

新增 `LspCompletionItemConverter` + `LspCompletionItemKindProvider` + `LspCompletionSortingUtil`。字段映射对位 kotlin-lsp 的 `LSCompletionProviderHelper`：

| LSP 字段 | 来源 |
|---|---|
| `label` | `presentation.itemText ?: lookup.lookupString` |
| `sortText` | 零填充下标（编码上游既有排序，保证客户端字符串比较与服务端顺序一致） |
| `labelDetails.detail` | `presentation.tailText` |
| `labelDetails.description` | `presentation.typeText` |
| `kind` | 按符号/PSI 类型映射，**不得一律降级为 `Text`** |
| `textEdit` | 位置处的空编辑 |
| `command` | `workspace/executeCommand`，指向本模块的 `applyCompletion` 命令 |
| `data` | 会话键（见 §7.11.4） |
| `documentation` | `resolve` 时填（CDoc 渲染） |

**插入机制（必须说准）**：空 `textEdit` + `command` 是 LSP 标准路径——`CompletionItem.command` 由客户端在**接受候选后自动执行**，不需要客户端额外实现。服务端因此有两项责任，**方向不可搞反**：

- `workspace/executeCommand`：服务端**实现**该命令（处理 `applyCompletion`）。当前 `CangjieWorkspaceService.executeCommand` 是返回 `Any()` 的空桩，且 `CangjieLanguageServerDescriptor` 的 `executeCommands` 为空列表——**两处都要改**，否则命令不会被路由。
- `workspace/applyEdit`：这是**服务端 → 客户端的请求**，服务端**不实现**它；服务端要做的是在 `CangjieServerCapabilitiesFactory` 中**声明** `capabilities.workspace.applyEdit = true`，然后在 `applyCompletion` 中**发送**该请求。当前能力工厂的 `WorkspaceServerCapabilities` 只在 `negotiation.workspaceFolders` 分支构造且只设 `workspaceFolders`，**未声明 `applyEdit`**——不声明则无权发送。

kotlin-lsp 的 VS Code 客户端即未自行注册 `jetbrains.kotlin.completion.apply`，同样依赖标准字段；这只说明该机制对 kotlin-lsp 的客户端成立，**不能替代对本仓库客户端的实测**（见 §7.11.8 / §12.8）。

**不设** `commitCharacters` / `filterText` / `preselect` / `deprecated` / `tags`——kotlin-lsp 同样不设，由服务端预排序 + 客户端默认前缀过滤承担。这是**服务端的自觉选择而非能力缺失**：宿主 LSP 客户端 `CangJieLanguageServerFactory` 的 `getCapabilities()` 已声明 `commitCharactersSupport` / `preselectSupport` / `deprecatedSupport` / `tagSupport` / `resolveSupport`。若后续要开启这些字段，改服务端即可，无需改协商。

**验收**：同一候选在 IDE 与 LSP 两端的 `label` / `tailText` / `typeText` / `kind` 一致；`sortText` 顺序与上游排序一致。

#### 7.11.4 会话存储与过期

新增 `LspCompletionSessionStore`，保留活的查找对象 + `data` 会话键，提供 `applyCompletion` 与 `resolve` 两条命令路径。跨请求持有平台对象受 §3.4 硬约束二约束：**必须携带会话键与过期判定**。

**验收**：会话过期返回明确错误（提示重新请求补全），**不抛未捕获异常、不写入过期编辑**。

#### 7.11.5 替换既有退化实现

`AnalysisApiCangjieAnalysisFacade.completion`（185–233 行）现为「优先取 `CjSimpleNameExpression` 的 reference variants；variants 为空时回退到文件作用域声明 + 全工作区 `CjNamedDeclaration` 遍历」。回退分支的 `kind` 全为 `Text`、无签名、无 import 插入，且每次击键扫全树（variants 分支另有 kind 映射与 `detail = fqName`）。

**做法**：新实现成为唯一路径；旧实现**迁至 `legacyCompletion` 并由独立开关保留**，直至 §7.11.7 验收全部通过后再由后续变更删除。**不做「加一层判断」式的混用**——两条路径的候选质量差一个量级，混用会让验收无法判定。

**验收**：同一位置走新路径；断言新实现给出的可见符号集合**严格不少于**旧实现（§7.11.6）。

#### 7.11.6 新旧候选集对比

在一批固定源码位置上对比新旧实现，断言新集合 ⊇ 旧集合，且旧的全工作区遍历路径**不以任何形式残留**（含未被触发的兜底分支）。

#### 7.11.7 LSP 侧测试

**覆盖**：无候选返回合法空列表而非异常；`$/cancelRequest` 取消；`isIncomplete` 与实际是否穷尽候选一致；import 编辑正确性；`resolve` 填 documentation；**`MockApplication` 上的会话失效路径**；**服务端侧只做「计算编辑 + 发 `workspace/applyEdit`」，文本写入由客户端执行**——因此**不断言服务端写事务的 EDT 上下文**（服务端不写文档），改为断言服务端正确声明了 `applyEdit` 能力、正确路由了 `executeCommand`、且发出的编辑内容正确。

> 已记录的平台环境事实「PSI 文档写入要求 EDT + 命令上下文」适用于**宿主内直接写 PSI 的路径**（如 IDE 侧 `InsertionValidator` 测试）。LSP 服务端走 `workspace/applyEdit` 由客户端落盘，不经该路径——两者不可混用。这也是 §10 G5② 措辞的由来。

#### 7.11.8 插件侧 LSP 客户端注册

`intellij-ide/modules/ide/lsp` 已声明完整 `CompletionCapabilities` 但未注册补全处理组件。补上注册，使能力声明与实现一致。

**验收**：在 IDE 内经该 LSP 客户端可获得补全；若该客户端不支持 `CompletionItem.command`，则本项降级为「仅注册能力并记录限制」，LSP 补全的主要交付渠道仍为标准外部编辑器。

**依赖**：§7.1–§7.7；**前置是 §7.11.0 的分支判定，以及 §7.7 的插入链路可用**，否则 LSP 拿得到候选却无法正确应用。

### 7.12 B12 分层测试和产品验收

- **Analysis**：共享 component 基座和 generator，覆盖 scope、签名、可见性/适用性、导入、dangling、代码片段。
- **上游 completion**：按 `test/testResources` 验位置分类、候选转换、排序数据、指针生命周期及服务契约。
- **宿主 base**：复用 light fixture，真 PSI/项目触发，验存在/缺席/数量/顺序、BASIC/SMART、after/caret、修改失效和取消。
- **LSP**：字段映射、命令与 resolve、会话过期、新旧候选集对比。
- **产品**：仿真实 quick-fix 接线测试检查生产描述符中 contributor/service 唯一性，再实际触发与插入。**不得手工注册替身掩盖装配缺失。**[T05–T08]
- 借鉴 Kotlin `EXIST`/`ABSENT`/`NUMBER`/`NOTHING_ELSE`/`WITH_ORDER`/`INVOCATION_COUNT` 测试 DSL，**语法及期望来自仓颉**。[T09]
- 生成类不手改。未完成编辑态用恢复 fixture，**不塞进要求合法源码的普通诊断数据**。

---

## 8. 框架级缺陷登记表

以下缺陷已在实施过程中定位。**它们必须在 LL / Analysis 层修，禁止在插件侧绕过**——绕过会让上层建立在校验不过的数据上。

编号采用 `D-n` 而非 `8.n`，避免与 `tasks.md` 第 8 组（插件侧 B1/B2/B3，条目 8.1–8.7）撞号。

| 编号 | 缺陷 | 根因位置 | 状态 | 阻塞 | 验收 |
|---|---|---|---|---|---|
| **D-1** | 块内局部声明未进入解析塔快照。副作用：`scopeContext(position)` 必须额外拼接文件级查找层，否则函数体内看不到任何顶层声明 | `BodyResolveContext.storeVariable` → `CfirTowerDataContext.addLocalVariable`（`localScopes.lastOrNull()` 为空时静默丢弃） | 未修 | §6.1 公开层局部测试 → §7.4 局部候选；§6.2 代码片段 | 块内 `let`/模式绑定在补全区段中**可见**的正例断言 |
| **D-2** | 同名重载被判为同一符号。`CaCfirPublicSymbolKeyMapping` 对非局部 `CfirNamedFunctionSymbol` 只生成 `CaCfirCallableSymbolCacheKey(callableId, kind)`，而 `callableId` 不含参数列表 | `analysis/analysis-api-cfir` | 未修（**现网以「按声明 PSI 去重」绕开**） | §7.6 排序与去重、§7.5 身份 | 同名不同参数的候选**同时出现**且带各自实例化签名 |
| **D-3** | dangling 副本的 PREFER_SELF 非局部解析返回 `null`，须修在 LL 的副本失效链路上（`CfirCacheWithInvalidation.getNotNullValueForNotNullContext` 抛 Failed requirement）。**已试过两条绕法（改测试的 `originalFile` 条件、在 Analysis 层绕过模块缓存），都会让 21 个生成用例失败，已全部回退** | LL 副本失效链路 | 未修 | §6.7 副本模式验收 | 副本模式下 PREFER_SELF 非局部解析返回非 null；**若判定 LL 修复不可行，必须给出记录在案的决策而非继续绕行** |
| **D-4** | 类字段/属性不出现在成员作用域 | `CfirClassUseSiteMemberScope` 等四层作用域均未覆写 `processVariablesByName`；实际落点是 `CfirClassUseSiteMemberScope`（`CaType.scope` 落到的那一层） | **已修 + 回归锁定**（`implementation-log.md` 「字段成员缺失缺陷：已修复并加回归锁定」） | — （曾阻塞 §7.4，现解除） | 复核既有回归仍绿；把 `handover.md` §5 第 4 条「根因未最终定位」更正为已修 |
| **D-5** | `Ca` 前缀下线时机械重命名**只改了文件名、漏改类名**。①`ngJieCfirCompletionContributor.kt` 丢失首字母 `C`（内声明的是 `class CangJieCfirCompletionContributor`，只有文件名错）；②`impl-shared/test` 下 5 个文件已改名但**类名仍带 `Ca` 前缀**（`CaCompletionPositionContextTest` / `CaPsiImportPlanApplierTest` / `CaCangjieInsertionTest` / `CaInsertionValidatorTest` / `CaLookupElementFactoryTest`） | `code-insight/completion` | 未修 | §7.1 平台入口；§3.1 命名规则 | 用 `git mv` 重命名 ①；把 ② 的 5 个类名改为无层前缀（测试引用同步）；验收：**`intellij-ide` 与 `deveco` 两个生产描述符**中 contributor 均唯一且可解析，且全树无残留 `Ca` 类声明 |
| **D-6** | `CaScopeProvider` 新增成员的 `context(session)` 桥接尚未由生成器产出；目前编译通过是因为上下文接收者直接暴露接口成员 | context 桥接生成器未定位 | 未修 | §6.10 生成器 | 跑真实生成器产出桥接；手改桥接被禁止 |

**修复时的连带回归（必须一并重跑）**：

- **D-1**：现存测试把该缺陷**断言为负例**——`CfirLocalScopeCompletionSectionTest` 显式断言块内 `let` 局部变量**不可见**（`implementation-log.md:152`）。修复后**必须翻转为正例断言**，否则会被读成回归。
- **D-2**：现网绕行点是「按声明 PSI 去重」，且 `CaCfirSnapshotFactory` 抽出的 `CaSession.callableIdentityKey(symbol)` **必须与 `callableSnapshot` 内部写入身份的键同源**。身份键改为含参数列表后，这两个绕行点都会被扰动，`同名重载保留` / `memberCandidatesIncludeExtend` 等当前绿的用例**必须重跑**。
- **D-4**：`handover.md` §2 与 §5 对该缺陷状态自相矛盾（§2 说已修并有回归覆盖，§5 说根因未最终定位）。以 `implementation-log.md` 为准，并回写 handover。

**依赖图**：

```
D-1 ──▶ §6.1 局部/嵌套块公开层测试 ──▶ §7.4 局部候选
     └──▶ §6.2 代码片段
D-2 ──▶ §7.6 排序与去重（并须重跑两个绕行去重点）
D-3 ──▶ §6.7 副本模式验收
D-5 ──▶ §7.1 平台入口
D-6 ──▶ §6.10 生成器
```

---

## 9. 风险 / 权衡

| 风险 | 缓解措施 |
|---|---|
| 同名 API 被误认同语义 | 每项绑定输入、输出、实现、消费者与反例，不以 KDoc 定性 |
| 缺公共桥接被误判为缺 resolver | 复用 LL ContextCollector、receiver scopes、accessibility、extend matcher |
| 对齐返回类型破坏旧调用者 | 新增入口和显式迁移，联动生成器/基类/消费者再弃用 |
| 插入发生 alias/shadow 冲突 | 使用点复验身份，分析计划和写事务分离，失效重请求 |
| 副本或并发会话污染 | §6.7 准入、串行先行、取消传播、并行与串行等价测试 |
| **重载身份错误传导到排序与去重**（缺陷 D-2） | 列为 §7.6 前置，先修 LL 层再实现排序 |
| **局部作用域缺失传导到局部候选**（缺陷 D-1） | 列为 §7.4 前置，禁止在补全侧拼补文件级作用域掩盖 |
| .cjo/泛型/primitive 签名丢失 | 原始类型、身份和替换共同传递，源码/库同矩阵 |
| 空结果掩盖异常 | 区分不适用、不可访问、未知、取消和基础设施失败 |
| 测试替身可用但产品失败 | 生产描述符、唯一性、实际插入和两种制品分别验收 |
| **LSP 侧无头扩展点/编辑器不可用** | §7.11.0 前置最小实验先判定分支；分支 B 直接驱动管线，不经过扩展点。已记录的「轻量夹具不注册 `EditorFactory`」只覆盖测试夹具，LSP 生产容器需实测 |
| **LSP 保留活平台对象导致会话过期崩溃** | 受 §3.4 硬约束二约束：必须携带会话键与过期判定；过期返回明确错误并计数；补过期用例 |
| **两个消费者的 `CaLifetime` 失效语义不同** | §7.11.7 单独验证 `MockApplication` 上的会话失效路径，不假设与 IDE 一致；LSP 走 `workspace/applyEdit` 由客户端落盘，服务端侧只断言能力声明与命令路由（见 §7.11.7） |
| **白名单改动波及 standalone 与测试容器** | §7.11.2 枚举确切条目、登记范围例外、并补测试证明另外两个容器未获得错误的补全行为 |
| **宿主 LSP 客户端不执行 `CompletionItem.command`** | §7.11.8 实测；不执行则该渠道降级并记录，LSP 补全主渠道为标准外部编辑器 |
| **`:lsp` → completion 的 classpath 边引入循环** | §4.2 明确该边方向；completion 三模块不得反向依赖 `:lsp` |
| worktree 上游路径错误 | 显式配置并核验，不假定相邻目录可构建 |
| 停在最小 demo | 任务包含 SMART、chain、代码片段、语言特有和发布准入 |

---

## 10. 实施顺序与准入

| 阶段 | 工作 | 准入条件 |
|---|---|---|
| G0 基线与装配 | 源码指纹、接口矩阵、上游路径、模块/制品骨架、**§7.11.0 前置最小实验** | 依赖与资源可核验；分支判定已写入 `implementation-log.md`；骨架不算功能完成 |
| G1 Analysis 基础 | §6.7、§6.1/§6.3/§6.2、§6.10；**含缺陷 D-1、D-3、D-6 修复** | 局部/导入/副本/代码片段正反例；D-1 修复后块内局部变量在补全区段**可见**（负例断言已翻转）；既有 scope 不无说明扩大 |
| G2 Analysis 语义 | §6.4/§6.5/§6.6/§6.8/§6.9；**含缺陷 D-2 修复** | receiver 签名、extend、权限/shadow、期望类型、源码/.cjo 有新执行结果；**D-2 修复后两个绕行去重点（`callableIdentityKey` 与声明 PSI 去重）已回归** |
| G3 原生基础闭环 | §7.1–§7.7 串行、§7.9、§7.10 基础接线；**前置缺陷 D-5（重命名）、D-4 复核关闭** | 无 LSP 的真实产品可触发、展示、插入、撤销，验 import/caret；生产描述符中 contributor 唯一 |
| G4 完整场景 | §7.8、完整 SMART、并行准入 | 高级场景、取消、顺序和失效满足规格，不混入任意链搜索 |
| G5 LSP 与发布验证 | §7.11、§7.10/§7.12 完整矩阵 | ①LSP 与 IDE 同源候选一致；②**`resolve` 与会话过期错误路径在请求线程内完成并有正例断言；实际文本应用经 `workspace/applyEdit` 由客户端执行**（服务端不写文档，故不存在服务端写事务的 EDT 断言）；③会话过期返回明确错误；④**`intellij-ide` 与 `deveco` 两个生产描述符中 contributor 均唯一且可解析**；⑤受影响模块、分发包、承诺平台均有新验证记录 |

G0 可并行准备 §7.1 骨架。G2 的独立组件可分开实施，但先稳定公共契约。**§7.12 的测试策略属跨阶段职责**：各阶段的用例编写随对应阶段执行，G5 只做汇总矩阵，不得把测试作者工作全部堆到最后。

**验证纪律**（曾因违反此条误报结论，必须遵守）：

1. 一次只跑一个测试类；
2. 跑完读 `build/test-results/test/TEST-*.xml`，核对根节点 `tests=` / `failures=` / `errors=`；
3. **一次运行混用多个 `--tests` 过滤时，`BUILD SUCCESSFUL` 不能作为通过证据**；
4. 分析层 PSI/LightTree 双套件需 `--rerun`；golden 用 `-Dupdate.test.data=true` 重生成；
5. **Gradle 必须串行**，不得与其它会话并行写同一工作树。

---

## 11. 迁移与停用策略

新增 API 优先，保留三态 API、file scope 消费者及旧 LSP 行为直至 §7.11 验收通过；变更契约时迁移生成器、实现和调用者。

native completion 设独立准入开关，G3 验证后再考虑默认启用。SMART、并行、LSP 接入分别准入，设计编写阶段不改任何开关。

若实施后需停用，关闭新入口或停用新补全模块，保留兼容的 Analysis 增量和旧服务；**禁止用 Git 回滚命令撤销用户修改**。不直接删除旧 import plan，不向不匹配的分发插件暴露新 API。

LSP 侧停用需保证能回到现有 `AnalysisApiCangjieAnalysisFacade.completion` 实现：按 §7.11.5，旧实现迁至 `legacyCompletion` 并由独立开关保留，直至 §7.11.7 验收全部通过后再由后续变更删除。**§7.11 自身失败时也有定义状态**——关闭 LSP 侧开关即回到 legacy 路径，不需要回滚代码。

---

## 12. 未决验证项与处理规则

这些是实施时的验收项，不是等待重述需求：

1. **平台 Completion API 制品/签名**：G0 对真实目标 SDK 定向核验最小依赖，**不用新版 Kotlin 推定旧平台支持**。
2. **索引查询成本**：§6.9 测大项目与无源码依赖；不达预算则优化 provider，**不在 contributor 缓存全项目 PSI**。持久化形态按 §4.8 分层，**不得把会话内快照宣称为持久索引**。
3. **不完整调用约束**：§6.8 用合法对照和编辑态 fixture 验证；未知显式降级，**不任取重载**。
4. **宏映射、缺 SDK、受限分析**：§7.8/§7.9 分类验收；只有可靠映射才展示相应语义，未通过不能冒充完整支持。无头宿主默认无 SDK 时的行为按 §7.9 的 LSP 表执行。
5. **并行与性能阈值**：先串行基线再评估；验证前并行保持关闭。预算须写入具名测试配置文件后才可评估阶段门禁。
6. **代码片段在两类宿主中的可用性**：见 §6.2 未决项。IDE 求值窗口与调试器求值窗口是否共用同一 `CodeFragmentScopeProvider` 路径需实测；不通则该场景单独降级并记录。
7. **LSP 分支判定**：§7.11.0 的最小实验结论（A 走平台补全进程 / B 直接驱动管线）必须先写入 `implementation-log.md`。LSP 生产容器是否注册 `EditorFactory` 尚未验证——已记录的「轻量夹具不注册」只覆盖**测试夹具**，不能直接外推。
8. **LSP 客户端 `CompletionItem.command` 支持度**：宿主 LSP 客户端（`modules/ide/lsp`）是否执行 `CompletionItem.command` 需实测；不执行则 §7.11.8 降级并记录，LSP 补全主渠道仍为标准外部编辑器。
9. **D-3 可行性**：dangling 副本 PREFER_SELF 的 LL 修复若判定不可行，**必须给出记录在案的决策**，不得继续绕行。
10. **D-4 复核**：该缺陷已修（`implementation-log.md`「字段成员缺失缺陷：已修复并加回归锁定」），但 `handover.md` §5 第 4 条仍写「根因未最终定位」。G3 需复核回归仍绿，并回写 handover。
11. **变更文档计数与一致性**：`verification.md` 的需求数、场景数、任务数与「全部未勾选」表述均已失效；`proposal.md` 的能力清单缺 `lsp-completion-support`、影响面缺 `:lsp`、且仍写「LSP 与原生 IDE 补全是不同入口」——与 §2 冲突。三份文件须在任务清单同步后一并重跑/重写。
12. **上游工作树状态**：`Ca` 前缀下线的重命名与补全三模块均**只存在于工作树，未并回主检出**。所有「已存在/已完成」的断言都以工作树为准；并回主检出是独立一步，不得在文档中把工作树状态写成主检出状态。

## 13. 任务清单增量

`tasks.md` 的任务清单写于 LSP 与代码片段纳入范围之前，**必须先按本节同步，再评估任何阶段门禁**。`verification.md` 中的需求数、场景数、任务数与「全部未勾选」表述在同步后全部失效，须重跑并回写（见 §12.11）。

**G0 前置（阻塞其余一切）**

| 编号 | 内容 | 章节 |
|---|---|---|
| 1.6 | **先提交上游工作树中 `Ca` 前缀下线的 27 个重命名**（当前仅在暂存区）。未提交前，§4.4 的文件清单随时可能因 stash / 工作树重建而失效。**本项是 G0 前置，阻塞其余一切** | §3.1、§4.4 |

**新增任务**

| 编号 | 内容 | 章节 |
|---|---|---|
| 3.7 | 实现 `CodeFragmentScopeProvider.getExtraScopes`（现为 `emptyList()` 桩）；验收：表达式/类型片段各一组正反夹具 | §6.2 |
| 3.8 | **既有 `enum class CaScopeKind`**（`components/CaScopeContext.kt`）**增枚举项** `CodeFragmentMemberScope`，并增 `fragmentMemberScope` 与生成的 `context(session)` 桥接；**不改枚举形态** | §6.2 |
| 3.9 | `CompletionPositionContext` 增代码片段分支（**不新建文件**） | §6.2 |
| 3.10 | 修复缺陷 D-1（`CfirTowerDataContext.addLocalVariable` 静默丢弃），并**翻转 `CfirLocalScopeCompletionSectionTest` 的两处负例断言为正例**（`localScopeCandidates` 与 `localScopeCandidatesRespectPrefix`） | §8 D-1 |
| 5.7 | 修复缺陷 D-2（`callableIdentityKey` 纳入参数列表），并回归两个绕行去重点 | §8 D-2 |
| 6.8 | 修复缺陷 D-3（PREFER_SELF 非局部解析）**或**产出记录在案的不可行性决策 | §8 D-3 |
| 6.9 | 复核缺陷 D-4 回归仍绿，回写 `handover.md` §5 第 4 条 | §8 D-4 |
| 6.10 | 为无平台索引实现补两项前置：`analysis-api-standalone` 新增 `:analysis:stubs` 依赖；`CaStubSnapshotBuilder` / `CaStubSnapshot` 由 `internal` 放开为公开 API | §4.8 |
| 7.5 | 定位并跑真实 context 桥接生成器（缺陷 D-6），消除手改桥接 | §8 D-6 |
| 8.8 | **修复缺陷 D-5 的两处**（`Ca` 前缀下线的机械重命名漏改）：①`git mv` 重命名 `ngJieCfirCompletionContributor.kt` → `CangJieCfirCompletionContributor.kt`；②把 `impl-shared/test` 下 5 个仍带 `Ca` 前缀的**类名**改为无层前缀并同步测试引用。验收：**`intellij-ide` 与 `deveco` 两个生产描述符**中 contributor 均唯一且可解析，且 **`code-insight/completion` 树内**无残留 `Ca` 类声明（不涉及 `analysis*` 与 `:lsp`——那些层的 `Ca*` 是合法的） | §8 D-5、§5 D4 |
| 8.9 | 按设计 §3 复核既有 42 个文件的命名与包名，把「code-insight 层不用 `Ca` 前缀」「采用 `org.cangnova.cangjie.ide.completion.*`」「工具/夹具文件允许无同名类型」记为显式决策 | §3.1、§3.2、§4.7 |
| 9.10 | 新增**索引召回区段**（`CfirCompletionService.sections` 现为固定四段，无索引段），与作用域路径候选共用同一去重器 | §4.8、§7.4 |
| 15.1 | §7.11.0 前置最小实验，判定**三项**：①能否提供 `Editor` / `CompletionProcess`；②**分析跑在哪份 `CjFile` 上**（B-1 自建 `analyzeCopy` 副本，受 D-3 阻塞 / B-2 直接分析 `LspAnalysisVirtualFile`，与 IDE 语义不等价）；③`invocationCount` 与 `completionKind` 的映射规则。结论写入 `implementation-log.md` | §7.11.0 |
| 15.2 | （分支 A）`:lsp` 新增 `LspCompletionHeadlessRegistrar` 并 prime 平台扩展点 | §7.11.1 |
| 15.3 | （分支 A）上游 `allowedExtensionPointNames` 增补**枚举条目**；登记范围例外；验证 standalone/测试容器行为不变 | §7.11.2 |
| 15.4 | （分支 B）补齐前置 ①②③④（无参占位修正入口、`documentStamp`、服务注册、转换对象）；**前置 ⑤⑥ 由 15.1 的实验一并判定** | §7.11.0 |
| 15.5 | `LspCompletionItemConverter` + `LspCompletionItemKindProvider` + `LspCompletionSortingUtil`；两端呈现一致性用例 | §7.11.3 |
| 15.6 | `CangjieServerCapabilitiesFactory` 声明 `workspace.applyEdit`；`CangjieLanguageServerDescriptor.executeCommands` 加入 `applyCompletion`；`CangjieWorkspaceService.executeCommand` 由空桩改为路由 | §7.11.3 |
| 15.7 | `LspCompletionSessionStore` + `applyCompletion` / `resolve` 命令路径；会话过期错误用例 | §7.11.4 |
| 15.8 | 旧实现迁至 `legacyCompletion` 并加独立开关；**默认路径唯一** | §7.11.5 |
| 15.9 | 新旧候选集对比用例（新集合 ⊇ 旧集合；全工作区遍历不在**默认路径**中出现） | §7.11.6 |
| 15.10 | LSP 侧测试套件（`$/cancelRequest`、`isIncomplete`、import 编辑、失效路径、`applyEdit` 能力声明与命令路由） | §7.11.7 |
| 15.11 | 宿主 LSP 客户端注册补全能力；实测 `CompletionItem.command` 支持度 | §7.11.8 |
| 15.12 | **DevEco 接线**：`deveco/settings.gradle.kts` 加 substitution；`deveco/product/build.gradle.kts` 加三个模块；`deveco` 扁平 `plugin.xml` **手工追加**一条 `completion.contributor`（不能用 `xi:include`） | §4.1、§5 D4 |

**修改任务**

| 编号 | 改动 |
|---|---|
| 6.5 / 6.6 | 从「纯查询契约」升级为 §6.9 的**持久化符号索引交付物**：契约 + IDE 持久实现 + 无平台会话实现 + 前缀分级过滤 |
| 10.1 | 现状为绿，但建立在 D-2 的绕行之上。**须重开勾选**，直至 D-2 修复后两个绕行去重点回归通过 |
| **10.4** | 同属 §7.6 排序/SMART 门禁之下，也建立在 D-2 之上。**一并重开勾选** |
| 13.1 | 增加 LSP 侧分流：`$/cancelRequest`、会话过期、无 SDK 基线（不再是 IDE 专属） |
| 13.2 | 增加 resolve 阶段耗时与会话过期错误计数（不并入弹窗路径）；预算须写入具名测试配置文件后才可评估阶段门禁 |
| 14.3 | 删去「回归 Analysis 变更影响的 LSP 方法」半句（§7.11.5 已把旧实现整体迁走）；保留「LSP 未启动条件下验 native contributor」 |
| 组号顺延 | 新增第 15 组「插件侧：LSP 接入 B11」（**15.1–15.12**）；原第 15 组「后续验证命令与完成记录」顺延为第 16 组，其中补 `./gradlew :lsp:test --tests '*LspCompletionTest*'` 与 DevEco 的 `verifyPlugin`，并更正「completion 模块当前仍是拟新增」（模块目录已存在于上游工作树，主检出待补） |

**新增规格**

- `specs/analysis-completion-support/spec.md`：A1 增代码片段场景（已落地）。
- `specs/ide-completion-support/spec.md`：B11 立场修正（已落地）。
- `specs/lsp-completion-support/spec.md`：新建，含 L1–L9（已落地）。
- `verification.md`：重跑并回写计数。

**作废**

- 无整项任务作废。仅 14.3 的后半句退役。
