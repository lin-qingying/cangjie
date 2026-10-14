// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 与 1.1.3 两版实测，静态库模式 `cjc <段>.cj -o out.a --output-type=staticlib
// --diagnostic-format json`；每个 `// FILE:` 段单独编译（官方整包编译时一处 parse 错误会屏蔽其余段的语义诊断，
// 且官方 HasPrevDiag 会吞掉同起点同文案的重复诊断）。本文件全部形态两版 kind、消息、Range 完全一致，无 1.0.0 / 1.1.3 分歧段。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是开区间；这里只用它确认官方诊断位置。
// 本文件的内联锚点遵守 CFIR 规范：标识符/关键字取完整 token，声明级诊断取最小完整语法单元，不裁剪 token。
// 本族官方 kind 的锚点口径分两种：
//   (1) `diag.Diagnose(node, kind)`（非 refactor kind，退化为 node.GetBegin() 一个字符，include/cangjie/Basic/
//       DiagnosticEngine.h:844-858）落在节点首字符；
//   (2) `diag.DiagnoseRefactor(kind, …, MakeRange(...))` 落在被 MakeRange 包住的名字（成员名 / 标识符）。
// 官方实现（`external/cangjie_compiler`，当前 checkout 在 tag v1.0.0）：
//   - sema_abstract_method_cannot_be_accessed_directly → src/Sema/TypeCheckReference.cpp:713-724
//     CheckInstanceMemberAccessLegality：:720-721 `refExpr->isSuper && target.TestAttr(Attribute::ABSTRACT)`，
//     :723 `diag.Diagnose(ma, …, target.identifier.Val())`；ma 是 MemberAccess 节点（`super.f`），退化锚点是 `super` 的 s。
//   - sema_abstract_class_can_not_be_instantiated / sema_interface_can_not_be_instantiated
//     → src/Sema/TypeCheckCall.cpp:2515-2543 CheckCallKind：:2521-2522 接口；:2528-2533 class/struct
//       （:2529-2530 super()/this() 委托调用豁免，:2532 判 abstract）；锚 `ce`（CallExpr）首字符 = 类型名首字符。
//   - sema_missing_func_body → src/Sema/DeclAttributeChecker.cpp:284-302 CheckAttributesForPropAndFuncDeclInClass：
//     :297-299 invalidAbstract = (成员 static 且非 foreign) || (宿主类非 abstract 且非 foreign)；:301
//     `diag.Diagnose(member, …, type, member.identifier.Val())`，锚 member 声明首字符（含 public / protected 等修饰符）。
//   - sema_class_need_abstract_modifier_or_func_need_impl → src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:862-867
//     （:863-867 class-like 走这一条，:868-872 非 class-like 走 sema_need_member_implementation），锚 structDecl 首字符 = 类名首字符。
//   - sema_inherit_not_return_this → StructInheritanceChecker.cpp:1211-1221（同签名分支内：:1218
//     `!CheckThisTypeCompatibility(*parentFunc, *childFunc)` → :1220 `DiagnoseRefactor(…, MakeRange(childFunc->identifier))`，
//     :1221 附注锚父函数名）；判定条件 src/Sema/TypeCheckUtil.cpp:162-175（父函数返回类型写 `This` 时子类必须也写 `This`）；
//     :1197 非 FUNC_DECL 提前返回，property 覆盖不走这一条。
//   - sema_weak_visibility → StructInheritanceChecker.cpp:1056-1084 CheckAccessVisibility（:1073-1080 三种弱化组合，
//     :1082 → :183-212 DiagWeakVisibility，:200 `DiagnoseRefactor(…, MakeRangeForDeclIdentifier(diagNode))`，
//     src/Sema/Diags.cpp:109 取标识符范围），锚子成员名首字符。
// 文案：include/cangjie/Basic/DiagnosticSema.def:211（abstract_class_can_not_be_instantiated）、:212（interface_can_not_be_instantiated）、
//   :198-199（class_need_abstract_modifier_or_func_need_impl）、:229（missing_func_body）、:248（abstract_method_cannot_be_accessed_directly）；
//   include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:114（weak_visibility）、:120-121（inherit_not_return_this）。
// 本项目 CFIR 名（列表 cfir/checkers/checkers-component-generator/src/.../diagnostics/CfirDiagnosticsList.kt；工厂
// cfir/checkers/gen/.../diagnostics/CfirErrors.kt；文案 cfir/checkers/src/.../diagnostics/CfirErrorsDefaultMessages.kt）：
//   ABSTRACT_METHOD_CANNOT_BE_ACCESSED_DIRECTLY（列表 :508，工厂 CfirErrors.kt:137，文案 :346-348，生产者
//     cfir/checkers/src/.../expression/CfirExpressionSemanticsChecker.kt:736-741 的 CfirAbstractSuperMemberAccessChecker）；
//   ABSTRACT_CLASS_CAN_NOT_BE_INSTANTIATED（列表 :1677，工厂 :419，文案 :1056，生产者 .../expression/CfirAbstractClassInstantiationChecker.kt:61-65）；
//   MISSING_FUNC_BODY（列表 :267，工厂 :88，文案 :163，生产者 .../declaration/CfirMemberBodyDeclarationChecker.kt:66-71）；
//   INHERIT_NOT_RETURN_THIS（列表 :1588，工厂 :397，文案 :1023，生产者 .../declaration/CfirOverrideChecker.kt:323-329）；
//   CANNOT_WEAKEN_ACCESS_PRIVILEGE（列表 :360，ACTUAL_DECLARATION_NAME）；WEAK_VISIBILITY（列表 :366）用于另一继承路径。
//   golden 映射表 DiagnosticNameMapper.kt 登记了 ABSTRACT_METHOD_CANNOT_BE_ACCESSED_DIRECTLY（:120）、MISSING_FUNC_BODY（:90）、
//   WEAK_VISIBILITY（:121）；ABSTRACT_CLASS_CAN_NOT_BE_INSTANTIATED 与 INHERIT_NOT_RETURN_THIS 未登记（全表 191 条）。
// CFIR 对应规则的锚点：
//   ABSTRACT_METHOD_CANNOT_BE_ACCESSED_DIRECTLY 使用完整 `super` 引用；函数与 property 都由通用 callable 目标状态检查。
//   ABSTRACT_CLASS_CAN_NOT_BE_INSTANTIATED 使用完整被调类型名 token；
//     this(...)/super(...) 委托调用在 CFIR 是独立 origin 直接 return（:49-53），与官方 :2529-2530 的豁免一致。
//   NO_CONSTRUCTOR 是 CFIR 对接口不可实例化的等价诊断；MISSING_FUNC_BODY 锚完整成员声明单元，property 分支在 .cj.d
//     文件被额外跳过（:64），本文件无 .cj.d 形态。
//   ABSTRACT_MEMBER_NOT_IMPLEMENTED 是 CFIR 对非 abstract class 留有未实现抽象成员的原生诊断，覆盖自身无体成员和继承义务。
//   INHERIT_NOT_RETURN_THIS 用完整 declaration source，并由 ACTUAL_DECLARATION_NAME 定位到完整函数名 token；
//     CFIR 只在被覆盖成员返回类型 isThisType 且子类返回类型非 isThisType 时报（:323），父函数返回 This 而子类不 override 时不报。
//   CANNOT_WEAKEN_ACCESS_PRIVILEGE 是 CFIR 对降低覆盖成员可见性的原生诊断，锚完整成员名 token。
// 本项目有而官方 v1.0.0 无同名 kind 的名字（本文件不为其写内联断言，只在此说明）：
//   CANNOT_OVERRIDE_INVISIBLE_MEMBER（列表 :1014，工厂 :274，DiagnosticNameMapper.kt:71 映射到官方并不存在的
//   `sema_cannot_override_invisible_member`；官方 include/ 与 src/ 全树 grep 不到该 kind）。官方对"override 一个 private 父成员"的
//   行为是 private 成员不进入继承成员表、override 找不到目标 → sema_missing_overridden_func（本项目 NOTHING_TO_OVERRIDE），已由
//   inheritance/nothingToOverride.cj、overrideInvisibleMember.cj、overrideInvisibleAndNothingToOverride.cj 覆盖，本文件不重复。
//   EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS（列表 :2126）对应官方 1.1.3 才有的
//   parse_explicitly_abstract_only_for_cjmp_abstract_class（官方 v1.0.0 无此 kind，且报的是 PARSE_ 前缀名，列表 :2216 已登记），
//   见"已删除的形态 1"。
// 官方 kind 名与 CFIR 名不同、但语义相同的形态使用现有 CFIR 诊断：接口实例化用 NO_CONSTRUCTOR，
// class 缺少抽象成员实现用 ABSTRACT_MEMBER_NOT_IMPLEMENTED，可见性弱化用 CANNOT_WEAKEN_ACCESS_PRIVILEGE。
// 已有覆盖不重复：NOTHING_TO_OVERRIDE（override 找不到可见父成员）在 inheritance/nothingToOverride.cj、overrideInvisibleMember.cj、
//   overrideInvisibleAndNothingToOverride.cj；CANNOT_OVERRIDE 在 inheritance/classNotOpenForInheritance.cj；
//   INVALID_MEMBER_VISIBILITY_IN_CLASS（abstract / open 成员未写 public 或 protected，含 private abstract 形态）在
//   inheritance/overrideRedefRules.cj 形态 9、10；INVALID_OVERRIDE_MEMBER_IN_CLASS、STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME、
//   REDEF_INSTANCE_ERROR 同 overrideRedefRules.cj；RETURN_TYPE_INCOMPATIBLE 在 inheritance/overrideReturnTypeMismatch.cj 与
//   overrideReturnTypeCovariant.cj；PARSE_EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS 在
//   declaration-status/staticCannotBeOpenAbstractOverride.cj。

