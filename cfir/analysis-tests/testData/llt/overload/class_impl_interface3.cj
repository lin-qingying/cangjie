

interface Base {
    static func foo(a: Int64) {
        return a
    }
}

abstract class Data <: Base {
    public func <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>foo<!>() {
        return 1
    }
}
