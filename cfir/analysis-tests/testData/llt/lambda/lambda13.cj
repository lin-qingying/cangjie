

class A {
    var a = 1
}

class B {}

func test(a: (B) -> Int64) {}

func test(a: (A) -> Int64) {}

func test(a: (A) -> Unit) {}

main() {
    <!AMBIGUOUS_FUNCTION_CALL!>test<!> {r => r.a}
    0
}
