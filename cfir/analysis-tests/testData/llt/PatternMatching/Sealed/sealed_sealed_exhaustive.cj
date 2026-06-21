

sealed interface Color {}

class Red <: Color {}

class Green <: Color {}

abstract sealed class Blue <: Color {}

class DarkBlue <: Blue {}

func f(c: Color) {
    <!NON_EXHAUSTIVE_MATCH!>match (c) {
        case _: Red => 0
        case _: Green => 1
        case _: DarkBlue => 2
    }<!>
}

main() {
    f(Red())
}
