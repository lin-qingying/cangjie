// RUN_PIPELINE_TILL: FRONTEND
// `redef` 的合法形态：只能修饰 static 成员（函数与 prop），顺序 `public redef static`；实例成员要用 `override`，且基类成员必须是 `open`
// （非 open 成员报 sema_cannot_override）。
// 官方依据：Cangjie 文档《成员函数》《属性》；官方 Sema/TypeCheckReference.cpp（sema_redef_modify_static_func）；cjc 1.0.5 / 1.1.3 零诊断。
open class Base {
    public init() {}

    public static func s(): Int64 {
        return 1
    }

    public static prop p: Int64 {
        get() { return 2 }
    }

    public open func f(): Int64 {
        return 3
    }
}

class Derived <: Base {
    public init() {
        super()
    }

    public redef static func s(): Int64 {
        return 10
    }

    public redef static prop p: Int64 {
        get() { return 20 }
    }

    public override func f(): Int64 {
        return 30
    }
}

func redefMemberForms(): Int64 {
    return Derived.s() + Derived.p + Derived().f()
}
