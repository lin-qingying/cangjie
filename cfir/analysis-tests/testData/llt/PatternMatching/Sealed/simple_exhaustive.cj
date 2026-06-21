

abstract sealed class Color {}

class Red <: Color {}

class Green <: Color {}

class Blue <: Color {}

func f(c: Color) {
    <!NON_EXHAUSTIVE_MATCH!>match (c) {
        case _: Red => 0
        case _: Green => 1
        case _: Blue => 2
    }<!>
}

main() {
    f(Red())
}
