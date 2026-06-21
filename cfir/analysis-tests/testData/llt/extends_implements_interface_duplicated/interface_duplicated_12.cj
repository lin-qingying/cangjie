


interface I<T> {}

<!SUPER_TYPES_DUPLICATE!>class A<!> <: I<Bool> & I<Bool> {}

main(): Int64 {
    return 0;
}
