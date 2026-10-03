// RUN_PIPELINE_TILL: FRONTEND
// 元组字段的 CType 约束（"tuple member mustn't be struct with @C"）语义夹具。
//
// 取证方式：官方 cjc 1.0.5 与 1.1.3 两版实测，静态库模式
// `-o out.a --output-type=staticlib --diagnostic-format json`；每个 `// FILE:` 段单独编译（官方整包编译时一处
// parse 错误会吞掉其余段的语义诊断，见 DiagnosticEngine.cpp 的 CanBeEmitted；本文件形态 6 必须逐段才看得到多条诊断）。
// 除特别注明外，两版对本文全部段的 kind、文案、MainHint.Range 完全一致，因此不拆 LANGUAGE_VERSION 夹具。
// 1.1.3 本机在语义检查**之后**的 codegen / Reflection 阶段对含 `@C` struct 的单元会失败（stdout 无 JSON，
// stderr 为 "Reflection format: modifier type is invalid"），语义诊断 JSON 仍完整产出。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），本文件标记逐字符对齐官方范围。
// 内联标记写法：由"小于感叹号 + 诊断名 + 大于感叹号 + 锚点文本 + 结束标记"五段构成；全文只在 `// FILE:` 段内出现，
// 头部注释一律用文字描述，不出现该序列的字面量。
//
// 官方实现（external/cangjie_compiler，checkout 在 tag v1.0.0）：
//   - src/Sema/TypeCheckType.cpp:300-309 `TypeCheckerImpl::CheckTupleType(ASTContext&, TupleType&)`
//       逐个遍历 `tt.fieldTypes`：`Synthesize(ctx, it.get())` 之后，
//       `if (it->ty && Ty::IsCTypeConstraint(*it->ty)) { diag.Diagnose(*it, DiagKind::sema_invalid_tuple_field_ctype); return; }`
//       三个要点：
//       (1) `return` 在第一次命中就退出 —— **同一个元组类型节点最多报一条**。两个 `@C` struct 字段只报第一个
//           （本文形态 3 实测）；先命中的字段决定锚点，官方不会"逐字段各报一条"。
//       (2) 锚点是 `*it`，即**出问题的那个字段的类型节点**，经 DiagnosticEngine.h:844-857 的
//           `Diagnose(const Node&, kind)` 退化成 `Diagnose(node.GetBegin(), kind)`，再由 :822-824
//           `Diagnose(pos, kind)` 取 `pos..pos+1`。所以锚**字段类型的首字符 1 个字符**，不是整个元组、不是整个字段。
//       (3) 判定前先 `Synthesize`，所以字段类型可以是任意可合成类型（RefType / 类型别名 / 嵌套元组 / 函数类型 /
//           VArray 元素 / 泛型实参……），本族在这些位置全部可达，本文逐段覆盖。
//   - src/AST/Types.cpp:874-879 `Ty::IsCTypeConstraint`：
//       `if (IsMetCType(ty) && !IsPrimitiveCType(ty)) { return ty.kind == TypeKind::TYPE_STRUCT && name != "String"; }`
//       命中条件精确到：**类型本身就是 `@C` struct**，且名字不是 `String`。
//   - src/AST/Types.cpp:865-872 `Ty::IsMetCType`（决定哪些类型"算 CType"从而进入上面那条判定）：
//       `VArray<…>` 递归到元素类型；其余是
//       `IsPrimitiveCType(ty) || ty.IsPointer() || ty.IsCString() || ty.IsCFunc() || IsCStructType(ty) || ty.kind == TYPE_QUEST`。
//       因此**不**命中的类型：primitive（`IsPrimitiveCType` 直接返回假）、`Pointer` / `CString` / `CFunc`
//       （是 CType 但 kind 不是 TYPE_STRUCT）、`VArray<…>`（kind 是 TYPE_VARRAY，即使元素是 `@C` struct 也不命中）、
//       `Array<T>` / `Option<T>`（既非 VArray 也非 CStruct，`IsMetCType` 直接为假）、`String`（源码显式排除）。
//   - src/AST/Types.cpp:853-858 `Ty::IsCStructType`：`ty.kind == TYPE_STRUCT && declPtr && declPtr->TestAttr(Attribute::C)`。
//   - src/AST/Types.cpp:843 `Ty::IsPrimitiveCType`：`IsPrimitive(ty) && !ty.IsCString()`。
//   - 调用点是 `CheckReferenceTypeLegality`（TypeCheckType.cpp:26-64）的 `case ASTKind::TUPLE_TYPE`（:42-44），
//     所以本检查作用于**任何**类型的递归分量：`REF_TYPE` / `QUALIFIED_TYPE` / `PAREN_TYPE`（:48-50）/
//     `OPTION_TYPE` 的脱糖类型（:68-73）/ `VARRAY_TYPE` 的元素 / `FUNC_TYPE` 的形参与返回（:313-319）/
//     泛型实参（:31-34 逐 typeArg 递归），以及 TypeChecker.cpp:1078-1091 的通用 Synthesize post-visit 分支
//     （`ASTKind::TYPE` / `TUPLE_TYPE` / `REF_TYPE` / … 全部落到 `CheckReferenceTypeLegality`）。另有三条独立入口：
//       - 类型别名 `CheckTypeAlias`（TypeCheckDecl.cpp:698-700；`IS_CHECK_VISITED` 保证同一个别名只查一次，
//         所以形态 10 / 11 的别名定义点与使用点合计只有一条诊断）；
//       - 泛型上界 / 约束 `CheckUpperBound`（TypeCheckGeneric.cpp:28 / :34），所以形态 15 可达；
//       - `CheckFuncType`（TypeCheckType.cpp:311-329）对 `CFunc` 的形参 / 返回额外走 CFFI 家族
//         （`CheckCFuncParamType` / `CheckCFuncReturnType`），所以 `foreign func` / `CFunc<…>` 签名上的元组会
//         **同时**报 CFFI 家族（形态 13 / 14 因此按 SUGGESTED 处理）。
//   - 文案 include/cangjie/Basic/DiagnosticSema.def:179
//     `ERROR(sema_invalid_tuple_field_ctype, "tuple member mustn't be struct with @C")`；非 refactor kind（它在
//     DiagnosticSema.def 而不在 DiagRefactor/DiagnosticSema.def），因此走上面的 1 字符退化锚点。
//
// 本项目 CFIR 名：INVALID_TUPLE_FIELD_CTYPE
//   列表：cfir/checkers/checkers-component-generator/src/org/cangnova/cangjie/cfir/checkers/generator/diagnostics/CfirDiagnosticsList.kt:1688
//        （条目注释即写明对齐本 kind）；生成物：cfir/checkers/gen/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrors.kt:422
//        （`CFIR_INVALID_TUPLE_FIELD_CTYPE`，`CjDiagnosticFactory0`，无参数）；文案表：
//        cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt:1059 =
//        "tuple member mustn't be struct with @C"（与官方 DiagnosticSema.def:179 逐字一致）；
//        生产者：cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/type/CfirTupleCFieldTypeChecker.kt:41-58
//        （`val tupleType = typeRef.coneType as? ConeTupleType ?: return` →
//         `if (tupleType.elementTypes.none { CfirCTypeSemantics.isCStruct(context.session, it) }) return` →
//         `source = typeRef.source ?: typeRef.delegatedTypeRef?.source` →
//         `reportOn(source = source, factory = CfirErrors.INVALID_TUPLE_FIELD_CTYPE)`）。
//
// 按源码可预判的锚点差异（**未跑 CFIR**，只写源码能直接看出的；CFIR 与官方不一致的缺口由主会话记入 README，
// 本文件一律按官方范围写标记）：
//   (1) 锚点粒度：官方锚**出问题的字段类型节点首字符**并 `return`；CFIR 的 source 是**整个元组类型引用**
//       （`typeRef.source`，没有字段选择），且用 `none { … }` 过滤后只报一次、不指明具体字段。因此形态 1 / 2 / 3 /
//       4 / 5 等字段位置不同的段，CFIR 侧预期红在**元组类型的起始左括号**而官方红在字段类型首字符。
//   (2) 报告次数：官方 `CheckTupleType` 每次调用独立 `return`，但形态 6 里 struct 的**第二个**元组字段变量会走另一次
//       `CheckTupleType` 调用，所以官方**仍然是两条**（实测）。CFIR 挂在 `CfirResolvedTypeRef` 上、逐个类型引用都会
//       调 `check`，结论同样是两条；但形态 6 官方锚在字段变量内的字段类型，CFIR 锚整个字段类型引用，仍属 (1) 的差异。
//   (3) 别名：官方靠 `IS_CHECK_VISITED` 只查一次（形态 10 / 11 各只有一条，锚在别名定义处）；CFIR 侧别名展开后的
//       `delegatedTypeRef` 是否复用同一个类型引用节点取决于展开实现，属可能差异，头部只作提示。
//   (4) `String` 字段：官方 Types.cpp:877 用 `name != "String"` 显式排除；CFIR 的 `CfirCTypeSemantics.isCStruct`
//       对 `String` 返回假（String 不是 `status.isC`），两者一致，合法对照 L1 直接验证这一点。
//   (5) 具名元组字段：`(first: TFCCPoint, …)` 时官方锚点落在**字段名** `first` 的首字符而不是字段类型的首字符
//       （实测 Range (6,24)..(6,25)，形态 23）——同样是 `Diagnose(*it)` 退化到 `GetBegin()`，说明具名字段下
//       `fieldTypes[i]` 节点的 begin 是字段名 token。CFIR 锚整个元组类型引用，不涉及这一层，形态 23 侧预计与 (1) 一致地红。
//
// 已有覆盖不重复清单（下列都是 grep 过 testData 后确认已存在的，本文件不重写）：
//   - diagnostics/constructor/interopMutabilityProbe.cj:27 已有单点形态
//     `let bad: (CPoint, Int64) = (CPoint(), 1)` 的 INVALID_TUPLE_FIELD_CTYPE 标记，但那是旧 diagnostics/ 目录里的
//     一个探针文件（整个文件只有一个 let 局部变量、无合法对照）。本文件是 interop/ 语义矩阵文件，形态列表以本文件
//     为准，不去改旧目录文件。
//   - INVALID_CFUNC_PARAMETER_TYPE（官方 sema_invalid_cfunc_arg_type，DiagnosticSema.def:177，锚整个形参类型节点，
//     非 1 字符）已由 interop/foreignFunctionParameterTypePlaceholder.cj、interop/foreignFunctionCFuncLegalityPlaceholder.cj
//     覆盖（含 `(Int64, Int64)` 元组形参）；INVALID_CFUNC_RETURN_TYPE（官方 sema_invalid_cfunc_return_type，
//     DiagRefactor/DiagnosticSema.def:221）已由 interop/foreignFunctionReturnType.cj 覆盖（含 `(Int64, Int64)` 元组返回）。
//     本文件形态 13 / 14 只按 SUGGESTED 记录这两个伴随诊断。
//   - UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE（官方 sema_upper_bound_must_be_class_or_interface，
//     DiagRefactor/DiagnosticSema.def:102，锚类型形参名）已在 diagnostics2/constraints/repeatedBoundTypeParameter.cj、
//     conflictingUpperBounds.cj、onlyOneClassBoundAllowed.cj 与 generic/genericBoundRules.cj 覆盖；形态 15 只注明伴随。
//   - ILLEGAL_MEMBER_OF_CSTRUCT（官方 sema_illegal_member_of_cstruct，DiagnosticSema.def:183，refactor kind 锚整条
//     字段声明）已在 llt/ffi/semantic_contract/cstructFieldsNegative.cj 与
//     macro/llt/varray/varray_cffi/varray_cstruct01.cj 覆盖；形态 6 只按 SUGGESTED 注明其伴随，不写标记。
//   - CSTRUCT_CANNOT_IMPL_INTERFACES（官方 sema_cstruct_cannot_impl_interfaces，DiagRefactor/DiagnosticSema.def:138）
//     不在本文件范围。
//
// 本文件的 SUGGESTED 段共 4 个（形态 6 / 13 / 14 / 15），它们是本族诊断与别的 kind 必然同段出现的形态，
// 段内不写内联标记；除此之外 19 个非法形态全部有内联标记，另有 5 个合法对照段。
//
// ── 形态 1：`let` 类型注解里第一个字段是 `@C` struct（最直接的形态）
// kind：sema_invalid_tuple_field_ctype "tuple member mustn't be struct with @C"
// 锚点：字段类型 `TFCCPoint` 的首字母 `T`，1 个字符。1.0.5 Range (8,13)..(8,14) / 1.1.3 同。探针 t01。
// FILE: tupleFieldLetAnnotation.cj
@C
struct TFCCPointS1 {
    var x: Int32 = 0
    var y: Int32 = 0
}