// ── 形态 1：子类用 `super.抽象函数` 直接访问父类的抽象函数（TypeCheckReference.cpp:713-724）
// kind：sema_abstract_method_cannot_be_accessed_directly "abstract method 'f' cannot be accessed directly"
// 官方 cjc 锚点是 MemberAccess 的首字符；CFIR 标记覆盖完整 `super` token。
// 1.0.5 / 1.1.3 一致 Range (7,16)..(7,17)。探针 p1 / q1 / r6。
// 抽象成员写成"无函数体的成员声明"（显式 `abstract` 修饰符被 parser 拦，见"已删除的形态 1"）；
// FILE: superAccessAbstractFunc.cj
abstract class AbsAccSuperFuncParent {
    public func f(): Int64
}

class AbsAccSuperFuncChild <: AbsAccSuperFuncParent {
    public override func f(): Int64 {
        return <!ABSTRACT_METHOD_CANNOT_BE_ACCESSED_DIRECTLY!>super<!>.f()
    }
}

// ── 形态 2：property 形态（官方 :701 对 FUNC_DECL / PROP_DECL 一视同仁，消息里的 function / property 来自 target 类型）
// 官方 cjc 锚点为 `super` 首字符；CFIR 使用完整 `super` token。
// FILE: superAccessAbstractProp.cj
abstract class AbsAccSuperPropParent {
    public prop p: Int64
}

