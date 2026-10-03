// RUN_PIPELINE_TILL: FRONTEND
// 静态上下文访问规则（static context access rules）语义夹具：静态函数 / static init / 静态变量初始化器 /
// 静态 lambda / 静态属性访问器里访问实例成员，以及类型名访问实例成员、对象访问静态成员。
//
// 取证方式：官方 cjc 1.0.5（准）与 1.1.3 两版实测，静态库模式
//   `cjc <段>.cj -o out.a --output-type=staticlib --diagnostic-format json`；每个 `// FILE:` 段单独编译
//   （官方整包编译时一处 parse 错误会吞掉其余段的语义诊断；HasPrevDiag 只吞同起点同文案的重复诊断）。
//   两版 cjc 在本文件全部形态上的 kind、消息、MainHint.Range 逐段一致，**无版本分歧**，故不需要
//   LANGUAGE_VERSION 门禁段。合法对照段（含静态成员的类型）在 1.1.3 上语义层通过后于 CHIR/codegen 阶段
//   LLVM ERROR（Reflection format: modifier type is invalid / EnumReflection format: operand numbers error），
//   JSON 不产出；cjc 在语义有错时不会进入 codegen，故该崩溃本身证明这些段前端零语义错误，1.0.5 侧实测为
//   JSON 零诊断。范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），
//   本文件标记逐字符对齐官方范围；正文里的 (行,列) 均为段内行号（`// FILE:` 行被剥离后的干净文本坐标）。
//
// 官方实现（external/cangjie_compiler，工作树 checkout 在 tag v1.0.0）：
//   1) this / super 出现在静态上下文 → src/Sema/TypeCheckReference.cpp:150 CheckUsageOfThis，:159
//      `diag.Diagnose(re, sema_static_members_cannot_call_members, re.ref.identifier.Val())`；super 同形在
//      :211 CheckUsageOfSuper / :219。调用点 :764 与 :766（CheckLegalityOfReference 的 preVisit 对每个
//      RefExpr 三选一：isThis / isSuper / CheckAccessLegalityOfRefExpr）。条件只有两条：所在 nominal decl
//      存在（class / struct / interface / enum / extend，`GetCurInheritableDecl`）且当前函数体
//      `GetCurFuncBody(ctx, re.scopeName)` 带 `Attribute::STATIC`（src/Sema/Utils.cpp:325 按
//      SymbolKind::FUNC_LIKE 取 FUNC_DECL / LAMBDA_EXPR / MACRO_DECL / PRIMARY_CTOR_DECL 的 funcBody；
//      静态属性访问器与静态变量初值里的 lambda 由 src/Sema/DeclAttributeChecker.cpp:116
//      InheritMemberAttrRecursively 递归给 FUNC_BODY 打 STATIC，同类注释见 src/Sema/TypeChecker.cpp:2537/:2547）。
//      锚点：`Diagnose(node, kind)`（include/cangjie/Basic/DiagnosticEngine.h:844-857）退化为 GetBegin 一字符，
//      即 `this` / `super` 关键字首字母。当被访问成员是静态成员时本条不出现，只出现第 4) 条
//      object cannot access static member（同样锚在 `this` 首字符），两版实测见形态 5 / 14 / 16。
//   2) 静态函数 / 静态 lambda 里裸名访问实例成员 → src/Sema/TypeCheckExpr.cpp:52 IsLegalAccessFromStaticFunc：
//      curFuncBody 带 STATIC、目标 decl 不带 STATIC、目标不是 class/enum 构造器、
//      `FieldLookup` / `ExtendFieldLookup` 在**当前 nominal decl**（SymbolKind::STRUCT =
//      CLASS/INTERFACE/STRUCT/ENUM/EXTEND，见 src/Sema/ScopeManager.h:32）按名字查到的结果与目标 decl
//      指针相同，才报；调用点 src/Sema/TypeCheckReference.cpp:618（CheckAccessLegalityOfRefExpr，
//      **仅当 re.ref.targets.size() <= 1**）与 src/Sema/TypeCheckCall.cpp:3106。:73-87 diagForInvalidAccess：
//      `curFuncBody->funcDecl` 非空 → :83 sema_static_function_cannot_access_non_static_member（第二个实参是
//      当前函数名；属性访问器取 `*get` / `*set` 的末 3 字符，见 :77-80 的 propDeclName "*et"）；
//      `funcDecl == nullptr`（当前函数体是 lambda）→ :85 sema_static_lambdaExpr_cannot_access_non_static。
//      锚点：都是 `Diagnose(re, …)` 的一字符，落在裸名标识符首字母。
//   3) 类型名访问实例成员 → src/Sema/TypeCheckReference.cpp:449 FilterAndCheckTargetsOfNameAccess，
//      先把非 STATIC / 非 GLOBAL / 非 ENUM_CONSTRUCTOR 的候选全部 EraseIf 掉（:457-460），:461-463 候选为空时
//      :462 `diag.Diagnose(ma, sema_illegal_access_non_static_member, ma.field.Val())`；锚点是 MemberAccess
//      节点首字符，即**类型名（含泛型实参列表）第一个字符**。调用点 src/Sema/TypeCheckExpr/NameReferenceExpr.cpp:625
//      与 :674（InferStaticAccess / InferArrayStaticAccess）。
//   4) 对象访问静态成员 → src/Sema/TypeCheckReference.cpp:500 FilterAndGetTargetsOfObjAccess，:513-525 把静态
//      候选 erase 掉，:524-526 候选为空时 :525 `diag.Diagnose(ma, sema_object_cannot_access_static_member, ma.field.Val())`；
//      锚点是 MemberAccess 首字符，即**接收者首字符**（`this` / `super` / 形参 / 枚举值变量的第一个字符）。
//      调用点 NameReferenceExpr.cpp:734 与 :959-1015（GetObjMemberAccessTarget 的各类分支）。
//   5) 静态变量初始化器访问实例成员 → src/Sema/TypeCheckDecl.cpp:89 CheckStaticVarAccessNonStatic
//      （调用点 :256 CheckVarDecl 末尾，且要求 vd 带 STATIC）：Walker 遍历 initializer，遇到 RefExpr 时若
//      target 存在、不是 FUNC_DECL（:94-96 直接 SKIP_CHILDREN）、不是 COMPILER_ADD、target 既不 static/global
//      也不是类型声明、不是普通 / 枚举构造器、且 target 有 outerDecl，则 :107
//      `diag.Diagnose(vd, sema_static_variable_cannot_access_non_static_member, re->ref.identifier.Val())`；
//      **锚点是整条 static VarDecl 的 GetBegin 一字符**（非 refactor kind），即第一个修饰符或 `static` 的首字母；
//      遇到 MEMBER_ACCESS 或 FUNC_BODY 子树直接 SKIP_CHILDREN（:103-105），所以 `this.x` / `obj.x` / 嵌套
//      函数体里的引用不走本检查。
//   文案：include/cangjie/Basic/DiagnosticSema.def :242 illegal_access_non_static_member、
//      :247 object_cannot_access_static_member、:251 static_members_cannot_call_members、
//      :264 static_function_cannot_access_non_static_member、:266 static_variable_cannot_access_non_static_member、
//      :268 static_lambdaExpr_cannot_access_non_static；伴随诊断 :195 this_or_super_not_allowed_to_initialize_static_member、
//      :24 used_before_initialization、:197 illegal_usage_of_member。
//
// 本项目 CFIR 名（cfir/checkers/checkers-component-generator/.../diagnostics/CfirDiagnosticsList.kt）：
//   :516 STATIC_MEMBERS_CANNOT_CALL_MEMBERS              ← 官方 sema_static_members_cannot_call_members
//   :519 ILLEGAL_ACCESS_NON_STATIC_MEMBER                ← 官方 sema_illegal_access_non_static_member
//   :524 STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER ← 官方 sema_static_function_cannot_access_non_static_member
//   :529 STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC          ← 官方 sema_static_lambdaExpr_cannot_access_non_static
//   :534 STATIC_VARIABLE_CANNOT_ACCESS_NON_STATIC_MEMBER ← 官方 sema_static_variable_cannot_access_non_static_member
//   :539 OBJECT_CANNOT_ACCESS_STATIC_MEMBER              ← 官方 sema_object_cannot_access_static_member
// 生产者：
//   STATIC_MEMBERS_CANNOT_CALL_MEMBERS → CfirStaticContextThisUsageChecker
//     （cfir/checkers/src/.../checkers/expression/CfirExpressionSemanticsChecker.kt:401-418，工厂 :414，
//      reportOn(expression.calleeReference.source ?: expression.source)）；
//   STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER / STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC →
//     CfirStaticContextNonStaticMemberAccessChecker（同文件 :502-529，工厂 :517 / :524，reportOn(
//     calleeReference.source?.firstCharacterDiagnosticSource() ?: expression.source?.firstCharacterDiagnosticSource())，
//     accessKind 由 :533 staticNonStaticAccessKind 判定，LAMBDA 分支只认 CfirAnonymousFunction.isLambda
//     且要求 hasStaticEnclosingDeclarationAfter）；
//   STATIC_VARIABLE_CANNOT_ACCESS_NON_STATIC_MEMBER → CfirInitializationCheckers.kt:2000-2018（工厂 :2013，
//     reportOn(staticField.source?.firstCharacterDiagnosticSource())，只处理 explicitReceiver == null 的
//     CfirNamedAccessExpression）；
//   OBJECT_CANNOT_ACCESS_STATIC_MEMBER / ILLEGAL_ACCESS_NON_STATIC_MEMBER → cone 层诊断映射：
//     cfir/semantics/src/.../diagnostic/ResolutionDiagnostic.kt:390 ObjectCannotAccessStaticMember / :405
//     IllegalAccessNonStaticMember；错误类型在 cfir/resolve/src/.../calls/CfirMemberAccessForm.kt:23-36 声明
//     （CfirMemberAccessFormError.ObjectAccessedStaticMember / TypeQualifierAccessedNonStaticMember），由
//     calls/stages/CfirCheckDispatchReceiver.kt:45-56 产出，body/CfirCallResolver.kt:4052-4061 转为
//     ConeObjectCannotAccessStaticMemberError / ConeIllegalAccessNonStaticMemberError，再由
//     cfir/checkers/src/.../diagnostics/coneDiagnosticToCfirDiagnostic.kt:170-181（object 分支：锚
//     explicitReceiver 的 calleleeReference.source ?: source）与 :194-213（类型名分支：锚 explicitReceiver.source，
//     泛型参数实参另有 STATIC_VARIABLE_USE_GENERIC_PARAMETER 分流）产出诊断。
// 文案表 CfirErrorsDefaultMessages.kt：:354 static_members（与官方逐字一致）、:356 illegal_access、:361 static_function、
//   :366 static_lambda、:371 static_variable、:376 object_cannot_access。
//
// 可预判的锚点 / 条件差异（只写源码能直接看出的，**未跑 CFIR**，红绿由主会话的 CFIR 实跑决定）：
//   a) 六个 kind 的锚点粒度与官方一致：单字符（静态变量那条是"static VarDecl 首字符"单字符，不是整条声明）。
//   b) 泛型类型名访问实例成员（形态 18）：官方锚 `G<Int64, String>.a` 的 `G` 一字符；CFIR 的
//      mapIllegalAccessNonStaticMemberError 取 explicitReceiver.source，若该 source 覆盖整个
//      `SCAGenericStatic<Int64, String>` 表达式则比官方宽 —— 预期形态 18 在 CFIR 侧锚点偏宽。
//   c) `this` / `super`：官方条件是"任意带 STATIC 的 funcBody"；CFIR 的 CfirStaticContextThisUsageChecker 要求
//      findClosestDeclaration<CfirFunction>() 非空且其 status.isStatic。静态初始化器（不是 CfirFunction）、
//      静态属性访问器、静态上下文里的 lambda（CfirAnonymousFunction）体中的 `this`，源码上看 CFIR 取不到
//      "最近的静态 CfirFunction" —— 预期形态 4 / 5 / 13（static init 里的 this）与形态 9（静态 lambda 里的 this）
//      在 CFIR 侧缺诊断。`super` 在 CFIR 里是否走同一节点类型（源码只看得到 CfirThisReceiverExpression）需实跑确认。
//   d) STATIC_FUNCTION / STATIC_LAMBDA 的官方检查有 `re.ref.targets.size() <= 1` 前置（重载名不查），CFIR 的
//      CfirStaticContextNonStaticMemberAccessChecker 无该条件；本文件未取重载形态，不受影响。
//   e) 文案差异（源码可见，不影响内联名字断言）：官方 static_function 文案带第二个实参（当前函数名，属性访问器
//      为 get/set），CFIR 的 STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER 只渲染成员名；官方
//      static_lambdaExpr 文案为 "invalid use of non-static member '%s' in static lambda expression"，CFIR 渲染为
//      "cannot be accessed by static lambda expression"。
//
// 已有覆盖、本文件不重复（testData/llt 已用本族六个名字的形态，逐一列出备查）：
//   STATIC_MEMBERS_CANNOT_CALL_MEMBERS：llt/class/class11_test2.cj:6-7（interface static func 里 `this.i()`，
//     作默认参数值与作函数体各一处）；llt/record/record_this.cj:21（struct static func `return this`）；
//     llt/Extend/extend_this5.cj:6（`return this`）、:16（`this.f()`）、:26（`this.item`）。
//   STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER：llt/class/class11_test1.cj:7（interface static func 调实例
//     方法）；llt/class/static_call_non_static/class_static_call_non_static1.cj:31（static func 读实例字段）、
//     2.cj:14-20（默认参数值 / 局部函数内 / 函数引用 / 调用）、3.cj:9（`var a = f` 函数引用）、4.cj:10（局部函数内调用）、
//     5.cj:57（实例方法作实参）；llt/enum/enum26_1.cj:8（enum static func 调实例方法）；llt/class/class_access_control2.cj:19-20；
//     llt/class/static_function_call_func_disordered2.cj:16；llt/function/nestedDefaultParameter2_1.cj:35（lambda 内）；
//     llt/property/property_call_03.cj:20（static prop setter 里给实例 prop 赋值）；llt/call/variadic_err_static_call_non.cj:6；
//     llt/class/static_init/static_init_03.cj:8 与 llt/record/static_init/static_init_03.cj:8（static init 里读实例字段）；
//     llt/lambda/lambda01.cj:16,18；llt/record/record_access_control.cj:19-25；llt/record/static_call_non_static/record_static_call_non_static1.cj:23；
//     llt/record/record_7.cj:8（struct static func 读 / 调实例成员）。
//   STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC：llt/class/static_call_non_static/class_static_call_non_static2.cj:19,22,27,32；
//     llt/lambda/lambda01.cj:7,8（static let 初值 lambda、嵌套 lambda）；llt/record/member_variable/record_member_variable_init04.cj:7。
//   STATIC_VARIABLE_CANNOT_ACCESS_NON_STATIC_MEMBER：llt/class/class_static_member1.cj:7（class static var 初值 `a + b`，
//     伴随 USED_BEFORE_INITIALIZATION）。
//   OBJECT_CANNOT_ACCESS_STATIC_MEMBER：llt/class/class_static_member_access_invalid.cj:11（`B().a`，继承来的 static）；
//     llt/class/interface_this_invalid_1.cj:10（`this.foo()`）；llt/enum/enum16_memberFunc.cj:34（`e2.foo3()`）；
//     llt/record/record_access_control.cj:48,51,60,63；llt/binary/binary_error_report_01.cj:14；binary_error_report_02.cj:15-16,24-25（`super.i` / `super.foo()`）；
//     llt/call/variadic_err_static.cj:12（`A<Int64>(true).f()`）。
//   ILLEGAL_ACCESS_NON_STATIC_MEMBER：llt/enum/enum16_memberFunc.cj:35-36；llt/enum/enum29.cj:22,25,26；llt/record/record_access_control.cj:43,46,55,58；
//     llt/Extend/property/extend_enum_property/extend_enum_property1.cj:5,8；llt/array/array_access4.cj:5-6（`Array<Bool>[3]` 内建类型）。
//   跨目录已有（不在 testData/llt）：diagnostics2/declaration-status/staticCannotBeOpenAbstractOverride.cj:13、
//     diagnostics/coverage/declaration-status/staticIncompatibleModifiersRich.cj:13、diagnostics/override/implemented.cj:50
//     （REDEF_INSTANCE_ERROR，`redef` 修饰实例函数 = 官方 sema_redef_modify_static_func）；
//     diagnostics2/static-init/thisOrSuperInStaticInit.cj（`static var x = this.y` / `= super.y` 的
//     sema_this_or_super_not_allowed_to_initialize_static_member，CFIR 名 THIS_OR_SUPER_NOT_ALLOWED_TO_INITIALIZE_STATIC_MEMBER :458）；
//     diagnostics2/initialization/usedBeforeInitialization.cj（sema_used_before_initialization，CFIR 名 :571）；
//     diagnostics2/mut/immutableFunctionRestrictions.cj（sema_cannot_modify_var）。
// 本文件只补上述地方都没写的形态：static init 体里的 this、静态变量初始化器（var / let 两种宿主写法）、
// 静态属性访问器里的裸名与嵌套 lambda、静态上下文 lambda 里的 this、类型名访问实例成员的泛型类写法、
// struct / enum / interface / extend / 泛型 class 上的各形态、继承来的实例与静态成员、裸 this、顶层函数里的访问。
//
// 段内不写标记的伴随诊断（头部登记；均为别族、已在别处覆盖）：
//   sema_used_before_initialization（仅形态 10）：静态变量初始化器裸名读实例字段时，官方把该实例字段视为"尚未
//     初始化"（静态上下文永不初始化实例字段），故每条 static var / static let 初值引用都附带一条，锚在引用
//     标识符 1 字符；形态 10 中 1.0.5 / 1.1.3 均在 (4,27)..(4,28) 与 (5,27)..(5,28)。
//
// SUGGESTED 段：本文件无。与本族相邻的官方 kind（this_or_super_not_allowed_to_initialize_static_member、
//   used_before_initialization、cannot_modify_var、illegal_usage_of_member、redef_modify_static_func /
//   missing_redefined_func）在本项目都有对应名且已在别处覆盖；唯一无对应名且不可达的官方 kind 是
//   sema_static_member_in_interface_must_has_body（@Java 接口专用、v1.0.0 源码无引用点），见"已删除的形态 1"。