func tfcLetAnnotation(): Int64 {
    let t: (<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS1, Int64) = (TFCCPointS1(), 1)
    return t[1]
}

// ── 形态 2：第二个字段是 `@C` struct（锚点随字段位置移动，不是固定落在元组开头）
// kind：sema_invalid_tuple_field_ctype；锚点：第二个字段类型首字母 `T`，1 个字符。
// 1.0.5 Range (7,20)..(7,21) / 1.1.3 同。探针 t02。
// FILE: tupleFieldSecondSlot.cj
@C
struct TFCCPointS2 {
    var x: Int32 = 0
}

func tfcSecondSlot(): Int64 {
    let t: (Int64, <!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS2) = (1, TFCCPointS2())
    return t[0]
}

// ── 形态 3：两个 `@C` struct 字段只报一条（官方 `return` 在第一次命中后退出，TypeCheckType.cpp:307）
// kind：sema_invalid_tuple_field_ctype；锚点：**第一个**命中字段的类型首字母 `T`，1 个字符。
// 1.0.5 Range (12,13)..(12,14) / 1.1.3 同。探针 t03。CFIR 的 `none { isCStruct }` 也是"只报一次"，条数一致、
// 锚点粒度见头部 (1)。
// FILE: tupleFieldTwoCStructsFirstWins.cj
@C
struct TFCCPointS3 {
    var x: Int32 = 0
}