class AbsAccSuperPropChild <: AbsAccSuperPropParent {
    public override prop p: Int64 {
        get() {
            return <!ABSTRACT_METHOD_CANNOT_BE_ACCESSED_DIRECTLY!>super<!>.p
        }
    }
}

// ── 形态 3：`super.抽象函数` 出现在调用实参里（锚点仍是 MemberAccess 首字符，不是整条表达式或实参列表）
// 官方 cjc 锚点为 `super` 首字符；CFIR 使用完整 `super` token。
// FILE: superAccessInCallArgument.cj
abstract class AbsAccSuperArgParent {
    public func f(): Int64
}

class AbsAccSuperArgChild <: AbsAccSuperArgParent {
    public func g(): Int64 {
        return 1 + <!ABSTRACT_METHOD_CANNOT_BE_ACCESSED_DIRECTLY!>super<!>.f()
    }

    public override func f(): Int64 {
        return 1
    }
}

// ── 形态 4：把接口当构造器调用（与形态 5 同属 CheckCallKind 的 switch，interface 分支排在 class 分支之前）
// kind：sema_interface_can_not_be_instantiated "interface 'AbsAccInstantiateIface' can not be instantiated"
// 锚点：CallExpr 首字符，即类型名 `AbsAccInstantiateIface` 的 A。1.0.5 / 1.1.3 一致 Range (6,13)..(6,14)。探针 p2 / q3。
// CFIR 对应诊断为 NO_CONSTRUCTOR，锚完整接口类型名 token。
// FILE: instantiateInterface.cj
interface AbsAccInstantiateIface {
    func f(): Int64
}

func absAccInstantiateInterface(): Int64 {
    let i = <!NO_CONSTRUCTOR!>AbsAccInstantiateIface<!>()
    return 0
}

// ── 形态 5：把 abstract class 当构造器调用（无参 / 带参两种；`super()` / `this()` 委托调用由 :2529-2530 豁免，见 L2）
// kind：sema_abstract_class_can_not_be_instantiated "abstract class 'AbsAccInstantiateParent' can not be instantiated"
//       / 同一 kind 的带参形态
// 锚点：类型名首字符。1.0.5 / 1.1.3 一致 Range (16,13)..(16,14)、(21,13)..(21,14)。探针 p2 / q3。
// FILE: instantiateAbstractClass.cj
abstract class AbsAccInstantiateParent {
    public func f(): Int64

