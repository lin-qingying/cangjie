// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 与 1.1.3 两版实测，静态库模式 `cjc <段>.cj -o out.a --output-type=staticlib
// --diagnostic-format json`；每个 `// FILE:` 段单独编译（官方整包编译时一处 parse 错误会屏蔽其它段的语义诊断，
// `HasPrevDiag` 还会吞掉同起点同文案的重复诊断）。本文件所有形态两版 kind、消息、Range 完全一致，不需要
// LANGUAGE_VERSION 拆分。1.1.3 在 CHIR/codegen 阶段可能 LLVM 崩，语义诊断 JSON 仍完整；1.0.5 在无 error 的
// 含 finalizer / const init 单元上 codegen 可能崩（本文件合法对照段不含这两者）。1.0.5 额外给 chir_dce_unused_variable
// 警告（形参 / 局部未使用），1.1.3 没有；本文件只比较语义 error，不受影响。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），本文件标记逐字符对齐官方范围。
//   官方 `diag.Diagnose(node, kind)` 对非 refactor kind 退化为节点 GetBegin 一个字符
//   （include/cangjie/Basic/DiagnosticEngine.h:844-857 → :822-825 → `Diagnose(pos, pos + 1)`），
//   `diag.Diagnose(node, pos, kind)` 锚 pos 一字符（:828-842），refactor kind 用 `MakeRange` 覆盖整节点。
//   本文件三条官方 kind 分属这三类，逐段注释写明锚点。
// 本文件与 generic/genericBoundRules.cj 互补：那份只写**声明期**的上界合法性（递归上界、泛型构造器 / 析构函数 /
// operator、自引用、类无关上界递归），本文件只写**实例化期**：类型实参沿宿主类型展开导致的无限实例化、
// 通过类型形参接收者在上界里查找成员 / 方法、static 限定符的替换一致性。
//
// 官方实现（external/cangjie_compiler，checkout 在 tag v1.0.0）：
//   - 无限实例化：src/Sema/InheritanceChecker/InstantiatedChecker.cpp
//     :288-300 CheckInstDupFuncsInNominalDecls（只遍历包内**顶层名义声明**；函数体 / 局部变量 / 顶层变量不作为入口，
//       但被入口声明的 Walker 递归访问到时同样检查）
//     :203-264 CheckInstDupFuncsRecursively → :234 `WillCauseInfiniteInstantiation(node, *target, instTys)`；
//       `instTys = ref ? ref->instTys : typeManager.GetTypeArgs(*baseTy)`（TypeManager.cpp:1840-1850，非数组取 `ty.typeArgs`），
//       `typeMapping = GenerateTypeMapping(*target, instTys)` 为空即提前返回（内建类型等）
//     :145-201 WillCauseInfiniteInstantiation：:153-157 先查 `HaveCyclicSubstitution`（src/Sema/MultiTypeSubstUtils.cpp:474-489，
//       {X->Y, Y->E<X>} 这类替换闭包）；:172-186 checkRecursion 对每个类型实参查
//       `ty->Contains(genericTy) || substitutedTy->Contains(genericTy)`——被访问节点的类型**自身**就是宿主泛型类型、
//       且它的类型实参里含宿主的类型形参时命中（自嵌套 `A<A<T>>`；`A<B<T>>` 这类互递归命中的是替换后的 substitutedTy）；
//       两处都 `diag.Diagnose(triggerNode, kind)`（:155 / :197）→ 节点 GetBegin 一个字符
//     前提：宿主与被判定的类型必须是同一处源码。`class A<T> { operator func +(o: A<A<T>>) }`、`enum E<T> { | First(E<E<T>>) }`
//       命中；而 `class Holder<T> { let payload: A<A<T>> }` 在字段处 `target` 是 `A<A<T>>` 自身而不是 Holder
//       （GetRealReferenceType :44-58 只对构造函数取 outerDecl），零诊断——见文件末已删除形态 2。
//     入口 StructInheritanceChecker.cpp:287 CheckInstDupFuncsInNominalDecls（继承检查末尾调用）
//     :205-207 一旦报过就 `VisitAction::STOP_NOW`，**每个编译单元只报第一条**
//     :32-42 MarkUpperBoundIgnored：进入 GENERIC 节点时把“含泛型参数的上界”标成已访问，防止沿上界递归
//   - 上界成员查找：src/Sema/TypeCheckExpr/NameReferenceExpr.cpp
//     :905-944 GetMemberAccessExposedTarget（:908 `ma.isExposedAccess = true`；由 src/AST/Create.cpp:503 `isExposedAccess =
//       field.ty && field.ty->IsGeneric()` 或本函数设置）→ :921-945 按目标 kind 分派：
//       :941 CheckUpperBoundTargetsCaseFuncCall（函数）、CheckUpperBoundTargetsCaseOthers（字段 / 属性）
//     :119-131 GetUpperBoundTargetsWithGivenKind（收集上界里的 VAR_DECL / PROP_DECL / FUNC_DECL）
//     :721-744 CheckUpperBoundTargetsCaseFuncCall：按“实例化后的形参类型列表”归并重载（MergeFuncTargetsInUpperBounds :204-243、
//       IsCloserToImpl :133），同名同签名的多个上界只留一个候选
//     :747-783 CheckUpperBoundTargetsCaseOthers：`tempTargets` 为空 → :772
//       `diag.Diagnose(ma, ma.field.Begin(), DiagKind::sema_generic_no_member_match_in_upper_bounds)`（锚成员名 1 个字符）
//     :295-329 DiagMemberAccessNotFound：函数形（`t.f()` 的被调部分）找不到目标时 :314-316 报
//       sema_not_found_from_generic_upper_bounds（refactor，`MakeRange(ma.field)`，锚成员名整段）
//     src/Sema/TypeCheckReference.cpp:449-498 FilterAndCheckTargetsOfNameAccess（对类型形参接收者的 `T.x` 走这条；
//       目标被 EraseIf 掉只剩 static / global / enum 构造器时报 sema_illegal_access_non_static_member，与本族无关）
//   - 上界方法调用：src/Sema/TypeCheckCall.cpp
//     :532-538 IsSearchTargetInMemberAccessUpperBound（`ma->isExposedAccess`）、:2562-2586 GetErrorKindForCall、
//     :2590-2641 DiagnoseForCall（:2615 / :2629 `diag.Diagnose(ce, identifierPos, errorKind, identifier)` → 锚 `ma.field.Begin()`）、
//     :306-323 DiagnoseForMultiMapping、:1099-1150 CheckCandidateConstrains、:1052-1097 CheckAndGetMappingForTypeDecl；
//     src/Sema/TypeCheckUtil.cpp:346-356 IsGenericUpperBoundCall、src/Sema/TypeManager.cpp:533-543 / 600-620
//       GenerateTypeMappingForUpperBounds（上界泛型 → 当前类型实参的替换）
//   - 替换一致性：src/Sema/TypeCheckCall.cpp:1052-1097 CheckAndGetMappingForTypeDecl（:1082-1093 遍历 MultiTypeSubst，
//     同一形参被推成 >1 个类型即失败；:1092 `diag.DiagnoseRefactor(DiagKindRefactor::sema_generic_type_inconsistent, baseExpr,
//     MakeRange(baseExpr.begin, baseExpr.end), typeDecl.ty->String())`，锚**整条 baseExpr**——即 `C.f` 里的 `C`，
//     含类型实参、不含 `.f`；附注 `AddNote(diagMessage)` 列出冲突的替换）
//   - 文案：include/cangjie/Basic/DiagnosticSema.def:143（generic infinite instantiation）、:276（no member match …）、
//     :278（ambiguous method '%s' match …）、:280（no method '%s' match …）、:282-283（cannot be instantiated by …）；
//     include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:68（'%s' is not found for generic type '%s' in its upper bounds）、
//     :89（generic types substitutions are inconsistent for '%s'）、:90-93
// 本项目 CFIR 名（CfirDiagnosticsList.kt 行号 / CfirErrors.kt 行号 / CfirErrorsDefaultMessages.kt 行号；生产者）：
//   GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS  :587 / :156 / :413-417，
//     生产者 coneDiagnosticToCfirDiagnostic.kt:2174-2189 mapGenericUpperBoundAccessDiagnostic（接收者是 ConeTypeParameterType
//     且 `operator == null` 时报；同一 Cone 错误按源码里有没有 `(` 分 method 变体，见该文件 :2160-2172 注释——
//     CFIR 把“名字解析出候选但调用没匹配重载”的情形留给匹配层，本文件的可达形态都会落到这条 member 变体）
//   GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS :592 / :157 / :418-423，CfirNonSuppressibleErrorNames.kt:107 收录；
//     除 CfirErrors.kt / DefaultMessages 外只有 ErrorNodeDiagnosticCollectorComponent.kt:1115 提到名字，
//     本文件写不出可达形态（见文件末已删除形态 1）
//   GENERIC_TYPE_INCONSISTENT :1421 / :364 / :953，生产者 CfirCallResolver.kt:3333-3367 staticQualifierGenericTypeInconsistentError
//     → coneDiagnosticToCfirDiagnostic.kt:2615-2622（锚 `callInfo.explicitReceiver?.source`）
//   GENERIC_INFINITE_INSTANTIATION :1445 / :370 / :959，生产者 CfirGenericInstantiationChecker.kt:266-290
//     （hasCyclicInstantiation 两个分支 + hasExpandingActiveInstantiation）与 :2183-2191 reportInfiniteInstantiation
//     （`source?.firstCharacterDiagnosticSource()`，按 SourceKey 去重）
//   GENERIC_ARGUMENT_NO_MATCH :1426 / :365 / :954、GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT :1435 / :368 / :957、
//     GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS :1448 / :371 / :960、CANNOT_INSTANTIATED_BY_INCOMPLETE_TYPE :1454 / :372 / :961、
//     GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT :947 / :262 / :810-814 —— 本文件按房规不重复（见下“已有覆盖”）
//   官方 sema_not_found_from_generic_upper_bounds、sema_generic_type_inconsistent 在 CfirDiagnosticsList.kt 里没有同名名字；
//     本文件用 Golden 套件 DiagnosticNameMapper.kt:168 / :170 / :171 已建立的映射所对应的 CFIR 名写内联标记
//     （GENERIC_INFINITE_INSTANTIATION / GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS / GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS），
//     该 mapper 只映射这四个名字，GENERIC_TYPE_INCONSISTENT 没有官方名映射。
//   GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS（CfirDiagnosticsList.kt:592）默认文案是
//   “Method ''{0}'' is not found for generic type ''{1}'' in its upper bounds.”，与官方 method 变体文案一致（探针不可达）。
// 按源码可预判的锚点 / 文案差异（只写源码能直接看出的，未跑 CFIR）：
//   - GENERIC_TYPE_INCONSISTENT：CFIR 用 `SourceElementPositioningStrategies.DEFAULT` + receiver.source，官方用 `MakeRange(baseExpr.begin, baseExpr.end)`；
//     本文件各形态的接收者都写成不带类型实参的类名（`P12N1C.f` 里的 `P12N1C`），两者重合。
//   - GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS：CFIR 用 REFERENCED_NAME_BY_QUALIFIED，官方用 `ma.field.Begin()`；
//     本文件成员名都是标识符（`count` / `Inner`），两者重合。
//   - GENERIC_INFINITE_INSTANTIATION：CFIR 用 firstCharacterDiagnosticSource，官方用 triggerNode.GetBegin()；两者都落在类型名的首字符。
//     官方这一族的可达形态受“宿主 = 被判定类型”限制（见文件末已删除形态 2）；本文件三种形态（enum 构造器形参、operator 形参、
//     operator 实参）都满足该前提，CFIR 侧是否有同样的限制未跑，按源码看不出 CFIR 的 Walker 入口差异。
//   - 文案：GENERIC_INFINITE_INSTANTIATION 的 CFIR 默认文案与官方逐字相同；GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS 的
//     CFIR 默认文案 “''{0}'' is not found for generic type ''{1}'' in its upper bounds.” 与官方 sema_not_found_from_generic_upper_bounds
//     逐字相同；GENERIC_TYPE_INCONSISTENT 的 CFIR 默认文案 “generic types substitutions are inconsistent for ''{0}''” 与官方一致，
//     但官方附注（AddNote）列出所有冲突替换，CFIR 的 GENERIC_TYPE_INCONSISTENT 没有 related information。
// 已有覆盖不重复：
//   - GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT / GENERIC_ARGUMENT_NO_MATCH / GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT /
//     GENERIC_CONSTRAINT_NOT_LOOSER 在 testData/llt/type/、llt/generics/、llt/solveTypeArgs/、llt/typealias/、diagnostics2/constraints/
//     等处已有大量内联标记，本文件不写。
//   - GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS 已有 3 处：generic/upperBounds.cj:27（`t.missing()`）、generic-access/
//     upperBoundsMemberAndMethod.cj:17 / :21（`value.id` / `value.reset()`）——都是**顶层函数 + 自由函数 + where 单接口上界**。
//     本文件补：类成员函数、构造器、lambda、嵌套类名作为构造器、同函数两处缺失、上界本身泛型（I<T>）、多层上界形态。
//   - GENERIC_INFINITE_INSTANTIATION 已有：llt/generics/class10_test1.cj / class10_test2.cj（函数形参 `A<A<T>>` / `A<(A<T>, A<T>)>`）、
//     class11_test1.cj（operator 形参 `A<B<T>>`）、generic_infinite_init.cj（`A<(A<T>, A<T>)>`）、generic_infinite_init_02..07.cj
//     （A↔B 互递归、`class C<T> <: A<C<T>>` 与 `& B<C<T>>` 的上界链）、function18_infinite_generic_instantiation.cj
//     （`buildList<Base<T>>` 递归调用）。LLT 里**没有** enum 宿主（generic_infinite_init_04.cj 只是 enum 当 `E<X>` 中间层），
//     本文件只写这一形态（泛型 enum 的构造器形参自嵌套），其余形态与已提交一致，不再重写。
//   - GENERIC_TYPE_INCONSISTENT 已有：llt/class/class_static_member_access_invalid03.cj（继承 `I1<K, K>`，锚 `I2`）与
//     class_static_member_access_invalid04.cj（`extend<F, H> A<F, H>` + `B<K> <: A<K, K>`，锚 `B`）。本文件补：两层继承链、
//     三形参两处冲突、作为实参 / lambda 体 / 泛型函数体内调用三种上下文，以及“形参互换”的合法对照。
//   - GENERIC_PARAM_DIRECTLY_RECURSIVE / GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY /
//     FORBID_GENERIC_CONSTRUCTOR / FORBID_GENERIC_FINALIZER / GENERIC_IN_OPERATOR_OVERLOAD / VALUE_TYPE_RECURSIVE /
//     MULTIPLE_CLASS_UPPER_BOUNDS / UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE 已在 generic/genericBoundRules.cj、
//     generic/upperBounds.cj、constraints/ 下覆盖，本文件不重复。
//   - GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS（sema_generic_instantiation_causes_ambiguous_functions，锚 `instTriggerInfos`
//     的 triggerNode）与 CANNOT_INSTANTIATED_BY_INCOMPLETE_TYPE 在 llt/generics/inst_dup_functions001..010.cj、err_ambiguous.cj、
//     generic_call_static_impl01.cj 已覆盖，不重复。
//   - 本文件各段在测试框架里是同包多文件，顶层名全文件唯一（按段主题加前缀 `GI…`），段内类型名再带段前缀。

