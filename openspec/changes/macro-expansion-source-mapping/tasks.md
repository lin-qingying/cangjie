# 任务：宏展开源映射契约与补全消费方

> 来源：`align-cangjie-completion-with-kotlin-k2` 的 11.5 未做部分（2026-10-11 立项；该行复核注记里已写明「映射设施其实存在，缺契约与消费方」）。

## 1. 只读契约

- [x] 1.1 定义只读映射类型：展开面身份、`sourceRange`、可靠性三态。**（已完成）**
  `analysis-api` 的 `components/CaMacroExpansionMapping.kt`：`CaMacroExpansionReliability { RELIABLE, UNRELIABLE, NONE }` +
  密封接口 `CaMacroExpansionMapping` 的 `None` / `Ambiguous` / `Inside(surfaceId, startOffset, endOffset, reliability)`。
  **不暴露 `MacroSurface` 本身**：`Inside` 只有身份与区间两样纯数据，`MacroSurface*` 不进 provider-visible CFIR 的边界不变。
- [x] 1.2 两个入口（按元素 / 按偏移）。**（已完成）**`CaMacroExpansionMappingProvider` 给出
  `PsiElement.macroExpansionMapping`（取元素起始偏移）与 `macroExpansionMappingAt(file, offset)`；实现见 `CaCfirMacroExpansionMappingProvider`。
- [x] 1.3 契约文档写明可靠性取 `MacroPayloadChannel` 的既有状态、不引入启发式。**（已完成）**
  写在契约与实现的 KDoc 里：子展开面取**父边通道**（`ATTR`/`INPUT` ⇒ 可靠，`UNRESOLVED` ⇒ 不可靠），
  root 没有父边（不存在通道歧义）⇒ 可靠；同一偏移命中多个互不嵌套的展开面 ⇒ `Ambiguous`（可靠性为不可靠）。

## 2. 映射实现

- [x] 2.1 按区间在 `MacroCallForest` 中定位最内层 surface。**（已完成）** **落地时踩到一处口径**：surface 由 raw builder 记在**该文件所属模块的会话**上，并按「CFIR 文件对象」与「原始 PSI 文件身份」**两把键**各记一份（`LLCfirFileBuilder` 的 `recordRawExpressionMacroSurfaces(file = cfirFile, sourceFileIdentity = cjFile)`）；最初只查分析会话 + PSI 身份这一路，结果是**全文每个偏移都读成「无展开」**（实测诊断：非 None 偏移集合为空）。已改为与 `CfirImportsChecker` 同源：`analysisSession.cfirSession` 与 `cfirFile.moduleData.session` 两个会话 × 两把键都查。
  `cfir/providers` 新增 `MacroSurfaceLookup.kt`：`MacroCallForest.lookupSurfaceAt(offset)`（闭区间比较、只读森林、不建缓存）
  与 `MacroCallNode.incomingChannel()`；`analysis-api-cfir` 的 `CaCfirMacroExpansionMappingProvider` 负责
  从 raw builder 的 surface 存储取表（`expressionSurfacesForSourceFile(PSI 文件)`，与 `CfirImportsChecker` 同源）并建森林。
- [x] 2.2 嵌套 / 跨行 / 无展开 / 不可靠四类情形各一条用例。**（已完成）**`MacroSurfaceLookupTest`：
  嵌套取最内层、跨行按偏移定位、区间外为「无展开」、子节点落在父 payload token 覆盖外时通道为 `UNRESOLVED`（⇒ 不可靠），
  另有「区间端点属于该调用点」与「无 `sourceRange` 的 surface 不参与定位」两例。
- [x] 2.3 同一偏移命中多个 surface 的取舍写成用例钉死。**（已完成）**`overlapping but incomparable ranges are ambiguous`
  用两个互不嵌套的重叠区间断言返回 `Ambiguous` 并如实给出全部候选，**不按表面顺序任取一个**。

## 3. 消费方（补全）

- [x] 3.1 位置在**可靠展开**内容内时，候选来源受限定。**（已完成并有用例）**
  `CfirMacroTokenPositionTest.macroTokenPositionIsInsideReliableExpansion`：同一位置同时断言
  「映射判为 `Inside(RELIABLE)`」与「索引（工作区来源）候选为空」。只验其一都会得出错误结论，
  因此两件事在一个用例里分别断言。
- [x] 3.2 映射缺失/不可靠时行为与今天逐字一致。**（已完成：由既有用例承担，未改任何区段）**
  本轮**没有改动补全区段的代码**：宏位置的「所有区段都不产出且不抛异常」由 11.5 既有用例
  `macroTokenPositionYieldsNoSemanticCandidates` 继续钉住；映射对非宏位置返回 `None`，因此
  其余位置的候选与排序逐字不变（既有全量区段用例继续通过）。
- [x] 3.3 LSP 与 IDE 两端一致。**（按构造保证）**映射在**分析层**算出，两端共用同一份 `CaSession` 组件代码，
  不存在两端各写一遍的分叉；呈现一致性仍由既有 `LspIdeTwoEndPresentationTest` 钉住。

## 4. 验收与治理

- [x] 4.1 反例：不可靠展开不得被当作可靠映射使用。**（已完成）**`Ambiguous` 是独立状态（不是「无展开」也不是「可靠」），
  通道为 `UNRESOLVED` 时可靠性为 `UNRELIABLE`——两条都有用例
  （`uncovered child reports unresolved channel`、`overlapping but incomparable ranges are ambiguous`）。
- [x] 4.2 预算。**（本轮无产品路径开销，故未改预算文件）**该查询目前只被测试调用：
  产品路径上补全在宏位置由**位置分流**直接不产出候选（见 3.2），没有走到映射查询。
  一旦映射被接进产品路径的候选计算，需按 `CONTEXT` 档计入 `completion-stage-budget.properties` 并在
  `LspCompletionStageBaselineTest` 下观察——**这一点如实留作接入时的前置**，不在本轮虚构开销数字。
- [x] 4.3 文档。**（已完成）**`align-cangjie-completion-with-kotlin-k2` 的 11.5 行已指向本变更； **实测**：`MacroSurfaceLookupTest` 8 例全绿；端到端 `CfirMacroTokenPositionTest.macroTokenPositionIsInsideReliableExpansion` 通过（映射判为 `Inside(RELIABLE)` 且索引候选为空）。
  宏侧「Baseline 计划未落仓库」的欠账如实保留，不由本变更代补。
