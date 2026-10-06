// RUN_PIPELINE_TILL: FRONTEND
// `foreign func` / `CFunc<…>` 签名规则语义夹具（具名形参、Unit 形参、非 CType 形参、`CFunc` 非函数类型实参、
// 返回类型、`CFunc` 构造器实参个数）。
//
// 取证方式：官方 cjc 1.0.5 与 1.1.3 两版实测，静态库模式
// `cjc <段>.cj -o out.a --output-type=staticlib --diagnostic-format json`；每个 `// FILE:` 段单独编译
// （官方整包编译时一处 parse 错误会吞掉其余段的语义诊断，见 DiagnosticEngine.cpp 的 CanBeEmitted）。
// **两版对本文件全部段的 kind、文案、MainHint.Range 完全一致**（逐段比对），因此不拆 LANGUAGE_VERSION 夹具。
// 1.1.3 本机对含 `@C` struct 的单元在语义检查**之后**的 codegen / Reflection 阶段失败（stdout 无 JSON，stderr 为
// "Reflection format: modifier type is invalid"），语义诊断 JSON 仍完整；这些段在 1.1.3 上 JSON 为空即证明前端零语义错误。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），本文件标记逐字符对齐官方范围。
// 内联标记写法：由"小于感叹号 + 诊断名 + 大于感叹号 + 锚点文本 + 结束标记"五段构成；全文只在 `// FILE:` 段内出现，
// 头部注释一律用文字描述，不出现该序列的字面量。
//
// 官方实现（external/cangjie_compiler，checkout 在 tag v1.0.0）：
//   A. `foreign` 声明的语法前提（parse 阶段，src/Parse/ParseDecl.cpp:1566-1581 `ParseFuncDef` 的 FOREIGN 分支）
//      - :1568-1570 `parse_foreign_func_should_not_be_generic`（锚 `*fb->generic`）—— 泛型 `foreign func`
//        在**语法阶段**就被拒，到不了 Sema 的 CFFI 家族；`sema_cffi_cannot_have_type_param`（CFFICheck.cpp:27-30，
//        锚 `*generic` → `generic->leftAnglePos`）只在 `@C func`（非 foreign）上可达。
//      - :1571-1580 `parse_foreign_func_must_declare_return_type`，Range =
//        `MakeRange(foreignPos->begin, decl.keywordPos + 4)`（`foreign` 修饰符起点 → `func` 关键字之后一位），
//        随后 `decl.EnableAttr(IS_BROKEN)`。所以 **`foreign func` 必须写返回类型**（两版实测 `(1,1)..(1,13)`），
//        这也是官方 `UnsafeCheck` 里 `CJC_NULLPTR_CHECK(fb.retType)`（CFFICheck.cpp:289）安全的原因。
//      - :1581-1584 `parse_foreign_function_with_body`（锚 `*fb->body`）：`foreign func` **不需要**实现体。
//   B. `UnsafeCheck(const FuncBody& fb)`（src/Sema/FFI/CFFICheck.cpp:281-302）只在 `fb.funcDecl` 带 FOREIGN 或 C 属性时进入；
//      唯一调用点 src/Sema/TypeChecker.cpp:266-268（`isCFFIBackend` 分支，在 `CheckBodyRetType` 之后）。进入后：
//      - :286-288 逐个 `CheckCFuncParam(*arg)`（`fb.paramLists[0]->params`）；
//      - :289 `CJC_NULLPTR_CHECK(fb.retType)`；:290-292 `if (!Ty::IsTyCorrect(fb.retType->ty)) return;`
//        —— 返回类型本身类型不合法时不再报本族；
//      - :293-296 `if (!Ty::IsMetCType(*fb.retType->ty)) { DiagnoseRefactor(sema_invalid_cfunc_return_type,
//        *fb.retType, GetFuncBodyRange(fb, *fb.retType)).AddNote("return type is " + …) }` —— **refactor kind**，
//        锚整段返回类型（`GetFuncBodyRange` 见 CFFICheck.cpp:148-155：`MakeRange(type.GetBegin(), type.GetEnd())`）；
//      - :297-300 `else if (Is<VArrayTy>(...)) DiagnoseRefactor(sema_varray_in_cfunc, …)` —— VArray 是 CType
//        （`IsMetCType` 对 VArray 递归到元素，Types.cpp:866-872），所以它落到 **else if** 分支、是**独立** kind，
//        不与 INVALID_CFUNC_RETURN_TYPE 同时出现。
//   C. `CheckCFuncParam(const AST::FuncParam& fp)`（CFFICheck.cpp:305-317）：
//      - :306-307 `if (!Ty::IsTyCorrect(fp.ty)) return;`
//      - :308-310 `if (fp.isNamedParam) diag.Diagnose(fp, DiagKind::sema_cfunc_cannot_have_named_args);` —— `fp` 是
//        FuncParam 节点，经 DiagnosticEngine.h:844-857 `Diagnose(const Node&, kind)` 退化成 `Diagnose(node.GetBegin(), kind)`，
//        再由 :822-824 `Diagnose(pos, kind)` 取 `pos..pos+1`：**锚形参首字符 1 个字符**（具名形参 `name!: T` 的首字符
//        就是形参名 `name`）。**不是 `if/else`**，所以具名形参同时类型不合法时会与下一条叠加（形态 2）。
//      - :311-316 `if (fp.ty->IsUnit()) Diagnose(fp.identifier.Begin(), fp.type->end, sema_cfunc_cannot_have_unit_args)`
//        `else if (!Ty::IsMetCType(*fp.ty)) Diagnose(fp.identifier.Begin(), fp.type->end, sema_invalid_cfunc_arg_type)`
//        → 两条都锚**"形参名起点 → 类型终点"整段**（`fp.type->end` 是类型节点 `end` token，闭区间末尾）。
//      - param 分支的 Unit 是**单独** kind，`sema_invalid_cfunc_arg_type` 不会在 Unit 形参上同时出现。
//      - 语法前提：`isNamedParam` 在 `ParseParameter`（ParseDecl.cpp:1796-1800）里由 `!` 置位，`!` 之后必须紧跟 `:`
//        （:1801-1808 `DiagExpectCharacter("':'")`），所以具名形参**必须是第一个**：`ParseParameterList`
//        （:1890-1899 `if (meetNamedParameter && !param->isNamedParam)`）一旦遇到具名形参，后续无名形参先被
//        `parse_named_parameter_after_unnamed` 拦下、声明 IS_BROKEN，`UnsafeCheck` 不再进入——这正是官方对
//        "具名形参后面还有无名形参"**不报**本族的原因（形态 3）。
//   D. `CheckCFuncParamType(const AST::Type&)`（CFFICheck.cpp:319-325）/ `CheckCFuncReturnType`（:331-340）是 `CFunc<…>`
//      类型实参那条路径：`CheckFuncType`（src/Sema/TypeCheckType.cpp:311-329）在 `ft.isC` 时逐 paramTypes 调
//      `CheckCFuncParamType`（锚 `type.begin..type.end`，即该形参类型节点整段）、对 retType 调 `CheckCFuncReturnType`
//      （refactor kind，锚整个返回类型节点）。`ft.isC` 由 lambda / `CFunc<T>` 语法设置
//      （src/Sema/TypeCheckExpr/NameReferenceExpr.cpp:505 `GetFunctionTy(…, {.isC = true})`）；
//      **普通函数类型 `(Int64) -> Unit` 的 `isC` 为假**，故形态 13（`CFunc<(Unit) -> Unit>`）只报 Unit 族一条。
//   E. `CFunc<非函数类型>`：`GetTyFromASTCFuncType`（src/Sema/PreCheck.cpp:431-441，**类型**位置，即形参 / 变量 /
//      返回类型上的 `CFunc<…>`）与 `InferCFuncExpr`（src/Sema/TypeCheckExpr/NameReferenceExpr.cpp:481-497，
//      **表达式**位置，即 `CFunc<…>(…)` 构造器）各有一份：`typeArguments.size() != 1` 报
//      `sema_generic_argument_no_match`（refactor kind，锚 `MakeRange(rt.GetBegin(), rt.GetEnd())` 整个引用）；
//      实参 DynamicCast 到 `FuncType` 失败时
//      `diag.Diagnose(rt.typeArguments[0]->GetBegin(), rt.typeArguments[0]->GetEnd(), DiagKind::sema_cfunc_type)`
//      —— **锚类型实参整段**（形态 2 / 3 / 4）。
//   F. `ChkCFuncConstructorExpr`（src/Sema/TypeCheckExpr/TypeConvExpr.cpp:34-78）：`ce.baseFunc` 必须是 RefExpr 且
//      `Synthesize` 后 `ce.ty->IsCFunc()`；`ce.args.size() != 1` 时 :48-52
//      `diag.Diagnose(*ce.baseFunc, DiagKind::sema_cfunc_too_many_arguments)` —— 退化 1 字符，锚 `CFunc` 的**首字母**
//      （**不含类型实参与实参表**），随后 `ce.ty = InvalidTy; return false`；实参恰 1 个但不是指针时 :77
//      `diag.Diagnose(*ce.args[0], DiagKind::sema_cfunc_ctor_must_be_cpointer)`（锚整个实参表达式）；
//      具名实参（:59-65）报 `sema_unknown_named_argument`（锚实参名）。官方对"0 个"与"2 个以上"用**同一个** kind
//      （文案写的是 "too many"，0 个时文案不变）。两版在本轮实测形态上行为一致。
//   G. `Ty::IsMetCType`（src/AST/Types.cpp:865-872）：`VArray<…>` 递归到元素；其余为
//      `IsPrimitiveCType || IsPointer || IsCString || IsCFunc || IsCStructType || kind == TYPE_QUEST`。
//      `IsPrimitiveCType`（:843）= `IsPrimitive(ty) && !ty.IsCString()`。故 **primitive / `CPointer<T>` / `CString` /
//      `CFunc<…>` / `@C` struct / `VArray<@C struct,…>`** 是合法 CType；`String` / `Rune` / `?T` / 元组 / 普通函数类型 /
//      普通 struct 不是（`?T` 脱糖后是 `Enum-Option`，见形态 12 的官方 note）。
//   H. 文案：include/cangjie/Basic/DiagnosticSema.def :186 `sema_cfunc_cannot_have_named_args`
//      "CFunc cannot have named arguments"；:184 `sema_cfunc_cannot_have_unit_args`
//      "CFunc cannot have arguments of type Unit"；:177 `sema_invalid_cfunc_arg_type`
//      "arguments type of CFunc must be instantiated with CType"；:75 `sema_cfunc_too_many_arguments`
//      "too many arguments given to CFunc constructor, only accept 1 argument"；:128 `sema_cfunc_ctor_must_be_cpointer`；
//      :178 `sema_cfunc_type` "cfunc type must be a function type"；:100 `sema_unknown_named_argument`；
//      include/cangjie/Basic/DiagRefactor/DiagnosticSema.def :221 `sema_invalid_cfunc_return_type`
//      "return type of CFunc must be instantiated with CType"、:217 `sema_varray_in_cfunc`；
//      include/cangjie/Basic/DiagRefactor/DiagnosticParser.def :123 `parse_named_parameter_after_unnamed`
//      "unnamed parameters must come before named parameters"（MainHint "unexpected unnamed parameter here" +
//      note "because it must come before this named parameter"）。本文件涉及的 `parse_named_parameter_after_unnamed`、
//      `sema_varray_in_cfunc` 是 refactor kind（锚整段），其余是普通 kind（1 字符或显式 Range）。
//
// 本项目 CFIR 名（cfir/checkers/checkers-component-generator/src/org/cangnova/cangjie/cfir/checkers/generator/
// diagnostics/CfirDiagnosticsList.kt，生成物 cfir/checkers/gen/.../diagnostics/CfirErrors.kt）：
//   :673 CFUNC_CANNOT_HAVE_NAMED_ARGS   ← 官方 sema_cfunc_cannot_have_named_args
//   :674 CFUNC_CANNOT_HAVE_UNIT_ARGS    ← 官方 sema_cfunc_cannot_have_unit_args
//   :683 CFUNC_TYPE                     ← 官方 sema_cfunc_type（CjTypeReference 载体）
//   :684 CFUNC_TOO_MANY_ARGUMENTS       ← 官方 sema_cfunc_too_many_arguments
//   :685 CFUNC_CTOR_MUST_BE_CPOINTER    ← 官方 sema_cfunc_ctor_must_be_cpointer
//   :688 INVALID_CFUNC_RETURN_TYPE      ← 官方 sema_invalid_cfunc_return_type（CjTypeReference + actualType 参数）
//   :692 INVALID_CFUNC_PARAMETER_TYPE   ← 官方 sema_invalid_cfunc_arg_type（CjTypeReference + actualType 参数）
//   :699 CFUNC_VAR_CANNOT_HAVE_VAR_PARAM、:701 ONLY_CFUNC_CAN_USE_ANNOTATION、:682 CFFI_CANNOT_HAVE_TYPE_PARAM、
//   :64 REF_NOT_BE_TYPE、:1426 GENERIC_ARGUMENT_NO_MATCH、VARRAY_IN_CFUNC（生成物 CfirErrors.kt:474）；
//   `parse_named_parameter_after_unnamed` 是 parser 诊断表内的同名诊断（PARSE_NAMED_PARAMETER_AFTER_UNNAMED）。
// 文案表 cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt:502
//   "CFunc cannot have named arguments"（与官方逐字一致）、:503 Unit 族、:508 CFUNC_TYPE（官方 :178 首字母小写
//   "cfunc type"，CFIR 首字母大写）、:509 CFUNC_TOO_MANY_ARGUMENTS "CFunc constructor requires exactly one argument"
//   （官方 :75 文案不同）、:510 CTOR_MUST_BE_CPOINTER、:512-518 INVALID_CFUNC_PARAMETER_TYPE
//   "arguments type of CFunc must be instantiated with CType, but ''{0}'' was found."、INVALID_CFUNC_RETURN_TYPE（:451 附近）、
//   :1128 VARRAY_IN_CFUNC（与官方逐字一致）。
// 生产者：
//   CFUNC_CANNOT_HAVE_NAMED_ARGS / CFUNC_CANNOT_HAVE_UNIT_ARGS / INVALID_CFUNC_PARAMETER_TYPE →
//     cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirForeignFunctionReturnTypeChecker.kt:25-46
//     `object CfirForeignFunctionParameterTypeChecker`（`:33-45` 的 for 循环；:34 named、:36 Unit、:42 非 CType）；
//     注册点同目录 ../CommonDeclarationCheckers.kt:77 `functionCheckers`。
//   INVALID_CFUNC_RETURN_TYPE → 同文件 :13-22 `object CfirForeignFunctionReturnTypeChecker`
//     （`reportOn(ref.source, …, type)`，`ref` = `declaration.returnTypeRef`）。
//   `CFunc<…>` 类型实参内部的同名诊断 → 同文件 :50-98 `CfirCFuncTypeLegalityReporter.reportNestedDiagnosticsIfCFunc`
//     （:64 CFUNC_TYPE、:72 Unit、:74 非 CType、:80 INVALID_CFUNC_RETURN_TYPE、:82 VARRAY_IN_CFUNC）。
//   CFUNC_TOO_MANY_ARGUMENTS / CFUNC_CTOR_MUST_BE_CPOINTER → cone 层映射：
//     cfir/resolve/src/.../calls/stages/CfirMapArguments.kt:206 `BuiltinCFuncConstructorTooManyArguments(calleeSource)`
//     （错误类 cfir/semantics/src/.../diagnostic/ResolutionDiagnostic.kt:251，字段注释即写明"官方 `CFunc` callee source，
//     不包含调用参数表"），由 cfir/checkers/src/.../diagnostics/coneDiagnosticToCfirDiagnostic.kt:737-739
//     映射为 `CFUNC_TOO_MANY_ARGUMENTS.on(rootCause.source, session)`。
//   `parse_named_parameter_after_unnamed` 由官方 Parser 在 PreCheck 之前报告；本项目由 psi 层同名诊断产出，
//     `RUN_PIPELINE_TILL: FRONTEND` 内可见。
//
// 按源码可预判的锚点差异（**未跑 CFIR**，只写源码能直接看出的；CFIR 与官方不一致的缺口由主会话记入 README）：
//   (1) CFUNC_CANNOT_HAVE_NAMED_ARGS：官方锚**形参首字符 1 字符**（`Diagnose(fp)` 退化）；CFIR 的
//        `reportOn(parameter.source, …)` 用的是形参**整个 source**（`bad!: String` 整段）——形态 2 / 5 / 8 / 9 预期
//        锚点偏宽。
//   (2) CFUNC_CANNOT_HAVE_UNIT_ARGS：官方锚 `identifier.Begin()..type->end`（`a: Unit`）；CFIR 同文件 :36-41 用
//        `CjOffsetsOnlySourceElement(nameSource.startOffset, typeSource.endOffset)`，源码上与官方一致（含形参名、
//        不含默认值）——形态 5 / 8 预期同锚（形态 13 官方锚类型节点，CFIR 用 `ref.source` 同锚）。
//   (3) INVALID_CFUNC_PARAMETER_TYPE / INVALID_CFUNC_RETURN_TYPE：官方锚"形参名→类型终点"整段 / 返回类型节点整段；
//        CFIR 用 `ref.source`（类型引用，不含形参名）——形态 2 / 9 / 10 预期锚点**偏窄**（缺形参名 `bad: ` /
//        `text: `）；形态 7 / 12 预期同锚。
//   (4) CFUNC_TOO_MANY_ARGUMENTS：官方锚 `CFunc` 首字母 1 字符；CFIR 的 cone 层 `calleeSource` 取
//        `CfirFunctionCall.calleeReference.source`（LLT cfuncConstructorArgumentsNegative.cj 已按"整个 `CFunc<() -> Unit>`"
//        写标记）——形态 15 / 16 / 17 预期锚点偏宽。
//   (5) CFUNC_TYPE：官方锚 `CFunc<…>` 的**类型实参整段**（PreCheck.cpp:440 / NameReferenceExpr.cpp:493 显式
//        `GetBegin()..GetEnd()`）；CFIR 的 `CfirCFuncTypeLegalityReporter`（同文件 :60-66）在 `function == null` 时
//        `reportOn(cfuncArgument.source ?: resolvedTypeRef.source, CFUNC_TYPE)`，`cfuncArgument` 就是那个类型实参，
//        源码上与官方同锚；且它只在 `extractCFuncArgument` 成功（`isCFunc` 为真或 `lookupTag == CFunc`）时进入，
//        官方则对**任何** `CFunc<非函数类型>` 报——形态 16（`CFunc<CFunc<…>>`）CFIR 若能取到内层实参会**少报**，
//        形态 17 若 `lookupTag` 不保留则会**漏报**，两处都记为可预判差异。
//   (6) 条件差异：官方 `UnsafeCheck` 的触发条件是 `fb.funcDecl->TestAttr(FOREIGN) || TestAttr(C)`；CFIR 是
//        `declaration.status.isC`。`foreign` 是否隐式带 C 取决于 `SetForeignABIAttr`（CFFICheck.cpp:178-189，
//        cjnative backend 下 `foreign` 默认 `Attribute::C`），源码上等价。
//   (7) 报告次数：官方对同一 `foreign func` 的每个形参各报一次（形态 2 两条、形态 8 四条、形态 9 三条）；CFIR 同样逐
//        `declaration.valueParameters` 循环（:33），条数一致。
//   (8) 形态 3 的 parse 诊断来自 Parser（官方 PreCheck 之前）；CFIR 侧走 psi 层同名诊断，
//        `Diagnose(*ce.baseFunc)`（构造器一条）与 `Diagnose(fp)`（具名一条）都是 1 字符退化，
//        CFIR 的 `parameter.source` / `calleeReference.source` 都比官方宽，属 (1) / (4)。