@C
struct TFCCOther {
    var z: Int32 = 0
}

func tfcTwoCStructs(): Int64 {
    let t: (<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS3, TFCCOther) = (TFCCPointS3(), TFCCOther())
    return 0
}

// ── 形态 4：嵌套元组——内层先被检查（post-visit 先内后外），外层不再重复报
// kind：sema_invalid_tuple_field_ctype；锚点：**内层**元组的字段类型首字母 `T`，1 个字符，Range (7,21)..(7,22)。
// 1.0.5 / 1.1.3 一致。探针 t04。源码依据：`Ty::IsCTypeConstraint` 对"元组类型"本身返回假（Types.cpp:877 要求
// kind == TYPE_STRUCT），所以外层 `CheckTupleType` 遍历到内层元组字段时判定为假、不在外层再报；内层是独立的
// TupleType 节点、走自己的 `CheckTupleType`，因此整条源码只有一条诊断。
// FILE: tupleFieldNestedTupleInnerFirst.cj
@C
struct TFCCPointS4 {
    var x: Int32 = 0
}

func tfcNestedTuple(): Int64 {
    let t: (Int64, (<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS4, Int64)) = (1, (TFCCPointS4(), 1))
    return t[0]
}

// ── 形态 5：括号类型 `((…元组…))` —— PAREN_TYPE 分支递归（TypeCheckType.cpp:48-50），锚点仍是内层字段类型
// kind：sema_invalid_tuple_field_ctype；锚点：字段类型首字母 `T`，1 个字符，Range (7,14)..(7,15)。
// 1.0.5 / 1.1.3 一致。探针 t18。
// FILE: tupleFieldParenType.cj
@C
struct TFCCPointS5 {
    var x: Int32 = 0
}

func tfcParenType(): Int64 {
    let t: ((<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS5, Int64)) = (TFCCPointS5(), 1)
    return t[1]
}

