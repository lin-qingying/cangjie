

main() {
    0
}

interface A {}

interface B <: A {}

extend <!ILLEGAL_EXTENDED_TYPE!>A<!> <: B {}
