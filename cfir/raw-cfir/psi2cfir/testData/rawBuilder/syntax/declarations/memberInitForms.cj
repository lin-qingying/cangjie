// RUN_PIPELINE_TILL: FRONTEND
// 成员初始化形态：私有构造器只在类内 static 工厂里调用、构造器之间的 `this(...)` 委托、构造器里对成员赋值、接口默认 prop 在实现类实例上的读取。
// 官方依据：Cangjie 文档《构造函数》《属性》；cjc 1.0.5 / 1.1.3 零诊断。
class Factory {
    private var v: Int64 = 0

    private init(v: Int64) {
        this.v = v
    }

    public static func make(v: Int64): Factory {
        return Factory(v)
    }

    public func get(): Int64 {
        return v
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

interface WithDefault {
    prop p: Int64 {
        get() { return 1 }
    }
}

class Impl <: WithDefault {
    public init() {}
}

func memberInitForms(): Int64 {
    let a = Factory.make(1).get()
    let b = Delegating(1, 2)
    let c = Impl()
    return a + b.v + c.p
}
