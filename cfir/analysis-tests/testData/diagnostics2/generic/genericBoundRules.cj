// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 与 1.1.3 两版实测，静态库模式 `cjc <段>.cj -o out.a --output-type=staticlib
// --diagnostic-format json`；每个 `// FILE:` 段单独编译（官方整包编译时一处 parse 错误会屏蔽其它段的语义诊断，
// `HasPrevDiag` 还会吞掉同起点同文案的重复诊断）。本文件所有形态两版 kind、消息、Range 完全一致，不需要
// LANGUAGE_VERSION 拆分。1.1.3 在 CHIR/codegen 阶段可能 LLVM 崩，语义诊断 JSON 仍完整；1.0.5 在无 error 的
// 含 finalizer / const init 单元上 codegen 可能崩（本文件合法对照段不含这两者）。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），本文件标记逐字符对齐官方范围。
//
// 官方实现（external/cangjie_compiler，checkout 在 tag v1.0.0）：
//   - src/Sema/PreCheck.cpp:913-940 ValidRecursiveConstraintCheck（AssumptionSanityCheck 的 rule 1，:1022-1035；
//     入口 CollectAndCheckAssumption :1123-1140 → CheckAssumption :1092-1108 → PreCheck.cpp:1737）
//     :923-930 sema_generic_param_directly_recursive，锚 genericTy->decl->identifier
//            （DiagnoseRefactor(kind, node, Position) → DiagnosticEngine.h:919-926 → MakeRange(pos, pos + 1)）
//     :931-939 sema_generic_param_exist_in_class_irrelevant_upperbound_recursively，同一锚点
//     辅助：IsGenericParamExistInUpperBounds :99-140（只从 upper 的 typeArgs 展开，并沿同宿主声明的类型形参上界递归；
//            裸类型形参没有 typeArgs，不会把别的形参展开出来）；AreUpperBoundsDirectlyRecursive :142-158（回环才 true）
//   - src/Sema/TypeChecker.cpp:1381-1392 CheckConstructor → :1391 sema_forbid_generic_constructor
//     （条件 funcBody->generic && !STATIC && !HasJavaAttr；`diag.Diagnose(fd, kind)` → DiagnosticEngine.h:844-857 → GetBegin()）
//   - src/Sema/TypeChecker.cpp:1659-1679 CheckFinalizer → :1668 sema_forbid_generic_finalizer（同类条件）
//   - src/Sema/TypeChecker.cpp:1696-1698 CheckPrimaryCtorForClassOrStruct 对主构造器执行同一规则（带 `<T>` 的主构造器
//     在 v1.0.0 无法用合法语法写出，见已删除的形态 4）
//   - src/Sema/DeclAttributeChecker.cpp:352-398 CheckGenericFuncDeclAttributes → :368（原生后端）/ :396（默认后端）
//     sema_generic_in_operator_overload，条件只有 funcBody->generic != nullptr && TestAttr(OPERATOR)
//   - src/Sema/Utils.cpp:431-459 CheckValueTypeRecursiveDFS → :447 sema_value_type_recursive（`diag.Diagnose(*root, ...)`，
//     root 是 DFS 入口 struct）；:460-500 CheckValueTypeRecursiveDFSSwitch（struct 沿非 static 字段，enum 沿构造器形参，
//     tuple / VArray 按元素展开）；:260-270 HasOnlyStructTypeInCycle（环内必须全是 STRUCT_DECL）
//   - 文案：include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:98 / :100；
//           include/cangjie/Basic/DiagnosticSema.def:36（sema_value_type_recursive）、:144 / :145 / :146、:261
// 本项目 CFIR 名（CfirDiagnosticsList.kt / CfirErrors.kt 行号；生产者与默认文案）：
//   GENERIC_PARAM_DIRECTLY_RECURSIVE  :1466 / :374，生成者 CfirGenericDeepChecker.kt:33（Direct 分支），
//     文案 CfirErrorsDefaultMessages.kt:963
//   GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY :1460 / :373，生成者 CfirGenericDeepChecker.kt:41
//     （ClassIrrelevant 分支），文案 :962
//   FORBID_GENERIC_CONSTRUCTOR :1669 / :417，生成者 CfirGenericConstructorChecker.kt:58，文案 :1054
//   FORBID_GENERIC_FINALIZER :1650 / :412，生成者 CfirFunctionSemanticsChecker.kt:312（checkGenericFinalizer），
//     文案 :1049
//   GENERIC_IN_OPERATOR_OVERLOAD :1674 / :418，生成者 CfirOperatorDeclarationChecker.kt:70，文案 :1055
//   VALUE_TYPE_RECURSIVE :443 / :125，生成者 CfirValueTypeRecursiveChecker.kt:62，文案 :287
// 按源码可预判的锚点 / 文案差异（只写源码能直接看出的，未跑 CFIR）：
//   - 两条递归上界诊断 CFIR 都用 declaration.source（类型形参的 PSI source 节点），官方用 identifier 首字符；
//     本文件形参名都是单字符 `T` / `K`，两者重合；多字符形参名时 CFIR 会覆盖整名、官方只覆盖首字符。
//   - FORBID_GENERIC_CONSTRUCTOR：CFIR 用 constructorNameDiagnosticSource(includeConstKeyword = false)，即 `init` 标识符，
//     与官方 FuncDecl GetBegin 一致。
//   - FORBID_GENERIC_FINALIZER：CFIR 用 finalizer.tildeSource()（`~`），官方 FuncDecl GetBegin 同样落在 `~` 上。
//   - GENERIC_IN_OPERATOR_OVERLOAD：CFIR 用 functionNameDiagnosticSource()，官方 GetBegin，两者都落在声明首字符。
//   - VALUE_TYPE_RECURSIVE：CFIR 取环内 source.startOffset 最小的声明再 firstCharacterDiagnosticSource()，官方取 DFS
//     入口 struct 的 GetBegin；两条链在本文件的写法下都是源码序最靠前的 struct 首字符。
//   - 文案：FORBID_GENERIC_FINALIZER 的 CFIR 默认文案是 "generic finalizer '{0}' is not allowed"，官方是
//     "generic finalizer '%s' is not supported"；锚点一致但默认文案不同（Golden 套件的 DiagnosticNameMapper.kt:179
//     映射 CFIR FORBID_GENERIC_FINALIZER → sema_forbid_generic_finalizer）。
// 已有覆盖不重复：
//   - MULTIPLE_CLASS_UPPER_BOUNDS（sema_multiple_class_upperbounds）与 UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE
//     （sema_upper_bound_must_be_class_or_interface）已在 constraints/conflictingUpperBounds.cj、
//     constraints/onlyOneClassBoundAllowed.cj、constraints/repeatedBoundTypeParameter.cj、generic/upperBounds.cj 有标记。
//   - CFIR 的 CONFLICTING_UPPER_BOUNDS / REPEATED_BOUND / ONLY_ONE_CLASS_BOUND_ALLOWED 三个名字
//     （golden/DiagnosticNameMapper.kt:172 / :175 / :174 分别映射 sema_conflicting_upper_bounds /
//     sema_repeated_bound / sema_only_one_class_bound_allowed）在官方 v1.0.0 的两个 DiagnosticSema.def 里都没有同名 kind
//     （全仓库 include/ src/ test/ 对这三个名字零命中），本文件不用它们写标记。
//   - 本族诊断在旧套件里已有（diagnostics/constructor/genericShape.cj：class 里 `init<T>(x: T) {}` 与
//     `public operator func +<T>(other: ...)`；llt/class/class_finalizer/class_finalizer3.cj：`~init<T>()`；
//     llt/generics/recursive_generics.cj：class 宿主两参数互递归；llt/generics/generic_constraint/generic_constraint9.cj：
//     `T <: Array<T>`；llt/constraint_check/constraint_check_test12.cj：Array / tuple / 函数类型 / 本地 struct / enum /
//     互递归共 8 种；llt/record/record_recursive.cj、llt/record/primaryConstructor/primaryConstructor8.cj、
//     llt/varray/varray14.cj、llt/record/langfuzz0109.cj、macro/llt/varray/varray_with_reftype/varray_with_reftype05.cj：
//     VALUE_TYPE_RECURSIVE）。旧套件用底部 EXPECTED 注释风格，本文件按 diagnostics2 内联风格重写，并刻意换形态：
//     struct 宿主、extend / interface 里的 operator、字段式值类型递归，以及本族所有合法对照段。