//
// 已有覆盖、本文件不重复（testData 下已 grep 确认）：
//   - INVALID_CFUNC_PARAMETER_TYPE / INVALID_CFUNC_RETURN_TYPE 的**声明级 `foreign func`** 形态已由
//     diagnostics2/interop/foreignFunctionParameterTypePlaceholder.cj（`CType` / `String` / 元组形参）、
//     foreignFunctionReturnType.cj、foreignFunctionCFuncLegalityPlaceholder.cj（形参 + 返回同行）、
//     foreignFunctionMixedSignatureLegality.cj（多形参 + 返回）、foreignFunctionTypeAliasExpansion.cj（类型别名）覆盖；
//     旧 diagnostics/interop/foreignFunctionReturnTypeRich.cj 同名覆盖返回形态。本文件**不**再写"foreign func 形参 /
//     返回类型上的 String / 元组 / CType"这几种最基本形态，只写它们没覆盖的形态（见形态 7 / 9 / 10 / 12 / 13）。
//   - `CFunc<…>` 类型实参内的 INVALID_CFUNC_PARAMETER_TYPE（String 形参）/ INVALID_CFUNC_RETURN_TYPE（String 返回）
//     / CFUNC_CANNOT_HAVE_UNIT_ARGS / VARRAY_IN_CFUNC 已由 llt/ffi/semantic_contract/cfuncSignaturesNegative.cj 覆盖
//     （`type UnitParameter`、`type StringParameter`、`type StringReturn`、`type VArrayReturn`）；
//     本文件形态 6 / 7 / 12 只补 `foreign func` 宿主 + 形参类型位置这条路径。
//   - `foreign func` 宿主上的 INVALID_CFUNC_PARAMETER_TYPE / RETURN_TYPE / VARRAY_IN_CFUNC 已由
//     llt/ffi/semantic_contract/ctypeParametersNegative.cj 覆盖（含 String / Rune / Float16 / Option / CType 接口 /
//     普通函数类型 / `foreign func returnArray(): VArray<Int64, $2>` 的 VARRAY_IN_CFUNC）、llt/native/c_abi_unit.cj
//     （CFUNC_CANNOT_HAVE_UNIT_ARGS 两条）、llt/native/foreign_2.cj（泛型 class 实例参数）、llt/array/array_builin_ctype.cj、
//     macro/llt/varray/varray_cffi/varray_ctype03.cj（`VArray<A, $3>` 形参）。本文件形态 8–13 与这些形态类型不同
//     （Rune 形参、别名形参、`CFunc` 内部、`CFunc` 返回 `VArray`、普通函数类型形参、Option 返回）。
//   - CFUNC_CANNOT_HAVE_NAMED_ARGS 在本文件之前**只**出现在 llt/ffi/semantic_contract/ctypeParametersNegative.cj:8
//     （`foreign func rejectNamed(value!: Int64): Unit` 单点，且**没有任何调用点**：本轮用 cjc 两版实测，
//     孤立的具名 `foreign` 声明零诊断（官方 `HasPrevDiag`），具名诊断只在同段还有别的 Sema 错误时出现——
//     所以该 LLT 标记实际未被触发验证）；diagnostics2 下无覆盖——形态 2 / 5 / 8 / 9 是本文件的主要增量。
//   - CFUNC_TOO_MANY_ARGUMENTS 在本文件之前**只**出现在 llt/ffi/semantic_contract/cfuncConstructorArgumentsNegative.cj:2,4
//     （`main()` 体内零参 / 双参两种）；diagnostics2 下无覆盖——形态 14 / 15 是增量。
//   - CFUNC_TYPE 只在 llt/ffi/semantic_contract/cfuncNonFunctionTypeNegative.cj（类型别名位置，`type NonFunction =
//     CFunc<Int64>`，标记锚类型实参 `Int64`）；本文件形态 3 补**形参位置**，形态 4 补嵌套 `CFunc` 位置。
//   - CFUNC_CTOR_MUST_BE_CPOINTER 只在 llt/ffi/semantic_contract/cfuncConstructorArgumentsNegative.cj:3；本文件不收。
//   - `extend` / `operator` 里的 CFunc 签名：官方 `CheckFuncType` 只在 `ft.isC` 时走 CFFI 分支，而 `extend` 成员的返回
//     类型是普通函数类型（`isC` 为假），**没有可达的 CFFI 诊断**；`operator` 成员另被 `PreCheckExtend` 检查
//     （src/Sema/TypeCheckExtend.cpp:307-317 对 `@C` struct 先报 sema_c_type_cannot_extend_interface）。见"已删除的形态 6"。
//   - `@C` struct 成员 / `@C` 泛型函数的 CFFI 族已由 llt/ffi/semantic_contract/cstructFieldsNegative.cj
//     （ILLEGAL_MEMBER_OF_CSTRUCT / CSTRUCT_CANNOT_HAVE_UNIT_FIELDS）与 annotationTargetsNegative.cj
//     （CFFI_CANNOT_HAVE_TYPE_PARAM）覆盖，本文件不收。
//   - UNSAFE_FUNCTION_INVOKE_FAILED（调用 `CFunc` 值 / `foreign` 函数需 `unsafe` 上下文，官方 CheckUnsafeInvoke，
//     CFFICheck.cpp:249-259，锚整个 CallExpr）属另一族，已由 llt/ffi/semantic_contract/unsafeFunctionInvokeNegative.cj
//     覆盖；形态 18 用 `unsafe` 上下文避开。
//   - GENERIC_ARGUMENT_NO_MATCH（官方 `CFunc<T>` 类型实参个数 ≠ 1）在本 testData 下无覆盖，但需要
//     `CFunc<>` 这种空类型实参列表语法，属 parser 族，本文件不收（见"已删除的形态 10"）。
//
// 本文件的 SUGGESTED 段共 0 个：形态 2 / 3 / 5 / 8 / 9 的官方诊断已全部逐字符锚定（'构造器双参 + @C struct' 那段最终没写，见下方形态清单）；
// 形态 18 / 19 / 20 是合法对照段（两版零诊断）；形态 1 / 2 / 3 / 5 是具名形参的四种位置形态
// （形态 1 诊断落在**第一个**形参，形态 2 / 5 落在第二个形参，形态 3 的具名形参后面还有无名形参、
// 被 parser 先拦下所以本族零诊断）。