// ── 形态 1：类型形参接收者 `T` 访问上界里不存在的属性名（顶层泛型函数 + where 单接口上界；已有
//     upperBoundsMemberAndMethod.cj 的 `value.id` 是**非泛型接收者**形态，这里换成类型形参 + 属性名，锚点为成员名整段 5 字符）
// kind：sema_not_found_from_generic_upper_bounds "'count' is not found for generic type 'T' in its upper bounds"
//      （DiagRefactor/DiagnosticSema.def:68；报告点 NameReferenceExpr.cpp:314-316，`getMemberRange()` = `MakeRange(ma.field)`）
// 锚点：成员名 `count` 整段（5 个字符），不是首字符。1.0.5 / 1.1.3 一致 Range (7,14)..(7,19)。
// 注：非泛型接收者（`func f(t: GIM1Shape)`）走的是 sema_not_member_of（:317-322 "is not a member of interface"），
//     两条 kind 锚点相同但文案不同；对照见 llt/class/class14.cj（官方 LLT 已覆盖该非泛型形态）。
// FILE: memberMissingPropNameInFreeFunc.cj
interface GIM1Shape {
    func area(): Int64
}

func giMemberMissingPropName<T>(t: T): Int64 where T <: GIM1Shape {
    return t.<!GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS!>count<!>
}

