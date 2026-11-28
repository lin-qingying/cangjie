

class C {}

main() {
    var t: (C, C, C) = (C(), C(), C())
    (<!OPTIONAL_CHAIN_NON_OPTIONAL!>t?[1]<!>, _) = <!MISMATCHED_TYPES_MULTIPLE_ASSIGN!>(C(), 1)<!>
}
