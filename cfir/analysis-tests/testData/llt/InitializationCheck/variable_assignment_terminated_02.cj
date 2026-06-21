

main() {
    var c: Nothing
    while (false) {
        c = throw Exception()
    }
    let b: Bool = <!USED_BEFORE_INITIALIZATION!>c<!>
}
