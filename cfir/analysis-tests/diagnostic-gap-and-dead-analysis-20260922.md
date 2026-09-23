
## 0. 方法与数据基础

- **官方侧**：`external/cangjie_compiler` 全部 47 个 tag（v1.0.0 → v1.3.0-alpha.05），逐 tag 提取
  `include/cangjie/Basic/**.def` 中 `ERROR/WARNING/NOTE/FATAL/REMARK(<name>, ...)` 条目，
  共 **1072 个唯一官方诊断**，含每个诊断的**首现版本**与**末现版本**（生命周期）。
- **CFIR 侧**：`cfir/checkers/gen/.../CfirErrors.kt`（源自 `DIAGNOSTICS_LIST`），共 **527 个诊断**。
- **映射与甄别**：
  1. 机械映射（后缀精确 458 + token 相似 69），逆向构建官方→CFIR 对照；
  2. 子代理对 110 个 v1.0.0 `sema_*` 无映射项逐条比对官方/CFIR 消息文本与触发点，甄别改名等价 vs 真缺失；
  3. 全仓引用扫描（`CfirErrors.<NAME>`、`import ...CfirErrors.<NAME>` 裸名、`"CFIR_` 动态字符串、testData 引用），
     得 24 个零报告点候选，再由子代理逐条验证死因（含官方 git 历史 `-S` 验证）。
- 前一阶段（门禁）分析见 [diagnostic-version-gating-analysis-20260922.md](diagnostic-version-gating-analysis-20260922.md)。

---

## 1. CFIR 缺哪些官方诊断（按官方首现版本）

### 1.1 v1.0.0 语义层缺失（CFIR 职责内）— 110 项甄别结果

**A. 有改名等价（60 项，非缺失）** — 代表性映射：

| 官方 | CFIR |
|---|---|
| `sema_mismatched_types` | `CFIR_TYPE_MISMATCH`（另有 `_BECAUSE` / `_MULTIPLE_ASSIGN` 变体） |
| `sema_undefined_variable` | `CFIR_USED_BEFORE_INITIALIZATION` |
| `sema_missing_overridden_func` / `sema_missing_redefined_func` | `CFIR_NOTHING_TO_OVERRIDE` |
| `sema_overload_conflicts` | `CFIR_CONFLICTING_OVERLOADS` |
| `sema_div_zero` / `sema_mod_zero` | `CFIR_CONST_EVAL_DIVIDE_BY_ZERO` |
| `sema_exceed_num_value_range` | `CFIR_LITERAL_NUMERIC_OVERFLOW` |
| `sema_duplicated_item_in_enum` | `CFIR_CONFLICTING_OVERLOADS` |
| `sema_interface_can_not_be_instantiated` | `CFIR_NO_CONSTRUCTOR`（文案泛化） |
| `sema_extend_use_super` | `CFIR_EXTEND_SUPER_NOT_ALLOWED` |
| `sema_immutable_type_illegal_property` | `CFIR_EXTEND_IMMUTABLE_MUT_PROPERTY` |
| 其余 50 项 | 见甄别明细（附录 JSON：`%TEMP%\cj_diag_version_scan\official_missing_from_cfir.json`） |

**B. 非用户诊断/由其它机制承载（15 项，非缺失）**：
`sema_diag_begin/end`（日志锚点）、`sema_found_candidate_decl`、`sema_found_possible_candidate_decl`、
`sema_invalid_unary_expr_note`、`sema_which_constraint_not_match`（附属 note，主错误由 CFIR 承载）、
`sema_unimplemented_func_or_property`（附属 note）、`sema_invalid_file_hash`（非语言规则）、
`sema_symbol_not_collected`、`sema_unexpected_wrapper`、`sema_invalid_tokens_implementation`（自举专用）、
以及 3 个官方自身就是死诊断的（`sema_unsupport_operator`、`sema_can_not_use_annotations_together`、`sema_return_unit`）。

**C. 真缺失（35 项）** — 按 cjc 硬编码细则聚类（名称为官方 .def 逐字核对）：

