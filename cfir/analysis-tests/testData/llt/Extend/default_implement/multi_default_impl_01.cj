

interface I1 {}

interface I2 <: I1 {}

interface I3 {}

interface I4 <: I3 {}

class A {}

<!EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE!>e<!>xtend A <: I1 & I4 {}

<!EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE!>e<!>xtend A <: I3 & I2 {}

main() {
}