// ── 形态 2：同一条诊断出现在**类成员函数**里（where 子句在类上；LLT 的 class14.cj 未覆盖，CFIR 走同一 checker）
// kind / 锚点：同形态 1，锚 `count` 整段。1.0.5 / 1.1.3 一致 Range (8,18)..(8,23)。
// FILE: memberMissingPropNameInClassMethod.cj
interface GIM2Shape {
    func area(): Int64
}

class GIM2Holder<T> where T <: GIM2Shape {
    func measure(t: T): Int64 {
        return t.<!GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS!>count<!>
    }
}

// ── 形态 3：同一条诊断出现在**构造器**里（隐式接收者 `this` 的类型形参；官方 Walk 会走到 FuncBody 的每个节点）
// kind / 锚点：同形态 1，锚 `count` 整段。1.0.5 / 1.1.3 一致 Range (9,20)..(9,25)。
// FILE: memberMissingPropNameInConstructor.cj
interface GIM3Shape {
    func area(): Int64
}

class GIM3Holder<T> where T <: GIM3Shape {
    private var value: Int64 = 0

    init(t: T) {
        this.value = t.<!GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS!>count<!>
    }
}

// ── 形态 4：同一条诊断出现在 **lambda 体**里（lambda 的 FuncBody 同样被 Walk 访问）
// kind / 锚点：同形态 1，锚 `count` 整段。1.0.5 / 1.1.3 一致 Range (7,20)..(7,25)。
// FILE: memberMissingPropNameInLambda.cj
interface GIM4Shape {
    func area(): Int64
}

