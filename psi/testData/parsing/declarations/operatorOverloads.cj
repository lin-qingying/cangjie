// 运算符重载：下标、调用、比较、一元负号与二元运算。
// 官方依据：Cangjie 文档《运算符重载》；cjc 1.0.5 / 1.1.3 零诊断。
class Vec {
    private let data: Array<Int64> = [0, 0]

    public init() {}

    public operator func [](index: Int64): Int64 {
        return data[index]
    }

    public operator func [](index: Int64, value!: Int64): Unit {
        data[index] = value
    }

    public operator func +(other: Vec): Vec {
        return Vec()
    }

    public operator func -(other: Int64): Int64 {
        return other
    }

    public operator func -(): Vec {
        return Vec()
    }

    public operator func ==(other: Vec): Bool {
        return true
    }
}

struct Point {}

extend Point {
    operator func +(other: Point): Point {
        return Point()
    }
}
