

class C {}

main() {
    var t: (C, C, C) = (C(), C(), C())
    (<!OPTIONAL_CHAIN_NON_OPTIONAL!>t?[1]<!>, _) = <!TYPE_MISMATCH!>(C(), 1)<!>
}
