

class A {
    operator func ()(a: Rune) {}
}

struct S {
    func C(a: Rune): String {
        "ok"
    }
    func test() {
        var C: A = A() // This variable shadows the outer C function at line 14, so only one candidate for line 20
        func foo(a: Int8) {
            func C(a: Int8) {} // This function shadows the outer C variable at line 18
            let v: Rune = 'a'
            let str: String = C(<!TYPE_MISMATCH!>v<!>)
        }
    }
}