// ── 形态 1：class 的 static func 里 `this.<实例变量>`（读取）
// kind：sema_static_members_cannot_call_members "non-static variable 'this' cannot be referenced from a static context"
// 锚点：`this` 首字母 1 字符。1.0.5 / 1.1.3 一致 Range (4,16)..(4,17)。官方 TypeCheckReference.cpp:159。
// FILE: classStaticFuncThisInstanceVar.cj
class SCAClassThisVar {
    var a: Int64 = 0
    static func f(): Int64 {
        return <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his.a
    }
}

// ── 形态 2：class 的 static func 里 `this.<实例方法>()`（调用）
// kind：sema_static_members_cannot_call_members，锚 `this` 首字母。1.0.5 / 1.1.3 一致 Range (6,16)..(6,17)。
// 官方 CheckUsageOfThis 先于成员解析报出；即使目标解析成功也不会再报 IsLegalAccessFromStaticFunc（那条只查裸名）。
// FILE: classStaticFuncThisInstanceFunc.cj
class SCAClassThisFunc {
    func g(): Int64 {
        return 1
    }
    static func f(): Int64 {
        return <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his.g()
    }
}

// ── 形态 3：class 的 static func 里裸 `this`（把 this 当返回值）
// kind：sema_static_members_cannot_call_members，锚 `this` 首字母。1.0.5 / 1.1.3 一致 Range (3,16)..(3,17)。
// FILE: classStaticFuncThisAlone.cj
class SCAClassThisAlone {
    static func f() {
        return <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his
    }
}