// ── 形态 6：`@C` struct 的成员变量是含 `@C` struct 的元组（本族两条 + `@C` struct 成员族一条，整段按 SUGGESTED 处理）
// 官方本族两条：锚属性类型里字段类型的首字母 `T` Range (8,13)..(8,14)；锚 `init` 形参类型里字段类型的首字母 `T`
// Range (10,14)..(10,15)。struct 的属性类型与 init 形参类型是两个独立的元组类型节点，各走一次 `CheckTupleType`，
// 所以各报一条。1.0.5 / 1.1.3 一致。探针 t08。
// 同段还叠加 sema_illegal_member_of_cstruct（"member variable 't' of struct 'TFCCHolder' with @C must be
// instantiated with CType"，refactor kind 锚整条字段变量声明 Range (8,9)..(8,30)），因为元组整体永远不是 CType
// （Types.cpp:877 要求 kind == TYPE_STRUCT）。该伴随名在本项目是 ILLEGAL_MEMBER_OF_CSTRUCT
// （CfirDiagnosticsList.kt:664、文案表 CfirErrorsDefaultMessages.kt:461、生产者 CfirCAnnotationChecker.kt:114），
// 已由 llt/ffi/semantic_contract/cstructFieldsNegative.cj 覆盖，本文件不重复写它的标记，故整段按 SUGGESTED 处理。
/*
SUGGESTED_DIAGNOSTIC: INVALID_TUPLE_FIELD_CTYPE（名字与文案都可用，但同段还有 @C struct 成员族的
    ILLEGAL_MEMBER_OF_CSTRUCT 需要一起断言，整段不写内联标记）
SUGGESTED_MESSAGE: tuple member mustn't be struct with @C
SOURCE: src/Sema/TypeCheckType.cpp:300-309 CheckTupleType；include/cangjie/Basic/DiagnosticSema.def:179；
    伴随项 DiagnosticSema.def:183（sema_illegal_member_of_cstruct，判定入口 src/Sema/TypeCheckDecl.cpp:553
    `IsCStructType(*sd.ty)`）
*/
// FILE: tupleFieldInCStructMember.cj
@C
struct TFCCPointS6 {
    var x: Int32 = 0
}

@C
struct TFCCHolder {
    let t: (TFCCPointS6, Int64)

    init(t: (TFCCPointS6, Int64)) {
        this.t = t
    }
}

// ── 形态 7：形参类型（诊断锚在字段类型首字符，不是形参名 `t`）
// kind：sema_invalid_tuple_field_ctype；Range (6,19)..(6,20)。1.0.5 / 1.1.3 一致。探针 t06。
// FILE: tupleFieldInParameter.cj
@C
struct TFCCPointS7 {
    var x: Int32 = 0
}

func tfcParam(t: (<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS7, Int64)): Int64 {
    return t[1]
}

// ── 形态 8：返回类型（Range (6,20)..(6,21)，锚返回元组的字段类型首字符）
// kind：sema_invalid_tuple_field_ctype；1.0.5 / 1.1.3 一致。探针 t07。
// FILE: tupleFieldInReturnType.cj
@C
struct TFCCPointS8 {
    var x: Int32 = 0
}

func tfcReturn(): (<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS8, Int64) {
    return (TFCCPointS8(), 1)
}

// ── 形态 9：class 字段类型 + `init` 形参类型（**两条**，与形态 6 同构但宿主是 class）
// 诊断 1 Range (7,13)..(7,14)、诊断 2 Range (9,14)..(9,15)。1.0.5 / 1.1.3 一致。探针 t09。
// 与形态 6 不同：class 字段变量类型不是 CType 这条**不报**（只有 `@C` struct 才查 sema_illegal_member_of_cstruct），
// 所以本段除本族两条外零伴随诊断。
// FILE: tupleFieldInClassMember.cj
@C
struct TFCCPointS9 {
    var x: Int32 = 0
}

class TFCClassHolder {
    let t: (<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS9, Int64)

    init(t: (<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS9, Int64)) {
        this.t = t
    }
}

// ── 形态 10：类型别名**直接**指向含 `@C` struct 的元组（别名定义处即诊断，`IS_CHECK_VISITED` 保证只有一条）
// kind：sema_invalid_tuple_field_ctype；锚点：别名右侧元组里字段类型的首字母 `T`，Range (6,23)..(6,24)。
// 1.0.5 / 1.1.3 一致。探针 t12。同段用 `let t: TFCTupleAlias` 使用该别名不再追加诊断（源码 TypeCheckDecl.cpp:699）。
// FILE: tupleFieldAliasToTuple.cj
@C
struct TFCCPointS10 {
    var x: Int32 = 0
}

type TFCTupleAlias = (<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS10, Int64)

func tfcAliasToTuple(): Int64 {
    let t: TFCTupleAlias = (TFCCPointS10(), 1)
    return t[1]
}

// ── 形态 11：元组字段是 `@C` struct 的**类型别名**（诊断锚在元组里的别名名首字母，不是别名定义处）
// kind：sema_invalid_tuple_field_ctype；锚点：元组字段类型 `TFCCPointAlias` 的首字母 `T`，Range (9,13)..(9,14)。
// 1.0.5 / 1.1.3 一致。探针 t13 / r16（两级别名链 `type A1 = TFCCPointS10` + `type A2 = A1` 同样只报元组里的那一处）。
// FILE: tupleFieldAliasToCStruct.cj
@C
struct TFCCPointS11 {
    var x: Int32 = 0
}

type TFCCPointAlias = TFCCPointS11

func tfcAliasToCStruct(): Int64 {
    let t: (<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointAlias, Int64) = (TFCCPointS11(), 1)
    return t[1]
}

