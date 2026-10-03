// RUN_PIPELINE_TILL: FRONTEND
// 语义族：class / struct 的实例字段（var / let）未在构造器里初始化 —— 官方 sema_class_uninitialized_field。
// 取证方式：官方 cjc 1.0.5 与 1.1.3 两版实测，静态库模式
//   `cjc <段>.cj -o out.a --output-type=staticlib --diagnostic-format json`；
//   每个 `// FILE:` 段单独编译（官方整包编译时一处 parse 错误会吞掉其余段的语义诊断，
//   且 HasPrevDiag 只吞同起点同文案的重复诊断，逐段编译才不会互相屏蔽）。
//   1.1.3 本机在含显式构造器的单元上会在语义检查**之后**的 codegen / Reflection 阶段崩
//   （stderr: "Reflection format: modifier type is invalid" / "LLVM ERROR: Broken global value"），
//   语义诊断 JSON 仍完整产出，本文件按语义诊断写期望，两版逐段比对一致。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），
//   本文件标记逐字符对齐官方范围。
// 官方实现（`external/cangjie_compiler`，工作树 checkout 在 tag v1.0.0）：
//   - 入口：src/Sema/LegalityOfUsage/InitializationChecker.cpp
//     `:518-543 CheckInitInClassDecl` → `:1462-1509 CheckInitInTypeDecl` → `:1510-1547 CheckInitInConstructors`。
//     唯一的本族报告点是 `:1539-1545`：
//         if (fd.TestAttr(Attribute::COMPILER_ADD, Attribute::PRIMARY_CONSTRUCTOR)) {
//             diag.Diagnose(fd, DiagKind::sema_class_uninitialized_field, decl->identifier.Val());
//         } else {
//             diag.Diagnose(fd.TestAttr(Attribute::COMPILER_ADD) ? *decl : fd, DiagKind::sema_class_uninitialized_field,
//                 decl->identifier.Val());
//         }
//   - 锚点三个分支，全部因为 sema_class_uninitialized_field 是**非 refactor** kind
//     （include/cangjie/Basic/DiagnosticSema.def:191）而在 include/cangjie/Basic/DiagnosticEngine.h:844-857
//     退化成 GetBegin **1 个字符**：
//     ① 用户写的构造器 → `fd`（FuncDecl）→ `init` 关键字首字母；
//     ② 编译器补出的无参构造器（COMPILER_ADD）→ `*decl`（字段声明节点）→ `var` / `let` 关键字首字母；
//     ③ 主构造器 → `fd`（主构造器节点）。本机两版 cjc 的 parser 不接受主构造器写法（见"已删除的形态 1"），
//        ③ 在本仓库不可达。
//   - 哪些声明进入待检查集合（`:1549-1575 CollectDecls` + `:1577-1598 GetNonFuncDeclsInSuperClass`）：
//     类型体里**非 static 的 var / let**（`nonFuncDecls`）没有内联初值就进入 `unInitNonFuncDecls`；
//     `prop` 走 `CollectDeclsFromPropDecl` 进 `funcDecls`，**不参与**本族；`static` 成员走
//     `CheckStaticInitForTypeDecl`（static-init 族）；父类的**非 private 的 var / let**（不含 prop）
//     会作为 `superClassNonFuncDecls` 并入子类的待检查集合（私有字段不传递，见形态 7）。
//   - 跳过的四条路径（`:1510-1537`）：
//     ① 构造器以 `this(...)` / `super(...)` 委托开头（ConstructorCall::OTHER_INIT）→ 全部标记为已初始化并 return，
//        真正被检查的是**被委托到的那个构造器**（形态 5）；
//     ② 宿主是 `@Foreign` / `@Java` 类型；
//     ③ 构造器体被 `throw` 直接终止（`scopeTerminationKinds[funcBody->scopeName] == THROW_EXPR`，:1530-1533，
//        即对象根本不会被创建）→ 合法对照 L7；
//     ④ 字段已 `INITIALIZED`：有内联初值，或在构造器体里出现过对该字段的**赋值**（`UpdateInitializationStatus`
//        :341-355，`a = 0` / `this.a = 0` / `this.a += 1` / `this.a++` 都算赋值，**读**不算），
//        或被父类构造器初始化过。
//   - 同一构造器漏多个字段时只报**一条**：官方对每个漏掉的字段都会调用一次 Diagnose（:1536-1545 的循环），
//     锚点都是同一个 `init` 首字母；去重发生在 src/Basic/DiagnosticEngine.cpp:725-723
//     `ConvertOldDiagToNew` 把 `errorMessage` 置成**未替换 %s 的模板文案**，再由 :778
//     `HasPrevDiag(d.mainHint.range.begin, d.errorMessage)` 按 (起点, 模板文案) 去重
//     （实现 :716-723 用 `std::pair<Position, string>` 查表）——所以同一起点的多条只留第一条，
//     消息里的字段名是 `unInitNonFuncDecls` 里声明顺序最靠前的那个漏掉的字段（形态 8：漏 b、c → 报 b）。
//   - 文案 include/cangjie/Basic/DiagnosticSema.def:191-192
//     `ERROR(sema_class_uninitialized_field, "the uninitialized member variable '%s' is not initialized in the
//     constructor of class or struct")`，`%s` 是漏掉的字段名（形态 8 里是 `b`）。
// 本项目 CFIR 名（cfir/checkers/checkers-component-generator/.../diagnostics/CfirDiagnosticsList.kt:575）：
//   `CLASS_UNINITIALIZED_FIELD by error<PsiElement>(PositioningStrategy.ACTUAL_DECLARATION_NAME)`，
//   文案表 cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt:408 =
//   "Member variable ''{0}'' is not initialized in this constructor."（措辞与官方不同，本文件按官方写标记，
//   文案差异记在 README）；两个生产者都在
//   cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirInitializationCheckers.kt：
//     `:707-745 checkConstructorCompleteness`（显式构造器路径，source = `constructor.constructorNameDiagnosticSource()`，
//        CfirDeclarationDiagnosticSources.kt:362-386 取 `init` 关键字）；`:1321-1343 reportFieldsLeftUninitializedByDefaultConstructor`
//        （无构造器体路径，source = `field.source`）。
//   按源码可预判的锚点差异（只写源码能直接看出的，未跑 CFIR）：
//     ① 无构造器路径：官方锚字段声明首字符（`var`/`let` 的首字母），CFIR 的 `field.source` 可能是整条字段声明
//        （`var a: Int64`），也可能被 ACTUAL_DECLARATION_NAME 策略改写成字段名——以实跑为准；
//     ② 父类未初始化字段（形态 6）：CFIR 的 `checkConstructorCompleteness` 用 `owner.instanceFieldInfos(context)`
//        （不带 includeInherited），官方则把父类非私有字段并入子类待检查集合，子类构造器那条可能不产出；
//     ③ CFIR 对 `this(...)` 委托（`firstDelegationKind() == THIS`）直接 return，与官方 OTHER_INIT 一致；
//        对 `isRedundantPrimaryConstructor` 也 return，官方主构造器分支本仓库不可达；
//     ④ CFIR 的 `analyzeFunctionBody` 用 `returnExitStates` + `endState.terminated` 判正常出口，
//        `throw` 终止的构造器应当同样不产出（官方 :1530-1533）；
//     ⑤ lambda 体内的赋值官方不跟踪（CheckInitialization 走 FuncBody，lambda 是另一个 FuncBody，
//        字段的 INITIALIZED 属性不会跨越），CFIR 的 `analyzeAnonymousFunctionBody` 是否把赋值算进外层状态
//        需要实跑确认（形态 9）。
//   已有覆盖不重复：diagnostics2/static-init/staticFieldInitializationRules.cj（static 字段族，同一 checker 文件，
//   但 kind 不同：sema_type_uninitialized_static_field）；diagnostics2/initialization/usedBeforeInitialization.cj
//   （sema_used_before_initialization；本文件形态 10 与已删除形态 6 / 7 只把它当伴随诊断记录、不写标记）；
//   diagnostics/initialization/classUninitializedFieldRich.cj 只在 diagnostics（非 diagnostics2）目录，本矩阵不引用。
// 各段 Range 均按**逐段单独编译**的干净文本行号（`// FILE:` 行被剥离），故正文里的形如 (4,5) 是段内行号。

