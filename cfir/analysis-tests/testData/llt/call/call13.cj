

interface I<X> {
    static func a(input: X): X {
        return input
    }
}

class Test<X> <: I<X> {}

main() {
    return Test<Int64>.a(0)
}
