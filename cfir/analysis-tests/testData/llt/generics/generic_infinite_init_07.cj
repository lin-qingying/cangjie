


interface B<T> where T <: A<T> {}

// This must defined before 'class C'
open class CI<T> <: A<C<T>> {}

interface A<T> <: B<C<T>> {}

open class C<T> <: <!GENERIC_INFINITE_INSTANTIATION!>A<!><C<T>> {}

main() {
    return 0
}
