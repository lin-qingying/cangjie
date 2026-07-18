

// DIAGNOSTICS: -warnings

private interface I {}

private class A <: I {}

private interface J <: I {}

private class B {}

extend B <: J {}

private func f(x: Option<I>) {
    match (<!NON_EXHAUSTIVE_MATCH!>x<!>) {
        case Some(_: A) => 0
        case Some(_: J) => 1
        case Some(_: B) => -1 // unreachable
    }
}

main() {
    f(Some(A()))
}
