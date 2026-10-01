// RUN_PIPELINE_TILL: FRONTEND
// 泛型声明形态：1 / 2 / 3 个类型形参、带 where 约束的类、形参上界的约束在成员体内可直接调用（`T <: ToString` 后调用 toString）。
// 官方依据：Cangjie 文档《泛型》；官方 ParseDecl.cpp ParseGeneric / ParseGenericConstraints / ParseGenericUpperBound；cjc 1.0.5 / 1.1.3 零诊断。
class One<T> {
    public init(v: T) {}
}

class Two<A, B> {
    public init(a: A, b: B) {}
}

class Three<A, B, C> {
    public init(a: A, b: B, c: C) {}
}

class Constrained<T> where T <: ToString {
    private let v: T

    public init(v: T) {
        this.v = v
    }

    public func render(): String {
        return v.toString()
    }
}

func singleConstraint<T>(x: T): String where T <: ToString {
    return x.toString()
}

func doubleConstraint<T, U>(a: T, b: U): String where T <: ToString, U <: Hashable {
    return a.toString()
}

func genericDeclarationForms(x: One<Int64>, y: Two<Int64, String>, z: Three<Int64, Int64, Int64>, w: Constrained<String>): Unit {
    let _ = (x, y, z, w)
}
