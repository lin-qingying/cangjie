# 仓颉快速修复框架：IDE/LSP 共享执行方案

Nov 12, 2026 · @lin

## 摘要

把 shared quick-fix factory、list、service 统一为平台 `ModCommandAction`，并为动作增加宿主无关的稳定 ID：共享 Analysis 层只负责诊断分发、动作创建和命令生成，不识别 IDE/LSP。IDE 由平台执行命令；LSP 在 `codeAction` 列表阶段预检命令并返回已生成的 WorkspaceEdit，不支持的命令不发布为可执行项。保留 `CangJieQuickFixRegistrar`、具体诊断类 `registerFactory` 和两条共享通道（`list` / `lazyList`）；删除 shared framework 中的 `IntentionBased` / `CommonIntentionAction` 双协议及 IDE 专用 `importOnTheFlyList` 占位通道，实际自动导入策略由 IDE host 负责。当前主 registrar 的 13 类有效修复是第一批迁移与验证样本，不构成未来修复的协议上限；`DeprecatedWarning` 因仓颉 `@Deprecated` 不提供 replacement 而删除 no-op 注册。

**两端同功能的首批验收范围**是 IDE 与 root LSP 都加载的 13 类 core registrar 修复。未来新增修复按共享 Analysis 契约实现；某一宿主暂不支持的命令由该宿主处理，不得把宿主判断写入共享层。`MacroUnresolved` registrar 也迁移到 ModCommandAction，但其候选 provider 位于独立 IntelliJ macro module，root LSP 当前未加载；本方案不把它列入首批两端共同能力。

**为什么这样设计**

1. **协议不自造。** 两端分叉的根因是修复持有 `PsiElement` 并绑定在 `IntentionAction` 上。平台已有“修复 = 声明式命令”的模型，自造同类协议等于重做它。
2. **阶段由平台保证。** `perform`（读，可分析）→ `psiUpdate`（文件副本上改 PSI）→ host 执行。共享 action 不负责事务或宿主调度。
3. **交互也是数据。** 平台的 `ModCommand.chooseMultipleMembers` 以 `ModEditOptions` 表达成员选择。各 host 对这类命令应用自己的交互策略；共享 action 不判断当前宿主。

**IDE host 验收**：以平台执行命令，验收撤销粒度、预览、光标位置及 lazy 触发时机。成员 chooser 的外观、分组和控件形式允许退化；默认选择、最终生成集合及取消不写文件的语义仍要核实，不据此改变共享 Analysis 契约。

**LSP host 前提**：headless 容器需提供 `ModCommandService` 和 `ModCommandExecutor`，并能对快照副本执行目标文件命令。当前和后续共享动作的命令可否转成 LSP 编辑，由 LSP host adapter 判定；服务或副本能力不满足时，不得在 Analysis 中增加特殊分支。

## 现状与根因

注册机制已经完整，分叉发生在“修复如何执行”：修复持有 PSI、绑定在 IDE 动作上，LSP 只能为成员生成另写一条路径。以下事实均来自 `lin-qingying/cangjie` 的源码（提交 `b50ebb1`）。

| # | 事实 | 位置 | 后果 |
| --- | --- | --- | --- |
| 1 | 当前主注册器有 12 个普通 factory、2 个 lazy；`UseExprWithoutImport` 还重复登记在 IDE import-on-the-fly 通道，共覆盖 14 类诊断 | `code-insight/code-actions/.../CangJieQuickFixRegistrar.kt` | 诊断到 factory 的基础映射已具备；本次改执行契约，并将 IDE 专用 import-on-the-fly 标记移出共享 service |
| 2 | core factory 当前通过 `IntentionBased` 返回 `IntentionAction`，service list 再规范化为 intention；旧 action 持有 `PsiElement` 并在 `invoke` 中写入 | `CangJieQuickFixFactory.kt`、`CangJieQuickFixesList.kt`、`QuickFixes.kt`、`ImportQuickFixes.kt`、`DeclarationQuickFixes.kt`、`RemoveModifierQuickFix.kt` | LSP 不能复用活动 PSI；shared factory/list/service 改用 ModCommandAction |
| 3 | LSP 只放行 2 类成员诊断；`resolve` 写死成员生成，不看是哪个动作 | `lsp/.../AnalysisApiLspCodeActionSupport.kt` | 其他修复在 LSP 不可用；同诊断的多个候选得到同一份编辑 |
| 4 | IDE 的 `GenerateMembersHandler.generateMembers` 与 `CangJieMemberGenerationServiceImpl.generateMembers` 都创建声明并调用 `addDeclaration`；IDE 路径另有写事务、整文件 reformat 和光标处理 | `code-insight/code-actions/.../overrideImplement/` | 可抽取共同 PSI 插入步骤；保留两种入口各自的事务、格式化和光标契约 |
| 5 | 线程模型是刻意分阶段的：IDE 在进度窗口收集、写事务里插入；LSP 在 EDT 之外收集，只把插入放进 `invokeAndWait`。源码注释记录了在后台线程进入带进度窗口的收集入口会死锁 | `CangJieMemberGenerationServiceImpl.kt`、`AnalysisApiLspCodeActionSupport.kt` | 任何把“分析”与“写入”合进同一阶段的设计都会回归 |
| 6 | 当前 `CangJieQuickFixFactory` 有 `IntentionBased` / `ModCommandBased` 双形态，list/service 再把结果转成 intention | `code-insight/api/.../CangJieQuickFixFactory.kt` | shared framework 需要单一 ModCommandAction 契约，不能把 Kotlin 的双协议作为迁移目标 |
| 7 | 官方 `@Deprecated` 参数只有 `message`、`since`、`strict`，没有 Kotlin `ReplaceWith`；当前 P1 fixture 的 `replacement` 为空且 factory 产出空列表 | 官方语言手册 `@Deprecated` 参数；`P1QuickFixesTest.kt` | 删除不可能触发的 `DeprecatedWarning` quick-fix factory 注册；保留诊断本身 |
| 8 | `docs/quickfix-gap-analysis.md` 称“只有 1 个 factory”，与源码不符 | 该文档 | 已过期，会误导后续工作 |
| 9 | `intellij-ide/`、`deveco/`、宏修复和 Kotlin 参考源码都在当前工作区 | `intellij-ide/modules/ide/base/.../CangJieDiagnosticHighlightVisitor.kt`、`intellij-ide/modules/ide/macro/...`、`deveco/modules/ide/lsp/.../CangJieLSPDiagnosticFeature.kt`、`external/kotlin*` | 必须按真实源码界定改动与排除项；不能以“文件不可见”为由留待用户贴代码 |
| 10 | IDE 产品和 root LSP 都加载主 fixes descriptor；root LSP 不加载宏 descriptor | `AnalysisApiLspServiceRegistrar.kt`、`intellij-ide/product/idea-plugin/src/test/.../CangJieQuickFixRegistrationTest.kt`、`intellij-ide/modules/ide/macro/src/main/resources/META-INF/cangjie-macro.xml` | 13 类 core 修复在两端共同加载；`MacroUnresolved` 另有 IDE 候选 provider，不属于本次 LSP action set |

