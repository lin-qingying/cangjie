


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

func test<T>(a: T): Unit where T <: I0 & I1 {
    <!AMBIGUOUS_USE!>a.test<!>
}

main() {}
