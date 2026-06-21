

interface Add<T1, T2> {
    operator func +(right: T1): T2
}

struct Point {
    public var x: Int32 = 1
    public var y: Int32 = 2
    public init(a: Int32, b: Int32) {
        x = a
        y = b
    }
}

extend Point <: <!EXTEND_NOT_INTERFACE!>Add<Point, <!UNDECLARED_TYPE_NAME!>P<!>oit><!> {
    public operator func +(right: Point): Point {
        Point(this.x + right.x, this.y + right.y)
    }
}

main(): Int64 {
    var p1 = Point(8, 24)
    let p2 = Point(9, 25)
    let p3 = p1 + p2
    if (p3.x == 17 && p3.y == 49) {
        return 0
    }
    return 1
}
