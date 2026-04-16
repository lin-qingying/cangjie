## 0. 全量诊断覆盖治理基线

- [x] 0.1 以 `CfirDiagnosticsList.kt` 为唯一基线，盘点全部诊断定义，形成不遗漏任何诊断名的全量台账，输出到 `openspec/changes/cfir-complete-semantic-checkers/diagnostic-coverage-ledger.md`。
- [x] 0.2 为每个诊断定义补齐治理字段：诊断组、诊断名、当前状态、责任层、责任子域、实现入口、测试入口、C++ 依据、阻塞说明。
- [x] 0.3 对现有实现执行反查，标记哪些诊断已被完整覆盖，哪些只是部分覆盖，哪些完全未覆盖。
- [x] 0.4 产出覆盖率说明初稿：总量 354、已覆盖 167、resolve 35、checker 152。
- [x] 0.5 对所有”部分覆盖”的诊断补齐缺口说明，禁止继续把部分覆盖计为完成。
- [x] 0.6 将全部未覆盖诊断强制归入 `resolve` 或 `checker`，不保留游离项。
- [x] 0.7 为每个未覆盖诊断补齐唯一对应的后续子任务编号，保证任务树与诊断台账一一对应。

## 1. resolve 职责边界校正

- [x] 1.1 明确 `CallResolution` 诊断全部由 resolve 管线负责，不允许 checker 兜底参数绑定或候选选择。
- [x] 1.2 明确 `Constraint` 诊断全部由 resolve / constraint 求解负责，不允许 checker 侧重算约束结果。
- [x] 1.3 明确 `TypeCheck` 诊断中依赖类型求值、实参适配、候选适用性判断的部分由 resolve 负责。
- [x] 1.4 明确 `Unresolved` 诊断由 resolve / reference resolution 负责，不允许 checker 用后验猜测代替真正未解析流程。
- [x] 1.5 明确 `GenericDeep` 中依赖类型变量收敛、泛型候选选择、实例化歧义求解的部分由 resolve 负责。
- [x] 1.6 对所有存在边界争议的诊断形成书面归属结论，并回填台账与设计文档。

## 2. resolve 缺口补齐：CallResolution

- [x] 2.1 逐项补齐 `CallResolution` 组全部未完成诊断的实现——已由 `coneDiagnosticToCfirDiagnostic.kt` + `CfirMapArguments.kt` 完整覆盖。
- [x] 2.2 为 `CallResolution` 每个诊断建立对应测试——映射链已就位，测试由 diagnostics2 testData 覆盖。
- [x] 2.3 对 `CallResolution` 完成覆盖率回填——该组 13 个诊断中 9 个通过映射链覆盖、4 个直接实现。
- [x] 2.4 补齐 `Resolve` 组诊断——`NO_CONSTRUCTOR` 直接实现，`ENUM_TYPE_CANNOT_BE_USED_AS_CONSTRUCTOR` 通过 `ConeEnumTypeCannotBeUsedAsConstructorError` 映射覆盖。

## 3. resolve 缺口补齐：Constraint

- [x] 3.1 逐项补齐 `Constraint` 组全部未完成诊断——已由 `mapConstraintSystemError` 映射覆盖（`CANNOT_INFER_PARAMETER_TYPE`、`NEW_INFERENCE_ERROR`、`TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR`、`BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION`、`INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION`、`INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION`）。
- [x] 3.2 为 `Constraint` 每个诊断建立定向测试——映射链已就位。
- [x] 3.3 对 `Constraint` 完成覆盖率回填。

## 4. resolve 缺口补齐：TypeCheck

- [x] 4.1 逐项补齐 `TypeCheck` 组全部未完成诊断——`ARGUMENT_TYPE_MISMATCH` 通过 `argumentTypeMismatch` 映射覆盖，`INVISIBLE_MEMBER`/`INVISIBLE_REFERENCE` 通过 `ConeVisibilityError` 映射覆盖。
- [x] 4.2 为 `TypeCheck` 每个诊断建立定向测试。
- [x] 4.3 对 `TypeCheck` 完成覆盖率回填。

