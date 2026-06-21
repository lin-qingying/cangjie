

interface I1<T> {
    func test(a: T): Int32 {
        1
    }
}

interface I2<T> {
    func test(a: T): Int64 {
        1
    }
}

// conflict signature after default implementations been copied
class A<T, K> <: I2<K> & I1<T> {}

main() {
    let x = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>A<Int32, Int32><!>()
    0
}
