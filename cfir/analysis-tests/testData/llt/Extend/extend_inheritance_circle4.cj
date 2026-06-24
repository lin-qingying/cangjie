


class D {}

interface A <: B {}

interface C <: A {}

interface B <: C {}

<!INHERITANCE_CYCLE!>extend D<!> <: C {}

main() {
    0
}
