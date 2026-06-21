


interface I0 {
    func test() {}
}

interface I1 {
    prop test: Int64 {
        get() {
            1
        }
    }
}

class <!INHERIT_SUPER_MEMBER_KIND_INCONSISTENT!>A<!> <: I0 & I1 {}

class B {}

<!INHERIT_SUPER_MEMBER_KIND_INCONSISTENT!>extend B <: I0 & I1 {}<!>

main() {
    return 0
}
