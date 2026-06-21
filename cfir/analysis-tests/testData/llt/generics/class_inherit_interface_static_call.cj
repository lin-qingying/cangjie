

interface Number<T> {}

class C <: Number<Int32> {}

class A<T1, T2> <: I<T1, T2> where T1 <: Number<T2> {
    static func foo() {
        1
    }
}

interface I<T1, T2> where T1 <: Number<T2> {}

main() {
    return A<C, Int32>.foo() - 1
}
