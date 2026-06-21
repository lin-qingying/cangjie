

main(): Int64 {
    return 0
}

abstract class Base {
    public static func foo(): Int32 {
        return 0
    }
}

class Data <: Base {
    public func <!INHERIT_MEMBER_KIND_INCONSISTENT!>foo<!>(): Int32 {
        return 4
    }
}
