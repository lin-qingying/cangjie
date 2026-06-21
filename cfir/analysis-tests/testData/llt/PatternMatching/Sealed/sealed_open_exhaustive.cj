

abstract sealed class Color {}

class Red <: Color {}

class Green <: Color {}

open class Blue <: Color {}

class DarkBlue <: Blue {}

func f(c: Color) {
    match (c) {
        case _: Red => 0
        case _: Green => 1
        case _: Blue => 2
        case _: Color => 3
    }
}

main() {
    f(Red())
}
