

open class A {
    public open func f(): Int32 {
        return 0
    }

    public init() {
    }
}

class B <: A {
    public init() {
        f()
        var a = {=> f}
        func f3(c!: Int32 = f()) {
            f
        }
        a()
    }
}

main(): Int64 {
    0
}
