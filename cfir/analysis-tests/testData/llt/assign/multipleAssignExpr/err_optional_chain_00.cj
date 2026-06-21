

class C {}

main() {
    var t: (C, C, C) = (C(), C(), C())
    <!TYPE_MISMATCH!>(<!OPTIONAL_CHAIN_NON_OPTIONAL!>t?[1]<!>, _) = (C(), 1)<!>
}