// ── 形态 1：class 的两个类型形参互递归绑定（`U <: K, K <: U`）
// kind：sema_generic_param_directly_recursive "generic parameter 'K' is bounded directly recursively with 'Generics-U'
//      which is forbidden"
// 锚点：先被检查的那个形参 K 的 identifier 首字符（ValidRecursiveConstraintCheck 按 generic.typeParameters 顺序遍历，
//      第一个形参 K 的上界 U 展开后回到 K → 命中）。1.0.5 / 1.1.3 一致。
// FILE: typeParamMutualRecursionInClass.cj
class TypeParamMutualRecursionInClass<<!GENERIC_PARAM_DIRECTLY_RECURSIVE!>K<!>, U> where U <: K, K <: U {
    public init() {}
}

// ── 形态 2：函数类型形参互递归绑定（同一规则的 FUNC_DECL 路径）
// kind / 锚点：同形态 1，锚第一个形参 K 的 identifier 首字符。1.0.5 / 1.1.3 一致。
// FILE: typeParamMutualRecursionInFunction.cj
func typeParamMutualRecursionInFunction<<!GENERIC_PARAM_DIRECTLY_RECURSIVE!>K<!>, U>(k: K, u: U): K where U <: K, K <: U {
    return k
}

// ── 形态 3：struct 的类型形参互递归绑定（CheckAssumption 对 STRUCT_DECL 同样收集 / 检查泛型假设）
// kind / 锚点：同形态 1，锚 K 的 identifier 首字符。1.0.5 / 1.1.3 一致。
// FILE: typeParamMutualRecursionInStruct.cj
struct TypeParamMutualRecursionInStruct<<!GENERIC_PARAM_DIRECTLY_RECURSIVE!>K<!>, U> where U <: K, K <: U {
    public init() {}
}

