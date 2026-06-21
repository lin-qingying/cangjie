


enum TimeUnit1 {
    Year(Int32) | Day(TimeUnit1)
}

enum TimeUnit1_2 {
    Year(Int64) | Day(TimeUnit1_2)
}

class A {}

class B {}

func f(a: A) {
    a
}

func f(a: B) {
    a
}

func g(a: Int64): A {
    A()
}

func g(a: Int32): B {
    B()
}

main(): Int64 {
    var a = <!AMBIGUOUS_CONSTRUCTOR_CALL!>Day<!>(Year(1))
    <!AMBIGUOUS_FUNCTION_CALL!>f<!>(g(1))
    return 0
}
