// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 与 1.1.3 两版实测，静态库模式 `cjc <段>.cj -o out.a --output-type=staticlib
// --diagnostic-format json`；每个 `// FILE:` 段单独编译（官方整包编译时一处 parse 错误会屏蔽其余段的语义诊断，
// 且官方 HasPrevDiag 会吞掉同起点同文案的重复诊断）。本文件全部形态两版 kind、消息、Range 完全一致，无 1.0.0 / 1.1.3 分歧段。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），本文件标记逐字符对齐官方范围。
// 本族官方 kind 的锚点口径分两种：
//   (1) `diag.Diagnose(node, kind)`（非 refactor kind，退化为 node.GetBegin() 一个字符，include/cangjie/Basic/
//       DiagnosticEngine.h:844-858）落在**整条成员声明的第一个字符**（含 `public` 等修饰符），
//       本文件把标记只包住该首字符（X 标记包住 `public` 的 p）；
//   (2) `diag.DiagnoseRefactor(kind, member, MakeRange(member.identifier))`（DeclAttributeChecker）落在**成员名首字符**，
//       本文件同样把标记只包住成员名首字符（X 标记包住 `func f()` 里的 f）。
// 官方实现（`external/cangjie_compiler`，当前 checkout 在 tag v1.0.0；主文件
// src/Sema/InheritanceChecker/StructInheritanceChecker.cpp）：
//   - :952-974 CheckSameNameInheritanceInfo → :961 `diag.Diagnose(child, sema_static_and_non_static_member_cannot_have_same_name,
//     childStatic, name, parentStatic, inheritance)`；child / parent 的 static 状态不同即报，锚 child 声明首字符；
//     inheritance 文案在 EXTEND_DECL 时是 "extended type"，否则 "parent class or interfaces"。调用点 :649（父类成员）、
//     :808（接口成员）。
//   - :976-1009 CheckInheritanceAttributes（prop 与 func 共用；func 路径需先过 :1192 CheckImplementationRelation，
//     后者返回 sameSignature = 父 / 子函数参数类型全同）：
//       :988 `!parentDecl->STATIC && child->REDEF` → sema_func_no_override_or_redefine_modifier "redef"，
//            锚 child 声明首字符；:1003 DiagCannotOverride（父非 open 且不在接口中，CANNOT_OVERRIDE 族，见"已有覆盖"）；
//            :1006 `!parentDecl->ABSTRACT && !parentDecl->STATIC && (父 open 或可 mock) && child->ABSTRACT && 父不在接口中`
//            → sema_invalid_override_member_in_class，锚 child 声明首字符。调用点 :654（prop）/ :658（func）。
//   - :1011-1030 CheckInheritanceForInterface（接口成员路径）：:1022 `childDecl->ABSTRACT && !interfaceMember->ABSTRACT`
//     → sema_invalid_override_or_redefine_member_in_interface，锚 child 声明首字符。调用点 :813 / :820。
//   - :1038-1054 DiagnoseForOverriddenMember（对合并后的继承成员表逐个走，:333 调用）：`child.replaceOther`（在
//     MergeInheritedMemberHelper.cpp:165 合并命中同名同签名父成员时置位）时直接返回；否则 :1050 `childDecl->OVERRIDE`
//     → sema_missing_overridden_func，:1052 `childDecl->REDEF && childDecl->STATIC` → sema_missing_redefined_func，
//     都锚 child 声明首字符。因此"static redef 但父类型没有对应 static 成员"与"父类型根本为空（无 supertype）"都走 :1052。
//   - src/Sema/TypeCheckDecl.cpp:58-70 CheckFuncDecl（redef 检查在 :67-70）：成员带 `redef` 而不带 `static`
//     （且不是 prop 的访问器）→ sema_redef_modify_static_func "function"，锚 `redef` 修饰符本身；
//     :350-361 CheckPropDecl（redef 检查在 :357-361）同理报 "property"。
//   - src/Sema/DeclAttributeChecker.cpp:284-329 CheckAttributesForPropAndFuncDeclInClass：:303-307 abstract 成员
//     位于 abstract class 且非 public / protected → sema_invalid_member_visibility_in_class "abstract"；
//     :310-317 open 成员位于 open / abstract class 且非 public / protected → 同一 kind 的 "open" 形态
//     （:311-313 同段先报 sema_ignore_open）。两处都用 MakeRange(member.identifier)，锚成员名首字符。
//   - 文案：include/cangjie/Basic/DiagnosticSema.def:122（func_no_override_or_redefine_modifier）、:202-203
//     （invalid_override_member_in_class）、:204-205（invalid_override_or_redefine_member_in_interface）、:234
//     （missing_overridden_func）、:235（missing_redefined_func）、:236-237（static_and_non_static_member_cannot_have_same_name）、
//     :270（redef_modify_static_func）；include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:113
//     （invalid_member_visibility_in_class）、:112（cannot_override）、:123（return_type_incompatible）。
// 本项目 CFIR 名（cfir/checkers/checkers-component-generator/src/.../diagnostics/CfirDiagnosticsList.kt；
// 工厂 cfir/checkers/gen/.../diagnostics/CfirErrors.kt；文案 cfir/checkers/src/.../diagnostics/CfirErrorsDefaultMessages.kt）：
//   INVALID_OVERRIDE_MEMBER_IN_CLASS（列表 :1524，工厂 CfirErrors.kt:386，文案 :994-1000，
//     生产者 CfirInheritanceDeepChecker.kt:1454 的 invalidAbstractOverrideInClass 分支）；
//   STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME（列表 :1505，工厂 :383，文案 :974-981，生产者 :1303）；
//   INVALID_MEMBER_VISIBILITY_IN_CLASS（列表 :1564，工厂 :393，文案 :1019，生产者 :1111）；
//   REDEF_INSTANCE_ERROR（列表 :288，工厂 :92，文案 :171，生产者 CfirModifierChecker.kt:182，对应官方
//     sema_redef_modify_static_func，锚 `redef` 修饰符，与官方一致）。
//   testFixtures 的 golden 映射表 DiagnosticNameMapper.kt 只登记了其中 STATIC_AND_NON_STATIC（:73）与
//   INVALID_MEMBER_VISIBILITY_IN_CLASS（:118）；INVALID_OVERRIDE_MEMBER_IN_CLASS / REDEF_INSTANCE_ERROR 未登记（全表 191 条）。
// 按源码读取（未跑 CFIR）可预判的锚点差异（以源码为准，实际差异以 CFIR 实跑为准）：
//   STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME 的 source 是 `ownInfo.nameSource ?: ownInfo.source ?: subject.source`，
//     优先取**成员名**，官方锚 child 声明首字符（`public` 的 `p`），二者不同；
//   INVALID_MEMBER_VISIBILITY_IN_CLASS 的 source 是 `memberInfo.source ?: member.source ?: classDecl.source`，取**成员声明**，
//     官方锚成员名，同一形态下声明首字符与成员名首字符不同（`func f` 的 `f` 与 `f`）；
//   INVALID_OVERRIDE_MEMBER_IN_CLASS 用 `ownInfo.source?.firstCharacterDiagnosticSource() ?: ownInfo.nameSource`，与官方同为声明首字符；
//   REDEF_INSTANCE_ERROR 用 `redefModifier.source`，与官方同为 `redef` 修饰符。
//   另：官方对"实例成员带 redef 且签名命中父类非 static 成员"报 sema_func_no_override_or_redefine_modifier（无 CFIR 对应名），
//   本项目 CfirModifierChecker.kt:186-190 在同一形态改报 NOTHING_TO_OVERRIDE（锚 override 修饰符），语义不同。
// 官方有而本项目无对应诊断名（按 SUGGESTED_DIAGNOSTIC 处理，段内不写标记）：
//   sema_func_no_override_or_redefine_modifier、sema_missing_redefined_func、sema_invalid_override_or_redefine_member_in_interface。
// 已有覆盖不重复：CANNOT_OVERRIDE（官方 sema_cannot_override）在 inheritance/classNotOpenForInheritance.cj；
//   NOTHING_TO_OVERRIDE（官方 sema_missing_overridden_func 的本项目对应名）在 inheritance/nothingToOverride.cj 与
//   inheritance/overrideInvisibleMember.cj、overrideInvisibleAndNothingToOverride.cj；RETURN_TYPE_INCOMPATIBLE
//   （官方 sema_return_type_incompatible，即 OVERRIDING_RETURN_TYPE_MISMATCH 的官方 kind）在
//   inheritance/overrideReturnTypeMismatch.cj、overrideReturnTypeCovariant.cj；PARAM_NAMED_MISMATCHED 在
//   inheritance/overrideParameterNameMismatch.cj；INCOMPATIBLE_MODIFIERS（`static override` / `static open` 的
//   parse_conflict_modifier）在 declaration-status/staticCannotBeOpenAbstractOverride.cj、staticOpenAndOverload.cj；
//   实例 `redef func` 的 REDEF_INSTANCE_ERROR 单形态也在 declaration-status/staticCannotBeOpenAbstractOverride.cj，
//   本文件只补 property 形态与命中父类成员的组合形态；属性继承规则在 property/propertyInheritanceRules.cj。

