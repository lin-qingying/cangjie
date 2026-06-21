


class Point {
    public var x: Int32 = 0
    public var y: Int32 = 0
    public init(x: Int32, y: Int32) {
        this.x = x
        this.y = y
    }
    public operator func -(<!CANNOT_HAVE_DEFAULT_PARAM!>oprand!: Point = Point<!NO_VALUE_FOR_PARAMETER!>()<!><!>): Point {
        Point(-oprand.x, -oprand.y)
    }
}

func foo(a: Int32, b!: Int64 = 10): Int64 {
    b + 1
}

main(): Int64 {
    var f = foo
    var d = f(1, 1)
    var e = <!NO_VALUE_FOR_PARAMETER, TYPE_MISMATCH!>f(1)<!>
}
