


open class A<T> <: B<C<T>> {}

interface B<T> where T <: A<T> {}

open class C<T> <: <!GENERIC_INFINITE_INSTANTIATION!>A<!><C<T>> & B<C<T>> {}

main() {
    return 0
}
