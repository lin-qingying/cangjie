// RUN_PIPELINE_TILL: FRONTEND
// 继承图规则语义夹具：继承环（`INHERITANCE_CYCLE`）、非 class-nor-interface 超类型
// （`CLASS_INHERIT_NON_CLASS_NOR_INTERFACE`）、接口成员未实现 / 需要实现
// （`INTERFACE_MEMBER_MUST_BE_IMPLEMENTED` / `NEED_MEMBER_IMPLEMENTATION`）、
// override 非 open 父类成员（`CANNOT_OVERRIDE`）。
//
// 取证方式：官方 cjc 1.0.5 与 1.1.3 两版实测，静态库模式
//   `cjc <段>.cj -o out.a --output-type=staticlib --diagnostic-format json`；每个 `// FILE:` 段单独编译
//   （官方整包编译时一处 parse 错误会吞掉其余段的语义诊断，见 DiagnosticEngine.cpp 的 CanBeEmitted）。
//   54 个探针段两版的 kind、文案、MainHint.Range 逐段完全一致，因此不拆 LANGUAGE_VERSION 夹具。
//   1.1.3 在语义检查**之后**的 codegen / Reflection 阶段对部分段失败（stderr "Reflection format: modifier
//   type is invalid" / "LLVM ERROR: Broken global value"），语义诊断 JSON 完整；cjc 语义有错时不进入 codegen，
//   所以这类段 JSON 为空即证明前端零语义错误。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1）。
//   前四个 kind 都是非 refactor kind，官方 `Diagnose(node, kind)`（include/cangjie/Basic/DiagnosticEngine.h:844-857）
//   退化成 `GetBegin()` **1 个字符**——锚在**声明的第一个 token**（`open` / `class` / `struct` / `enum` /
//   `interface` / `extend` 关键字的首字母）或 `<:` 后面那个超类型名的首字母，不是整个声明、也不是类型名。
//   `sema_cannot_override` 是 refactor kind（DiagRefactor/DiagnosticSema.def:112），锚**成员名整段**。
// 内联标记写法：小于感叹号 + 诊断名 + 大于感叹号 + 锚点文本 + 结束标记；全文只在 `// FILE:` 段内出现，
//   头部注释一律用文字描述，不出现该序列的字面量。
// **未跑 CFIR**：本文件不依据本项目 CFIR 的实际输出编写，头部只按源码写出 CFIR 名、生产者位置与可预判的锚点差异。
//
// 官方实现（external/cangjie_compiler，工作树 checkout 在 tag v1.0.0）：
//   A. 继承环 → src/Sema/PreCheck.cpp:1315-1322 `CheckInheritanceCycleDFSHandleVisiting`：
//      `diag.Diagnose((extendDecl == nullptr) ? decl : *extendDecl, DiagKind::sema_inheritance_cycle,
//      ConstructDiagInfoForCycle(decl, path))` —— 锚 1 字符；**有 extend 参与时报在 extend 块上**；
//      消息里的链条从环上第一次出现的 decl 起按 `标识符->` 拼接（:35-52 `ConstructDiagInfoForCycle`）。
//      DFS 入口 :1204-1235 `StructDeclCircleOrDupCheckForOneSymbol`：class / interface 走
//      `CheckInheritanceCycleDFS`（:1289-1310，逐个 `<:` 目标递归），struct / enum 只查重复接口
//      （`CheckDupInterfaceInStructDecl` :1246-1253），**struct / enum 自身不查环**。
//      :1256-1269 `CheckInheritanceCycleHelper`：目标是 typeAlias 时**穿过别名继续找真实类型**
//      （所以"经类型别名成环"可达），`te` 不是 REF_TYPE / QUALIFIED_TYPE 时报
//      `sema_inheritance_non_ref_type`（本项目无同名 CFIR 名，见形态 12 / 13 的伴随诊断）。
//      文案 include/cangjie/Basic/DiagnosticSema.def:37 "inheritance cycle detected: '%s'"。
//   B. 非 class-nor-interface 超类型 → src/Sema/TypeCheckClassLike.cpp:146-150：`<:` 的目标既不是 class
//      也不是 interface（struct / enum / `Array<T>` / `Option<T>` / 指向 struct 的别名 / 泛型形参）时
//      `diag.Diagnose(type, DiagKind::sema_class_inherit_non_class_nor_interface, cd.identifier.Val())`
//      → 锚 1 字符（超类型名首字母）；`cd.identifier.Val()` 进消息。同函数 :139-145 是
//      `sema_non_inheritable_super_class`（父类非 open/abstract，已有夹具覆盖，不重复）。
//      文案 DiagnosticSema.def:252 "class '%s' can only inherit a class or implement interfaces"。
//   C. 接口成员未实现 / 需要实现 → src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:855-878：
//      宿主是非 abstract 的 class-like（class / struct / enum）时
//      :863-867 报 `sema_class_need_abstract_modifier_or_func_need_impl`（本项目无同名 CFIR 名 → SUGGESTED，
//      见形态 26）；否则 `member.shouldBeImplemented`（接口成员**没有默认实现**；宿主 class-like / extend
//      都算）为真时 :874-875 `diag.Diagnose(structDecl, DiagKind::sema_interface_member_must_be_implemented,
//      type, identifier, structName)` → 锚 1 字符（宿主声明首 token）；:871-872
//      `diag.Diagnose(structDecl, DiagKind::sema_need_member_implementation, …)` → 同锚点。
//      :778-790 `diagForIncompleteOverrideOrImplement`：在 extend 里实现泛型宿主的接口成员而 extend 不在
//      宿主泛型实例化范围内时，接口成员带 `default` 报 `sema_cannot_override`（refactor），否则报
//      `sema_interface_member_must_be_implemented_in_struct`（本项目无 CFIR 名 → SUGGESTED）。
//      文案 DiagnosticSema.def:230 "interface %s '%s' must be implemented in '%s'"、:232
//      "implementation of %s '%s' is needed in '%s'"、:198 "class %s missing abstract modifier, otherwise
//      abstract function or property must be implemented"（refactor）。
//   D. override 非 open 父类成员 → src/Sema/Diags.cpp:368-377 `DiagCannotOverride`：
//      `childRange = MakeRange(child.identifier)`、`diag.DiagnoseRefactor(sema_cannot_override, child,
//      childRange, kind, name)` → 锚**成员名整段**，note "the parent <kind> is not modified by 'open'"。
//      调用点 StructInheritanceChecker.cpp:985-1008：父类成员非 abstract / 非 static 时，
//      :993（CJNATIVE 分支，本机不走）`(!OPEN && notInInterface) || IsDeclOpenToMock(parent)` → `DiagCannotOverride`；
//      :996-1001 父类成员是**泛型**函数 / prop 时改报 `sema_overload_conflicts`
//      （本项目无同名 CFIR 名 → SUGGESTED）；:1002-1005 子类成员带 `abstract` 且不在 interface 时
//      `sema_invalid_override_member_in_class`（已有夹具覆盖）。文案 DiagRefactor/DiagnosticSema.def:112
//      "cannot override %s '%s'"。
//
// 本项目 CFIR 名与生产者（cfir/checkers/checkers-component-generator/.../diagnostics/CfirDiagnosticsList.kt）：
//   INHERITANCE_CYCLE(:129, `by error<PsiElement>(PositioningStrategy.DEFAULT)`；cone 层映射
//     cfir/checkers/src/.../diagnostics/coneDiagnosticToCfirDiagnostic.kt:2521)
//   CLASS_INHERIT_NON_CLASS_NOR_INTERFACE(:143, `by error<CjTypeReference>`；CfirSupertypesChecker.kt:198，
//     source = `superTypeRef.declaredSupertypeClassifierSource()`)
//   INTERFACE_MEMBER_MUST_BE_IMPLEMENTED(:1535) / NEED_MEMBER_IMPLEMENTATION(:1530)（无定位策略 ⇒ 默认
//     PsiElement 整段）；两者生产者都在 CfirInheritanceDeepChecker.kt（:193 与 :1001 文案表），
//     source = `declaration.classLikeNameDiagnosticSource()`（**宿主类型名**）
//   CANNOT_OVERRIDE(:1518, `by error<PsiElement>`)；生产者 CfirInheritanceDeepChecker.kt:186，触发条件是
//     `superInfo.isDefaultInterfaceMember(context)`，source 同样是 `classLikeNameDiagnosticSource()`
//   文案表 cfir/checkers/src/.../diagnostics/CfirErrorsDefaultMessages.kt:95（INHERITANCE_CYCLE
//     "inheritance cycle detected"，与官方同措辞但**不带链条实参**）、:156、:993、:1001、:1006。
//   按源码可预判的锚点差异（**未跑 CFIR**）：`CANNOT_OVERRIDE` 与两条 must-impl 系官方锚"成员名整段 / 宿主声明首
//   token"，CFIR 统一锚 `classLikeNameDiagnosticSource()`（宿主类型名），预计形态不同；
//   `CLASS_INHERIT_NON_CLASS_NOR_INTERFACE` 两侧都锚超类型引用、首字母形态可能一致；
//   `INHERITANCE_CYCLE` 走 cone 层且定位策略是 DEFAULT，实际锚点以实跑为准。
//
// 已有覆盖不重复（先读）：
//   - `testData/diagnostics2/inheritance/` 21 个夹具已用：MISSING_FUNC_BODY(7)、NOTHING_TO_OVERRIDE(4)、
//     INVALID_MEMBER_VISIBILITY_IN_CLASS(4)、INTERFACE_CANNOT_INHERIT_CLASS(4)、
//     STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME(3)、PARAM_NAMED_MISMATCHED(3)、
//     INVALID_OVERRIDE_MEMBER_IN_CLASS(3)、ILLEGAL_MULTI_INHERITANCE(3)、
//     GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS(3)、ABSTRACT_METHOD_CANNOT_BE_ACCESSED_DIRECTLY(3)、
//     WEAK_VISIBILITY(2)、NON_INHERITABLE_SUPER_CLASS(2)、INHERIT_NOT_RETURN_THIS(2)、
//     ABSTRACT_CLASS_CAN_NOT_BE_INSTANTIATED(2)、以及 THIS_SUPER_USE_ERROR_OUTSIDE_CLASS、
//     SUPER_TYPES_SELF_REFERENCE、SUPER_TYPES_DUPLICATE、SUPERCLASS_MUST_BE_PLACED_AT_FIRST、
//     STRUCT_SUPER_NOT_ALLOWED、RETURN_TYPE_INCOMPATIBLE、REDUNDANT_MODIFIER、REDEF_INSTANCE_ERROR、
//     INVALID_THIS_CALL_OUTSIDE_CTOR、ILLEGAL_SUPER_ALONE、ENUM_SUPER_NOT_ALLOWED、CANNOT_INHERIT_SEALED。
//     其中 `multipleClassSupers.cj` 用的是 `ILLEGAL_MULTI_INHERITANCE`（官方 `sema_illegal_multi_inheritance`，
//     多个类父类），**不是**本文件的 `MULTIPLE_CLASS_SUPER_TYPES`（后者对应官方 `sema_multiple_class_inheritance`，
//     在 v1.0.0 `src/` 无报告点 → 死条目，SUGGESTED 记录）。
//   - LLT 侧：`llt/class/class1.cj`（class ↔ class 环 + INHERITANCE_CYCLE + NON_INHERITABLE_SUPER_CLASS +
//     RECURSIVE_CONSTRUCTOR_CALL）、`llt/class/class_property/property8.cj`（CANNOT_OVERRIDE ×3，`mut prop`）、
//     `llt/class/class_no_override_modifier_invalid_5.cj`（INTERFACE_MEMBER_MUST_BE_IMPLEMENTED，**class 宿主** +
//     泛型参数）、`llt/class/declared_supertype_classification.cj`（CLASS_INHERIT_NON_CLASS_NOR_INTERFACE：
//     struct / enum / struct 别名 / 泛型形参 T）。
//   本文件只补它们没写的：interface 环、类经接口的环、别名环、extend 参与的环、struct / enum 宿主 extend 环、
//   非 class-nor-interface 的 `Array` / `Option` / 别名→原始类型 / 别名→元组、must-impl 的 struct / enum /
//   extend / 泛型 struct / prop / 三接口宿主、need-impl 的 struct / enum / extend / 泛型 / 两成员形态、
//   CANNOT_OVERRIDE 的函数 / 不可变 prop / 三层继承，以及父类成员带 `open`、父类成员是 static 的合法对照。
//
// 各段 Range 均按**逐段单独编译**的干净文本行号（`// FILE:` 行被剥离），故正文里的形如 (4,1) 是段内行号。

