// 枚举完整形态：泛型枚举、构造器参数、成员函数与运算符重载。
// 官方依据：Cangjie 文档《枚举类型》；cjc 1.0.5 / 1.1.3 零诊断。
enum Option<T> {
    | Some(T)
    | None
}

enum Color {
    | Red
    | Green
    | Blue

    public func isWarm(): Bool {
        return this == Red
    }

    public operator func ==(other: Color): Bool {
        return true
    }
}

enum TimeUnit {
    | Year(Int32)
    | Month(Int32, Int32)

    public func label(): String {
        return "year"
    }
}

enum Shape {
    | Circle(Float64)
    | Rectangle(Float64, Float64)

    public func area(): Float64 {
        return match (this) {
            case Circle(r) => r
            case Rectangle(w, h) => w * h
        }
    }
}
