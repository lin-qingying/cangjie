

class Point <: ToString {
    var x: Int64
    var y: Int64
    init(x: Int64, y: Int64) {
        this.x = x
        this.y = y
    }

    operator func -(): Point {
        return Point(-x, -y)
    }

    public func toString(): String {
        return "Point(${x}, ${y})"
    }
}

main() {
    let p = Point(3, 2)
    let i: ToString = -p
    0
}