// ── 形态 1：孤立具名形参（`value!: Int64`）—— 本族诊断的最小形态，锚点在**第一个**形参上
// kind：sema_cfunc_cannot_have_named_args "CFunc cannot have named arguments"（普通 kind，
// `Diagnose(fp, …)` → 形参首字符 1 个字符，CFFICheck.cpp:308-310）；Range (1,27)..(1,28)，1.0.5 / 1.1.3 一致。
// 源码依据：`CheckCFuncParam` 对 `fp.isNamedParam` 无条件报，本形态没有别的 Sema 错误时**同样**报
// （`HasPrevDiag` 只吞同起点同文案的重复诊断，这里是首条）。探针 cs01 / 主会话 r10 / s1 复核。
// FILE: cfuncForeignNamedArgumentAlone.cj
foreign func csNamedAlone(<!CFUNC_CANNOT_HAVE_NAMED_ARGS!>value!: Int64<!>): Unit
// ── 形态 2：具名形参（`name!: T`）+ 第二个形参是非 CType —— 官方**恰好两条** Sema 诊断
// 诊断 1 sema_cfunc_cannot_have_named_args 锚 `b` 1 字符；诊断 2 sema_invalid_cfunc_arg_type
// 锚 `bad: String` 整段。1.0.5 / 1.1.3 一致。探针 cs02。CFIR 的锚点差异见头部 (1) / (3)。
// FILE: cfuncForeignNamedArgumentWithNonCType.cj
foreign func csNamedFirst(a: Int64, <!INVALID_CFUNC_PARAMETER_TYPE!><!CFUNC_CANNOT_HAVE_NAMED_ARGS!>b<!>ad!: String<!>): Unit
// ── 形态 3：具名形参在**第二位** —— parser 先报 parse_named_parameter_after_unnamed（锚 `c: Unit` 整段），声明 IS_BROKEN，本族零诊断
// 报告点 ParseParameterList（ParseDecl.cpp:1890-1899 `if (meetNamedParameter && !param->isNamedParam)`）→
// ParserDiag.cpp:1250-1255；文案 DiagnosticParser.def:123。`b!: Int64`（第二个形参类型合法）是让 parser 诊断出现的必要搭配：
// 若第二个形参本身也非 CType，两条 parse / sema 诊断会同时出现（探针 cs03 对照）。1.0.5 / 1.1.3 一致。探针 cs04b。
// CFIR 在这条 parser 已判 IS_BROKEN 的声明上仍产出 CFUNC_CANNOT_HAVE_NAMED_ARGS / CANNOT_HAVE_UNIT_ARGS（误报，见 README）。
// FILE: cfuncForeignNamedArgumentAfterUnnamed.cj
foreign func csNamedAfterUnnamed(a: Int64, b!: Int64, c: Unit): Unit
// ── 形态 4：Unit 形参（锚 `a: Unit` 整段，不是类型节点）
// kind：sema_cfunc_cannot_have_unit_args "CFunc cannot have arguments of type Unit"；锚 `a: Unit`。探针 cs05。
// FILE: cfuncForeignUnitArgument.cj
foreign func csUnitArgument(<!CFUNC_CANNOT_HAVE_UNIT_ARGS!>a: Unit<!>): Unit
// ── 形态 5：具名 Unit 形参（两条同锚点：具名 1 字符 + Unit 整段），锚点落在**第二个**形参
// 诊断 1 锚 `b` 1 字符；诊断 2 锚 `bad!: Unit` 整段（含形参名，`fp.identifier.Begin()..fp.type->end`）。
// 两条 Range 同起点，标记写成嵌套：外层 CANNOT_HAVE_UNIT_ARGS 从形参名起，内层 CANNOT_HAVE_NAMED_ARGS 只包首字符。
// 1.0.5 / 1.1.3 一致。探针 cs05b / 主会话 s5（Range (1,36)..(1,37) 与 (1,36)..(1,46)）。
// FILE: cfuncForeignNamedUnitArgument.cj
foreign func csNamedUnit(a: Int64, <!CFUNC_CANNOT_HAVE_UNIT_ARGS!><!CFUNC_CANNOT_HAVE_NAMED_ARGS!>b<!>ad!: Unit<!>): Unit
// ── 形态 6：`CFunc` 形参的返回类型是 `VArray`（VArray 是 CType ⇒ 报 VARRAY_IN_CFUNC 而不是 INVALID_CFUNC_RETURN_TYPE）
// kind：sema_varray_in_cfunc "return type of CFunc cannot be 'VArray' type"（refactor kind，DiagRefactor/DiagnosticSema.def:217，
// 锚整个返回类型节点）；CFIR 名 VARRAY_IN_CFUNC（生成物 CfirErrors.kt:474，文案表
// CfirErrorsDefaultMessages.kt:1128 与官方逐字一致）。该名字已由 llt/ffi/semantic_contract/cfuncSignaturesNegative.cj
// 的 `type VArrayReturn` 覆盖（别名位置），本段补 `foreign func` 宿主 + 形参类型位置这条路径。探针 cs06。
// FILE: cfuncForeignCFuncParameterVArrayReturn.cj
foreign func csVArrayReturn(cb: CFunc<() -> <!VARRAY_IN_CFUNC!>VArray<Int64, $2><!>>): Unit
// ── 形态 7：`CFunc` 形参的形参类型是 `String`（`CheckCFuncParamType` 锚类型节点整段，**不含形参名**）
// kind：sema_invalid_cfunc_arg_type；锚 `String`。与 `foreign func` 的 `CheckCFuncParam` 锚 `name: String` 整段形成对照。探针 cs06b。
// FILE: cfuncForeignCFuncParameterStringArgument.cj
foreign func csStringArgument(cb: CFunc<(<!INVALID_CFUNC_PARAMETER_TYPE!>String<!>) -> Unit>): Unit
// ── 形态 8：四个形参各触发一条（非 CType + Unit + 合法 + 具名 Unit），验证"逐形参各报一次"
// 诊断 1 锚 `b: Rune`（Range (3,5)..(3,12)）；诊断 2 锚 `c: Unit`（(4,5)..(4,12)）；诊断 3 锚 `e` 1 字符（(6,5)..(6,6)）；
// 诊断 4 锚 `end!: Unit` 整段（(6,5)..(6,15)）。诊断 3 / 4 同起点，嵌套标记。1.0.5 / 1.1.3 一致。
// 探针 cs08 / 主会话 s8（带尾随逗号与不带，Range 相同）。
// FILE: cfuncForeignFourArgumentKinds.cj
foreign func csFourKinds(
    a: Int64,
    <!INVALID_CFUNC_PARAMETER_TYPE!>b: Rune<!>,
    <!CFUNC_CANNOT_HAVE_UNIT_ARGS!>c: Unit<!>,
    d: Int64,
    <!CFUNC_CANNOT_HAVE_UNIT_ARGS!><!CFUNC_CANNOT_HAVE_NAMED_ARGS!>e<!>nd!: Unit<!>,
): Unit
// ── 形态 9：非 CType 形参与具名形参**混排**（Rune + 具名 Rune）——三条 Sema 诊断
// 诊断 1 锚 `b: Rune`；诊断 2 锚 `c` 1 字符；诊断 3 锚 `cr!: Rune` 整段（含形参名）。
// 诊断 2 / 3 同起点，嵌套标记。1.0.5 / 1.1.3 一致。探针 cs09 / 主会话 s9（Range (4,5)..(4,6) 与 (4,5)..(4,14)）。
// FILE: cfuncForeignNonCTypeAndNamedArgument.cj
foreign func csMixed(
    a: Int64,
    <!INVALID_CFUNC_PARAMETER_TYPE!>b: Rune<!>,
    <!INVALID_CFUNC_PARAMETER_TYPE!><!CFUNC_CANNOT_HAVE_NAMED_ARGS!>c<!>r!: Rune<!>,
): Unit
// ── 形态 10：形参类型是**类型别名**指向非 CType（锚仍落在形参处的别名名）
// kind：sema_invalid_cfunc_arg_type；锚 `text: CSRSpecText`。别名先在 `CheckTypeAlias` 合成。1.0.5 / 1.1.3 一致。探针 cs10。
// FILE: cfuncForeignAliasArgument.cj
type CSRSpecText = String