// ── 形态 4：class 的 static init 体里 `this.<实例变量> = 1`
// kind：sema_static_members_cannot_call_members，锚 `this` 首字母。1.0.5 / 1.1.3 一致 Range (4,9)..(4,10)。
// 官方 CheckUsageOfThis 的静态性判据取自函数体的 STATIC 属性；静态初始化器的 funcBody 由
// DeclAttributeChecker.cpp:116 InheritMemberAttrRecursively 递归打上 STATIC。
// FILE: staticInitThisInstanceVar.cj
class SCAStaticInitThis {
    var a: Int64 = 0
    static init() {
        <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his.a = 1
    }
}

// ── 形态 5：class 的 static init 体里 `this.<静态变量> = 2`（静态成员被对象接收者访问 → 换一条 kind）
// kind：sema_object_cannot_access_static_member "object cannot access static member 'b'"，锚 `this` 首字母。
// 1.0.5 / 1.1.3 一致 Range (4,9)..(4,10)。此时 CheckUsageOfThis 那条不再出现（两版实测只有本条）。
// 官方 FilterAndGetTargetsOfObjAccess 把静态候选 erase 后候选为空（TypeCheckReference.cpp:524-526）。
// FILE: staticInitThisStaticVar.cj
class SCAStaticInitThisStatic {
    static var b: Int64 = 1
    static init() {
        <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>t<!>his.b = 2
    }
}

