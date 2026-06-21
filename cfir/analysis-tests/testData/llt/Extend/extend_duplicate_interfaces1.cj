

interface Foo {}

class A {}

extend A <: <!EXTEND_DUPLICATE_INTERFACE!>Foo<!> {}

extend A <: Foo {}

main(): Int64 {
    return 1
}