foreign func csAliasArgument(<!INVALID_CFUNC_PARAMETER_TYPE!>text: CSRSpecText<!>): Unit
// ── 形态 11：`foreign func` 的返回类型是 `Option`（refactor kind，锚整段返回类型，带官方 note "return type is Enum-Option"）
// kind：sema_invalid_cfunc_return_type "return type of CFunc must be instantiated with CType"；锚 `?Int64`。探针 cs06c。
// FILE: cfuncForeignOptionReturn.cj
foreign func csOptionReturn(): <!INVALID_CFUNC_RETURN_TYPE!>?Int64<!>
// ── 形态 12：`CFunc` 形参内部的 `Unit` 形参类型（`CheckCFuncParamType` 锚类型节点）
// kind：sema_cfunc_cannot_have_unit_args；锚类型实参里的 `Unit`。1.0.5 / 1.1.3 一致。探针 cs06d。
// FILE: cfuncForeignCFuncParameterUnitArgument.cj
foreign func csCFuncUnit(cb: CFunc<(<!CFUNC_CANNOT_HAVE_UNIT_ARGS!>Unit<!>) -> Unit>): Unit
// ── 形态 13：`foreign func` 形参是非 CFunc 的**普通函数类型**（`isC` 为假 ⇒ 只报 arg_type 一条，无 Unit 族叠加）
// kind：sema_invalid_cfunc_arg_type；锚 `f: (Int64) -> Unit`。1.0.5 / 1.1.3 一致。探针 cs06e。
// FILE: cfuncForeignPlainFunctionTypeArgument.cj
foreign func csPlainFunc(<!INVALID_CFUNC_PARAMETER_TYPE!>f: (Int64) -> Unit<!>): Unit
// ── 形态 14：`CFunc` 构造器零实参（锚 `CFunc` 的首字母 1 字符，**不含**类型实参与实参表）
// kind：sema_cfunc_too_many_arguments "too many arguments given to CFunc constructor, only accept 1 argument"
//（官方 0 个实参也用这一个 kind，文案里的 "too many" 是官方原文）。探针 cs08a。
// FILE: cfuncConstructorNoArgument.cj
func csCtorNone(): Int64 {
    let missing = <!CFUNC_TOO_MANY_ARGUMENTS!>C<!>Func<() -> Unit>()
    return 1
}
// ── 形态 15：`CFunc` 构造器两个实参（与形态 14 同一 kind；合法对照见形态 18）
// kind：sema_cfunc_too_many_arguments；锚 `C` 的首字母 1 字符。探针 cs08b。
// FILE: cfuncConstructorTwoArguments.cj
func csCtorTwo(): Int64 {
    let extra = <!CFUNC_TOO_MANY_ARGUMENTS!>C<!>Func<(Int64) -> Unit>(CPointer<Int64>(), CPointer<Int64>())
    return 1
}
// ── 形态 16：`CFunc<CFunc<(Int64) -> Unit>>` 形参——内层实参不是 `FuncType`，官方在 PreCheck 报 CFUNC_TYPE 整段
// kind：sema_cfunc_type "cfunc type must be a function type"（普通 kind；`Diagnose(begin, end)` 显式 Range，锚类型实参整段）。
// 报告点 PreCheck.cpp:438-441 `GetTyFromASTCFuncType`；实参是 `RefType`（CFunc 再次应用）而非 `FuncType`，DynamicCast 失败。探针 cs04。CFIR 差异见头部 (5)。
// FILE: cfuncForeignNestedCFuncParameter.cj
foreign func csNestedCFunc(cb: CFunc<<!CFUNC_TYPE!>CFunc<(Int64) -> Unit><!>>): Unit
// ── 形态 17：`CFunc<Int64>` 形参——实参是 primitive 类型引用（不是函数类型），CFUNC_TYPE 锚 `Int64`
// 与 LLT cfuncNonFunctionTypeNegative.cj 的别名位置对照。1.0.5 / 1.1.3 一致。探针 cs07。
// FILE: cfuncForeignNonFunctionTypeArgument.cj
foreign func csNonFunc(cb: CFunc<<!CFUNC_TYPE!>Int64<!>>): Unit
// ── 形态 18：合法对照 L1——`CFunc<…>(CPointer<…>)` 恰一个指针实参 + `unsafe` 上下文调用（零诊断）
// 源码依据：TypeConvExpr.cpp:54-71 实参恰 1 且 `IsPointer()` ⇒ 不报任何 CFFI 族；
// `CheckUnsafeInvoke`（CFFICheck.cpp:249-259）要求调用点带 UNSAFE，故本段整体放进 `unsafe`。探针 cs09。
// FILE: legalCFuncConstructorSinglePointer.cj
func csLegalCtor(): Int64 {
    unsafe {
        let callback: CFunc<(Int64) -> Unit> = CFunc<(Int64) -> Unit>(CPointer<Int64>())
        callback(1)
    }
    return 1
}
// ── 形态 19：合法对照 L2——`foreign func` 的完整 CType 形参 / 返回矩阵（零诊断）
// 源码依据：`IsMetCType`（Types.cpp:865-872）+ `IsPrimitiveCType`（:843）+ `IsCStructType`（:853-858）。探针 cs10。
// 1.1.3 在 codegen / Reflection 阶段对含 `@C` struct 的单元失败，语义 JSON 为空。
// FILE: legalCFuncForeignSignatureMatrix.cj
@C
struct CSRLegalRecord {
    var value: Int64 = 0
}

