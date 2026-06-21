


class D {}

interface A <: B {}

interface C <: A {}

interface B <: C {}

<!INHERITANCE_CYCLE!>e<!>xtend D <: C {}

main() {
    0
}
