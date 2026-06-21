


<!INHERITANCE_CYCLE!>i<!>nterface A <: B {}

interface C <: A {}

interface B <: C {}

extend <!ILLEGAL_EXTENDED_TYPE!>A<!> <: C {}

main() {
    0
}