// ── 形态 1：两个 interface 互相继承 —— 锚在**第一个** interface 的 `interface` 关键字首字母
// kind：sema_inheritance_cycle "inheritance cycle detected: 'IgIpA->IgIpB->IgIpA'"；Range (1,1)..(1,2)，两版一致。
// 官方只在 DFS 里第一次回到 VISITING 的那个 decl 上报一条，所以环上另一个 interface 不再报（实测仅 1 条）。
// FILE: inheritanceCycleInterfacePair.cj
<!INHERITANCE_CYCLE!>interface IgIpA<!> <: IgIpB {}
interface IgIpB <: IgIpA {}

// ── 形态 2：class ↔ interface 成环（interface 在中间）—— 锚 interface 声明，两个 class 侧都不报
// kind：sema_inheritance_cycle "…'IgCiMid->IgCiRoot->IgCiMid'"；Range (2,1)..(2,2)，两版一致（仅 1 条）。
// 与形态 1 的差别：环的根不是文件里第一个声明（`IgCiSub` 在环外）；DFS 从每个 class / interface 起点各走一遍。
// FILE: inheritanceCycleClassViaInterface.cj
class IgCiSub <: IgCiMid {}
<!INHERITANCE_CYCLE!>interface IgCiMid<!> <: IgCiRoot {}
class IgCiRoot <: IgCiMid {}

