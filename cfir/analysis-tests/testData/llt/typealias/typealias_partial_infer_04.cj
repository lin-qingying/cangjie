


// Test for compilation of instantiation
open class I1<F, K> {}

extend<F, K> I1<F, K> {
    static func f3(a: F, b: K) {
        return 11
    }
}

class I2<K> <: I1<Int64, K> {}

main() {
    let a = I2.f3(1, Int32(1))
    if (a != 11) {
        return 1
    }
    return 0
}
