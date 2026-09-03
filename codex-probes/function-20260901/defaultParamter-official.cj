


class Point {
    public var x: Int32 = 0
    public var y: Int32 = 0
    public init(x: Int32, y: Int32) {
        this.x = x
        this.y = y
    }
    public operator func -(oprand!: Point = Point()): Point {
        Point(-oprand.x, -oprand.y)
    }
}

func foo(a: Int32, b!: Int64 = 10): Int64 {
    b + 1
}

main(): Int64 {
    var f = foo
    var d = f(1, 1)
    var e = f(1)
}