// ── 形态 4：类型形参出现在"类无关"泛型（struct）的类型实参里：`T <: Box<T>`
// kind：sema_generic_param_exist_in_class_irrelevant_upperbound_recursively "generic parameter 'T' cannot be used in
//      class irrelevant upper bounds 'Struct-IrrelevantUpperBoundBox<Generics-T>'"
// 锚点：T 的 identifier 首字符（PreCheck.cpp:936）。1.0.5 / 1.1.3 一致。
// 官方条件：上界不是 class-like（struct / tuple / 函数类型 / 类型形参…）且沿其 typeArgs 能回到 T。
// 官方 LLT 用的是 `T <: Array<T>`（Array 在官方被当作类无关类型），本段用本地 struct 以便不依赖导入。
// FILE: typeParamInIrrelevantUpperBound.cj
struct IrrelevantUpperBoundBox<V> {
    public init() {}
}

func typeParamInIrrelevantUpperBound<<!GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY!>T<!>>(x: T): T where T <: IrrelevantUpperBoundBox<T> {
    return x
}

// ── 形态 5：间接递归——两个形参经"类无关"泛型互相引用（`T <: Box<U>` 且 `U <: Box<T>`）
// kind：sema_generic_param_exist_in_class_irrelevant_upperbound_recursively，报在先被遍历的 T 上，上界显示
//      'Struct-IrrelevantMutualBox<Generics-U>'（只显示该形参自己的第一条上界）。1.0.5 / 1.1.3 一致。
// IsGenericParamExistInUpperBounds 从 T 的上界 Box<U> 的 typeArgs 进入 U，再沿 U 的上界 Box<T> 的 typeArgs 找到 T。
// FILE: typeParamInIrrelevantUpperBoundMutually.cj
struct IrrelevantMutualBox<V> {
    public init() {}
}

func typeParamInIrrelevantUpperBoundMutually<<!GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY!>T<!>, U>(t: T, u: U): T where T <: IrrelevantMutualBox<U>, U <: IrrelevantMutualBox<T> {
    return t
}

// ── 形态 6：类型形参出现在元组上界里（tuple 类型同样不是 class-like）
// kind：sema_generic_param_exist_in_class_irrelevant_upperbound_recursively，上界显示 'Tuple<Generics-T, Int64>'。
// 1.0.5 / 1.1.3 一致。
// FILE: typeParamInTupleUpperBound.cj
func typeParamInTupleUpperBound<<!GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY!>T<!>>(t: T): T where T <: (T, Int64) {
    return t
}

