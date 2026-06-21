


interface I1 {
    static func f(): Unit {}
}

interface I2 <: I1 {
    static func f1(): Unit
    static func f(): Unit {}
}

func f<T>(): Unit where T <: I1 {
    T.f()
}

main() {
    f<<!CANNOT_INSTANTIATED_BY_INCOMPLETE_TYPE!>I<!>2>() // error
}