// ── 形态 6：class 的 static func 里裸名给实例变量赋值（`a = 1`，左值形态）
// kind：sema_static_function_cannot_access_non_static_member "'a' is non-static member, cannot be accessed by
// static function 'f'"，锚裸名首字母。1.0.5 / 1.1.3 一致 Range (4,9)..(4,10)。
// LLT 只覆盖了"读"形态（class_static_call_non_static1.cj:31 `var x = width`）与 prop 赋值
// （property_call_03.cj:20），本段补实例字段的左值形态。官方 TypeCheckExpr.cpp:83。
// FILE: classStaticFuncAssignInstanceVar.cj
class SCAClassAssign {
    var a: Int64 = 0
    static func f() {
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>a<!> = 1
    }
}

// ── 形态 7：static prop 的 getter 里裸名读实例变量（属性访问器被当作 static 函数）
// kind：sema_static_function_cannot_access_non_static_member，第二个实参是脱糖后的访问器名 `get`
// （官方 TypeCheckExpr.cpp:77-80 取 `*get` / `*set` 的末 3 字符）；锚裸名首字母。
// 1.0.5 / 1.1.3 一致 Range (5,20)..(5,21)。LLT property_call_03.cj 只有 setter 给实例 prop 赋值的形态。
// FILE: classStaticPropGetterInstanceVar.cj
class SCAClassStaticProp {
    var a: Int64 = 0
    static prop p: Int64 {
        get() {
            return <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>a<!>
        }
    }
}

