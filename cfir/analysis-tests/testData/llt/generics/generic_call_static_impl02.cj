


interface I1 {
    mut static prop a: Int64
}

interface I2 <: I1 {}

func f<T>(): Unit where T <: I2 {
    T.a = 1
}

main() {
    f<<!CANNOT_INSTANTIATED_BY_INCOMPLETE_TYPE!>I<!>2>()
}
