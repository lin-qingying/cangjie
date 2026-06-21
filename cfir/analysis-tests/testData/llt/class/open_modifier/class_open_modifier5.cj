

open class A {
    public open operator func ()() {
        1
    }
}

class B <: A {
    public static var obj = A()
    public init() {
        obj()
    }
    public operator override func ()(): Int64 {
        2
    }
}

main(): Int64 {
    0
}
