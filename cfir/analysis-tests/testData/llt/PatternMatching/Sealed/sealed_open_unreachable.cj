

abstract sealed class Color {}

class Red <: Color {}

class Green <: Color {}

open class Blue <: Color {}

class DarkBlue <: Blue {}

func f(c: Color) {
    match (c) {
        case <!UNREACHABLE_PATTERN!>_: Int64<!> => -1 // unreachable
        case _: Red => 0
        case _: Green => 1
        case _: Blue => 2
        case <!UNREACHABLE_PATTERN!>_: DarkBlue<!> => -1 // unreachable
        case _ => -1 // unreachable
    }
}

main() {
    f(Red())
}