// ── 形态 12：函数类型的形参 / 返回类型（`CheckFuncType` 先对每个 paramTypes / retType 递归本族，
//             非 CFunc 时不进入 CFFI 分支，所以本段只有本族诊断、零伴随）
// kind：sema_invalid_tuple_field_ctype；诊断 1 锚形参元组字段类型首字母 `T` Range (7,14)..(7,15)；
// 诊断 2 锚返回元组字段类型首字母 `T` Range (12,19)..(12,20)。1.0.5 / 1.1.3 一致。探针 r10 / r11。
// FILE: tupleFieldInFunctionType.cj
@C
struct TFCCPointS12 {
    var x: Int32 = 0
}

func tfcFunctionType(): Int64 {
    let f: ((<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS12, Int64)) -> Int64 = { t => t[1] }
    return f((TFCCPointS12(), 1))
}

func tfcFunctionTypeReturn(): Int64 {
    let g: () -> (<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS12, Int64) = { => (TFCCPointS12(), 1) }
    return 1
}

// ── 形态 13：`foreign func` 形参 / 返回类型上的元组（本族 + CFFI 家族，整段按 SUGGESTED 处理）
// 官方两条本族：形参元组字段类型首字母 `T` Range (6,34)..(6,35)、返回元组字段类型首字母 `T` Range (8,35)..(8,36)。
// 1.0.5 / 1.1.3 一致。探针 t19 / t20。
// 伴随 CFFI 家族：sema_invalid_cfunc_arg_type Range (6,30)..(6,51)、sema_invalid_cfunc_return_type
// Range (8,34)..(8,52)（锚整个形参 / 返回类型节点，带 notes "return type is Tuple<Struct-TFCCPointS12, Int64>"）。
// 这两个伴随名在 CFIR 里是 INVALID_CFUNC_PARAMETER_TYPE / INVALID_CFUNC_RETURN_TYPE，已由
// interop/foreignFunctionParameterTypePlaceholder.cj、interop/foreignFunctionReturnType.cj 覆盖，本文件不重复。
/*
SUGGESTED_DIAGNOSTIC: INVALID_TUPLE_FIELD_CTYPE（名字与文案都可用，但官方锚点是"元组内字段类型首字符"、CFIR 锚整个
    元组类型引用，且同段还有两条 CFFI 家族诊断需要一起断言，故整段不写内联标记）
SUGGESTED_MESSAGE: tuple member mustn't be struct with @C
SOURCE: src/Sema/TypeCheckType.cpp:300-309 CheckTupleType + :311-329 CheckFuncType 的 CFFI 分支；
    include/cangjie/Basic/DiagnosticSema.def:179（本族）、:177（sema_invalid_cfunc_arg_type）；
    include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:221（sema_invalid_cfunc_return_type）
*/
// FILE: tupleFieldInForeignSignature.cj
@C
struct TFCCPointS13 {
    var x: Int32 = 0
}

foreign func tfcForeignParam(t: (TFCCPointS13, Int64)): Unit

foreign func tfcForeignReturn(): (TFCCPointS13, Int64)

// ── 形态 14：`CFunc<…>` 类型实参上的元组（本族 + CFFI 家族 + unsafe 调用，整段按 SUGGESTED 处理）
// 官方三条：本族锚元组字段类型首字母 `T` Range (7,20)..(7,21)；sema_invalid_cfunc_arg_type Range (7,19)..(7,37)
// （锚整个 `((TFCCPointS13, Int64))` 形参类型节点）；sema_unsafe_function_invoke_failed Range (8,12)..(8,13)
// （在非 unsafe 上下文调用 CFunc 值）。1.0.5 / 1.1.3 一致。探针 t17。三个伴随诊断分属 CFFI / unsafe 家族。
/*
SUGGESTED_DIAGNOSTIC: INVALID_TUPLE_FIELD_CTYPE（同形态 13：名字可用，但锚点粒度与伴随诊断不适合写内联标记）
SUGGESTED_MESSAGE: tuple member mustn't be struct with @C
SOURCE: src/Sema/TypeCheckType.cpp:300-309 + :311-329（`ft.isC` 时 CheckCFuncParamType）；
    include/cangjie/Basic/DiagnosticSema.def:177；sema_unsafe_function_invoke_failed 见 DiagnosticSema.def:171
*/
// FILE: tupleFieldInCFuncArgument.cj
@C
struct TFCCPointS14 {
    var x: Int32 = 0
}

func tfcCFuncArgument(): Int64 {
    let f: CFunc<((TFCCPointS14, Int64)) -> Int64> = { t => t[1] }
    return f((TFCCPointS14(), 1))
}

// ── 形态 15：泛型上界 `where T <: (TFCCPointS14, Int64)`（`CheckUpperBound`，TypeCheckGeneric.cpp:28-35）
// 本族 kind：sema_invalid_tuple_field_ctype；锚点：上界元组里字段类型的首字母 `T`，1 个字符，Range (6,48)..(6,49)。
// 1.0.5 / 1.1.3 一致。探针 t21。
// 伴随 sema_upper_bound_must_be_class_or_interface（锚类型形参名 `T`，Range (6,20)..(6,21)），属泛型上界族，已由
// diagnostics2/constraints/repeatedBoundTypeParameter.cj 等覆盖；本段按 SUGGESTED 处理、整段不写内联标记。
/*
SUGGESTED_DIAGNOSTIC: UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE（已有名字与生产者
    cfir/checkers/src/.../declaration/CfirTypeParameterBoundsChecker.kt:98，但同段还需断言本族另一条不同 kind 的
    诊断，整段不写内联标记）
SUGGESTED_MESSAGE: the upper bound '%s' of generic parameter '%s' must be class or interface
SOURCE: src/Sema/TypeCheckGeneric.cpp:23-36 CheckUpperBound；include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:102
*/
// FILE: tupleFieldInGenericUpperBound.cj
@C
struct TFCCPointS15 {
    var x: Int32 = 0
}