// ── 形态 1：class 的 `var` 无初值，`init` 体为空 —— 锚 `init` 首字母
// kind：sema_class_uninitialized_field；锚点：`init` 的 `i`，1 个字符；Range (4,5)..(4,6)，两版一致。
// FILE: classVarNoInitEmptyInit.cj
class IFI01VarNoInit {
    var a: Int64

    <!CLASS_UNINITIALIZED_FIELD!>i<!>nit() {
    }
}

// ── 形态 2：class 的 `let` 无初值，`init` 体里只有无关语句 —— 锚 `init` 首字母
// kind：sema_class_uninitialized_field；Range (4,5)..(4,6)，两版一致。`let` 与 `var` 走同一条报告点。
// FILE: classLetNoInitOtherStmt.cj
class IFI02LetNoInit {
    let a: Int64

    <!CLASS_UNINITIALIZED_FIELD!>i<!>nit(x: Int64) {
        println(x)
    }
}

// ── 形态 3：class 完全不写构造器（编译器补一个无参构造器）—— 锚**字段声明**首字母 `var`
// kind：sema_class_uninitialized_field；Range (2,5)..(2,6)，两版一致。
// FILE: classNoCtorVarNoInit.cj
class IFI03NoCtor {
    <!CLASS_UNINITIALIZED_FIELD!>v<!>ar a: Int64
}