// ── 形态 7：class 里的主构造器自带类型参数（`init<T>(x: T) {}`）
// kind：sema_forbid_generic_constructor "generic constructor 'init' is not supported"
// 锚点：FuncDecl GetBegin，即 `init` 的首字符 `i`（TypeChecker.cpp:1391）。1.0.5 / 1.1.3 一致。
// FILE: genericConstructorInClass.cj
class GenericConstructorInClass {
    <!FORBID_GENERIC_CONSTRUCTOR!>i<!>nit<T>(x: T) {}
}

// ── 形态 8：struct 里的主构造器自带类型参数（CheckConstructor 对 CLASS_DECL / STRUCT_DECL 同规则）
// kind / 锚点：同形态 7。1.0.5 / 1.1.3 一致。
// FILE: genericConstructorInStruct.cj
struct GenericConstructorInStruct {
    <!FORBID_GENERIC_CONSTRUCTOR!>i<!>nit<T>(x: T) {}
}

// ── 形态 9：泛型类的主构造器自带另一组类型参数（类级类型形参不豁免构造器自身的类型形参）
// kind / 锚点：同形态 7。1.0.5 / 1.1.3 一致。
// FILE: genericConstructorInGenericClass.cj
class GenericConstructorInGenericClass<T> {
    <!FORBID_GENERIC_CONSTRUCTOR!>i<!>nit<U>(x: U) {}
}

// ── 形态 10：class 析构函数自带类型参数
// kind：sema_forbid_generic_finalizer "generic finalizer '~init' is not supported"
// 锚点：FuncDecl GetBegin，即 `~` 首字符（TypeChecker.cpp:1668）。1.0.5 / 1.1.3 一致。
// FILE: genericFinalizerInClass.cj
class GenericFinalizerInClass {
    var counter: Int64 = 0

    <!FORBID_GENERIC_FINALIZER!>~<!>init<T>() {
        this.counter = 1
    }
}

// ── 形态 11：泛型类的析构函数自带类型参数
// kind / 锚点：同形态 10，锚 `~`。1.0.5 / 1.1.3 一致。
// FILE: genericFinalizerInGenericClass.cj
class GenericFinalizerInGenericClass<T> {
    var counter: Int64 = 0

    <!FORBID_GENERIC_FINALIZER!>~<!>init<U>() {
        this.counter = 1
    }
}

// ── 形态 12：class 成员 operator 重载自带类型参数
// kind：sema_generic_in_operator_overload "generic is not allowed in operator overload function"
// 锚点：FuncDecl GetBegin，即声明首字符 `public` 的 `p`（DeclAttributeChecker.cpp:368 / :396）。1.0.5 / 1.1.3 一致。
// FILE: genericOperatorInClass.cj
class GenericOperatorInClass {
    <!GENERIC_IN_OPERATOR_OVERLOAD!>p<!>ublic operator func +<T>(other: GenericOperatorInClass): GenericOperatorInClass {
        this
    }
}

// ── 形态 13：struct 成员 operator 重载自带类型参数（形参类型就是形参 T）
// kind / 锚点：同形态 12，锚声明首字符。1.0.5 / 1.1.3 一致。
// FILE: genericOperatorInStruct.cj
struct GenericOperatorInStruct {
    <!GENERIC_IN_OPERATOR_OVERLOAD!>p<!>ublic operator func +<T>(other: T): T {
        other
    }
}

// ── 形态 14：extend 块里的 operator 重载自带类型参数
// kind / 锚点：同形态 12，锚 extend 内实现成员的声明首字符。1.0.5 / 1.1.3 一致。
// FILE: genericOperatorInExtend.cj
struct GenericOperatorExtendHost {
    var value: Int64 = 0
}

extend GenericOperatorExtendHost {
    <!GENERIC_IN_OPERATOR_OVERLOAD!>p<!>ublic operator func +<T>(other: T): T {
        other
    }
}

// ── 形态 15：接口里的 operator 重载自带类型参数（无函数体的接口成员同样被 DeclAttributeChecker 检查）
// kind / 锚点：同形态 12，锚接口成员声明首字符 `operator` 的 `o`。1.0.5 / 1.1.3 一致。
// FILE: genericOperatorInInterface.cj
interface GenericOperatorInInterface {
    <!GENERIC_IN_OPERATOR_OVERLOAD!>o<!>perator func +<T>(other: T): T
}

