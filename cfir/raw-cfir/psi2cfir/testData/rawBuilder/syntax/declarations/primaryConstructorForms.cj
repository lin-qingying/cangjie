// RUN_PIPELINE_TILL: FRONTEND
// 主构造函数形态：定义在 class / struct 体内、名字与类型名相同、形参分普通形参与成员变量形参（`let` / `var`）。
// 覆盖：全 `let` 成员形参；`var` 成员形参；普通形参与成员形参混写（普通形参在前）；主构造函数自身的访问修饰符
// （无修饰 / internal / protected / private / const）；成员形参自身的访问修饰符（private / protected）；
// 主构造函数与 `init` 构成重载（另一个 `init` 也必须初始化成员形参生成的字段）；open 基类主构造函数 + 子类 `super(...)`；
// 泛型主构造函数与 `where` 约束；主构造函数体内 `this.` 赋值 `var` 成员、访问 `this` 成员变量、调用成员函数；
// 主构造函数只有普通形参，或形参列表为空。
// 官方依据：文档《定义 struct 类型》"struct 构造函数"节 —— 主构造函数名字与 struct 类型名相同，形参分普通形参与成员变量形参
// （需在参数名前加 `let` 或 `var`），示例 `public Rectangle(let width: Int64, let height: Int64) {}`、
// `public Rectangle(name: String, let width: Int64, let height: Int64) {}`。
// 官方源码：`src/Parse/ParseDecl.cpp:64 ParseDecl` → `:589 ParsePrimaryConstructor`（`SetPrimaryDecl` 后按
// `CheckDeclModifiers(..., DefKind::PRIMARY_CONSTRUCTOR)` 校验修饰符）；`src/Parse/ParserImpl.h:465 SeeingPrimaryConstructor`
// （scopeKind 仅限 `STRUCT_BODY` / `CLASS_BODY`）；`src/Parse/ParserUtils.cpp:552 SeeingPrimaryIdentifer`
// （标识符后跟 `(`/`[`/`{` 且与类型名编辑距离 ≤ 1）；`src/Parse/ParseDecl.cpp:1916 ParseLetOrVarInParamList`
// （仅主构造函数作用域下 `let`/`var` 形参才是成员变量形参 `isMemberParam`，其它作用域报 `parse_expected_parameter_rp`）；
// `src/Parse/ParseDecl.cpp:1896 DiagMemberParameterAfterRegular`（普通形参必须排在成员变量形参之前）；
// `src/Parse/ParseDecl.cpp:1816 ParseAssignInParam` 与 `:1939 CheckModifierInParamList`（默认值只对具名形参开放，成员形参 `const` 被拒）；
// `src/Parse/ParserModifierRules.cpp:232 AGGREGATE_BODY_INSTANCE_INIT_MODIFIERS` 与 `:286 DefKind::PRIMARY_CONSTRUCTOR`
// （主构造函数只接受 public / protected / internal / private / const）；`src/Parse/ParseDecl.cpp:913 ParseClassDecl`（`:930 SetPrimaryDecl`）、
// `:1119 ParseStructDecl`（`:1146 SetPrimaryDecl`）；`src/Sema/Desugar/DesugarInTypeCheck.cpp:153
// DesugarPrimaryCtorHandleParamSetEachParam`（成员形参 desugar 成成员变量并在函数体末尾追加 `this.<name> = <name>`）。
// cjc 1.0.5 / 1.1.3 零诊断。
struct PctorRect {
    public PctorRect(let width: Int64, let height: Int64) {}

    public func area(): Int64 {
        return this.width * this.height
    }
}

class PctorCounter {
    public PctorCounter(var count: Int64) {
        this.count = count + this.offset()
    }

    private func offset(): Int64 {
        return 1
    }

    public func bump(): Unit {
        this.count = this.count + 1
    }

    public func value(): Int64 {
        return this.count
    }
}

struct PctorNamed {
    var label: String = ""

    public PctorNamed(label: String, let width: Int64, var height: Int64) {
        this.label = label
        this.height = height + 1
    }

    public func describe(): String {
        return "${this.label}:${this.width}x${this.height}"
    }
}

struct PctorModNone {
    PctorModNone(let v: Int64) {}

    func get(): Int64 {
        return this.v
    }
}

struct PctorModInternal {
    internal PctorModInternal(let v: Int64) {}

    func get(): Int64 {
        return this.v
    }
}

struct PctorModProtected {
    protected PctorModProtected(let v: Int64) {}

    func get(): Int64 {
        return this.v
    }
}

struct PctorModPrivate {
    private PctorModPrivate(let v: Int64) {}

    func get(): Int64 {
        return this.v
    }
}

struct PctorModConst {
    const PctorModConst(let v: Int64) {}

    func get(): Int64 {
        return this.v
    }
}

class PctorMemberMods {
    public PctorMemberMods(private var secret: Int64, protected var level: Int64) {}

    public func sum(): Int64 {
        return this.secret + this.level
    }
}

struct PctorOverloads {
    public PctorOverloads(let value: Int64) {}

    public init(base: Int64, extra: Int64) {
        this.value = base + extra
    }

    public func get(): Int64 {
        return this.value
    }
}

open class PctorBase {
    public PctorBase(let seed: Int64) {}

    public open func value(): Int64 {
        return this.seed
    }
}

open class PctorSub <: PctorBase {
    public init() {
        super(0)
    }

    public open func value(): Int64 {
        return 9
    }
}

class PctorGeneric<T> where T <: Any {
    public PctorGeneric(let item: T) {}

    public func get(): T {
        return this.item
    }
}

struct PctorPlain {
    var stored: Int64 = 0

    public PctorPlain(value: Int64) {
        this.stored = value
    }

    public func get(): Int64 {
        return this.stored
    }
}

struct PctorEmpty {
    public PctorEmpty() {}

    func get(): Int64 {
        return 0
    }
}

func primaryConstructorForms(): Int64 {
    let rect = PctorRect(2, 3)
    let counter = PctorCounter(1)
    counter.bump()
    let named = PctorNamed("a", 1, 2)
    let overload = PctorOverloads(1)
    let overload2 = PctorOverloads(1, 2)
    let sub = PctorSub()
    let generic = PctorGeneric<Int64>(5)
    let plain = PctorPlain(4)
    let empty = PctorEmpty()
    let memberMods = PctorMemberMods(1, 2)
    let total = rect.area() + counter.value() + named.width + overload.get() + overload2.get() +
        sub.value() + generic.get() + plain.get() + empty.get() + memberMods.sum() +
        PctorModNone(1).get() + PctorModInternal(1).get() + PctorModProtected(1).get() +
        PctorModConst(1).get()
    named.describe()
    return total
}