// ── 形态 4：struct 不写构造器 —— 同样锚字段声明首字母（官方文案里的 "class or struct" 就是为这条）
// kind：sema_class_uninitialized_field；Range (2,5)..(2,6)，两版一致。
// FILE: structNoCtorLetNoInit.cj
struct IFI04StructNoCtor {
    <!CLASS_UNINITIALIZED_FIELD!>l<!>et a: Int64
}

// ── 形态 5：`this(...)` 委托：委托方构造器不报，被委托到的构造器报（锚它的 `init`）
// kind：sema_class_uninitialized_field；Range (9,5)..(9,6)，两版一致。
// 官方 :1517-1526：`constructorCall == OTHER_INIT` 的构造器把所有未初始化字段标记为已初始化并 return。
// FILE: classThisDelegateMissField.cj
class IFI05ThisDelegate {
    var a: Int64
    var b: Int64

    init() {
        this(1)
    }

    <!CLASS_UNINITIALIZED_FIELD!>i<!>nit(x: Int64) {
        this.b = x
    }
}

// ── 形态 6：父类没有构造器且有未初始化的非私有字段 —— 父类自己报一条（锚父类字段声明），
//     子类构造器再报一条（锚子类 `init`，消息里的字段名是父类的 `x`）
// kind：sema_class_uninitialized_field ×2；Range (2,5)..(2,6) 与 (8,5)..(8,6)，两版一致。
// 官方 :1577-1598 `GetNonFuncDeclsInSuperClass` 把父类体里**非 private、非 prop** 的 var / let 并入子类的待检查集合。
// FILE: classSuperFieldUninitialized.cj
open class IFI06SuperNoCtor {
    <!CLASS_UNINITIALIZED_FIELD!>v<!>ar x: Int64
}

class IFI06Sub <: IFI06SuperNoCtor {
    var y: Int64

    <!CLASS_UNINITIALIZED_FIELD!>i<!>nit() {
        this.y = 1
    }
}

// ── 形态 7：父类的字段是 `private` —— 不传递给子类，只有父类自己那一条
// kind：sema_class_uninitialized_field；Range (2,5)..(2,6)（`private` 修饰符首字母，官方锚整条字段声明首字符），
// 两版一致。官方 `GetNonFuncDeclsInSuperClass` 的过滤条件是 `!vd->TestAttr(Attribute::PRIVATE)`。与形态 6 对照。
// FILE: classSuperPrivateFieldUninitialized.cj
open class IFI07SuperPrivate {
    <!CLASS_UNINITIALIZED_FIELD!>p<!>rivate var x: Int64
}

class IFI07Sub <: IFI07SuperPrivate {
    var y: Int64

    init() {
        this.y = 1
    }
}

