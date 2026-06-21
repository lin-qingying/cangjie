


interface Interface16 {}

interface Interface17 {}

interface Interface15 <: Interface16 & Interface17 {}

class Class12 <: Interface15 & Interface17 {}

class Class824 <: Interface16 & Interface17 {}

// Class12 <: Interface16 & Interface17 {}

main() {
    let a = <!DIAG_REPORT_ERROR_MESSAGE!>i<!>f (true) {
        Class824()
    } else {
        Class12()
    }
    0
}
