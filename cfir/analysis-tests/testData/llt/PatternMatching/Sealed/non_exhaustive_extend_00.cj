

sealed interface I {
    func f() {
        match (<!NON_EXHAUSTIVE_MATCH!>this<!>) {
            case _: A => 1
        }
    }
}

extend Int64 <: I {}

class A <: I {}

main() {
    1.f()
}
