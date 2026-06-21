

interface Base3 {
    func foo(): Int64
}

interface Data <: Base3 {
    func foo(): Int64 {
        return 1
    }
    func foo(a: Int64): Int64 {
        return a
    }
}

class Test <: Data {}

main(): Int64 {
    return 0
}