// ── 形态 3：经类型别名成环（别名指向 open class，class 父类写别名）—— 锚 class 声明的 `open` 修饰符首字母
// kind：sema_inheritance_cycle "…'IgCaoSub->IgCaoSub'"（穿过别名后链条只剩一个名字，官方 :1260-1262 会递归进
// 别名 RHS，所以这条路径可达）；Range (1,1)..(1,2)，两版一致。
// 伴随诊断 sema_recursive_constructor_call "recursive constructor calling detected"（Range (1,12)..(1,20)，
// 锚 `IgCaoSub` 整段，notes "depends on"）属构造器族（diagnostics2/constructor/superThisCallRules.cj 已覆盖），
// 本文件只记录不写标记。对照 LLT class1.cj 的 class ↔ class 环（那边锚 `class` 关键字，本段锚点规则一致）。
// FILE: inheritanceCycleViaTypeAliasOpen.cj
<!INHERITANCE_CYCLE!>o<!>pen class IgCaoSub <: IgCaoAlias {}
type IgCaoAlias = IgCaoSub

// ── 形态 4：extend 参与的环（interface 三元环，extend 挂在环上）—— 锚 **extend 块**的 `extend` 首字母
// kind：sema_inheritance_cycle "…'IgCeC->IgCeA->IgCeB->IgCeC'"；Range (5,1)..(5,2)，两版一致（仅 1 条）。
// 官方 :1321 的 `extendDecl == nullptr ? decl : *extendDecl`：DFS 从 `IgCeTarget` 经 extend 走到环后，
// 报告落在 extend 块而不是环上任何一个 interface。
// FILE: inheritanceCycleViaExtend.cj
class IgCeTarget {}
interface IgCeA <: IgCeB {}
interface IgCeB <: IgCeC {}
interface IgCeC <: IgCeA {}
<!INHERITANCE_CYCLE!>extend IgCeTarget<!> <: IgCeC {}

