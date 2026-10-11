// RUN_PIPELINE_TILL: FRONTEND
// 泛型调用：显式类型实参 `id<Int64>(1)`、省略（`id(2)`）、类构造的显式 / 省略类型实参、无形参泛型函数 `g<Int64>()`、形参类型不匹配时报
// sema_wrong_number_of_arguments（见 recovery）。
// 官方依据：Cangjie 文档《泛型》调用；cjc 1.0.5 / 1.1.3 零诊断。
func id<T>(x: T): T {
    return x
}

func empty<T>(): T {
    throw Exception()
}

class Box<T> {
    var v: T

    public init(v: T) {
        this.v = v
    }
}

func genericCallForms(): Int64 {
    let a = id<Int64>(1)
    let b = id(2)
    let c = Box<Int64>(3)
    let d = Box(4)
    let e = empty<Int64>()
    return a + b + c.v + d.v + e
}