    public init() {
    }
}

abstract class AbsAccInstantiateWithArgs {
    public func f(): Int64

    public init(a: Int64) {
    }
}

func absAccInstantiateNoArg(): Int64 {
    let a = <!ABSTRACT_CLASS_CAN_NOT_BE_INSTANTIATED!>AbsAccInstantiateParent<!>()
    return 0
}

func absAccInstantiateWithArg(): Int64 {
    let b = <!ABSTRACT_CLASS_CAN_NOT_BE_INSTANTIATED!>AbsAccInstantiateWithArgs<!>(1)
    return 0
}

// ── 形态 6：非 abstract 的 class 里的无体成员（同一个 class 同时产生声明级与类型级官方诊断）
//   sema_missing_func_body "function 'f' can not be abstract" / "property 'p' can not be abstract"，
//     锚成员声明首字符：无修饰符时是标识符本身（`func f` 的 f），有修饰符时是修饰符首字符（`public` / `protected` 的 p）；
//   sema_class_need_abstract_modifier_or_func_need_impl（每个 class 一条，锚类名首字符，附注锚未实现的成员声明）。
// 1.0.5 / 1.1.3 一致 Range (2,5)..(2,6)、(4,5)..(4,6)、(8,5)..(8,6)、(1,1)..(1,2)、(7,1)..(7,2)。探针 q4b / r4。
// FILE: missingBodyInNonAbstractClass.cj
class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>AbsAccMissingBodyNoModifier<!> {
    <!MISSING_FUNC_BODY!>func f(): Int64<!>

    <!MISSING_FUNC_BODY!>public prop p: Int64<!>
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>AbsAccMissingBodyProtected<!> {
    <!MISSING_FUNC_BODY!>protected func h(): Int64<!>
}

// ── 形态 7：abstract class 里的 static 无体成员（DeclAttributeChecker.cpp:297-299 的 static 条件成立；宿主是 abstract class，
//     所以 :862 的类型级诊断与 :876-880 的 extend / 继承分支都不触发，整段只有声明级诊断）。
// 锚点：成员声明首字符（`public` 的 p）。1.0.5 / 1.1.3 一致 Range (2,5)..(2,6)、(4,5)..(4,6)。探针 q4b。
// FILE: missingBodyStaticInAbstractClass.cj
abstract class AbsAccMissingStaticInAbstract {
    <!MISSING_FUNC_BODY!>public static func f(): Int64<!>

    <!MISSING_FUNC_BODY!>public static prop q: Int64<!>
}

// ── 形态 8：open class 里的 open 无体成员（非 abstract 条件成立，官方额外给类型级诊断；同 kind 同锚点，两版一致）
// 锚点：成员声明首字符。1.0.5 / 1.1.3 一致 Range (2,5)..(2,6)、(4,5)..(4,6)、(7,1)..(7,2)。探针 q4b。
// open 成员无体是否合法属声明状态族（官方同时会考虑 sema_ignore_open），本段只取"缺函数体"这一条。
// FILE: missingBodyOpenInOpenClass.cj
open class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>AbsAccMissingOpenInOpen<!> {
    <!MISSING_FUNC_BODY!>public open func h(): Int64<!>

