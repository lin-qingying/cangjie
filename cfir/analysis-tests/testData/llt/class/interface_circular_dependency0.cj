

class B <: A {}

interface A {
    func test(): A {
        return B()
    }
}

main(): Int64 {
    return 0
}
