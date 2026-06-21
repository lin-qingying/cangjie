

open class A<F, H> {}

extend<F, H> A<F, H> {
    static func f3(a: F, b: H) {
    }
}

class B<K> <: A<K, K> {}

main(): Unit {
    <!GENERIC_TYPE_INCONSISTENT!>B<!>.f3(Int64(1), Int32(1))
}
