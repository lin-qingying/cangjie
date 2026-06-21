


interface I1 {
    func foo(): Int
}

interface I2 {
    func foo(): String
}

func bar<T>(x: T) where <!INHERIT_MEMBER_TYPE_INCONSISTENT!>T <: I1 & I2<!> {
}

class A<T> where <!INHERIT_MEMBER_TYPE_INCONSISTENT!>T <: I1 & I2<!> {}

main() {
}
