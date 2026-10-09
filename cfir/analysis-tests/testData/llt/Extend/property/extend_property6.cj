


class A {}

interface Foo {
    prop v1: Int64
}

<!NEED_MEMBER_IMPLEMENTATION!>extend A <: Foo {}<!>

main() {
    0
}