func tfcUpperBound<T>(t: T): Int64 where T <: (TFCCPointS15, Int64) {
    return 0
}

// ── 形态 16：泛型实参是含 `@C` struct 的元组（`CheckReferenceTypeLegality` 逐 typeArg 递归，TypeCheckType.cpp:31-34）
// kind：sema_invalid_tuple_field_ctype；锚点：实参元组里字段类型的首字母 `T`，Range (6,33)..(6,34)。
// 1.0.5 / 1.1.3 一致。探针 r02。同段一次编译里出现**两个**元组类型实参节点时官方报两条（探针 r03：
// `TFCBox3<(…)>(tfcR03Default<(…)>())`，Range (29,22)..(29,23) 与 (29,56)..(29,57)，各 1 字符），
// 即"每个元组类型节点各报一条"，与形态 3 的"同一节点只报一条"不矛盾。
// FILE: tupleFieldAsGenericTypeArgument.cj
@C
struct TFCCPointS16 {
    var x: Int32 = 0
}

func tfcGenericArg(box: TFCBox<(<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS16, Int64)>): Int64 {
    return 0
}

struct TFCBox<T> {
    var v: T = tfcBoxDefault<T>()

    init(v: T) {
        this.v = v
    }
}

func tfcBoxDefault<T>(): T {
    throw Exception()
}

// ── 形态 17：`Option<元组>`（`CheckOptionType` 对脱糖类型递归，TypeCheckType.cpp:68-73）
// kind：sema_invalid_tuple_field_ctype；锚点：`Option` 实参元组里字段类型的首字母 `T`，Range (9,20)..(9,21)。
// 1.0.5 / 1.1.3 一致。探针 r01。同段的 sema_unused_import 是 warning、不是 error，不参与错误标记比对。
// FILE: tupleFieldUnderOption.cj
import std.collection.*

@C
struct TFCCPointS17 {
    var x: Int32 = 0
}

func tfcUnderOption(): Int64 {
    let t: Option<(<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS17, Int64)> = None
    return 0
}

// ── 形态 18：`VArray<元组, $n>` 的类型实参（`CheckVArrayType` 先对 typeArgument 走 Synthesize，
//             元组作为泛型实参再被 `CheckReferenceTypeLegality` 递归）
// kind：sema_invalid_tuple_field_ctype；锚点：元组实参里字段类型的首字母 `T`，Range (7,21)..(7,22)。
// 1.0.5 / 1.1.3 一致。探针 r06。注意本族命中与否跟 `CheckVArrayWithRefType`（TypeCheckType.cpp:75-107）的
// "不能用引用类型当元素"判定是两条独立路径：`VArray<(TFCCPointS17, Int64), $2>` 不报 sema_varray_arg_type_with_reftype
// （元组不是 ref type），只报本族。
// FILE: tupleFieldAsVArrayArgument.cj
@C
struct TFCCPointS18 {
    var x: Int32 = 0
}

func tfcVArrayArg(): Int64 {
    let a = VArray<(<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS18, Int64), $2>(repeat: (TFCCPointS18(), 1))
    return 0
}

// ── 形态 19：class 静态成员函数形参 / class 静态字段（Range (7,36)..(7,37) 与 (11,33)..(11,34)）
// kind：sema_invalid_tuple_field_ctype；1.0.5 / 1.1.3 一致。探针 t24 / r21。
// FILE: tupleFieldInStaticMember.cj
@C
struct TFCCPointS19 {
    var x: Int32 = 0
}

class TFCStaticHolder {
    static func tfcStaticParam(t: (<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS19, Int64)): Int64 {
        return t[1]
    }

    static var tfcStaticField: (<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS19, Int64) = (TFCCPointS19(), 1)

    init() {}
}

// ── 形态 20：`extend` **普通** struct（不是 `@C` struct），成员函数形参含 `@C` struct 的元组
// kind：sema_invalid_tuple_field_ctype；锚点：字段类型首字母 `T`，Range (11,29)..(11,30)。1.0.5 / 1.1.3 一致。探针 r14。
// 源码依据：`PreCheckExtend`（TypeCheckExtend.cpp:307-317）对 `@C` struct 的 extend 先报
// sema_c_type_cannot_extend_interface，所以"extend `@C` struct"这条路径本族虽仍可达（探针 t25 / r36 实测本族诊断
// 依然出现在形参处，Range (17,21)..(17,22)），但总被那条伴随诊断污染；本文件用普通 struct 承载 extend 位置，
// `extend @C struct` 形态记在"已删除的形态 3"。
// FILE: tupleFieldInExtendOfPlainStruct.cj
@C
struct TFCCPointS20 {
    var x: Int32 = 0
}

struct TFCPlain {
    var x: Int64 = 0
}

extend TFCPlain {
    func tfcExtendParam(t: (<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS20, Int64)): Int64 {
        return t[1]
    }
}