| 类别 | 缺失项（官方名） |
|---|---|
| 内建字面量/数组/range/模式（9） | `sema_array_first_arg_cannot_be_named`, `sema_array_second_arg_cannot_be_named`, `sema_array_second_wrong_named_arg`, `sema_ambiguous_expo_right_operand_type`, `sema_inconsistency_range_elemType`, `sema_range_step_not_int64`, `sema_pattern_can_not_be_assigned`, `sema_pattern_literal_expected`, `sema_wrong_forin_guard` |
| 泛型/函数形态/实例化（7） | `sema_forbid_generic_constructor`, `sema_forbid_generic_nonstatic_method`, `sema_generic_function_in_interface`, `sema_generic_in_operator_overload`, `sema_invalid_generic_function_in_class`, `sema_numeric_convert_must_be_numeric`, `sema_abstract_class_can_not_be_instantiated` |
| C 互操作/可变性/比较（7） | `sema_illegal_cpointer_generic_type`, `sema_illegal_ctype_generic_argument`, `sema_invalid_tuple_field_ctype`, `sema_immutable_access_mutable_func`, `sema_invalid_coalescing`, `sema_tuple_element_cmp_not_bool`, `sema_pointer_unknow_generic_type` |
| 作用域与访问（6） | `sema_type_must_toplevel`, `sema_illegal_access_inner_classlike`, `sema_illegal_access_interface_field`, `sema_illegal_this_in_interface`, `sema_import_not_in_current_module`, `sema_redefinition_entry`（多 main） |
| 其余（6） | `sema_typealias_external_refer_internal`, `sema_invalid_constructor_in_enum`, `sema_invalid_enum_member_access`, `sema_expand_macro_redefinition`, `sema_fail_flow_expr_operand_has_named_param`, `sema_flow_expressions_use_this_or_super` |

### 1.2 v1.1.0 新增且 CFIR 缺失（CFIR 支持矩阵内，真缺口）

| 官方 | 说明 |
|---|---|
| `sema_java_interoplib_version_mismatch` / `sema_java_interoplib_version_too_old` | interoplib 版本一致性检查（与 `JavaInteropAnnotations`(1.1.0) 配套） |
| `packages_macro_inconsistent` / `packages_visibility_inconsistent` | packages 元数据一致性（`PackageProductMetadata`(1.1.0) 配套） |
| `common_non_exaustive_platfrom_exaustive_mismatch` | 对应 CFIR 死诊断（见 §3） |
| `feature_already_seen_name` 等 `feature_*` ×3、`module_common_cjo_wrong_package` / `module_common_part_path_is_required`、`frontend_can_not_handle_to_many_chir` | 序列化/编译配置层，非 CFIR 分析职责，登记不实现 |

### 1.3 v1.2.0+ 新增（超出当前支持矩阵 `LATEST_STABLE=1.1.3`，不属缺失，前瞻登记）

`sema_apilevel_integer_form_unsupported`、`sema_apilevel_invalid_version_format`（v1.2.0-alpha.19）、
`sema_spawn_capture_var`、`sema_try_handle_capture_var`（v1.2.0-alpha.20）、`sema_func_can_only_be_called`（v1.2.0-beta.rc1）。

### 1.4 不属 CFIR 职责（登记，不算缺失）

- **parser/lexer 语法层 225 项**（`parse_*` / `lex_*`）：由 PSI 解析器承担；其中 v1.1.0 的 `parse_java_mirror_*` / `parse_objc_*` / `parse_cjmp_*` 语义部分已由 CFIR 的 `CFIR_JAVA_MIRROR_*` / `CFIR_OBJC_*` / `CFIR_COMMON_*/CJMP_*` 在语义层承接。
- **driver/chir 后端 87 项**、宏展开执行器（`macro_*` 21 项，由 `macro:macro-process` 承担）、IO/cache/interp/incremental 等编译基础设施。

---

## 2. CFIR 中未使用版本门禁的诊断

（门禁正规机制：`LanguageFeature` + `supportsFeature`，见 [LanguageVersionSettings.kt](../../common/src/org/cangnova/cangjie/LanguageVersionSettings.kt)）

### 2.1 门禁缺口（官方首现 > 1.0.0，无任何版本门禁）

| 优先级 | 诊断 | 官方首现 | 报告位置 | 说明 |
|---|---|---|---|---|
| **P0** | COMMON/CJMP 全家族 16 个 | v1.1.0+ | CfirCommonSpecificChecker.kt、CfirCommonCtorImmutableAssignChecker.kt | 仅 `status.isCommon/isSpecific` 触发；语言中无对应 `LanguageFeature` 条目，v1.0.0 下源码含 common/specific 修饰符即报错，与官方不符 |
| P1 | `CFIR_OBJC_POINTER_ARGUMENT_MUST_BE_OBJC_COMPATIBLE`、`CFIR_OBJC_FUNC_ARGUMENT_MUST_BE_OBJC_COMPATIBLE` | v1.1.0 | CfirObjCTypeArgumentChecker.kt:51/63 | type-use 全局触发 |
| P1 | `CFIR_OBJC_FUNC_CALL_PROPERTY_CAN_ONLY_BE_CALLED` | v1.1.0 | CfirObjCCallPropertyChecker.kt:84 | 表达式级触发 |
| P1 | `CFIR_OBJC_CJMAPPING_GENERIC_NOT_SUPPORTED`、`CFIR_OBJC_CJMAPPING_INHERITANCE_INTERFACE_NOT_SUPPORTED` | v1.1.0 | CfirCJMappingCheckers.kt:129/143 | 仅 CJMapping 配置门禁；同文件 `checkCJMappingConfigValid` 已有 `supportsFeature` 先例可复用 |
| P1 | `CFIR_JAVA_MIRROR_INTEROPLIB_MUST_BE_IMPORTED` | v1.1.0 形态 | CfirGeneralSemanticsChecker.kt:206 | 普通 `@Java`（1.0.0 支持）也满足触发条件，interoplib 属 1.1.0 第二代 |
| P2 | `CFIR_UNUSED_IMPORT` | v1.0.2 | CfirImportsChecker.kt:269/279 | WARNING 级一致；官方 1.0.0 不报，严格对齐需钉 1.0.2 |
| P2 | `CFIR_APILEVEL_MISSING_ARG` | v1.0.3-beta | CfirBuiltInAnnotationSemanticsChecker.kt:314 | 同函数 `since` 参数本体已走 `ApiLevelSinceParameter` 门禁，missing-arg 分支漏了 |