// ── 形态 5：interface 环 + extend 挂在 **struct** 上 —— 锚环上第一个 interface
// kind：sema_inheritance_cycle "…'IgCsxA->IgCsxB->IgCsxA'"；Range (2,1)..(2,2)，两版一致（仅 1 条）。
// 与形态 4 的差别：struct 自身不查环（官方 :1226-1231 struct / enum 只查重复接口），extend 也不是环的一部分，
// 所以锚回 interface。
// FILE: inheritanceCycleExtendOnStruct.cj
struct IgCsxS {}
<!INHERITANCE_CYCLE!>i<!>nterface IgCsxA <: IgCsxB {}
interface IgCsxB <: IgCsxA {}
extend IgCsxS <: IgCsxA {}

// ── 形态 6：同形态 5 但 extend 挂在 **enum** 上
// kind：sema_inheritance_cycle "…'IgCexA->IgCexB->IgCexA'"；Range (2,1)..(2,2)，两版一致。
// FILE: inheritanceCycleExtendOnEnum.cj
enum IgCexE { A }
<!INHERITANCE_CYCLE!>i<!>nterface IgCexA <: IgCexB {}
interface IgCexB <: IgCexA {}
extend IgCexE <: IgCexB {}

// ── 形态 7：interface 环 + extend 挂在 class 上 + class 继承该 extend 宿主 —— 锚 extend 块
// kind：sema_inheritance_cycle "…'IgCveI2->IgCveI3->IgCveI1->IgCveI2'"；Range (5,1)..(5,2)，两版一致。
// 伴随诊断 sema_non_inheritable_super_class "super class 'IgCveRoot' is not inheritable"（Range (6,20)..(6,21)，
// 锚 `IgCveRoot` 首字母，CFIR 名 NON_INHERITABLE_SUPER_CLASS，已由 classAndInterfaceInheritanceBoundaries.cj
// 覆盖）——`IgCveRoot` 不是 open，只记录不写标记。
// FILE: inheritanceCycleViaExtendInterface.cj
class IgCveRoot {}
interface IgCveI1 <: IgCveI2 {}
interface IgCveI2 <: IgCveI3 {}
interface IgCveI3 <: IgCveI1 {}
<!INHERITANCE_CYCLE!>e<!>xtend IgCveRoot <: IgCveI2 {}
class IgCveLeaf <: IgCveRoot {}

// ── 形态 8：interface 环 + extend 挂在 interface 上 —— 锚环上第一个 interface，extend 侧另有一条 refactor 诊断
// kind：sema_inheritance_cycle "…'IgEoiA->IgEoiB->IgEoiA'"；Range (1,1)..(1,2)，两版一致。
// 伴随诊断 sema_illegal_extended_type "extending type 'Interface-IgEoiA' is not allowed"（Range (3,8)..(3,14)，
// 锚 `IgEoiA` 整段，refactor kind；CFIR 名 ILLEGAL_EXTENDED_TYPE，已由 extend/extendIllegalMember.cj 覆盖）
// 只记录不写标记。官方 PreCheckExtend 先报这条、Sema 再查环，两条互不屏蔽。
// FILE: inheritanceCycleExtendOnInterface.cj
<!INHERITANCE_CYCLE!>interface IgEoiA<!> <: IgEoiB {}
interface IgEoiB <: IgEoiA {}
extend IgEoiA <: IgEoiB {}