// ── 形态 8：一个构造器漏多个字段 —— 只报一条，锚同一个 `init`，消息里是声明顺序里第一个没被赋值的字段
// kind：sema_class_uninitialized_field（b 与 c 都漏，只出一条，消息字段名 `b`）；Range (6,5)..(6,6)，两版一致。
// 官方 :1535-1545 循环里每个未初始化字段都会调用一次 Diagnose，但所有报告锚点相同（`init` 首字母），
// DiagnosticEngine 的 CanBeEmitted 按起点去重，所以只剩一条。
// FILE: classOneCtorTwoFieldsMissing.cj
class IFI08TwoMissing {
    var a: Int64
    var b: Int64
    var c: Int64

    <!CLASS_UNINITIALIZED_FIELD!>i<!>nit() {
        this.a = 0
    }
}

// ── 形态 9：赋值写在 lambda 里（lambda 未调用 / 已调用都一样）—— 官方不跟踪，字段仍未初始化
// kind：sema_class_uninitialized_field；Range (4,5)..(4,6)，两版一致。
// 官方 `CheckInitialization(fd)` 只在构造器自己的 FuncBody 里标记字段，lambda 是另一个 FuncBody。
// FILE: classAssignInsideLambda.cj
class IFI09LambdaAssign {
    var a: Int64

    <!CLASS_UNINITIALIZED_FIELD!>i<!>nit() {
        let f = { => this.a = 0 }
        f()
    }
}

// ── 形态 10：构造器里把字段传给函数（读未初始化字段）—— 本族报在 `init`，同时伴随 used_before_initialization
// kind：sema_class_uninitialized_field；Range (8,5)..(8,6)，两版一致。
// 伴随诊断 sema_used_before_initialization "variable 'a' is used before initialization"（官方 JSON Range (9,19)..(9,20)），
// 属 used-before-init 族（diagnostics2/initialization/usedBeforeInitialization.cj），本文件只记录不写标记。
// FILE: classReadFieldInInit.cj
func ifi10Sink(v: Int64): Int64 {
    return v
}

class IFI10ReadField {
    var a: Int64

    <!CLASS_UNINITIALIZED_FIELD!>i<!>nit() {
        ifi10Sink(this.a)
    }
}

// ── 形态 11：构造器里调用实例方法 —— 本族报在 `init`，同时伴随 illegal_usage_of_member
// kind：sema_class_uninitialized_field；Range (4,5)..(4,6)，两版一致。
// 伴随诊断 sema_illegal_usage_of_member "'reset' is not allowed to be accessed before all member variables are
// initialized"（官方 JSON Range (5,9)..(5,10)，锚 `reset` 首字母），属成员访问合法性族，本文件只记录不写标记。
// FILE: classCallMemberInInit.cj
class IFI11CallMember {
    var a: Int64

    <!CLASS_UNINITIALIZED_FIELD!>i<!>nit() {
        this.reset()
    }

    private func reset() {
        this.a = 0
    }
}

// ── 形态 12：递归委托（`init()` 里 `this()`）—— 本族报在被委托到的构造器，伴随 recursive_constructor_call
// kind：sema_class_uninitialized_field；Range (8,5)..(8,6)，两版一致。
// 伴随诊断 sema_recursive_constructor_call "recursive constructor calling detected"（官方 JSON Range (4,5)..(4,9)，
// 锚第一个 `init` 整词，refactor kind，notes "depends on; depends on"），属构造器族（diagnostics2/constructor/superThisCallRules.cj），
// 本文件只记录不写标记。
// FILE: classRecursiveThisDelegate.cj
class IFI12RecursiveDelegate {
    var a: Int64

    init() {
        this()
    }

    <!CLASS_UNINITIALIZED_FIELD!>i<!>nit(x: Int64) {
        println(x)
    }
}

// ── 形态 13：条件分支只在一支里赋值 —— 字段仍算未初始化，锚 `init`
// kind：sema_class_uninitialized_field；Range (4,5)..(4,6)，两版一致。与合法对照 L3 对照。
// FILE: classConditionalInit.cj
class IFI13CondInit {
    var a: Int64