## 目标与约束

方案约束如下，其余设计要求都是它们的推论。

**硬约束**

- 保留 `CangJieQuickFixRegistrar`，仍用诊断定义的类来注册。
- 框架级共享：不要补丁，不要兼容层。
- IDE 关键行为需保持：撤销粒度、预览、光标和 lazy 时机；成员 chooser 可改用平台通用 UI，并如实说明交互差异。
- 代码要读得懂：上一版“读相/写相”自造接口被否决。
- 共享 Analysis 层禁止出现 IDE/LSP 特判：不依赖 host 类型、不读 headless 标志、不按宿主分支、不声明某修复“仅 IDE/LSP 可用”。宿主能力只能在各自 host adapter 内判断。

**由此推出的设计要求**

1. 修复逻辑只写一份，两个宿主执行的是同一份。
2. “分析 / 改 PSI / 落地”的阶段边界由平台保证，不自造。
3. 共享层只产出标准 `ModCommandAction`；IDE/LSP 选择、能力判断、执行与协议转换分别留在 host。
4. LSP host 必须在返回可执行 CodeAction 前确认命令能被转换；不支持的动作不得以普通可执行项展示。
5. 不变量放在测试期：覆盖共享动作身份、每种命令的 host 消费规则、注册一致性及共享层无 host 特判。
6. 按 `CLAUDE.md` 做框架级设计；对照 Kotlin 的命令语义，shared quick-fix 执行契约采用单一 ModCommandAction。

## 分层职责

| 层 | 负责 | 禁止 |
| --- | --- | --- |
| 共享 Analysis quick-fix 层（`code-insight:api` / `code-insight:code-actions`） | 诊断到 factory/action 的分发；在有效 `CaSession` 内构造动作；动作的 `perform` 只读分析并产出 `ModCommand`；提供与展示文案无关的稳定动作身份 | 引用 `org.cangnova.cangjie.lsp` 或 `intellij-ide`；检测 IDE/headless；按 host 选择不同动作/命令；把 LSP WorkspaceEdit 或 IDE Editor/WriteAction 放入修复逻辑 |
| IntelliJ IDE host | 把共享 action 注册到高亮、lazy 和其他 IDE 入口；由平台展示交互并执行命令；维护 IDE 撤销、预览、光标和写入契约 | 在 host 中重写修复语义或另造一份 PSI 变更 |
| LSP host | 调用同一 action；判断并执行当前 LSP 能消费的命令；将结果转换为带文档版本的 WorkspaceEdit；对不支持的命令不发布可执行 CodeAction | 修改共享 action 以绕开 headless 限制；把 LSP 编辑协议泄漏进共享 Analysis |

这里禁止的是宿主分支和宿主依赖，不是要求共享层脱离 IntelliJ API。共享 action 有意使用 IntelliJ PSI 与 `ModCommandAction`；它不询问自己当前由哪个 host 执行。

这里的“共享”指修复逻辑与命令产物共用，不代表两个 host 具有相同交互能力，也不代表每一种 IntelliJ 命令都能直接编码成 LSP WorkspaceEdit。后续要让某类命令进入 LSP，只扩展 LSP host adapter 及其测试，不给 Analysis 增加 host 分支。

## 选型：为什么是 ModCommand

选 ModCommand，因为平台已提供“分析动作产生命令、宿主消费命令”的抽象。当前 shared factory/list/service 以 `IntentionAction` 为中心，LSP 只能重写动作逻辑。本次把 shared contract 本身切到 `ModCommandAction`，不保留 intention-only factory 分支；框架以稳定契约服务后续修复，当前有效修复集只是迁移基线，`DeprecatedWarning` no-op 注册移除。

| 方案 | 做法 | 优点 | 问题 | 结论 |
| --- | --- | --- | --- | --- |
| A | 保留 `IntentionAction`，LSP 逐类另写实现 | 改动最小 | 修复逻辑两份，永远会分叉（现状） | 否 |
| B | 自造描述 + 目标 + 应用器 + 读写两相协议（上一版） | 完全可控 | 重做 ModCommand：预览用的文件副本、撤销分组、chooser 抽象、光标都要自己做和维护；两相接口难读；与 K2 结构不一致 | 否 |
| C | 在活动 PSI 上直接改，LSP 对快照改后求 diff | 不依赖平台命令 | IDE 丢预览与批量应用；LSP 写 PSI 需要 EDT 调度和 undo-transparent 包装 | 否 |
| D | shared fix = `ModCommandAction` | factory/list/service 只传递平台动作；IDE 使用平台 executor；LSP 由 host adapter 预检并消费可支持的命令 | API 与所有 registrar/host consumer 需一次切换；LSP 的执行服务和能力范围须先实测；IDE 交互行为需验收 | **采用，有门槛** |

**为什么否决上一版（B）**：它的 `prepare` / `commit` 两相就是 ModCommand 的 `perform` / `psiUpdate` 的拙劣复制。自造意味着每次平台升级都要自己跟进预览、撤销和选择交互，而这些正是 ModCommand 设计来解决的问题。

