

open class I1<F, K> {
    static func f3(a: F, b: K) {
        println("123")
    }
}

class I2<K> <: I1<K, K> {}

main(): Unit {
    <!GENERIC_TYPE_INCONSISTENT!>I2<!>.f3(Int64(1), Int32(1))
}