func giMemberMissingPropNameInLambda<T>(t: T): Int64 where T <: GIM4Shape {
    let g = { => t.<!GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS!>count<!> }
    return g()
}

// ── 形态 5：类型形参接收者把**嵌套类名当构造器**调用（`t.Inner()`，MemberAccess 的 field 是类型名；
//     GetUpperBoundTargetsWithGivenKind 只收 VAR_DECL / PROP_DECL / FUNC_DECL，上界里没有嵌套类型 → tempTargets 空 → 同一条诊断）
// kind：sema_not_found_from_generic_upper_bounds "'Inner' is not found for generic type 'T' in its upper bounds"
// 锚点：`Inner` 整段（5 个字符）。1.0.5 / 1.1.3 一致 Range (11,14)..(11,19)。
// FILE: memberMissingNestedTypeNameInUpperBounds.cj
interface GIM5Shape {
    func area(): Int64
}

class GIM5Inner {
    public init() {}
}

func giMemberMissingNestedTypeName<T>(t: T): GIM5Inner where T <: GIM5Shape {
    return t.<!GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS!>Inner<!>()
}

// ── 形态 6：上界本身是**泛型接口**（`T <: GIM6Box<Int64>`），泛型化不改变查找行为（探针 b28：合法调用零诊断）
// kind / 锚点：同形态 1，锚 `count` 整段。1.0.5 / 1.1.3 一致 Range (7,14)..(7,19)。
// FILE: memberMissingPropNameOnGenericUpperBound.cj
interface GIM6Box<V> {
    func get(): V
}

func giMemberMissingPropNameOnGenericBound<V>(t: V): Int64 where V <: GIM6Box<Int64> {
    return t.<!GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS!>count<!>
}

// ── 形态 7：同一函数里两处缺失（官方对每个 MemberAccess 各报一条；HasPrevDiag 只吞“同起点同文案”的重复）
// kind：两条 sema_not_found_from_generic_upper_bounds。1.0.5 / 1.1.3 一致：Range (7,15)..(7,20) 与 (8,15)..(8,19)。
// FILE: memberMissingTwoNames.cj
interface GIM7Shape {
    func area(): Int64
}

func giMemberMissingTwoNames<T>(t: T): Int64 where T <: GIM7Shape {
    let a = t.<!GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS!>count<!>
    let b = t.<!GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS!>name<!>
    return a + b
}

// ── 形态 8：多层上界——`T <: GIM8Box<T> & GIM8Marker`（**where 形参自己的类型形参**作上界、两个上界分别泛型化与非泛型化），
//     `count` 不在任何一个上界里；GetMemberAccessExposedTarget 遍历 `genericsTy.upperBounds` 全部上界（:921-938）后
//     tempTargets 仍为空 → 同一条诊断。这是任务要求的“嵌套 / 继承多层上界”形态：泛型上界的类型形参就是被查找的形参本身，
//     等价于"上界里再套一层泛型实例化"，官方实测正常（探针 a01 / b01 的合法对照），缺失时报告点不变。
// kind / 锚点：同形态 1，锚 `count` 整段。1.0.5 / 1.1.3 一致 Range (12,26)..(12,31)。
// FILE: memberMissingPropNameAcrossTwoUpperBounds.cj
interface GIM8Box<V> {
    func get(): V
}

interface GIM8Marker {
    func tag(): Int64
}

func giMemberMissingPropNameAcrossTwoBounds<T>(t: T): Int64 where T <: GIM8Box<T> & GIM8Marker {
    return t.<!GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS!>count<!>
}

// ── 合法对照 L1：类型形参 `T` 的两个接口上界（其一泛型化），成员分别存在（`t.f(1)`、`t.g()`）：0 error。
//     CheckUpperBoundTargetsCaseFuncCall 归并后各留一个候选；`where T <: I1<T> & I2` 是合法单类上界集（两个类上界无子类型关系
//     才报 MULTIPLE_CLASS_UPPER_BOUNDS，见 genericBoundRules.cj）。探针 m35 / b35 / P12M05。1.0.5 / 1.1.3 零诊断。
// FILE: legalMembersPresentInTwoUpperBounds.cj
interface GIL1First<V> {
    func f(a: V): V
}

interface GIL1Second {
    func g(): Int64
}

func giLegalMembersPresentInTwoBounds<T>(t: T): Int64 where T <: GIL1First<T> & GIL1Second {
    let _ = t.f(t)
    return t.g()
}

// ── 合法对照 L2：上界成员作为**函数值**引用（`t.f` 不带调用）：0 error。
//     FilterAndCheckTargetsOfNameAccess :483-494 的 referenceNeedTypeInfer 分支走 FilterTargetsForFuncReference，
//     候选存在即通过；探针 a11 / P12M07。1.0.5 / 1.1.3 零诊断。
// FILE: legalUpperBoundMemberAsFunctionValue.cj
interface GIL2Shape {
    func f(a: Int64): Int64
}

func giLegalUpperBoundMemberAsValue<T>(t: T): (Int64) -> Int64 where T <: GIL2Shape {
    return t.f
}

