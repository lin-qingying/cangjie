// RUN_PIPELINE_TILL: FRONTEND
// extend 结构：多个 extend 块、extend 里实现接口、extend 里定义 operator（二元 / 一元 / `[]` / `!`）与 prop、泛型结构的 extend（含自身方法与泛型方法）。
// 官方依据：Cangjie 文档《扩展概述》；cjc 1.0.5 / 1.1.3 零诊断。
interface I {
    func f(): Int64
}

struct S {
    var v: Int64

    public init(v: Int64) {
        this.v = v
    }
}

extend S {
    public operator func + (o: S): S { return S(v + o.v) }

    public operator func - (o: S): S { return S(v - o.v) }

    public operator func - (): S { return S(-v) }

    public operator func !(): Bool { return v == 0 }

    public operator func [] (i: Int64): Int64 { return v + i }
}

extend S <: I {
    public func f(): Int64 { return v }
}

extend S {
    public prop doubled: Int64 {
        get() { return v * 2 }
    }
}

struct P<T> {
    var v: T

    public init(v: T) {
        this.v = v
    }
}

extend<T> P<T> {
    public func get(): T { return v }

    public func pair<U>(u: U): (T, U) { return (v, u) }
}

func extendStructForms(a: S, b: S, p: P<Int64>): Int64 {
    let c = a + b - a
    return c[0] + a.doubled + a.f() + p.get() + p.pair("s")[1].size
}