// ── 形态 9：class 的父类是 `Array<T>` —— 锚 `Array` 的首字母
// kind：sema_class_inherit_non_class_nor_interface "class 'IgNaqC' can only inherit a class or implement
// interfaces"；Range (1,17)..(1,18)，两版一致。官方 TypeCheckClassLike.cpp:146-150。
// 对照 LLT declared_supertype_classification.cj 的 struct / enum / struct 别名 / 泛型形参 T 四种，本段只补容器类型。
// FILE: classInheritsArray.cj
class IgNaqC <: <!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>Array<!><Int64> {}

// ── 形态 10：class 的父类是 `Option<T>` —— 锚 `Option` 的首字母
// kind：sema_class_inherit_non_class_nor_interface；Range (1,16)..(1,17)，两版一致。
// FILE: classInheritsOption.cj
class IgNoC <: <!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>Option<!><Int64> {}

// ── 形态 11：class 的父类是**两级别名**（别名指向 struct）—— 锚别名的首字母，不穿透到 struct
// kind：sema_class_inherit_non_class_nor_interface "class 'IgNaC' …"；Range (4,16)..(4,17)，两版一致。
// 官方只判 `<:` 位置上语法写下的那个 decl（`rt->ref.target`），别名的展开类型不参与判定。
// FILE: classInheritsAliasChainToStruct.cj
struct IgNaS {}
type IgNaA1 = IgNaS
type IgNaA2 = IgNaA1
class IgNaC <: <!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>IgNaA2<!> {}

// ── 形态 12：class 的父类是**指向原始类型的别名** —— 锚别名首字母，伴随一条无 CFIR 名的 non-ref 诊断
// kind：sema_class_inherit_non_class_nor_interface；Range (2,17)..(2,18)，两版一致。
// 伴随诊断 sema_inheritance_non_ref_type "inheritance is not a ref type: 'IgNapC'"（Range (2,1)..(2,2)，
// 锚 class 声明首 token；`DiagnosticSema.def:190`，官方 PreCheck.cpp:1268 在 DFS 里发现 `te` 展开后不是
// REF_TYPE / QUALIFIED_TYPE 时报；本项目无同名 CFIR 名 → 按 SUGGESTED 记录，不写标记）。
// 直接写 `class C <: Int64` 会被 parser 拦（见"已删除的形态 1"），别名是唯一可达路径。
// FILE: classInheritsAliasToPrimitive.cj
type IgNapT = Int64
class IgNapC <: <!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>IgNapT<!> {}

// ── 形态 13：class 的父类是**指向元组的别名**（别名再套一层）—— 锚别名首字母
// kind：sema_class_inherit_non_class_nor_interface；Range (3,17)..(3,18)，两版一致。
// 伴随诊断同形态 12 的 sema_inheritance_non_ref_type（Range (3,1)..(3,2)），同样只记录不写标记。
// FILE: classInheritsAliasToTuple.cj
type IgNtnT1 = (Int64, Int64)
type IgNtnT2 = IgNtnT1
class IgNtnC <: <!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>IgNtnT2<!> {}

// ── 形态 14：struct 宿主 + 两个接口的同名**带体**成员（struct 里已有同名实现）—— 锚 struct 首字母
// kind：sema_interface_member_must_be_implemented "interface function 'foo' must be implemented in 'IgMsS'"；
// Range (11,1)..(11,2)，两版一致。官方 StructInheritanceChecker.cpp:874-875（同名成员判为 `shouldBeImplemented`）。
// 对照 LLT class_no_override_modifier_invalid_5.cj（**class 宿主** + 泛型参数），本段是 struct 宿主。
// FILE: interfaceMemberMustBeImplementedInStruct.cj
interface IgMsI1 {
    func foo(): Int64 {
        return 1
    }
}
interface IgMsI2 {
    func foo(): Int64 {
        return 2
    }
}
struct <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>IgMsS<!> <: IgMsI1 & IgMsI2 {}

// ── 形态 15：struct 宿主 + 两个接口的同名带体 **prop** 成员 —— 锚 struct 首字母
// kind：sema_interface_member_must_be_implemented "interface property 'a' must be implemented in 'IgMpS'"；
// Range (15,1)..(15,2)，两版一致。
// FILE: interfacePropMustBeImplementedInStruct.cj
interface IgMpI1 {
    prop a: Int64 {
        get() {
            return 1
        }
    }
}
interface IgMpI2 {
    prop a: Int64 {
        get() {
            return 2
        }
    }
}
struct <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>IgMpS<!> <: IgMpI1 & IgMpI2 {}

