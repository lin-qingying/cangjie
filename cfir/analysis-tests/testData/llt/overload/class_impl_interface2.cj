

interface Base {
    func foo(a: Int64) {
        return a
    }
}

class Data <: Base {
    public static func <!INHERIT_MEMBER_KIND_INCONSISTENT!>foo<!>() {
        return 1
    }
}
