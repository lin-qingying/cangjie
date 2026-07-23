

abstract sealed class Color {}

class Red <: Color {}

class Green <: Color {}

class Blue <: Color {}

func f(c: Color) {
    match (<!NON_EXHAUSTIVE_MATCH!>c<!>) { // Green, Blue not covered
        case _: Red => 0
    }
}

main() {
    f(Red())
}