    <!CLASS_UNINITIALIZED_FIELD!>i<!>nit(b: Bool) {
        if (b) {
            this.a = 1
        }
    }
}

// ── 形态 14：循环里赋值 —— 循环可能一次都不执行，字段仍算未初始化，锚 `init`
// kind：sema_class_uninitialized_field；Range (4,5)..(4,6)，两版一致。
// FILE: classLoopAssign.cj
class IFI14LoopAssign {
    var a: Int64

    <!CLASS_UNINITIALIZED_FIELD!>i<!>nit() {
        for (i in 0..1) {
            this.a = i
        }
    }
}

// ── 形态 15：先 `return` 再赋值 —— 提前出口那条路径上没赋值，字段算未初始化，锚 `init`
// kind：sema_class_uninitialized_field；Range (4,5)..(4,6)，两版一致。
// 官方 `ctorUninitVarsMap` / `CheckInitialization` 把 `return` 当正常出口，return 之后的状态不会被采纳。
// FILE: classReturnBeforeAssign.cj
class IFI15ReturnFirst {
    var a: Int64

    <!CLASS_UNINITIALIZED_FIELD!>i<!>nit(b: Bool) {
        if (b) {
            return
        }
        this.a = 1
    }
}

// ── 形态 16：struct 的显式构造器里漏字段 —— 锚 `init`（struct 与 class 同一条报告点）
// kind：sema_class_uninitialized_field；Range (4,5)..(4,6)，两版一致。
// FILE: structInitMissField.cj
struct IFI16StructInitMiss {
    let a: Int64

    <!CLASS_UNINITIALIZED_FIELD!>i<!>nit(b: Int64) {
        println(b)
    }
}

// ── 形态 17：类里只有 `prop` 没有字段 —— prop 不参与本族（官方 `CollectDeclsFromPropDecl` 把 prop 归入 funcDecls），
//     零诊断。合法对照，说明 prop 的访问器体不受构造器初始化检查约束。
// FILE: legalPropOnlyNoField.cj
class IFI17PropOnly {
    prop p: Int64 {
        get() {
            return 1
        }
    }
}

// 合法对照段（1.0.5 / 1.1.3 两版均零 error、零 warning）：
// L1：构造器里把全部字段赋值（含 `let`）。
// FILE: legalAssignAllInInit.cj
class IFI18AssignAll {
    var a: Int64
    let b: Int64

    init() {
        this.a = 0
        this.b = 1
    }
}

// L2：全部字段有内联初值，没有构造器也不报。
// FILE: legalInlineInitAll.cj
class IFI19InlineAll {
    var a: Int64 = 0
    let b: Int64 = 1
}

// L3：if / else 两支都赋值 —— 正常出口上字段已初始化。
// FILE: legalIfElseBothAssign.cj
class IFI20IfElseBoth {
    var a: Int64

    init(b: Bool) {
        if (b) {
            this.a = 1
        } else {
            this.a = 2
        }
    }
}

// L4：try 与 catch 两边都赋值。
// FILE: legalTryCatchBothAssign.cj
class IFI21TryCatchBoth {
    var a: Int64

    init() {
        try {
            this.a = 0
        } catch (e: Exception) {
            this.a = 1
        }
    }
}

// L5：match 两支都赋值。
// FILE: legalMatchBothAssign.cj
class IFI22MatchBoth {
    var a: Int64

    init(b: Bool) {
        match (b) {
            case true => this.a = 1
            case _ => this.a = 2
        }
    }
}

// L6：`this(...)` 委托且被委托的构造器把字段都初始化了。
// FILE: legalThisDelegateComplete.cj
class IFI23DelegateComplete {
    var a: Int64
    var b: Int64

    init() {
        this(1)
    }

    init(x: Int64) {
        this.a = 0
        this.b = x
    }
}

// L7：构造器体被 `throw` 直接终止 —— 对象不会被创建，官方跳过整段检查（:1530-1533），零诊断。
// FILE: legalThrowTerminatedInit.cj
class IFI24ThrowInit {
    var a: Int64

    init() {
        throw Exception()
    }
}

// L8：先赋值再读 / 先赋值再 return。
// FILE: legalAssignThenRead.cj
class IFI25AssignThenRead {
    var a: Int64

