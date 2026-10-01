// RUN_PIPELINE_TILL: FRONTEND
// 静态成员形态：类上的 static const / static var / static mut prop（get+set）、接口上的 static prop、枚举上的 static prop、struct 上通过 extend 访问 static const。
// 官方依据：Cangjie 文档《属性》《类成员》；cjc 1.0.5 / 1.1.3 零诊断。
class Statics {
    static const A: Int64 = 1
    static const B: String = "b"
    static var v: Int64 = 0

    static mut prop p: Int64 {
        get() { return v }
        set(x) { v = x }
    }

    public init() {}
}

interface StaticProp {
    static prop s: Int64 {
        get() { return 1 }
    }

    prop p: Int64
}

enum EnumStatic {
    | A

    public static prop n: Int64 {
        get() { return 1 }
    }
}

struct WithConst {
    public static const A: Int64 = 1

    public init() {}
}

extend WithConst {
    public func f(): Int64 { return WithConst.A }
}