**Kotlin 对照范围**：本地 [`KotlinQuickFixFactory.kt`](../external/kotlin-plugin/code-insight/api/src/org/jetbrains/kotlin/idea/codeinsight/api/applicators/fixes/KotlinQuickFixFactory.kt) / [`KotlinQuickFixesList.kt`](../external/kotlin-plugin/code-insight/api/src/org/jetbrains/kotlin/idea/codeinsight/api/applicators/fixes/KotlinQuickFixesList.kt) 保留双形态 factory 并在 list 层适配。本方案只采用一个共享动作协议：`ModCommandAction`；IDE/LSP 对命令的消费不进入 Analysis API。官方参考：[IntelliJ ModCommand API](https://github.com/JetBrains/intellij-community/blob/master/platform/analysis-api/src/com/intellij/modcommand/ModCommand.java)。

## 架构

本次 shared quick fixes 由 `ModCommandAction` 实现，`perform` 产生命令。两个 host 使用同一 registrar/factory/action 实现；host 只负责消费命令。一条修复从诊断到落地经过四个阶段：

&#91;embedded content: 修复执行流程 · 4 个阶段，2 个宿主\]

上图是全景：动作产出的命令是唯一的共享物，两个宿主各自执行它。下表逐阶段列出允许与禁止的事项。

| 阶段 | 运行位置 | 允许 | 禁止 | 为什么 |
| --- | --- | --- | --- | --- |
| factory | `CaSession` 内，读 | 分析诊断、决定是否有修复、创建动作 | 改 PSI；检查 host | registrar 负责创建时机与诊断映射 |
| `perform` | 平台规定的后台读动作，原始 PSI | 分析并返回命令 | 改活动 PSI、弹 UI、检查 host | 修复语义只在共享 action 中实现 |
| `psiUpdate` | 平台创建的文件副本 | 纯 PSI 更新，经 `ModPsiUpdater` | 分析、弹 UI、开事务、访问 Editor | 平台跟踪副本变更并形成命令 |
| host 消费 | IDE 或 LSP 自己的宿主 | 按自身协议呈现/执行命令 | 修改共享 action 来适配另一 host | 所有差异在 host 边界消化 |

**为什么这样设计**

1. **协议用平台的。** 共享 API 传递 `ModCommandAction`，不声明 IDE/LSP 子协议。
2. **阶段由平台保证。** 分析、PSI 副本修改、宿主执行各有明确阶段。
3. **宿主各自消费。** IDE 使用平台 executor；LSP 在自己的 adapter 中测试命令是否可转成版本化编辑。
4. **未来能力可扩展。** 新增命令支持只改相应 host adapter 和测试，不改 Analysis action 逻辑。
5. **共享注册与通道无 host 语义。** Analysis 只保留普通与 lazy 两条动作创建通道；IDE 的 import-on-the-fly 标记由 IDE host 自己处理。

## 分层设计

注册入口和具体诊断键保持不变；factory/list/service 返回类型原子切换为 `CangJieQuickFixAction`，它只在 `ModCommandAction` 上增加宿主无关的稳定动作 ID。共享 service 保留 `list` / `lazyList`，移除 IDE 专用的 `importOnTheFlyList` 与相关查询。ModCommand API 需同时核对 root SDK `253.29346.379` 与 IDE 运行目标 `253.29346.50`。

### Shared Analysis 层

### 6.1 factory 协议：shared 层只产出 ModCommandAction

`CangJieQuickFixFactory` 只定义一种执行协议。稳定 ID 是通用动作身份元数据，不带 IDE/LSP 语义：

```kotlin
fun interface CangJieQuickFixFactory<D : CaDiagnosticWithPsi<*>> {
    /** 同一诊断有多个 factory 时，值小的排前面。 */
    val priority: Int get() = DEFAULT_PRIORITY

    /** 在 CaSession 内读取诊断并创建平台命令动作，不修改 PSI。 */
    fun CaSession.createQuickFixes(diagnostic: D): List<CangJieQuickFixAction>

    companion object {
        const val DEFAULT_PRIORITY: Int = 100
    }
}
```

删除 `IntentionBased`、`ModCommandBased`、`CommonIntentionAction` 分支。`CangJieQuickFixesList`、`CangJieQuickFixService` 的普通和 lazy 查询返回 `CangJieQuickFixAction`；共享层不调用 `asIntention()` 或 `asModCommandAction()`，也不暴露 import-on-the-fly 查询。ID 不含 host 字段，不改变动作执行协议。所有共享 registrar 都必须符合这一类型。

**为什么：** 每个注册修复只有一种可执行表示。两个 host 拿到同一动作和稳定身份；展示名、family name 只用于 UI，不承担机器路由。

动作身份接口只增加宿主无关的 ID：

```kotlin
interface CangJieQuickFixAction : ModCommandAction {
    val actionId: String
}
```

### 6.2 修复类的写法

```kotlin
internal class RemoveModifierFix(
    owner: CjModifierListOwner,
    private val modifier: CjKeywordToken,
) : PsiUpdateModCommandAction<CjModifierListOwner>(owner), CangJieQuickFixAction {

    override val actionId: String = "remove-modifier.${modifier.value}"

    override fun getFamilyName() = "CangJie modifier quick fixes"

    override fun getPresentation(context: ActionContext, element: CjModifierListOwner) =
        Presentation.of("Remove modifier '${modifier.value}'")

    override fun invoke(context: ActionContext, element: CjModifierListOwner, updater: ModPsiUpdater) =
        removeModifier(element, modifier)
}

object ModifierQuickFixFactories {
    val repeatedModifier: CangJieQuickFixFactory<CaCfirDiagnostic.RepeatedModifier> =
        CangJieQuickFixFactory { diagnostic ->
            listOfNotNull(removeModifierFix(diagnostic.psi, diagnostic.modifier))
        }

    val incompatibleModifiers: CangJieQuickFixFactory<CaCfirDiagnostic.IncompatibleModifiers> =
        CangJieQuickFixFactory { diagnostic ->
            listOfNotNull(
                removeModifierFix(diagnostic.psi, diagnostic.modifier1),
                removeModifierFix(diagnostic.psi, diagnostic.modifier2),
            )
        }

    private fun removeModifierFix(at: PsiElement, modifier: CjKeywordToken): CangJieQuickFixAction? =
        at.getNonStrictParentOfType<CjModifierListOwner>()?.let { RemoveModifierFix(it, modifier) }
}
```

该示例只展示共享 action 如何从 PSI 目标生成命令；稳定 ID 不承载 host 能力。具体代码不得引用 LSP/IDE host 类型或执行入口。

与旧 intention 相比，`PsiUpdateModCommandAction` 去掉手写的 `WriteCommandAction`；元素有效性、文件副本和事务交给平台执行器。

**共享 action 规则：**

1. 所有 registrar 修复都实现 `CangJieQuickFixAction : ModCommandAction`；单步 PSI 更新继承 `PsiUpdateModCommandAction<E>`，组合分析与命令时使用 `PsiBasedModCommandAction<E>`。
2. `invoke` / `psiUpdate` 只做副本 PSI 修改，不分析、不弹 UI、不开事务、不接触 `Editor`。
3. 语义分析只在 factory 的 `CaSession` 和 `perform` 中进行；不把跨 session 的 symbol 或 analysis session 保存到 action。
4. 光标、选择、导航等副作用用 ModCommand 数据表达；action 内不得根据宿主修改命令。
5. `actionId` 稳定且同一诊断下唯一；family/presentation 只用于展示。
6. action 只通过构造参数持有目标 PSI 元素，不持有其他 PSI。

### 6.3 修复的身份：稳定 actionId

每个共享动作提供非本地化 `actionId`。它在同一诊断的所有不同语义动作中唯一；同一 factory/action 可因注册到多个通道而重复出现。改展示文案或调整 priority 不改变身份。一个 factory 若针对不同语义目标返回多个动作，ID 必须包含稳定的目标区分值，不能用列表位置。

**身份与去重键分开：** 共享语义身份为 `(diagnosticClass, actionId)`，不同语义动作在同一诊断类型下必须有不同 ID；同一 action 因注册在普通和 lazy 通道而重复时允许复用 ID。LSP 单次请求的诊断实例去重键为 `(diagnosticClass, exactPrimaryRange, actionId)`，不同范围的同类诊断不会合并。共享层只提供稳定动作身份，不持有协议 payload 或跨请求状态。

### 6.4 服务与注册：保持项与变化项

| 保持不变 | 变化 |
| --- | --- |
| `CangJieQuickFixRegistrar`、扩展点 `org.cangnova.cangjie.codeinsight.quickfix.registrar`、具体诊断类注册 | factory 只返回 `CangJieQuickFixAction : ModCommandAction`；删除 `IntentionBased` / `CommonIntentionAction` shared 分支 |
| `list` / `lazyList` 两条共享通道 | 移除 shared registrar/service 的 `importOnTheFlyList` 与专用查询；该消费时机归 IDE host |
| `priority` 排序、`WithCatching` 单个 factory 失败隔离、`PCE` 向上传播 | 移除 `getAvailableQuickFixesWithCatchingFor` 这类合并协议客户端通道的入口；host 自行组合需要的查询结果 |

factory/list/service 只管理注册和普通/lazy 动作创建时机；service 不判断消费方。IDE host 如需 import-on-the-fly 标记，由自己的 adapter 根据稳定 action 身份处理，不向共享 service 添加 host-specific 查询。

### IDE host（6.5）：平台执行与行为验收

高亮 visitor 实际位于 `intellij-ide/modules/ide/base/.../CangJieDiagnosticHighlightVisitor.kt`。普通修复在 `HighlightInfo.Builder.registerFix(ModCommandAction, ...)` 入口直接注册；lazy registrar 若目标 SDK 仍只接受 `IntentionAction`，仅在该 IDE host 边界调用 `action.asIntention()`。shared service 不做转换。需用目标 SDK 核对两个签名，并验收撤销和预览。

当前 IntelliJ visitor 的 import-on-the-fly 查询只是占位，不产生额外 UI。移除 shared 查询时一并删除该 marker 接线；未来真正接入平台自动导入时，只在 IntelliJ/DevEco host adapter 处理。

DevEco 是独立 IDE host。当前 `DevecoCangJieDiagnosticHighlightVisitor` 只生成高亮、没有 quick-fix 接线；若方案承诺 DevEco 也提供共享修复，必须在该 visitor 接入相同 service/action 的普通与 lazy 注册，并在 DevEco 实际运行目标上验收。仅构建通过不能证明 IDE 修复可用。

| 能力 | 新实现 | 状态 |
| --- | --- | --- |
| 撤销 | 平台把文本更新放进一个 command | 命令名与撤销粒度待验收 |
| 预览 | 平台根据命令生成 | chooser 命令的预览待验收 |
| 光标 | 用 `updater.moveCaretTo` | 待验收 |
| 重计算 | 保留 lazy 通道，由平台后台执行 `perform` | Alt+Enter 延迟待验收 |
| 批量应用 | 平台 `executeInBatch` 消费默认选项 | 验证 IDE host 所需入口 |

**关于撤销粒度：** 平台执行 ModCommand 的交互路径与当前 `WriteCommandAction` 路径不同。预期一次修复形成一个 undo command；撤销分组、命令名和预览必须由 R8 手工验收。

### LSP host（6.6）：命令执行器

放在 `lsp/src/org/cangnova/cangjie/lsp/analysis`。LSP host 调用平台 `ModCommandExecutor.executeForFileCopy`，并从非物理 PSI 副本生成 WorkspaceEdit；不自行重做 PSI 命令执行。每个 action 使用独立副本，副本需保留命令记录的源 VirtualFile 身份。当前 PSI 快照可能按文档版本复用，禁止把缓存中的 `snapshot.psiFile` 直接交给会修改文档的 executor。目标版本如何构造这种副本由 R3 探针确认。

LSP host 对交互命令使用单独策略：

- `ModEditOptions` 不直接交给文件副本 executor。只有 `canUseDefaults` 为真时，LSP host 才在平台读动作中创建默认 option container、调用 `nextCommand(container)` 并递归分类；否则本次请求不返回该 action。
- `ModNavigate`、`ModHighlight` 不进入 WorkspaceEdit；当它们与文件更新组合时只丢弃宿主不可表达的 UI 效果。
- `ModCompositeCommand` 按原顺序拆成 leaf command；执行前先完整分类，存在一个不支持的子命令就拒绝整个 action；可支持的文本更新在同一副本上按序交给 executor，逐步校验 `oldText`。
- 修改目标必须是本次快照对应的文档；executor 在每个 action 独占的 disposable file copy 上应用文本命令并产出最终 PSI 文本。
- 文件创建、删除、移动、跨文件引用更新、模板和其它 UI 命令，在 LSP adapter 支持前都不作为可执行 action 返回。

平台 `executeForFileCopy` 会直接改写并提交副本文档，不负责替 host 获取写访问。LSP adapter 必须在 SDK 要求的写访问上下文执行该步骤（若要求 EDT，则在 EDT）；该写访问只覆盖 disposable copy，不创建或修改活动文档的写命令。

| 命令类型 | 首批 LSP host 行为 |
| --- | --- |
| `ModNothing` | 不返回 CodeAction |
| `ModUpdateFileText` | 仅当目标 VirtualFile 与当前文档快照对应时交给平台文件副本 executor；executor 校验 `oldText` |
| `ModCompositeCommand` | 先递归分类全部子命令；全可支持时按原顺序逐个执行文本更新 |
| `ModEditOptions` | `canUseDefaults` 为真时由 LSP host 在平台读动作中调用 `nextCommand(containerSupplier.get())` 并递归分类；否则不返回 action |
| `ModNavigate` / `ModHighlight` | 丢弃非文本 UI 效果；命令无文本修改时不返回 action |
| 其它 sealed subtype | 首批不支持，过滤 action；未来要支持时只扩展 LSP host adapter 和测试 |

这里的支持集合属于 LSP host，不是共享 Analysis 的修复白名单。LSP adapter 必须对每个目标 SDK 的 sealed command subtype 明确支持/拒绝；不能用静默空编辑伪装成功。先分类完整命令树，再改动副本，避免一个 composite 前半段可写、后半段 Unsupported 时留下部分结果。若 headless 运行时拿不到平台 executor，本方案在此处阻断，不自行复制平台 PSI executor。

首批 LSP adapter 只承诺能应用到当前文档副本的命令。未来修复若需要多文档编辑，扩展点是 LSP host：为参与编辑的文档构造独立快照，明确跨文件 leaf command 的顺序/依赖，再生成多个版本化 `TextDocumentEdit`。不能把多文件 composite 原样交给单文件 `executeForFileCopy`。支持该命令前，LSP host 过滤它；共享 action 仍由 IDE 正常执行。

### LSP host（6.7）：预检后返回可执行编辑

`textDocument/codeAction` 仅处理与请求范围重叠的诊断，分为两个阶段：

```
分析阶段（在 CaSession / 读上下文内）：
  诊断与请求范围重叠
    → LSP host 分别查询 service 的 list 与 lazyList
    → 以 (diagnosticClass, exact primary range, actionId) 去重同一语义动作
    → ActionContext 指向当前快照和诊断主范围
    → getPresentation(context) 为 null 时跳过；只对非 null presentation 调用 perform(context)
    → 收集 actionId、presentation、ModCommand、源 VirtualFile、旧文本和文档版本

执行阶段（CaSession 已退出）：
  对每个待返回 action 单独创建 disposable PSI file copy
    → LSP adapter 预分类完整命令树
    → 在 SDK 要求的写访问上下文执行文件副本命令，不改活动文档
    → 支持且产生文本变化：diff 副本文本，生成带版本的 WorkspaceEdit 并返回 CodeAction
    → 无变化 / Unsupported / 副本目标不匹配：不返回 action，并记录原因
```

共享 quick-fix 在列表阶段直接生成编辑，不经过二次 `codeAction/resolve`：这样 unsupported 命令不会先显示成可执行 action，动作也不需要依赖展示名或跨请求重定位。LSP 的其它功能如需 resolve，可继续使用自己的协议流程。

单个 action 计算失败时记录诊断并省略该 action；其它候选继续处理。取消异常按 IntelliJ 约定向上传播。执行器返回 Unsupported 或无文本变化时不生成 CodeAction。

客户端支持版本化编辑时返回 `WorkspaceEdit.documentChanges` 和当前文档版本。若客户端只支持 `WorkspaceEdit.changes`，协议没有 apply-time 版本校验；服务端只能保证编辑按生成时快照计算，客户端之后若改过文档仍可能把旧编辑套到新文本。一次 codeAction 请求仅在请求范围内执行候选，不在后台全文件扫描/执行修复。

此选择会在用户请求 codeAction 时立即计算范围内的候选命令和编辑；重型 action 可能增加请求延迟。LSP request cancellation 必须终止当前 action 计算。若实际延迟不满足 IDE 交互要求，只在 LSP host 引入延迟/缓存执行策略，并保持共享 Analysis 不变。
### Shared Analysis 示例（6.8）：成员生成 action 与插入逻辑

`collectUnimplementedMemberChooserItems` 在一个 `CaSession` 内对每个缺失 symbol 同时构造生成草稿和 chooser 展示文本：草稿复用 `CangJieMemberGenerationServiceImpl` 当前的 symbol-to-member 渲染逻辑，展示文本复用当前 `CangJieOverrideMemberChooserObject` 的完整签名文本。Chooser item 只携带 `CangJieGeneratableMember` 与显示文本，不把 symbol pointer 带出 session，也不只用名称显示重载成员。

```kotlin
import com.intellij.codeInspection.options.OptMultiSelector

internal class ImplementMembersFix(type: CjTypeStatement) :
    PsiBasedModCommandAction<CjTypeStatement>(type), CangJieQuickFixAction {

    override val actionId: String = "implement-members"

    override fun getFamilyName() = CangJieOverrideImplementBundle.message("implement.members.handler.family")

    override fun getPresentation(context: ActionContext, element: CjTypeStatement) =
        Presentation.of(familyName)

    // 读阶段：平台在后台线程调用，可以分析
    override fun perform(context: ActionContext, type: CjTypeStatement): ModCommand {
        val chooserItems = analyze(type) { collectUnimplementedMemberChooserItems(type) }
        if (chooserItems.isEmpty()) return ModCommand.nop()

        return ModCommand.chooseMultipleMembers(chooserTitle, chooserItems) { chosen ->
            val selectedMembers = chosen.map { (it as MemberDraftChooserObject).draft }
            ModCommand.psiUpdate(type) { writable, updater ->           // 副本上只做语法修改
                val last = insertMembers(writable, selectedMembers)
                CodeStyleManager.getInstance(writable.project).reformat(writable.containingFile)
                last?.let(updater::moveCaretTo)
            }
        }
    }
}

/** ModCommand 多选项中的显示对象；实际选择由 option container 保存。 */
private class MemberDraftChooserObject(
    val draft: CangJieGeneratableMember,
    private val displayText: String,
) : OptMultiSelector.OptElement {
    override fun getText(): String = displayText
}

/** 三处共用的插入例程：只插入，不开启事务或排序成员。返回最后插入的声明。 */
internal fun insertMembers(type: CjTypeStatement, members: List<CangJieGeneratableMember>): PsiElement? {
    val factory = CjPsiFactory(type.project)
    return members.map { member ->
        type.addDeclaration(
            when (member.kind) {
                CangJieGeneratableMember.Kind.FUNCTION -> factory.createFunction(member.renderedText)
                CangJieGeneratableMember.Kind.PROPERTY -> factory.createProperty(member.renderedText)
            }
        )
    }.lastOrNull()
}
```

`insertMembers` 的调用者包括共享 quick-fix、`GenerateMembersHandler.generateMembers`（Code → Generate 菜单，IDE-only 入口，保留自己的 `WriteCommandAction`、当前插入位置和光标）及 `CangJieMemberGenerationServiceImpl.generateMembers`（补全窄接口，不开事务、不排序成员）。IDE handler 保留整文件格式化；调用者的事务和光标策略不进入共享 action。

**共享语义：** chooser item 实现 IntelliJ `OptMultiSelector.OptElement`，携带完整展示文本和渲染好的 `CangJieGeneratableMember` 草稿；共享 action 不保存 symbol pointer，不根据 host 改变候选或命令。`perform` 每次重新分析目标类型并收集当前缺失成员。候选选择策略由各 host 的 ModCommand consumer 执行。

## 注册点迁移

13 类有效诊断收敛为 5 个修复类；另外删除不产出动作的 `DeprecatedWarning` factory 注册。这只是第一批迁移内容；factory 返回 `CangJieQuickFixAction`，稳定 `actionId` 与展示文案分离。

| 诊断 | 新修复类 | 通道 | 修复体（沿用现有实现） | 展示名 |
| --- | --- | --- | --- | --- |
| `RepeatedModifier`、`RedundantModifier`、`WrongModifierTarget`、`WrongModifierContainingDeclaration`、`RedundantModifierForTarget`、`DeprecatedModifierForTarget` | `RemoveModifierFix` | list | `psiUtil.removeModifier(owner, token)` | `Remove modifier 'open'` |
| `IncompatibleModifiers` | `RemoveModifierFix` × 2 | list | 同上，两个互斥修饰符各一条 | 两条名字不同 |
| `UseExprWithoutImport` | `AddImportFix(file, path)` | list | 在 `psiUpdate` 内调用 `PsiImportPlanApplier(project).apply(file, AddImport)`；已存在等价 import 时不产生文本变化 | `Add import 'a.b'` |
| `UnusedImport` | `RemoveImportFix` | list | `importItem.delete()` | `Remove unused import` |
| `UnresolvedImport` | `RemoveImportFix` | list | 对 `CjImportItem` 或 `CjImportDirective` 调用 `delete()` | `Remove unresolved import` |
| `NonExhaustiveMatch` | `AddMatchBranchFix(match, case)` | list | `createMatchEntry` + `addAfter` + 补换行；`updater.moveCaretTo(分支体)` | `Add match branch for 'A'` |
| `AbstractMemberNotImplemented`、`InterfaceMemberMustBeImplemented` | `ImplementMembersFix(type)` | lazy | 见 6.8 | family 名 |
| `DeprecatedWarning` | 删除 no-op factory 注册 | — | 官方 `@Deprecated` 只定义 `message`、`since`、`strict`；当前 fixture 也验证空 replacement 不产出修复 | — |

**合并两个重复类：** 现有的 `RemoveImportQuickFix`（未使用）与 `RemoveUnresolvedImportQuickFix`（未解析）逻辑相同，只是标题不同，合并为一个 `RemoveImportFix(entry, label)`。

**等价性检查：** 迁移后每个修复的修改结果应与迁移前一致。这一点目前的测试只断言“存在”和标题，所以金标测试（第 9 节）应先在旧实现上写出期望结果，再迁移。

## 文件级改动清单

这是 shared API 类型切换，必须连同所有 registrar 和 host consumer 一起迁移；不保留 IntentionAction 回退分支或并行列表。shared Analysis 只新增稳定动作身份，不增加 IDE/LSP 能力字段或分支。

| 模块 | 动作 | 文件 | 说明 |
| --- | --- | --- | --- |
| `code-insight/api` | 改写 | 新增 `CangJieQuickFixAction.kt`；改写 `CangJieQuickFixFactory.kt`、`CangJieQuickFixesList.kt`、`CangJieQuickFixService.kt` | factory/list/service 返回 `CangJieQuickFixAction : ModCommandAction`；删除 intention-only 协议、shared 层 action 转换及 `getAvailableQuickFixesWithCatchingFor` 宿主聚合 API |
| `code-insight/code-actions` | 改写 | `CangJieQuickFixRegistrar.kt`、第一批有效目标 factory | 生成带稳定 `actionId` 的动作；移除 `DeprecatedWarning` 注册 |
|  | 改写 | `RemoveModifierQuickFix.kt` → `RemoveModifierFix.kt` | 第 6.2 节 |
|  | 改写 | `ImportQuickFixes.kt`、`DeclarationQuickFixes.kt` | `AddImportFix`、`RemoveImportFix`（合并）、`AddMatchBranchFix`；删除 `ReplaceWithQuickFix` |
|  | 改写 | `ImplementMembersHandler.kt` | `ImplementMembersQuickfix` → `ImplementMembersFix`；提供 chooser 草稿收集并复用共同的 symbol-to-member 渲染 helper |
|  | 改写 | `GenerateMembersHandler.kt` | 插入改调 `insertMembers`；保留现有 `MemberChooser` 显示模型 |
|  | 改写 | `CangJieMemberGenerationServiceImpl.kt` | 成员渲染委托共享 symbol-to-member helper；`generateMembers` 委托 `insertMembers` |
|  | 新增 | `overrideImplement/MemberInsertion.kt` | 共享 symbol-to-member 渲染 helper 与 `insertMembers` |
| `lsp` | 新增 | `analysis/LspModCommandExecutionAdapter.kt` | 第 6.6 节；在 disposable PSI 文件副本上调用平台 executor，处理默认选项并分类不支持命令 |
|  | 改写 | `AnalysisApiLspCodeActionSupport.kt` | 第 6.7 节；列表阶段预检并返回已带 WorkspaceEdit 的 CodeAction；共享 quick-fix 不再走 resolve round-trip |
|  | 视探针结果改 | `AnalysisApiLspServiceRegistrar.kt` | 在 headless host 装配 `ModCommandService` / `ModCommandExecutor`；只改 LSP host |
| 测试 | 改写 | `QuickFixTestSupport.kt`、`QuickFixTestSupportTest.kt`、`P1QuickFixesTest.kt`、`ImportQuickFixesTest.kt`、`CangJieQuickFixRegistrationTest.kt`、`CjMacroCallQuickFixTest.kt` | 断言稳定 actionId、展示名与标准命令结果；`DeprecatedWarning` 通过 registrar 断言无修复 |
|  | 改写 | `QuickFixCoverageMatrixTest.kt` | 删除 `DeprecatedWarning` 承诺项和 `importOnTheFlyList` 反射项；验证 shared API 仅保留 `list` / `lazyList` |
|  | 改写 | `CangjieCodeActionIntegrationTest.kt`、`CangjieCodeActionFailureRetentionTest.kt`、`AnalysisApiLspCodeActionDeduplicatorTest.kt` | 验证列表阶段只返回可执行编辑、Unsupported 不曝光、稳定 ID 去重与文档版本 |
| 文档 | 改写 | `docs/quickfix-gap-analysis.md` | 重写现状部分（已过期） |
|  | 新增 | `docs/quickfix-framework.md` | 共享 action 规则、stable actionId、host 消费边界与命令能力矩阵 |
| `intellij-ide` | 改写 | `CangJieDiagnosticHighlightVisitor.kt` 的普通/lazy 注册 | 普通修复直接注册 ModCommandAction；仅当 SDK lazy registrar 要求 IntentionAction 时，在该 IDE host 边界包装；移除对 shared import-on-the-fly 查询的占位接线 |
| `intellij-ide/modules/ide/macro` | 改写 | `MacroQuickFixRegistrar.kt`、`MacroCallQuickFixes.kt` | 让 shared registrar 的补 import/改名 action 改为 ModCommandAction；继续复用候选发现和 `MacroImportEditor` 的 PSI 操作 |
| LSP 宏范围 | 明确限制 | `AnalysisApiLspServiceRegistrar.quickFixPluginXmls` | root LSP 当前不加载 IntelliJ macro plugin descriptor，故 `MacroUnresolved` 不属于本次两端共同加载的 action 集；共享宏支持需另行迁移候选 provider 并纳入 LSP classpath |
| DevEco host | 改写并验收 | `DevecoCangJieDiagnosticHighlightVisitor.kt`、独立产品 descriptor | 接入共享 action 的普通/lazy 注册；确认 service 和 registrar descriptor 均实际加载 |

## 测试策略

测试分为共享 Analysis 契约、IDE host、LSP host 和跨宿主等价。每层只验证自身职责；不得把 host 能力探测写成共享 action 的条件分支。

| 层 | 内容 | 位置 | 为什么 |
| --- | --- | --- | --- |
| 共享 Analysis 契约 | 对 action factory 输出执行 `perform`，断言稳定 `actionId`、纯读分析和标准 `ModCommand`；检查共享 quick-fix 代码不引用 LSP/IDE host 包、不检测 headless 或 IDE 状态 | `code-insight/api/test`、`code-insight/code-actions/test` | host 隔离属于架构不变量；13 类只是第一批覆盖样本 |
| shared 修改金标 | 对第一批迁移夹具：取诊断 → 跑 factory → 对动作 `perform` → 用平台文件副本 executor 应用 → 断言最终 PSI 文本；`DeprecatedWarning` 断言无注册 | `code-insight/code-actions/test`，沿用 `AbstractAnalysisApiExecutionTest` 夹具 | 校验共享 action 产物，不夹带 LSP 协议验证 |
| LSP host adapter | 覆盖每个目标 SDK 的 ModCommand subtype；测试平台 executor 可执行项、`ModEditOptions` 默认值、目标文件/快照匹配、Unsupported 不生成 CodeAction、版本化 WorkspaceEdit 和请求取消传播 | `lsp/test` | 命令能力和协议转换完全由 LSP host 持有 |
| IDE host | 普通/lazy 注册、平台预览、撤销分组、光标、文件只读和错误处理；测试调用相同 action 实现 | IntelliJ product test | 验证 IDE 宿主契约，不检查 LSP 行为 |
| DevEco host | 对 `DevecoCangJieDiagnosticHighlightVisitor` 验证普通/lazy action 注册、预览和执行 | DevEco product test | service/registrar 能构建不代表 visitor 已接线 |
| 跨宿主等价 | 同一 action、相同快照和选择输入分别由 IDE 平台 executor 与 LSP adapter 执行，比较最终文本 | `intellij-ide/product/idea-plugin/src/test` / `lsp/test` | 验证共享语义；host 交互差异单独测试 |
| host registrar parity | 对照 IntelliJ、DevEco 与 LSP 实际加载的 descriptor，断言 shared registrar 与两条共享通道一致；同一语义动作跨渠道重复登记可复用 ID | IntelliJ/DevEco product test + `lsp/test` | 确保各 host 装配同一共享注册集；IDE import-on-the-fly 另由 IDE host test |
| 宏 registrar 类型 | 验证 `MacroQuickFixRegistrar` 返回 `CangJieQuickFixAction`，并以 `CjMacroCallQuickFixTest` 验证 IDE 行为 | IntelliJ product test | 验证扩展点/API 迁移；root LSP 是否装配宏 provider 由 LSP descriptor 决定 |
| 覆盖矩阵 | 保留 `QuickFixCoverageMatrixTest`，删除 `DeprecatedWarning` no-op factory 项 | `code-actions/test` | 防止注册回退、泛型基类键静默失效 |
| LSP 集成 | codeAction list 返回已预检的 WorkspaceEdit；unsupported/无变化不作为可执行项；客户端支持时使用带版本的 `documentChanges` | `lsp/test`，沿用 `CangjieCodeActionIntegrationTest` | 验证目标文档、快照版本和 host 不支持时的过滤 |

**先写金标、再迁移：** 金标测试的期望文件应先在旧实现上生成并人工确认，再迁移修复类，这样迁移后的行为等价性有可比较的基线。旧实现返回 `IntentionAction`，所以基线可用 `invoke` 在 PSI 上应用后取文本。

## 实施步骤与验收

先跑 root/IDE 目标 SDK 与 LSP headless 宿主探针。探针通过后，在同一变更中切换 `code-insight/api` 的 factory/list/service 类型、共享 action、所有 registrar 和 IDE/LSP host consumer；不添加临时双协议。

| 步骤 | 内容 | 验收 |
| --- | --- | --- |
| 0 探针（一次性测试，不改生产代码） | 在 `lsp` 测试里验证 `ModCommandService` / `ModCommandExecutor` 服务装配；用快照副本构造 action，调用 `perform` 和 `executeForFileCopy`；另测 `ModEditOptions` 默认选项与一个明确不支持的命令 | 服务已注册；命令可在指定非物理副本执行；目标文件、oldText、最终 PSI 文本一致；不支持命令被 LSP adapter 拒绝 |
| 1 金标基线 | 在旧实现上为 13 类有效修复生成期望文件并人工确认；记录 `DeprecatedWarning` service-level 无修复期望 | 期望文件入库 |
| 2 单一动作协议 | `api`、`code-actions`、`intellij-ide` macro registrar：factory/list/service 返回带稳定 ID 的 ModCommandAction；移除 shared `importOnTheFlyList` 和 no-op `DeprecatedWarning` | 所有 registrar 编译；共享层 host 边界检查通过 |
| 3 双 host 联动 | IDE visitor 直接注册或只在 lazy 平台边界包装；LSP list 调用相同 action，通过平台文件副本 executor 预检并返回版本化编辑 | IDE/LSP 对相同命令产生一致文本；LSP Unsupported 项不出现在列表 |
| 4 产品构建与文档 | 分别构建 root shared/LSP 和独立 IntelliJ/DevEco 产品；确认各产品实际加载的 registrar descriptor | shared core set 在 IDE/LSP 两端一致；宏范围边界清楚；R8 手工清单通过 |

**步骤依赖：** `CangJieQuickFixFactory`、`CangJieQuickFixesList`、`CangJieQuickFixService` 的类型改变会影响所有 registrar 和 host consumer，须原子切换。探针先确认平台服务、文件副本和双 SDK 签名；若平台 executor 在 headless LSP 中不可用，停止该执行设计，不在 Analysis 补 host 特判或自造 executor。

**为什么金标先于迁移：** 它给“行为不变”提供可比较的基线，迁移后的差异都能在测试里看到。

## 风险与本机验证清单

当前工作区包含 `external/kotlin`、`external/kotlin-plugin`、`external/kotlin-lsp`、`intellij-ide/` 和 `deveco/`。root 编译 SDK 为 `253.29346.379`，IDE 手工验收目标为 `253.29346.50`。本次已核对两者的核心 ModCommand 方法签名；headless 服务装配、文件副本执行和产品行为仍须实测。本次文档修订没有运行生产构建或测试。

| # | 项 | 怎么查 | 失败怎么办 |
| --- | --- | --- | --- |
| R1 | LSP headless application 能取得平台 `ModCommandService` / `ModCommandExecutor` | 确认 `intellijCore()` 带入 `analysis-impl` runtime；在真实 MockApplication 验证平台 `META-INF/AnalysisImpl.xml` 中的服务注册链；分别调用 `getInstance()`、构造 `psiUpdate`、调用 `executeForFileCopy` | 只在 LSP host 补齐正式平台服务注册；artifact 依赖不另造一份；任一服务仍不可用则停止，不复制平台 executor |
| R2 | `PsiUpdateModCommandAction` 可在 LSP 容器使用其 PSI 元素/指针服务 | 构造 action 并调用 `perform` | 记录缺失的平台服务与注册来源；不在 Analysis 根据 host 补分支 |
| R3 | 平台 executor 能把命令应用到 disposable PSI 副本 | 每个 action 从当前文档文本创建独立非物理副本，保留命令记录的源 VirtualFile 身份；调用 `executeForFileCopy` 并确认缓存快照不变 | 副本创建或文件身份映射不可满足则停止此方案 |
| R4 | 命令目标文件、文本和快照 URI 映射一致 | 断言 `oldText`、换行、VirtualFile 身份与最终副本 PSI 文本 | LSP adapter 拒绝目标文档不匹配的命令 |
| R5 | 两个目标 SDK 的 API 与方案一致 | 核对 root `253.29346.379` 和 IDE `253.29346.50` 中 `ModCommandAction`、`psiUpdate`、`executeForFileCopy`、`ModEditOptions`、chooser 与注册签名；枚举完整 sealed subtype | 按各目标 jar 修正文档；在两个目标上分别编译/验证 |
| R6 | 第一批 AddImport action 在非物理副本上可执行 | 仅作为首批迁移探针，检查 `PsiImportPlanApplier` 的 Document/commit 使用 | 若不能在副本运行，调整该 action 的实现，不改变共享层 host 契约 |
| R7 | 第一批成员 action 在 headless 副本上可格式化 | 金标覆盖 `CodeStyleManager` 输出 | 缺少 formatter 服务时只补 LSP host 装配；共享 action 不检查 host |
| R8 | IDE 行为：一次撤销、命令名、预览、光标、lazy Alt+Enter；chooser 改用平台 UI 后，修改结果须对应平台实际选择且取消不写文件 | 在真实 IDE 上操作 | chooser 的外观、分组和控件形式允许退化；默认全选、所选成员生成和取消不写文件的语义仍须保持；撤销、预览、光标和 lazy 差异需修正 IDE adapter 或验收声明 |
| R9 | 所有产品消费者完成 API 迁移 | 搜索 registrar、descriptor、service 调用点；分别构建 root、IntelliJ、DevEco | 更新一方 registrar；明确 extension API breaking change |
| R10 | LSP 的分析与副本写入阶段访问权限正确 | 验证 `perform` 在 `CaSession` 读上下文执行；退出 session 后，在 SDK 要求的写访问（并核实是否要求 EDT）下对独立副本调用文件副本 executor | 不满足则停止当前设计，不能在共享 action 判断当前 host |
| R11 | codeAction 返回编辑的文档版本边界明确 | 检查客户端 `documentChanges` 能力、请求文档版本与应用行为 | 支持时返回版本化 edit；不支持时明确只保证生成瞬间的快照 |
| R12 | 稳定动作键与请求实例去重键各自正确 | 断言不同语义动作的 `(diagnosticClass, actionId)` 唯一、同一动作跨通道重复允许；LSP 另测 `(diagnosticClass, exactPrimaryRange, actionId)` 只合并同一诊断实例；覆盖多 factory 和 priority 重排 | 不允许回退到 family/presentation 文案或列表序号；不同范围的同类诊断不得误合并 |

**IDE 手工清单（R8）：** ① 修复后一次撤销完全还原；② chooser 默认全选、子集选择和取消仍不误写；③ Alt+Enter 预览正常；④ 补 match 分支后光标在分支体、成员生成后光标在最后插入处；⑤ 大类型上 Alt+Enter 不阻塞编辑器。chooser 外观不作为等价性要求。LSP 集成测试独立验证 `ModEditOptions` 默认值及版本化编辑。

## 默认设计选择与范围外事项

以下是本方案采用的默认设计选择，沉默本身不视为额外授权；需要确认的是对应验收结果和范围边界。

| # | 决策 | 默认 | 理由 |
| --- | --- | --- | --- |
| 1 | `DeprecatedWarning` 的替换修复 | 删除，并同步删掉覆盖矩阵里的承诺项 | `replacement` 恒为空，这条修复从未产出；等检查器真正给出 replacement 再加 |
| 2 | 成员生成后的格式化范围 | 保持当前 `CodeStyleManager.reformat(containingFile)` 的整文件格式化；不增加成员排序 | 当前源码执行的是整文件 reformat，不是成员重排；修改格式化范围属于后续独立行为变更 |
| 3 | LSP 对 `ModEditOptions` 的处理 | 在 LSP host 检查 `canUseDefaults`；为真则应用默认 option container，否则不返回该 CodeAction | 交互策略留在 LSP host，后续可以扩展客户端交互，不在共享 Analysis 加分支 |

**范围外（本方案不处理）**

- `cjpm.toml` 的版本号 `LocalQuickFix`、Safe delete、查看宏展开结果：不是诊断修复，没有 LSP 对应，保持原样。
- `MacroUnresolved` 的 LSP 候选发现：宏 quick-fix registrar/action 类型随 shared API 一并迁移；root LSP 当前未加载宏插件 descriptor，且没有 `MacroDeclarationIndex`。将宏候选 provider 及依赖纳入 root LSP 后，才能把宏修复加入两端共同 action 集。
- `MacroCallSiteAnnotator` / `CompileMacroPackageIntentionAction` 的独立 IDE action 通路不属于共享 quick-fix registrar；保持原通路并单独验证，不把它误报为本次已迁移。
- Code → Generate 菜单的 Implement Members（非诊断入口）：保持 `LanguageCodeInsightActionHandler`，只把插入改调共用的 `insertMembers`；后续可转 ModCommand。
- 其余约 540 条诊断的修复（类型不匹配、推断失败、命名参数等）：框架就绪后按族添加，每族一个修复类加一组金标。
- `intellij-ide`、DevEco visitor 的实际 quick-fix 接线都属于 IDE host；不得以 service/descriptor 构建通过代替 visitor 行为验收。
