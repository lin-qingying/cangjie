


class A<T> <: B<T> {}

interface B<T> {
    operator func +(b: B<T>) {
    }
}

main() {
    let a: B<Int32> = A<Int32>();
    let b: B<Int32> = A<Int32>();
    let c = a + b
    0
}
