

open class Base {
    public let val = 1
}

open class Data <: Base {
    public func <!INHERIT_MEMBER_KIND_INCONSISTENT!>val<!>() {
        return 0
    }
}

main(): Int64 {
    return 0
}