    init(b: Bool) {
        this.a = 0
        if (b) {
            return
        }
        println(this.a)
    }
}

// L9：字段初值间接引用另一个字段（内联初值）—— 有初值即已初始化，零诊断。
// FILE: legalIndirectInlineInit.cj
class IFI26IndirectInline {
    var a: Int64 = 0
    var b: Int64 = this.a
}

// L10：子类构造器不显式调 `super()`（父类有无参构造器）—— 合法，零诊断。
// FILE: legalSubNoSuperCall.cj
open class IFI27Base {
    var x: Int64

    init() {
        this.x = 0
    }
}

class IFI27Sub <: IFI27Base {
    var y: Int64

    init() {
        this.y = 1
    }
}

// 已删除的形态（取证 kind 与 Range 见下；1.0.5 / 1.1.3 两版一致，除注明外）：
//   1. 主构造器写法 `struct S(\n    let a: Int64\n) { ... }` / `class C(let a: Int64) { ... }`
//      → parse_expected_left_brace "expected '{', found '('"（锚 `(`，1 字符）。本机两版 cjc 的 parser
//      都不接受主构造器写法，所以官方 :1539-1540 的 PRIMARY_CONSTRUCTOR 分支在本仓库不可达；
//      CFIR 的 `isRedundantPrimaryConstructor` 分支同理。探针 d04 / d05 / d06 / p01-p07。
//   2. interface 体里的 `var` → parse_unexpected_declaration_in_scope "unexpected variable declaration in
//      interface body"（锚 `var`，3 字符）。属 parser 族。探针 s19。
//   3. enum 体里的实例字段 `var x: Int64 = 0` → 同款 parse_unexpected_declaration_in_scope（锚 `var`）。
//      enum 构造器形参只有类型、没有字段声明（`enum E { | A(Int64) }` 合法、零诊断），因此本族在 enum 上不可达。
//      探针 d08 / p08 / p09（`A(v: Int64)` 具名形参形态本仓库 parser 不接受）。
//   4. 构造器里 `this(1)` 不在第一条语句 → sema_illegal_place_of_calling_this_or_super "call to 'this' must be
//      first expression in constructor of class 'X'"（锚 `this` 的 `t`），属 constructor 族
//      （diagnostics2/constructor/superThisCallRules.cj）。探针 v8 / s07。
//   5. 构造器里裸块 `{ this.a = 0 }` → parse_expected_double_arrow_in_lambda（parser 把裸块当 lambda）。
//      与形态 13 的 if 分支对照，赋值语义相同但语法不可用。探针 d11 / p10。
//   6. 构造器里给字段做复合赋值 / 自增（`this.a += 1`、`this.a++`）→ 只有
//      sema_used_before_initialization（锚 `this.a` 的 `a`），**没有** sema_class_uninitialized_field：
//      官方 `UpdateInitializationStatus`（:341-355）把这类表达式也当成赋值，字段已算初始化。
//      属 used-before-init 族，本文件不写标记。探针 s29 / p11 / p12。
//   7. 构造器里读字段（`println(a)`）后用裸名 `a = 0` 赋值 → 只有 sema_used_before_initialization（锚 `a`），
//      没有本族诊断：裸名赋值同样把字段标记为已初始化。属 used-before-init 族。探针 s18。
//   8. 子类构造器里给父类未初始化字段赋值（`this.x = 0`）→ 仍报一条，锚在**父类**的字段声明（2,5)，
//      因为父类自己检查时没有构造器；本族不锚子类构造器。探针 p16。
//   9. extend 体里不能有字段，只有函数 / prop（DiagRefactor/DiagnosticSema.def:147
//      sema_extend_illegal_member "illegal extend member, only functions, props, associated types are allowed"），
//      本族在 extend 上不可达。官方 :544-565 `CheckInitInExtendDecl` 只把被扩展类型的未初始化字段
//      临时借给 extend 体检查、用完即 DisableAttr，因此 extend 内出现的报错锚点仍是被扩展类型的字段声明。
//  10. `class C() { var a: Int64 }`（空参数列表）→ 同形态 3，锚字段声明；写成主构造器形态则见已删除形态 1。探针 p06。