// ── 形态 16：struct 字段自引用（值类型递归，环内全是 struct）
// kind：sema_value_type_recursive "value type recursive detected: 'ValueTypeRecursiveSelfField->ValueTypeRecursiveSelfField'"
// 锚点：DFS 入口 struct 声明首字符 `s`（`diag.Diagnose(*root, ...)` → GetBegin()，Utils.cpp:447）。
// 1.0.5 / 1.1.3 一致。附带一条 sema_class_uninitialized_field（锚 `let` 的首字母 `l`，1 字符）—— struct 的 let 字段；该伴随诊断已按官方写 CLASS_UNINITIALIZED_FIELD 标记（锚字段声明首字符 `let` 的首字母，官方 Diagnose 取 VarDecl 的 GetBegin）
// 不带初始化时官方必然报未初始化；改成 `= X()` 又会触发 sema_recursive_constructor_call，见文件末"已删除的形态 7"。
// FILE: valueTypeRecursiveSelfField.cj
<!VALUE_TYPE_RECURSIVE!>s<!>truct ValueTypeRecursiveSelfField {
    <!CLASS_UNINITIALIZED_FIELD!>l<!>et self: ValueTypeRecursiveSelfField
}

// ── 形态 17：两个 struct 的非 static 字段互相引用
// kind：sema_value_type_recursive，上界串
//      'ValueTypeRecursiveMutualFieldA->ValueTypeRecursiveMutualFieldB->ValueTypeRecursiveMutualFieldA'；
// 锚源码序最靠前的 struct 声明首字符。1.0.5 / 1.1.3 一致。两条 sema_class_uninitialized_field 同形态 16，两条均已按官方写 CLASS_UNINITIALIZED_FIELD 标记（锚字段声明首字符 `let` 的首字母，官方 Diagnose 取 VarDecl 的 GetBegin）
// FILE: valueTypeRecursiveMutualFields.cj
<!VALUE_TYPE_RECURSIVE!>s<!>truct ValueTypeRecursiveMutualFieldA {
    <!CLASS_UNINITIALIZED_FIELD!>l<!>et other: ValueTypeRecursiveMutualFieldB
}

struct ValueTypeRecursiveMutualFieldB {
    <!CLASS_UNINITIALIZED_FIELD!>l<!>et back: ValueTypeRecursiveMutualFieldA
}

// ── 形态 18：值类型递归经元组字段（CheckValueTypeRecursiveDFSSwitch 对 tuple / VArray 按元素类型展开）
// kind：sema_value_type_recursive，上界串
//      'ValueTypeRecursiveTupleFieldA->ValueTypeRecursiveTupleFieldB->ValueTypeRecursiveTupleFieldA'；锚 A 的声明首字符。
// 1.0.5 / 1.1.3 一致。
// FILE: valueTypeRecursiveThroughTupleField.cj
<!VALUE_TYPE_RECURSIVE!>s<!>truct ValueTypeRecursiveTupleFieldA {
    <!CLASS_UNINITIALIZED_FIELD!>l<!>et pair: (ValueTypeRecursiveTupleFieldB, Int64)
}

struct ValueTypeRecursiveTupleFieldB {
    <!CLASS_UNINITIALIZED_FIELD!>l<!>et back: ValueTypeRecursiveTupleFieldA
}

// ── 合法对照 1：`T <: Holder<T>` 中 Holder 是 class——"直接递归"检查命中的是类无关类型，类上界不触发任何一条递归诊断。
// 1.0.5 / 1.1.3 零诊断。
// FILE: selfTypeParamInClassUpperBoundOk.cj
func selfTypeParamInClassUpperBoundOk<T>(x: T): T where T <: SelfTypeParamInClassUpperBoundOkHolder<T> {
    return x
}

class SelfTypeParamInClassUpperBoundOkHolder<V> {
    public init() {}
}

// ── 合法对照 2：接口上界零诊断（接口是 class-like，ValidRecursiveConstraintCheck 的两条检查都跳过）。
// 1.0.5 / 1.1.3 零诊断。
// FILE: interfaceUpperBoundOk.cj
interface InterfaceUpperBoundOkInterface {
    // 接口成员隐式 public：显式写 public 时官方 1.0.5 会给 parse_redundant_modifier 警告（generic/upperBounds.cj 同此写法）
    func marker(): Int64
}

func interfaceUpperBoundOk<T>(x: T): T where T <: InterfaceUpperBoundOkInterface {
    return x
}

