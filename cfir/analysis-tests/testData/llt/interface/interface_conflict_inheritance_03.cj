


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

interface I2 <: I0 {}

class <!INHERIT_SUPER_MEMBER_KIND_INCONSISTENT!>A<!> <: I2 & I1 {}

main() {
    return 0
}