// ── 形态 1：abstract class 的抽象成员覆盖父类 open 非抽象成员（写 public 排除可见性噪声）
// kind：sema_invalid_override_member_in_class "cannot override non-abstract function 'f' with abstract function"
// 锚点：child 声明首字符（`public` 的 `p`，diag.Diagnose(child, …) 取 Decl::GetBegin）。1.0.5 / 1.1.3 一致 Range (12,5)..(12,6)、(14,5)..(14,6)。
// 抽象成员写成"无函数体的成员声明"（`public func f(): Int64`），官方 ParseDecl.cpp:1548-1554 在 CLASS_BODY /
// INTERFACE_BODY 给无体成员打 ABSTRACT 标记；写显式 `abstract` 修饰符会被 parser 拦（见"已删除的形态 1"）。
// FILE: abstractMemberOverridesOpenFunc.cj
open class OverrideRedefOpenFuncParent {
    public open func f(): Int64 {
        return 1
    }

    public open func g(x: Int64): Int64 {
        return x
    }
}

abstract class OverrideRedefOpenFuncChild <: OverrideRedefOpenFuncParent {
    <!INVALID_OVERRIDE_MEMBER_IN_CLASS!>p<!>ublic func f(): Int64

    <!INVALID_OVERRIDE_MEMBER_IN_CLASS!>p<!>ublic func g(x: Int64): Int64
}