// ── 合法对照 L3：上界是泛型接口 `T <: GIL3Box<T>`，成员 `t.get()` 用类型实参自身 `t` 调用：0 error。
//     GenerateTypeMappingForUpperBounds（TypeManager.cpp:533-543）把 `V -> T` 代入上界成员签名后正常匹配；探针 a01 / b28。
//     1.0.5 / 1.1.3 零诊断。
// FILE: legalGenericUpperBoundSelfCall.cj
interface GIL3Box<V> {
    func get(): V
    func put(v: V): Unit
}

func giLegalGenericUpperBoundSelfCall<T>(t: T): T where T <: GIL3Box<T> {
    t.put(t)
    return t.get()
}

// ── 形态 9：泛型 enum 自身的构造器形参 `E<E<T>>`（`enum GII1E<T> { | First(GII1E<GII1E<T>>) | Second(Int64) }`）。
//     LLT 现有 10 个用例全是 class 宿主；enum 是第一个可写出自嵌套的**声明宿主**形态：`CheckInstDupFuncsInNominalDecls`
//     （InstantiatedChecker.cpp:288-300）遍历包内顶层名义声明，enum 声明在列表内，Walker 访问构造器形参类型节点
//     `GII1E<GII1E<T>>` 时 typeArgs = [GII1E<T>] 直接含 genericTy T → 报。宿主不必有子类（对比已删除形态 2）。
//     探针 y08（1.0.5 / 1.1.3 一致，Range (3,13)..(3,14)）。
// kind：sema_generic_infinite_instantiation "generic infinite instantiation"（DiagnosticSema.def:143；
//      报告点 InstantiatedChecker.cpp:197 `diag.Diagnose(triggerNode, kind)` → triggerNode.GetBegin() 一个字符）
// 锚点：类型名首字符 `G`。1.0.5 / 1.1.3 一致 Range (3,13)..(3,14)。
// FILE: infiniteInstantiationInEnumConstructorParam.cj
enum GII1E<T> {
    | First(<!GENERIC_INFINITE_INSTANTIATION!>G<!>II1E<GII1E<T>>)
    | Second(Int64)
}

// ── 形态 10：class 成员 `operator func` 的形参类型 `A<A<T>>`——宿主 A 自身就是被实例化的类型，替换映射 `T -> A<A<T>>`
//     非空，`checkRecursion` 直接命中。LLT 的 class11_test1 是 `class A<T> { operator func [](b: A<B<T>>) }`（A↔B 互递归、
//     宿主与被嵌套类型不同），本段是自嵌套、且 operator 名为 `+`；两版实测锚点都在**形参类型** `GII2A<GII2A<T>>` 的首字符。
// 锚点：类型名首字符 `G`。1.0.5 / 1.1.3 一致 Range (3,33)..(3,34)。
// FILE: infiniteInstantiationInOperatorParamType.cj
class GII2A<T> {
    public operator func +(other: <!GENERIC_INFINITE_INSTANTIATION!>G<!>II2A<GII2A<T>>): GII2A<T> {
        return this
    }
}

// ── 形态 11：operator 重载的**实参**构造调用 `a + A<A<T>>()`（`instTys = [A<A<T>>]`，同样直接命中 checkRecursion；
//     LLT 无此形态。与 class11_test1 的区别是实参而非形参，且宿主 A 声明在 operator 之后。）
// 锚点：形参类型节点首字符 `G`（Range (2,35)..(2,36)）——与形态 10 相同：只要 operator 的形参类型是 A<A<T>>
//     且宿主 A 就是被判定类型，官方在形参处就报（checkRecursion 的 STOP_NOW），实参位置不报；本段 operator 的
//     声明与形态 10 同形，所以形参处同样带标记。1.0.5 / 1.1.3 一致。
// FILE: infiniteInstantiationInOperatorArgument.cj
class GII3A<T> {
    public operator func +(other: <!GENERIC_INFINITE_INSTANTIATION!>G<!>II3A<GII3A<T>>): GII3A<T> {
        return this
    }
}

func giInfiniteInstantiationInOperatorArgument<T>(a: GII3A<T>): GII3A<T> {
    return a + GII3A<GII3A<T>>()
}

// ── 合法对照 L4：enum 构造器形参是**具体类型实参** `A<A<Int64>>`（Int64 不是泛型，替换映射为空 → `GenerateTypeMapping`
//     为空即提前返回）→ 0 error。探针 j16 / s05 / x12。1.0.5 / 1.1.3 零诊断。
// FILE: legalNestedTypeArgsInEnumConstructorParam.cj
class GIL4A<T> {
    public init() {}
}

enum GIL4E {
    | First(GIL4A<GIL4A<Int64>>)
    | Second(Int64)
}

// ── 合法对照 L5：class 字段类型是**单层**泛型 `A<T>` → 0 error。`instTys = [T]`，`typeArgs` 为空，
//     `HaveCyclicSubstitution({T -> T})` 为 false，`Contains(genericTy)` 不成立；探针 j05 / j24 / P11K19 / x13。
//     1.0.5 / 1.1.3 零诊断。
// FILE: legalSingleNestingInClassField.cj
class GIL5A<T> {
    public init() {}
}

class GIL5Holder<T> {
    let payload: GIL5A<T>
    init() {
        this.payload = GIL5A<T>()
    }
}

// ── 合法对照 L6：两个不同宿主类型互递归的字段（`A<T> { let b: Option<B<T>> }` / `B<T> { let a: Option<A<T>> }`）：
//     自嵌套与互递归的**区别**只在同一类型出现在自己的 typeArgs 里，这里两个宿主不同 → 0 error。
//     探针 i16（裸可空写法 `A<T>?` 在两版都是 parse 错误，改用 Option 后官方零诊断）。1.0.5 / 1.1.3 零诊断。
// FILE: legalMutualNestingInClassFields.cj
class GIL6B<T> {
    let a: Option<GIL6A<T>>
    public init() {
        this.a = None
    }
}

