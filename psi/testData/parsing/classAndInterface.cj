// 类型声明：class / interface / struct / extend 及其成员、继承与修饰符。
// 官方依据：Cangjie 文档《类与接口》《struct》《extend》；cjc 1.0.5 / 1.1.3 零诊断。
open class Animal {
    public let name: String

    public init(name: String) {
        this.name = name
    }

    public open func speak(): String {
        return "..."
    }
}

interface Display {
    func show(): String
}

interface Named <: Display {
    func label(): String
}

struct Point {
    public let x: Int64
    public let y: Int64

    public init(x: Int64, y: Int64) {
        this.x = x
        this.y = y
    }
}

class Circle <: Animal & Display {
    private let radius: Float64 = 1.0

    public init() {
        super("circle")
    }

    public override func speak(): String {
        return "circle"
    }

    public func show(): String {
        return speak()
    }
}

extend Point <: Display {
    public func show(): String {
        return "point"
    }
}