// ── 形态 2：同 kind 的 property 形态（PROP_DECL 走 :654 调用点，消息里的 kind 词是 property）
// 锚点：child 声明首字符。1.0.5 / 1.1.3 一致 Range (10,5)..(10,6)。
// FILE: abstractPropOverridesOpenProp.cj
open class OverrideRedefOpenPropParent {
    public open prop p: Int64 {
        get() {
            return 1
        }
    }
}

abstract class OverrideRedefOpenPropChild <: OverrideRedefOpenPropParent {
    <!INVALID_OVERRIDE_MEMBER_IN_CLASS!>p<!>ublic prop p: Int64
}

/*
SUGGESTED_DIAGNOSTIC: 无对应 CFIR 名（官方 sema_invalid_override_or_redefine_member_in_interface）
SUGGESTED_MESSAGE: cannot override implemented interface %s '%s' with abstract %s
SOURCE: src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:1022（CheckInheritanceForInterface）；include/cangjie/Basic/DiagnosticSema.def:204-205
*/
// ── 形态 3：abstract class 的抽象成员"覆盖"接口里带默认实现的成员（接口成员非 abstract，child 成员 abstract）
// kind：sema_invalid_override_or_redefine_member_in_interface "cannot override implemented interface function 'f' with abstract function"
// 锚点：child 声明首字符（`public` 的 `p`）。1.0.5 / 1.1.3 一致 Range (12,5)..(12,6)、(14,5)..(14,6)。
// 条件只依赖接口成员是否 abstract：接口成员写无体声明（abstract）时子类同样写无体成员零诊断（见合法段 L2），
// 接口成员带默认实现体时子类写抽象成员才触发：无体成员由 ParseDecl.cpp:1548-1554 打 ABSTRACT，SetDefaultFunc（:874-895）
// 只给无 ABSTRACT 标记的 FUNC_DECL / PROP_DECL 打 DEFAULT。
// CfirDiagnosticsList 无同名诊断，本段不写内联断言。
// FILE: abstractMemberOverridesImplementedInterfaceFunc.cj
interface OverrideRedefImplementedFuncIface {
    func f(): Int64 {
        return 1
    }

