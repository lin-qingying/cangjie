


class A {}

interface Foo {
    prop v1: Int64
}

<!NEED_MEMBER_IMPLEMENTATION!>e<!>xtend A <: Foo {}

main() {
    0
}
