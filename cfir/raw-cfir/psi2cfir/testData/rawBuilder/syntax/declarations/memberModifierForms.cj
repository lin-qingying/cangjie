// RUN_PIPELINE_TILL: FRONTEND
// 各类作用域成员声明的修饰符组合：只收录「作用域白名单里合法、且现有夹具尚未覆盖」的修饰符组合。
//
// 形态清单（按作用域）：
//   顶层 main ：`unsafe main()` —— main 的白名单只有 {UNSAFE}，不能再叠加任何可见性修饰符
//                （见 recovery/mainWithModifier.cj 的 `public main()` 反例）。
//   class 体  ：可见性缺省的 `func`；`private func` / `internal static func` / `private static func`（可见性 + static 无冲突）；
//                `static const func`（const + static 合法，见 CLASS_BODY_FUNCDECL_MODIFIERS 的 CONST:{OPEN}，且 const static
//                成员函数不要求 const 构造器）；`unsafe func`；`mut prop` 与 `internal mut prop`（get + set）；
//                `protected open func` 与其 `public override func` 实现（open 只允许配 public / protected，
//                配 internal / private 会冲突）。
//   interface 体：`static func`（带默认实现）、`static prop`（与抽象 prop 同文件共存）、`mut func`、`mut prop`（get + set）、
//                `unsafe func`、`const func`。接口成员隐式 public，写 `public` 只会得到冗余警告。
//   struct 体 ：`public mut func` 与 `private mut func`（mut 只能配可见性，配 static / const 冲突）；
//                `public mut prop`、`static mut prop`（配 `private static var` 作后备存储）、`private static var`。
//   enum 体   ：可见性缺省的 `func`、`private func`、`protected func`、`internal static func`、`const func`、`unsafe func`
//                （ENUM_BODY_FUNCDECL_MODIFIERS 没有 MUT，故 enum 成员不能写 `mut func` / `mut prop`）。
//   extend 体 ：`private mut func` / `internal mut func` / `public mut func`；`public static func`、`static func`（可见性缺省）、
//                `private static func`；`public const func`；`unsafe func`；`static prop`。
//
// 官方依据：
//   src/Parse/ParserModifierRules.cpp:118 TOPLEVEL_MAINDECL_MODIFIERS（整表只有 {UNSAFE}，见 :301 的 DefKind::MAIN 绑定）；
//   src/Parse/ParserModifierRules.cpp:122 CLASS_BODY_FUNCDECL_MODIFIERS（可见性四选一；STATIC:{OPEN,OVERRIDE,OPERATOR}；
//                REDEF:{OPEN,OVERRIDE,OPERATOR}；OPEN:{STATIC,INTERNAL,PRIVATE,REDEF,CONST}；UNSAFE:{}；CONST:{OPEN}）；
//   src/Parse/ParserModifierRules.cpp:136 INTERFACE_BODY_FUNCDECL_MODIFIERS（STATIC:{OPERATOR,MUT,OVERRIDE,OPEN}；
//                REDEF:{OPEN,OVERRIDE,OPERATOR}；OPERATOR:{STATIC}；MUT:{STATIC}；UNSAFE:{}；CONST:{}；PUBLIC:{}）；
//   src/Parse/ParserModifierRules.cpp:148 STRUCT_BODY_FUNCDECL_MODIFIERS（ACCESSIBLE_MODIFIERS；STATIC:{OPERATOR,MUT}；
//                MUT:{STATIC,CONST}；UNSAFE:{}；CONST:{MUT}）；
//   src/Parse/ParserModifierRules.cpp:159 ENUM_BODY_FUNCDECL_MODIFIERS（ACCESSIBLE_MODIFIERS；STATIC:{OPERATOR}；UNSAFE:{}；CONST:{}）；
//   src/Parse/ParserModifierRules.cpp:174 STRUCT_BODY_VARIABLE_MODIFIERS（ACCESSIBLE_MODIFIERS；STATIC:{}；CONST:{}）；
//   src/Parse/ParserModifierRules.cpp:180 EXTEND_BODY_FUNCDECL_MODIFIERS（ACCESSIBLE_MODIFIERS；OPERATOR:{STATIC}；MUT:{STATIC}；
//                STATIC:{MUT,OPERATOR}；UNSAFE:{}；CONST:{}）；
//   src/Parse/ParserModifierRules.cpp:189 CLASS_BODY_PROP_MODIFIERS（STATIC:{OPEN,OVERRIDE}；OPEN:{STATIC,INTERNAL,PRIVATE,REDEF}；MUT:{}）；
//   src/Parse/ParserModifierRules.cpp:201 INTERFACE_BODY_PROP_MODIFIERS（STATIC:{OPEN,OVERRIDE}；MUT:{}；PUBLIC:{}）；
//   src/Parse/ParserModifierRules.cpp:210 STRUCT_BODY_PROP_MODIFIERS（ACCESSIBLE_MODIFIERS；STATIC:{OVERRIDE}；MUT:{}）；
//   src/Parse/ParserModifierRules.cpp:225 EXTEND_BODY_PROP_MODIFIERS（ACCESSIBLE_MODIFIERS；STATIC:{}；MUT:{}）；
//   src/Parse/ParserModifierRules.cpp:232 AGGREGATE_BODY_INSTANCE_INIT_MODIFIERS（`const init()` 所需的 CONST:{} 条目）；
//   src/Parse/ParserModifierRules.cpp:320 INTERFACE_MEMBER_WARNING_MODIFIERS —— 接口成员上显式写 open / public 会告警
//                "interface member implies 'public'"（由 :140 ParseModifiers.cpp 的 ReportModifierWarning 报出），
//                故本文件的接口成员一律不写可见性修饰符。
//   src/Parse/ParseModifiers.cpp:49 CheckDeclModifiers 的修饰符校验入口：:104-114 白名单外修饰符报 parse_illegal_modifier_in_scope，
//                :115-127 冲突组合报 parse_conflicted_modifier，:68-72 修饰符集为空时给 IN_CLASSLIKE（即可见性缺省形态合法）。
//   src/Sema/ConstEvaluationChecker.cpp:376 ChkDeclNoConstInitNoConstFunc —— :383 `const` 且非 `static` 的成员函数必须配套
//                `const` 构造器（sema_no_const_init），因此本文件用 `static const func` 与「const 构造器 + const 实例成员函数」两种写法；
//                :60 同一个诊断对 EXTEND_DECL 会回溯到被扩展类型上要求 const 构造器。
//
// cjc 1.0.5 / 1.1.3 零诊断。

