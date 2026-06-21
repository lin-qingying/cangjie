

// DIAGNOSTICS: -warnings

private interface I {}

private class A <: I {}

private interface J <: I {}

private class B {}

extend B <: J {}

func f(x: Option<I>) {
    <!NON_EXHAUSTIVE_MATCH!>match (x) {
        case Some(_: A) => 0
        case Some(_: J) => 1
        case Some(_: B) => -1 // unreachable
    }<!>
}

main() {
    f(Some(0))
}
