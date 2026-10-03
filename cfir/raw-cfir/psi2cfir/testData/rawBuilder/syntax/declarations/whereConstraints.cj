// RUN_PIPELINE_TILL: FRONTEND
// where 约束：函数、类、接口、结构上的单约束与多约束。官方 1.0.5 / 1.1.3 不允许 typealias、非泛型 extend 上写 where（见 recovery）。
// 官方依据：Cangjie 文档《泛型》「泛型约束（where）」；cjc 1.0.5 / 1.1.3 零诊断。
interface A {}
interface B {}

func single<T>(x: T): T where T <: A {
    return x
}

func multiple<T, U>(a: T, b: U): T where T <: A, U <: B {
    return a
}

class ClassBound<T> where T <: A {
    var value: T

    init(value: T) {
        this.value = value
    }

    func get(): T {
        return value
    }
}

interface InterfaceBound<T> where T <: A {
    func f(): T
}

struct StructBound<T> where T <: B {
    var value: T

    init(value: T) {
        this.value = value
    }

    func get(): T {
        return value
    }
}
