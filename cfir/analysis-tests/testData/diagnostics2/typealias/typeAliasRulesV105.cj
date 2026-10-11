// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_VERSION: 1.0.5
// 本文件是 typeAliasRules.cj 中只在 1.0.5 产出诊断的 6 个段（形态 7、形态 8 的 1.0.5 段、
// 形态 25–28）。LLT 框架的版本指令是**文件级**指令、不能写在 `// FILE:` 段内，故单独成文件。
// 其余取证说明见 typeAliasRules.cj 头部。

// FILE: typeAliasUnusedParamWithPropagatedConstraint.cj
interface TARBA {}

class TARBB<X> where X <: TARBA {}

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type TARBC<T, K><!> = TARBB<T>

func tarMainUnusedConstraint() {}

// ===== 形态 8：环上别名**带形参且形参未被使用** → **版本分歧**：1.0.5 报两条 `sema_typealias_cycle` 且**不**报未用形参
// （`CheckTypeAlias` 在 `!Ty::IsTyCorrect(tad.type->ty)` 处早退，TypeCheckDecl.cpp:686）；1.1.3 额外报两条
// `typealias_unused_type_parameters`。两段**必须用不同声明名**：写成同名会触发官方 `sema_redefinition`
// （DiagnosticSema.def:21，锚声明名 `TARBF`，1 字符），那是重定义家族、与别名规则无关，所以分家。
// FILE: typeAliasCycleUnusedParamGate.cj
<!TYPEALIAS_CYCLE!>type TARBD<T><!> = TARBE<T>

<!TYPEALIAS_CYCLE!>type TARBE<U><!> = TARBD<U>

func tarMainCycleUnusedGate() {}

// FILE: typeAliasAccessibilityOnlyV105.cj
private class TARBnV3 {}

public <!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>TARBoV3<!><!> = Array<TARBnV3>

func tarMainAccessibilityOnlyV105() {}

// ===== 形态 26（1.0.5-only）：函数类型返回 private 类 → 同形态 25 的版本分歧（C 两版都有，D 仅 1.0.5）。
// FILE: typeAliasAccessibilityFuncTypeV105.cj
private class TARBpV4 {}

public <!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>TARBqV4<!><!> = (Int64) -> TARBpV4

func tarMainAccessibilityFuncTypeV105() {}

// ===== 形态 27（1.0.5-only）：元组含 private 类 → 同形态 25/26 的版本分歧（D 的 note 是整个元组类型）。
// FILE: typeAliasAccessibilityTupleV105.cj
private class TARBrS28 {}

public <!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>TARBsV5<!><!> = (TARBrS28, Int64)

func tarMainAccessibilityTupleV105() {}

// ===== 形态 28（1.0.5-only）：`Option<private 类>` → 同形态 25/26/27 的版本分歧（D 的 note 类型是 `Enum-Option<…>`）。
// FILE: typeAliasAccessibilityOptionV105.cj
private class TARBt {}

public <!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>TARBuV6<!><!> = Option<TARBt>

func tarMainAccessibilityOptionV105() {}

// ===== 形态 29（两版一致）：类型别名的**别名**（`type TARBv<T> = TARBw`，internal）被 public 别名引用 → C 报 `TARBv`
// （internal），D 报 `TARBw` 是 private（1.0.5 / 1.1.3 都**穿透**别名看目标，与形态 15 的 public→internal 别名不同——
// 区别在于**目标**是 private 级，参见 `CheckTypeAliasAccess` 之后 `DiagLowerAccessLevelTypesUse` 的判定）。
