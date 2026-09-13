


interface I<T> {}

class <!SUPER_TYPES_DUPLICATE!>A<!> <: I<Bool> & I<Bool> {}

main(): Int64 {
    return 0;
}
