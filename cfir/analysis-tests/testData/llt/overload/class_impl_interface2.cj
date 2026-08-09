

interface Base {
    func foo(a: Int64) {
        return a
    }
}

class Data <: Base {
    public static func <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>foo<!>() {
        return 1
    }
}
