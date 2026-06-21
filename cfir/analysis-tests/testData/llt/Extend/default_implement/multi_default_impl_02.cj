

interface I1 {}

interface I2 <: I1 {}

interface I5 <: I2 {}

class B {}

<!EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE!>e<!>xtend B <: I2 {}

<!EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE!>e<!>xtend B <: I1 & I5 {}

main() {
}