### 2.2 门禁不完整（checker 侧级联风险）

| 诊断 | 报告位置 | 问题 |
|---|---|---|
| `CFIR_MISMATCHING_HANDLE_BLOCK` | CfirEffectsExtraChecker.kt:85 | `EffectHandlers` 关闭时 handle body 仍被解析，可级联报出 |
| `CFIR_RETURN_IN_TRY_HANDLE_BLOCK` | CfirEffectsExtraChecker.kt:134、CfirReturnLegalityChecker.kt:34 | 两处均无 `supportsFeature(EffectHandlers)` |

### 2.3 门禁正确（抽样确认）

`CFIR_EXPORT_SAME_PRIVATE_DECL`（`ExportSamePrivateDeclCheck` 1.0.2）、5 个 effect 创建点诊断（`EffectHandlers`@resolve 创建点）、17 个 OBJC 注解身份诊断（`ObjCInteropAnnotations` 1.1.0）、29 个 JAVA 家族（`JavaBuiltinAnnotations` 1.0.0 / `JavaInteropAnnotations` 1.1.0）。

---

## 3. CFIR 多出的诊断（24 个零报告点，精准甄别）

全仓引用扫描（含裸名导入与动态字符串排假）确认以下 24 个诊断在 527 个中**没有任何报告点**且无测试数据引用。甄别后分三类：

### 3.1 未实现（2 个）— 官方现役诊断，CFIR 声明了但检查器从未写出

| 诊断 | 官方对应 | 状态 |
|---|---|---|
| `CFIR_ANNOTATION_ERROR_ARG_RANGE` | `sema_annotation_error_arg_range`（官方 HEAD 仍在） | JFFI 注解参数 range 校验未实现，现有检查只报 `ANNOTATION_ERROR_ARG_NUM` |
| `CFIR_ANNOTATION_ERROR_OBJECT` | `sema_annotation_error_object`（官方 HEAD 仍在） | 注解可修饰目标校验未实现 |

### 3.2 冗余（17 个）— 同语义已由现役诊断精准承载，声明可删

**A. 检查器完整在跑、纯命名错位（3 个，实为"落名不同"）**：

| 多余声明 | 实际承载者 | 证据 |
|---|---|---|
| `CFIR_EXTEND_ORPHAN_RULE` | `CFIR_TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE` | CfirExtendCheckers.kt:244-286（orphan rule 完整实现，报告 :281） |
| `CFIR_EXTEND_SPECIALIZATION_CONFLICT` | `CFIR_EXTEND_DUPLICATE_INTERFACE` | CfirExtendCheckers.kt:426-481 |
| `CFIR_EXTEND_IMMUTABLE_MUT_INTERFACE` | `CFIR_EXTEND_INTERFACE_NOT_EXTENDABLE` | CfirExtendCheckers.kt:315-331 + CfirExtendSemantics.kt:179 |

**B. 场景由现役诊断承载（14 个）**：