// ── 形态 16：enum 宿主 + 两个接口的同名带体成员 —— 锚 enum 首字母
// kind：sema_interface_member_must_be_implemented "… must be implemented in 'IgMfE'"；Range (11,1)..(11,2)。
// FILE: interfaceMemberMustBeImplementedInEnum.cj
interface IgMfI1 {
    func foo(): Int64 {
        return 1
    }
}
interface IgMfI2 {
    func foo(): Int64 {
        return 2
    }
}
<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>num IgMfE <: IgMfI1 & IgMfI2 {
    A
}

// ── 形态 17：**extend 块**作为宿主（struct 自己不实现接口，extend 里也没有）—— 锚 extend 首字母
// kind：sema_interface_member_must_be_implemented "… must be implemented in 'extend IgMeS'"；
// Range (12,1)..(12,2)，两版一致。消息第三个实参是 `extend` + 宿主名（官方 :874-875 传 `structName`）。
// FILE: interfaceMemberMustBeImplementedInExtend.cj
interface IgMeI1 {
    func foo(): Int64 {
        return 1
    }
}
interface IgMeI2 {
    func foo(): Int64 {
        return 2
    }
}
struct IgMeS {}
<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>extend<!> IgMeS <: IgMeI1 & IgMeI2 {}

// ── 形态 18：泛型 struct 宿主 + 两个接口的同名泛型带体成员（实参化后）—— 锚 struct 首字母
// kind：sema_interface_member_must_be_implemented "… must be implemented in 'IgMsgS'"；Range (11,1)..(11,2)。
// FILE: interfaceMemberMustBeImplementedInGenericStruct.cj
interface IgMsgI1<T> {
    func foo(x: T): T {
        return x
    }
}
interface IgMsgI2<T> {
    func foo(x: T): T {
        return x
    }
}
struct <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>IgMsgS<!> <: IgMsgI1<Int64> & IgMsgI2<Int64> {}

// ── 形态 19：struct 宿主 + 三个接口的同名带体成员 —— 锚 struct 首字母，仍只报一条
// kind：sema_interface_member_must_be_implemented "… must be implemented in 'IgM3S'"；Range (16,1)..(16,2)。
// 与形态 14 的差别只是接口数量，条数与锚点规则不变（官方按成员去重）。
// FILE: interfaceMemberMustBeImplementedThreeInterfaces.cj
interface IgM3I1 {
    func foo(): Int64 {
        return 1
    }
}
interface IgM3I2 {
    func foo(): Int64 {
        return 2
    }
}
interface IgM3I3 {
    func foo(): Int64 {
        return 3
    }
}
struct <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>IgM3S<!> <: IgM3I1 & IgM3I2 & IgM3I3 {}

// ── 形态 20：struct 宿主 + 接口成员**没有实现体**（无默认实现）—— need-impl 系，锚 struct 首字母
// kind：sema_need_member_implementation "implementation of function or property 'foo' is needed in 'IgNfS'"；
// Range (4,1)..(4,2)，两版一致。官方 StructInheritanceChecker.cpp:871-872。
// FILE: interfaceMemberNeedsImplementationInStruct.cj
interface IgNfI {
    func foo(): Int64
}
<!NEED_MEMBER_IMPLEMENTATION!>s<!>truct IgNfS <: IgNfI {}

// ── 形态 21：struct 宿主 + 接口的无体 prop 成员
// kind：sema_need_member_implementation；Range (4,1)..(4,2)，两版一致。
// FILE: interfacePropNeedsImplementationInStruct.cj
interface IgNp2I {
    prop a: Int64
}
<!NEED_MEMBER_IMPLEMENTATION!>s<!>truct IgNp2S <: IgNp2I {}

// ── 形态 22：extend 宿主 + 接口无体成员 —— 锚 extend 首字母
// kind：sema_need_member_implementation "… is needed in 'extend IgNxS'"；Range (5,1)..(5,2)，两版一致。
// FILE: interfaceMemberNeedsImplementationInExtend.cj
interface IgNxI {
    func foo(): Int64
}
struct IgNxS {}
<!NEED_MEMBER_IMPLEMENTATION!>e<!>xtend IgNxS <: IgNxI {}

// ── 形态 23：enum 宿主 + 接口无体成员 —— 锚 enum 首字母
// kind：sema_need_member_implementation "… is needed in 'IgNe2E'"；Range (4,1)..(4,2)，两版一致。
// FILE: interfaceMemberNeedsImplementationInEnum.cj
interface IgNe2I {
    func foo(): Int64
}
<!NEED_MEMBER_IMPLEMENTATION!>e<!>num IgNe2E <: IgNe2I {
    A
}

// ── 形态 24：泛型 struct 宿主 + 泛型接口无体成员（实参化）—— 锚 struct 首字母
// kind：sema_need_member_implementation "… is needed in 'IgNgsS'"；Range (4,1)..(4,2)，两版一致。
// FILE: interfaceMemberNeedsImplementationInGenericStruct.cj
interface IgNgsI<T> {
    func foo(x: T): T
}
<!NEED_MEMBER_IMPLEMENTATION!>s<!>truct IgNgsS <: IgNgsI<Int64> {}