// 供 interface 默认实现里的 mut func / mut prop 使用的顶层存储；必须声明在被引用的 interface 之前。
const MEMBER_MOD_LIMIT: Int64 = 7

var memberModShared: Int64 = 0

// 顶层 main：白名单只允许 unsafe。
unsafe main(): Int64 {
    return memberModTotal()
}

// class 体：可见性与 static / const / unsafe / mut 的各种合法搭配。
class MemberModClass {
    static var shared: Int64 = 0
    static const K: Int64 = 1

    // 可见性缺省（ParseModifiers.cpp:68-72 记为 IN_CLASSLIKE）。
    func implicit(): Int64 {
        return 1
    }

    private func priv(): Int64 {
        return 2
    }

    internal static func intStatic(): Int64 {
        return 4
    }

    private static func privStatic(): Int64 {
        return 3
    }

    // const + static：合法，且不要求 const 构造器（ConstEvaluationChecker.cpp:383 排除 static）。
    static const func staticConst(): Int64 {
        return K * 2
    }

    unsafe func uns(): Int64 {
        return 6
    }

    mut prop writable: Int64 {
        get() { return shared }
        set(v) { shared = v }
    }

    internal mut prop intWritable: Int64 {
        get() { return shared }
        set(v) { shared = v }
    }

    public init() {}
}

// open 只能配 public / protected（OPEN:{STATIC,INTERNAL,PRIVATE,REDEF,CONST}），这里展示 protected + open 及其 override。
open class MemberModOpen {
    protected open func hook(): Int64 {
        return 1
    }

    public init() {}
}

class MemberModOpenImpl <: MemberModOpen {
    public init() {
        super()
    }

    public override func hook(): Int64 {
        return 2
    }
}

// interface 体：成员隐式 public，不写可见性修饰符。
interface MemberModInterface {
    static func origin(): Int64 {
        return 0
    }

