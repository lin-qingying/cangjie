

interface Foo {}

open class A <: Foo {}

class B <: A {}

extend B <: <!EXTEND_DUPLICATE_INTERFACE!>Foo<!> {}

main(): Int64 {
    return 1
}
