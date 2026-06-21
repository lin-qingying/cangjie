

public abstract class A {
    init() {}
}

class B <: A {}

class C <: A {}

func f(x: A) {
    <!NON_EXHAUSTIVE_MATCH!>match (x) {
        case _: B => 0
        case _: C => 1
    }<!>
}

main() {
    f(B())
}
