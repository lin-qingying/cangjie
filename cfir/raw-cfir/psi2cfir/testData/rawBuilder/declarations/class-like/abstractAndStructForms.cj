// RUN_PIPELINE_TILL: FRONTEND
abstract class Shape {
    public func area(): Int64

    public func describe(): String {
        return "shape"
    }
}

class Square <: Shape {
    private let side: Int64

    init(side: Int64) {
        this.side = side
    }

    public func area(): Int64 {
        return this.side * this.side
    }
}

sealed abstract class Root {
    public init() {}
}

class Left <: Root {}
class Right <: Root {}

struct Point {
    let x: Int64
    let y: Int64

    init(x: Int64, y: Int64) {
        this.x = x
        this.y = y
    }
}
