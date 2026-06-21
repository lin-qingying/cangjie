

abstract sealed class Color {}

class Red <: Color {}

class Green <: Color {}

class Blue <: Color {}

func f(c: Color) {
    <!NON_EXHAUSTIVE_MATCH!>match (c) { // Green, Blue not covered
        case _: Red => 0
    }<!>
}

main() {
    f(Red())
}