    static prop originProp: Int64 {
        get() { return 0 }
    }

    mut func bump(): Int64 {
        memberModShared = memberModShared + 1
        return memberModShared
    }

    mut prop writable: Int64 {
        get() { return memberModShared }
        set(v) { memberModShared = v }
    }

    unsafe func uns(): Int64 {
        return 9
    }

    const func tag(): Int64 {
        return 8
    }

    func plain(): Int64 {
        return 1
    }
}

class MemberModInterfaceImpl <: MemberModInterface {
    public init() {}
}

// struct 体：mut 成员函数 / mut prop / static 存储与 static mut prop。
struct MemberModStruct {
    private var hidden: Int64 = 0
    private static var store: Int64 = 0
    public static const ZERO: Int64 = 0

    init() {
        hidden = 0
    }

    public static func make(): MemberModStruct {
        return MemberModStruct()
    }

    public mut func bump() {
        hidden = hidden + 1
    }

    private mut func privateBump() {
        hidden = hidden + 2
    }

    public mut prop mutableView: Int64 {
        get() { return hidden }
        set(v) { hidden = v }
    }

    static mut prop mutableStatic: Int64 {
        get() { return store }
        set(v) { store = v }
    }
}

// const 构造器 + const 实例成员函数的合法搭配（struct 的 const init 见 AGGREGATE_BODY_INSTANCE_INIT_MODIFIERS 的 CONST:{}）。
struct MemberModConstTarget {
    let limit: Int64 = MEMBER_MOD_LIMIT

    const init() {}
}

// extend 体：const 成员函数回溯到被扩展类型，要求该类型有 const 构造器（ConstEvaluationChecker.cpp:60）。
extend MemberModConstTarget {
    public const func plusOne(): Int64 {
        return MEMBER_MOD_LIMIT + 1
    }
}

// enum 体：可见性四选一 + static / const / unsafe；enum 成员没有 mut（ENUM_BODY_FUNCDECL_MODIFIERS 不含 MUT）。
enum MemberModEnum {
    | A
    | B(Int64)

    func base(): Int64 {
        return 1
    }

    private func privateTag(): Int64 {
        return 11
    }

    protected func protTag(): Int64 {
        return 12
    }

    internal static func make(v: Int64): MemberModEnum {
        return B(v)
    }

    const func tag(): Int64 {
        return 7
    }

    unsafe func uns(): Int64 {
        return 10
    }
}

struct MemberModTarget {
    public var v: Int64 = 0

    public init(v: Int64) {
        this.v = v
    }
}

// extend 体：可见性 + mut / static / unsafe / const / static prop 的各种搭配。
extend MemberModTarget {
    private mut func bumpPrivate() {
        v = v + 1
    }

    public mut func bump() {
        v = v + 1
    }

    internal mut func bumpInternal() {
        v = v + 1
    }

    public static func make(v: Int64): MemberModTarget {
        return MemberModTarget(v)
    }

    static func bareStatic(): Int64 {
        return 1
    }

    private static func privStatic(): Int64 {
        return 2
    }

    unsafe func uns(): Int64 {
        return v
    }

    static prop staticView: Int64 {
        get() { return 1 }
    }
}

// 调用侧：unsafe 成员必须在 unsafe 块内调用；mut 成员函数的接收者必须是可变绑定。
func memberModTotal(): Int64 {
    let c = MemberModClass()
    c.writable = c.writable + c.intWritable + c.implicit()
    var i: MemberModInterface = MemberModInterfaceImpl()
    var s = MemberModStruct.make()
    let e = MemberModEnum.make(1)
    var t = MemberModTarget(2)
    s.bump()
    t.bump()
    unsafe {
        let _ = c.uns() + t.uns() + i.uns() + e.uns()
    }
    i.writable = i.bump()
    return MemberModOpenImpl().hook() + MemberModClass.staticConst() + s.mutableView +
        MemberModStruct.ZERO + MemberModStruct.mutableStatic + e.base() + e.tag() + e.protTag() +
        MemberModConstTarget().plusOne() + MemberModTarget.staticView + MemberModTarget.bareStatic() +
        MemberModInterface.origin() + MemberModInterface.originProp + i.tag() + i.plain()
}