type CSRLegalAddress = CPointer<Int64>
type CSRLegalCallback = CFunc<(Int64) -> Unit>

foreign func csLegalMatrix(
    flag: Bool,
    i8: Int8, i16: Int16, i32: Int32, i64: Int64, ni: IntNative,
    u8: UInt8, u16: UInt16, u32: UInt32, u64: UInt64, nu: UIntNative,
    f32: Float32, f64: Float64,
    address: CSRLegalAddress,
    text: CString,
    callback: CSRLegalCallback,
    record: CSRLegalRecord,
): CPointer<CSRLegalRecord>

foreign func csLegalReturnScalar(): Int64

foreign func csLegalReturnText(): CString
// ── 形态 20：合法对照 L3——`@C` struct 形参（`IsCStructType` 为真即满足 `IsMetCType`，零诊断）
// 1.0.5 零诊断；1.1.3 在 codegen / Reflection 阶段失败，语义 JSON 为空。探针 cs11。
// FILE: legalCFuncForeignCStructParameter.cj
@C
struct CSRLegalPoint {
    var x: Int32 = 0
    var y: Int32 = 0
}

foreign func csLegalCStruct(p: CSRLegalPoint): Unit

// 已删除的形态（取证 kind 与 Range 见下；1.0.5 与 1.1.3 两版一致，除注明外）：
//   1. 具名形参被调用（`q06(value: 1)` / `q07(1)`）：官方两条**调用**诊断——sema_unknown_named_argument
//      （"unknown named argument prefix '%s:'"，锚实参名）与 sema_need_named_argument（锚实参），属"实参名"族，
//      与本族的具名诊断正交；同段本族诊断照常出现。探针 cs12 / cs13。
//   2. `foreign func csRet() {}`（省略返回类型）：官方 parse_foreign_func_must_declare_return_type
//      （ParseDecl.cpp:1571-1580，锚 `foreign func` 整段）在 Parser 就把声明标 IS_BROKEN，`fb.retType` 保持 null，
//      所以官方 `UnsafeCheck` 的 `CJC_NULLPTR_CHECK(fb.retType)`（CFFICheck.cpp:289）在这条路径上到不了；
//      同文件 :1581-1584 另报 parse_foreign_function_with_body（锚 `{}` 整段）——**Sema 的 CFFI 家族一条不报**。
//      该形态按 parser 族处理，本文件不单列。探针 cs20。
//   3. `foreign func` 的形参**省略类型** `foreign func csP(a)`：ParseParameter（ParseDecl.cpp:1801-1808）在 `:` 缺失
//      时 `DiagExpectCharacter("':'")` 并置 HAS_BROKEN，本族不报。`foreign func` 声明**不需要**实现体
//      （1.0.5 / 1.1.3 均接受 `foreign func f(): Unit`）。探针 cs21。
//   4. 具名形参写成 `v! Int64`（`!` 后漏冒号）：ParseDecl.cpp:1801 `DiagExpectCharacter("':'")` 报
//      parse_expected_character（锚 `alue`），形参 HAS_BROKEN ⇒ `!fb->retType` ⇒ 再报
//      parse_foreign_func_must_declare_return_type（锚 `foreign func`）；本族零诊断。同段共 5 条 parse 诊断。探针 cs22。
//   5. `CFunc<() -> Unit>(p: CPointer<Unit>())`（给构造器实参加名）：官方 `ChkCFuncConstructorExpr` 的具名实参
//      分支（TypeConvExpr.cpp:59-65）报 `sema_unknown_named_argument`（DiagnosticSema.def:100，
//      "unknown named argument prefix '%s:'"，锚实参名 `p`），属"调用实参名"族，与 CFUNC_TOO_MANY_ARGUMENTS 互斥
//      （实参数仍为 1）。探针 cs23。
//   6. `extend` / `operator` 成员里的 CFunc 签名：官方 `CheckFuncType` 的 CFFI 分支只在 `ft.isC` 时进入，而
//      `extend` 成员的函数类型 `isC` 为假（只有 lambda 与 `CFunc<T>` 语法置 isC，见
//      src/Sema/TypeCheckExpr/NameReferenceExpr.cpp:505 与 src/Sema/TypeCheckExpr/LambdaExpr.cpp:357），
//      所以"extend 成员返回 `CFunc<(String) -> Unit>`"不产生任何 CFFI 族诊断；`extend @C struct` 另被
//      PreCheckExtend（TypeCheckExtend.cpp:307-317）拦下。不可达。探针 cs24。
//   7. `CFunc` 的具名形参语法 `CFunc<(value!: Int64) -> Unit>`：函数**类型**的形参位置是类型列表，官方
//      ParseFuncParam 只在声明形参列表里处理 `!`（src/Parse/ParseDecl.cpp:1796-1800），故 CFUNC_CANNOT_HAVE_NAMED_ARGS
//      在 `CFunc<…>` 类型位置不可达。探针 cs25。
//   8. `foreign func` 的形参带默认值 `foreign func csD(a: Int64 = 1)`：`ParseAssignInParam`（ParseDecl.cpp:1817-1824）
//      对无名形参的 `=` 报 parse_expected_dot_lparen，parser 族，本族不可达。探针 cs26。
//   9. `foreign func csG<T>(value: T): Unit`（泛型 foreign）：官方 parse_foreign_func_should_not_be_generic
//      （ParseDecl.cpp:1568-1570，锚 `<T>` 整段）+ `sema_cffi_cannot_have_type_param`（CFFICheck.cpp:27-30）在
//      `@C func` 上可达；该名字已由 llt/ffi/semantic_contract/annotationTargetsNegative.cj 覆盖，本文件不收。探针 cs27。
//  10. `CFunc<>`（空类型实参列表）：官方 `sema_generic_argument_no_match`（PreCheck.cpp:433-436，refactor kind，
//      锚整个 `CFunc<>`），但本仓库两版 cjc 的 parser 先报 parse_expected_lt_paren / parse_expected_gt 一类，
//      Sema 到不了；且它属于**另一个 kind**，本文件不收。探针 cs28。
//  11. `CFunc` 构造器实参恰 1 个但不是 CPointer（CFUNC_CTOR_MUST_BE_CPOINTER，TypeConvExpr.cpp:77，锚整个实参表达式）：
//      已由 llt/ffi/semantic_contract/cfuncConstructorArgumentsNegative.cj 覆盖，本文件不收。探针 cs29。
//  12. `CFUNC_VAR_CANNOT_HAVE_VAR_PARAM`（带 C 变长参数的函数值不能赋给变量，官方 DiagnosticSema.def:187）：
//      宿主是变量初始化器而非签名，已由 llt/ffi/semantic_contract/cfuncVariadicProbe.cj 覆盖，本文件不收。探针 cs30。
