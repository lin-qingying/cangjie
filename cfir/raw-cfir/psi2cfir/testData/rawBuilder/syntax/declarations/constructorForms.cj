// RUN_PIPELINE_TILL: FRONTEND
// 构造器形态：this 委托（按名传实参）、super 委托作为构造器首语句、子类构造器里赋值成员变量、构造器中的 static 成员赋值。
// 官方依据：Cangjie 文档《构造函数》；官方 ParseDecl.cpp ParseParameter（具名形参 `!`）；cjc 1.0.5 / 1.1.3 零诊断。
open class Base {
    var a: Int64 = 0

    public init() {}

    public init(x: Int64) {
        this.a = x
    }
}

class Derived <: Base {
    var b: Int64 = 0

    public init() { super() }

    public init(x: Int64) {
        super(x * 2)
        this.b = x
    }

    public init(x: Int64, y: Int64) {
        this(x + y)
    }
}

class Delegating {
    var v: Int64 = 0

    public init() {}

    public init(x: Int64) {
        this.v = x
    }

    public init(x: Int64, y: Int64) {
        this(x + y)
    }
}

func constructorForms(): Unit {
    let a = Derived()
    let b = Derived(1)
    let c = Derived(1, 2)
    let d = Delegating(1, 2)
    let _ = (a, b, c, d)
}