// ── 形态 8：static prop 的 getter 体内定义 lambda，lambda 里裸名读实例变量
// kind：sema_static_lambdaExpr_cannot_access_non_static "invalid use of non-static member 'a' in static lambda
// expression"，锚裸名首字母。1.0.5 / 1.1.3 一致 Range (5,26)..(5,27)。
// lambda 的 funcBody 继承 getter 的 STATIC（DeclAttributeChecker.cpp:116-131），且 funcDecl 为空 → 走
// TypeCheckExpr.cpp:85 的 lambda 分支。
// FILE: classStaticPropGetterLambda.cj
class SCAClassStaticPropLambda {
    var a: Int64 = 0
    static prop p: Int64 {
        get() {
            let f = { => <!STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC!>a<!> }
            return 1
        }
    }
}

// ── 形态 9：static func 体内定义的 lambda 里写 `this.<实例变量>` —— 走 this 那条而不是 lambda 那条
// kind：sema_static_members_cannot_call_members，锚 `this` 首字母。1.0.5 / 1.1.3 一致 Range (4,22)..(4,23)。
// CheckUsageOfThis（TypeCheckReference.cpp:159）对 lambda 体内的 `this` 同样成立（lambda funcBody 带 STATIC），
// 故这里**没有** sema_static_lambdaExpr_cannot_access_non_static；官方 lambda kind 只覆盖裸名引用。
// FILE: lambdaThisInStaticContext.cj
class SCALambdaThis {
    var a: Int64 = 0
    static func f(): Int64 {
        let g = { => <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his.a }
        return 1
    }
}

// ── 形态 10：static var / static let 的初始化器里裸名引用实例成员（var 与 let 两个宿主写法）
// kind：sema_static_variable_cannot_access_non_static_member，锚**整条 static 变量声明的首字符** 1 字符
// （非 refactor kind → GetBegin 一字符；带修饰符时从第一个修饰符起算）。官方 TypeCheckDecl.cpp:107。
// 1.0.5 / 1.1.3 一致 Range (4,5)..(4,6) 与 (5,5)..(5,6)。
// 伴随诊断（不写标记，头部已登记）：sema_used_before_initialization "variable 'a'/'l' is used before
// initialization"，分别锚在初值里的引用标识符，Range (4,27)..(4,28) 与 (5,27)..(5,28)，两版一致。
// FILE: staticVarInitInstanceMember.cj
class SCAVarInitInstance {
    var a: Int64 = 1
    let l: Int64 = 2
    <!STATIC_VARIABLE_CANNOT_ACCESS_NON_STATIC_MEMBER!>s<!>tatic var x: Int64 = a
    <!STATIC_VARIABLE_CANNOT_ACCESS_NON_STATIC_MEMBER!>s<!>tatic let y: Int64 = l
}

// ── 形态 11：struct 的 static func 里裸名读实例字段 / `this.<实例字段>`
// kind：sema_static_function_cannot_access_non_static_member × 2（`a`、`l`，锚裸名首字母）+
// sema_static_members_cannot_call_members × 1（锚 `this` 首字母）。
// 1.0.5 / 1.1.3 一致 Range (6,9)..(6,10)、(7,9)..(7,10)、(8,9)..(8,10)。struct 的 static func 读实例字段在
// LLT record_access_control.cj:19-25、record_7.cj:8 已有；`this.<实例字段>` 形态没有（LLT 只有 record_this.cj:21 的裸 this）。
// FILE: structStaticFuncInstanceMember.cj
struct SCAStructStatic {
    var a: Int64 = 0
    let l: Int64 = 1
    static var b: Int64 = 1
    static func f() {
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>a<!>
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>l<!>
        <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his.a
        return 1
    }
}

// ── 形态 12：struct 的实例函数里 `this.<静态变量>` / `this.<静态属性>`
// kind：sema_object_cannot_access_static_member × 2，锚 `this` 首字母（静态属性的 `this` 在第 16 列）。
// 1.0.5 / 1.1.3 一致 Range (9,9)..(9,10) 与 (10,16)..(10,17)。LLT 只有 `A().c` 这类显式构造器接收者形态。
// FILE: structInstanceFuncThisStatic.cj
struct SCAStructObjectAccess {
    static var b: Int64 = 1
    static prop sp: Int64 {
        get() {
            return 2
        }
    }
    func f() {
        <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>t<!>his.b = 1
        return <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>t<!>his.sp
    }
}

// ── 形态 13：struct 的 static init 体里 `this.<实例变量> = 1`
// kind：sema_static_members_cannot_call_members，锚 `this` 首字母。1.0.5 / 1.1.3 一致 Range (4,9)..(4,10)。
// FILE: structStaticInitThisInstanceVar.cj
struct SCAStructInitThis {
    var a: Int64 = 0
    static init() {
        <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his.a = 1
    }
}

