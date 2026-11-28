# 设计：宏展开源映射的只读契约与补全消费方

## 上下文

- **既有设施**（2026-10-11 实测）：
  - `MacroSurface.sourceRange`：展开面的源区间（`startOffset`/`endOffset`）；
  - `MacroCallForest`：按 `(sourceRange.startOffset, surfaceId)` 排序、按区间包含关系表达嵌套，并把展开 payload 落在 `replaceRange = surface.sourceRange` 的载体上（`MacroPayloadChannel` 另有 `UNRESOLVED` 状态）；
  - `CfirRawMacroSurfaceUsageStore`：按 `sourceRange?.startOffset/endOffset` 记录使用点。
- **硬性边界不变**：`MacroSurface*` **不**实现 `CfirElement`、**不**进入 provider-visible 的 final CFIR（`RecordExpandedRawFilesOnce` 的状态机围绕这条边界设）。
- **补全侧的现状行为**（11.5 已锁）：宏 token 位置**所有区段都不产出候选且不抛异常**。

## 目标 / 非目标

**目标：**

- 提供**只读**的「位置 → 宏展开源映射（展开面身份 + `sourceRange` + 可靠性）」查询；
- 让补全在**可靠展开**内容内的候选来源受映射限定；映射缺失时行为与今天逐字一致；
- 嵌套宏、跨行宏、无展开、不可靠展开四类情形各有用例。

**非目标：**

- 不把展开产物放进 CFIR / visitor / checker（与该边界冲突，属宏侧路线决定）；
- 不做展开代码里的导航/找定义（那是另一条产品线，本变更不引入）；
- 不补宏侧的 Baseline 计划文档（宏侧欠账，如实保留）。

## 决策

- **D1：契约只暴露映射，不暴露产物。** 返回类型是「身份 + 区间 + 可靠性」的纯数据；调用方拿不到 `MacroSurface` 本身，边界不被绕过。
- **D2：可靠性是**三态**而非布尔。** 可靠 / 不可靠（`MacroPayloadChannel.UNRESOLVED` 等）/ 无展开。消费方对「不可靠」与「无」采取同一保守行为（不改变既有分流）。
- **D3：首个消费方选补全。** 理由：G1–G5 内已有宏位置分流与用例骨架；且「展开内容内不该出现工作区无关名字」是拒绝式（保守）的收紧，出错面小。
- **D4：映射实现只读既有结构。** 不新增缓存、不新增遍历：`MacroCallForest` 已是按区间组织的结构，查询按其区间定位即可；性能进 13.2 的预算文件观察。

## 风险 / 权衡

- **区间歧义**（嵌套宏的同一偏移属于多个 surface）：按 `MacroCallForest` 的既有次序（`startOffset` 升序、同序按 `surfaceId`）取**最内层**并写明；歧义无法化解时按 D2 返回「不可靠」。
- **可靠性判据漂移**：判据必须以 `MacroPayloadChannel` 的既有状态为准，并在用例中钉死，不自行发明启发式。
- **消费方误收紧**：若映射在真实工程里频繁缺失，表现为「宏展开内候选变少」；因此**默认行为不变**，收紧只在映射明确可靠时生效，并有回归用例保证宏位置行为不变。