// ── 合法对照 3：泛型类的构造器用类级类型参数——官方只看构造器自身的 funcBody->generic。
// 1.0.5 / 1.1.3 零诊断（1.0.5 额外给 chir_dce_unused_variable 警告，语义诊断无 error）。
// FILE: classLevelTypeParameterConstructorOk.cj
class ClassLevelTypeParameterConstructorOk<V> {
    init(value: V) {
        let _ = value
    }
}

// ── 合法对照 4：class 里的 static 泛型函数合法（官方只禁构造器 / 析构函数 / operator 重载上的类型参数）。
// 1.0.5 / 1.1.3 零诊断。
// FILE: staticGenericMemberFunctionOk.cj
class StaticGenericMemberFunctionOk {
    public static func make<T>(x: T): T {
        return x
    }
}

// ── 合法对照 5：enum 构造器形参用自身类型零诊断（enum 不是 struct，HasOnlyStructTypeInCycle 要求环内全是 struct）。
// 1.0.5 / 1.1.3 零诊断。
// FILE: enumConstructorWithSelfParameterOk.cj
enum EnumConstructorWithSelfParameterOk {
    | Wrap(EnumConstructorWithSelfParameterOk)
    | Empty(Int64)
}

// ── 合法对照 6：非泛型的 operator 成员函数零诊断。
// 1.0.5 / 1.1.3 零诊断。
// FILE: nonGenericOperatorMemberOk.cj
class NonGenericOperatorMemberOk {
    public operator func +(other: NonGenericOperatorMemberOk): NonGenericOperatorMemberOk {
        other
    }
}