    func g(): Int64 {
        return 1
    }
}

abstract class OverrideRedefImplementedFuncChild <: OverrideRedefImplementedFuncIface {
    public func f(): Int64

    public func g(): Int64
}

/*
SUGGESTED_DIAGNOSTIC: 无对应 CFIR 名（官方 sema_invalid_override_or_redefine_member_in_interface）
SUGGESTED_MESSAGE: cannot override implemented interface %s '%s' with abstract %s
SOURCE: src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:1022（CheckInheritanceForInterface，PROP_DECL 走 :813 调用点）；include/cangjie/Basic/DiagnosticSema.def:204-205
*/
// ── 形态 4：同 kind 的 property 形态
// 锚点：child 声明首字符。1.0.5 / 1.1.3 一致 Range (10,5)..(10,6)。
// FILE: abstractPropOverridesImplementedInterfaceProp.cj
interface OverrideRedefImplementedPropIface {
    prop q: Int64 {
        get() {
            return 1
        }
    }
}

abstract class OverrideRedefImplementedPropChild <: OverrideRedefImplementedPropIface {
    public prop q: Int64
}

/*
SUGGESTED_DIAGNOSTIC: 无对应 CFIR 名（官方 sema_func_no_override_or_redefine_modifier，锚 child 声明首字符）
SUGGESTED_MESSAGE: do not need '%s' modifier for %s '%s'
SOURCE: src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:988（CheckInheritanceAttributes）；include/cangjie/Basic/DiagnosticSema.def:122
*/
// ── 形态 5：子类成员带 `redef`（实例成员）且签名命中父类的非 static 成员 —— 同一声明触发两条官方诊断
//   sema_redef_modify_static_func（锚 `redef` 修饰符，Range (12,12)..(12,16) / (18,12)..(18,16)）
//   + sema_func_no_override_or_redefine_modifier（锚 child 声明首字符 `public` 的 `p`，Range (12,5)..(12,6)）；
//   形参表不同的 `redef func g(x)`（父类是 `g()`）只报前一条：后者需先过 CheckImplementationRelation 的同签名判定，
//   签名不同时到不了 :988。
// 本项目 CfirModifierChecker.kt:182 在同一形态报 REDEF_INSTANCE_ERROR（锚 `redef`，与官方同），
// 另在 :186-190 报 NOTHING_TO_OVERRIDE（锚 override 修饰符），与官方 sema_func_no_override_or_redefine_modifier 语义不同。
// CfirDiagnosticsList 无 sema_func_no_override_or_redefine_modifier 对应名，本段不写内联断言。
// FILE: redefInstanceFuncMatchingParentFunc.cj
open class OverrideRedefMatchingParent {
    public open func f(): Int64 {
        return 1
    }

