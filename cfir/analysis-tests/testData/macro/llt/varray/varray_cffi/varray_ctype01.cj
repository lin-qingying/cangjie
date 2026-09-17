


struct A {}

main() {
    sizeOf<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>VArray<A, $3><!>>()
}
