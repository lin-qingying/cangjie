

abstract sealed class Color {}

class Red <: Color {}

class Green <: Color {}

public abstract class Blue <: Color {}

class DarkBlue <: Blue {}

func f(c: Color) {
    <!NON_EXHAUSTIVE_MATCH!>match (c) { // Blue not covered
        case _: Red => 0
        case _: Green => 1
        case _: DarkBlue => 2
    }<!>
}

main() {
    f(Red())
}
