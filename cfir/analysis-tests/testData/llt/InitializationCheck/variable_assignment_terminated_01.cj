

main() {
    let c: Nothing
    try {
        c = throw Exception()
    } catch (_) {}
    let b: Bool = <!USED_BEFORE_INITIALIZATION!>c<!>
}