class GIL6A<T> {
    let b: Option<GIL6B<T>>
    public init() {
        this.b = None
    }
}

// ── 合法对照 L7：自由函数形参 / 返回类型 / 局部变量 / 泛型类的类型实参里都放 `A<A<T>>` → 0 error（官方设计）：
//     `CheckInstDupFuncsInNominalDecls` 的入口是**顶层名义声明**，函数体 / 局部变量 / 顶层变量不作为入口；
//     形如 `class Holder { let p: A<A<Int64>> }` 的具体实参写法同样零诊断（替换映射为空）。
//     探针 k01 / k03 / k11 / j07 / j27 / i01 / s01 / y01 / y04 / y05 / y12。1.0.5 / 1.1.3 零诊断。
//     注意：GIL7Holder 的字段是具体类型 GIL7A<GIL7A<Int64>> / GIL7A<GIL7A<Bool>>，实参必须写具体类型；
//     传形参 a（GIL7A<GIL7A<T>>）官方报 sema_mismatched_types（锚第一个实参，Range (15,29)..(15,30)），不是合法形态。
// FILE: legalNestedTypeArgsInFunctionSignature.cj
class GIL7A<T> {
    public init() {}
}

class GIL7Holder {
    let p: GIL7A<GIL7A<Int64>>
    let q: GIL7A<GIL7A<Bool>>
    init(p: GIL7A<GIL7A<Int64>>, q: GIL7A<GIL7A<Bool>>) {
        this.p = p
        this.q = q
    }
}

func giLegalNestedTypeArgsInSignature<T>(a: GIL7A<GIL7A<T>>): GIL7A<GIL7A<T>> {
    let holder = GIL7Holder(GIL7A<GIL7A<Int64>>(), GIL7A<GIL7A<Bool>>())
    let g = { => GIL7A<GIL7A<T>>() }
    let _ = a
    let _ = holder
    return g()
}

// ── 形态 14：两层继承链上的 static 成员（`Mid<X> <: Base<X, X>`、`C<K> <: Mid<K>`）调用时同一形参 K 被推成两种类型
// kind：sema_generic_type_inconsistent "generic types substitutions are inconsistent for 'Class-GIT1C<Generics-K>'"
//      + 附注 "'Generics-K' to 'Int64', 'Bool'"（DiagRefactor/DiagnosticSema.def:89；
//      报告点 TypeCheckCall.cpp:1082-1093，`diag.DiagnoseRefactor(kind, baseExpr, MakeRange(baseExpr.begin, baseExpr.end), ...)`）
// 锚点：整条 baseExpr，即 `GIT1C.f` 里的 `GIT1C`（6 字符，不含 `.f`）。1.0.5 / 1.1.3 一致 Range (13,12)..(13,18)。
// FILE: typeInconsistentThroughTwoLevelInheritance.cj
open class GIT1Base<F, H> {
    public static func f(a: F, b: H): Int64 {
        return 0
    }
}

open class GIT1Mid<X> <: GIT1Base<X, X> {}

open class GIT1C<K> <: GIT1Mid<K> {}

func giTypeInconsistentThroughTwoLevelInheritance(): Int64 {
    return <!GENERIC_TYPE_INCONSISTENT!>GIT1C<!>.f(1, true)
}

// ── 形态 15：三形参父类中两处映射到同一个形参 `Base<K, L, K>`，调用 `f(1, true, true)`：只有 K 冲突（第二条替换 `K -> Bool`），
//     附注 "'Generics-K' to 'Int64', 'Bool'"；锚点整条 baseExpr。1.0.5 / 1.1.3 一致 Range (11,12)..(11,18)。
// FILE: typeInconsistentWithThreeMappings.cj
open class GIT2Base<F, G, H> {
    public static func f(a: F, b: G, c: H): Int64 {
        return 0
    }
}

class GIT2C<K, L> <: GIT2Base<K, L, K> {}

func giTypeInconsistentWithThreeMappings(): Int64 {
    return <!GENERIC_TYPE_INCONSISTENT!>GIT2C<!>.f(1, true, true)
}

// ── 形态 16：static 调用作为**实参**（`take(GIT3C.f(1, true))`）：锚点仍是 baseExpr 而不是整条实参表达式
// kind / 锚点：同形态 14。1.0.5 / 1.1.3 一致 Range (15,22)..(15,28)。
// FILE: typeInconsistentCallAsArgument.cj
open class GIT3Base<F, H> {
    public static func f(a: F, b: H): Int64 {
        return 0
    }
}

class GIT3C<K> <: GIT3Base<K, K> {}

func giTypeInconsistentTake(g: Int64): Int64 {
    return g
}

func giTypeInconsistentCallAsArgument(): Int64 {
    return giTypeInconsistentTake(<!GENERIC_TYPE_INCONSISTENT!>GIT3C<!>.f(1, true))
}

// ── 形态 17：static 调用在 **lambda 体**里（`{ => GIT4C.f(1, true) }()`）
// kind / 锚点：同形态 14，锚 `GIT4C`。1.0.5 / 1.1.3 一致 Range (11,18)..(11,24)。
// FILE: typeInconsistentInsideLambda.cj
open class GIT4Base<F, H> {
    public static func f(a: F, b: H): Int64 {
        return 0
    }
}

class GIT4C<K> <: GIT4Base<K, K> {}

func giTypeInconsistentInsideLambda(): Int64 {
    let g = { => <!GENERIC_TYPE_INCONSISTENT!>GIT4C<!>.f(1, true) }
    return g()
}

// ── 形态 18：static 调用在**泛型函数体**里（外层函数带无关的类型形参，替换不一致检查只看 static 限定符的宿主类型形参）
// kind / 锚点：同形态 14，锚 `GIT5C`。1.0.5 / 1.1.3 一致 Range (11,12)..(11,18)。
// FILE: typeInconsistentInsideGenericFunction.cj
open class GIT5Base<F, H> {
    public static func f(a: F, b: H): Int64 {
        return 0
    }
}

