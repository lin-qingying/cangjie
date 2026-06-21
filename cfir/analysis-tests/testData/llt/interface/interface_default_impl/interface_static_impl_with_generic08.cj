

interface I<T> {
    static func f1(a: T): Unit
    static func f2(a: T): Unit {
        f1(a)
    }
}

interface I1<K> <: I<K> {
    static func f1(a: K): Unit {
        println("I1")
    }
}

interface I2<K> <: I<K> {
    static func f1(a: Int32): Unit {
        println("I2")
    }
}

func f<T>(a: T): Unit {
    I1<T>.f2(a)

    <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I2<T>.f2(a)<!>
}

main() {
    <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>f<Int32><!>(Int32(1))

    I1<Int32>.f2(Int32(1))

    <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS, INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I2<Int32><!>.f2(Int32(1))
}