// ── 形态 25：struct 宿主 + 接口有两个无体成员（func + prop）—— 仍只报一条
// kind：sema_need_member_implementation；Range (5,1)..(5,2)，两版一致（官方按宿主 decl 报一次，消息里带
// 第一个未实现成员的种类与名字）。
// FILE: interfaceTwoMembersNeedImplementation.cj
interface IgNtmI {
    func foo(): Int64
    prop a: Int64
}
<!NEED_MEMBER_IMPLEMENTATION!>s<!>truct IgNtmS <: IgNtmI {}

// ── 形态 26（SUGGESTED 段，不写内联标记）：class 宿主 + 接口无体成员
// 官方改报 sema_class_need_abstract_modifier_or_func_need_impl "class 'IgNcC' missing abstract modifier,
// otherwise abstract function or property must be implemented"（Range (4,1)..(4,2)，`DiagnosticSema.def:198`，
// refactor kind，官方 StructInheritanceChecker.cpp:863-867），**本项目无同名 CFIR 名**，故整段按 SUGGESTED 记录。
// 该形态的"host 缺 abstract 修饰符"诊断已由 inheritance/abstractMemberAccessRules.cj 的 MISSING_FUNC_BODY
// 与 README 批次 10 的 SUGGESTED 段记录，本段只补 class 宿主的取证。
/*
SUGGESTED_DIAGNOSTIC: 无对应 CFIR 名（官方 sema_class_need_abstract_modifier_or_func_need_impl）
SUGGESTED_MESSAGE: class 'IgNcC' missing abstract modifier, otherwise abstract function or property must be implemented
SOURCE: include/cangjie/Basic/DiagnosticSema.def:198；src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:863-867；
        探针 igrNeedImplClassFunc（1.0.5 / 1.1.3 一致，Range (4,1)..(4,2)）
*/
// FILE: interfaceMemberNeedsImplementationInClass.cj
interface IgNcI {
    func foo(): Int64
}
open class IgNcC <: IgNcI {}

// ── 形态 27：子类重写**非 open** 父类的同名函数（没写 `override` 也算 override）—— 锚成员名整段
// kind：sema_cannot_override "cannot override function 'foo'"（refactor kind，Diags.cpp:368-377 显式
// `MakeRange(child.identifier)`）；Range (7,17)..(7,20)，两版一致；note "the parent function is not
// modified by 'open'"。与 LLT property8.cj 的 `mut prop` 形态对照，本段是函数 + 不可变 prop。
// FILE: cannotOverrideNonOpenFunc.cj
open class IgCoBase {
    public func foo(): Int64 {
        return 1
    }
}
class IgCoSub <: IgCoBase {
    public func <!CANNOT_OVERRIDE!>foo<!>(): Int64 {
        return 2
    }
}

// ── 形态 28：子类重写**非 open** 父类的同名不可变 prop —— 锚 prop 名整段（1 字符）
// kind：sema_cannot_override "cannot override property 'a'"；Range (9,17)..(9,18)，两版一致。
// FILE: cannotOverrideNonOpenProp.cj
open class IgCqBase {
    public prop a: Int64 {
        get() {
            return 1
        }
    }
}
class IgCqSub <: IgCqBase {
    public prop <!CANNOT_OVERRIDE!>a<!>: Int64 {
        get() {
            return 2
        }
    }
}

// ── 形态 29：三层继承，最顶层父类成员非 open —— 锚中间那层子类的成员名
// kind：sema_cannot_override "cannot override function 'foo'"；Range (8,17)..(8,20)，两版一致。
// 官方沿继承链向上找，`IgCdGrandBase::foo` 没有 `open` 即报错，锚点落在**重写者**（IgCdSub）上。
// FILE: cannotOverrideNonOpenDeepInheritance.cj
open class IgCdGrandBase {
    public func foo(): Int64 {
        return 1
    }
}
open class IgCdBase <: IgCdGrandBase {}
class IgCdSub <: IgCdBase {
    public func <!CANNOT_OVERRIDE!>foo<!>(): Int64 {
        return 2
    }
}

// 合法对照段（1.0.5 / 1.1.3 两版均零 error）：
// L1：父类成员带 `open` 时同名重写合法（不写 `override` 也不报 CANNOT_OVERRIDE）——对照形态 27。
// FILE: legalOverrideOpenProp.cj
open class IgCopBase {
    public open prop a: Int64 {
        get() {
            return 1
        }
    }
}
class IgCopSub <: IgCopBase {
    public prop a: Int64 {
        get() {
            return 2
        }
    }
}