class GIT5C<K> <: GIT5Base<K, K> {}

func giTypeInconsistentInsideGenericFunction<X>(x: X): Int64 {
    return <!GENERIC_TYPE_INCONSISTENT!>GIT5C<!>.f(1, true)
}

// ── 合法对照 L8：同样的 `GIT6Base<K, L, K>`，实参按替换顺序给出 `(1, true, 1)`（F=K→Int64，G=L→Bool，H=K→Int64 一致）：0 error。
//     探针 c03 / P12N03。1.0.5 / 1.1.3 零诊断。
// FILE: legalTypeConsistentWithThreeMappings.cj
open class GIL8Base<F, G, H> {
    public static func f(a: F, b: G, c: H): Int64 {
        return 0
    }
}

class GIL8C<K, L> <: GIL8Base<K, L, K> {}

func giLegalTypeConsistentWithThreeMappings(): Int64 {
    return GIL8C.f(1, true, 1)
}

// ── 合法对照 L9：形参互换 `GIL9Base<L, K>`，调用 `(true, 1)`：每个形参只被推成一个类型：0 error。
//     探针 c07 / P12N05。1.0.5 / 1.1.3 零诊断。
// FILE: legalTypeConsistentSwappedMappings.cj
open class GIL9Base<F, G> {
    public static func f(a: F, b: G): Int64 {
        return 0
    }
}

class GIL9C<K, L> <: GIL9Base<L, K> {}

func giLegalTypeConsistentSwappedMappings(): Int64 {
    return GIL9C.f(true, 1)
}

// ── 合法对照 L10：同一类型形参在实参里重复出现且类型一致 `GIL10C.f(1, 1)`：0 error（与 class_static_member_access_invalid03 的对照）。
//     探针 a24。1.0.5 / 1.1.3 零诊断（1.0.5 额外给 chir_dce_unused_variable 警告，形参 a / b 未使用）。
// FILE: legalTypeConsistentRepeatedParameter.cj
open class GIL10Base<F, H> {
    public static func f(a: F, b: H): Int64 {
        return 0
    }
}

class GIL10C<K> <: GIL10Base<K, K> {}

func giLegalTypeConsistentRepeatedParameter(): Int64 {
    return GIL10C.f(1, 1)
}