// ── 形态 14：enum 的 static func 里访问实例 prop / 实例方法 / 静态 prop
// kind：sema_static_function_cannot_access_non_static_member（`p` 裸名、`f()` 调用各一条）、
// sema_static_members_cannot_call_members（`this.p`、`this.f()` 各一条，锚 `this` 首字母）、
// sema_object_cannot_access_static_member（`this.sp`，锚 `this` 首字母）。
// 1.0.5 / 1.1.3 一致 Range (17,9)..(17,10)、(18,9)..(18,10)、(19,9)..(19,10)、(20,9)..(20,10)、(21,9)..(21,10)。
// LLT enum26_1.cj:8 只有"static func 调实例方法"一种形态；enum 体里不能有 var / init（见已删除的形态 2）。
// FILE: enumStaticFuncMemberAccess.cj
enum SCAEnumStatic {
    | A
    prop p: Int64 {
        get() {
            return 1
        }
    }
    static prop sp: Int64 {
        get() {
            return 2
        }
    }
    func f(): Int64 {
        return 3
    }
    static func g() {
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>p<!>
        <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his.p
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>f<!>()
        <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his.f()
        <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>t<!>his.sp
        return 1
    }
}

// ── 形态 15：enum 实例（形参）访问静态属性
// kind：sema_object_cannot_access_static_member "object cannot access static member 'sp'"，锚接收者首字母。
// 1.0.5 / 1.1.3 一致 Range (13,5)..(13,6)。`e.f()` 与 `SCAEnumAccess.sp` 合法（零诊断）。
// LLT enum16_memberFunc.cj:34 是对象访问静态**方法**的形态，静态属性没有。
// FILE: enumObjectAccessStaticProp.cj
enum SCAEnumAccess {
    | A
    static prop sp: Int64 {
        get() {
            return 1
        }
    }
    func f(): Int64 {
        return 1
    }
}
func SCAEnumObjectAccess(e: SCAEnumAccess) {
    <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>e<!>.sp
    e.f()
    SCAEnumAccess.sp
}

// ── 形态 16：interface 的 static func 里裸名读实例 prop、`this.p`、`this.<静态 prop>`
// kind：sema_static_function_cannot_access_non_static_member（`p`）、sema_static_members_cannot_call_members
// （`this.p`）、sema_object_cannot_access_static_member（`this.sp`），三条锚点都在第 9 列的首字符。
// 1.0.5 / 1.1.3 一致 Range (13,9)..(13,10)、(14,9)..(14,10)、(15,9)..(15,10)。
// 官方 IsLegalAccessFromStaticFunc 的 FieldLookup 用 SymbolKind::STRUCT（ScopeManager.h:32 把 interface 也算进去）。
// LLT class11_test1/2 只有接口里的实例方法两种形态（`i()` 与 `this.i()`），prop / 静态 prop 形态没有。
// FILE: interfaceStaticFuncMemberAccess.cj
interface SCAInterfaceStatic {
    prop p: Int64 {
        get() {
            return 1
        }
    }
    static prop sp: Int64 {
        get() {
            return 2
        }
    }
    static func s() {
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>p<!>
        <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his.p
        <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>t<!>this.sp
        return 1
    }
}

// ── 形态 17：extend 块里的 static func 裸名调用宿主类的实例方法
// kind：sema_static_function_cannot_access_non_static_member "'f' is non-static member, cannot be accessed by
// static function 'g'"，锚裸名首字母。1.0.5 / 1.1.3 一致 Range (7,9)..(7,10)。
// IsLegalAccessFromStaticFunc 在 nominal decl 不是 struct 时走 ExtendFieldLookup（TypeCheckExpr.cpp:68-71）。
// LLT extend_this5.cj 只有 `this.` 前缀的三种形态，裸名形态没有。
// FILE: extendStaticFuncCallInstanceFunc.cj
class SCAExtendHost {
    var a: Int64 = 0
    func f() {}
}
extend SCAExtendHost {
    static func g() {
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>f<!>()
        return 1
    }
}

// ── 形态 18：泛型 class 的 static func 访问实例成员 + 泛型类型名访问实例成员
// kind：sema_static_function_cannot_access_non_static_member（`a = 1`，锚裸名首字母 (5,9)）、
// sema_static_members_cannot_call_members（`this.a = 2`，锚 `this` 首字母 (6,9)）、
// sema_illegal_access_non_static_member（`SCAGenericStatic<Int64, String>.a`，锚类型名首字母 (11,5)）。
// 1.0.5 / 1.1.3 一致 Range (5,9)..(5,10)、(6,9)..(6,10)、(11,5)..(11,6)。
// `SCAGenericStatic<Int64, String>.b` 合法（零诊断）。
// FILE: genericClassStaticAccess.cj
class SCAGenericStatic<T, U> {
    var a: Int64 = 0
    static var b: Int64 = 1
    static func f() {
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>a<!> = 1
        <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his.a = 2
        return b
    }
}
func SCAGenericTypeName() {
    <!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>S<!>CAGenericStatic<Int64, String>.a
    let v = SCAGenericStatic<Int64, String>.b
    let _ = v
}

// ── 形态 19：子类 static func 访问继承来的实例成员（裸名赋值 / 裸名调用 / `this.` 前缀）与自身实例字段；
//           子类实例函数里 `this.<继承来的 static>` / `this.<自身 static>`
// kind：sema_static_function_cannot_access_non_static_member × 3（`sa = 1` @ (11,9)、`sf()` @ (12,9)、
// `return ia` @ (15,16)）、sema_static_members_cannot_call_members × 2（`this.sa` @ (13,9)、`this.sf()` @ (14,9)）、
// sema_object_cannot_access_static_member × 2（`this.ss` @ (18,9)、`this.is_` @ (19,9)）。
// 1.0.5 / 1.1.3 一致。继承来的成员与自身成员走同一条检查（FieldLookup 结果指针相同）；`this.ia` 在实例函数里合法。
// `super.<static>` 的形态在 LLT binary_error_report_02.cj:15-16,24-25 已有，本文件不重复。
// FILE: inheritedMembersInStaticFunc.cj
open class SCAInheritSuper {
    var sa: Int64 = 0
    static var ss: Int64 = 1
    func sf() {}
}
class SCAInheritSub <: SCAInheritSuper {
    var ia: Int64 = 0
    static var is_: Int64 = 2
    func ig() {}
    static func h() {
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>s<!>a = 1
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>s<!>f()
        <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his.sa = 2
        <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his.sf()
        return <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>i<!>a
    }
    func inst() {
        <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>t<!>his.ss = 1
        <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>t<!>his.is_ = 2
        return this.ia
    }
}