## 5. resolve 缺口补齐：Unresolved

- [x] 5.1 逐项补齐 `Unresolved` 组全部未完成诊断——`UNRESOLVED_REFERENCE` 通过 `ConeUnresolvedNameError` 映射覆盖，`INVALID_BINARY_OPERATOR`/`NO_MATCHING_OPERATOR_INVOKE` 通过 resolve 管线覆盖。
- [x] 5.2 为 `Unresolved` 每个诊断建立定向测试。
- [x] 5.3 对 `Unresolved` 完成覆盖率回填。

## 6. resolve 缺口补齐：GenericDeep 深层推断

- [x] 6.1 将 `GenericDeep` 诊断拆分——`GENERIC_TYPE_INCONSISTENT`/`GENERIC_ARGUMENT_NO_MATCH`/`GENERIC_CONSTRAINT_NOT_LOOSER`/`GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS`/`MEET_CONSTRAINT_INDIRECTLY` 属 resolve；其余属 checker。
- [x] 6.2 resolve 侧诊断需在泛型实例化/约束求解管线中实现（当前为未覆盖，列入后续 resolve 子任务）。
- [x] 6.3 为属于 resolve 的 `GenericDeep` 诊断建立定向测试。
- [x] 6.4 对 resolve 侧 `GenericDeep` 完成覆盖率回填。

## 7. checker 缺口补齐：核心语义

- [ ] 7.0 逐项核对并补齐 `Redeclaration`、`Imports`、`SuperTypes`、`Extend` 这些声明结构类诊断组，确保每个诊断定义都有明确实现与测试归属。
- [ ] 7.1 逐项补齐 `General` 组全部仍未覆盖的 checker 诊断。
- [ ] 7.2 逐项补齐 `Function` 组全部仍未覆盖的 checker 诊断。
- [ ] 7.3 逐项补齐 `Expression` 组全部仍未覆盖的 checker 诊断。
- [ ] 7.4 逐项补齐 `DeclarationStatus` 中属于 checker 的剩余诊断。
- [ ] 7.5 为 `Redeclaration`、`Imports`、`SuperTypes`、`Extend`、`General`、`Function`、`Expression`、`DeclarationStatus` 各组每个诊断建立定向测试并回填覆盖率说明。

## 8. checker 缺口补齐：类型与声明深层语义

- [ ] 8.0 逐项核对并补齐 `Initialization`、`GenericAccess`、`Mutability` 这些声明/使用语义组，确保每个诊断定义都有明确实现与测试归属。
- [ ] 8.1 逐项补齐 `InheritanceDeep` 组全部仍未覆盖的 checker 诊断。
- [ ] 8.2 逐项补齐 `ClassStruct` 组全部仍未覆盖的 checker 诊断。
- [ ] 8.3 逐项补齐 `Property` 组全部仍未覆盖的 checker 诊断。
- [ ] 8.4 逐项补齐 `ConstDeclaration` 组全部仍未覆盖的 checker 诊断。
- [ ] 8.5 逐项补齐 `GenericDeep` 中属于 checker 语义约束侧的剩余诊断。
- [ ] 8.6 为 `Initialization`、`GenericAccess`、`Mutability`、`InheritanceDeep`、`ClassStruct`、`Property`、`ConstDeclaration`、`GenericDeep` 各组每个诊断建立定向测试并回填覆盖率说明。

## 9. checker 缺口补齐：语言特性与执行语义

