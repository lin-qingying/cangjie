// RUN_PIPELINE_TILL: FRONTEND
// 值构造：类 / 结构构造调用（具名形参按名传）、泛型构造调用、enum 构造（Enum.Ctor、非泛型 enum 的裸 Ctor / Ctor(args)；泛型 enum 的构造器裸调会报 sema_generic_type_without_type_argument，必须写成 GenericEnum<T>.Ctor）、
// 数组 / VArray 构造、Option 的 Some（`Some(v)` 可推导）与 None（必须写 `None<T>`，否则报 sema_generic_type_without_type_argument）。
// 官方依据：Cangjie 文档《枚举类型》《数组类型》《VArray 类型》；cjc 1.0.5 / 1.1.3 零诊断（带形参的 `operator func ()` 重载被拒绝，见 recovery/operatorCallOverload.cj）。
class WithInit {
    var a: Int64 = 0
    var b: String = ""

    public init() {
    }

    public init(a: Int64) {
        this.a = a
    }

    public init(a: Int64, b!: String) {
        this.a = a
        this.b = b
    }
}

struct Pair {
    var x: Int64 = 0
    var y: Int64 = 0

    init() {
    }

    init(x: Int64, y: Int64) {
        this.x = x
        this.y = y
    }
}

class Box<T> {
    var value: T

    public init(value: T) {
        this.value = value
    }
}

enum Color {
    | Red
    | Custom(Int64)
}

enum GenericEnum<T> {
    | Some(T)
    | None
}

class Callable {
    public init() {
    }

    public operator func ()(): Int64 {
        return 1
    }
}

func constructionForms(): Unit {
    let a = WithInit()
    let b = WithInit(1)
    let c = WithInit(1, b: "x")
    let e = Pair()
    let f = Pair(1, 2)
    let h = Box<Int64>(1)
    let i = Box<String>("x")
    let j = Array<Int64>()
    let k = Array<Int64>(2, repeat: 0)
    let l: VArray<Int64, $3> = [1, 2, 3]
    let m = VArray<Int64, $2>({ size => size })
    let m1 = VArray<Int64, $2>(repeat: 0)
    let n = Color.Red
    let o = Color.Custom(1)
    let p = GenericEnum<Int64>.Some(2)
    let q = Some(1)
    let r = None<Int64>
    let s = GenericEnum<Int64>.Some(3)
    let t = GenericEnum<Int64>.None
    let u = Callable()
    let v = u()
    let _ = (a, b, c, e, f, h, i, j, k, l, m, m1, n, o, p, q, r, s, t, v)
}
