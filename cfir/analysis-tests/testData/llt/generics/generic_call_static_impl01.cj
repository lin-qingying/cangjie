


interface I1 {
    static func f(): Unit
}

interface I2 <: I1 {}

func f<T>(): Unit where T <: I2 {
    T.f()
}

main() {
    f<<!CANNOT_INSTANTIATED_BY_INCOMPLETE_TYPE!>I2<!>>()
}