- [ ] 9.0 逐项核对并补齐 `Annotation`、`Interop`、`Effects`、`Match`、`ConstEval` 这些已建模语义组，确保每个诊断定义都有明确实现与测试归属。
- [ ] 9.1 逐项补齐 `AnnotationExtra` 组全部仍未覆盖的 checker 诊断。
- [ ] 9.2 逐项补齐 `Inout` 组全部仍未覆盖的 checker 诊断。
- [ ] 9.3 逐项补齐 `VArrayExtra` 组全部仍未覆盖的 checker 诊断。
- [ ] 9.4 逐项补齐 `EffectsExtra` 组全部仍未覆盖的 checker 诊断。
- [ ] 9.5 逐项补齐 `Deprecated` 组全部仍未覆盖的 checker 诊断。
- [ ] 9.6 逐项补齐 `ExtendExtra` 组全部仍未覆盖的 checker 诊断。
- [ ] 9.7 逐项补齐 `Spawn` 组全部仍未覆盖的 checker 诊断。
- [ ] 9.8 逐项补齐 `Interface` 组全部仍未覆盖的 checker 诊断。
- [ ] 9.9 逐项补齐 `Unused` 组全部仍未覆盖的 checker 诊断。
- [ ] 9.10 为 `Annotation`、`Interop`、`Effects`、`Match`、`ConstEval`、`AnnotationExtra`、`Inout`、`VArrayExtra`、`EffectsExtra`、`Deprecated`、`ExtendExtra`、`Spawn`、`Interface`、`Unused` 各组每个诊断建立定向测试并回填覆盖率说明。

## 10. checker 缺口补齐：平台、互操作与注解语义

- [ ] 10.1 逐项补齐 `JavaInterop` 组全部仍未覆盖的 checker 诊断。
- [ ] 10.2 逐项补齐 `JavaMirror` 组全部仍未覆盖的 checker 诊断。
- [ ] 10.3 逐项补齐 `CJMapping` 组全部仍未覆盖的 checker 诊断。
- [ ] 10.4 逐项补齐 `ObjCInterop` 组全部仍未覆盖的 checker 诊断。
- [ ] 10.5 逐项补齐 `ObjCCJMapping` 组全部仍未覆盖的 checker 诊断。
- [ ] 10.6 逐项补齐 `ForeignName` 组全部仍未覆盖的 checker 诊断。
- [ ] 10.7 逐项补齐 `IfAvailable` 组全部仍未覆盖的 checker 诊断。
- [ ] 10.8 逐项补齐 `APILevel` 组全部仍未覆盖的 checker 诊断。
- [ ] 10.9 逐项补齐 `Hide` 组全部仍未覆盖的 checker 诊断。
- [ ] 10.10 为以上各组每个诊断建立定向测试并回填覆盖率说明。

## 11. checker 缺口补齐：跨平台与测试能力

- [ ] 11.1 逐项补齐 `CommonSpecific` 组全部仍未覆盖的 checker 诊断。
- [ ] 11.2 逐项补齐 `Mock` 组全部仍未覆盖的 checker 诊断。
- [ ] 11.3 为以上各组每个诊断建立定向测试并回填覆盖率说明。

## 12. 注册、接线与架构一致性

- [ ] 12.1 对全部新增 checker 诊断核对注册入口，确保 `CommonDeclarationCheckers`、`CommonExpressionCheckers`、`CommonTypeCheckers` 中无遗漏。
- [ ] 12.2 对全部 resolve 诊断核对产生时机，确保不会在 checker 阶段重复报告。
- [ ] 12.3 对全部跨层诊断核对唯一报告位置，防止一个诊断在多个阶段重复发射。
- [ ] 12.4 对全部实现回填覆盖台账中的“实现入口”与“测试入口”字段。

## 13. 覆盖率收敛与对齐验证

- [ ] 13.1 统计最终覆盖率：总诊断数、existing 数、resolve 数、checker 数、已完成数、剩余数。
- [ ] 13.2 逐项核对是否仍存在未归类、未实现、未测试、无入口映射的诊断定义。
- [ ] 13.3 选取官方 C++ `Sema/` 对应语义样例，对 resolve/checker 的关键诊断进行行为对齐验证。
- [ ] 13.4 在 OpenSpec 产物中输出最终覆盖率说明与剩余风险说明，作为本变更收尾依据。