    <!MISSING_FUNC_BODY!>public open prop r: Int64<!>
}

// ── 形态 9：override 返回 `This` 的 open 函数时，子类把返回类型改成父类名或子类自身名（StructInheritanceChecker.cpp:1218-1221）
// kind：sema_inherit_not_return_this "an open function that returns 'This' must keep the return type 'This' when overridden"
// 锚点：子函数名首字符（MakeRange(childFunc->identifier)，refactor kind 不退化为 GetBegin）；附注 "the overriden function" 锚父函数名。
// 1.0.5 / 1.1.3 一致 Range (8,26)..(8,27)、(14,26)..(14,27)。探针 q5b。
// 父函数返回类型不是 `This`、子类保持 `This`（见 L3）、以及 property 覆盖（:1197 非 FUNC_DECL 提前返回）都不报。
// FILE: inheritNotReturnThis.cj
open class AbsAccThisReturnParent {
    public open func f(): This {
        return this
    }
}

class AbsAccThisReturnParentType <: AbsAccThisReturnParent {
    public override func <!INHERIT_NOT_RETURN_THIS!>f<!>(): AbsAccThisReturnParent {
        return this
    }
}

class AbsAccThisReturnSelfType <: AbsAccThisReturnParent {
    public override func <!INHERIT_NOT_RETURN_THIS!>f<!>(): AbsAccThisReturnSelfType {
        return this
    }
}

// ── 形态 10：父类 public 成员在子类被覆盖成 protected（"case 1" 弱化组合；函数与 property 各一）
// kind：sema_weak_visibility；CFIR 使用语义等价的 CANNOT_WEAKEN_ACCESS_PRIVILEGE，锚完整子成员名 token。
// 写 private 同理且只多这一条（探针 v2：`private override prop` 只有 weak_visibility，不触发属性继承族诊断），本段用 protected。
// 父成员非 abstract / open / static 且宿主不是接口时 :1060-1063 直接返回，本段用 public open 成员。
// FILE: overrideWeakerVisibility.cj
open class AbsAccWeakVisParent {
    public open func f(): Int64 {
        return 1
    }

    public open prop p: Int64 {
        get() {
            return 1
        }
    }
}

class AbsAccWeakVisChildProtected <: AbsAccWeakVisParent {
    protected override func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>f<!>(): Int64 {
        return 2
    }
}

class AbsAccWeakVisChildProtectedProp <: AbsAccWeakVisParent {
    protected override prop <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>p<!>: Int64 {
        get() {
            return 2
        }
    }
}

// 合法对照段（1.0.5 / 1.1.3 两版均零诊断）：

// L1：抽象成员通过抽象类型的引用访问（非 super 的实例访问官方零诊断）；抽象类型的变量持有具体子类实例；
//     实例访问抽象类型的成员时不触发 MISSING_FUNC_BODY 或 ABSTRACT_CLASS_CAN_NOT_BE_INSTANTIATED。探针 p8。
// FILE: abstractMemberAccessThroughAbstractType.cj
abstract class AbsAccLegalBase {
    public func f(): Int64

    public func g(): Int64 {
        return 1
    }
}

class AbsAccLegalImpl <: AbsAccLegalBase {
    public override func f(): Int64 {
        return 1
    }
}

func absAccLegalUse(): Int64 {
    let x: AbsAccLegalBase = AbsAccLegalImpl()
    return x.f() + x.g()
}

// L2：`super.具体成员` 调用合法（CheckInstanceMemberAccessLegality 只拦 ABSTRACT 目标），子类构造器里的 `super()`
//     委托调用对 abstract 父类豁免（CheckCallKind :2529-2530）。探针 q3 / s2 / t1。
// FILE: legalSuperAccessAndDelegation.cj
abstract class AbsAccLegalCtorParent {
    public func f(): Int64

    public func g(): Int64 {
        return 1
    }

    public init() {
    }
}

class AbsAccLegalCtorChild <: AbsAccLegalCtorParent {
    public init() {
        super()
    }

    public override func f(): Int64 {
        return super.g()
    }
}

// L3：override 返回 `This` 的 open 函数时子类保持 `This`；覆盖返回非 This 的 open 函数同样合法（非继承 NotReturnThis）。
// FILE: thisReturnKeptInOverride.cj
open class AbsAccLegalThisParent {
    public open func f(): This {
        return this
    }

    public open func g(): Int64 {
        return 1
    }
}

class AbsAccLegalThisChild <: AbsAccLegalThisParent {
    public override func f(): This {
        return this
    }

    public override func g(): Int64 {
        return 2
    }
}

// L4：抽象成员（无体声明）放在 abstract class / interface 里合法；public 与 protected 可见性都合法。
// FILE: abstractMembersInAbstractClassAndInterface.cj
abstract class AbsAccLegalAbstractHost {
    public func f(): Int64

    protected prop q: Int64
}

interface AbsAccLegalIface {
    func g(): Int64

    prop r: Int64
}

// L5：覆盖时保持 public（父成员 public abstract）、或父子同为 protected（父成员须写 protected open，否则先报
//     sema_cannot_override"the parent function is not open"）都零诊断；两个子类都要实现父类抽象成员 f，
//     否则官方先报 sema_class_need_abstract_modifier_or_func_need_impl（属形态 6 的类型级诊断）。探针 r5 / v1。
// FILE: legalOverrideVisibilityKept.cj
abstract class AbsAccLegalVisParent {
    public func f(): Int64

