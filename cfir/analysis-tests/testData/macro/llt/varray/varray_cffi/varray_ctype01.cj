


struct A {}

main() {
    sizeOf<<!GENERIC_ARGUMENT_NO_MATCH!>VArray<A, $3><!>>()
}
