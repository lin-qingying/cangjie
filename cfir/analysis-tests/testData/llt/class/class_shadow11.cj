

open class Base {
    public func val() {
        return 0
    }
}

open class Data <: Base {
    public let <!INHERIT_MEMBER_KIND_INCONSISTENT!>val<!> = 1
}

main(): Int64 {
    return 0
}