    protected open func g(): Int64 {
        return 1
    }
}

class AbsAccLegalVisChildPublic <: AbsAccLegalVisParent {
    public override func f(): Int64 {
        return 1
    }
}

class AbsAccLegalVisChildProtected <: AbsAccLegalVisParent {
    public override func f(): Int64 {
        return 1
    }

    protected override func g(): Int64 {
        return 1
    }
}

// 已删除的形态（取证 kind 与 Range 见下，除注明外 1.0.5 与 1.1.3 一致）：
//   1. 成员上写显式 `abstract` 修饰符（`class C { public abstract func f(): Int64 }`、
//      `abstract class C { public abstract func g(): Int64 }`）
//      → 1.0.5：parse_illegal_modifier_in_scope "unexpected modifier 'abstract' on function declaration in class body"
//        （锚 `abstract` 修饰符 8 个字符，Range (n,12)..(n,20)）；1.1.3 额外报
//        parse_explicitly_abstract_only_for_cjmp_abstract_class "only common/specific or Native FFI mirror abstract classes can have
//        explicitly abstract members"（锚整条成员声明，Range (n,5)..(n,36)）。官方 v1.0.0 无后者（include/ 与 src/ 全树 grep 不到
//        explicitly_abstract）；本项目 EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS（列表 :2126）对应的官方 kind 在 v1.0.0 不存在，
//        且 1.1.3 报的是 PARSE_ 前缀名（PARSE_EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS，列表 :2216，已由
//        declaration-status/staticCannotBeOpenAbstractOverride.cj 覆盖）。本文件的"抽象成员"一律写成无函数体的成员声明，官方
//        src/Parse/ParseDecl.cpp:1548-1555 在 CLASS_BODY / INTERFACE_BODY 给无体成员打 ABSTRACT 标记，语义等价。探针 p6。
//   2. extend 块里的无体成员（`extend C { func y(): Int64 }`）→ parse_missing_body "body of function 'y' is missing"
//        （锚类型名之后的 1 个字符，Range (44,20)..(44,21)）；extend 成员不走 DeclAttributeChecker 的 class 成员路径，
//        MISSING_FUNC_BODY 不适用，且该形态会触发 parse 错误屏蔽整段的语义诊断（所以探针里单独放在最后一段）。探针 q4。
//   3. 实现类的构造器里 `super.接口抽象成员()` → sema_not_member_of "'h' is not a member of class 'Object'"（锚成员名首字符）。
//        官方接口没有 Object 父类型，接口成员不进入 super 查找，所以"通过 super 访问接口的抽象成员"形态不存在；形态 1-3 只能用
//        抽象类父类的成员。探针 p1 / q1。
//   4. 接口成员写 `This` 返回类型（`interface I { func h(): This }`）→ parse_this_type_not_allow "'This' type is not allowed"
//        （锚 `This` 4 个字符，Range (50,15)..(50,19)）；"接口成员 override 时丢失 This" 形态因此不存在，INHERIT_NOT_RETURN_THIS
//        只有类这一条路径。探针 q5。
//   5. private（或不写修饰符的 internal）抽象成员放在 abstract class 里 → sema_invalid_member_visibility_in_class
//        "the visibility of an 'abstract' function must be 'public' or 'protected'"（锚成员名首字符，Range (2,18)..(2,19)），
//        属可见性族，已由 inheritance/overrideRedefRules.cj 形态 9 覆盖；官方 def:217 sema_privated_abstract_func_in_class 在 v1.0.0
//        的 src/ 里没有报告点（只在 .def 中定义），本文件不取该形态。探针 p7。
//   6. struct 不能被继承（`class C <: SomeStruct`）→ sema_class_inherit_non_class_nor_interface；struct 成员也没有 abstract 形态，
//        所以"struct 里的 super 访问抽象成员"不存在。探针 q1。
//   7. 非 abstract class 只实现部分父类抽象成员（只 override `g()` 不实现 `f()`）→ sema_class_need_abstract_modifier_or_func_need_impl
//        （锚类名首字符，与形态 6 的类型级诊断同 kind），所以 L5 的两个子类都实现 f；不实现形态在 inheritance 目录另有
//        ABSTRACT_MEMBER_NOT_IMPLEMENTED 侧的覆盖。探针 r5 / v1。
//   8. 本项目 CANNOT_OVERRIDE_INVISIBLE_MEMBER 对应的形态被官方 NOTHING_TO_OVERRIDE 语义覆盖（见头部），已由既有夹具覆盖，不重复。
