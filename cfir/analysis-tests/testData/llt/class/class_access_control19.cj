

interface I0 {}

interface I<F, H> {
    func f3(a: F, b: H) {}
}

open class A<F, H> {}

extend<F, H> A<F, H> <: I<F, H> where H <: I0 {}

class B<K> <: A<K, K> {}

main(): Unit {
    // type of 'B<Int64>' does not fit the constraint of extend A, so 'f3' is invisible from 'B<Int64>'
    B<Int64>().<!NOT_MEMBER_OF!>f3<!>(Int64(1), 1)
    B<Int64>().<!NOT_MEMBER_OF!>f3<!>
}
