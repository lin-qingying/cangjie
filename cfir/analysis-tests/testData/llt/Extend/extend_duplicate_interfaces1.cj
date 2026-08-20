

interface Foo {}

class A {}

extend A <: Foo {}

extend A <: <!EXTEND_DUPLICATE_INTERFACE!>Foo<!> {}

main(): Int64 {
    return 1
}