    public open func g(): Int64 {
        return 1
    }
}

class OverrideRedefMatchingChildA <: OverrideRedefMatchingParent {
    public redef func f(): Int64 {
        return 2
    }
}

class OverrideRedefMatchingChildB <: OverrideRedefMatchingParent {
    public redef func g(x: Int64): Int64 {
        return 3
    }
}

// ── 形态 6：static 成员与父类同名非 static 成员冲突（子 static / 父非 static，子非 static / 父 static 两个方向）
// kind：sema_static_and_non_static_member_cannot_have_same_name
//       "static member 'g' cannot have the same name with non-static member in parent class or interfaces"
//       / "non-static member 'f' cannot have the same name with static member in parent class or interfaces"
// 锚点：child 声明首字符。1.0.5 / 1.1.3 一致 Range (12,5)..(12,6)、(16,5)..(16,6)。
// 子类写成 open class 以免额外触发 sema_ignore_open 警告（非 open 类的 open 成员只是 warning，不在本文件）。
// 同一形态下带 `static redef` 也只产生这一条（CheckImplementationRelation 对 static / 非 static 函数类型判不同，
// :988 分支到不了），见形态 11 的静态 redef 段。
// FILE: staticChildMemberVsNonStaticParentMember.cj
open class OverrideRedefStaticConflictParent {
    public static func f(): Int64 {
        return 1
    }

    public open func g(): Int64 {
        return 1
    }
}

open class OverrideRedefStaticConflictChild <: OverrideRedefStaticConflictParent {
    <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>p<!>ublic static func g(): Int64 {
        return 2
    }

    <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>p<!>ublic open func f(): Int64 {
        return 3
    }
}

// ── 形态 7：同 kind 的 var 字段形态（CheckSameNameInheritanceInfo 对 VAR_DECL 先做 static 比较，
//     命中后 return，不会落到 :972 的 member_variable_can_not_shadow）
// 锚点：child 声明首字符。1.0.5 / 1.1.3 一致 Range (6,5)..(6,6)。
// FILE: staticChildVarVsNonStaticParentVar.cj
open class OverrideRedefStaticVarParent {
    public var x: Int64 = 0
}

open class OverrideRedefStaticVarChild <: OverrideRedefStaticVarParent {
    <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>p<!>ublic static var x: Int64 = 1
}

// ── 形态 8：`redef` 修饰符用在实例成员上（无父类同名成员）——官方 kind 是 sema_redef_modify_static_func
//       "'redef' cannot be used to modify an instance 'property'"，锚 `redef` 修饰符首字符（TypeCheckDecl.cpp:360 传
//       *redefModifier，Diagnose(node, kind) 退化为 GetBegin 一字符）。本项目对应名 REDEF_INSTANCE_ERROR。
// 锚点：`redef` 修饰符。1.0.5 / 1.1.3 一致 Range (2,5)..(2,6)。
// function 形态由 declaration-status/staticCannotBeOpenAbstractOverride.cj 覆盖，本段只补 property 形态。
// FILE: redefInstanceProp.cj
class OverrideRedefInstancePropClass {
    <!REDEF_INSTANCE_ERROR!>r<!>edef prop p: Int64 {
        get() {
            return 1
        }
    }
}

// ── 形态 9：abstract class 里的抽象成员没写 public / protected（"abstract" 形态，DeclAttributeChecker.cpp:303-307）
// kind：sema_invalid_member_visibility_in_class "the visibility of an 'abstract' function must be 'public' or 'protected'"
//       / 同一 kind 的 "property" 形态
// 锚点：成员名首字符（MakeRange(member.identifier)）。1.0.5 / 1.1.3 一致 Range (2,10)..(2,11)、(4,10)..(4,11)。
// 抽象成员同样是无体声明（显式 `abstract` 被 parser 拦，见"已删除的形态 1"）；本段所在类没有 supertype，
// 所以不会顺带触发 INVALID_OVERRIDE_MEMBER_IN_CLASS。
// FILE: abstractMemberVisibilityInAbstractClass.cj
abstract class OverrideRedefAbstractVisibilityClass {
    func <!INVALID_MEMBER_VISIBILITY_IN_CLASS!>f<!>(): Int64