| 多余声明 | 承载者 | 证据 |
|---|---|---|
| `CFIR_COMMAND_RESUMPTION_MISMATCH` | `CFIR_TYPE_MISMATCH`（resume-with 类型检查） | CfirEffectsExtraChecker.kt:161-166 |
| `CFIR_RESUME_WRONG_RESUMPTION_TYPE` | `CFIR_RESUMPTION_HANDLE_TYPE_ERROR` | CfirEffectsExtraChecker.kt:60-68 |
| `CFIR_RESUMPTION_INCORRECT_RETURN_TYPE` | `CFIR_MISMATCHING_HANDLE_BLOCK` | CfirEffectsExtraChecker.kt:77-87 |
| `CFIR_EXPLICIT_SUPER_CALL_REQUIRED` | `CFIR_NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS` | CfirConstructorDelegationChecker.kt:191 |
| `CFIR_ILLEGAL_THIS_OR_SUPER_CALL` | `CFIR_ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER` | CfirConstructorDelegationChecker.kt:82 |
| `CFIR_INVALID_INOUT_ARGUMENT` | `CFIR_INOUT_MUST_BE_VAR_VARIABLE` | CfirInoutSemanticsChecker.kt:86/94/137 |
| `CFIR_MISMATCHED_TYPES_MULTIPLE_ASSIGN` | `CFIR_TYPE_MISMATCH`（ConeMismatchedTypesMultipleAssignError 映射） | CfirAssignmentTypeMismatchChecker.kt:55-66、coneDiagnosticToCfirDiagnostic.kt:2707 |
| `CFIR_OVERRIDING_RETURN_TYPE_MISMATCH` | `CFIR_RETURN_TYPE_INCOMPATIBLE`（+`INHERIT_NOT_RETURN_THIS`/`PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF`） | CfirOverrideChecker.kt:301-370 |
| `CFIR_PATTERN_INITIALIZER_TYPE_MISMATCH` | `CFIR_TYPE_MISMATCH`（按官方锚定方式） | CfirPatternVariableInitializerTypeMismatchChecker.kt:31/56 |
| `CFIR_MULTIPLE_CLASS_SUPER_TYPES` | `CFIR_ILLEGAL_MULTI_INHERITANCE` | CfirSupertypesChecker.kt:222 |
| `CFIR_ONLY_ONE_CLASS_BOUND_ALLOWED` | `CFIR_MULTIPLE_CLASS_UPPER_BOUNDS` | CfirTypeParameterBoundsChecker.kt:110-116 |
| `CFIR_REPEATED_BOUND` | 上界去重静默消化 | CfirTypeParameterBoundsChecker.kt:88-91 |
| `CFIR_OVERRIDE_STATIC_ERROR` | `CFIR_STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE` | CfirFunctionSemanticsChecker.kt:165-184 |
| `CFIR_EXTEND_DEFAULT_IMPLEMENTATION_CONFLICT` | `CFIR_INTERFACE_MEMBER_MUST_BE_IMPLEMENTED` | CfirInheritanceDeepChecker.kt:567-624 |

### 3.3 废弃/过期（5 个）— 官方引入后已 revert/移除，CFIR 照抄后未实现

| 诊断 | 官方历史 |
|---|---|
| `CFIR_CANNOT_USE_ANNOTATION_JFFI` | fc4b011f 引入 → 382a2554/14276740 移除 |
| `CFIR_COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH` | fc4b011f 引入 → revert |
| `CFIR_DUPLICATE_INOUT_ARGUMENT` | 官方全历史无此检查（预言性死条目） |
| `CFIR_SPAWN_ARG_NO_EFFECT` | e3200e19 引入 → revert（场景由 `CFIR_SPAWN_ARG_INVALID`/`TYPE_MISMATCH` 承载） |
| `CFIR_USELESS_COMMAND_TYPE` | 1b96c6ec "effect handler removal for release/1.0" 移除 |

---

## 4. 汇总与建议

**总量**：官方 1072 vs CFIR 527。CFIR 职责内真缺失 **37 个**（v1.0.0 语义 35 + v1.1.0 语义 2，另 packages_* 2 个视配置层归属）；多余死声明 **24 个**（未实现 2 / 冗余 17 / 废弃 5）；门禁缺口 **27 个**（COMMON/CJMP 16 + OBJC 5 + JAVA 1 + effect checker 2 + v1.0.2/1.0.3 增量 2 + 平台正交 1）。

**建议优先级**：
1. **P0 门禁**：新增 `CommonSpecificDeclarations(1.1.0)` 之类 `LanguageFeature`，对 COMMON/CJMP 16 个统一门禁（唯一整族超前报错）。
2. **P1 清理**：删除 17 个冗余死声明（其中 3 个 extend 系命名错位可顺手把承载者改名为官方对齐名）；5 个废弃项一并删除。
3. **P1 门禁**：OBJC 5 个 + `JAVA_MIRROR_INTEROPLIB_MUST_BE_IMPORTED` 收窄触发条件；effect checker 侧补 `supportsFeature(EffectHandlers)`。
4. **P2 补齐**：实现 `CFIR_ANNOTATION_ERROR_ARG_RANGE/OBJECT` 两个 JFFI 检查（官方现役）；按 §1.1C 清单分批补 35 个 v1.0.0 细则检查。
5. **P2 决策**：`UNUSED_IMPORT` 是否钉 1.0.2、`APILEVEL_MISSING_ARG` 并入 `ApiLevelSinceParameter`。
