

sealed interface Color {}

class Red <: Color {}

class Green <: Color {}

abstract sealed class Blue <: Color {}

class DarkBlue <: Blue {}

func f(c: Color) {
    match (<!NON_EXHAUSTIVE_MATCH!>c<!>) {
        case _: Red => 0
        case _: Green => 1
        case _: DarkBlue => 2
    }
}

main() {
    f(Red())
}
