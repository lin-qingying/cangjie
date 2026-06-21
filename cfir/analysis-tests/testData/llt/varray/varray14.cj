
// DIAGNOSTICS: -warnings


// test type recursive.
<!VALUE_TYPE_RECURSIVE!>s<!>truct S1 {
    var a: S2 = S2()
}

struct S2 {
    var a: VArray<S1, $1> = [<!RECURSIVE_CONSTRUCTOR_CALL!>S1<!>()]
}

main() {
    var a: S2 = S2()
    0
}
