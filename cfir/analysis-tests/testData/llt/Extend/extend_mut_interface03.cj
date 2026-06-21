


interface I {
    mut prop p: Int64 {
        get() {
            1
        }
        set(_) {}
    }
}

interface I2 <: I {}

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend Int64 <: I2 {}<!>

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend Bool <: I2 {}<!>

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend Unit <: I2 {}<!>

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend Nothing <: I2 {}<!>

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend String <: I2 {}<!>

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend<T> Range<T> <: I2 {}<!>

enum A {
    | AAA
}

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend A <: I2 {}<!>

main() {}