    prop <!INVALID_MEMBER_VISIBILITY_IN_CLASS!>p<!>: Int64
}

// ── 形态 10：open / abstract class 里的 open 成员没写 public / protected（"open" 形态，DeclAttributeChecker.cpp:310-317）
// kind：sema_invalid_member_visibility_in_class "the visibility of an 'open' function must be 'public' or 'protected'"
//       / 同一 kind 的 "property" 形态
// 锚点：成员名首字符。1.0.5 / 1.1.3 一致 Range (2,15)..(2,16)、(8,15)..(8,16)。
// 非 open / 非 abstract 类里的 open 成员报 sema_ignore_open（warning，属声明状态族），不属本文件。
// FILE: openMemberVisibilityInOpenClass.cj
open class OverrideRedefOpenVisibilityFuncClass {
    open func <!INVALID_MEMBER_VISIBILITY_IN_CLASS!>g<!>(): Int64 {
        return 1
    }
}

open class OverrideRedefOpenVisibilityPropClass {
    open prop <!INVALID_MEMBER_VISIBILITY_IN_CLASS!>q<!>: Int64 {
        get() {
            return 1
        }
    }
}

/*
SUGGESTED_DIAGNOSTIC: 无对应 CFIR 名（官方 sema_missing_redefined_func，锚 child 声明首字符）
SUGGESTED_MESSAGE: 'redef' %s '%s' does not have a redefined 'static' %s in its supertype
SOURCE: src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:1052（DiagnoseForOverriddenMember）；include/cangjie/Basic/DiagnosticSema.def:235
*/
// ── 形态 11：`static redef` 但父类型没有对应 static 成员（两种写法：成员名不存在 / 存在但签名不同）
// kind：sema_missing_redefined_func "'redef' function 'g' does not have a redefined 'static' function in its supertype"
// 锚点：child 声明首字符。1.0.5 / 1.1.3 一致 Range (8,5)..(8,6)、(14,5)..(14,6)。
// 签名不同（形参表不同）时 MergeInheritedMemberHelper 不置 replaceOther，官方同样报本条；CFIR 无同名诊断，本段不写内联断言。
// FILE: staticRedefWithoutStaticParentMember.cj
open class OverrideRedefMissingStaticParent {
    public static func f(): Int64 {
        return 1
    }
}

class OverrideRedefMissingStaticChildA <: OverrideRedefMissingStaticParent {
    public static redef func g(): Int64 {
        return 2
    }
}

class OverrideRedefMissingStaticChildB <: OverrideRedefMissingStaticParent {
    public static redef func f(x: Int64): Int64 {
        return 3
    }
}

/*
SUGGESTED_DIAGNOSTIC: 无对应 CFIR 名（官方 sema_missing_redefined_func，锚 child 声明首字符）
SUGGESTED_MESSAGE: 'redef' %s '%s' does not have a redefined 'static' %s in its supertype
SOURCE: src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:1052（DiagnoseForOverriddenMember，父类型为空时同样走这一分支）；include/cangjie/Basic/DiagnosticSema.def:235
*/
// ── 形态 12：没有 supertype 的 class / struct 里的 `static redef` 成员（继承成员表为空，:1052 仍然触发）
// kind：sema_missing_redefined_func。锚点：child 声明首字符。1.0.5 / 1.1.3 一致 Range (2,5)..(2,6)、(8,5)..(8,6)。
// FILE: staticRedefWithoutSupertype.cj
class OverrideRedefNoSupertypeClass {
    public static redef func h(): Int64 {
        return 1
    }
}

struct OverrideRedefNoSupertypeStruct {
    public static redef func g(): Int64 {
        return 2
    }
}

