// RUN_PIPELINE_TILL: FRONTEND
// 泛型声明与使用：函数 / 类 / 结构 / 接口 / 枚举 / 类型别名上的类型形参，多形参，泛型构造调用，泛型 extend。
// 官方依据：Cangjie 文档《泛型》；cjc 1.0.5 / 1.1.3 零诊断。
func identity<T>(x: T): T {
    return x
}

class Box<T> {
    var value: T

    public init(v: T) {
        value = v
    }
}

struct Pair<A, B> {
    var firstValue: A
    var secondValue: B

    init(a: A, b: B) {
        firstValue = a
        secondValue = b
    }

    func left(): A {
        return firstValue
    }

    func right(): B {
        return secondValue
    }
}

interface Container<E> {
    func empty(): Bool

    prop size: Int64 {
        get() {
            return 0
        }
    }
}

enum Opt<T> {
    | Some(T)
    | None
}

type Alias<T> = Array<T>

func genericUses(): Unit {
    let a = identity<Int64>(1)
    let b = Box<Int64>(2)
    let c = Opt<Int64>.Some(3)
    let d: Alias<Box<Int64>> = [b]
    let e = Pair<Int64, String>(1, "x")
    let _ = (a, c, d, e)
}
