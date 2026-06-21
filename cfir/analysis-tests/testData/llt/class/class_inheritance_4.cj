

abstract class C1 {
    public func foo(): Int64
}

open class C2 <: C1 {
    public func foo(): Int64 {
        return 1
    }
}

class C3 <: C2 {}

main(): Int64 {
    var obj = C3()
    if (obj.foo() == 1) {
        return 0
    }

    return 1
}