// 合法对照段（1.0.5 / 1.1.3 两版均零诊断）：

// L1：override 父类 open 成员；`static redef` 命中父类 static 成员（两代链式）；`static redef` 命中接口 static 成员（class 与 struct 实现）。
// FILE: legalOverrideAndStaticRedef.cj
open class OverrideRedefLegalOpenParent {
    public open func h(): Int64 {
        return 1
    }

    public static func s(): Int64 {
        return 1
    }
}

class OverrideRedefLegalOverrideChild <: OverrideRedefLegalOpenParent {
    public override func h(): Int64 {
        return 2
    }
}

open class OverrideRedefLegalRedefChildA <: OverrideRedefLegalOpenParent {
    public static redef func s(): Int64 {
        return 2
    }
}

open class OverrideRedefLegalRedefChildB <: OverrideRedefLegalRedefChildA {
    public static redef func s(): Int64 {
        return 3
    }
}

interface OverrideRedefLegalStaticIface {
    static func t(): Int64 {
        return 1
    }
}

open class OverrideRedefLegalStaticIfaceParent <: OverrideRedefLegalStaticIface {
}

class OverrideRedefLegalStaticIfaceChild <: OverrideRedefLegalStaticIfaceParent {
    public static redef func t(): Int64 {
        return 2
    }
}

struct OverrideRedefLegalStaticIfaceStruct <: OverrideRedefLegalStaticIface {
    public static redef func t(): Int64 {
        return 3
    }
}

// L2：抽象成员（无体声明）覆盖父类 / 接口的抽象成员；具体实现接口的抽象成员（function 与 property）。
//     官方 SetDefaultFunc 只给接口里"无 ABSTRACT 标记的 FUNC_DECL"打 DEFAULT，抽象成员不参与 INVALID_OVERRIDE_OR_REDEFINE 判定。
// FILE: legalAbstractMemberRedefinitions.cj
abstract class OverrideRedefLegalAbstractParent {
    public func f(): Int64
}

abstract class OverrideRedefLegalAbstractChild <: OverrideRedefLegalAbstractParent {
    public func f(): Int64
}

interface OverrideRedefLegalAbstractFuncIface {
    func g(): Int64
}

abstract class OverrideRedefLegalAbstractImpl <: OverrideRedefLegalAbstractFuncIface {
    public func g(): Int64
}

interface OverrideRedefLegalAbstractPropIface {
    prop p: Int64
}

abstract class OverrideRedefLegalAbstractPropImpl <: OverrideRedefLegalAbstractPropIface {
    public prop p: Int64
}

interface OverrideRedefLegalConcreteImplIface {
    func k(): Int64
}

class OverrideRedefLegalConcreteImpl <: OverrideRedefLegalConcreteImplIface {
    public func k(): Int64 {
        return 1
    }
}

// L3：接口成员带默认实现（DEFAULT）时的普通实现；protected / public 的抽象与 open 成员都合法。
// FILE: legalVisibilityAndDefaultImpl.cj
interface OverrideRedefLegalDefaultIface {
    func f(): Int64 {
        return 1
    }

    prop p: Int64 {
        get() {
            return 1
        }
    }
}

class OverrideRedefLegalDefaultImpl <: OverrideRedefLegalDefaultIface {
    public func f(): Int64 {
        return 2
    }

    public prop p: Int64 {
        get() {
            return 2
        }
    }
}

abstract class OverrideRedefLegalVisibilityAbstract {
    protected func f(): Int64

    public func g(): Int64
}

open class OverrideRedefLegalVisibilityOpen {
    protected open func h(): Int64 {
        return 1
    }

    public open func k(): Int64 {
        return 2
    }
}

