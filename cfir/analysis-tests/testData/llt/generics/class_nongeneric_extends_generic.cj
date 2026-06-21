

open class Base<T> {
    public var member: T
    public init(a: T) {
        member = a
    }
}

class Child <: Base<Int32> {
    public init() {
        super(1)
    }
}

main() {
    var obj1: Base<Int32> = Child()
    var obj2: Child = Child()
    if (obj1.member == 1 && obj2.member == 1) {
        return 0
    }
    return 1
}
