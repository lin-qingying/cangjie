

sealed interface I {
    func f() {
        <!NON_EXHAUSTIVE_MATCH!>match (this) {
            case _: A => 1
        }<!>
    }
}

extend Int64 <: I {}

class A <: I {}

main() {
    1.f()
}