// ── 形态 21：interface 方法形参（Range (7,23)..(7,24)；interface 侧本族与普通函数完全一致）
// kind：sema_invalid_tuple_field_ctype；1.0.5 / 1.1.3 一致。探针 t11。
// FILE: tupleFieldInInterfaceMethod.cj
@C
struct TFCCPointS21 {
    var x: Int32 = 0
}

interface TFCIface {
    func tfcIface(t: (<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS21, Int64)): Unit
}

// ── 形态 22：形参类型 + 元组模式匹配（match 侧的 selector 只是表达式、不是类型位置，不产生第二条诊断）
// kind：sema_invalid_tuple_field_ctype；Range (6,26)..(6,27)。1.0.5 / 1.1.3 一致。探针 t32 / r37。
// FILE: tupleFieldWithTuplePatternMatch.cj
@C
struct TFCCPointS22 {
    var x: Int32 = 0
}

func tfcPatternMatch(t: (<!INVALID_TUPLE_FIELD_CTYPE!>T<!>FCCPointS22, Int64)): Int64 {
    match (t) {
        case (_, b) => return b
    }
}

// ── 形态 23：具名元组字段 `(first: TFCCPointS22, second: Int64)` —— 锚点是**字段名**，不是字段类型
// kind：sema_invalid_tuple_field_ctype；锚点：字段名 `first` 的首字母 `f`，1 个字符，Range (6,24)..(6,25)。
// 1.0.5 / 1.1.3 一致。探针 r07（实测锚点落在 `first` 而不是 `TFCCPointS22`，见头部 (5)）。
// 元组**类型**位置用具名形参是合法的；元组**字面量**不能用具名实参，见"已删除的形态 5"。
// FILE: tupleFieldNamedTupleType.cj
@C
struct TFCCPointS23 {
    var x: Int32 = 0
}

func tfcNamedTuple(t: (<!INVALID_TUPLE_FIELD_CTYPE!>f<!>irst: TFCCPointS23, second: Int64)): Int64 {
    return t[1]
}

// 合法对照段（1.0.5 / 1.1.3 两版均零 error）：

// L1：`String` 字段合法——官方 Types.cpp:877 用 `name != "String"` 显式排除 `String`，
//     即便 `String` 是 `Struct-String`、走到 `IsCTypeConstraint` 的判定里也被排除。探针 r26。
// FILE: legalStringTupleField.cj
func tfcLegalString(): Int64 {
    let t: (String, Int64) = ("x", 1)
    return t[1]
}

// L2：`CPointer<Int64>` / `CFunc<(Int64) -> Int64>` 字段合法——它们是 CType（Types.cpp:869 的
//     `IsPointer() || IsCFunc()`），但 kind 不是 TYPE_STRUCT，所以不命中。探针 r28 / r29。
//     `CString` 字段同样不命中本族（`IsPrimitiveCType` 为真被排除），但用它当初值会额外报
//     sema_mismatched_types（"expected 'CString', found 'Struct-String'"），属 TYPE_MISMATCH 族，
//     本文件只把 CPointer / CFunc 写成合法对照。探针 r27。
// FILE: legalPointerFuncTupleFields.cj
func tfcLegalPointer(p: CPointer<Int64>): Int64 {
    let a: (CPointer<Int64>, Int64) = (p, 1)
    return a[1]
}

func tfcLegalCFunc(f: CFunc<(Int64) -> Int64>): Int64 {
    let b: (CFunc<(Int64) -> Int64>, Int64) = (f, 1)
    return b[1]
}

// L3：普通 struct 字段 / 纯 primitive 字段合法。探针 r30 / r31。
// FILE: legalPlainStructTupleField.cj
struct TFCPlainLegal {
    var x: Int64 = 0
}

func tfcLegalPlainStruct(): Int64 {
    let t: (TFCPlainLegal, Int64) = (TFCPlainLegal(), 1)
    return t[1]
}

func tfcLegalPrimitive(): Int64 {
    let t: (Int64, Bool) = (1, true)
    return t[0]
}

// L4：`VArray<TFCCPointS26>` / `Array<TFCCPointS26>` / `Option<TFCCPointS26>` 单独作类型合法，且
//     `(VArray<TFCCPointS26, $2>, Int64)` / `(Option<TFCCPointS26>, Int64)` / `(Array<TFCCPointS26>, Int64)` 作元组字段也合法——
//     源码依据：`IsMetCType`（Types.cpp:866-872）只对 VArray 递归到元素，`Array` / `Option` 既非 VArray 也非 CStruct，
//     直接返回假；VArray 虽递归成功但 `IsCTypeConstraint` 的 kind == TYPE_STRUCT 判定过不去。
//     探针 r22 / r23 / r24 / r33 / r34。
// FILE: legalContainerOfCStructTupleField.cj
import std.collection.*

@C
struct TFCCPointS27 {
    var x: Int32 = 0
}

func tfcLegalContainer(): Int64 {
    let a = VArray<TFCCPointS27, $2>(repeat: TFCCPointS27())
    let b: Array<TFCCPointS27> = [TFCCPointS27()]
    let c: Option<TFCCPointS27> = None
    let t: (VArray<TFCCPointS27, $2>, Int64) = (a, 1)
    let u: (Option<TFCCPointS27>, Int64) = (c, 2)
    let v: (Array<TFCCPointS27>, Int64) = (b, 3)
    return t[1] + u[1] + v[1]
}

