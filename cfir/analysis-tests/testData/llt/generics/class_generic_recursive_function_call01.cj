

class A<T> {
    public <!RECURSIVE_CONSTRUCTOR_CALL!>init<!>(a!: A<T> = A<T>()) {}
}
