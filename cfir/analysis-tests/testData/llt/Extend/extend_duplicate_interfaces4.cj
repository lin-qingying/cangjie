

interface Foo {}

class A {}

extend A <: Foo & <!EXTEND_DUPLICATE_INTERFACE!>Foo<!> {}

main(): Int64 {
    return 1
}