// 已删除的形态（探到但不可达 / 被前置错误屏蔽 / 与本族无关；Range 为官方 MainHint.Range，结束列开区间，两版一致）：
//   1. `func selfBound<T>(x: T) where T <: T` —— 1.0.5 / 1.1.3 **零诊断**。AreUpperBoundsDirectlyRecursive
//      （PreCheck.cpp:142-158）只有在沿上界集合能重新进入同一个 GenericsTy 时才返回 true；若 T 仍在自己的上界集合里
//      就会命中，零诊断说明约束构造后 T 的上界集合不含 T 自身。所以自指形态不可达，CFIR 的
//      GENERIC_PARAM_DIRECTLY_RECURSIVE 对 `T <: T` 应同样零诊断（能回环的只有形态 1-3 的双形参写法）。
//   2. `func bareParamBound<V, K>(v: V, k: K) where K <: V` —— 只有 sema_upper_bound_must_be_class_or_interface
//      "the upper bound 'Generics-V' of generic parameter 'Generics-K' must be class or interface"，
//      锚 K 的 identifier 首字符，1 字符（Range 锚该字符）；**不报** sema_generic_param_exist_in_class_irrelevant_upperbound_recursively。
//      原因：IsGenericParamExistInUpperBounds（PreCheck.cpp:99-140）只从 upper 的 **typeArgs** 开始展开，裸类型形参 V
//      没有 typeArgs（q.push(&upper) 后只遍历 curTy->typeArgs），展开即空，V 的上界里的 K 永远到不了。
//      因此"间接递归"必须让形参出现在某个类无关泛型（struct / tuple / 函数类型）的类型实参里，即形态 5 / 6。
//      同一形态在 diagnostics2/constraints/repeatedBoundTypeParameter.cj 已按 UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE 标记，不重复。
//   3. @Java 互操作家族在本机不可达，且多数在官方 v1.0.0 没有报告点（对 include/ src/ test/ 全仓库 grep 这几个名字，
//      只命中 .def 里的定义行，没有任何 Diagnose 调用）：
//      sema_shadow_cannot_in_type_args（DiagRefactor/DiagnosticSema.def:210）、
//      sema_forbid_generic_nonstatic_method（DiagnosticSema.def:144）、
//      sema_primitive_type_as_generics_arg（DiagRefactor .def:200）、
//      sema_meet_constraint_indirectly（:201）、
//      sema_generic_upper_bounds_must_be_java_in_java（:205）——v1.0.0 前端不产生这五条诊断。
//      CFIR 的 SHADOW_CANNOT_IN_TYPE_ARGS（CfirDiagnosticsList.kt:2267）、PRIMITIVE_TYPE_AS_GENERICS_ARG（:1481）、
//      MEET_CONSTRAINT_INDIRECTLY（:1484）、GENERIC_UPPER_BOUNDS_MUST_BE_JAVA_IN_JAVA（:1487）在官方 v1.0.0 无同名 kind；
//      CFIR 没有 FORBID_GENERIC_NONSTATIC_METHOD 这个名字。
//      sema_generic_static_access（DiagRefactor .def:199）有报告点（src/Sema/TypeCheckExpr/NameReferenceExpr.cpp:688-695：
//      接收者类型是泛型、且**当前函数**带 @Java 属性时，锚整条 MemberAccess `ma`），但 `@Java` 在本机两版 cjc 下
//      都是 sema_undeclared_identifier "undeclared identifier 'Java'"（锚注解名的首字母 `J`，1 字符），
//      进不了该分支；CFIR 的 GENERIC_STATIC_ACCESS（CfirDiagnosticsList.kt:1478）同理无可达形态。
//   4. 真正的"主构造器"形态 `class P { init<T>(x: T) }`（不带花括号体）→ parse_missing_body
//      "body of main constructor is missing"（官方 src/Parse/ParseDecl.cpp:588-635 ParsePrimaryConstructor，
//      :631-635 调 DiagMissingBody("main constructor", …)，位置 lastToken.End()，锚 1 字符；hint "main constructor body here"），
//      语义阶段不再报 sema_forbid_generic_constructor —— TypeChecker.cpp:1697 的主构造器分支在 v1.0.0 无法用合法语法触发。
//      带花括号体的 `init<T>(x: T) {}` 走 ParseConstructor（ParseDecl.cpp:545-583），报 TypeChecker.cpp:1391，
//      就是本文件形态 7-9。
//   5. 索引 operator 带类型参数 `public operator func [](index: Int64)<T>(other: T): T` → 两版都是
//      parse_abstract_func_must_have_return_type "abstract function must have return type"（锚类型形参 `T`，1 字符；
//      operator 名 `[]` 那行还会先报 parse_invalid_overloaded_operator "cannot overload operator 'get'"）。
//      解析器不接受写在形参列表之后的类型参数列表，所以下标 operator 的 sema_generic_in_operator_overload 形态不可达。
//   6. 自由函数 operator `public operator func +<T>(a: T, b: T): T` → parse_illegal_modifier_in_scope
//      "unexpected modifier 'operator' on function declaration in 'top-level' scope"（锚修饰符 `operator` 整段，8 字符，
//      Range (1,8)..(1,16)）。operator 只能作 class / struct / extend / interface 的成员，即形态 12-15。
//   7. enum 之间的值类型递归 `enum E1 { | A(E2) } enum E2 { | B(E1) }` → 零诊断。HasOnlyStructTypeInCycle
//      （Utils.cpp:260-270）要求环内全部是 STRUCT_DECL，enum 只参与 CheckValueTypeRecursiveDFSSwitch 的遍历。
//      合法对照 5 是 enum 自引用的同类形态；CFIR CfirValueTypeRecursiveChecker.kt:60 有同样的 hasOnlyStructDeclarations 判断。
//   8. class 字段互相引用 `class A { var b: B } class B { var a: A }` → 没有 VALUE_TYPE_RECURSIVE（引用语义），
//      只有两条 sema_class_uninitialized_field（锚字段名，1 字符）；class 是引用类型，字段互引用合法。
//   9. 本文件三段 VALUE_TYPE_RECURSIVE 各附带 sema_class_uninitialized_field（锚字段名，1 字符）：
//      struct 的 `let` 字段不带初始化时官方必然报未初始化（无字段初值的 struct `var b: Int64` 同样报），
//      而给字段写 `= X()` 初值又触发 sema_recursive_constructor_call（探针：两个 class 互相 `= X()` →
//      sema_recursive_constructor_call @ `X()` 4 字符），所以在官方 v1.0.0 找不到"保留 VALUE_TYPE_RECURSIVE 且没有伴随诊断"的写法；
//      伴随的 sema_class_uninitialized_field 按官方锚字段声明首字符（`let` 首字母）写进本文件三段（CLASS_UNINITIALIZED_FIELD；initialization 域更完整的形态见 diagnostics/initialization/classUninitializedFieldRich.cj）。
// 附：1.0.5 的 chir_dce_unused_variable 警告在 1.1.3 不再出现（合法对照 3 的 `init(value: V)`），本文件只比较语义诊断 error，不受影响。
