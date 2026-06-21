


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

interface I2 {
    func test(): Int64
}

interface <!INHERIT_SUPER_MEMBER_KIND_INCONSISTENT!>A<!> <: I0 & I1 & I2 {}

main() {
    return 0
}