// L2：父类成员是 `static` 时同名"重写"不触发 CANNOT_OVERRIDE（static 成员不参与 override 检查，
//     官方 :987 `!parentDecl->TestAttr(Attribute::STATIC)` 直接跳过）。
// FILE: legalStaticFuncSameNameInSubclass.cj
open class IgCsBase {
    public static func foo(): Int64 {
        return 1
    }
}
class IgCsSub <: IgCsBase {
    public static func foo(): Int64 {
        return 2
    }
}

// L3：struct 实现了接口里**只有一个来源**的带体成员（不是同名冲突）——零诊断，对照形态 14。
// FILE: legalSingleInterfaceImplementation.cj
interface IgSiI {
    func foo(): Int64 {
        return 1
    }
}
struct IgSiS <: IgSiI {}

// 已删除的形态（取证 kind 与 Range 见下；1.0.5 与 1.1.3 两版一致，除注明外）：
//   1. `class C <: (Int64, Int64)` / `class C <: (Int64) -> Unit` / `class C <: Int64`
//      → parse_invalid_super_declaration "cannot inherit from type: '(Int64, Int64)'"（DiagnosticParser.def，
//      Range (1,17)..(1,31) / (1,17)..(1,32) / (1,20)..(1,25)，hint "this type cannot be inherited"）——
//      parser 层就拒了，Sema 的 class_inherit_non_class_nor_interface 与 inheritance_non_ref_type 都到不了；
//      因此这两个 kind **必须经类型别名**才能到达（形态 12 / 13）。属 parser 族。探针 igrNonRefTupleClass /
//      igrNonClassTuple / igrNonRefTupleIface / igrNonClassFuncType / igrNonRefPrimitive。
//   2. `extend CfgTarget <: (Int64, Int64)` → sema_extend_not_interface "expected an interface, found
//      non-interface type"（Range (2,22)..(2,36)，CFIR 名 EXTEND_NOT_INTERFACE，已由 extend/ 目录夹具覆盖），
//      仍不到本族两个 kind。探针 igrNonRefExtend。
//   3. `interface IfaceS <: SomeStruct {}` → sema_interface_inherit_non_interface "interface 'IgNsiI' can only
//      inherit interface"（Range (2,1)..(2,2)，`DiagnosticSema.def:254`），本项目无同名 CFIR 名；
//      interface 宿主走的是另一条分支（`sema_interface_cannot_inherit_class` 已有夹具覆盖）。探针 igrNonClassStructInIface。
//   4. `extend` 块里定义与 class 成员同名的函数 → sema_extend_member_cannot_shadow "extend member 'foo' is not
//      allowed to shadow members of …"（Range (7,10)..(7,13)，refactor，CFIR 名 EXTEND_MEMBER_CANNOT_SHADOW，
//      已由 extend/extendIllegalMember.cj 覆盖），不是 CANNOT_OVERRIDE。探针 igrCannotOverrideExtend。
//   5. 成员变量遮蔽（同名 var / 形参）→ sema_member_variable_can_not_shadow（StructInheritanceChecker.cpp:972，
//      Range (5,5)..(5,6)，CFIR 名 MEMBER_VARIABLE_CAN_NOT_SHADOW），属另一个族。探针 igrCannotOverrideVar。
//   6. 同一个类里两个同名同签名成员 → sema_overload_conflicts "function 'foo' has overload conflicts"
//      （`DiagnosticSema.def:97` 与 `DiagRefactor/DiagnosticSema.def:40` 两处同名，Range (5,17)..(5,20)），
//      本项目无同名 CFIR 名 → SUGGESTED。探针 igrCannotOverrideSameClass。
//   7. 父类成员是**泛型**函数 / prop 时的非 open override → 官方在 StructInheritanceChecker.cpp:996-1001
//      改报 `sema_overload_conflicts`（本机非 CJNATIVE 分支；CJNATIVE 分支 :993 才走 DiagCannotOverride），
//      所以 CANNOT_OVERRIDE 只在"父类成员非泛型"时可达。探针 igrOverloadConflictsGeneric（源码写成了
//      `public func <T> foo`，parser 报 parse_expected_name / parse_expected_lt_paren）、
//      igrOverloadConflictsGenericProp（`prop` 后写类型形参，parse_expected_character）、
//      igrOverloadConflictsGeneric2（`public func foo<T>(x: T)` 正确写法，两版零诊断——泛型父类成员
//      + 非泛型子类同名这一组合在本仓库写法下**到不了** overload_conflicts，需自己再探）。
//   8. `struct` 上的 `open` 修饰符 → parse_illegal_modifier_in_scope（Range (1,1)..(1,5)），parser 族。
//      探针 igrCannotOverrideInStruct。
//   9. `MULTIPLE_CLASS_SUPER_TYPES`（官方 `sema_multiple_class_inheritance`）在 v1.0.0 `src/` 全仓无报告点，
//      是死条目；`ILLEGAL_MULTI_INHERITANCE`（`sema_illegal_multi_inheritance`）已由
//      inheritance/multipleClassSupers.cj 覆盖，不在本文件重复。