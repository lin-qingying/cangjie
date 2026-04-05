# 继承 / extend / super 诊断续完善方案

## Summary
本轮先围绕 `diagnostics-coverage-gap-vs-cpp.md` 中优先级最高的继承语义域推进，目标不是只补几个 `.cj` 用例，而是按 Kotlin FIR 的分层方式把“已有 producer 但未回归覆盖”和“同域缺失 producer/映射”一起纳入统一方案。

实施顺序分两段：
1. 先把已存在 checker producer 的继承类诊断补成稳定回归基线。
2. 再在同一语义域内补齐缺失的 producer / coneDiagnostic 映射，为后续 visibility / generic / constructor 批次建立可复用框架。

## Key Changes
### 1. 先完成已有 producer 的回归覆盖
在 `cfir/analysis-tests/testData/diagnostics` 下补齐继承批次负例，并保留现有正例文件不回退：
- `coverage/inheritance/overrideReturnTypeMismatchRich.cj`
  覆盖 `OVERRIDING_RETURN_TYPE_MISMATCH` 的真实负例，验证 `CfirOverrideChecker` 已有返回类型协变检查。
- `super/illegalSuperInStructAndEnum.cj`
  覆盖 `STRUCT_SUPER_NOT_ALLOWED`、`ENUM_SUPER_NOT_ALLOWED`，与现有 `INTERFACE_SUPER_NOT_ALLOWED` 分开建样例，避免复用错误分支。
- 将 `coverage/extensions/extendCTypeNotAllowed.cj` 从 TODO 占位升级为真实断言样例，验证 `CfirExtendTargetLegalityChecker` 的 `EXTEND_C_TYPE_NOT_ALLOWED`。

这一段只补“已存在 producer 的可见行为”，不做弱化兜底，不改诊断语义。

### 2. 继承语义域内补齐缺失 producer / 映射
按 Kotlin FIR 的两层结构推进，而不是把所有问题塞进单个 checker：
- `checker 层`
  继续放在 declaration / expression 分层下，保持与 Kotlin FIR 的 `FirOverrideChecker`、`FirSupertypesChecker`、相关 expression checker 对齐。
- `coneDiagnostic -> CfirDiagnostic 层`
  扩展 [`coneDiagnosticToCfirDiagnostic.kt`](/D:/code/intellij/cangjie/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/coneDiagnosticToCfirDiagnostic.kt)，让解析/解析后错误能稳定映射到新增或既有 `CfirErrors`。

本轮在继承域内应新增或补齐以下能力：
- `CLASS_NOT_OPEN_FOR_INHERITANCE`
  对齐 Kotlin `FINAL_SUPERTYPE` 的职责，放在 supertype / inheritance checker 路径，不借用 unrelated type mismatch。
- `CANNOT_OVERRIDE_INVISIBLE_MEMBER`
  对齐 Kotlin `FirOverrideChecker` 的可见性分支；不要继续把“只有不可见基成员”降格为 `NOTHING_TO_OVERRIDE`。
- `INVISIBLE_REFERENCE` / `INVISIBLE_MEMBER`
  在继承语义相关访问路径中建立稳定 producer 或映射入口，尤其是继承链成员访问与 override 目标筛选处。
- 允许新增少量同域诊断工厂，但仅限 Kotlin 对齐后确实缺失的继承相关语义，不做大规模重命名。

### 3. 保持框架级组织，不做局部补丁
实现时遵守以下结构性约束：
- 诊断来源明确区分：
    - 直接语义规则检查走 checker。
    - 解析结果携带的 cone 诊断走映射层。
- `CfirErrors` / 默认消息 / positioning strategy 一起补齐，避免只注册 factory 不接入 reporter。
- 重要接口、诊断入口、框架分层添加中文注释，解释“为什么放在 checker 层”或“为什么放在 cone 映射层”。
- 目录与命名继续贴近 Kotlin FIR：
    - declaration 继承规则仍收敛在 override / supertypes checker 侧。
    - expression 中的非法 `super` 继续留在 expression checker，不挪到 declaration checker。

## Public API / Diagnostic Interface Changes
- 保留现有 `CfirErrors` 名称与消息兼容性。
- 允许在继承语义域新增少量诊断工厂，用于承接 Kotlin FIR 对齐后当前仓库尚未建模的错误。
- 不做已有诊断名重命名；若 Kotlin 参考名不同，优先通过中文注释说明语义对齐关系。
- 若 `CfirOverrideChecker` 当前将“不可见 override 目标”处理为 `NOTHING_TO_OVERRIDE`，应改为区分：
    - 完全无候选：`NOTHING_TO_OVERRIDE`
    - 有候选但不可见：`CANNOT_OVERRIDE_INVISIBLE_MEMBER`

## Test Plan
需要同时补“样例回归”与“框架行为验证”：

- 诊断样例
    - override 返回类型不协变时报 `OVERRIDING_RETURN_TYPE_MISMATCH`
    - `struct` / `enum` 内使用 `super` 分别命中各自诊断
    - `extend` 到 C / Java 边界类型时报 `EXTEND_C_TYPE_NOT_ALLOWED`
    - final / non-open 继承时报 `CLASS_NOT_OPEN_FOR_INHERITANCE`
    - override 不可见成员时报 `CANNOT_OVERRIDE_INVISIBLE_MEMBER`
- 定向模块验证
    - `:cfir:analysis-tests:test`
    - 若 checker 或映射层改动影响 `cfir/checkers` 行为，再加相关模块测试或最小可行 `assemble/test`
- 回归检查
    - 现有正例文件继续通过，特别是：
        - `coverage/inheritance/overrideReturnType.cj`
        - `coverage/inheritance/classSuperCallAllowed.cj`
        - `super/repeated_inheritance.cj`
    - 确认新增不可见 override 诊断不会把真正的 `NOTHING_TO_OVERRIDE` 误报成可见性错误。

## Assumptions
- 本轮只推进 first-party 模块，不修改 `external/`。
- 先完成“继承 / extend / super”整批能力，不在本轮展开 constructor / initialization / call 语义块。
- Kotlin 参考实现用于对齐分层和职责，不要求一比一复制诊断名。
- 当前仓库未初始化 OpenSpec 变更目录，因此本轮以仓库内文档和代码结构为准，不额外生成 OpenSpec 产物。