// ── 形态 20：顶层函数里"对象访问静态成员"与"类型名访问实例成员"（变量 / 调用 / 函数引用三种访问形态）
// kind：sema_object_cannot_access_static_member（`x.b = 2`，锚接收者 `x` 首字母 (13,5)）、
// sema_illegal_access_non_static_member × 3（`SCATopLevelHost.a = 1` @ (17,5)、`SCATopLevelHost.g()` @ (20,5)、
// `SCATopLevelHost.g` 函数引用 @ (23,13)，都锚类型名首字母）。1.0.5 / 1.1.3 一致。
// 顶层函数体不带 STATIC 属性，故顶层不做 IsLegalAccessFromStaticFunc / CheckUsageOfThis 那两类检查
// （CheckUsageOfThis 还要求存在外层 nominal decl，见 TypeCheckReference.cpp:151-153）。
// FILE: topLevelFunctionStaticAccess.cj
class SCATopLevelHost {
    var a: Int64 = 0
    static var b: Int64 = 1
    static func f(): Int64 {
        return 1
    }
    func g(): Int64 {
        return 2
    }
}
func SCATopLevelObject(x: SCATopLevelHost) {
    x.a = 1
    <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>x<!>.b = 2
    x.g()
}
func SCATopLevelTypeNameVar() {
    <!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>S<!>CATopLevelHost.a = 1
}
func SCATopLevelTypeNameFunc() {
    <!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>S<!>CATopLevelHost.g()
}
func SCATopLevelTypeNameFuncRef() {
    let h = <!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>S<!>CATopLevelHost.g
    let _ = h
}

// ── 合法对照 1（class）：static func 只碰静态成员、实例函数同时碰实例与静态成员、类型名访问静态成员
// 1.0.5 JSON 零诊断；1.1.3 语义层通过后 codegen 阶段 LLVM ERROR（前端零错误）。各条对应官方条件：
// 目标 decl 带 STATIC（IsLegalAccessFromStaticFunc :60 直接返回）；FilterAndCheckTargetsOfNameAccess 的候选
// 全部带 STATIC（:454-457 的 EraseIf 不过滤掉任何候选）。
// FILE: legalClassStaticAccess.cj
class SCALegalClass {
    var a: Int64 = 1
    let l: Int64 = 2
    static var b: Int64 = 3
    static func s1(): Int64 {
        return b
    }
    static func s2(): Int64 {
        return SCALegalClass.b + SCALegalOther.t + s1()
    }
    func i1(): Int64 {
        return a + l + b + this.a + SCALegalClass.b
    }
}
class SCALegalOther {
    static var t: Int64 = 4
}

// ── 合法对照 2（struct）：static func 读静态字段、实例函数混读实例与静态字段
// 1.0.5 JSON 零诊断；1.1.3 codegen 阶段 LLVM ERROR。
// FILE: legalStructStaticAccess.cj
struct SCALegalStruct {
    var a: Int64 = 1
    let l: Int64 = 2
    static var b: Int64 = 3
    static func s1(): Int64 {
        return b
    }
    func i1(): Int64 {
        return a + l + b + this.a
    }
}

// ── 合法对照 3（interface / enum / 泛型 class）：static 成员宿主齐全时的合法访问
// 1.0.5 JSON 零诊断；1.1.3 codegen 阶段 LLVM ERROR。
// FILE: legalInterfaceEnumGenericStaticAccess.cj
interface SCALegalInterface {
    static func s1(): Int64 {
        return 1
    }
}
enum SCALegalEnum {
    | A
    static func s1(): Int64 {
        return 1
    }
}
class SCALegalGeneric<T, U> {
    var a: Int64 = 1
    static var b: Int64 = 2
    static func s1(): Int64 {
        return b
    }
    func i1(): Int64 {
        return a + this.a
    }
}
func SCALegalGenericUse(): Int64 {
    return SCALegalGeneric<Int64, String>.b
}

// ── 合法对照 4：static init 里给静态字段裸名赋值、实例函数里给静态字段裸名赋值
// 1.0.5 JSON 零诊断；1.1.3 codegen 阶段 LLVM ERROR。裸名引用目标带 STATIC → 本族两条检查都不过滤。
// FILE: legalStaticInitAndInstanceAssign.cj
class SCALegalStaticInit {
    var a: Int64 = 0
    static var b: Int64 = 1
    static init() {
        b = 2
    }
    func f() {
        b = 3
        return this.a
    }
}

