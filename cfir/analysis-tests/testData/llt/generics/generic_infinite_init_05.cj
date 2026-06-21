


interface A<T> <: B<C<T>> {}

interface B<T> {}

open class C<T> <: <!GENERIC_INFINITE_INSTANTIATION!>A<!><C<T>> {}

main(): Int64 {
    return 0
}
