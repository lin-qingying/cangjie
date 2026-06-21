

private interface I {}

private class A <: I {}

private class B <: I {}

private func f(x: I) {
    <!NON_EXHAUSTIVE_MATCH!>match (x) {
        case _: A => 0
        case _: B => 1
    }<!>
}

main() {
    f(A())
}