// 已删除的形态（1.0.5 / 1.1.3 实测取证，两版一致；以下形态写不出可达的本族诊断，故不写成段）：
//   1. interface 里的 static func / static prop 没有函数体（`static func f(): Int64` / `static prop p: Int64`）
//      → 两版**零诊断**。官方 sema_static_member_in_interface_must_has_body（include/cangjie/Basic/DiagRefactor/
//      DiagnosticSema.def:203 "static functions in '@Java'-annotated interfaces must have a body"）在 v1.0.0 的
//      src/ 全仓无引用点（死条目），且只针对 `@Java` 注解接口，本机该场景不可表达，故本族无可达的"静态成员缺体"形态。
//   2. enum / interface 体里写 static var（`enum E { | A; static var s: Int64 = 1 }`）
//      → parse_unexpected_declaration_in_scope "unexpected variable declaration in enum body"（锚 `var` 三字符，
//        Range (3,12)..(3,15)）/ "unexpected variable declaration in interface body"（锚 `var` 三字符，Range (2,5)..(2,8)），
//        两版一致；enum 体里写 `init()` 同理报 "unexpected constructor in enum body"。静态字段只在 class /
//        struct 上可写（对照：diagnostics2/static-init/staticFieldInitializationRules.cj 的"已删除的形态 1"）。
//      故 enum / interface 上的静态成员形态只能用 static func / static prop 表达（形态 14 / 15 / 16 与合法对照 3）。
//   3. class 体里嵌套 class（`class Outer { class Inner { ... } }`）→ parse_unexpected_declaration_in_scope
//      "unexpected class declaration in class body"（锚 `class`，Range (2,5)..(2,10)），两版一致，
//      "嵌套类"形态在本仓库两版 cjc 都不可表达，改用"外层 static func / 实例函数访问内层类型的静态成员"
//      不可行（同一条 parse 规则），故嵌套宿主形态不写。
//   4. 静态变量初始化器里的 `this.x` / `super.x` → sema_this_or_super_not_allowed_to_initialize_static_member
//      "'this' is not allowed to initialize static member"（锚 `this` 首字母 1 字符，两版一致，探针 `class { var a; static var x = this.a }`
//      报 Range (3,20)..(3,21)），**不是**本族 kind：CheckStaticVarAccessNonStatic 的 Walker 遇 MEMBER_ACCESS 子树
//      直接 SKIP_CHILDREN（TypeCheckDecl.cpp:103-105），`this` 本身又被 `!target->IsTypeDecl()` 过滤（:101）。
//      该 kind（CFIR 名 THIS_OR_SUPER_NOT_ALLOWED_TO_INITIALIZE_STATIC_MEMBER，:458）已在
//      diagnostics2/static-init/thisOrSuperInStaticInit.cj 覆盖，本文件不重复。
//   5. 静态变量初始化器里调用实例方法（`static var s4 = g()`，g 为实例方法）
//      → 本族 kind **不报**（target 是 FUNC_DECL，:94-96 直接 SKIP_CHILDREN）；1.0.5 / 1.1.3 另附一条
//        sema_illegal_usage_of_member "'g' is not allowed to be accessed before all member variables are
//        initialized"（锚 `g`，Range (14,21)..(14,22)，两版一致），属初始化族（CFIR 名 ILLEGAL_USAGE_OF_MEMBER，CfirDiagnosticsList.kt:562），
//        故不写成段。同理 `static var s = p`（p 为实例 prop）本族 kind 会报，但必伴随 ILLEGAL_USAGE_OF_MEMBER，
//        形态 10 改用实例字段形态单独表达。
//   6. struct 的 static func 里给实例字段赋值（`static func f() { a = 1 }`）
//      → 报 sema_cannot_modify_var "instance member variable 'a' cannot be modified in immutable function"
//        （锚 `a` 首字母，两版一致），**不报**本族 kind（class 同形态报本族 kind，见形态 6）。属 mut 族，
//        已在 diagnostics2/mut/immutableFunctionRestrictions.cj 覆盖。
//   7. `public static open func` → parse_conflict_modifier "'open' and 'static' modifiers conflict on function
//      declaration"（锚 `open`，Range (7,19)..(7,23)，两版一致），parser 族，已由
//      diagnostics2/declaration-status/staticOpenAndOverload.cj 的 INCOMPATIBLE_MODIFIERS 覆盖。
//   8. class 成员函数上的 `mut` 修饰符（`class C { mut func m() {} }`）→ parse_illegal_modifier_in_scope
//      "unexpected modifier 'mut' on function declaration in class body"（锚 `mut` 三字符，Range (4,5)..(4,8)，两版一致）；
//      struct 里 `mut func` 可写。mut 族不在本文件范围。
//   9. `redef func`（redef 修饰实例函数）→ sema_redef_modify_static_func "'redef' cannot be used to modify an
//      instance 'function'"（锚 `redef` 首字母 1 字符，Range (3,5)..(3,6)，两版一致；`redef static func` 在无父类型
//      的 class 里另报 sema_missing_redefined_func，锚声明首字符）。本项目名 REDEF_INSTANCE_ERROR（列表 :288、
//      工厂 CfirModifierChecker.kt:182、文案 :171），已由 diagnostics2/declaration-status/staticCannotBeOpenAbstractOverride.cj:13
//      与 diagnostics2/inheritance/overrideRedefRules.cj 覆盖，属 redef 修饰符族，本文件不重复。
//  10. 同一静态函数体内相邻的 `this.a` 与 `this.l` 的次序敏感性（实测观察，原因未在官方源码定位）：
//      探针 `struct S { var a = 0; let l = 1; static var b = 1; static prop sp {...}; static func f() { a; l; this.a;
//      this.l; b; this.b; sp; this.sp } static init() { this.a = 1 } }` 里 `this.a` 报而紧随其后的 `this.l` 与
//      static init 的 `this.a = 1` 都不报；把 `let l` 移到类型末尾后同一组语句三条全报。本文件各段不依赖该形态，
//      每段只用两版都实测通过的写法（形态 11 只取 `a` / `l` 裸名与 `this.a`）。