// 已删除的形态（1.0.5 / 1.1.3 两版取证一致，除注明外；Range 为官方 MainHint.Range，结束列开区间）：
//   1. `t.f()` / `t.f(x)` 在类型形参接收者上**不匹配上界成员签名** —— 写不出 sema_generic_no_method_match_in_upper_bounds
//      （DiagnosticSema.def:280）与 sema_generic_ambiguous_method_match_in_upper_bounds（:278），CFIR 的
//      GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS 没有可达形态。
//      源码依据：这两条 kind 只在 `GetErrorKindForCall`（TypeCheckCall.cpp:2562-2586）里、且
//      `IsSearchTargetInMemberAccessUpperBound(ce)` 为真（`ma->isExposedAccess`，:532-538）时返回，
//      而该函数唯一的调用点是 `DiagnoseForCall`（:2590-2641）；官方对 include/ + src/ 全仓库 grep 这两个名字，
//      除 .def 定义行、`GetErrorKindForCall` 的两个 return、`DiagnoseForMultiMapping`（:319）外没有任何报告点。
//      三条可达路径都被更早的诊断挡住（探针 m01 / m02 / m08 / m11 / m20）：
//        (a) 名字在上界里找不到 → 解析阶段 `CheckUpperBoundTargetsCaseFuncCall`（NameReferenceExpr.cpp:721-744）返回 nullptr
//            → `DiagMemberAccessNotFound`（:295-329）报 sema_not_found_from_generic_upper_bounds（→ CFIR member 变体）；
//        (b) 名字存在但实参个数不对 → `MatchFunctionForCall` 内部已报 sema_wrong_number_of_arguments（锚 `()` / `(1, 2)`），
//            `DiagnoseForCall` 只在 `diagnostics.empty()` 时被调用（:2966-2968 / :3070-3072）；
//        (c) 候选唯一但实参类型不满足 → `DiagnoseForCall` 在 `candidatesBeforeCheck.size() == 1` 时直接 return（:2620-2622），
//            类型不匹配由 CheckWithCache / sema_mismatched_types / sema_cannot_convert_literal 报；
//        (d) 两个上界有同名方法 → `MergeFuncTargetsInUpperBounds`（:204-243）按实例化后的形参列表去重，
//            不同签名保留为多个候选、按实参挑选；同名同签名只留一个候选（探针 m07 零诊断）。
//        `DiagnoseForMultiMapping`（:306-323）的 ambiguous 分支要求 `resMappings.size() > typeMappings.size()`（同一候选多解），
//        需要模糊类型实参 + 函数引用（`let g = t.f`），探针 a11 / P12M07 官方零诊断（走 FilterTargetsForFuncReference，
//        不经过 MatchFunctionForCall）。
//      探针清单（1.0.5 / 1.1.3 结果一致）：m01 `t.f()` 少参、m02 `t.f(Bool)` 错参、m03/m06 两个上界不同元数、
//      m04/m05/m08/m09/m10 两个上界同元数不同签名、m11 class 上界少参、m20 三个上界、a01/b01/b09/b10/b11/b12/b13/b14
//      泛型上界歧义、b02 上界类型形参是别的形参（报 sema_mismatched_types）、b29 同形态正常调用。
//   2. `A<A<T>>` 出现在**class / struct / interface 的字段类型、形参类型，或自由函数 / 局部变量 / lambda / 顶层变量 /
//      typealias** 上：官方零诊断。原因是 WillCauseInfiniteInstantiation 的 `checkRecursion` 要求被访问节点 `target`
//      **就是宿主泛型声明**（`ty->Contains(genericTy)`），而字段类型节点 `A<A<T>>` 的 target 是 `A` 本身而不是声明它的
//      Holder / S / I——`GetRealReferenceType`（:44-58）只对构造函数取 outerDecl。逐条结论：
//        (a) `class Holder<T> { let payload: A<A<T>>; init(payload: A<A<T>>) { … } }` → 零诊断（探针 y01 / y05）。
//        (b) `open class Holder<T>` / `abstract class Holder<T>` / `struct S<T>` 的同类字段 → 同样零诊断
//            （探针 y04 / y06 / x01 / x03 / x06 / x10 / x13；open / abstract 不改变结论）。
//        (c) `var payload: A<A<T>> = A<A<T>>()`（字段带初值）、`let payload: A<A<T<A<T>>>`（三层）→ 零诊断
//            （探针 x07 / x13 / y07 / y12）。
//        (d) 自由函数形参 / 局部变量 / lambda 形参 / 顶层变量 / typealias → 零诊断（探针 k01 / j07 / i01 / s01 / x09）：
//            它们不在 CheckInstDupFuncsInNominalDecls 的入口集合内。
//        (e) `class A<K> { func f<U>(a: A<A<K>>, u: U) }`（泛型成员函数的形参）→ 零诊断（探针 k11）。
//        (f) `class Holder<T> { init(payload: A<A<T>>) { this.payload = … } }`（构造器形参、宿主 ≠ A）→ 零诊断；
//            把 `this.payload` 赋值给同类型字段会额外报 sema_mismatched_types，两条都不写（探针 x04 / x05 / x11 / y02 / y03）。
//        (g) `enum E { | First(A<A<Int64>>) }`（enum 宿主 + 具体实参）→ 零诊断（探针 j16 / s05 / x12 / y12 变体，合法对照 L4）；
//            `enum E<T> { | First(A<A<T>>) }`（enum 宿主 + T）同样零诊断——target 是 A 而不是 E（探针 y09 / x06）。
//        (h) `interface I { func f(): A<A<Int64>> { … } }`（接口方法默认体）→ 零诊断（探针 x08 / i19 / l05）。
//      官方源码的 `visitedByWalkerID` 标记（MarkUpperBoundIgnored :32-42）解释了为什么**上界**里的自嵌套不会触发。
//   3. `class C<T> <: A<A<T>>`（上界自嵌套）→ 零诊断：上界在 PreCheck 阶段绑定并被 MarkUpperBoundIgnored 标记；
//      generic_infinite_init_05..07 的上界链（`C<T> <: A<C<T>>`）能报是因为 C 本身是**被嵌套的类型实参**而非上界。
//      官方接口不能继承非接口（`interface I<T> <: A<A<T>>` → sema_interface_inherit_non_interface），class 上界写法无诊断。
//   4. `T.f()`（类型形参**作为 static 限定符**的成员调用，`IsSearchTargetInMemberAccessUpperBound` 依赖 MemberAccess
//      而 `T.f` 是 REF_EXPR）+ 上界里有 static 成员 → 报 sema_cannot_convert_literal（探针 b07），不走本族 kind。
//   5. `let g = t.f`（函数引用）后 `g(1, 2)` → 报 sema_wrong_number_of_arguments（候选非空），
//      `t.count = 1`（赋值）→ sema_not_found_from_generic_upper_bounds + sema_cannot_assign_to_immutable（探针 j41/j42/j43）。
//   6. `P9C4Base<K, L, M>` 直接 `P9C4C.f(1, true, "x")`（K→Int64、L→Bool、M→String，每个形参一个替换）→ 零诊断；
//      `P9C3Base<K, K, L>` 时 `f(1, true, 1)` 报 K→Int64, Bool（两处 K 冲突），附注只列冲突形参（探针 c03/c04/c08）。
//   7. `type P9C26Alias = P9C26C`（泛型类型别名不带类型实参）→ sema_generic_type_without_type_argument（锚别名名首字母），
//      属 GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT 族，已在 llt/typealias/ 有覆盖（探针 a26）。
//   8. `extend<F, H> A<F, H> { static func f }` + `B<K> <: A<K, K>` 中 A 不是 open → sema_non_inheritable_super_class，
//      但替换不一致仍会报（class_static_member_access_invalid04.cj 已覆盖该形态）。
//   9. `GIL10Base.f(1, true)`（直接在泛型声明自身上调用 static）→ sema_unable_to_infer_generic_func（锚被调名 `GIL10Base.f`），
//      不是替换不一致：`needCheckTypeInconsistency` 要求 `typeDecl != structDecl`（TypeCheckCall.cpp:1127），
//      同声明时直接走 `CheckGenericDeclInstantiation`；该 kind 属推断族、在 41 个 testData 文件里已有标记，不在本文件（探针 a27）。
//  10. `P10J8A<P10J8A<T>>` 作为 VArray 元素类型 `VArray<P10J8A<P10J8A<T>>, $3>` → sema_varray_arg_type_with_reftype
//      （"directly or indirectly contains unsupported instance member variable"，属 VArray 族），不报本族诊断（探针 j19）。
// 附：官方 kind 无 CFIR 对应名字的两个——sema_not_found_from_generic_upper_bounds（CFIR 用 GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS，
//      golden/DiagnosticNameMapper.kt:170 已建立映射关系但 CfirDiagnosticsList.kt 没有该官方名）与 sema_generic_type_inconsistent
//      （CFIR 用 GENERIC_TYPE_INCONSISTENT，CfirDiagnosticsList.kt:1421 / CfirErrors.kt:364；mapper 未收录），本文件按 CFIR 名写标记；
//      sema_generic_instantiation_causes_ambiguous_functions / sema_cannot_instantiated_by_incomplete_type
//      在官方 DiagRefactor .def:92-93 / DiagnosticSema.def:282-283，LLT 已覆盖，不在本文件。
