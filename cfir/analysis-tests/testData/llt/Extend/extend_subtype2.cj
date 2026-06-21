


interface EQ {}

class A<T> {}

interface B<T> {}

extend Int32 <: EQ {}

extend<T> A<T> <: B<T> where T <: EQ {}

main() {
    let a: B<Int32> = A<Int32>()
    let s: B<Int64> = <!TYPE_MISMATCH!>A<Int64>()<!>
    0
}
