// struct：字段、init、成员函数、静态常量与扩展操作符。
// 官方依据：Cangjie 文档《struct 类型》；cjc 1.0.5 / 1.1.3 零诊断。
struct Point {
    public let x: Int64
    public let y: Int64

    public init(x: Int64, y: Int64) {
        this.x = x
        this.y = y
    }

    public func distanceSquared(other: Point): Int64 {
        let dx = this.x - other.x
        let dy = this.y - other.y
        return dx * dx + dy * dy
    }
}

struct Empty {}

struct Counter {
    private static const LIMIT: Int64 = 100

    public static func limit(): Int64 {
        return LIMIT
    }
}

extend Point {
    operator func +(other: Point): Point {
        return Point(this.x + other.x, this.y + other.y)
    }
}
