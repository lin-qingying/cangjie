

abstract class Base {
    public static func foo(a: Int64) {
        return a
    }
}

class Data <: Base {
    public func <!INHERIT_MEMBER_KIND_INCONSISTENT!>foo<!>() {
        return 1
    }
}

main(): Int64 {
    0
}
