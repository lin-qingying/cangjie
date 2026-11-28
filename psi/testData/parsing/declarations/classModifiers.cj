// 类修饰符：abstract / open / sealed 与 static 成员（无体抽象成员仅在 .cj.d 合法）。
// 官方依据：Cangjie 文档《类》《修饰符》；cjc 1.0.5 / 1.1.3 零诊断。
abstract class Shape {
    public func area(): Float64 {
        return 0.0
    }
}

sealed abstract class Color {
    public func tag(): String {
        return "color"
    }
}

class Counter {
    static var count: Int64 = 0

    static func getCount(): Int64 {
        return count
    }
}

open class Base {
    public var v: Int64 = 0

    public init() {}
}

class Leaf <: Base {
    public init() {
        super()
    }
}
