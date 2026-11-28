// RUN_PIPELINE_TILL: FRONTEND
// struct：字段（var / let / private）、构造器（无参 / 具名形参带默认值 / this 委托）、成员函数、static 函数、static const、mut prop、泛型 struct。
// 注意：默认值只能写在具名形参上（`x: Int64 = 0` 是官方 parse 错误），调用带具名形参的构造器要按名传参（`S(1, y: 2)`）。
// 官方依据：Cangjie 文档《定义 struct 类型》；cjc 1.0.5 / 1.1.3 零诊断（struct 里不能写 ~init，见 recovery/structFinalizer.cj）。
struct Point {
    var x: Int64 = 0
    let y: Int64
    private var hidden: Int64 = 0
    static const ORIGIN: Int64 = 0

    init() {
        y = 0
    }

    init(x: Int64, y!: Int64 = 0) {
        this.x = x
        this.y = y
    }

    init(from: Point) {
        this(from.x, y: from.y)
    }

    public func lengthSquared(): Int64 {
        return x * x + y * y + hidden
    }

    public mut prop magnitude: Int64 {
        get() {
            return lengthSquared()
        }

        set(v) {
            x = v
        }
    }

    public static func zero(): Point {
        return Point(0, y: 0)
    }
}

struct Wrapper<T> {
    var value: T

    init(value: T) {
        this.value = value
    }

    func get(): T {
        return value
    }
}
