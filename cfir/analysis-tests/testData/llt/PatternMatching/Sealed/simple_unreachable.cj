

sealed interface Color {}

class Red <: Color {}

class Green <: Color {}

class Blue <: Color {}

func f(c: Color) {
    match (c) {
        case _: Red => 0
        case _: Green => 1
        case _: Blue => 2
        case _ => -1 // reachable
    }
}

main() {
    f(Red())
}
