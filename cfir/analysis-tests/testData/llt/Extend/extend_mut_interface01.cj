


interface I {
    mut prop p: Int64 {
        get() {
            1
        }
        set(_) {}
    }
}

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend Int64 <: I {}<!>

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend Bool <: I {}<!>

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend Unit <: I {}<!>

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend Nothing <: I {}<!>

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend String <: I {}<!>

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend<T> Range<T> <: I {}<!>

enum A {
    | AAA
}

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend A <: I {}<!>

main() {}