// 已删除的形态（取证 kind 与 Range 见下，除注明外 1.0.5 与 1.1.3 一致）：
//   1. 成员上写显式 `abstract` 修饰符（`public abstract func f(): Int64`、`protected abstract func h(): Int64`）
//      → parse_illegal_modifier_in_scope "unexpected modifier 'abstract' on function declaration in class body"
//        （锚 `abstract` 修饰符，Range (n,12)..(n,20)）；1.1.3 还额外报
//        parse_explicitly_abstract_only_for_cjmp_abstract_class "only common/specific or Native FFI mirror abstract
//        classes can have explicitly abstract members"（锚整条成员声明，Range (n,5)..(n,36)）。显式 abstract 只在 CJMP
//        common / specific 包与 Native FFI 镜像类里合法，属 CJMP / 声明状态族（PARSE_EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS
//        已由 declaration-status/staticCannotBeOpenAbstractOverride.cj 覆盖）。本文件的"抽象成员"一律写成无函数体的成员声明，
//        官方 ParseDecl.cpp:1548-1554 在 CLASS_BODY / INTERFACE_BODY 给无体成员打 ABSTRACT 标记，语义等价。
//   2. `static override func`（无论父成员 static 与否）→ parse_conflict_modifier "'override' and 'static' modifiers
//        conflict on function declaration"（锚 `override` 修饰符，Range (n,19)..(n,27)），parser 阶段失败后不再进入
//        继承检查。官方 v1.0.0 的 DiagnosticSema.def 没有 sema_override_static_error（DiagnosticNameMapper.kt:112 的
//        OVERRIDE_STATIC_ERROR 映射在 v1.0.0 无同名 kind）；`static open` / `static abstract` 的同类冲突见
//        declaration-status/staticCannotBeOpenAbstractOverride.cj。
//   3. `redef` 修饰符写在 extend 块的成员上 → parse_illegal_modifier_in_scope "unexpected modifier 'redef' on function
//        declaration in extend body"（锚 `redef`，Range (n,19)..(n,24)）。extend 成员不能 redef，"static redef 找不到父 static
//        成员"的形态只存在于 class / struct 成员（形态 11、12）。
//   4. extend 块里的 static / 非 static 同名冲突：官方会同时报 sema_static_and_non_static_member_cannot_have_same_name
//        （锚 extend 成员声明首字符）与 sema_extend_member_cannot_shadow / sema_extend_function_cannot_overridden
//        （锚成员名），后两个属 extend 族（extend/ 域），且 CheckExtendMemberValid 会先短路，为避免与
//        extend 族夹具交叉，本文件不取该形态。
//   5. override 父成员返回类型不兼容（官方 sema_return_type_incompatible / sema_return_type_invariance，
//        StructInheritanceChecker.cpp:1229-1248）→ 属返回类型族，已由 inheritance/overrideReturnTypeMismatch.cj
//        （RETURN_TYPE_INCOMPATIBLE）与 overrideReturnTypeCovariant.cj 覆盖；OVERRIDING_RETURN_TYPE_MISMATCH 的
//        官方 kind 就是 sema_return_type_incompatible（DiagnosticNameMapper.kt:43），无独立形态。
//   6. `override` 但父类型没有可覆盖成员（官方 sema_missing_overridden_func，:1050）→ 本项目对应 NOTHING_TO_OVERRIDE，
//        已由 inheritance/nothingToOverride.cj、overrideInvisibleMember.cj、overrideInvisibleAndNothingToOverride.cj 覆盖。
//   7. 覆盖父类非 open 成员（官方 sema_cannot_override，DiagCannotOverride src/Sema/Diags.cpp:368-377）→ CANNOT_OVERRIDE，
//        已由 inheritance/classNotOpenForInheritance.cj 覆盖。
//   8. 非 abstract / 非 open 的类里写无体成员（`class C { func g(): Int64 }`）→ sema_missing_func_body "function 'g'
//        can not be abstract" + sema_class_need_abstract_modifier_or_func_need_impl（锚类名首字符），属声明状态族；
//        因此本文件所有无体成员都放在 abstract class 里。
