


interface EQ {}

class A<T> {}

interface B<T> {}

extend Int32 <: EQ {}

extend<T> A<T> <: B<T> where T <: EQ {}

func test(a: (EQ) -> Int64) {
    0
}

func subFunc(a: Int32): Int64 {
    0
}

func test2(a: (A<Int32>) -> Int64) {
    0
}

func subFunc2(a: B<Int32>): Int64 {
    0
}

main() {
    test(<!NO_MATCH_FUNCTION_DECLARATION_FOR_REF!>s<!>ubFunc)
    test2(subFunc2)
    0
}
