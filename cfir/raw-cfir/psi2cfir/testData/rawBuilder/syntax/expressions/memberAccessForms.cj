// RUN_PIPELINE_TILL: FRONTEND
// 成员访问形态：多级链 `a.b.c`、类静态成员 `C.v` 与静态 prop `C.p`、枚举静态工厂与实例方法 `E.make().n()`、结构字段链式读取。
// 官方依据：Cangjie 文档《成员访问》；cjc 1.0.5 / 1.1.3 零诊断。
class B {
    var c: Int64 = 1

    public init() {}
}

class A {
    var b: B = B()

    public init() {}
}

class C {
    static var v: Int64 = 1

    static prop p: Int64 {
        get() { return 2 }
    }

    public init() {}
}

enum E {
    | A
    | B

    public static func make(): E {
        return A
    }

    public func n(): Int64 {
        return 1
    }
}

func memberAccessForms(a: A): Int64 {
    let chain = a.b.c
    let stat = C.v + C.p
    let en = E.make().n()
    return chain + stat + en
}
