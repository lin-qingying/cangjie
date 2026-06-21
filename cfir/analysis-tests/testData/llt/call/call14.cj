

open class Base {}

class Sub <: Base {}

func test<X, Y>(a: X, b: Y) {
    return 1
}

func test<I>(a: Base, b: I) where I <: Sub {
    return 2
}

main(): Int64 {
    var I = test<Sub>(Base(), Sub())
    if (I != 2) {
        return 1
    }
    return 0
}