// L5：`@C` struct 的成员变量是另一个 `@C` struct 合法——`@C` struct 本身**就是** CType（Types.cpp:853-858），
//     官方对 `@C` struct 成员只要求 CType，sema_illegal_member_of_cstruct 不触发。与形态 6 对照。探针 r25 / t31。
// FILE: legalCStructMemberOfCStruct.cj
@C
struct TFCCPointS28 {
    var x: Int32 = 0
}

@C
struct TFCCPointPair {
    var p: TFCCPointS28 = TFCCPointS28()
    var q: TFCCPointS28 = TFCCPointS28()
}

// 已删除的形态（取证 kind 与 Range 见下；1.0.5 与 1.1.3 两版一致，除注明外）：
//   1. 只声明不使用元组变量、而是 `println(t)` 打印：
//      → 本族诊断不变（Range (8,13)..(8,14)），但额外叠加 sema_mismatched_types（锚 `t`，Range (9,13)..(9,14)，
//        "expected 'Unit', found ..." 一类），属 TYPE_MISMATCH 族，会让 exact_check 多出一条 UNMARKED。
//        本文件所有形态改用 `t[1]` 下标取值避开；需要"声明但不使用"的写法见已删除形态 2。探针 t01 对照 / 主会话 u2。
//   2. 同上但用别名：`type TA = (TFCCPointS28, Int64)` 后 `let t: TA = …; println(t)`：伴随诊断同上。探针 t12 对照。
//   3. `extend TFCCPointS28 { func f(t: (TFCCPointS28, Int64)) … }`（extend `@C` struct 本身）：
//      → sema_c_type_cannot_extend_interface（refactor kind，锚 `extend` 的目标类型 `TFCCPointS28` 整段
//        Range (6,8)..(6,17)，文案 "c type 'TFCCPointS28' cannot support interface extend"，
//        源码 TypeCheckExtend.cpp:307-317 + DiagRefactor/DiagnosticSema.def:150）**加上**本族诊断
//        Range (7,21)..(7,22)。本族可达但被那条 refactor 诊断污染，改由形态 20（extend 普通 struct）承载。
//        探针 t25 / r36。
//   4. lambda 形参带类型注解 `{ (t: (TFCCPointS28, Int64)) => t[1] }`：
//      → 解析错误 parse_expected_double_arrow_in_lambda（锚 lambda 的 `(`）+ parse_expected_operator_or_end +
//        parse_expected_right_delimiter ×2；把形参类型换成普通 struct 也一样（`{ (t: TFCCPointS28) => … }` 同样报，
//        探针 r08），即本仓库 cjc 1.0.5 / 1.1.3 都不支持 lambda 形参类型注解，本族在该位置不可达。探针 t10 / r08。
//   5. 元组**字面量**用具名实参 `let t: (Int64, TFCCPointS28) = (first: …, second: …)`：
//      → parse_expected_operator_or_end（锚第二个 `:`）+ parse_expected_right_delimiter（锚 `(`），属 parser 族。
//        元组类型位置用具名形参是合法的（形态 23）。探针 t30 / r07。
//   6. 泛型 `@C` struct 作元组字段（`@C struct TFCGeneric<T> { … }`，`let t: (TFCGeneric<Int32>, Int64)`）：
//      → 本族诊断仍在（Range (34,13)..(34,14)），但同段叠加 3 条 sema_cffi_cannot_have_type_param
//        （`@C` struct 不能有类型形参，锚各类型形参位置），属 CFFI 家族、噪声大，本文件不收。探针 t29。
//   7. `enum` 构造器形参含 `@C` struct（`enum TFCEnum { | A(TFCCPointS28, Int64) }`）：
//      → **只有** sema_enum_pattern_func_param_cty_error（锚构造器名 `A` 的首字母 Range (17,7)..(17,8)，
//        源码 src/Sema/TypeCheckDecl.cpp:481-497 `CheckEnumFuncDeclIsCStructParam`），**本族不报**——枚举构造器形参
//        不走 `CheckReferenceTypeLegality` 的元组分派。所以本族在 enum 上不可达。探针 r04。
//   8. `Pointer<Int64>` 作元组字段：本机两版 cjc 都是 sema_undeclared_type_name（`Pointer` 不在默认导入里，
//      与本族无关）。本文件的 C 指针合法对照统一用 `CPointer`。探针 主会话 u3。
//   9. interface 里的抽象 prop 带元组类型（`interface I { prop t: (TFCCPointS28, Int64) { get() } }`）：
//      → parse_expected_left_brace + parse_missing_body（"body of function 'get' is missing"），本仓库语法不支持；
//        interface 侧改用方法形参（形态 21）。探针 r20。
//  10. primitive 元组作 CFunc 形参类型（`CFunc<(Int32, Int32) -> Int32>` / `foreign func f(t: (Int32, Int32))`）：
//      → 本族不报（primitive 被 `IsPrimitiveCType` 排除），但 CFFI 家族仍报 sema_invalid_cfunc_arg_type
//        （Range (1,21)..(1,38)），说明"元组整体不是 CType"这条与本族无关、只影响 CFFI 签名合法性；
//        写成 lambda 实参时还会多一条 sema_param_miss_match。已由 interop/ 现有夹具覆盖，本文件不收。探针 r12 / r13。
//  11. 嵌套 struct 里的 `@C` struct 用限定名（`TFCOuter.TFCCInner`）：
//      → parse_unexpected_declaration_in_scope "unexpected struct declaration in struct body"，本仓库不允许在
//        struct 体里声明嵌套 struct，限定名形态不可构造。探针 r32。