

open class A {}

class B <: A {}

func foo<X>(a: X, b: X): Int64 {
    return 1
}

main(): Int64 {
    if (foo<Int64>(1, 2) == 1) {
        return 0
    }
    return 1
}
